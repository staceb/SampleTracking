package com.atlassian.jira.notification;

import com.atlassian.core.ofbiz.association.AssociationManager;
import com.atlassian.core.ofbiz.util.EntityUtils;
import com.atlassian.core.user.preferences.Preferences;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.ClearCacheEvent;
import com.atlassian.jira.event.issue.IssueEvent;
import com.atlassian.jira.exception.RemoveException;
import com.atlassian.jira.extension.Startable;
import com.atlassian.jira.notification.type.GroupCFValue;
import com.atlassian.jira.notification.type.UserCFValue;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.jira.ofbiz.OfBizListIterator;
import com.atlassian.jira.permission.PermissionContextFactory;
import com.atlassian.jira.permission.PermissionTypeManager;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.scheme.AbstractSchemeManager;
import com.atlassian.jira.scheme.Scheme;
import com.atlassian.jira.scheme.SchemeEntity;
import com.atlassian.jira.scheme.SchemeFactory;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.preferences.JiraUserPreferences;
import com.atlassian.jira.user.preferences.PreferenceKeys;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.bean.I18nBean;
import com.atlassian.mail.MailException;
import com.atlassian.mail.MailFactory;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.EntityCondition;
import org.ofbiz.core.entity.EntityExpr;
import org.ofbiz.core.entity.EntityFieldMap;
import org.ofbiz.core.entity.EntityFindOptions;
import org.ofbiz.core.entity.EntityOperator;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.atlassian.core.util.map.EasyMap.build;

public class DefaultNotificationSchemeManager extends AbstractSchemeManager implements NotificationSchemeManager, Startable
{
    private static final Logger log = Logger.getLogger(DefaultNotificationSchemeManager.class);
    private static final String SCHEME_ENTITY_NAME = "NotificationScheme";
    private static final String NOTIFICATION_ENTITY_NAME = "Notification";
    private static final String SCHEME_DESC = "Notification";
    private static final String DEFAULT_NAME_KEY = "admin.schemes.notifications.default";
    private final OfBizDelegator delegator;
    private final EventPublisher eventPublisher;
    private final NotificationTypeManager notificationTypeManager;

    public DefaultNotificationSchemeManager(ProjectManager projectManager, PermissionTypeManager permissionTypeManager,
            PermissionContextFactory permissionContextFactory, OfBizDelegator delegator,
            SchemeFactory schemeFactory, EventPublisher eventPublisher, NotificationTypeManager notificationTypeManager,
            final AssociationManager associationManager, final GroupManager groupManager)
    {
        super(projectManager, permissionTypeManager, permissionContextFactory, schemeFactory, associationManager, delegator, groupManager);
        this.delegator = delegator;
        this.eventPublisher = eventPublisher;
        this.notificationTypeManager = notificationTypeManager;
    }

    public void start() throws Exception
    {
        eventPublisher.register(this);
    }

    @EventListener
    public void onClearCache(final ClearCacheEvent event)
    {
        super.onClearCache(event);
    }

    public String getSchemeEntityName()
    {
        return SCHEME_ENTITY_NAME;
    }

    public String getEntityName()
    {
        return NOTIFICATION_ENTITY_NAME;
    }

    public String getSchemeDesc()
    {
        return SCHEME_DESC;
    }

    public String getDefaultNameKey()
    {
        return DEFAULT_NAME_KEY;
    }

    public String getDefaultDescriptionKey()
    {
        return null;
    }

    public GenericValue createDefaultScheme() throws GenericEntityException
    {
        if (getDefaultScheme() == null)
        {
            String name = getApplicationI18n().getText(DEFAULT_NAME_KEY);
            GenericValue defaultScheme;
            if (DEFAULT_NAME_KEY.equals(name))
            {
                defaultScheme = createSchemeGenericValue(build("name", "Default Notification Scheme", "description", null));
            }
            else
            {
                defaultScheme = createSchemeGenericValue(build("name", name, "description", null));
            }
            return defaultScheme;
        }
        else
        {
            return getDefaultScheme();
        }
    }

    public void removeSchemeEntitiesForField(String customFieldId) throws RemoveException
    {
        // this hack will only work for the two built in Notification Event Types that use Custom Fields.
        // If anyone writes a plug-in that uses Custom Fields, we will currently not be able to delete this automatically.
        // Remove for User_Custom_Field_Value
        removeEntities(UserCFValue.ID, customFieldId);
        removeEntities(GroupCFValue.ID, customFieldId);
    }

    /**
     * Get the notification scheme for this project. There should be only one.
     * <p/>
     * Returns null if there problems (e.g. more than one scheme association) encountered.
     *
     * @param projectGV the project
     * @return notificationScheme   the GenericValue object representing a notification scheme
     */
    public GenericValue getNotificationSchemeForProject(GenericValue projectGV)
    {
        try
        {
            List<GenericValue> notificationSchemes = getSchemes(projectGV);
            if (notificationSchemes != null && !notificationSchemes.isEmpty()) {
                // Ensure that there is only one notification scheme associated with the project
                if (notificationSchemes.size() > 1)
                {
                    log.error("There are multiple notification schemes associated with the project: " + projectGV.getString("name") + ". " +
                            "No emails will be sent for issue events in this project.");
                }
                else
                {
                    return notificationSchemes.get(0);
                }
            }
        }
        catch (GenericEntityException e)
        {
            log.error("There was an error retrieving the notification schemes for the project: " + projectGV.getString("name") + ". " +
                    "No emails will be sent for issue events in this project.", e);
        }
        // No notification scheme associated with this project or multiple
        return null;
    }

    /**
     * Retrieve a map of scheme ids to scheme names that match the specified conditions.
     *
     * @param conditions    Map of conditions on which to limit the search
     * @return Map          scheme ids -> scheme names
     */
    public Map<Long, String> getSchemesMapByConditions(Map<String, ?> conditions)
    {
        Map<Long, String> schemeMap = new LinkedHashMap<Long, String>();
        OfBizListIterator listIterator = null;
        Long schemeId = null;

        try
        {
            // Set the search to be distinct
            EntityFindOptions entityFindOptions = new EntityFindOptions();
            entityFindOptions.setDistinct(true);

            // Retrieve the notification scheme entities that are assocaited with the conditions map
            listIterator = delegator.findListIteratorByCondition("Notification", new EntityFieldMap(conditions, EntityOperator.AND), null, null, null, entityFindOptions);
            Map resultMap = listIterator.next();
            while (resultMap != null)
            {
                if (resultMap.get("scheme") != null)
                {
                    schemeId = (Long) resultMap.get("scheme");
                    final Scheme notificationScheme = getSchemeObject(schemeId);
                    schemeMap.put(schemeId, notificationScheme.getName());
                }
                resultMap = listIterator.next();
            }
        }
        finally
        {
            if (listIterator != null)
            {
                // Close the iterator
                listIterator.close();
            }
        }

        return schemeMap;
    }

    public Collection<GenericValue> getSchemesContainingEntity(String type, String parameter)
    {
        Map<String, ?> conditions = build("type", type, "parameter", parameter);
        Collection<GenericValue> entities = delegator.findByAnd(NOTIFICATION_ENTITY_NAME, conditions);
        Set<Long> schemeIds = new HashSet<Long>();
        List<EntityCondition> entityConditions = new ArrayList<EntityCondition>();
        for (GenericValue schemeEntity: entities)
        {
            // This is not needed if we can do a distinct select
            schemeIds.add(schemeEntity.getLong("scheme"));
        }
        for (Long id: schemeIds)
        {
            entityConditions.add(new EntityExpr("id", EntityOperator.EQUALS, id));
        }

        if (entityConditions.isEmpty())
        {
            return Collections.emptyList();
        }
        return delegator.findByOr(SCHEME_ENTITY_NAME, entityConditions, Collections.<String>emptyList());
    }

    public boolean isHasMailServer() throws MailException
    {
        try
        {
            Object smtp = MailFactory.getServerManager().getDefaultSMTPMailServer();
            return (smtp != null);
        }
        catch (Exception ignored)
        {
            // This isn't the place to die if anything is wrong
        }
        return false;
    }

    public GenericValue createSchemeEntity(GenericValue scheme, SchemeEntity schemeEntity) throws GenericEntityException
    {
        if (!(schemeEntity.getEntityTypeId() instanceof Long))
        {
            throw new IllegalArgumentException("Notification scheme IDs must be Long values.");
        }

        return EntityUtils.createValue(getEntityName(), build("scheme", scheme.getLong("id"), "eventTypeId", schemeEntity.getEntityTypeId(), "type", schemeEntity.getType(), "parameter", schemeEntity.getParameter(), "templateId", schemeEntity.getTemplateId()));
    }

    /**
     * Retrieve the set of recipients for the event using the specified notification entity.
     * @param event issue event
     * @param notification scheme entity
     * @return a Set of notification recipients
     * @throws GenericEntityException
     */
    public Set<NotificationRecipient> getRecipients(IssueEvent event, SchemeEntity notification) throws GenericEntityException
    {
        Set<NotificationRecipient> recipients = new HashSet<NotificationRecipient>();

        NotificationType notificationType = notificationTypeManager.getNotificationType(notification.getType());

        recipients.addAll(notificationType.getRecipients(event, notification.getParameter()));

        final User user = event.getUser();
        if (user != null)
        {
            // check if the user wishes to be notified of his own changes
            Preferences userPreference = new JiraUserPreferences(user);
            if (!userPreference.getBoolean(PreferenceKeys.USER_NOTIFY_OWN_CHANGES))
            {
                recipients.remove(new NotificationRecipient(user));
                log.debug("Removed user " + user.getDisplayName() + " with email address " + user.getEmailAddress() + " from notification because they are they modifier and do not wish to be notified.");
            }
        }
        return recipients;
    }

    public boolean hasEntities(GenericValue scheme, Long eventTypeId, String type, String parameter, Long templateId)
            throws GenericEntityException
    {
        List entity = scheme.getRelatedByAnd("Child" + getEntityName(), build("eventTypeId", eventTypeId, "type", type, "parameter", parameter, "templateId", templateId));
        return entity != null && !entity.isEmpty();
    }

    public GenericValue copySchemeEntity(GenericValue scheme, GenericValue entity) throws GenericEntityException
    {
        SchemeEntity schemeEntity = new SchemeEntity(entity.getLong("id"), entity.getString("type"), entity.getString("parameter"), entity.getLong("eventTypeId"), entity.getLong("templateId"), null);
        return createSchemeEntity(scheme, schemeEntity);
    }

    //This one if for Workflow Scheme Manager as the entity type is is a string
    public List<GenericValue> getEntities(GenericValue scheme, String entityTypeId) throws GenericEntityException
    {
        throw new IllegalArgumentException("Notification scheme event type IDs must be Long values.");
    }

    public List<GenericValue> getEntities(GenericValue scheme, Long eventTypeId) throws GenericEntityException
    {
        return scheme.getRelatedByAnd("Child" + getEntityName(), build("eventTypeId", eventTypeId));
    }

    public List<GenericValue> getEntities(GenericValue scheme, Long eventTypeId, String parameter) throws GenericEntityException
    {
        return scheme.getRelatedByAnd("Child" + getEntityName(), build("eventTypeId", eventTypeId, "parameter", parameter));
    }

    public List<GenericValue> getEntities(GenericValue scheme, String type, Long entityTypeId) throws GenericEntityException
    {
        return scheme.getRelatedByAnd("Child" + getEntityName(), build("eventTypeId", entityTypeId, "type", type));
    }

    public boolean hasSchemeAuthority(Long entityType, GenericValue entity)
    {
        return false;
    }

    public boolean hasSchemeAuthority(Long entityType, GenericValue entity, com.atlassian.crowd.embedded.api.User user, boolean issueCreation)
    {
        return false;
    }

    I18nHelper getApplicationI18n()
    {
        return new I18nBean((User) null);
    }
}
