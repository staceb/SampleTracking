package com.atlassian.jira.security.roles;

/**
 * This is an abstraction that allows us to associate users with ProjectRoles for a project. Implementations can make
 * this association indirectly like a GroupActor or more directly via something like a UserActor.
 * <p>
 * Please note: implementations <strong>must be immutable</strong> as caching presumes this.
 */
public interface ProjectRoleActor extends RoleActor
{

    /**
     * Gets the project this is associated with.
     * @return project associated with
     */
    public Long getProjectId();
}
