package com.atlassian.jira.jelly.tag.projectroles;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.bc.projectroles.ProjectRoleService;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.security.roles.ProjectRole;
import com.atlassian.jira.util.SimpleErrorCollection;
import org.apache.commons.jelly.JellyTagException;
import org.apache.commons.jelly.MissingAttributeException;
import org.apache.commons.jelly.XMLOutput;

import java.util.Collection;
import java.util.Iterator;

/**
 * Jelly tag for {@link com.atlassian.jira.bc.projectroles.ProjectRoleService#removeActorsFromProjectRole(com.opensymphony.user.User, java.util.Collection, com.atlassian.jira.security.roles.ProjectRole, com.atlassian.jira.project.Project, String, com.atlassian.jira.util.ErrorCollection)}
 */
public class RemoveActorsFromProjectRole extends ProjectRoleTagSupport
{
    public void doTag(XMLOutput xmlOutput) throws MissingAttributeException, JellyTagException
    {
        SimpleErrorCollection errorCollection = new SimpleErrorCollection();
        ProjectRoleService projectRoleService = (ProjectRoleService) ComponentManager.getComponentInstanceOfType(ProjectRoleService.class);

        // get project by key
        Project project = getProject();

        // specify the actor type
        String actorType = (String) getProperties().get(KEY_PROJECTROLE_ACTOR_TYPE);

        boolean actorsProvided = actorsProvided();
        if (project == null && actorsProvided)
        {
            Collection actors = getActors();
            for (Iterator iterator = actors.iterator(); iterator.hasNext();)
            {
                String actorName =  (String) iterator.next();
                projectRoleService.removeAllRoleActorsByNameAndType(getUser(), actorName, actorType, errorCollection);
            }
        }
        else if (project != null && actorsProvided)
        {
            ProjectRole projectRole = getProjectRole(errorCollection);
            projectRoleService.removeActorsFromProjectRole(getUser(), getActors(), projectRole, project, actorType, errorCollection);
        }
        else
        {
            projectRoleService.removeAllRoleActorsByProject(getUser(), project, errorCollection);
        }

        // CHECK IF ANY ERROR WERE CAUSED BY THE SERVICE - this must be checked
        if (errorCollection.hasAnyErrors())
        {
            throw new JellyTagException(errorCollection.toString());
        }

        invokeBody(xmlOutput);
    }

    private boolean actorsProvided()
    {

        Collection actors = getActors();
        return actors != null && !actors.isEmpty();
    }
}
