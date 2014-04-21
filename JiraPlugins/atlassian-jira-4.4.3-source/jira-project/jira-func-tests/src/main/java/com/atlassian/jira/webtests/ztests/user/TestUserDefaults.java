package com.atlassian.jira.webtests.ztests.user;

import com.atlassian.jira.functest.framework.FuncTestCase;
import com.atlassian.jira.functest.framework.locator.XPathLocator;
import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;

/**
 * Test the UpdateUserDefaults page
 *
 * @since v4.0
 */
@WebTest ({ Category.FUNC_TEST, Category.USERS_AND_GROUPS })
public class TestUserDefaults extends FuncTestCase
{
    @Override
    protected void setUpTest()
    {
        super.setUpTest();
        administration.restoreBlankInstance();
    }

    public void testIssuesPerPageValidation() throws Exception
    {
        _testIssuesPerPageValidation("0", false);
        _testIssuesPerPageValidation("-100", false);
        _testIssuesPerPageValidation("1001", false);
        
        _testIssuesPerPageValidation("1", true);
        _testIssuesPerPageValidation("1000", true);
        _testIssuesPerPageValidation("500", true);
    }

    private void _testIssuesPerPageValidation(final String setting, final boolean valid)
    {
        navigation.gotoAdminSection("user_defaults");
        navigation.clickLinkWithExactText("Edit default values");
        tester.setWorkingForm("jiraform");
        tester.setFormElement("numIssues", setting);
        tester.submit("Update");
        if (valid)
        {
            text.assertTextSequence(new XPathLocator(tester, "//table[@id='view_user_defaults']"),  new String[] {"Number of Issues displayed per Issue Navigator page", setting});
        }
        else
        {
            text.assertTextPresent(new XPathLocator(tester, "//table[@id='edit_user_defaults']"), "Issues per page must be a number between 1 and 1000");
        }
    }
}
