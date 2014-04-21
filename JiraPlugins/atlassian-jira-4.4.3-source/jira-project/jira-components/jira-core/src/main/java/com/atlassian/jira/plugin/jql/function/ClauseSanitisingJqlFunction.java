package com.atlassian.jira.plugin.jql.function;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.util.NotNull;
import com.atlassian.query.operand.FunctionOperand;

/**
 * <p>An additional interface which can be implemented by {@link com.atlassian.jira.plugin.jql.function.JqlFunction}
 * classes in order to indicate to the {@link com.atlassian.jira.jql.operand.JqlOperandResolver} that their arguments
 * are able to be sanitised if necessary.
 *
 * <p>This was not added to the {@link com.atlassian.jira.plugin.jql.function.JqlFunction} interface as the default
 * behaviour is not to care about sanitising, and we didn't want to bloat the plugin point.
 *
 * @see com.atlassian.jira.plugin.jql.function.JqlFunction
 * @see com.atlassian.jira.jql.operand.JqlOperandResolver#sanitiseFunctionOperand(com.opensymphony.user.User, com.atlassian.query.operand.FunctionOperand)
 * @since v4.0
 */
public interface ClauseSanitisingJqlFunction
{
    /**
     * Sanitise a function operand for the specified user, so that information is not leaked.
     *
     * @param searcher the user performing the search
     * @param operand the operand to sanitise; will only be sanitised if valid
     * @return the sanitised operand; never null.
     */
    @NotNull FunctionOperand sanitiseOperand(User searcher, @NotNull FunctionOperand operand);
}
