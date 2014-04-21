package com.atlassian.jira.issue.search.handlers;

import org.junit.Test;
import static org.junit.Assert.*;
import com.atlassian.jira.issue.search.constants.SystemSearchConstants;
import com.atlassian.jira.issue.search.searchers.impl.IssueTypeSearcher;
import com.atlassian.jira.jql.query.IssueTypeClauseQueryFactory;
import com.atlassian.jira.jql.validator.IssueTypeValidator;

/**
 * Test for {@link com.atlassian.jira.issue.search.handlers.IssueTypeSearchHandlerFactory}.
 *
 * @since v4.0
 */
public class TestIssueTypeSearchHandlerFactory extends AbstractTestSimpleSearchHandlerFactory
{
    @Test
    public void testCreateSearchHandler()
    {
        _testSystemSearcherHandler(IssueTypeSearchHandlerFactory.class,
                IssueTypeClauseQueryFactory.class,
                IssueTypeValidator.class,
                SystemSearchConstants.forIssueType(),
                IssueTypeSearcher.class, null);
    }

}
