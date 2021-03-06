/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */

package com.atlassian.jira.acceptance.jira.security;

import com.atlassian.core.ofbiz.test.UtilsForTests;
import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.crowd.exception.InvalidCredentialException;
import com.atlassian.crowd.exception.InvalidUserException;
import com.atlassian.crowd.exception.OperationNotPermittedException;
import com.atlassian.crowd.exception.embedded.InvalidGroupException;
import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.permission.PermissionSchemeManager;
import com.atlassian.jira.scheme.SchemeEntity;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.user.MockGroup;
import com.atlassian.jira.user.MockUser;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;
import com.atlassian.jira.local.AbstractUsersIndexingTestCase;

import java.util.Collection;

public class TestPermissions extends AbstractUsersIndexingTestCase
{
    public TestPermissions(String s)
    {
        super(s);
    }

    /**
     * Test that if i user the PermissionsManager when it has a scheme that i can retrieve the
     * permission and that when i remove it it is removed
     */
    public void testSchemePermissionNotStoredInGlobalPermissionsCache()
            throws GenericEntityException, OperationNotPermittedException, InvalidUserException, InvalidCredentialException, InvalidGroupException
    {
        String groupName = "agroup";
        User u = createMockUser("misc-user");
        Group g = createMockGroup(groupName);
        addUserToGroup(u, g);



        //Create a project
        GenericValue project = UtilsForTests.getTestEntity("Project", EasyMap.build("name", "Project"));

        //Create a scheme
        PermissionSchemeManager permissionSchemeManager = ManagerFactory.getPermissionSchemeManager();
        GenericValue defaultScheme = permissionSchemeManager.createDefaultScheme();
        permissionSchemeManager.addDefaultSchemeToProject(project);

        //Create a browse perimssion for that project
        SchemeEntity schemePermission = new SchemeEntity("group", groupName, (long) Permissions.BROWSE);
        GenericValue schemeEntity = permissionSchemeManager.createSchemeEntity(defaultScheme, schemePermission);

        //Ask PermissionManager if i has a project that i can view (should have one)
        PermissionManager permissionManager = ManagerFactory.getPermissionManager();
        Collection projects = permissionManager.getProjects(Permissions.BROWSE, u);
        assertEquals(1, projects.size());

        //Remove the browse permissions
        permissionSchemeManager.deleteEntity(schemeEntity.getLong("id"));

        //Ask PermissionManager if i has a project that i can view (shouldn't have one)
        projects = permissionManager.getProjects(Permissions.BROWSE, u);
        assertTrue(projects.isEmpty());
    }
}
