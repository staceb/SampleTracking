package com.atlassian.jira.jql.validator;

import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.resolver.VersionResolver;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.util.I18nHelper;

/**
 * The Affected Version clause validator.
 *
 * @since v4.0
 */
public class AffectedVersionValidator extends AbstractVersionValidator
{
    ///CLOVER:OFF
    public AffectedVersionValidator(final VersionResolver versionResolver, final JqlOperandResolver operandResolver,
            final PermissionManager permissionManager, final VersionManager versionManager, I18nHelper.BeanFactory beanFactory)
    {
        super(versionResolver, operandResolver, permissionManager, versionManager, beanFactory);
    }

    ///CLOVER:ON
}
