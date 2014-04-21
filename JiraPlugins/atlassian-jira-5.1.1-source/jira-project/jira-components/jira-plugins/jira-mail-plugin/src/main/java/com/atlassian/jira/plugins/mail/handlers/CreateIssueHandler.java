package com.atlassian.jira.plugins.mail.handlers;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.IssueFieldConstants;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.SummarySystemField;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.security.IssueSecurityLevelManager;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.mail.MailThreadManager;
import com.atlassian.jira.plugin.assignee.AssigneeResolver;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.service.util.handler.MessageHandlerContext;
import com.atlassian.jira.service.util.handler.MessageHandlerErrorCollector;
import com.atlassian.jira.service.util.handler.MessageHandlerExecutionMonitor;
import com.atlassian.jira.user.UserUtils;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.web.FieldVisibilityManager;
import com.atlassian.jira.web.action.issue.IssueCreationHelperBean;
import com.atlassian.jira.web.bean.I18nBean;
import com.atlassian.jira.workflow.WorkflowFunctionUtils;
import com.atlassian.mail.MailUtils;
import com.opensymphony.util.TextUtils;
import org.ofbiz.core.entity.GenericValue;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.atlassian.jira.ComponentManager.getComponentInstanceOfType;
import static org.apache.commons.lang.StringUtils.join;

/**
 * A message handler to create a new issue from an incoming message. Note: requires public noarg constructor as this
 * class is instantiated by reflection
 */
public class CreateIssueHandler extends AbstractMessageHandler
{
    public static final String KEY_PROJECT = "project";
    public static final String KEY_ISSUETYPE = "issuetype";
    public static final String CC_ASSIGNEE = "ccassignee";
    public static final String CC_WATCHER = "ccwatcher";

    public String projectKey; // default project where new issues are created
    public String issueType; // default type for new issues

    public static final boolean DEFAULT_CC_ASSIGNEE = true;

    public boolean ccAssignee = DEFAULT_CC_ASSIGNEE; // Whether the first existing Cc'ed user becomes the assignee
    public boolean ccWatcher = false; // Whether each Cc'ed user should watch the created issue

    public void init(Map<String, String> params, MessageHandlerErrorCollector errorCollector)
    {
        log.debug("CreateIssueHandler.init(params: " + params + ")");

        super.init(params, errorCollector);

        if (params.containsKey(KEY_PROJECT))
        {
            projectKey = params.get(KEY_PROJECT);
        }

        if (params.containsKey(KEY_ISSUETYPE))
        {
            issueType = params.get(KEY_ISSUETYPE);
        }

        if (params.containsKey(CC_ASSIGNEE))
        {
            ccAssignee = Boolean.valueOf(params.get(CC_ASSIGNEE));
        }
        if (params.containsKey(CC_WATCHER))
        {
            ccWatcher = Boolean.valueOf(params.get(CC_WATCHER));
        }
    }

    @Override
    public boolean handleMessage(Message message, MessageHandlerContext context) throws MessagingException
    {
        log.debug("CreateIssueHandler.handleMessage");

        if (!canHandleMessage(message, context.getMonitor()))
        {
            return deleteEmail;
        }

        try
        {
            // get either the sender of the message, or the default reporter
            User reporter = getReporter(message, context);

            // no reporter - so reject the message
            if (reporter == null)
            {
                final String error = getI18nBean().getText("admin.mail.no.default.reporter");
                context.getMonitor().warning(error);
                context.getMonitor().messageRejected(message, error);
                return false;
            }

            final Project project = getProject(message);

            log.debug("Project = " + project);
            if (project == null)
            {
                final String text = getI18nBean().getText("admin.mail.no.project.configured");
                context.getMonitor().warning(text);
                context.getMonitor().messageRejected(message, text);
                return false;
            }

            // Check that the license is valid before allowing issues to be created
            // This checks for: evaluation licenses expired, user limit licenses where limit has been exceeded
            ErrorCollection errorCollection = new SimpleErrorCollection();
            // Note: want English locale here for logging purposes
            I18nHelper i18nHelper = new I18nBean(Locale.ENGLISH);

            getIssueCreationHelperBean().validateLicense(errorCollection, i18nHelper);
            if (errorCollection.hasAnyErrors())
            {
                context.getMonitor().warning(getI18nBean().getText("admin.mail.bad.license", errorCollection.getErrorMessages().toString()));
                return false;
            }

            // If user does not have create permissions, there's no point proceeding. Error out here to avoid a stack
            // trace blow up from the WorkflowManager later on.
            if (!getPermissionManager().hasPermission(Permissions.CREATE_ISSUE, project, reporter, true) && reporter.getDirectoryId() != -1)
            {
                final String error = getI18nBean().getText("admin.mail.no.create.permission", reporter.getName());
                context.getMonitor().warning(error);
                context.getMonitor().messageRejected(message, error);
                return false;
            }

            log.debug("Issue Type Key = = " + issueType);

            if (!hasValidIssueType())
            {
                context.getMonitor().warning(getI18nBean().getText("admin.mail.invalid.issue.type"));
                return false;
            }
            String summary = message.getSubject();
            if (!TextUtils.stringSet(summary))
            {
                context.getMonitor().error(getI18nBean().getText("admin.mail.no.subject"));
                return false;
            }
            if (summary.length() > SummarySystemField.MAX_LEN.intValue())
            {
                context.getMonitor().info("Truncating summary field because it is too long: " + summary);
                summary = summary.substring(0, SummarySystemField.MAX_LEN.intValue() - 3) + "...";
            }

            // JRA-7646 - check if priority/description is hidden - if so, do not set
            String priority = null;
            String description = null;

            if (!getFieldVisibilityManager().isFieldHiddenInAllSchemes(project.getId(),
                    IssueFieldConstants.PRIORITY, Collections.singletonList(issueType)))
            {
                priority = getPriority(message);
            }

            if (!getFieldVisibilityManager().isFieldHiddenInAllSchemes(project.getId(),
                    IssueFieldConstants.DESCRIPTION, Collections.singletonList(issueType)))
            {
                description = getDescription(reporter, message);
            }

            MutableIssue issueObject = getIssueFactory().getIssue();
            issueObject.setProjectObject(project);
            issueObject.setSummary(summary);
            issueObject.setDescription(description);
            issueObject.setIssueTypeId(issueType);
            issueObject.setReporter(reporter);

            // if no valid assignee found, attempt to assign to default assignee
            User assignee = null;
            if (ccAssignee)
            {
                assignee = getFirstValidAssignee(message.getAllRecipients(), project);
            }
            if (assignee == null)
            {
                assignee = getAssigneeResolver().getDefaultAssignee(issueObject, Collections.EMPTY_MAP);
            }

            if (assignee != null)
            {
                issueObject.setAssignee(assignee);
            }

            issueObject.setPriorityId(priority);

            // Ensure issue level security is correct
            setDefaultSecurityLevel(issueObject);

            /*
             * + FIXME -- set cf defaults @todo +
             */
            // set default custom field values
            // CustomFieldValuesHolder cfvh = new CustomFieldValuesHolder(issueType, project.getId());
            // fields.put("customFields", CustomFieldUtils.getCustomFieldValues(cfvh.getCustomFields()));
            Map<String, Object> fields = new HashMap<String, Object>();
            fields.put("issue", issueObject);
            // TODO: How is this supposed to work? There is no issue created yet; ID = null.
            // wseliga note: Ineed I think that such call does not make sense - it will be always null
            MutableIssue originalIssue = getIssueManager().getIssueObject(issueObject.getId());

            // Give the CustomFields a chance to set their default values JRA-11762
            List<CustomField> customFieldObjects = ComponentAccessor.getCustomFieldManager().getCustomFieldObjects(issueObject);
            for (CustomField customField : customFieldObjects)
            {
                issueObject.setCustomFieldValue(customField, customField.getDefaultValue(issueObject));
            }

            fields.put(WorkflowFunctionUtils.ORIGINAL_ISSUE_KEY, originalIssue);
            final Issue issue = context.createIssue(reporter, issueObject);

            if (issue != null)
            {
                // Add Cc'ed users as watchers if params set - JRA-9983
                if (ccWatcher)
                {
                    addCcWatchersToIssue(message, issue, reporter, context, context.getMonitor());
                }

                // Record the message id of this e-mail message so we can track replies to this message
                // and associate them with this issue
                recordMessageId(MailThreadManager.ISSUE_CREATED_FROM_EMAIL, message, issue.getId(), context);
            }

            // TODO: if this throws an error, then the issue is already created, but the email not deleted - we will keep "handling" this email over and over :(
            createAttachmentsForMessage(message, issue, context);

            return true;
        }
        catch (Exception e)
        {
            context.getMonitor().warning(getI18nBean().getText("admin.mail.unable.to.create.issue"), e);
        }

        // something went wrong - don't delete the message
        return false;
    }

    private IssueCreationHelperBean getIssueCreationHelperBean()
    {
        return ComponentAccessor.getComponent(IssueCreationHelperBean.class);
    }

    PermissionManager getPermissionManager()
    {
        return ComponentAccessor.getPermissionManager();
    }

    FieldVisibilityManager getFieldVisibilityManager()
    {
        return ComponentAccessor.getComponent(FieldVisibilityManager.class);
    }

    AssigneeResolver getAssigneeResolver()
    {
        return ComponentAccessor.getComponent(AssigneeResolver.class);
    }

    IssueManager getIssueManager()
    {
        return ComponentAccessor.getIssueManager();
    }

    IssueFactory getIssueFactory()
    {
        return ComponentAccessor.getIssueFactory();
    }

    /**
     * Adds all valid users that are in the email to and cc fields as watchers of the issue.
     *
     *
     * @param message  message to extract the email addresses from
     * @param issue    issue to add the watchers to
     * @param reporter
     * @param context
     *@param messageHandlerExecutionMonitor @throws MessagingException message errors
     */
    public void addCcWatchersToIssue(Message message, Issue issue, User reporter, MessageHandlerContext context, MessageHandlerExecutionMonitor messageHandlerExecutionMonitor) throws MessagingException
    {
        Collection<User> users = getAllUsersFromEmails(message.getAllRecipients());
        //we don't want to add the reporter to the watchers list, lets get rid of him.
        users.remove(reporter);
        if (!users.isEmpty())
        {
            if (context.isRealRun())
            {
                for (User user : users)
                {
                    getWatcherManager().startWatching(user, issue);
                }
            }
            else
            {
                final String watchers = join(users, ",");
                messageHandlerExecutionMonitor.info("Adding watchers " + watchers);
                log.debug("Watchers [" + watchers + "] not added due to dry-run mode.");
            }
        }
    }

    WatcherManager getWatcherManager()
    {
        return getComponentInstanceOfType(WatcherManager.class);
    }

    public Collection<User> getAllUsersFromEmails(Address addresses[])
    {
        if (addresses == null || addresses.length == 0)
        {
            return Collections.emptyList();
        }
        final List<User> users = new ArrayList<User>();
        for (Address address : addresses)
        {
            String emailAddress = getEmailAddress(address);
            if (emailAddress != null)
            {
                User user = UserUtils.getUserByEmail(emailAddress);
                if (user != null)
                {
                    users.add(user);
                }
            }
        }
        return users;
    }

    private String getEmailAddress(Address address)
    {
        if (address instanceof InternetAddress)
        {
            InternetAddress internetAddress = (InternetAddress) address;
            return internetAddress.getAddress();
        }
        return null;
    }

    protected Project getProject(Message message)
    {
        // if there is no project then the issue cannot be created
        if (projectKey == null)
        {
            log.debug("Project key NOT set. Cannot find project.");
            return null;
        }

        log.debug("Project key = " + projectKey);

        return getProjectManager().getProjectObjByKey(projectKey.toUpperCase(Locale.getDefault()));
    }

    protected boolean hasValidIssueType()
    {
        // if there is no project then the issue cannot be created
        if (issueType == null)
        {
            log.debug("Issue Type NOT set. Cannot find Issue type.");
            return false;
        }

        IssueType issueTypeObject = getConstantsManager().getIssueTypeObject(issueType);
        if (issueTypeObject == null)
        {
            log.debug("Issue Type with does not exist with id of " + issueType);
            return false;
        }

        log.debug("Issue Type Object = " + issueTypeObject.getName());
        return true;
    }

    protected ProjectManager getProjectManager()
    {
        return ComponentAccessor.getProjectManager();
    }

    /**
     * Extracts the description of the issue from the message
     *
     * @param reporter the established reporter of the issue
     * @param message  the message from which the issue is created
     * @return the description of the issue
     * @throws MessagingException if cannot find out who is the message from
     */
    private String getDescription(User reporter, Message message) throws MessagingException
    {
        return recordFromAddressForAnon(reporter, message, MailUtils.getBody(message));
    }

    /**
     * Adds the senders' from addresses to the end of the issue's details (if they could be extracted), if the e-mail
     * has been received from an unknown e-mail address and the mapping to an "anonymous" user has been enabled.
     *
     * @param reporter    the established reporter of the issue (after one has been established)
     * @param message     the message that is used to create issue
     * @param description the issues exracted description
     * @return the modified description if the e-mail is from anonymous user, unmodified description otherwise
     * @throws MessagingException if cannot find out who is the message from
     */
    private String recordFromAddressForAnon(User reporter, Message message, String description) throws MessagingException
    {
        // If the message has been created for an anonymous user add the senders e-mail address to the description.
        if (reporteruserName != null && reporteruserName.equals(reporter.getName()))
        {
            description += "\n[Created via e-mail ";
            if (message.getFrom() != null && message.getFrom().length > 0)
            {
                description += "received from: " + message.getFrom()[0] + "]";
            }
            else
            {
                description += "but could not establish sender's address.]";
            }
        }
        return description;
    }

    /**
     * Using the first 'X-Priority' from the message, get the issue's priority
     *
     * @param message message
     * @return message priority
     * @throws MessagingException if cannot read message's header
     */
    private String getPriority(Message message) throws MessagingException
    {
        String[] xPrioHeaders = message.getHeader("X-Priority");

        if (xPrioHeaders != null && xPrioHeaders.length > 0)
        {
            String xPrioHeader = xPrioHeaders[0];

            int priorityValue = Integer.parseInt(TextUtils.extractNumber(xPrioHeader));

            if (priorityValue == 0)
            {
                return getDefaultSystemPriority();
            }

            // if priority is unset - pick the closest priority, this should be a sensible default
            Collection<Priority> priorities = getConstantsManager().getPriorityObjects();

            Iterator<Priority> priorityIt = priorities.iterator();

            /*
             * NOTE: Valid values for X-priority are (1=Highest, 2=High, 3=Normal, 4=Low & 5=Lowest) The X-Priority
             * (priority in email header) is divided by 5 (number of valid values) this gives the percentage
             * representation of the priority. We multiply this by the priority.size() (number of priorities in jira) to
             * scale and map the percentage to a priority in jira.
             */
            int priorityNumber = (int) Math.ceil(((double) priorityValue / 5d) * (double) priorities.size());
            // if priority is too large, assume its the 'lowest'
            if (priorityNumber > priorities.size())
            {
                priorityNumber = priorities.size();
            }

            String priority = null;

            for (int i = 0; i < priorityNumber; i++)
            {
                priority = priorityIt.next().getId();
            }

            return priority;
        }
        else
        {
            return getDefaultSystemPriority();
        }
    }

    ConstantsManager getConstantsManager()
    {
        return getComponentInstanceOfType(ConstantsManager.class);
    }

    /**
     * Returns a default system priority. If default system priority if not
     * set, tries to find 'middle' priority based on other priorities. It may
     * throw RuntimeException if there is not default priority set and there
     * are no other priorities (which is highly unlikely).
     *
     * @return a default system priority
     * @throws RuntimeException if no default set and no other priorities found.
     */
    private String getDefaultSystemPriority()
    {
        // if priority header is not set, assume it's 'default'
        Priority defaultPriority = getConstantsManager().getDefaultPriorityObject();
        if (defaultPriority == null)
        {
            log.warn("Default priority was null. Using the 'middle' priority.");
            Collection<Priority> priorities = getConstantsManager().getPriorityObjects();
            final int times = (int) Math.ceil((double) priorities.size() / 2d);
            Iterator<Priority> priorityIt = priorities.iterator();
            for (int i = 0; i < times; i++)
            {
                defaultPriority = priorityIt.next();
            }
        }
        if (defaultPriority == null)
        {
            throw new RuntimeException("Default priority not found");
        }
        return defaultPriority.getId();
    }

    /**
     * Given an array of addresses, this method returns the first valid
     * assignee for the appropriate project.
     * It returns null if addresses is null or empty array, or none of the
     * users found by addresses is assignable.
     *
     * @param addresses array of addresses
     * @param project   project generic value
     * @return first assignable user based on the array of addresses
     */
    public static User getFirstValidAssignee(Address[] addresses, Project project)
    {
        if (addresses == null || addresses.length == 0)
        {
            return null;
        }

        for (Address address : addresses)
        {
            if (address instanceof InternetAddress)
            {
                InternetAddress email = (InternetAddress) address;

                User validUser = UserUtils.getUserByEmail(email.getAddress());
                if (validUser != null && getComponentInstanceOfType(PermissionManager.class).
                        hasPermission(Permissions.ASSIGNABLE_USER, project, validUser))
                {
                    return validUser;
                }
            }
        }

        return null;
    }

    private void setDefaultSecurityLevel(MutableIssue issue) throws Exception
    {
        GenericValue project = issue.getProject();
        if (project != null)
        {
            final Long levelId = getIssueSecurityLevelManager().getSchemeDefaultSecurityLevel(project);
            if (levelId != null)
            {
                issue.setSecurityLevel(getIssueSecurityLevelManager().getIssueSecurity(levelId));
            }
        }
    }

    IssueSecurityLevelManager getIssueSecurityLevelManager()
    {
        return getComponentInstanceOfType(IssueSecurityLevelManager.class);
    }

    /**
     * Text parts are not attached but rather potentially form the source of issue text.
     * However text part attachments are kept providing they aint empty.
     *
     * @param part The part which will have a content type of text/plain to be tested.
     * @return Only returns true if the part is an attachment and not empty
     * @throws MessagingException
     * @throws IOException
     */
    protected boolean attachPlainTextParts(final Part part) throws MessagingException, IOException
    {
        return !MailUtils.isContentEmpty(part) && MailUtils.isPartAttachment(part);
    }

    /**
     * Html parts are not attached but rather potentially form the source of issue text.
     * However html part attachments are kept providing they aint empty.
     *
     * @param part The part which will have a content type of text/html to be tested.
     * @return Only returns true if the part is an attachment and not empty
     * @throws MessagingException
     * @throws IOException
     */
    protected boolean attachHtmlParts(final Part part) throws MessagingException, IOException
    {
        return !MailUtils.isContentEmpty(part) && MailUtils.isPartAttachment(part);
    }
}
