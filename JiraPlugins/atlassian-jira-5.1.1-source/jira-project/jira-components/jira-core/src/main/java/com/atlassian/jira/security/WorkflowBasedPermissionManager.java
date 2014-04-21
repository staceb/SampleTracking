package com.atlassian.jira.security;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.permission.PermissionContext;
import com.atlassian.jira.permission.PermissionContextFactory;
import com.atlassian.jira.permission.WorkflowPermission;
import com.atlassian.jira.permission.WorkflowPermissionFactory;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;

import java.util.List;

/**
 * Permission manager which allows workflow permissions to be further restricted for each workflow step, in the workflow XML descriptor.
 * For instance, if the workflow contains a step:
 * <pre>
 *    &lt;step id="1" name="Open"&gt;
 *    &lt;meta name="jira.status.id"&gt;1&lt;/meta&gt;
 *    &lt;meta name="jira.permission.comment.group"&gt;${pkey}-bizusers&lt;/meta&gt;
 *    &lt;meta name="jira.permission.comment.user"&gt;qa&lt;/meta&gt;
 *    &lt;meta name="jira.permission.edit.group.1"&gt;jira-developers&lt;/meta&gt;
 *    &lt;meta name="jira.permission.edit.group.2"&gt;jira-editors&lt;/meta&gt;
 *    &lt;meta name="jira.permission.edit.projectrole">10001&lt;/meta>
 * </pre>
 * then only members of the project's bizusers group and user 'qa' will be able to comment on open issues, and only members of
 * 'jira-developers' and 'jira-editors' groups or members of the project role with id '10001' will be able to edit issues.
 * Assuming, of course, these users already have the relevant permission in the permission scheme.<p>
 * Meta attributes can also modify subtasks' permissions. For example if the 'Bug' workflow's Open step has:
 * <pre>
 *             &lt;meta name="jira.permission.subtasks.edit.group"&gt;jira-qa&lt;/meta&gt;
 * </pre>
 * Then subtasks of Bugs will only be editable by 'jira-qa' members, when their parent is in the Open state.<p>
 * The format is 'jira.permission.[subtasks.]{permission}.{type}[.suffix]', where:
 * <ul>
 * <li>{permission} is a short name specified in {@link Permissions}
 * <li>{type} is a type (group, user, assignee, reporter, lead, userCF, projectrole)
 * of permission granted, or <tt>denied</tt> to deny the permission.
 * <li><tt>subtasks.</tt>, if specified, indicates that the permission
 * applies to the subtasks of issues in this step.
 * <p/>
 * <b>Important:</b>Workflow permissions can only <i>restrict</i>
 * permissions set in the permission scheme, not grant permissions.
 *
 * @see com.atlassian.jira.permission.WorkflowBasedPermissionSchemeManager
 */
public class WorkflowBasedPermissionManager extends DefaultPermissionManager
{
    private static final Logger log = Logger.getLogger(WorkflowBasedPermissionManager.class);

    private final WorkflowPermissionFactory workflowPermissionFactory;
    private final PermissionContextFactory permissionContextFactory;

    public WorkflowBasedPermissionManager(final WorkflowPermissionFactory workflowPermissionFactory, final PermissionContextFactory permissionContextFactory)
    {
        this.workflowPermissionFactory = workflowPermissionFactory;
        this.permissionContextFactory = permissionContextFactory;
    }

    @Override
    @edu.umd.cs.findbugs.annotations.SuppressWarnings(value="NM_WRONG_PACKAGE", justification="OSUser is deprecated and dying anyway. Plus the method in question is final so we can't override it.")
    public boolean hasPermission(final int permissionId, final GenericValue entity, final User u, final boolean issueCreation)
    {
        final boolean permSchemeAllows = super.hasPermission(permissionId, entity, u, issueCreation);
        final String permName = Permissions.getShortName(permissionId);
        if (permSchemeAllows)
        {
            if ("Issue".equals(entity.getEntityName()))
            {
                final Issue issue = ComponentAccessor.getIssueFactory().getIssue(entity);

                final List<WorkflowPermission> workflowPerms = workflowPermissionFactory.getWorkflowPermissions(
                    permissionContextFactory.getPermissionContext(issue), permissionId, false);

                // add parent workflow permissions if issue is a sub-task
                addParentPermissionsIfSubTask(workflowPerms, issue, permissionId);

                if (!workflowPerms.isEmpty())
                {
                    for (final WorkflowPermission permission : workflowPerms)
                    {
                        if (permission.allows(permissionId, issue, u))
                        {
                            if (log.isInfoEnabled())
                            {
                                log.info(permName + " granted by permission scheme and " + permission);
                            }
                            return true;
                        }
                        if (log.isInfoEnabled())
                        {
                            log.info("\t" + permName + " not granted by " + permission);
                        }
                    }
                    if (log.isInfoEnabled())
                    {
                        log.info(permName + " granted by permission scheme but DENIED by workflow");
                    }
                    return false;
                }
                else
                {
                    if (log.isDebugEnabled())
                    {
                        log.debug(permName + " granted by permission scheme");
                    }
                    return true;
                }
            }
            else
            {
                if (log.isDebugEnabled())
                {
                    log.debug(permName + " permission granted by permission scheme");
                }
                return true;
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug(permName + " permission denied by permission scheme");
            }
            return false;
        }
    }

    /**
     * Adds parent's workflow permissions if sub-tasks are enabled and the given issue is a sub-task
     *
     * @param workflowPerms list of workflow permissions to add parent's into
     * @param issue         issue to check for being a sub-task
     * @param permissionId  permission id
     */
    private void addParentPermissionsIfSubTask(final List<WorkflowPermission> workflowPerms, final Issue issue, final int permissionId)
    {
        final SubTaskManager subTaskManager = ComponentManager.getInstance().getSubTaskManager();
        final Issue parent = issue.getParentObject();
        if (subTaskManager.isSubTasksEnabled() && (parent != null))
        {
            final PermissionContext parentPermissionContext = permissionContextFactory.getPermissionContext(parent);
            workflowPerms.addAll(workflowPermissionFactory.getWorkflowPermissions(parentPermissionContext, permissionId, true));
        }
    }

}
