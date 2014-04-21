package com.atlassian.jira.issue.search.handlers;

import org.junit.Test;
import static org.junit.Assert.*;
import com.atlassian.jira.issue.search.constants.SystemSearchConstants;
import com.atlassian.jira.issue.search.searchers.impl.ResolutionSearcher;
import com.atlassian.jira.jql.query.ResolutionClauseQueryFactory;
import com.atlassian.jira.jql.validator.ResolutionValidator;

/**
 * Test for {@link com.atlassian.jira.issue.search.handlers.ResolutionSearchHandlerFactory}.
 *
 * @since v4.0
 */
public class TestResolutionSearchHandlerFactory extends AbstractTestSimpleSearchHandlerFactory
{
    @Test
    public void testCreateHandler() throws Exception
    {
        _testSystemSearcherHandler(ResolutionSearchHandlerFactory.class,
                ResolutionClauseQueryFactory.class,
                ResolutionValidator.class,
                SystemSearchConstants.forResolution(),
                ResolutionSearcher.class, null);
    }
}
