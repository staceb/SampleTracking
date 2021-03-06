package com.atlassian.jira.security.groups;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.crowd.embedded.impl.ImmutableGroup;
import com.atlassian.crowd.exception.GroupNotFoundException;
import com.atlassian.crowd.exception.OperationFailedException;
import com.atlassian.crowd.exception.OperationNotPermittedException;
import com.atlassian.crowd.exception.UserNotFoundException;
import com.atlassian.jira.user.util.OSUserConverter;
import com.atlassian.jira.util.collect.MultiMap;
import com.atlassian.jira.util.collect.MultiMaps;

import java.util.*;

/**
 * @since v4.3
 */
public class MockGroupManager implements GroupManager
{
    private Map<String, Group> groupMap = new HashMap<String, Group>();
    private Map<String, String> membershipMap = new HashMap<String, String>();
    private MultiMap<String, String, Set<String>> userToGroups = MultiMaps.createSetMultiMap();

    @Override
    public Collection<Group> getAllGroups()
    {
        return groupMap.values();
    }

    public boolean groupExists(final String groupname)
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Group createGroup(String groupName)
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public com.opensymphony.user.Group getGroup(final String groupname)
    {
        return OSUserConverter.convertToOSGroup(groupMap.get(groupname));
    }

    @Override
    public Group getGroupObject(String groupName)
    {
        return groupMap.get(groupName);
    }

    @Override
    public boolean isUserInGroup(final String username, final String groupname)
    {
        final String member = membershipMap.get(groupname);
        return member != null && member.equals(username);
    }

    @Override
    public boolean isUserInGroup(final User user, final Group group)
    {
        final String member = membershipMap.get(group.getName());
        return member != null && member.equals(user.getName());
    }

    @Override
    public boolean isUserInGroup(com.opensymphony.user.User user, com.opensymphony.user.Group group)
    {
        return isUserInGroup((User) user, (Group) group);
    }

    public Collection<User> getUsersInGroup(final String groupName)
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Collection<User> getUsersInGroup(Group group)
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    public Collection<User> getDirectUsersInGroup(Group group)
    {
        return getUsersInGroup(group);
    }

    public Collection<Group> getGroupsForUser(final String userName)
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public Collection<String> getGroupNamesForUser(final String userName)
    {
        final Set<String> groups = userToGroups.get(userName);
        if (groups == null)
        {
            return Collections.emptySet();
        }
        return groups;
    }

    @Override
    public void addUserToGroup(User user, Group group)
            throws GroupNotFoundException, UserNotFoundException, OperationNotPermittedException, OperationFailedException
    {
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    public void addGroup(String groupName)
    {
        groupMap.put(groupName, new ImmutableGroup(groupName));
    }

    public void addMember(String groupName, String userName)
    {
        membershipMap.put(groupName, userName);
        userToGroups.put(userName, groupName);
    }
}
