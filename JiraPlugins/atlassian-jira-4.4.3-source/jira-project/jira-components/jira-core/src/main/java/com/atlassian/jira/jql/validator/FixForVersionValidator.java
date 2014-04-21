package com.atlassian.jira.jql.validator;

import com.atlassian.jira.jql.operand.JqlOperandResolver;
import com.atlassian.jira.jql.resolver.VersionResolver;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.PermissionManager;

/**
 * The Fix Version clause validator.
 *
 * @since v4.0
 */
public class FixForVersionValidator extends AbstractVersionValidator
{
    ///CLOVER:OFF
    public FixForVersionValidator(final VersionResolver versionResolver, final JqlOperandResolver operandResolver,
            final PermissionManager permissionManager, final VersionManager versionManager)
    {
        super(versionResolver, operandResolver, permissionManager, versionManager);
    }
    ///CLOVER:ON
}
