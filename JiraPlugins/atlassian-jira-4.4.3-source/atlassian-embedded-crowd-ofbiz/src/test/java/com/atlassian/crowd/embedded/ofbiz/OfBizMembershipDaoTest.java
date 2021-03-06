package com.atlassian.crowd.embedded.ofbiz;

import com.atlassian.crowd.embedded.api.PasswordCredential;
import com.atlassian.crowd.embedded.testing.TestData;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.crowd.model.directory.DirectoryImpl;
import com.atlassian.crowd.model.group.GroupType;
import com.atlassian.crowd.search.EntityDescriptor;
import com.atlassian.crowd.search.query.membership.MembershipQuery;
import com.atlassian.event.api.EventPublisher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.List;

import static com.atlassian.crowd.embedded.testing.TestData.DIRECTORY_ID;
import static com.atlassian.crowd.embedded.testing.TestData.Group;
import static com.atlassian.crowd.embedded.testing.TestData.User;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OfBizMembershipDaoTest extends AbstractTransactionalOfBizTestCase
{
    private OfBizUserDao userDao;
    private OfBizGroupDao groupDao;
    private OfBizDelegatingMembershipDao membershipDao;
    private InternalMembershipDao internalmembershipDao;
    private OfBizDirectoryDao directoryDao;

    @Before
    public void setUp() throws Exception
    {
        final EventPublisher eventPublisher = new MockEventPublisher();
        directoryDao = new OfBizDirectoryDao(getGenericDelegator(), eventPublisher);
        internalmembershipDao = new OfBizInternalMembershipDao(getGenericDelegator(), directoryDao, eventPublisher);
        userDao = new OfBizUserDao(getGenericDelegator(), directoryDao, internalmembershipDao, eventPublisher);
        groupDao = new OfBizGroupDao(getGenericDelegator(), directoryDao, internalmembershipDao, eventPublisher);
        membershipDao = new OfBizDelegatingMembershipDao(internalmembershipDao, userDao, groupDao);
    }

    @After
    public void tearDown() throws Exception
    {
        membershipDao = null;
        userDao = null;
        groupDao = null;
        directoryDao = null;
        internalmembershipDao = null;
    }

    @Test
    public void testAddUserToGroupAndCheckIsUserDirectMemberAndRemoveUserFromGroup() throws Exception
    {
        userDao.add(User.getTestData(), User.CREDENTIAL);
        groupDao.add(Group.getTestData());

        membershipDao.addUserToGroup(DIRECTORY_ID, User.NAME, Group.NAME);

        assertTrue(membershipDao.isUserDirectMember(DIRECTORY_ID, User.NAME, Group.NAME));

        membershipDao.removeUserFromGroup(DIRECTORY_ID, User.NAME, Group.NAME);

        assertFalse(membershipDao.isUserDirectMember(DIRECTORY_ID, User.NAME, Group.NAME));
    }

    @Test
    public void testAddGroupToGroupAndCheckIsGroupDirectMemberAndRemoveGroupFromGroup() throws Exception
    {
        final String childName = "child-group";
        final String description = "child group description";

        groupDao.add(Group.getTestData());
        groupDao.add(Group.getGroup(childName, DIRECTORY_ID, true, description, GroupType.GROUP));

        membershipDao.addGroupToGroup(DIRECTORY_ID, childName, Group.NAME);

        assertTrue(membershipDao.isGroupDirectMember(DIRECTORY_ID, childName, Group.NAME));

        membershipDao.removeGroupFromGroup(DIRECTORY_ID, childName, Group.NAME);

        assertFalse(membershipDao.isGroupDirectMember(DIRECTORY_ID, childName, Group.NAME));
    }

    @Test
    public void testRemoveUserWithMemberships() throws Exception
    {
        // This test that if we remove a member and add him again then the groups don't magically reappear.
        // JRA-25611
        DirectoryImpl directory = directoryDao.add(TestData.Directory.getTestData());
        com.atlassian.crowd.model.group.Group group = groupDao.add(Group.getTestData(directory.getId()));
        com.atlassian.crowd.model.user.User user = userDao.add(User.getTestData(directory.getId()), User.CREDENTIAL);
        membershipDao.addUserToGroup(directory.getId(), user.getName(), group.getName());
        assertTrue(membershipDao.isUserDirectMember(directory.getId(), user.getName(), group.getName()));

        userDao.remove(user);
        internalmembershipDao.flushCache();
        user = userDao.add(TestData.User.getTestData(directory.getId()), TestData.User.CREDENTIAL);

        assertFalse(membershipDao.isUserDirectMember(directory.getId(), user.getName(), group.getName()));
    }

    @Test
    public void testSearchUserNamesInGroup() throws Exception
    {
        final String userName2 = "user2";

        userDao.add(User.getTestData(), User.CREDENTIAL);
        userDao.add(User.getUser(userName2, DIRECTORY_ID, true, "f", "l", "d", "e"), PasswordCredential.encrypted("password"));

        groupDao.add(Group.getTestData());

        membershipDao.addUserToGroup(DIRECTORY_ID, User.NAME, Group.NAME);
        membershipDao.addUserToGroup(DIRECTORY_ID, userName2, Group.NAME);


        final MembershipQuery<String> query = mock(MembershipQuery.class);
        when(query.getReturnType()).thenReturn(String.class);
        when(query.isFindChildren()).thenReturn(true);
        when(query.getEntityNameToMatch()).thenReturn(Group.NAME);
        when(query.getEntityToMatch()).thenReturn(EntityDescriptor.group());
        when(query.getEntityToReturn()).thenReturn(EntityDescriptor.user());
        when(query.getStartIndex()).thenReturn(0);
        when(query.getMaxResults()).thenReturn(-1);

        final List<String> userNames = membershipDao.search(DIRECTORY_ID, query);
        assertEquals(2, userNames.size());
        assertTrue(userNames.contains(User.NAME));
        assertTrue(userNames.contains(userName2));
    }
    
    @Test
    public void testSearchGroupNamesOfUser() throws Exception
    {
        final String groupName2 = "group2";

        userDao.add(User.getTestData(), User.CREDENTIAL);

        groupDao.add(Group.getTestData());
        groupDao.add(Group.getGroup(groupName2, DIRECTORY_ID, true, "d", GroupType.GROUP));

        membershipDao.addUserToGroup(DIRECTORY_ID, User.NAME, Group.NAME);
        membershipDao.addUserToGroup(DIRECTORY_ID, User.NAME, groupName2);


        final MembershipQuery<String> query = mock(MembershipQuery.class);
        when(query.getReturnType()).thenReturn(String.class);
        when(query.isFindChildren()).thenReturn(false);
        when(query.getEntityNameToMatch()).thenReturn(User.NAME);
        when(query.getEntityToMatch()).thenReturn(EntityDescriptor.user());
        when(query.getEntityToReturn()).thenReturn(EntityDescriptor.group());
        when(query.getStartIndex()).thenReturn(0);
        when(query.getMaxResults()).thenReturn(-1);

        final List<String> groupNames = membershipDao.search(DIRECTORY_ID, query);
        assertEquals(2, groupNames.size());
        assertTrue(groupNames.contains(Group.NAME));
        assertTrue(groupNames.contains(groupName2));
    }

    @Test
    public void testNestedGroups() throws Exception
    {
        final String groupName2 = "group2";
        final String groupName3 = "group3";
        final String groupName4 = "group4";

        userDao.add(User.getTestData(), User.CREDENTIAL);

        groupDao.add(Group.getTestData());
        com.atlassian.crowd.model.group.Group group2 = groupDao.add(Group.getGroup(groupName2, DIRECTORY_ID, true, "d2", GroupType.GROUP));
        com.atlassian.crowd.model.group.Group group3 = groupDao.add(Group.getGroup(groupName3, DIRECTORY_ID, true, "d3", GroupType.GROUP));
        com.atlassian.crowd.model.group.Group group4 = groupDao.add(Group.getGroup(groupName4, DIRECTORY_ID, true, "d4", GroupType.GROUP));

        membershipDao.addUserToGroup(DIRECTORY_ID, User.NAME, groupName2);
        membershipDao.addGroupToGroup(DIRECTORY_ID, groupName2, Group.NAME);
        membershipDao.addGroupToGroup(DIRECTORY_ID, groupName3, Group.NAME);
        membershipDao.addGroupToGroup(DIRECTORY_ID, groupName4, groupName2);

        MembershipQuery<String> query = mock(MembershipQuery.class);
        when(query.getReturnType()).thenReturn(String.class);
        when(query.isFindChildren()).thenReturn(true);
        when(query.getEntityNameToMatch()).thenReturn(Group.NAME);
        when(query.getEntityToMatch()).thenReturn(EntityDescriptor.group());
        when(query.getEntityToReturn()).thenReturn(EntityDescriptor.group());
        when(query.getStartIndex()).thenReturn(0);
        when(query.getMaxResults()).thenReturn(-1);

        // Nesting is NOT handled by the DAO but higher up the stack by the Directory Manager
        List<String> groupNames = membershipDao.search(DIRECTORY_ID, query);
        assertEquals(2, groupNames.size());
        assertTrue(groupNames.contains(groupName2));
        assertTrue(groupNames.contains(groupName3));

        query = mock(MembershipQuery.class);
        when(query.getReturnType()).thenReturn(String.class);
        when(query.isFindChildren()).thenReturn(true);
        when(query.getEntityNameToMatch()).thenReturn(groupName2);
        when(query.getEntityToMatch()).thenReturn(EntityDescriptor.group());
        when(query.getEntityToReturn()).thenReturn(EntityDescriptor.group());
        when(query.getStartIndex()).thenReturn(0);
        when(query.getMaxResults()).thenReturn(-1);

        // Nesting is NOT handled by the DAO but higher up the stack by the Directory Manager
        groupNames = membershipDao.search(DIRECTORY_ID, query);
        assertEquals(1, groupNames.size());
        assertTrue(groupNames.contains(groupName4));

        query = mock(MembershipQuery.class);
        when(query.getReturnType()).thenReturn(String.class);
        when(query.isFindChildren()).thenReturn(false);
        when(query.getEntityNameToMatch()).thenReturn(groupName4);
        when(query.getEntityToMatch()).thenReturn(EntityDescriptor.group());
        when(query.getEntityToReturn()).thenReturn(EntityDescriptor.group());
        when(query.getStartIndex()).thenReturn(0);
        when(query.getMaxResults()).thenReturn(-1);

        // Nesting is NOT handled by the DAO but higher up the stack by the Directory Manager
        groupNames = membershipDao.search(DIRECTORY_ID, query);
        assertEquals(1, groupNames.size());
        assertTrue(groupNames.contains(groupName2));

        // Now test that remove works (and we don't get stale items from the cache
        membershipDao.removeGroupFromGroup(DIRECTORY_ID, groupName2, Group.NAME);
        query = mock(MembershipQuery.class);
        when(query.getReturnType()).thenReturn(String.class);
        when(query.isFindChildren()).thenReturn(true);
        when(query.getEntityNameToMatch()).thenReturn(Group.NAME);
        when(query.getEntityToMatch()).thenReturn(EntityDescriptor.group());
        when(query.getEntityToReturn()).thenReturn(EntityDescriptor.group());
        when(query.getStartIndex()).thenReturn(0);
        when(query.getMaxResults()).thenReturn(-1);

        // Nesting is NOT handled by the DAO but higher up the stack by the Directory Manager
        groupNames = membershipDao.search(DIRECTORY_ID, query);
        assertEquals(1, groupNames.size());
        assertTrue(groupNames.contains(groupName3));

        groupDao.remove(group4);
        query = mock(MembershipQuery.class);
        when(query.getReturnType()).thenReturn(String.class);
        when(query.isFindChildren()).thenReturn(false);
        when(query.getEntityNameToMatch()).thenReturn(groupName4);
        when(query.getEntityToMatch()).thenReturn(EntityDescriptor.group());
        when(query.getEntityToReturn()).thenReturn(EntityDescriptor.group());
        when(query.getStartIndex()).thenReturn(0);
        when(query.getMaxResults()).thenReturn(-1);

        // Nesting is NOT handled by the DAO but higher up the stack by the Directory Manager
        groupNames = membershipDao.search(DIRECTORY_ID, query);
        assertEquals(0, groupNames.size());
    }
}
