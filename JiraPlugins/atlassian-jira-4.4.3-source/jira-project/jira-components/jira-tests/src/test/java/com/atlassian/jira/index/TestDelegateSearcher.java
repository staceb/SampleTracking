package com.atlassian.jira.index;

import com.atlassian.jira.local.ListeningTestCase;
import org.junit.Test;
import static org.junit.Assert.*;
import com.atlassian.jira.util.searchers.MockSearcherFactory;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.FieldSelector;
import org.apache.lucene.document.FieldSelectorResult;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Explanation;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Similarity;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.search.Weight;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class TestDelegateSearcher extends ListeningTestCase
{
    @Test
    public void testClose() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public void close() throws IOException
            {
                called.set(true);
                super.close();
            }
        };
        new DelegateSearcher(searcher).close();
        assertTrue(called.get());
    }

    @Test
    public void testMaxDoc() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public int maxDoc() throws IOException
            {
                called.set(true);
                return 27;
            }
        };
        assertEquals(27, new DelegateSearcher(searcher).maxDoc());
        assertTrue(called.get());
    }

    @Test
    public void testGet() throws IOException
    {
        final IndexSearcher searcher = new IndexSearcher(getDirectory());
        assertSame(searcher, new DelegateSearcher(searcher).get());
    }

    @Test
    public void testGetIndexReader() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public IndexReader getIndexReader()
            {
                called.set(true);
                return super.getIndexReader();
            }
        };
        new DelegateSearcher(searcher).getIndexReader();
        assertTrue(called.get());
    }

    @Test
    public void testCreateWeightQuery() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public Query rewrite(final Query original) throws IOException
            {
                called.set(true);
                return super.rewrite(original);
            }
        };
        new DelegateSearcher(searcher).createWeight(new BooleanQuery());
        assertTrue(called.get());
    }

    @Test
    public void testDocIntFieldSelector() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public Document doc(final int i, final FieldSelector fieldSelector) throws CorruptIndexException, IOException
            {
                called.set(true);
                return new Document();
            }
        };
        new DelegateSearcher(searcher).doc(3, new FieldSelector()
        {
            public FieldSelectorResult accept(final String fieldName)
            {
                return null;
            }
        });
        assertTrue(called.get());
    }

    @Test
    public void testDocInt() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public Document doc(final int i) throws CorruptIndexException, IOException
            {
                called.set(true);
                return new Document();
            }
        };
        new DelegateSearcher(searcher).doc(3);
        assertTrue(called.get());
    }

    @Test
    public void testDocFreqTerm() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public int docFreq(final Term term) throws IOException
            {
                called.set(true);
                return super.docFreq(term);
            }
        };
        new DelegateSearcher(searcher).docFreq(new Term("one", "two"));
        assertTrue(called.get());
    }

    @Test
    public void testDocFreqsTermArray() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public int[] docFreqs(final Term[] terms) throws IOException
            {
                called.set(true);
                return super.docFreqs(terms);
            }
        };
        new DelegateSearcher(searcher).docFreqs(new Term[] { new Term("one", "two") });
        assertTrue(called.get());
    }

    @Test
    public void testEqualsObject() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public boolean equals(final Object obj)
            {
                called.set(true);
                return super.equals(obj);
            }
        };
        new DelegateSearcher(searcher).equals(null);
        assertTrue(called.get());
    }

    @Test
    public void testExplainQueryInt() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public Explanation explain(final Query query, final int doc) throws IOException
            {
                called.set(true);
                return null;
            }
        };
        new DelegateSearcher(searcher).explain(new BooleanQuery(), 1);
        assertTrue(called.get());
    }

    @Test
    public void testExplainWeightInt() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public Explanation explain(final Weight weight, final int doc) throws IOException
            {
                called.set(true);
                return null;
            }
        };
        new DelegateSearcher(searcher).explain(new BooleanQuery().weight(searcher), 1);
        assertTrue(called.get());
    }

    @Test
    public void testGetSimilarity() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public Similarity getSimilarity()
            {
                called.set(true);
                return super.getSimilarity();
            }
        };
        new DelegateSearcher(searcher).getSimilarity();
        assertTrue(called.get());
    }

    @Test
    public void testRewriteQuery() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public Query rewrite(final Query original) throws IOException
            {
                called.set(true);
                return super.rewrite(original);
            }
        };
        new DelegateSearcher(searcher).rewrite(new BooleanQuery());
        assertTrue(called.get());
    }

    @Test
    public void testSearchQueryFilterHitCollector() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public void search(final Query query, final Filter filter, final HitCollector results) throws IOException
            {
                called.set(true);
                super.search(query, filter, results);
            }
        };
        new DelegateSearcher(searcher).search(new BooleanQuery(), new Filter()
        {
            @Override
            public BitSet bits(final IndexReader reader) throws IOException
            {
                return new BitSet();
            }
        }, new HitCollector()
        {
            @Override
            public void collect(final int doc, final float score)
            {}
        });
        assertTrue(called.get());
    }

    @Test
    public void testSearchQueryFilterIntSort() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public TopFieldDocs search(final Query query, final Filter filter, final int n, final Sort sort) throws IOException
            {
                called.set(true);
                return super.search(query, filter, n, sort);
            }
        };
        new DelegateSearcher(searcher).search(new BooleanQuery(), new Filter()
        {
            @Override
            public BitSet bits(final IndexReader reader) throws IOException
            {
                return new BitSet();
            }
        }, 0, new Sort());
        assertTrue(called.get());
    }

    @Test
    public void testSearchQueryFilterInt() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public TopDocs search(final Query query, final Filter filter, final int n) throws IOException
            {
                called.set(true);
                return super.search(query, filter, n);
            }
        };
        new DelegateSearcher(searcher).search(new BooleanQuery(), new Filter()
        {
            @Override
            public BitSet bits(final IndexReader reader) throws IOException
            {
                return new BitSet();
            }
        }, 1);
        assertTrue(called.get());
    }

    @Test
    public void testSearchQueryFilterSort() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public Hits search(final Query query, final Filter filter, final Sort sort) throws IOException
            {
                called.set(true);
                return super.search(query, filter, sort);
            }
        };
        new DelegateSearcher(searcher).search(new BooleanQuery(), new Filter()
        {
            @Override
            public BitSet bits(final IndexReader reader) throws IOException
            {
                return new BitSet();
            }
        }, new Sort());
        assertTrue(called.get());
    }

    @Test
    public void testSearchQueryFilter() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public Hits search(final Query query, final Filter filter) throws IOException
            {
                called.set(true);
                return super.search(query, filter);
            }
        };
        new DelegateSearcher(searcher).search(new BooleanQuery(), new Filter()
        {
            @Override
            public BitSet bits(final IndexReader reader) throws IOException
            {
                return new BitSet();
            }
        });
        assertTrue(called.get());
    }

    @Test
    public void testSearchQueryHitCollector() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public void search(final Query query, final HitCollector results) throws IOException
            {
                called.set(true);
                super.search(query, results);
            }
        };
        new DelegateSearcher(searcher).search(new BooleanQuery(), new HitCollector()
        {
            @Override
            public void collect(final int doc, final float score)
            {}
        });
        assertTrue(called.get());
    }

    @Test
    public void testSearchQuerySort() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public Hits search(final Query query, final Sort sort) throws IOException
            {
                called.set(true);
                return super.search(query, sort);
            }
        };
        new DelegateSearcher(searcher).search(new BooleanQuery(), new Sort());
        assertTrue(called.get());
    }

    @Test
    public void testSearchWeightFilterHitCollector() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public void search(final Weight weight, final Filter filter, final HitCollector results) throws IOException
            {
                called.set(true);
                super.search(weight, filter, results);
            }
        };
        new DelegateSearcher(searcher).search(new BooleanQuery().weight(searcher), new Filter()
        {
            @Override
            public BitSet bits(final IndexReader reader) throws IOException
            {
                return new BitSet();
            }
        }, new HitCollector()
        {
            @Override
            public void collect(final int doc, final float score)
            {}
        });
        assertTrue(called.get());
    }

    @Test
    public void testSearchWeightFilterIntSort() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public TopFieldDocs search(final Weight weight, final Filter filter, final int docs, final Sort sort) throws IOException
            {
                called.set(true);
                return super.search(weight, filter, docs, sort);
            }
        };
        new DelegateSearcher(searcher).search(new BooleanQuery().weight(searcher), new Filter()
        {
            @Override
            public BitSet bits(final IndexReader reader) throws IOException
            {
                return new BitSet();
            }
        }, 0, new Sort());
        assertTrue(called.get());
    }

    @Test
    public void testSearchWeightFilterInt() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public TopDocs search(final Weight weight, final Filter filter, final int docs) throws IOException
            {
                called.set(true);
                return super.search(weight, filter, docs);
            }
        };
        new DelegateSearcher(searcher).search(new BooleanQuery().weight(searcher), new Filter()
        {
            @Override
            public BitSet bits(final IndexReader reader) throws IOException
            {
                return new BitSet();
            }
        }, 1);
        assertTrue(called.get());
    }

    @Test
    public void testSetSimilaritySimilarity() throws IOException
    {
        final AtomicBoolean called = new AtomicBoolean();
        final IndexSearcher searcher = new IndexSearcher(getDirectory())
        {
            @Override
            public void setSimilarity(final Similarity similarity)
            {
                called.set(true);
                super.setSimilarity(similarity);
            }
        };
        new DelegateSearcher(searcher).setSimilarity(null);
        assertTrue(called.get());
    }

    @Test
    public void testHashCode() throws IOException
    {
        final IndexSearcher searcher = new IndexSearcher(getDirectory());
        assertEquals(searcher.hashCode(), new DelegateSearcher(searcher).hashCode());
    }

    @Test
    public void testToString() throws IOException
    {
        final IndexSearcher searcher = new IndexSearcher(getDirectory());
        assertEquals(searcher.toString(), new DelegateSearcher(searcher).toString());
    }

    private Directory getDirectory()
    {
        return MockSearcherFactory.getCleanRAMDirectory();
    }
}
