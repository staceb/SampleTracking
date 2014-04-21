package com.atlassian.jira.issue.customfields.searchers;

import com.atlassian.jira.jql.query.ClauseQueryFactory;
import com.atlassian.jira.jql.validator.ClauseValidator;
import com.atlassian.jira.JiraDataType;
import com.atlassian.query.operator.Operator;

import java.util.Set;

/**
 * Provides access to objects that can perform validation and query generation for clauses generated by this searcher.
 *
 * @since v4.0
 */
public interface CustomFieldSearcherClauseHandler
{
    /**
     * Provides a validator for {@link com.atlassian.query.clause.TerminalClause}'s created by this searcher.
     *
     * @return a validator for {@link com.atlassian.query.clause.TerminalClause}'s created by this searcher.
     */
    ClauseValidator getClauseValidator();

    /**
     * Provides a lucene query generator for {@link com.atlassian.query.clause.TerminalClause}'s created by this searcher.
     *
     * @return a lucene query generator for {@link com.atlassian.query.clause.TerminalClause}'s created by this searcher.
     */
    ClauseQueryFactory getClauseQueryFactory();

    /**
     * Provides a set of the supported {@link com.atlassian.query.operator.Operator}'s that this custom field searcher
     * can handle for its searching.
     *
     * This will be used to populate the {@link com.atlassian.jira.jql.ClauseInformation#getSupportedOperators()}.
     *
     * @return a set of supported operators.
     */
    Set<Operator> getSupportedOperators();

    /**
     * Provides the {@link com.atlassian.jira.JiraDataType} that this clause handles and searches on. This allows us
     * to infer some information about how the search will behave and how it will interact with other elements in
     * the system.
     *
     * This will be used to populate the {@link com.atlassian.jira.jql.ClauseInformation#getDataType()}.
     *
     * @return the JiraDataType that this clause can handle.
     */
    JiraDataType getDataType();

}
