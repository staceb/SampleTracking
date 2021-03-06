package com.atlassian.jira.sharing.type;

import com.atlassian.jira.user.MockCrowdService;
import org.junit.After;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import com.atlassian.jira.MockProviderAccessor;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.MockJiraServiceContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.sharing.SharePermission;
import com.atlassian.jira.sharing.SharePermissionImpl;
import com.atlassian.jira.sharing.search.GlobalShareTypeSearchParameter;
import com.atlassian.jira.sharing.search.PrivateShareTypeSearchParameter;
import com.atlassian.jira.sharing.type.ShareType.Name;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.P;
import com.opensymphony.user.User;
import com.atlassian.jira.local.ListeningTestCase;

/**
 * Unit tests for {@link GlobalShareTypeValidator}
 * 
 * @since v3.13
 */
public class TestGlobalShareTypeValidator extends ListeningTestCase
{
    protected User user;
    protected Mock permMgrMock;
    private static final SharePermission GLOBAL_PERM = new SharePermissionImpl(GlobalShareType.TYPE, null, null);
    private static final SharePermission INVALID_PERM = new SharePermissionImpl(new Name("group"), "developers", null);
    protected JiraServiceContext ctx;

    @Before
    public void setUp() throws Exception
    {
        final MockProviderAccessor mpa = new MockProviderAccessor();
        user = new User("admin", mpa, new MockCrowdService());
        permMgrMock = new Mock(PermissionManager.class);
        permMgrMock.setStrict(true);
        ctx = new MockJiraServiceContext(user);
    }

    @After
    public void tearDown() throws Exception
    {
        user = null;
        permMgrMock = null;
        ctx = null;
    }

    @Test
    public void testCheckSharePermissionWithValidSharePermissionAndPermission()
    {

        permMgrMock.expectAndReturn("hasPermission", P.args(P.eq(new Integer(Permissions.CREATE_SHARED_OBJECTS)), P.eq(user)), Boolean.TRUE);
        final PermissionManager permissionManager = (PermissionManager) permMgrMock.proxy();
        final ShareTypeValidator validator = new GlobalShareTypeValidator(permissionManager);

        assertTrue(validator.checkSharePermission(ctx, TestGlobalShareTypeValidator.GLOBAL_PERM));

        assertFalse(ctx.getErrorCollection().hasAnyErrors());

        permMgrMock.verify();
    }

    @Test
    public void testCheckSharePermissionWithValidSharePermissionAndNoPermission()
    {
        permMgrMock.expectAndReturn("hasPermission", P.args(P.eq(new Integer(Permissions.CREATE_SHARED_OBJECTS)), P.eq(user)), Boolean.FALSE);
        final PermissionManager permissionManager = (PermissionManager) permMgrMock.proxy();
        final ShareTypeValidator validator = new GlobalShareTypeValidator(permissionManager);

        assertFalse(validator.checkSharePermission(ctx, TestGlobalShareTypeValidator.GLOBAL_PERM));
        assertTrue(ctx.getErrorCollection().hasAnyErrors());

        permMgrMock.verify();
    }

    @Test
    public void testCheckSharePermissionWithNoValidSharePermission()
    {
        // Short circuit means this wont be called
        // permMgrMock.expectAndReturn("hasPermission", P.args(P.eq(new Integer(Permissions.CREATE_SHARED_OBJECTS)), P.eq(user)), Boolean.TRUE);
        final PermissionManager permissionManager = (PermissionManager) permMgrMock.proxy();
        final ShareTypeValidator validator = new GlobalShareTypeValidator(permissionManager);

        try
        {
            validator.checkSharePermission(ctx, TestGlobalShareTypeValidator.INVALID_PERM);
            fail("checkSharePermission should have thrown IllegalArgumentException illegal permission");
        }
        catch (final IllegalArgumentException e)
        {
            // expected
        }

        assertFalse(ctx.getErrorCollection().hasAnyErrors());
        permMgrMock.verify();
    }

    @Test
    public void testCheckSharePermissionNullPermission()
    {
        final ShareTypeValidator validator = new GlobalShareTypeValidator(null);

        try
        {
            validator.checkSharePermission(ctx, null);
            fail("checkSharePermission should not accept null permission");
        }
        catch (final IllegalArgumentException e)
        {
            // expected
        }
        assertFalse(ctx.getErrorCollection().hasAnyErrors());
        permMgrMock.verify();
    }

    @Test
    public void testCheckSharePermissionNullContext()
    {
        final ShareTypeValidator validator = new GlobalShareTypeValidator(null);

        try
        {
            validator.checkSharePermission(null, TestGlobalShareTypeValidator.GLOBAL_PERM);
            fail("checkSharePermission should not accept null ctx");
        }
        catch (final IllegalArgumentException e)
        {
            // expected
        }
        assertFalse(ctx.getErrorCollection().hasAnyErrors());
        permMgrMock.verify();
    }

    @Test
    public void testCheckSharePermissionNullUserInContext()
    {
        final ShareTypeValidator validator = new GlobalShareTypeValidator(null);

        final JiraServiceContext nullUserCtx = new JiraServiceContextImpl(null);

        try
        {
            validator.checkSharePermission(nullUserCtx, TestGlobalShareTypeValidator.GLOBAL_PERM);
            fail("checkSharePermission should not accept null user for ctx");
        }
        catch (final IllegalArgumentException e)
        {
            // expected
        }
        assertFalse(ctx.getErrorCollection().hasAnyErrors());
        permMgrMock.verify();
    }

    @Test
    public void testCheckGoodUser()
    {
        final ShareTypeValidator validator = new GlobalShareTypeValidator(null);

        final boolean result = validator.checkSearchParameter(ctx, GlobalShareTypeSearchParameter.GLOBAL_PARAMETER);
        assertTrue(result);
    }

    @Test
    public void testCheckAnonymous()
    {
        final ShareTypeValidator validator = new GlobalShareTypeValidator(null);
        final JiraServiceContext nullUserCtx = new JiraServiceContextImpl(null);

        final boolean result = validator.checkSearchParameter(nullUserCtx, GlobalShareTypeSearchParameter.GLOBAL_PARAMETER);
        assertTrue(result);

    }

    @Test
    public void testInvalidArgument()
    {
        final ShareTypeValidator validator = new GlobalShareTypeValidator(null);

        try
        {
            validator.checkSearchParameter(ctx, PrivateShareTypeSearchParameter.PRIVATE_PARAMETER);
            fail("Should not accept invalid search parameter.");
        }
        catch (final IllegalArgumentException e)
        {
            // expected.
        }
    }
}
