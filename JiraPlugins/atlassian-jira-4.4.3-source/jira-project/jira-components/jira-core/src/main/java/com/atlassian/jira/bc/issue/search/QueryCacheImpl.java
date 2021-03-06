package com.atlassian.jira.bc.issue.search;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.jql.ClauseHandler;
import com.atlassian.jira.jql.context.QueryContext;
import com.atlassian.jira.security.JiraAuthenticationContextImpl;
import com.atlassian.jira.security.RequestCacheKeys;
import com.atlassian.query.Query;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * The default implementation for the QueryCache
 *
 * The cache is stored in a common request cache, so caches are shared amongst instances.
 *
 * @since v4.0
 */
public class QueryCacheImpl implements QueryCache
{
    public Boolean getDoesQueryFitFilterFormCache(final User searcher, final Query query)
    {
        return getFitCache().get(new QueryCacheKey(searcher, query));
    }

    public void setDoesQueryFitFilterFormCache(final User searcher, final Query query, final boolean doesItFit)
    {
        getFitCache().put(new QueryCacheKey(searcher, query), doesItFit);
    }

    public QueryContext getQueryContextCache(final User searcher, final Query query)
    {
        return getQueryCache().get(new QueryCacheKey(searcher, query));
    }

    public void setQueryContextCache(final User searcher, final Query query, final QueryContext queryContext)
    {
        getQueryCache().put(new QueryCacheKey(searcher, query), queryContext);
    }

    public QueryContext getSimpleQueryContextCache(final User searcher, final Query query)
    {
        return getExplicitQueryCache().get(new QueryCacheKey(searcher, query));
    }

    public void setSimpleQueryContextCache(final User searcher, final Query query, final QueryContext queryContext)
    {
        getExplicitQueryCache().put(new QueryCacheKey(searcher, query), queryContext);
    }

    @Override
    public Collection<ClauseHandler> getClauseHandlers(User searcher, String jqlClauseName)
    {
        return getClauseHandlerCache().get(new QueryCacheClauseHandlerKey(searcher, jqlClauseName));
    }

    @Override
    public void setClauseHandlers(User searcher, String jqlClauseName, Collection<ClauseHandler> clauseHandlers)
    {
        getClauseHandlerCache().put(new QueryCacheClauseHandlerKey(searcher, jqlClauseName), clauseHandlers);
    }

    ///CLOVER:OFF
    Map<QueryCacheKey, QueryContext> getExplicitQueryCache()
    {
        return getCache(RequestCacheKeys.SIMPLE_QUERY_CONTEXT_CACHE);
    }

    Map<QueryCacheKey, QueryContext> getQueryCache()
    {
        return getCache(RequestCacheKeys.QUERY_CONTEXT_CACHE);
    }

    Map<QueryCacheKey, Boolean> getFitCache()
    {
        return getCache(RequestCacheKeys.QUERY_DOES_IT_FIT_NAVIGATOR_CACHE);
    }

    Map<QueryCacheClauseHandlerKey, Collection<ClauseHandler>> getClauseHandlerCache()
    {
        return getCache(RequestCacheKeys.JQL_CLAUSE_HANDLER_CACHE);
    }

    static <K, V> Map<K, V> getCache(final String key)
    {
        final Map<String, Object> requestCache = JiraAuthenticationContextImpl.getRequestCache();
        @SuppressWarnings("unchecked")
        Map<K, V> result = (Map<K, V>) requestCache.get(key);
        if (result == null)
        {
            result = new HashMap<K, V>();
            requestCache.put(key, result);
        }
        return result;
    }

    ///CLOVER:ON

    /**
     * A a key used for caching on Query and user pairs.
     *
     * @since v4.0
     */
    static class QueryCacheKey
    {
        private final User searcher;
        private final Query query;

        public QueryCacheKey(final User searcher, final Query query)
        {
            this.searcher = searcher;
            this.query = notNull("query", query);
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if ((o == null) || (getClass() != o.getClass()))
            {
                return false;
            }

            final QueryCacheKey that = (QueryCacheKey) o;

            if (!query.equals(that.query))
            {
                return false;
            }
            if (searcher != null ? !searcher.equals(that.searcher) : that.searcher != null)
            {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = searcher != null ? searcher.hashCode() : 0;
            result = 31 * result + query.hashCode();
            return result;
        }
    }
    /**
     * A a key used for caching on jqlClauseName and user pairs.
     *
     * @since v4.3
     */
    static class QueryCacheClauseHandlerKey
    {
        private final User searcher;
        private final String jqlClauseName;

        public QueryCacheClauseHandlerKey(final User searcher, final String jqlClauseName)
        {
            this.searcher = searcher;
            this.jqlClauseName = notNull("jqlClauseName", jqlClauseName);
        }

        @Override
        public boolean equals(final Object o)
        {
            if (this == o)
            {
                return true;
            }
            if ((o == null) || (getClass() != o.getClass()))
            {
                return false;
            }

            final QueryCacheClauseHandlerKey that = (QueryCacheClauseHandlerKey) o;

            if (!jqlClauseName.equals(that.jqlClauseName))
            {
                return false;
            }
            if (searcher != null ? !searcher.equals(that.searcher) : that.searcher != null)
            {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode()
        {
            int result = searcher != null ? searcher.hashCode() : 0;
            result = 31 * result + jqlClauseName.hashCode();
            return result;
        }
    }
}
