package com.atlassian.jira.jql.validator;


import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.index.IndexedChangeHistoryFieldManager;
import com.atlassian.jira.jql.operand.PredicateOperandResolver;
import com.atlassian.jira.jql.operand.registry.JqlFunctionHandlerRegistry;
import com.atlassian.jira.jql.util.JqlDateSupport;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.util.MessageSetImpl;
import com.atlassian.jira.web.bean.I18nBean;
import com.atlassian.query.clause.ChangedClause;


/**
 * Validat the chnaged clause against any field.
 *
 * @since v5.0
 */
public class ChangedClauseValidator 
{
    private final IndexedChangeHistoryFieldManager indexedChangeHistoryFieldManager;
    private final PredicateOperandResolver predicateOperandResolver;
    private final JqlDateSupport jqlDateSupport;
    private final HistoryPredicateValidator  historyPredicateValidator;
    private final JiraAuthenticationContext authContext;
    private final HistoryFieldValueValidator historyFieldValueValidator;
    private final JqlFunctionHandlerRegistry registry;


    public ChangedClauseValidator(
            final IndexedChangeHistoryFieldManager indexedChangeHistoryFieldManager,
            final PredicateOperandResolver predicateOperandResolver,
            final JqlDateSupport jqlDateSupport,
            final JiraAuthenticationContext authContext,
            final HistoryFieldValueValidator historyFieldValueValidator,
            final JqlFunctionHandlerRegistry registry)
    {
        this.indexedChangeHistoryFieldManager = indexedChangeHistoryFieldManager;
        this.predicateOperandResolver = predicateOperandResolver;
        this.jqlDateSupport = jqlDateSupport;
        this.authContext = authContext;
        this.historyFieldValueValidator = historyFieldValueValidator;
        this.registry = registry;
        this.historyPredicateValidator = getHistoryPredicateValidator();
    }

    private HistoryPredicateValidator getHistoryPredicateValidator()
    {
        return new HistoryPredicateValidator(authContext, predicateOperandResolver, jqlDateSupport, historyFieldValueValidator, registry);
    }

    public MessageSet validate(final User searcher, final ChangedClause clause)
    {
        final MessageSet messageSet = new MessageSetImpl();
        validateField(searcher, clause.getField(), messageSet);
        if (clause.getPredicate() != null)
        {
           messageSet.addMessageSet(historyPredicateValidator.validate(searcher, clause, clause.getPredicate()));
        }
        return messageSet;
    }

    private void validateField(User searcher, String fieldName, MessageSet messages)
    {
        if (!indexedChangeHistoryFieldManager.getIndexedChangeHistoryFieldNames().contains(fieldName.toLowerCase()))
        {
            messages.addErrorMessage(getI18n(searcher).getText("jira.jql.history.field.not.supported", fieldName));
        }
    }


    I18nHelper getI18n(User user)
    {
        return new I18nBean(user);
    }


}

