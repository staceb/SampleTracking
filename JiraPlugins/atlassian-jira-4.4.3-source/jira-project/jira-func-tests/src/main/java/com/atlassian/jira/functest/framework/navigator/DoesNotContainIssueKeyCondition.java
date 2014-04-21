package com.atlassian.jira.functest.framework.navigator;

import com.atlassian.jira.functest.framework.assertions.TextAssertions;
import com.atlassian.jira.functest.framework.locator.CssLocator;
import net.sourceforge.jwebunit.WebTester;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * A condition which asserts that the table containing search results does not contain the specified issue key.
 *
 * @since v4.4
 */
public class DoesNotContainIssueKeyCondition implements SearchResultsCondition
{
    private static final String ISSUE_KEYS_CSS_LOCATOR = "#issuetable .issuekey";
    private final String issueKey;
    private final TextAssertions text;

    public DoesNotContainIssueKeyCondition(final TextAssertions text, final String issueKey)
    {
        this.text = text;
        checkNotNull(issueKey, "Can not build an instance of DoesNotContainIssueKeyCondition for a 'null' issue key.");
        this.issueKey = issueKey;
    }

    @Override
    public void assertCondition(final WebTester tester)
    {
        text.assertTextNotPresent(new CssLocator(tester, ISSUE_KEYS_CSS_LOCATOR), issueKey);
    }
}
