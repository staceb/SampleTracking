package com.atlassian.jira.scheme;

import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.issue.security.IssueSecuritySchemeManager;
import com.atlassian.jira.notification.NotificationSchemeManager;
import com.atlassian.jira.permission.PermissionSchemeManager;
import com.atlassian.jira.workflow.WorkflowSchemeManager;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.atlassian.jira.ComponentManager.getComponentInstanceOfType;

/**
 * The implementation of the SchemeFactory
 */
public class DefaultSchemeFactory implements SchemeFactory
{
    private static final String NOTIFICATION_SCHEME = "NotificationScheme";
    private static final String NOTIFICATION_ENTITY = "Notification";
    private static final String PERMISSION_SCHEME = "PermissionScheme";
    private static final String PERMISSION_ENTITY = "SchemePermissions";
    private static final String ISSUE_SECURITY_SCHEME = "IssueSecurityScheme";
    private static final String ISSUE_SECURITY_ENTITY = "SchemeIssueSecurities";
    private static final String WORKFLOW_SCHEME = "WorkflowScheme";
    private static final String WORKFLOW_SCHEME_ENTITY = "WorkflowSchemeEntity";

    public Scheme getScheme(GenericValue schemeGV) throws DataAccessException
    {
        return getScheme(schemeGV, true);
    }

    public List<Scheme> getSchemes(List<GenericValue> schemeGVs) throws DataAccessException
    {
        List<Scheme> schemes = new ArrayList<Scheme>();
        for (GenericValue schemeGV : schemeGVs)
        {
            schemes.add(getScheme(schemeGV, true));
        }
        return schemes;
    }

    public Scheme getSchemeWithEntitiesComparable(GenericValue schemeGV)
    {
        return getScheme(schemeGV, false);
    }

    public List<Scheme> getSchemesWithEntitiesComparable(List<GenericValue> schemeGVs)
    {
        List<Scheme> schemes = new ArrayList<Scheme>();
        for (GenericValue schemeGV : schemeGVs)
        {
            schemes.add(getScheme(schemeGV, false));
        }
        return schemes;
    }

    public Scheme getScheme(GenericValue schemeGV, Collection<GenericValue> schemeEntityGVs)
    {
        Scheme scheme = getScheme(schemeGV);
        scheme.setEntities(convertToSchemeEntities(schemeEntityGVs, scheme.getId(), true));
        return scheme;
    }

    private Scheme getScheme(GenericValue schemeGV, boolean includeEntityIds) throws DataAccessException
    {
        try
        {
            final Long id = schemeGV.getLong("id");
            final String type = schemeGV.getEntityName();
            final String name = schemeGV.getString("name");
            final String description = schemeGV.getString("description");

            Collection<GenericValue> schemeEntites = null;
            // This is the parent entity name, not the scheme entities name
            if (NOTIFICATION_SCHEME.equals(type))
            {
                schemeEntites = getComponentInstanceOfType(NotificationSchemeManager.class).getEntities(schemeGV);
            }
            else if (PERMISSION_SCHEME.equals(type))
            {
                schemeEntites = getComponentInstanceOfType(PermissionSchemeManager.class).getEntities(schemeGV);
            }
            else if (ISSUE_SECURITY_SCHEME.equals(type))
            {
                schemeEntites = getComponentInstanceOfType(IssueSecuritySchemeManager.class).getEntities(schemeGV);
            }
            else if (WORKFLOW_SCHEME.equals(type))
            {
                schemeEntites = getComponentInstanceOfType(WorkflowSchemeManager.class).getEntities(schemeGV);
            }
            else
            {
                schemeEntites = Collections.emptyList();
            }

            return new Scheme(id, type, name, description, convertToSchemeEntities(schemeEntites, id, includeEntityIds));
        }
        catch (GenericEntityException e)
        {
            throw new DataAccessException(e);
        }
    }

    private Collection<SchemeEntity> convertToSchemeEntities(Collection<GenericValue> schemeEntityGVs, Long schemeId, boolean includeEntityIds)
    {
        Collection<SchemeEntity> entities = new ArrayList<SchemeEntity>();
        for (GenericValue schemeEntityGV : schemeEntityGVs)
        {
            final Long templateId;
            final Object eventTypeId;
            // We have permission scheme entities
            String type;
            String parameter;
            if (PERMISSION_ENTITY.equals(schemeEntityGV.getEntityName()))
            {
                templateId = null;
                eventTypeId = schemeEntityGV.getLong("permission");
                type = schemeEntityGV.getString("type");
                parameter = schemeEntityGV.getString("parameter");
            }
            else if (NOTIFICATION_ENTITY.equals(schemeEntityGV.getEntityName()))
            {
                templateId = schemeEntityGV.getLong("templateId");
                eventTypeId = schemeEntityGV.getLong("eventTypeId");
                type = schemeEntityGV.getString("type");
                parameter = schemeEntityGV.getString("parameter");
            }
            else if (ISSUE_SECURITY_ENTITY.equals(schemeEntityGV.getEntityName()))
            {
                templateId = null;
                eventTypeId = schemeEntityGV.getLong("security");
                type = schemeEntityGV.getString("type");
                parameter = schemeEntityGV.getString("parameter");
            }
            else if (WORKFLOW_SCHEME_ENTITY.equals(schemeEntityGV.getEntityName()))
            {
                templateId = null;
                eventTypeId = schemeEntityGV.getString("workflow");
                type = "issuetype";
                parameter = schemeEntityGV.getString("issuetype");
            }
            else
            {
                throw new IllegalArgumentException("Unrecognised Scheme entity '" + schemeEntityGV.getEntityName() + "'");
            }
            SchemeEntity schemeEntity = new SchemeEntity(includeEntityIds ? schemeEntityGV.getLong("id") : null,
                    type,
                    parameter,
                    eventTypeId,
                    templateId,
                    includeEntityIds ? schemeId : null);
            entities.add(schemeEntity);
        }
        return entities;
    }
}
