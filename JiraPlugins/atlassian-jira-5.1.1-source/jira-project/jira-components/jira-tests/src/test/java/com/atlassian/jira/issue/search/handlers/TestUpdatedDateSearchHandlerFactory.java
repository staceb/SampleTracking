package com.atlassian.jira.issue.search.handlers;

import org.junit.Test;
import static org.junit.Assert.*;
import com.atlassian.jira.issue.search.constants.SystemSearchConstants;
import com.atlassian.jira.issue.search.searchers.impl.UpdatedDateSearcher;
import com.atlassian.jira.jql.query.UpdatedDateClauseQueryFactory;
import com.atlassian.jira.jql.validator.UpdatedDateValidator;

/**
 * Test for {@link UpdatedDateSearchHandlerFactory}.
 *
 * @since v4.0
 */
public class TestUpdatedDateSearchHandlerFactory extends AbstractTestSimpleSearchHandlerFactory
{
    @Test
    public void testCreateHandler() throws Exception
    {
        _testSystemSearcherHandler(UpdatedDateSearchHandlerFactory.class,
                UpdatedDateClauseQueryFactory.class,
                UpdatedDateValidator.class,
                SystemSearchConstants.forUpdatedDate(),
                UpdatedDateSearcher.class, null);
    }
}
