/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */
package com.atlassian.jira.issue.util;

import com.atlassian.jira.local.ListeningTestCase;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import com.atlassian.jira.issue.IssueFactory;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.util.EasyList;

import com.mockobjects.dynamic.Mock;

import java.util.Collection;

public class TestIssueIdsIssueIterable extends ListeningTestCase
{
    private final Mock mockIssueFactory = new Mock(IssueFactory.class);
    private final Mock mockIssueManager = new Mock(IssueManager.class);
    private final Collection<Long> issueIds = EasyList.build(new Long(1), new Long(8756), new Long(826), new Long(926));

    @Before
    public void setUp() throws Exception
    {
        mockIssueFactory.setStrict(true);
        mockIssueManager.setStrict(true);
    }

    @Test
    public void testToString()
    {
        final IssueIdsIssueIterable issueIterable = new IssueIdsIssueIterable(issueIds, (IssueManager) mockIssueManager.proxy());

        assertEquals(IssueIdsIssueIterable.class.getName() + " (4 items): [1, 8756, 826, 926]", issueIterable.toString());

        mockIssueFactory.verify();
        mockIssueManager.verify();
    }

    @Test
    public void testNullIdCollection()
    {
        try
        {
            new IssueIdsIssueIterable(null, (IssueManager) mockIssueManager.proxy());
            fail("Cannot operate with a null id collection");
        }
        catch (final IllegalArgumentException expected)
        {}
    }

    @Test
    public void testNullManager()
    {
        try
        {
            new IssueIdsIssueIterable(issueIds, null);
            fail("Cannot operate with a null IssueManager");
        }
        catch (final IllegalArgumentException expected)
        {}
    }
}
