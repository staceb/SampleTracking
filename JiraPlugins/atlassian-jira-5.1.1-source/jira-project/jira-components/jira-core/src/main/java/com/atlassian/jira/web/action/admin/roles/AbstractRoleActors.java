package com.atlassian.jira.web.action.admin.roles;

import com.atlassian.jira.bc.projectroles.ProjectRoleService;
import com.atlassian.jira.plugin.roles.ProjectRoleActorModuleDescriptor;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.plugin.PluginAccessor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: detkin Date: Jun 2, 2006 Time: 12:29:26 PM To change this template use File |
 * Settings | File Templates.
 */
public class AbstractRoleActors extends JiraWebActionSupport
{
    protected ProjectRoleService projectRoleService;
    protected PluginAccessor pluginAccessor;
    private Long projectRoleId;

    public AbstractRoleActors(ProjectRoleService projectRoleService, PluginAccessor pluginAccessor)
    {
        this.projectRoleService = projectRoleService;
        this.pluginAccessor = pluginAccessor;
    }

    public Collection getRoles()
    {
        return projectRoleService.getProjectRoles(getLoggedInUser(), this);
    }

    public ProjectRole getProjectRole()
    {
        return projectRoleService.getProjectRole(getLoggedInUser(), projectRoleId, this);
    }

    public void setProjectRoleId(Long projectRoleId)
    {
        this.projectRoleId = projectRoleId;
    }

    public void setId(Long projectRoleId)
    {
        this.projectRoleId = projectRoleId;
    }

    public Long getProjectRoleId()
    {
        return projectRoleId;
    }

    public List getRoleActorTypes()
    {
        List/*<ProjectRoleActorModuleDescriptor>*/ roleActorTypes = new ArrayList();
        List descriptors = pluginAccessor.getEnabledModuleDescriptorsByClass(ProjectRoleActorModuleDescriptor.class);
        for (Iterator iterator = descriptors.iterator(); iterator.hasNext();)
        {
            ProjectRoleActorModuleDescriptor projectRoleModuleDescriptor = (ProjectRoleActorModuleDescriptor) iterator.next();
            roleActorTypes.add(projectRoleModuleDescriptor);
        }
        return roleActorTypes;
    }

    public String getConfigurationUrl(String roleActorType)
    {
        List descriptors = pluginAccessor.getEnabledModuleDescriptorsByClass(ProjectRoleActorModuleDescriptor.class);
        for (Iterator iterator = descriptors.iterator(); iterator.hasNext();)
        {
            ProjectRoleActorModuleDescriptor projectRoleModuleDescriptor = (ProjectRoleActorModuleDescriptor) iterator.next();
            String type = projectRoleModuleDescriptor.getType();
            if (type.equals(roleActorType))
            {
                return projectRoleModuleDescriptor.getConfigurationUrl();
            }
        }
        return null;
    }

    public int getTableWidthForRoleActorTypes(int percentToDivide)
    {
        int size = getRoleActorTypes().size();
        if (size > 0)
        {
            return percentToDivide/size;

        }
        return percentToDivide;
    }
}
