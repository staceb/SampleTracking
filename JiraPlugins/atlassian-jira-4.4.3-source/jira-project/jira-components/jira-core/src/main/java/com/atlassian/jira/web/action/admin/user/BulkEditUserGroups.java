/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */

package com.atlassian.jira.web.action.admin.user;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.group.GroupRemoveChildMapper;
import com.atlassian.jira.bc.group.GroupService;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.customfields.converters.MultiUserConverter;
import com.atlassian.jira.issue.fields.option.GroupOption;
import com.atlassian.jira.issue.fields.option.UserOption;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.GlobalPermissionGroupAssociationUtil;
import com.atlassian.jira.util.JiraUrlCodec;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.websudo.WebSudoRequired;
import com.opensymphony.util.TextUtils;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@WebSudoRequired
public class BulkEditUserGroups extends JiraWebActionSupport
{
    private String[] selectedGroupsStr;
    private List selectedGroups;
    private Set selectedGroupsUsernames;

    private String[] usersToUnassign;

    private String usersToAssignStr;
    private ArrayList usersToAssign;
    private String prunedUsersToAssign;

    private ArrayList membersList;
    private ArrayList overloadedGroups;

    //form buttons
    private String assign;
    private String unassign;

    private Integer maxMembers;

    private final MultiUserConverter multiUserConverter;
    private final ApplicationProperties applicationProperties;
    private final GlobalPermissionGroupAssociationUtil globalPermissionGroupAssociationUtil;
    private final GroupService groupService;
    private final GroupManager groupManager;
    private final UserManager userManager;

    private static final int MAX_LIST_SIZE = 20;
    private static final String OPTION_VALUE_SEPERATOR = "______";

    public BulkEditUserGroups(GroupManager groupManager, MultiUserConverter multiUserConverter, ApplicationProperties applicationProperties, GlobalPermissionGroupAssociationUtil globalPermissionGroupAssociationUtil, GroupService groupService, UserManager userManager)
    {
        this.globalPermissionGroupAssociationUtil = globalPermissionGroupAssociationUtil;
        this.multiUserConverter = multiUserConverter;
        this.applicationProperties = applicationProperties;
        this.groupService = groupService;
        this.groupManager = groupManager;
        this.userManager = userManager;
    }

    public void doValidation()
    {
        super.doValidation();

        // Common validation for both add/remove
        if (selectedGroupsStr == null || selectedGroupsStr.length == 0)
        {
            addErrorMessage(getText("admin.bulkeditgroups.error.no.group.selected"));
            return;
        }

        // Perform the add specific validation
        if (TextUtils.stringSet(assign))
        {
            if (getUsersToAssign() == null || getUsersToAssign().isEmpty())
            {
                addErrorMessage(getText("admin.bulkeditgroups.error.no.users.to.add"));
                return;
            }

            GroupService.BulkEditGroupValidationResult bulkEditGroupValidationResult =
                    groupService.validateAddUsersToGroup(getJiraServiceContext(), Arrays.asList(selectedGroupsStr), getUsersToAssign());

            if (!bulkEditGroupValidationResult.isSuccess())
            {
                ArrayList validUsers = new ArrayList(getUsersToAssign());
                validUsers.removeAll(bulkEditGroupValidationResult.getInvalidChildren());
                prunedUsersToAssign = StringUtils.join(validUsers.iterator(), ", ");                
            }
        }
        // Perform the remove specific validation
        else if (TextUtils.stringSet(unassign))
        {
            if (usersToUnassign == null || usersToUnassign.length <= 0)
            {
                addErrorMessage(getText("admin.bulkeditgroups.error.no.users.to.remove"));
                return;
            }

            groupService.validateRemoveUsersFromGroups(getJiraServiceContext(), getGroupRemoveUserMapper());
        }
    }

    @RequiresXsrfCheck
    public String doExecute() throws Exception
    {
        if (TextUtils.stringSet(assign))
        {
            groupService.addUsersToGroups(getJiraServiceContext(), Arrays.asList(selectedGroupsStr), getUsersToAssign());
        }
        else if (TextUtils.stringSet(unassign))
        {
            groupService.removeUsersFromGroups(getJiraServiceContext(), getGroupRemoveUserMapper());
        }
        return redirectToView();
    }

    /**
     * gets all the groups, used to populate the groups select list
     */
    public Collection getAllVisibleGroups()
    {
        return globalPermissionGroupAssociationUtil.getGroupsModifiableByCurrentUser(getRemoteUser(), new ArrayList(groupManager.getAllGroups()));
    }

    /**
     * Checks if the group is selected
     */
    public boolean getIsGroupSelected(Group group)
    {
        return (getSelectedGroupsUserHasPermToSee() != null && getSelectedGroupsUserHasPermToSee().contains(group));
    }

    /**
     * Used to populate the assigned users of the selected groups.<br>
     * Always has the 'All' group which represents all the members of the selected groups.<br>
     * Rest of the users are added under individual group names.
     */
    public Collection getMembersList()
    {
        if (membersList != null || getSelectedGroupsUserHasPermToSee() == null)
            return membersList;

        membersList = new ArrayList();
        overloadedGroups = new ArrayList();
        boolean singleGroupSelected = getSelectedGroupsUserHasPermToSee().size() == 1;

        //for each selected group
        GroupOption allGroupOption = new GroupOption(getText("admin.bulkeditgroups.all.selected.groups"));
        membersList.add(allGroupOption);
        Iterator groups = getSelectedGroupsUserHasPermToSee().iterator();
        while (groups.hasNext())
        {
            Group group = (Group) groups.next();
            //and for each users in that group
            if (group != null)
            {
                GroupOption groupOption = new GroupOption(group);
                Collection<User> users = groupManager.getDirectUsersInGroup(group);
                Iterator<User> usersOfGroup = users.iterator();
                int count = 0;
                while (usersOfGroup.hasNext())
                {
                    User user = usersOfGroup.next();
                    if (user != null)
                    {
                        // increment count
                        count++;

                        if (count > getMaxUsersDisplayedPerGroup())
                        {
                            overloadedGroups.add(group.getName());
                            break;
                        }

                        if (singleGroupSelected || !isUserInAllGroupsSelected(user))
                        {
                            groupOption.addChildOption(new UserOption(user));
                        }
                        else //if more than one group is selected and the user is in all groups add to the main list
                        {
                            allGroupOption.addChildOption(new UserOption(user));
                        }
                    }
                }

                if (!groupOption.getChildOptions().isEmpty())
                    membersList.add(groupOption);
            }
        }
        if (allGroupOption.getChildOptions().isEmpty()) //remove only if empty (must be inserted at top to keep order)
            membersList.remove(allGroupOption);
        return membersList;
    }

    /**
     * Counts the total number of user entries from the memberslist.<br>
     * NOTE: This does not count distinct users - so with multiple selected groups, the count may be off
     */
    public int getAssignedUsersCount()
    {
        int assignedUsersCount = 0;
        Iterator groupOptions = getMembersList().iterator();
        while (groupOptions.hasNext())
        {
            GroupOption groupOption = (GroupOption) groupOptions.next();
            assignedUsersCount += groupOption.getChildOptions().size();
        }
        return assignedUsersCount;
    }

    /**
     * determine what size the assigned users select list should be (capped at MAX_LIST_SIZE)
     */
    public int getAssignedUsersListSize()
    {
        return getListSize(getAssignedUsersCount() + getMembersList().size());
    }

    /**
     * use this to limit the listSizes to MAX_LIST_SIZE
     */
    public int getListSize(int size)
    {
        return size < MAX_LIST_SIZE ? size : MAX_LIST_SIZE;
    }

    /**
     * used to determine what the option value (format) for a UserOption should be
     */
    public String getOptionValue(UserOption userOption)
    {
        if (userOption != null)
        {
            GroupOption parentOption = userOption.getParentOption();
            if (parentOption != null && parentOption.getGroup() != null)
            {
                return userOption.getName() + OPTION_VALUE_SEPERATOR + parentOption.getRawName();
            }
            else
            {
                return userOption.getName();
            }
        }
        return "";
    }

    public String getUnassign()
    {
        return unassign;
    }

    public void setUnassign(String unassign)
    {
        this.unassign = unassign;
    }

    public String getAssign()
    {
        return assign;
    }

    public void setAssign(String assign)
    {
        this.assign = assign;
    }

    public String[] getSelectedGroupsStr()
    {
        return selectedGroupsStr;
    }

    public void setSelectedGroupsStr(String[] selectedGroupsStr)
    {
        this.selectedGroupsStr = selectedGroupsStr;
    }

    /**
     * Of the groups the user has selected, return only those the current user has permission to edit.
     * @return those {@link com.opensymphony.user.Group Groups}.
     */
    public List getSelectedGroupsUserHasPermToSee()
    {
        if (selectedGroupsStr == null)
        {
            return new ArrayList();
        }

        if (selectedGroups == null)
        {
            ArrayList selectedGroupsHolder = new ArrayList();
            for (int i = 0; i < selectedGroupsStr.length; i++)
            {
                String groupName = selectedGroupsStr[i];
                Group group = groupManager.getGroupObject(groupName);
                if (group != null)
                {
                    selectedGroupsHolder.add(group);
                }
            }
            selectedGroups = globalPermissionGroupAssociationUtil.getGroupsModifiableByCurrentUser(getRemoteUser(), selectedGroupsHolder);
        }
        return selectedGroups;
    }

    public void setUsersToAssignStr(String usersToAssignStr)
    {
        this.usersToAssignStr = usersToAssignStr;
    }

    public String getUsersToAssignStr()
    {
        return usersToAssignStr;
    }

    public Collection getUsersToAssign()
    {
        if (usersToAssign == null)
        {
            usersToAssign = (ArrayList) multiUserConverter.extractUserStringsFromString(usersToAssignStr);
        }
        return usersToAssign;
    }

    /**
     * @return collection of valid user names to assign to the currently selected groups
     */
    public String getPrunedUsersToAssign()
    {
        return prunedUsersToAssign;
    }

    public void setUsersToUnassign(String[] usersToUnassign)
    {
        this.usersToUnassign = usersToUnassign;
    }

    public String[] getUsersToUnassign()
    {
        return usersToUnassign;
    }

    public boolean isTooManyUsersListed()
    {
        return overloadedGroups != null && !overloadedGroups.isEmpty();
    }

    public int getMaxUsersDisplayedPerGroup()
    {
        if (maxMembers == null)
        {
            final int MAX_USERS_DISPLAYED_PER_GROUP = 200;

            String maxMembersStr = applicationProperties.getDefaultBackedString(APKeys.USER_MANAGEMENT_MAX_DISPLAY_MEMBERS);
            if (maxMembersStr != null)
            {
                try
                {
                    maxMembers = Integer.valueOf(maxMembersStr.trim());
                }
                catch (NumberFormatException e)
                {
                    log.warn("Invalid format of '" + APKeys.USER_MANAGEMENT_MAX_DISPLAY_MEMBERS + "' property: '" + maxMembersStr + "'. Value should be an integer. Using " + MAX_USERS_DISPLAYED_PER_GROUP);
                    maxMembers = new Integer(MAX_USERS_DISPLAYED_PER_GROUP);
                }
            }
            else
            {
                log.debug("'" + APKeys.USER_MANAGEMENT_MAX_DISPLAY_MEMBERS + "' is missing. Using " + MAX_USERS_DISPLAYED_PER_GROUP + " instead.");
                maxMembers = new Integer(MAX_USERS_DISPLAYED_PER_GROUP);
            }
        }

        return maxMembers.intValue();
    }

    public String getPrettyPrintOverloadedGroups()
    {
        StringBuffer sb = new StringBuffer();
        int length = overloadedGroups.size();
        for (int i=0; i < length; i++)
        {
            sb.append(overloadedGroups.get(i));
            if (i == length - 2 && length > 1 )
                sb.append(" ").append(getText("common.words.and")).append(" ");
            else if (i < length - 1)
                sb.append(", ");
        }
        return sb.toString();
    }

    private GroupRemoveChildMapper getGroupRemoveUserMapper()
    {
        GroupRemoveChildMapper groupRemoveChildMapper = new GroupRemoveChildMapper(Arrays.asList(selectedGroupsStr));

        for (int i = 0; i < usersToUnassign.length; i++)
        {
            //extract the username and the group name
            String username = extractUserName(usersToUnassign[i]);
            String groupname = extractGroupName(usersToUnassign[i]);
            if (groupname != null)
            {
                groupRemoveChildMapper.register(username, groupname);
            }
            else
            {
                groupRemoveChildMapper.register(username);
            }
        }
        return groupRemoveChildMapper;
    }

    /**
     * Returns the username without without the appended group name
     * @param optionValue
     * @return
     */
    private String extractUserName(String optionValue)
    {
        int splitIndex = optionValue.indexOf(OPTION_VALUE_SEPERATOR);
        //JRA-14495: Need to allow for usernames that are 1 character long too.
        if (splitIndex >= 1)
        {
            return optionValue.substring(0, splitIndex);
        }

        return optionValue;
    }

    /**
     * Returns the group name the user is to be removed from.<br>
     * The group name is null if the user is a member of all the selected groups.
     * @param optionValue
     */
    private String extractGroupName(String optionValue)
    {
        int splitIndex = optionValue.indexOf(OPTION_VALUE_SEPERATOR);
        //JRA-14495: Need to allow for usernames that are 1 character long too.
        if (splitIndex >= 1)
        {
            return optionValue.substring(splitIndex + OPTION_VALUE_SEPERATOR.length());
        }
        return null;
    }

    private String redirectToView()
    {
        StringBuffer redirectUrl = new StringBuffer("BulkEditUserGroups!default.jspa?");

        Iterator groups = getSelectedGroupsUserHasPermToSee().iterator();
        while (groups.hasNext())
        {
            Group group = (Group) groups.next();
            redirectUrl.append("selectedGroupsStr=").append(JiraUrlCodec.encode(group.getName()));
            if (groups.hasNext())
                redirectUrl.append("&");
        }

        return getRedirect(redirectUrl.toString());
    }

    private boolean isUserInAllGroupsSelected(User user)
    {
        //get all the users of all the selected groups into one hashmap
        if (selectedGroupsUsernames == null)
        {
            selectedGroupsUsernames = new HashSet();
            Iterator selectedGroups = getSelectedGroupsUserHasPermToSee().iterator();
            if (selectedGroups.hasNext())
            {
                Group group = (Group) selectedGroups.next();
                selectedGroupsUsernames.addAll(groupManager.getDirectUsersInGroup(group));
                while (selectedGroups.hasNext())
                {
                    group = (Group) selectedGroups.next();
                    selectedGroupsUsernames.retainAll(groupManager.getDirectUsersInGroup(group));
                }
            }
        }
        return selectedGroupsUsernames.contains(user);
    }

}
