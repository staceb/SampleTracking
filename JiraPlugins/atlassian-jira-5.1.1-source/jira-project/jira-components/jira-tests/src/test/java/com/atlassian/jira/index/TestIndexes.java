package com.atlassian.jira.index;

import com.atlassian.instrumentation.Counter;
import com.atlassian.instrumentation.InstrumentRegistry;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.index.DefaultIndexManager;
import com.atlassian.jira.local.ListeningTestCase;
import com.atlassian.jira.local.runner.ListeningMockitoRunner;
import com.atlassian.jira.local.testutils.MultiTenantContextTestUtils;
import com.atlassian.jira.mock.MockApplicationProperties;
import com.atlassian.jira.mock.component.MockComponentWorker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import com.atlassian.jira.index.Index.UpdateMode;
import com.atlassian.jira.util.searchers.MockSearcherFactory;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;

@RunWith (ListeningMockitoRunner.class)
public class TestIndexes extends ListeningTestCase
{
    @Mock
    private InstrumentRegistry instrumentRegistry;
    @Mock
    private Counter counter;

    @Before
    public void setUp() throws Exception
    {
        MultiTenantContextTestUtils.setupMultiTenantSystem();
        when(instrumentRegistry.pullCounter(any(String.class))).thenReturn(counter);
        final MockComponentWorker worker = new MockComponentWorker();
        worker.addMock(InstrumentRegistry.class, instrumentRegistry);
        worker.addMock(ApplicationProperties.class, new MockApplicationProperties());
        ComponentAccessor.initialiseWorker(worker);
    }

    @Test
    public void testQueuedManagerNewSearcherAfterCreate() throws Exception
    {
        final Index.Manager manager = Indexes.createQueuedIndexManager("TestQueuedManager", new DefaultConfiguration(
            MockSearcherFactory.getCleanRAMDirectory(), new StandardAnalyzer(DefaultIndexManager.LUCENE_VERSION)));
        try
        {
            final IndexSearcher searcher = manager.getSearcher();
            manager.getIndex().perform(Operations.newCreate(new Document(), UpdateMode.INTERACTIVE)).await();
            final IndexSearcher searcher2 = manager.getSearcher();
            assertNotSame(searcher, searcher2);

            searcher.close();
            searcher2.close();
        }
        finally
        {
            manager.close();
        }
    }

    @Test
    public void testQueuedManagerNewSearcherAfterUpdate() throws Exception
    {
        final Index.Manager manager = Indexes.createQueuedIndexManager("TestQueuedManager", new DefaultConfiguration(
            MockSearcherFactory.getCleanRAMDirectory(), new StandardAnalyzer(DefaultIndexManager.LUCENE_VERSION)));
        try
        {
            final IndexSearcher searcher = manager.getSearcher();
            manager.getIndex().perform(Operations.newUpdate(new Term("test", "1"), new Document(), Index.UpdateMode.INTERACTIVE)).await();
            final IndexSearcher searcher2 = manager.getSearcher();
            assertNotSame(searcher, searcher2);
            searcher.close();
            searcher2.close();
        }
        finally
        {
            manager.close();
        }
    }

    @Test
    public void testQueuedManagerNewSearcherAfterDelete() throws Exception
    {
        final Index.Manager manager = Indexes.createQueuedIndexManager("TestQueuedManager", new DefaultConfiguration(
            MockSearcherFactory.getCleanRAMDirectory(), new StandardAnalyzer(DefaultIndexManager.LUCENE_VERSION)));
        try
        {
            final IndexSearcher searcher = manager.getSearcher();
            manager.getIndex().perform(Operations.newDelete(new Term("test", "1"), UpdateMode.INTERACTIVE)).await();
            final IndexSearcher searcher2 = manager.getSearcher();
            assertNotSame(searcher, searcher2);

            searcher.close();
            searcher2.close();
        }
        finally
        {
            manager.close();
        }
    }

    @Test
    public void testSimpleManagerNewSearcherAfterDelete() throws Exception
    {
        final Index.Manager manager = Indexes.createSimpleIndexManager(new DefaultConfiguration(MockSearcherFactory.getCleanRAMDirectory(),
                new StandardAnalyzer(DefaultIndexManager.LUCENE_VERSION)));
        try
        {
            final IndexSearcher searcher = manager.getSearcher();
            manager.getIndex().perform(Operations.newCreate(new Document(), UpdateMode.INTERACTIVE)).await();
            final IndexSearcher searcher2 = manager.getSearcher();
            assertNotSame(searcher, searcher2);

            searcher.close();
            searcher2.close();
        }
        finally
        {
            manager.close();
        }
    }

    @After
    public void teardown() throws Exception
    {
        ComponentAccessor.initialiseWorker(null);
    }
}
