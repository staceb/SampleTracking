package com.atlassian.jira.jql.validator;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.util.JqlTimetrackingDurationSupport;
import com.atlassian.jira.util.InjectableComponent;

/**
 * Time Spent validator
 *
 * @since v4.0
 */
@InjectableComponent
public class TimeSpentValidator extends AbstractTimeTrackingValidator
{
    ///CLOVER:OFF
    public TimeSpentValidator(final JqlOperandResolver operandResolver,
            final ApplicationProperties applicationProperties,
            final JqlTimetrackingDurationSupport jqlTimetrackingDurationSupport)
    {
        super(operandResolver, applicationProperties, jqlTimetrackingDurationSupport);
    }
    ///CLOVER:ON
}
