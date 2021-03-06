package com.atlassian.jira.webtests.ztests.issue;

import com.atlassian.jira.functest.framework.locator.IdLocator;
import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.webtests.JIRAWebTest;

@WebTest ({ Category.FUNC_TEST, Category.ISSUES, Category.PERMISSIONS })
public class TestIssueOperationsWithLimitedPermissions extends JIRAWebTest
{
    public static final String RESTRICTED_ISSUE_ID = "10000";
    public static final String CLOSED_ISSUE_ID = "10020";
    public static final String PERMISSION_ERROR_DESC_ANONYMOUS = "You are not logged in, and do not have the permissions required to act on the selected issue as a guest.";
    public static final String PERMISSION_ERROR_DESC_ANONYMOUS_DIALOG = "You do not have the permission to see the specified issue";
    public static final String PERMISSION_ERROR_DESC_USER = "You do not have permission to act on this issue.";
    private static final String LOGIN = "log in";
    private static final String SIGNUP = "sign up";
    private static final String YOU_CANNOT_VIEW_THIS_AS_A_GUEST = "You must log in to access this page.";

    public TestIssueOperationsWithLimitedPermissions(String name)
    {
        super(name);
    }

    public void setUp()
    {
        super.setUp();
        login(ADMIN_USERNAME, ADMIN_PASSWORD);
        restoreData("TestIssueOperationsWithLimitedPermissionsProEnt.xml");
    }

    public void testIssueOperationsWithLimitedPermissions()
    {
        //testViewIssue()
        assertActionIsInaccessibleToAnonymousUser("ViewIssue.jspa?", "new test issue", YOU_CANNOT_VIEW_THIS_AS_A_GUEST, true);
        assertActionIsInaccessibleToUser("ViewIssue.jspa?", "new test issue", "It seems that you have tried to perform an operation which you are not permitted to perform.");
        assertActionIsAccessibleToAdmin("ViewIssue.jspa?", "new test issue", YOU_CANNOT_VIEW_THIS_AS_A_GUEST);

        //testViewIssueVote()
        assertActionIsInaccessibleToAnonymousUser("VoteOrWatchIssue.jspa?vote=vote&", "new test issue", YOU_CANNOT_VIEW_THIS_AS_A_GUEST, true);
        assertActionIsInaccessibleToUser("VoteOrWatchIssue.jspa?vote=vote&", "new test issue", "It seems that you have tried to perform an operation which you are not permitted to perform.");
        assertActionIsAccessibleToAdmin("VoteOrWatchIssue.jspa?vote=vote&", "new test issue", YOU_CANNOT_VIEW_THIS_AS_A_GUEST);
        assertLinkPresent("toggle-vote-issue");
        text.assertTextPresent(new IdLocator(tester, "toggle-vote-issue"), "Remove Vote");
        text.assertTextNotPresent(new IdLocator(tester, "toggle-vote-issue"), "Add Vote");
        assertActionIsInaccessibleToAdminForClosedIssue("VoteOrWatchIssue.jspa?vote=vote&", "An issue that will be closed!", "You cannot vote or change your vote on resolved issues.");

        //testViewIssueUnvote()
        assertActionIsInaccessibleToAnonymousUser("VoteOrWatchIssue.jspa?vote=unvote&", "new test issue", YOU_CANNOT_VIEW_THIS_AS_A_GUEST, true);
        assertActionIsInaccessibleToUser("VoteOrWatchIssue.jspa?vote=unvote&", "new test issue", "It seems that you have tried to perform an operation which you are not permitted to perform.");
        assertActionIsAccessibleToAdmin("VoteOrWatchIssue.jspa?vote=unvote&", "new test issue", YOU_CANNOT_VIEW_THIS_AS_A_GUEST);
        assertLinkPresent("toggle-vote-issue");
        text.assertTextPresent(new IdLocator(tester, "toggle-vote-issue"), "Add Vote");
        text.assertTextNotPresent(new IdLocator(tester, "toggle-vote-issue"), "Remove Vote");
        assertActionIsInaccessibleToAdminForClosedIssue("VoteOrWatchIssue.jspa?vote=unvote&", "An issue that will be closed!", "You cannot vote or change your vote on resolved issues.");


        //testViewIssueStartWatching()
        assertActionIsInaccessibleToAnonymousUser("VoteOrWatchIssue.jspa?watch=watch&", "new test issue", YOU_CANNOT_VIEW_THIS_AS_A_GUEST, true);
        assertActionIsInaccessibleToUser("VoteOrWatchIssue.jspa?watch=watch&", "new test issue", "It seems that you have tried to perform an operation which you are not permitted to perform.");
        assertActionIsAccessibleToAdmin("VoteOrWatchIssue.jspa?watch=watch&", "new test issue", YOU_CANNOT_VIEW_THIS_AS_A_GUEST);
        assertLinkPresent("toggle-watch-issue");
        text.assertTextPresent(new IdLocator(tester, "toggle-watch-issue"), "Stop Watching");
        text.assertTextNotPresent(new IdLocator(tester, "toggle-watch-issue"), "Watch Issue");

        //testViewIssueStopWatching()
        assertActionIsInaccessibleToAnonymousUser("VoteOrWatchIssue.jspa?watch=unwatch&", "new test issue", YOU_CANNOT_VIEW_THIS_AS_A_GUEST, true);
        assertActionIsInaccessibleToUser("VoteOrWatchIssue.jspa?watch=unwatch&", "new test issue", "It seems that you have tried to perform an operation which you are not permitted to perform.");
        assertActionIsAccessibleToAdmin("VoteOrWatchIssue.jspa?watch=unwatch&", "new test issue", YOU_CANNOT_VIEW_THIS_AS_A_GUEST);
        assertLinkPresent("toggle-watch-issue");
        text.assertTextNotPresent(new IdLocator(tester, "toggle-watch-issue"), "Stop Watching");
        text.assertTextPresent(new IdLocator(tester, "toggle-watch-issue"), "Watch Issue");

        //testWorkflowUIDispatcher()
        assertActionIsInaccessibleToAnonymousUser("WorkflowUIDispatcher.jspa?action=5&", "Resolve Issue", YOU_CANNOT_VIEW_THIS_AS_A_GUEST, true);
        assertActionIsInaccessibleToUser("WorkflowUIDispatcher.jspa?action=5&", "Resolve Issue", "It seems that you have tried to perform an operation which you are not permitted to perform.");
        assertActionIsAccessibleToAdmin("WorkflowUIDispatcher.jspa?action=5&", "Resolve Issue", YOU_CANNOT_VIEW_THIS_AS_A_GUEST);

        //testAssignIssue()
        assertActionIsInaccessibleToAnonymousUser("AssignIssue!default.jspa?", "assign-issue-submit", "You do not have the permission to see the specified issue", true);
        assertActionIsInaccessibleToUser("AssignIssue!default.jspa?", "assign-issue-submit", "You do not have the permission to see the specified issue");
        assertActionIsAccessibleToAdmin("AssignIssue!default.jspa?", "assign-issue-submit", PERMISSION_ERROR_DESC_ANONYMOUS);
        assertActionIsInaccessibleToAdminForClosedIssue("AssignIssue!default.jspa?", "assign-issue-submit", "It seems that you have tried to perform an operation which you are not permitted to perform.");

        //testCommentAssignIssue()
        assertActionIsInaccessibleToAnonymousUser("CommentAssignIssue!default.jspa?action=5&", "Resolve Issue", PERMISSION_ERROR_DESC_ANONYMOUS, true);
        assertActionIsInaccessibleToUser("CommentAssignIssue!default.jspa?action=5&", "Resolve Issue", PERMISSION_ERROR_DESC_USER);
        assertActionIsAccessibleToAdmin("CommentAssignIssue!default.jspa?action=5&", "Resolve Issue", PERMISSION_ERROR_DESC_ANONYMOUS);

        //testEditIssue()
        assertActionIsInaccessibleToAnonymousUser("EditIssue!default.jspa?", "Edit Issue", PERMISSION_ERROR_DESC_ANONYMOUS, true);
        assertActionIsInaccessibleToUser("EditIssue!default.jspa?", "Edit Issue", PERMISSION_ERROR_DESC_USER);
        assertActionIsAccessibleToAdmin("EditIssue!default.jspa?", "Edit Issue", PERMISSION_ERROR_DESC_ANONYMOUS);
        assertActionIsInaccessibleToAdminForClosedIssue("EditIssue!default.jspa?", null, "You are not allowed to edit this issue due to its current status in the workflow.");

        //testLabels()
        assertActionIsInaccessibleToAnonymousUser("EditLabels!default.jspa?", null, "You do not have the permission to see the specified issue", true);
        assertActionIsInaccessibleToUser("EditLabels!default.jspa?", null, "You do not have the permission to see the specified issue");
        assertActionIsAccessibleToAdmin("EditLabels!default.jspa?", "Labels", PERMISSION_ERROR_DESC_ANONYMOUS);
        assertActionIsInaccessibleToAdminForClosedIssue("EditLabels!default.jspa?", null, "It seems that you have tried to perform an operation which you are not permitted to perform.");

        //testCloneIssueDetails()
        assertActionIsInaccessibleToAnonymousUser("CloneIssueDetails!default.jspa?", "Summary", "You do not have the permission to see the specified issue", false);
        assertActionIsInaccessibleToUser("CloneIssueDetails!default.jspa?", "Summary", "You do not have the permission to see the specified issue");
        assertActionIsAccessibleToAdmin("CloneIssueDetails!default.jspa?", "Summary", "You do not have the permission to see the specified issue");

        //testMoveIssue()
        assertActionIsInaccessibleToAnonymousUser("MoveIssue!default.jspa?", "Current Project", "You are not logged in and do not have the permissions required to browse projects as a guest.", true);
        assertActionIsInaccessibleToUser("MoveIssue!default.jspa?", "Current Project", "You do not have the permissions required to browse any projects.");
        assertActionIsAccessibleToAdmin("MoveIssue!default.jspa?", "Current Project", "You are not logged in and do not have the permissions required to browse projects as a guest.");

        //testViewVoters()
        assertActionIsInaccessibleToAnonymousUser("ViewVoters!default.jspa?", "There are no voters for this issue", PERMISSION_ERROR_DESC_ANONYMOUS, true);
        assertActionIsInaccessibleToUser("ViewVoters!default.jspa?", "There are no voters for this issue", PERMISSION_ERROR_DESC_USER);
        assertActionIsAccessibleToAdmin("ViewVoters!default.jspa?", "There are no voters for this issue", PERMISSION_ERROR_DESC_ANONYMOUS);

        //testViewVotersAddVote()
        assertActionIsInaccessibleToAnonymousUser("ViewVoters!addVote.jspa?", "There are no voters for this issue", PERMISSION_ERROR_DESC_ANONYMOUS, true);
        assertActionIsInaccessibleToUser("ViewVoters!addVote.jspa?", "There are no voters for this issue", PERMISSION_ERROR_DESC_USER);
        assertActionIsAccessibleToAdmin("ViewVoters!addVote.jspa?", "Remove your vote", PERMISSION_ERROR_DESC_ANONYMOUS);

        //testViewVotersRemoveVote()
        assertActionIsInaccessibleToAnonymousUser("ViewVoters!removeVote.jspa?", "There are no voters for this issue", PERMISSION_ERROR_DESC_ANONYMOUS, true);
        assertActionIsInaccessibleToUser("ViewVoters!removeVote.jspa?", "There are no voters for this issue", PERMISSION_ERROR_DESC_USER);
        assertActionIsAccessibleToAdmin("ViewVoters!removeVote.jspa?", "There are no voters for this issue", PERMISSION_ERROR_DESC_ANONYMOUS);

        //testManageWatchers()
        assertActionIsInaccessibleToAnonymousUser("ManageWatchers!default.jspa?", "Watch Issue", PERMISSION_ERROR_DESC_ANONYMOUS, true);
        assertActionIsInaccessibleToUser("ManageWatchers!default.jspa?", "Watch Issue", PERMISSION_ERROR_DESC_USER);
        assertActionIsAccessibleToAdmin("ManageWatchers!default.jspa?", "Watch Issue", PERMISSION_ERROR_DESC_ANONYMOUS);

        //testManageWatchersStartWatching()
        assertActionIsInaccessibleToAnonymousUser("ManageWatchers!startWatching.jspa?", "Watch Issue", PERMISSION_ERROR_DESC_ANONYMOUS, true);
        assertActionIsInaccessibleToUser("ManageWatchers!startWatching.jspa?", "Watch Issue", PERMISSION_ERROR_DESC_USER);
        assertActionIsAccessibleToAdmin("ManageWatchers!startWatching.jspa?", "Stop Watching", PERMISSION_ERROR_DESC_ANONYMOUS);

        //testManageWatchersStopWatching()
        assertActionIsInaccessibleToAnonymousUser("ManageWatchers!stopWatching.jspa?", "Watch Issue", PERMISSION_ERROR_DESC_ANONYMOUS, true);
        assertActionIsInaccessibleToUser("ManageWatchers!stopWatching.jspa?", "Watch Issue", PERMISSION_ERROR_DESC_USER);
        assertActionIsAccessibleToAdmin("ManageWatchers!stopWatching.jspa?", "Watch Issue", PERMISSION_ERROR_DESC_ANONYMOUS);

        //testManageWatchersStartWatchers()
        assertActionIsInaccessibleToAnonymousUser("ManageWatchers!startWatchers.jspa?userNames=admin&", "Watch Issue", PERMISSION_ERROR_DESC_ANONYMOUS, true);
        assertActionIsInaccessibleToUser("ManageWatchers!startWatchers.jspa?userNames=admin&", "Watch Issue", PERMISSION_ERROR_DESC_USER);
        assertActionIsAccessibleToAdmin("ManageWatchers!startWatchers.jspa?userNames=admin&", "Stop Watching", PERMISSION_ERROR_DESC_ANONYMOUS);

        //testManageWatchersStopWatchers()
        assertActionIsInaccessibleToAnonymousUser("ManageWatchers!stopWatchers.jspa?userNames=admin&", "Watch Issue", PERMISSION_ERROR_DESC_ANONYMOUS, true);
        assertActionIsInaccessibleToUser("ManageWatchers!stopWatchers.jspa?userNames=admin&", "Watch Issue", PERMISSION_ERROR_DESC_USER);
        assertActionIsAccessibleToAdmin("ManageWatchers!stopWatchers.jspa?userNames=admin&", "Stop Watching", PERMISSION_ERROR_DESC_ANONYMOUS);

        //testCreateWorklog()
        assertActionIsInaccessibleToAnonymousUser("CreateWorklog!default.jspa?", null, PERMISSION_ERROR_DESC_ANONYMOUS_DIALOG, true);
        assertActionIsInaccessibleToUser("CreateWorklog!default.jspa?", null, "It seems that you have tried to perform an operation which you are not permitted to perform");
        assertActionIsAccessibleToAdmin("CreateWorklog!default.jspa?", "Log Work", PERMISSION_ERROR_DESC_ANONYMOUS);
        assertActionIsInaccessibleToAdminForClosedIssue("CreateWorklog!default.jspa?", null, "It seems that you have tried to perform an operation which you are not permitted to perform.");

        //testUpdateWorklog!default
        assertActionIsInaccessibleToAnonymousUser("UpdateWorklog!default.jspa?worklogId=10000&", "Edit Work Log", YOU_CANNOT_VIEW_THIS_AS_A_GUEST, false);
        assertActionIsInaccessibleToUser("UpdateWorklog!default.jspa?worklogId=10000&", "Edit Work Log", "It seems that you have tried to perform an operation which you are not permitted to perform.");
        assertActionIsAccessibleToAdmin("UpdateWorklog!default.jspa?worklogId=10000&", "Edit Work Log", "It seems that you have tried to perform an operation which you are not permitted to perform.");

        //testDeleteWorklog!default
        assertActionIsInaccessibleToAnonymousUser("DeleteWorklog!default.jspa?worklogId=10000&", "Delete Worklog", YOU_CANNOT_VIEW_THIS_AS_A_GUEST, false);
        assertActionIsInaccessibleToUser("DeleteWorklog!default.jspa?worklogId=10000&", "Delete Worklog", "It seems that you have tried to perform an operation which you are not permitted to perform.");
        assertActionIsAccessibleToAdmin("DeleteWorklog!default.jspa?worklogId=10000&", "Delete Worklog", "It seems that you have tried to perform an operation which you are not permitted to perform.");

        //testEditComment
        assertActionIsInaccessibleToAnonymousUser("EditComment!default.jspa?commentId=10000&", "ignoreMeAndSeeAssertBelow", "You do not have the permission to edit this comment.", false);
        assertActionIsInaccessibleToUser("EditComment!default.jspa?commentId=10000&", "ignoreMeAndSeeAssertBelow", "you do not have the permission to edit this comment.");
        assertActionIsAccessibleToAdmin("EditComment!default.jspa?commentId=10000&", "Edit Comment", "you do not have the permission to edit this comment.");

        //testDeleteComment
        assertActionIsInaccessibleToAnonymousUser("DeleteComment!default.jspa?commentId=10000&", "ignoreMeAndSeeAssertBelow", "You do not have permission to delete comment with id: 10000", false);
        assertActionIsInaccessibleToUser("DeleteComment!default.jspa?commentId=10000&", "ignoreMeAndSeeAssertBelow", "You do not have permission to delete comment with id: 10000");
        assertActionIsAccessibleToAdmin("DeleteComment!default.jspa?commentId=10000&", "Delete Comment", "You do not have permission to delete comment with id: 10000");

        //testCreateSubTaskIssue!default()
        assertActionIsInaccessibleToAnonymousUser("CreateSubTaskIssue!default.jspa?parentIssueId=10000&", "Component/s", "You are not logged in and do not have the permissions required to browse projects as a guest.", true);
        assertActionIsInaccessibleToUser("CreateSubTaskIssue!default.jspa?parentIssueId=10000&", "Component/s", "You do not have the permissions required to browse any projects");
        assertActionIsAccessibleToAdmin("CreateSubTaskIssue!default.jspa?parentIssueId=10000&", "Component/s", PERMISSION_ERROR_DESC_ANONYMOUS);        

        //testCreateSubTaskIssue()
        assertActionIsInaccessibleToAnonymousUser("CreateSubTaskIssue.jspa?parentIssueId=10000&", "Choose the issue type", "You are not logged in and do not have the permissions required to browse projects as a guest.", true);
        assertActionIsInaccessibleToUser("CreateSubTaskIssue.jspa?parentIssueId=10000&", "Choose the issue type", "You do not have the permissions required to browse any projects");
        assertActionIsAccessibleToAdmin("CreateSubTaskIssue.jspa?parentIssueId=10000&", "Choose the issue type", PERMISSION_ERROR_DESC_ANONYMOUS);

        //testCreateSubTaskIssueDetails()
        assertActionIsInaccessibleToAnonymousUser("CreateSubTaskIssueDetails.jspa?parentIssueId=10000&issuetype=5&pid=10000&", "ignoreMeAndSeeAssertBelow", "You are not logged in, and do not have the permissions required to create an issue in this project as a guest.", false);
        assertTextPresent("Create Sub-Task");
        assertActionIsInaccessibleToUser("CreateSubTaskIssueDetails.jspa?parentIssueId=10000&issuetype=5&pid=10000&", "ignoreMeAndSeeAssertBelow", "pid: You do not have permission to create issues in this project.");
        assertTextPresent("Create Sub-Task");
        assertActionIsAccessibleToAdmin("CreateSubTaskIssueDetails.jspa?parentIssueId=10000&issuetype=5&pid=10000&", "Create Sub-Task", PERMISSION_ERROR_DESC_ANONYMOUS);
    }

    //--------------------------------------------------------------------------------------------------- Helper Methods
    private void assertActionIsInaccessibleToAnonymousUser(String actionUrl, String textNotPresent, String error_desc, boolean hasLoginLink)
    {
        logout();
        page.getFreshXsrfToken();

        gotoPage("/secure/" + actionUrl + "id=" + RESTRICTED_ISSUE_ID + "&atl_token=" + page.getXsrfToken());
        if (hasLoginLink)
        {
            assertTextPresent(error_desc);
            assertLinkPresentWithText(SIGNUP);
        } else {
            assertTextPresent(error_desc);
        }
        if (textNotPresent != null)
        {
            assertTextNotPresent(textNotPresent);
        }
    }

    private void assertActionIsInaccessibleToUser(String actionUrl, String textNotPresent, String error_desc)
    {
        login(FRED_USERNAME, FRED_PASSWORD);

        gotoPage("/secure/" + actionUrl + "id=" + RESTRICTED_ISSUE_ID + "&atl_token=" + page.getXsrfToken());
        assertTextPresent(error_desc);
        assertLinkNotPresentWithText(LOGIN);
        assertLinkNotPresentWithText(SIGNUP);
        if (textNotPresent != null)
        {
            assertTextNotPresent(textNotPresent);
        }
    }

    private void assertActionIsAccessibleToAdmin(String actionUrl, String textPresent, String textNotPresent)
    {
        login(ADMIN_USERNAME, ADMIN_PASSWORD);

        gotoPage("/secure/" + actionUrl + "id=" + RESTRICTED_ISSUE_ID + "&atl_token=" + page.getXsrfToken());
        assertTextNotPresent(textNotPresent);
        assertLinkNotPresentWithText(LOGIN);
        assertLinkNotPresentWithText(SIGNUP);
        assertTextPresent(textPresent);
    }

    private void assertActionIsInaccessibleToAdminForClosedIssue(String actionUrl, String textNotPresent, String error_desc)
    {
        login(ADMIN_USERNAME, ADMIN_PASSWORD);

        gotoPage("/secure/" + actionUrl + "id=" + CLOSED_ISSUE_ID + "&atl_token=" + page.getXsrfToken());
        assertTextPresent(error_desc);
        assertLinkNotPresentWithText(LOGIN);
        assertLinkNotPresentWithText(SIGNUP);
        if (textNotPresent != null)
        {
            assertTextNotPresent(textNotPresent);
        }
    }
}
