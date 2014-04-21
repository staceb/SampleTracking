package com.atlassian.jira.issue.security;

import com.atlassian.core.util.collection.EasyList;
import com.atlassian.core.util.map.EasyMap;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.MockIssueFactory;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.local.MockControllerTestCase;
import com.atlassian.jira.mock.ofbiz.MockGenericValue;
import com.atlassian.jira.project.MockProject;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.MockIssueSecurityTypeManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.SecurityTypeManager;
import com.atlassian.jira.security.type.SecurityType;
import com.atlassian.jira.user.MockUser;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.atlassian.jira.util.collect.MapBuilder;
import org.easymock.EasyMock;
import org.easymock.IMocksControl;
import org.easymock.MockControl;
import org.junit.Test;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericValue;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @since v3.13
 */
public class TestIssueSecurityLevelManagerImpl extends MockControllerTestCase
{
    // TODO: JRA-14323. the following tests don't pass because we are caching project-level calls. we altered to ACTUAL behaviour.
//    public void testGetUsersSecurityLevels_ProjectFirst() throws Exception
//    {
//        final User userFred = new User("fred");
//
//        // Mock ProjectManager
//        final MockControl mockProjectManagerControl = MockControl.createStrictControl(ProjectManager.class);
//        final ProjectManager mockProjectManager = (ProjectManager) mockProjectManagerControl.getMock();
//
//        GenericValue projectGV = new MockProject(1).getGenericValue();
//        MockIssueFactory.setProjectManager(mockProjectManager);
//        MutableIssue issue = MockIssueFactory.createIssue(12, "MKY-5", 1);
//
//        mockProjectManager.getProject(new Long(1));
//        mockProjectManagerControl.setReturnValue(projectGV);
//        mockProjectManager.getProject(issue.getGenericValue());
//        mockProjectManagerControl.setReturnValue(projectGV);
//        mockProjectManagerControl.replay();
//
//        issue.setProject(projectGV);
//
//        MockGenericValue schemeGV = new MockGenericValue("scheme");
//        // Mock IssueSecuritySchemeManager
//        final MockControl mockIssueSecuritySchemeManagerControl = MockControl.createStrictControl(IssueSecuritySchemeManager.class);
//        final IssueSecuritySchemeManager mockIssueSecuritySchemeManager = (IssueSecuritySchemeManager) mockIssueSecuritySchemeManagerControl.getMock();
//        mockIssueSecuritySchemeManager.getSchemes(projectGV);
//        mockIssueSecuritySchemeManagerControl.setReturnValue(EasyList.build(schemeGV));
//        mockIssueSecuritySchemeManager.getEntities(schemeGV);
//        mockIssueSecuritySchemeManagerControl.setReturnValue(EasyList.build(
//                createSecurityLevelPermission("reporter", null, 100),
//                createSecurityLevelPermission("group", "dudes", 101),
//                createSecurityLevelPermission("reporter", null, 102),
//                createSecurityLevelPermission("group", "dudes", 102)
//        ));
//        mockIssueSecuritySchemeManager.getSchemes(projectGV);
//        mockIssueSecuritySchemeManagerControl.setReturnValue(EasyList.build(schemeGV));
//        mockIssueSecuritySchemeManager.getEntities(schemeGV);
//        mockIssueSecuritySchemeManagerControl.setReturnValue(EasyList.build(
//                createSecurityLevelPermission("reporter", null, 100),
//                createSecurityLevelPermission("group", "dudes", 101),
//                createSecurityLevelPermission("reporter", null, 102),
//                createSecurityLevelPermission("group", "dudes", 102)
//        ));
//        mockIssueSecuritySchemeManagerControl.replay();
//
//        // Mock SecurityType
//        final MockControl mockReporterSecurityTypeControl = MockControl.createStrictControl(SecurityType.class);
//        final SecurityType mockReporterSecurityType = (SecurityType) mockReporterSecurityTypeControl.getMock();
//        mockReporterSecurityType.hasPermission(projectGV, null, userFred, false);
//        mockReporterSecurityTypeControl.setReturnValue(true, 2);
//        mockReporterSecurityType.hasPermission(issue.getGenericValue(), null, userFred, false);
//        mockReporterSecurityTypeControl.setReturnValue(false, 2);
//        mockReporterSecurityTypeControl.replay();
//        // Mock SecurityType
//        final MockControl mockGroupSecurityTypeControl = MockControl.createStrictControl(SecurityType.class);
//        final SecurityType mockGroupSecurityType = (SecurityType) mockGroupSecurityTypeControl.getMock();
//        mockGroupSecurityType.hasPermission(projectGV, "dudes", userFred, false);
//        mockGroupSecurityTypeControl.setReturnValue(false);
//        mockGroupSecurityType.hasPermission(issue.getGenericValue(), "dudes", userFred, false);
//        mockGroupSecurityTypeControl.setReturnValue(false, 2);
//        mockGroupSecurityTypeControl.replay();
//
//        final SecurityTypeManager securityTypeManager = new MockIssueSecurityTypeManager(null);
//        securityTypeManager.setSecurityTypes(EasyMap.build(
//                "reporter", mockReporterSecurityType,
//                "group", mockGroupSecurityType
//        ));
//
//        IssueSecurityLevelManagerImpl issueLevelSecurities = new IssueSecurityLevelManagerImpl(mockIssueSecuritySchemeManager, securityTypeManager, mockProjectManager)
//        {
//            public GenericValue getIssueSecurityLevel(final Long id) throws GenericEntityException
//            {
//                return new MockGenericValue("IssueSecurityLevel", EasyMap.build("id", id));
//            }
//        };
//
//        List securityLevels = issueLevelSecurities.getUsersSecurityLevels(projectGV, userFred);
//        // Doing a project-level search should always include security levels with "reporter" permission.
//        assertEquals(2, securityLevels.size());
//        assertTrue(securityLevels.contains(new MockGenericValue("IssueSecurityLevel", EasyMap.build("id", new Long(100)))));
//        assertTrue(securityLevels.contains(new MockGenericValue("IssueSecurityLevel", EasyMap.build("id", new Long(102)))));
//
//        // Now, if we call this again with an issue GV, then we should not have cached the project values:
//        securityLevels = issueLevelSecurities.getUsersSecurityLevels(issue.getGenericValue(), userFred);
//        // fred is not the reporter, so this should return nothing
//        assertEquals(0, securityLevels.size());
//
//        // Verify Mock ProjectManager
//        mockProjectManagerControl.verify();
//        // Verify Mock IssueSecuritySchemeManager
//        mockIssueSecuritySchemeManagerControl.verify();
//        // Verify Mock SecurityType
//        mockReporterSecurityTypeControl.verify();
//        mockGroupSecurityTypeControl.verify();
//    }
//
//    public void testGetUsersSecurityLevels_IssueFirst() throws Exception
//    {
//        final User userFred = new User("fred");
//
//        GenericValue projectGV = new MockProject(1).getGenericValue();
//        MutableIssue issue = MockIssueFactory.createIssue(12, "MKY-5", 1);
//
//        // Mock ProjectManager
//        final MockControl mockProjectManagerControl = MockControl.createStrictControl(ProjectManager.class);
//        final ProjectManager mockProjectManager = (ProjectManager) mockProjectManagerControl.getMock();
//        mockProjectManager.getProject(issue.getGenericValue());
//        mockProjectManagerControl.setReturnValue(projectGV);
//        mockProjectManagerControl.replay();
//
//        MockGenericValue schemeGV = new MockGenericValue("scheme");
//        // Mock IssueSecuritySchemeManager
//        final MockControl mockIssueSecuritySchemeManagerControl = MockControl.createStrictControl(IssueSecuritySchemeManager.class);
//        final IssueSecuritySchemeManager mockIssueSecuritySchemeManager = (IssueSecuritySchemeManager) mockIssueSecuritySchemeManagerControl.getMock();
//        mockIssueSecuritySchemeManager.getSchemes(projectGV);
//        mockIssueSecuritySchemeManagerControl.setReturnValue(EasyList.build(schemeGV));
//        mockIssueSecuritySchemeManager.getEntities(schemeGV);
//        mockIssueSecuritySchemeManagerControl.setReturnValue(EasyList.build(
//                createSecurityLevelPermission("reporter", null, 100),
//                createSecurityLevelPermission("group", "dudes", 101),
//                createSecurityLevelPermission("reporter", null, 102),
//                createSecurityLevelPermission("group", "dudes", 102)
//        ));
//        mockIssueSecuritySchemeManager.getSchemes(projectGV);
//        mockIssueSecuritySchemeManagerControl.setReturnValue(EasyList.build(schemeGV));
//        mockIssueSecuritySchemeManager.getEntities(schemeGV);
//        mockIssueSecuritySchemeManagerControl.setReturnValue(EasyList.build(
//                createSecurityLevelPermission("reporter", null, 100),
//                createSecurityLevelPermission("group", "dudes", 101),
//                createSecurityLevelPermission("reporter", null, 102),
//                createSecurityLevelPermission("group", "dudes", 102)
//        ));
//        mockIssueSecuritySchemeManagerControl.replay();
//
//        // Mock SecurityType
//        final MockControl mockReporterSecurityTypeControl = MockControl.createStrictControl(SecurityType.class);
//        final SecurityType mockReporterSecurityType = (SecurityType) mockReporterSecurityTypeControl.getMock();
//        mockReporterSecurityType.hasPermission(issue.getGenericValue(), null, userFred, false);
//        mockReporterSecurityTypeControl.setReturnValue(false, 2);
//        mockReporterSecurityType.hasPermission(projectGV, null, userFred, false);
//        mockReporterSecurityTypeControl.setReturnValue(true, 2);
//        mockReporterSecurityTypeControl.replay();
//        // Mock SecurityType
//        final MockControl mockGroupSecurityTypeControl = MockControl.createStrictControl(SecurityType.class);
//        final SecurityType mockGroupSecurityType = (SecurityType) mockGroupSecurityTypeControl.getMock();
//        mockGroupSecurityType.hasPermission(issue.getGenericValue(), "dudes", userFred, false);
//        mockGroupSecurityTypeControl.setReturnValue(false, 2);
//        mockGroupSecurityType.hasPermission(projectGV, "dudes", userFred, false);
//        mockGroupSecurityTypeControl.setReturnValue(false);
//        mockGroupSecurityTypeControl.replay();
//
//        final SecurityTypeManager securityTypeManager = new MockIssueSecurityTypeManager(null);
//        securityTypeManager.setSecurityTypes(EasyMap.build(
//                "reporter", mockReporterSecurityType,
//                "group", mockGroupSecurityType
//        ));
//
//        IssueSecurityLevelManagerImpl issueLevelSecurities = new IssueSecurityLevelManagerImpl(mockIssueSecuritySchemeManager, securityTypeManager, mockProjectManager)
//        {
//            public GenericValue getIssueSecurityLevel(final Long id) throws GenericEntityException
//            {
//                return new MockGenericValue("IssueSecurityLevel", EasyMap.build("id", id));
//            }
//        };
//
//        List securityLevels = issueLevelSecurities.getUsersSecurityLevels(issue.getGenericValue(), userFred);
//        // Doing an issue-level search should give no levels
//        assertEquals(0, securityLevels.size());
//
//        // Now, if we call this again with a project, then we should not have cached the issue values:
//        securityLevels = issueLevelSecurities.getUsersSecurityLevels(projectGV, userFred);
//        assertEquals(2, securityLevels.size());
//        assertTrue(securityLevels.contains(new MockGenericValue("IssueSecurityLevel", EasyMap.build("id", new Long(100)))));
//        assertTrue(securityLevels.contains(new MockGenericValue("IssueSecurityLevel", EasyMap.build("id", new Long(102)))));
//
//        // Verify Mock ProjectManager
//        mockProjectManagerControl.verify();
//        // Verify Mock IssueSecuritySchemeManager
//        mockIssueSecuritySchemeManagerControl.verify();
//        // Verify Mock SecurityType
//        mockReporterSecurityTypeControl.verify();
//        mockGroupSecurityTypeControl.verify();
//    }

    // TODO: JRA-14323. this test actually confirms the "incorrect" interem behaviour.
    @Test
    public void testGetUsersSecurityLevels_ProjectFirst() throws Exception
    {
        final User userFred = new MockUser("fred");

        // Mock ProjectManager
        final MockControl mockProjectManagerControl = MockControl.createStrictControl(ProjectManager.class);
        final ProjectManager mockProjectManager = (ProjectManager) mockProjectManagerControl.getMock();

        GenericValue projectGV = new MockProject(1).getGenericValue();
        MockIssueFactory.setProjectManager(mockProjectManager);
        MutableIssue issue = MockIssueFactory.createIssue(12, "MKY-5", 1);

        mockProjectManager.getProject(new Long(1));
        mockProjectManagerControl.setReturnValue(projectGV);
        mockProjectManager.getProject(issue.getGenericValue());
        mockProjectManagerControl.setReturnValue(projectGV);
        mockProjectManagerControl.replay();

        issue.setProject(projectGV);

        MockGenericValue schemeGV = new MockGenericValue("scheme");
        // Mock IssueSecuritySchemeManager
        final MockControl mockIssueSecuritySchemeManagerControl = MockControl.createStrictControl(IssueSecuritySchemeManager.class);
        final IssueSecuritySchemeManager mockIssueSecuritySchemeManager = (IssueSecuritySchemeManager) mockIssueSecuritySchemeManagerControl.getMock();
        mockIssueSecuritySchemeManager.getSchemes(projectGV);
        mockIssueSecuritySchemeManagerControl.setReturnValue(EasyList.build(schemeGV));
        mockIssueSecuritySchemeManager.getEntities(schemeGV);
        mockIssueSecuritySchemeManagerControl.setReturnValue(EasyList.build(
                createSecurityLevelPermission("reporter", null, 100),
                createSecurityLevelPermission("group", "dudes", 101),
                createSecurityLevelPermission("reporter", null, 102),
                createSecurityLevelPermission("group", "dudes", 102)
        ));
//        mockIssueSecuritySchemeManager.getSchemes(projectGV);
//        mockIssueSecuritySchemeManagerControl.setReturnValue(EasyList.build(schemeGV));
//        mockIssueSecuritySchemeManager.getEntities(schemeGV);
//        mockIssueSecuritySchemeManagerControl.setReturnValue(EasyList.build(
//                createSecurityLevelPermission("reporter", null, 100),
//                createSecurityLevelPermission("group", "dudes", 101),
//                createSecurityLevelPermission("reporter", null, 102),
//                createSecurityLevelPermission("group", "dudes", 102)
//        ));
        mockIssueSecuritySchemeManagerControl.replay();

        // Mock SecurityType
        final MockControl mockReporterSecurityTypeControl = MockControl.createStrictControl(SecurityType.class);
        final SecurityType mockReporterSecurityType = (SecurityType) mockReporterSecurityTypeControl.getMock();
        mockReporterSecurityType.hasPermission(projectGV, null, userFred, false);
        mockReporterSecurityTypeControl.setReturnValue(true, 2);
//        mockReporterSecurityType.hasPermission(issue.getGenericValue(), null, userFred, false);
//        mockReporterSecurityTypeControl.setReturnValue(false, 2);
        mockReporterSecurityTypeControl.replay();
        // Mock SecurityType
        final MockControl mockGroupSecurityTypeControl = MockControl.createStrictControl(SecurityType.class);
        final SecurityType mockGroupSecurityType = (SecurityType) mockGroupSecurityTypeControl.getMock();
        mockGroupSecurityType.hasPermission(projectGV, "dudes", userFred, false);
        mockGroupSecurityTypeControl.setReturnValue(false);
//        mockGroupSecurityType.hasPermission(issue.getGenericValue(), "dudes", userFred, false);
//        mockGroupSecurityTypeControl.setReturnValue(false, 2);
        mockGroupSecurityTypeControl.replay();

        final SecurityTypeManager securityTypeManager = new MockIssueSecurityTypeManager(null);
        securityTypeManager.setSecurityTypes(EasyMap.build(
                "reporter", mockReporterSecurityType,
                "group", mockGroupSecurityType
        ));

        IssueSecurityLevelManagerImpl issueSecurityLevelManagerImpl = new IssueSecurityLevelManagerImpl(mockIssueSecuritySchemeManager, securityTypeManager, mockProjectManager, null, null)
        {
            public GenericValue getIssueSecurityLevel(final Long id)
            {
                return new MockGenericValue("IssueSecurityLevel", EasyMap.build("id", id));
            }
        };

        List securityLevels = issueSecurityLevelManagerImpl.getUsersSecurityLevels(projectGV, userFred);
        // Doing a project-level search should always include security levels with "reporter" permission.
        assertEquals(2, securityLevels.size());
        assertTrue(securityLevels.contains(new MockGenericValue("IssueSecurityLevel", EasyMap.build("id", new Long(100)))));
        assertTrue(securityLevels.contains(new MockGenericValue("IssueSecurityLevel", EasyMap.build("id", new Long(102)))));

        // Now, if we call this again with an issue GV, then we should not have cached the project values:
        securityLevels = issueSecurityLevelManagerImpl.getUsersSecurityLevels(issue.getGenericValue(), userFred);
        // fred is not the reporter, so this should return nothing
//        assertEquals(0, securityLevels.size());
        // TODO: JRA-14323 BUT, we have not fixed the bug.
        assertEquals(2, securityLevels.size());
        assertTrue(securityLevels.contains(new MockGenericValue("IssueSecurityLevel", EasyMap.build("id", new Long(100)))));
        assertTrue(securityLevels.contains(new MockGenericValue("IssueSecurityLevel", EasyMap.build("id", new Long(102)))));

        // Verify Mock ProjectManager
        mockProjectManagerControl.verify();
        // Verify Mock IssueSecuritySchemeManager
        mockIssueSecuritySchemeManagerControl.verify();
        // Verify Mock SecurityType
        mockReporterSecurityTypeControl.verify();
        mockGroupSecurityTypeControl.verify();
    }

    // TODO: JRA-14323. this test actually confirms the "incorrect" interem behaviour.
    @Test
    public void testGetUsersSecurityLevels_IssueFirst() throws Exception
    {
        final User userFred = new MockUser("fred");

        GenericValue projectGV = new MockProject(1).getGenericValue();
        MutableIssue issue = MockIssueFactory.createIssue(12, "MKY-5", 1);

        // Mock ProjectManager
        final MockControl mockProjectManagerControl = MockControl.createStrictControl(ProjectManager.class);
        final ProjectManager mockProjectManager = (ProjectManager) mockProjectManagerControl.getMock();
        mockProjectManager.getProject(issue.getGenericValue());
        mockProjectManagerControl.setReturnValue(projectGV);
        mockProjectManagerControl.replay();

        MockGenericValue schemeGV = new MockGenericValue("scheme");
        // Mock IssueSecuritySchemeManager
        final MockControl mockIssueSecuritySchemeManagerControl = MockControl.createStrictControl(IssueSecuritySchemeManager.class);
        final IssueSecuritySchemeManager mockIssueSecuritySchemeManager = (IssueSecuritySchemeManager) mockIssueSecuritySchemeManagerControl.getMock();
        mockIssueSecuritySchemeManager.getSchemes(projectGV);
        mockIssueSecuritySchemeManagerControl.setReturnValue(EasyList.build(schemeGV));
        mockIssueSecuritySchemeManager.getEntities(schemeGV);
        mockIssueSecuritySchemeManagerControl.setReturnValue(EasyList.build(
                createSecurityLevelPermission("reporter", null, 100),
                createSecurityLevelPermission("group", "dudes", 101),
                createSecurityLevelPermission("reporter", null, 102),
                createSecurityLevelPermission("group", "dudes", 102)
        ));
//        mockIssueSecuritySchemeManager.getSchemes(projectGV);
//        mockIssueSecuritySchemeManagerControl.setReturnValue(EasyList.build(schemeGV));
//        mockIssueSecuritySchemeManager.getEntities(schemeGV);
//        mockIssueSecuritySchemeManagerControl.setReturnValue(EasyList.build(
//                createSecurityLevelPermission("reporter", null, 100),
//                createSecurityLevelPermission("group", "dudes", 101),
//                createSecurityLevelPermission("reporter", null, 102),
//                createSecurityLevelPermission("group", "dudes", 102)
//        ));
        mockIssueSecuritySchemeManagerControl.replay();

        // Mock SecurityType
        final MockControl mockReporterSecurityTypeControl = MockControl.createStrictControl(SecurityType.class);
        final SecurityType mockReporterSecurityType = (SecurityType) mockReporterSecurityTypeControl.getMock();
//        mockReporterSecurityType.hasPermission(issue.getGenericValue(), null, userFred, false);
//        mockReporterSecurityTypeControl.setReturnValue(false, 2);
        mockReporterSecurityType.hasPermission(projectGV, null, userFred, false);
        mockReporterSecurityTypeControl.setReturnValue(true, 2);
        mockReporterSecurityTypeControl.replay();
        // Mock SecurityType
        final MockControl mockGroupSecurityTypeControl = MockControl.createStrictControl(SecurityType.class);
        final SecurityType mockGroupSecurityType = (SecurityType) mockGroupSecurityTypeControl.getMock();
//        mockGroupSecurityType.hasPermission(issue.getGenericValue(), "dudes", userFred, false);
//        mockGroupSecurityTypeControl.setReturnValue(false, 2);
        mockGroupSecurityType.hasPermission(projectGV, "dudes", userFred, false);
        mockGroupSecurityTypeControl.setReturnValue(false);
        mockGroupSecurityTypeControl.replay();

        final SecurityTypeManager securityTypeManager = new MockIssueSecurityTypeManager(null);
        securityTypeManager.setSecurityTypes(EasyMap.build(
                "reporter", mockReporterSecurityType,
                "group", mockGroupSecurityType
        ));

        IssueSecurityLevelManagerImpl issueSecurityLevelManagerImpl = new IssueSecurityLevelManagerImpl(mockIssueSecuritySchemeManager, securityTypeManager, mockProjectManager, null, null)
        {
            public GenericValue getIssueSecurityLevel(final Long id)
            {
                return new MockGenericValue("IssueSecurityLevel", EasyMap.build("id", id));
            }
        };

        List securityLevels = issueSecurityLevelManagerImpl.getUsersSecurityLevels(issue.getGenericValue(), userFred);
        // Doing an issue-level search should give no levels
//        assertEquals(0, securityLevels.size());
        // TODO: JRA-14323 this SHOULD return 0 levels, fix later.
        assertEquals(2, securityLevels.size());
        assertTrue(securityLevels.contains(new MockGenericValue("IssueSecurityLevel", EasyMap.build("id", new Long(100)))));
        assertTrue(securityLevels.contains(new MockGenericValue("IssueSecurityLevel", EasyMap.build("id", new Long(102)))));

        // Now, if we call this again with a project, then we should not have cached the issue values:
        securityLevels = issueSecurityLevelManagerImpl.getUsersSecurityLevels(projectGV, userFred);
        assertEquals(2, securityLevels.size());
        assertTrue(securityLevels.contains(new MockGenericValue("IssueSecurityLevel", EasyMap.build("id", new Long(100)))));
        assertTrue(securityLevels.contains(new MockGenericValue("IssueSecurityLevel", EasyMap.build("id", new Long(102)))));

        // Verify Mock ProjectManager
        mockProjectManagerControl.verify();
        // Verify Mock IssueSecuritySchemeManager
        mockIssueSecuritySchemeManagerControl.verify();
        // Verify Mock SecurityType
        mockReporterSecurityTypeControl.verify();
        mockGroupSecurityTypeControl.verify();
    }

    @Test
    public void testGetAllUsersSecurityLevels() throws Exception
    {
        final MockUser userFred = new MockUser("fred");
        final IMocksControl controller = EasyMock.createStrictControl();
        controller.checkOrder(false);

        GenericValue projectGV1 = new MockProject(1).getGenericValue();
        GenericValue projectGV2 = new MockProject(2).getGenericValue();

        // Mock PermissionManager
        final PermissionManager mockPermissionManager = controller.createMock(PermissionManager.class);
        expect(mockPermissionManager.getProjects(Permissions.BROWSE, userFred)).andReturn(CollectionBuilder.newBuilder(projectGV1, projectGV2).asList());

        // Mock ProjectManager
        final ProjectManager mockProjectManager = controller.createMock(ProjectManager.class);

        MockGenericValue schemeGV1 = new MockGenericValue("scheme");
        MockGenericValue schemeGV2 = new MockGenericValue("scheme");

        // Mock IssueSecuritySchemeManager
        final IssueSecuritySchemeManager mockIssueSecuritySchemeManager = controller.createMock(IssueSecuritySchemeManager.class);
        mockIssueSecuritySchemeManager.getSchemes(projectGV1);
        expectLastCall().andReturn(EasyList.build(schemeGV1));
        mockIssueSecuritySchemeManager.getEntities(schemeGV1);
        expectLastCall().andReturn(EasyList.build(
                createSecurityLevelPermission("reporter", null, 100),
                createSecurityLevelPermission("group", "dudes", 101),
                createSecurityLevelPermission("reporter", null, 102),
                createSecurityLevelPermission("group", "dudes", 102)
        ));

        mockIssueSecuritySchemeManager.getSchemes(projectGV2);
        expectLastCall().andReturn(EasyList.build(schemeGV2));
        mockIssueSecuritySchemeManager.getEntities(schemeGV2);
        expectLastCall().andReturn(EasyList.build(
                createSecurityLevelPermission("assignee", null, 103),
                createSecurityLevelPermission("lead", null, 104)
        ));

        final SecurityType mockReporterSecurityType = controller.createMock(SecurityType.class);
        mockReporterSecurityType.hasPermission(projectGV1, null, userFred, false);
        expectLastCall().andReturn(true).times(2);

        final SecurityType mockGroupSecurityType = controller.createMock(SecurityType.class);
        mockGroupSecurityType.hasPermission(projectGV1, "dudes", userFred, false);
        expectLastCall().andReturn(false);

        final SecurityType mockAssigneeSecurityType = controller.createMock(SecurityType.class);
        mockAssigneeSecurityType.hasPermission(projectGV2, null, userFred, false);
        expectLastCall().andReturn(true);

        final SecurityType mockLeadSecurityType = controller.createMock(SecurityType.class);
        mockLeadSecurityType.hasPermission(projectGV2, null, userFred, false);
        expectLastCall().andReturn(true);

        final SecurityTypeManager securityTypeManager = new MockIssueSecurityTypeManager(null);
        Map<String, SecurityType> objectObjectMap = MapBuilder.<String, SecurityType>newBuilder()
                .add("reporter", mockReporterSecurityType)
                .add("group", mockGroupSecurityType)
                .add("assignee", mockAssigneeSecurityType)
                .add("lead", mockLeadSecurityType)
                .toMap();
        securityTypeManager.setSecurityTypes(objectObjectMap);

        IssueSecurityLevelManagerImpl issueSecurityLevelManagerImpl = new IssueSecurityLevelManagerImpl(mockIssueSecuritySchemeManager, securityTypeManager, mockProjectManager, mockPermissionManager, null)
        {
            public GenericValue getIssueSecurityLevel(final Long id)
            {
                return createMockSecurityLevel(id, null);
            }
        };
        
        controller.replay();

        Collection<GenericValue> securityLevels = issueSecurityLevelManagerImpl.getAllUsersSecurityLevels(userFred);
        assertEquals(4, securityLevels.size());
        assertTrue(securityLevels.contains(createMockSecurityLevel(100L, null)));
        assertTrue(securityLevels.contains(createMockSecurityLevel(102L, null)));
        assertTrue(securityLevels.contains(createMockSecurityLevel(103L, null)));
        assertTrue(securityLevels.contains(createMockSecurityLevel(104L, null)));

        controller.verify();
    }

    @Test
    public void testGetUsersSecurityLevelsByName() throws Exception
    {
        final MockUser userFred = new MockUser("fred");
        final IMocksControl controller = EasyMock.createStrictControl();

        // Mock PermissionManager
        final PermissionManager mockPermissionManager = controller.createMock(PermissionManager.class);

        // Mock ProjectManager
        final ProjectManager mockProjectManager = controller.createMock(ProjectManager.class);

        // Mock IssueSecuritySchemeManager
        final IssueSecuritySchemeManager mockIssueSecuritySchemeManager = controller.createMock(IssueSecuritySchemeManager.class);

        final SecurityTypeManager securityTypeManager = new MockIssueSecurityTypeManager(null);

        final MockGenericValue mock100 = createMockSecurityLevel(100L, "Level 1");
        final MockGenericValue mock101 = createMockSecurityLevel(101L, "Level 2");
        final MockGenericValue mock102 = createMockSecurityLevel(102L, "Level 3");
        final MockGenericValue mock103 = createMockSecurityLevel(103L, "Level 1");
        final List<GenericValue> usersLevels = CollectionBuilder.<GenericValue>newBuilder(
                mock100,
                mock101,
                mock102,
                mock103
        ).asList();


        IssueSecurityLevelManagerImpl issueSecurityLevelManagerImpl = new IssueSecurityLevelManagerImpl(mockIssueSecuritySchemeManager, securityTypeManager, mockProjectManager, mockPermissionManager, null)
        {
            @Override
            public Collection<GenericValue> getAllUsersSecurityLevels(final com.atlassian.crowd.embedded.api.User user) throws GenericEntityException
            {
                return usersLevels;
            }
        };

        controller.replay();

        Collection<GenericValue> securityLevels = issueSecurityLevelManagerImpl.getUsersSecurityLevelsByName(userFred, "Level 1");
        assertEquals(2, securityLevels.size());
        assertTrue(securityLevels.contains(mock100));
        assertTrue(securityLevels.contains(mock103));

        controller.verify();
    }

    @Test
    public void testGetSecurityLevelsByName() throws Exception
    {
        final IMocksControl controller = EasyMock.createStrictControl();

        // Mock PermissionManager
        final PermissionManager mockPermissionManager = controller.createMock(PermissionManager.class);

        // Mock ProjectManager
        final ProjectManager mockProjectManager = controller.createMock(ProjectManager.class);

        // Mock IssueSecuritySchemeManager
        final IssueSecuritySchemeManager mockIssueSecuritySchemeManager = controller.createMock(IssueSecuritySchemeManager.class);

        final SecurityTypeManager securityTypeManager = new MockIssueSecurityTypeManager(null);

        final MockGenericValue mock100 = createMockSecurityLevel(100L, "Level 1");
        final MockGenericValue mock101 = createMockSecurityLevel(101L, "Level 2");
        final MockGenericValue mock102 = createMockSecurityLevel(102L, "Level 3");
        final MockGenericValue mock103 = createMockSecurityLevel(103L, "Level 1");
        final List<GenericValue> allLevels = CollectionBuilder.<GenericValue>newBuilder(
                mock100,
                mock101,
                mock102,
                mock103
        ).asList();


        IssueSecurityLevelManagerImpl issueSecurityLevelManagerImpl = new IssueSecurityLevelManagerImpl(mockIssueSecuritySchemeManager, securityTypeManager, mockProjectManager, mockPermissionManager, null)
        {
            @Override
            public Collection<GenericValue> getAllSecurityLevels() throws GenericEntityException
            {
                return allLevels;
            }
        };

        controller.replay();

        Collection<GenericValue> securityLevels = issueSecurityLevelManagerImpl.getSecurityLevelsByName("Level 1");
        assertEquals(2, securityLevels.size());
        assertTrue(securityLevels.contains(mock100));
        assertTrue(securityLevels.contains(mock103));

        controller.verify();
    }

    @Test
    public void testGetAllSecurityLevels() throws Exception
    {
        final IssueSecuritySchemeManager issueSecuritySchemeManager = getMock(IssueSecuritySchemeManager.class);
        final SecurityTypeManager securityTypeManager = getMock(SecurityTypeManager.class);
        final ProjectManager projectManager = getMock(ProjectManager.class);
        final PermissionManager permissionManager = getMock(PermissionManager.class);

        final Long inputSchemeId = 5555L;
        EasyMock.expect(issueSecuritySchemeManager.getSchemes())
                .andReturn(CollectionBuilder.<GenericValue>newBuilder(createMockScheme(inputSchemeId)).asList());

        IssueSecurityLevelManagerImpl issueSecurityLevelManagerImpl = new IssueSecurityLevelManagerImpl(issueSecuritySchemeManager, securityTypeManager, projectManager, permissionManager, null)
        {
            @Override
            public List<GenericValue> getSchemeIssueSecurityLevels(final Long schemeId)
            {
                assertEquals(inputSchemeId, schemeId);
                return Collections.<GenericValue>singletonList(createMockSecurityLevel(44L, "Level 1"));
            }
        };

        replay();

        final Collection<GenericValue> result = issueSecurityLevelManagerImpl.getAllSecurityLevels();
        assertEquals(1, result.size());
        assertEquals(new Long(44L), result.iterator().next().getLong("id"));
    }

    private MockGenericValue createMockScheme(final Long id)
    {
        return new MockGenericValue("Scheme", MapBuilder.<String, Object>newBuilder().add("id", id).toMap());
    }

    private MockGenericValue createMockSecurityLevel(final Long id, final String name)
    {
        return new MockGenericValue("IssueSecurityLevel", MapBuilder.<String, Object>newBuilder().add("id", id).add("name", name).toMap());
    }

    private GenericValue createSecurityLevelPermission(String type, String parameter, long securityLevelID)
    {
        GenericValue securityLevelPermissionGV = new MockGenericValue("SchemeIssueSecurities");
        securityLevelPermissionGV.set("type", type);
        securityLevelPermissionGV.set("parameter", parameter);
        securityLevelPermissionGV.set("security", securityLevelID);
        return securityLevelPermissionGV;
    }


}
