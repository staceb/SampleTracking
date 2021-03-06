package com.atlassian.jira.webtest.selenium.navigator;

import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.webtest.framework.core.ui.Shortcuts;
import com.atlassian.jira.webtest.selenium.JiraSeleniumTest;
import com.atlassian.jira.webtest.selenium.framework.components.IssueNavResults;
import com.atlassian.jira.webtest.selenium.framework.pages.IssueNavigator;

import java.util.ArrayList;
import java.util.List;

@WebTest({Category.SELENIUM_TEST })
public class TestSearchFocus extends JiraSeleniumTest
{
    private IssueNavigator issueNav;
    private List<SearchFocusTester> focusTests = new ArrayList<SearchFocusTester>();

    @Override
    public void onSetUp()
    {
        super.onSetUp();
        issueNav = new IssueNavigator(context());
        focusTests.add(new SimpleSearchFocusTester());
        focusTests.add(new JQLSearchFocusTester());
    }

    public void testSearchFieldFocusing() throws InterruptedException
    {
        for (SearchFocusTester focusTest : focusTests)
        {
            restoreData("TestIssueNavigatorKeyboardNavigation.xml");
            focusTest.goTo();
            focusTest.runTests();
        }
    }

    private abstract class SearchFocusTester
    {
        private static final String WORKFLOW_TRANSITION_SUBMIT = "id=issue-workflow-transition-submit";
        private final String $id;

        protected SearchFocusTester(final String id)
        {
            this.$id = "#" + id;
        }

        private void assertFocused(final String jQuerySelector)
        {
            assertTrue("Expected [" + jQuerySelector + "] to be focused", isElementFocused(jQuerySelector));
        }

        private Boolean isElementFocused(final String jQuerySelector)
        {
            return Boolean.parseBoolean(client.getEval("window.jQuery('" + jQuerySelector + "')[0] "
                    + "=== window.document.activeElement"));
        }

        private void assertNotFocused(final String jQuerySelector)
        {
            assertFalse("Expected [" + jQuerySelector + "] to NOT be focused", isElementFocused(jQuerySelector));
        }

        protected void testDefaultFocus()
        {
            issueNav.goTo();
            assertFocused($id);
        }

        protected void testFocusAfterSearching()
        {
            getNavigator().findAllIssues();
            assertFocused($id);
        }

        protected void testNoFocusAfterUsingUCommand()
        {
            getNavigator().gotoIssue(issueNav.results().selectedIssueKey());
            context().ui().pressInBody(Shortcuts.UP);
            client.waitForPageToLoad();
            assertNotFocused($id);
        }

        protected void testFocusWhenClickingReturnToSearch()
        {
            getNavigator().gotoIssue(issueNav.results().selectedIssueKey());
            client.click("return-to-search", true);
            assertNotFocused($id);
        }

        protected void testNoFocusPaginationUsingKeyboard()
        {
            issueNav.results().selectIssue("HSP-21");
            context().ui().pressInBody(Shortcuts.J_NEXT);
            client.waitForPageToLoad();
            assertNotFocused($id);
            context().ui().pressInBody(Shortcuts.K_PREVIOUS);
            client.waitForPageToLoad();
            assertNotFocused($id);
        }

        protected void testFocusPaginationUsingMouse()
        {
            issueNav.results().selectIssue("HSP-21");
            issueNav.toNextPage();
            client.waitForPageToLoad();
            assertNotFocused($id);
            issueNav.toPreviousPage();
            client.waitForPageToLoad();
            assertNotFocused($id);
        }

        protected void testNoFocusAfterEditing()
        {
            getNavigator().findAllIssues();
            issueNav.results().selectIssue("HSP-30").selectedIssue()
                    .executeFromCog(IssueNavResults.IssueNavAction.EDIT);

            client.click("id=issue-edit-submit", true);

            assertNotFocused($id);
        }

        public void testNoFocusAfterUsingDialog()
        {
            client.click("id=page_2", true);
            issueNav.results().selectIssue("HSP-20")
                    .selectedIssue()
                    .executeFromCog(IssueNavResults.IssueNavAction.RESOLVE);


            assertThat.elementPresentByTimeout(WORKFLOW_TRANSITION_SUBMIT, 2000);
            client.click(WORKFLOW_TRANSITION_SUBMIT);
            client.chooseOkOnNextConfirmation();
            client.waitForPageToLoad();

            assertNotFocused($id);
        }


        public void runTests()
        {

            testDefaultFocus();
            testFocusAfterSearching();
            testNoFocusAfterUsingUCommand();
            testFocusWhenClickingReturnToSearch();
            testNoFocusPaginationUsingKeyboard();
            testFocusPaginationUsingMouse();
            testNoFocusAfterUsingDialog();
            testNoFocusAfterEditing();
        }

        abstract void goTo();
    }

    private class SimpleSearchFocusTester extends SearchFocusTester
    {

        public SimpleSearchFocusTester()
        {
            super("searcher-query");
        }

        @Override
        void goTo()
        {
            issueNav.goTo().toSimpleMode();
        }
    }


    private class JQLSearchFocusTester extends SearchFocusTester
    {

        public JQLSearchFocusTester()
        {
            super("jqltext");
        }

        @Override
        protected void testFocusAfterSearching()
        {
            issueNav.runJql("");
        }

        @Override
        void goTo()
        {
            issueNav.goTo().toJqlMode();
        }
    }

}
