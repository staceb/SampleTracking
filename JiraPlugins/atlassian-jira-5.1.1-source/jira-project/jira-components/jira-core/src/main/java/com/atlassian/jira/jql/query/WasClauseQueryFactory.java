package com.atlassian.jira.jql.query;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.issue.search.SearchProviderFactory;
import com.atlassian.jira.issue.search.filters.IssueIdFilter;
import com.atlassian.jira.issue.statistics.util.DocumentHitCollector;
import com.atlassian.jira.jql.operand.EmptyWasClauseOperandHandler;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.resolver.ChangeHistoryFieldIdResolver;
import com.atlassian.query.clause.WasClause;
import com.atlassian.query.operand.EmptyOperand;
import com.atlassian.query.operand.FunctionOperand;
import com.atlassian.query.operand.MultiValueOperand;
import com.atlassian.query.operator.Operator;
import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Factory class for validating and building the Lucene Was query.
 *
 * @since v4.3
 */
public class WasClauseQueryFactory
{
    private static final Logger log = Logger.getLogger(WasClauseQueryFactory.class);
    private final SearchProviderFactory searchProviderFactory;
    private final JqlOperandResolver operandResolver;
    private final HistoryPredicateQueryFactory wasPredicateQueryFactory;
    private final EmptyWasClauseOperandHandler emptyWasClauseOperandHandler;
    private final ChangeHistoryFieldIdResolver changeHistoryFieldIdResolver;


    /**
     * @param searchProviderFactory factory for retrieving the history search provider
     * @param operandResolver resolves {@link com.atlassian.query.operand.Operand}  and retrieves their values
     * @param wasPredicateQueryFactory returns queries for the predicates
     * @param emptyWasClauseOperandHandler handler for WAS EMPTY queries
     * @param changeHistoryFieldIdResolver
     */
    public WasClauseQueryFactory(final SearchProviderFactory searchProviderFactory,
            final JqlOperandResolver operandResolver,
            final HistoryPredicateQueryFactory wasPredicateQueryFactory,
            final EmptyWasClauseOperandHandler emptyWasClauseOperandHandler,
            final ChangeHistoryFieldIdResolver changeHistoryFieldIdResolver)
    {
        this.searchProviderFactory = searchProviderFactory;
        this.operandResolver = operandResolver;
        this.changeHistoryFieldIdResolver = changeHistoryFieldIdResolver;
        this.wasPredicateQueryFactory = wasPredicateQueryFactory;
        this.emptyWasClauseOperandHandler = emptyWasClauseOperandHandler;
    }


    /**
     * @param searcher the {@link User} representing the current searcher
     * @param clause the search cluase , for instance "Status was Open"
     * @return {@link QueryFactoryResult} that wraps the  Lucene Query
     */
    public QueryFactoryResult create(final User searcher, final WasClause clause)
    {
        ConstantScoreQuery issueQuery;
        Query historyQuery = makeQuery(searcher, clause);
        IndexSearcher historySearcher = searchProviderFactory.getSearcher(SearchProviderFactory.CHANGE_HISTORY_INDEX);
        Set<String> issueIds;
        final Set<String> queryIds = new HashSet<String>();
        final Set<String> allIssueIds = new HashSet<String>();
        Collector Collector = new DocumentHitCollector(historySearcher)
        {
            @Override
            public void collect(Document doc)
            {
                queryIds.add(doc.get(DocumentConstants.ISSUE_ID));
            }
        };
        Collector allDocsCollector = new DocumentHitCollector(historySearcher)
        {
            @Override
            public void collect(Document doc)
            {
                allIssueIds.add(doc.get(DocumentConstants.ISSUE_ID));
            }
        };
        try
        {
            if (log.isDebugEnabled())
            {
                log.debug("Running history query (" + clause + "): " + historyQuery);
            }

            historySearcher.search(historyQuery, Collector);
            if (clause.getOperator() == Operator.WAS || clause.getOperator() == Operator.WAS_IN)
            {
                issueIds = queryIds;
            }
            else
            {
                historySearcher.search(new MatchAllDocsQuery(), allDocsCollector);
                allIssueIds.removeAll(queryIds);
                issueIds = allIssueIds;
            }

            if (log.isDebugEnabled())
            {
                log.debug("History query returned: " + issueIds);
            }

            issueQuery = new ConstantScoreQuery(new IssueIdFilter(issueIds));
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
        return new QueryFactoryResult(issueQuery);

    }

    private Query makeQuery(final User searcher, final WasClause clause)
    {
        final BooleanQuery outerWasQuery = new BooleanQuery();
        final boolean isEmptyOperand = clause.getOperand() instanceof EmptyOperand;
        final List<QueryLiteral> literals = new ArrayList<QueryLiteral>();
        if (isEmptyOperand)
        {
            literals.addAll(emptyWasClauseOperandHandler.getEmptyValue(clause));
        }
        else if (clause.getOperand() instanceof MultiValueOperand)
        {
            literals.addAll(operandResolver.getValues(searcher, clause.getOperand(), clause));
        }
        else if (clause.getOperand() instanceof FunctionOperand)
        {
            literals.addAll(operandResolver.getValues(searcher, clause.getOperand(), clause));
        }
        else
        {
            literals.add(operandResolver.getSingleValue(searcher, clause.getOperand(), clause));
        }
        for (QueryLiteral literal : literals)
        {
            BooleanQuery wasQuery = new BooleanQuery();
            Collection<String> ids = changeHistoryFieldIdResolver.resolveIdsForField(clause.getField(), literal, isEmptyOperand);
            for (String id : ids)
            {
                final TermQuery fromQuery = createTermQuery(clause, id, DocumentConstants.OLD_VALUE);
                final TermQuery toQuery = createTermQuery(clause, id, DocumentConstants.NEW_VALUE);
                //  searches for status was EMPTY will have null ids
                if (id != null && clause.getPredicate() == null)
                {
                    wasQuery.add(fromQuery, BooleanClause.Occur.SHOULD);
                }
                wasQuery.add(toQuery, BooleanClause.Occur.SHOULD);
            }
            // JRADEV-7161 : need to should between each id, rather than at the end
            if (clause.getPredicate() != null)
            {
                final BooleanQuery wasPredicateQuery = wasPredicateQueryFactory.makePredicateQuery(searcher, clause.getField().toLowerCase(), clause.getPredicate(), false);
                final BooleanQuery wasWithPredicateQuery = new BooleanQuery();
                wasWithPredicateQuery.add(wasQuery, BooleanClause.Occur.MUST);
                wasWithPredicateQuery.add(wasPredicateQuery, BooleanClause.Occur.MUST);
                wasQuery = wasWithPredicateQuery;
            }
            outerWasQuery.add(wasQuery, BooleanClause.Occur.SHOULD);
        }
        return outerWasQuery;
    }


    private TermQuery createTermQuery(WasClause clause, String value, String documentField)
    {
        return new TermQuery(new Term(clause.getField().toLowerCase() + "." + documentField, encodeProtocol(value)));
    }

    private String encodeProtocol(String changeItem)
    {
        return DocumentConstants.CHANGE_HISTORY_PROTOCOL + (changeItem == null ? "" : changeItem.toLowerCase());
    }
}



