/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */
package com.atlassian.jira.issue.index;

import com.atlassian.jira.local.ListeningTestCase;
import com.atlassian.jira.mock.ofbiz.MockGenericValue;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;

import com.atlassian.jira.config.ReindexMessageManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.config.util.IndexingConfiguration;
import com.atlassian.jira.config.util.MockIndexPathManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.util.IssuesIterable;
import com.atlassian.jira.mock.issue.MockIssue;
import com.atlassian.jira.util.Consumer;
import com.atlassian.jira.util.EasyList;
import com.atlassian.johnson.event.Event;
import com.atlassian.johnson.event.EventType;
import com.mockobjects.dynamic.Mock;
import org.easymock.EasyMock;
import org.ofbiz.core.entity.GenericValue;

import java.util.Collection;
import java.util.Collections;

public class TestBulkOnlyIndexManager extends ListeningTestCase
{
    private final Mock mockApplicationProperties = new Mock(ApplicationProperties.class);
    private final Mock mockIssueIndexer = new Mock(IssueIndexer.class);
    private final ApplicationProperties props = (ApplicationProperties) mockApplicationProperties.proxy();
    private ReindexMessageManager reindexMessageManager = EasyMock.createMock(ReindexMessageManager.class);
    private final BulkOnlyIndexManager indexManager = new BulkOnlyIndexManager(new IndexingConfiguration.PropertiesAdapter(props),
        (IssueIndexer) mockIssueIndexer.proxy(), new MockIndexPathManager(), reindexMessageManager);
    private final Event event = new Event(new EventType("social", "butterfly"), "party");
    private final IssuesIterable issuesIterable = new IssuesIterable()
    {
        public int size()
        {
            throw new UnsupportedOperationException();
        }

        public boolean isEmpty()
        {
            throw new UnsupportedOperationException();
        }

        public void foreach(final Consumer<Issue> sink)
        {
            throw new UnsupportedOperationException();
        }
    };

    @Before
    public void setUp() throws Exception
    {
        // these guys should NEVER get called
        mockApplicationProperties.setStrict(true);
        mockIssueIndexer.setStrict(true);
        EasyMock.replay(reindexMessageManager);
    }

    private void verifyMocks()
    {
        EasyMock.verify(reindexMessageManager);
        mockApplicationProperties.verify();
        mockIssueIndexer.verify();
    }

    /**
     * Test that all the methods we want overridden are declared by BulkOnlyIndexManager
     *
     * @throws NoSuchMethodException if any of the methods are not declared by BulkOnlyIndexManager
     */
    @Test
    public void testMethodsDeclared() throws NoSuchMethodException
    {
        final Class<BulkOnlyIndexManager> theClass = BulkOnlyIndexManager.class;
        theClass.getDeclaredMethod("deIndex", new Class[] { GenericValue.class });
        theClass.getDeclaredMethod("reIndex", new Class[] { GenericValue.class });
        theClass.getDeclaredMethod("reIndex", new Class[] { Issue.class });
        theClass.getDeclaredMethod("reIndexAll", new Class[0]);
        theClass.getDeclaredMethod("reIndexAll", new Class[] { Event.class });
        theClass.getDeclaredMethod("reIndexIssueObjects", new Class[] { Collection.class });
        theClass.getDeclaredMethod("reIndexIssues", new Class[] { IssuesIterable.class, Event.class });
        theClass.getDeclaredMethod("reIndexIssues", new Class[] { Collection.class });
        theClass.getDeclaredMethod("optimize", new Class[0]);
    }

    @Test
    public void testReIndexAll() throws IndexException
    {
        assertEquals(-1, indexManager.reIndexAll());
        verifyMocks();
    }

    public void testReIndexAllWithEvent() throws IndexException
    {
        assertEquals(-1, indexManager.reIndexAll(event));
        verifyMocks();
    }

    @Test
    public void testReIndexIssueObjects() throws IndexException
    {
        assertEquals(-1, indexManager.reIndexIssueObjects(Collections.<Issue> emptyList()));
        final Collection<MockIssue> issueObjects = EasyList.build(new MockIssue(), new MockIssue());
        assertEquals(-1, indexManager.reIndexIssueObjects(issueObjects));
        verifyMocks();
    }

    @Test
    public void testReIndexIssues() throws IndexException
    {
        assertEquals(-1, indexManager.reIndexIssues(issuesIterable, event));
        verifyMocks();
    }

    @Test
    public void testReIndexIssuesWithIssues() throws IndexException
    {
        assertEquals(-1, indexManager.reIndexIssues(Collections.<GenericValue> emptyList()));
        final Collection<GenericValue> issueObjects = EasyList.<GenericValue> build(new MockGenericValue("Issue"), new MockGenericValue("Issue"));
        assertEquals(-1, indexManager.reIndexIssues(issueObjects));
        verifyMocks();
    }

    @Test
    public void testOptimize() throws IndexException
    {
        assertEquals(-1, indexManager.optimize());
        verifyMocks();
    }
}
