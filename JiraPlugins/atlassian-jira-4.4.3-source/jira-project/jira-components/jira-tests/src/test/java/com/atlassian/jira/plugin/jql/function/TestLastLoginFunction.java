package com.atlassian.jira.plugin.jql.function;

import com.atlassian.jira.JiraDataTypes;
import com.atlassian.jira.MockProviderAccessor;
import com.atlassian.jira.bc.security.login.LoginInfo;
import com.atlassian.jira.bc.security.login.LoginService;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.jql.query.QueryCreationContext;
import com.atlassian.jira.jql.query.QueryCreationContextImpl;
import com.atlassian.jira.local.ListeningTestCase;
import com.atlassian.jira.user.MockCrowdService;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.MessageSet;
import com.atlassian.jira.web.bean.MockI18nBean;
import com.atlassian.query.clause.TerminalClause;
import com.atlassian.query.operand.FunctionOperand;
import com.opensymphony.user.User;
import org.easymock.EasyMock;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @since v4.1
 */
public class TestLastLoginFunction extends ListeningTestCase
{
    private TerminalClause terminalClause = null;

    @Test
    public void testValidateTooManyArguments() throws Exception
    {
        LastLoginFunction loginFunction = new LastLoginFunction(null)
        {
            @Override
            protected I18nHelper getI18n()
            {
                return new MockI18nBean();
            }
        };
        FunctionOperand function = new FunctionOperand(LastLoginFunction.FUNCTION_LAST_LOGIN, Arrays.asList("should", "not", "be", "here"));
        final MessageSet messageSet = loginFunction.validate(null, function, terminalClause);
        assertTrue(messageSet.hasAnyMessages());
        assertTrue(messageSet.hasAnyErrors());
        assertFalse(messageSet.hasAnyWarnings());
        assertEquals(1, messageSet.getErrorMessages().size());
        assertEquals("Function 'lastLogin' expected '0' arguments but received '4'.", messageSet.getErrorMessages().iterator().next());
    }

    @Test
    public void testDataType() throws Exception
    {
        LastLoginFunction handler = new LastLoginFunction(null);
        assertEquals(JiraDataTypes.DATE, handler.getDataType());
    }

    @Test
    public void testGetValuesNullContext() throws Exception
    {
        LastLoginFunction loginFunction = new LastLoginFunction(null);
        FunctionOperand function = new FunctionOperand(LastLoginFunction.FUNCTION_LAST_LOGIN, Collections.<String>emptyList());
        final List<QueryLiteral> value = loginFunction.getValues(null, function, terminalClause);
        assertNotNull(value);
        assertEquals(0, value.size());
    }

    @Test
    public void testGetValuesNullUser() throws Exception
    {
        LastLoginFunction loginFunction = new LastLoginFunction(null);
        FunctionOperand function = new FunctionOperand(LastLoginFunction.FUNCTION_LAST_LOGIN, Collections.<String>emptyList());
        final QueryCreationContext context = new QueryCreationContextImpl(null);
        final List<QueryLiteral> value = loginFunction.getValues(context, function, terminalClause);
        assertNotNull(value);
        assertEquals(0, value.size());
    }

    @Test
    public void testGetValues() throws Exception
    {
        final User user = new User("bob", new MockProviderAccessor(), new MockCrowdService());

        final LoginService loginService = EasyMock.createMock(LoginService.class);
        final long LOGIN = 1234567890000L;
        final LoginInfo loginInfo = new LoginInfo() {
            public Long getLastLoginTime()
            {
                return 2345678900000L;
            }

            public Long getPreviousLoginTime()
            {
                return LOGIN;
            }

            public Long getLastFailedLoginTime()
            {
                return null;
            }

            public Long getLoginCount()
            {
                return 2L;
            }

            public Long getCurrentFailedLoginCount()
            {
                return null;
            }

            public Long getTotalFailedLoginCount()
            {
                return null;
            }

            public Long getMaxAuthenticationAttemptsAllowed()
            {
                return null;
            }

            public boolean isElevatedSecurityCheckRequired()
            {
                return false;
            }
        };
        EasyMock.expect(loginService.getLoginInfo(user.getName())).andReturn(loginInfo);

        EasyMock.replay(loginService);

        LastLoginFunction loginFunction = new LastLoginFunction(loginService);
        FunctionOperand function = new FunctionOperand(LastLoginFunction.FUNCTION_LAST_LOGIN, Collections.<String>emptyList());

        final QueryCreationContext context = new QueryCreationContextImpl(user);
        final List<QueryLiteral> value = loginFunction.getValues(context, function, terminalClause);
        assertNotNull(value);
        assertEquals(1, value.size());
        assertEquals(LOGIN, value.get(0).getLongValue().longValue());

        EasyMock.verify(loginService);
    }

    @Test
    public void testGetMinimumNumberOfExpectedArguments() throws Exception
    {
        LastLoginFunction loginFunction = new LastLoginFunction(null);
        assertEquals(0, loginFunction.getMinimumNumberOfExpectedArguments());
    }
}
