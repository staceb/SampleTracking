package com.atlassian.jira.bc.workflow;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.local.runner.ListeningMockitoRunner;
import com.atlassian.jira.mock.i18n.MockI18nHelper;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.MockUser;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.web.bean.MockI18nBean;
import com.atlassian.jira.workflow.JiraWorkflow;
import com.atlassian.jira.workflow.MockJiraWorkflow;
import com.atlassian.jira.workflow.WorkflowException;
import com.atlassian.jira.workflow.WorkflowManager;
import com.google.common.collect.Lists;
import com.opensymphony.workflow.loader.DescriptorFactory;
import com.opensymphony.workflow.loader.StepDescriptor;
import com.opensymphony.workflow.loader.WorkflowDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.google.common.collect.Iterables.getFirst;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(ListeningMockitoRunner.class)
public class TestDefaultWorkflowService
{
    private I18nHelper mockI18nHelper;

    @Mock private WorkflowManager mockWorkflowManager;
    @Mock private PermissionManager mockPermissionManager;

    @Mock private JiraWorkflow mockJiraWorkflow;
    @Mock private JiraWorkflow mockDraftWorkflow;


    @Before
    public void setUp() throws Exception
    {
        mockI18nHelper = new MockI18nHelper();
    }

    private DefaultWorkflowService createWorkflowServiceOverridingPermissionCheck(final boolean hasAdminPermission)
    {
        return new DefaultWorkflowService(mockWorkflowManager, null, null)
        {
            @Override
            I18nHelper getI18nBean()
            {
                return mockI18nHelper;
            }

            @Override
            boolean hasAdminPermission(final JiraServiceContext jiraServiceContext)
            {
                return hasAdminPermission;
            }
        };
    }

    private DefaultWorkflowService createWorkflowServiceWithRealPermissionCheck()
    {
        return new DefaultWorkflowService(mockWorkflowManager, null, mockPermissionManager)
        {
            @Override
            I18nHelper getI18nBean()
            {
                return mockI18nHelper;
            }
        };
    }

    private void stubHasAllPermisions()
    {
        when(mockPermissionManager.hasPermission(anyInt(), Matchers.any(User.class))).thenReturn(true);
    }

    private JiraServiceContextImpl createServiceContext(User testUser)
    {
        return new JiraServiceContextImpl(testUser,  new SimpleErrorCollection());
    }

    private String getFirstMessage(JiraServiceContext jiraServiceContext)
    {
        return getFirst(jiraServiceContext.getErrorCollection().getErrorMessages(), null);
    }

    private void assertNoErrors(JiraServiceContext jiraServiceContext)
    {
        assertFalse("Expected no errors in " + jiraServiceContext, jiraServiceContext.getErrorCollection().hasAnyErrors());
    }

    /**
     * Asserts that the given service context has error collection with exactly one error message equal to
     * <tt>expectedMessage</tt>
     *
     * @param expectedMessage expected message
     * @param serviceContext service context to check
     */
    private void assertErrorMessage(final String expectedMessage, final JiraServiceContext serviceContext)
    {
        assertErrorMessage(expectedMessage, serviceContext.getErrorCollection());
    }

    /**
     * Asserts that the given ErrorCollection contains only the given message.
     *
     * @param message         Expected message
     * @param errorCollection Actual ErrorCollection
     */
    private void assertErrorMessage(final String message, final ErrorCollection errorCollection)
    {
        assertEquals("Expected 1 error message", 1, errorCollection.getErrorMessages().size());
        // We only expect "error messages", not "errors"
        if (errorCollection.getErrors().isEmpty())
        {
            assertEquals(message, errorCollection.getErrorMessages().iterator().next());
        }
        else
        {
            fail("ErrorCollection was only expected to contain an error of type 'ErrorMessage', but it contains type"
                    + " 'Error' as well.");
        }
    }

    private void resetErrorCollection(JiraServiceContext serviceContext)
    {
        serviceContext.getErrorCollection().setErrorMessages(Lists.<String>newArrayList());
        serviceContext.getErrorCollection().getErrors().clear();
    }

    @Test
    public void testGetDraftWorkflow()
    {
        when(mockWorkflowManager.getWorkflow("testworkflow")).thenReturn(mockJiraWorkflow);
        when(mockWorkflowManager.getDraftWorkflow("testworkflow")).thenReturn(mockDraftWorkflow);

        final User testUser = new MockUser("testuser");
        final ErrorCollection errorCollection = new SimpleErrorCollection();

        final JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(testUser, errorCollection);
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);

        assertSame(mockDraftWorkflow, workflowService.getDraftWorkflow(jiraServiceContext, "testworkflow"));
        assertFalse(jiraServiceContext.getErrorCollection().hasAnyErrors());
    }

    @Test
    public void testGetDraftWorkflowWithNoAdminPermission()
    {
        final User testUser = new MockUser("testuser");
        final ErrorCollection errorCollection = new SimpleErrorCollection();

        final JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(testUser, errorCollection);
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(false);

        assertNull(workflowService.getDraftWorkflow(jiraServiceContext, "testworkflow"));
        assertTrue(jiraServiceContext.getErrorCollection().hasAnyErrors());
        assertEquals("admin.workflows.service.error.no.admin.permission", getFirstMessage(jiraServiceContext));
    }

    @Test
    public void testGetDraftWorkflowWithNoParent()
    {
        when(mockWorkflowManager.getWorkflow(anyString())).thenReturn(null);

        final User testUser = new MockUser("testuser");
        final ErrorCollection errorCollection = new SimpleErrorCollection();
        final JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(testUser, errorCollection);
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);

        assertNull(workflowService.getDraftWorkflow(jiraServiceContext, "testworkflow"));
        assertTrue(jiraServiceContext.getErrorCollection().hasAnyErrors());
        assertEquals("admin.workflows.service.error.retrieve.no.parent", jiraServiceContext.getErrorCollection().getErrorMessages().iterator().next());

        jiraServiceContext.getErrorCollection().setErrorMessages(Lists.<String>newArrayList());
        assertNull(workflowService.getDraftWorkflow(jiraServiceContext, null));
        assertEquals("admin.workflows.service.error.no.parent", getFirstMessage(jiraServiceContext));

        jiraServiceContext.getErrorCollection().setErrorMessages(Lists.<String>newArrayList());
        workflowService.getDraftWorkflow(jiraServiceContext, "");
        assertEquals("admin.workflows.service.error.no.parent", getFirstMessage(jiraServiceContext));
    }

    @Test
    public void testCreateDraftWorkflow()
    {
        when(mockWorkflowManager.getWorkflow("testworkflow")).thenReturn(mockJiraWorkflow);
        when(mockWorkflowManager.isActive(mockJiraWorkflow)).thenReturn(true);
        when(mockWorkflowManager.createDraftWorkflow("testuser", "testworkflow")).thenReturn(mockDraftWorkflow);

        final User testUser = new MockUser("testuser");
        final ErrorCollection errorCollection = new SimpleErrorCollection();
        final JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(testUser, errorCollection);
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);

        assertSame(mockDraftWorkflow, workflowService.createDraftWorkflow(jiraServiceContext, "testworkflow"));
        assertFalse(jiraServiceContext.getErrorCollection().hasAnyErrors());
    }

    @Test
    public void testCreateDraftWorkflowNoAdminPermission()
    {
        final User testUser = new MockUser("testuser");
        final ErrorCollection errorCollection = new SimpleErrorCollection();
        final JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(testUser, errorCollection);
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(false);

        assertNull(workflowService.createDraftWorkflow(jiraServiceContext, "testworkflow"));
        assertTrue(jiraServiceContext.getErrorCollection().hasAnyErrors());
        assertEquals("admin.workflows.service.error.no.admin.permission",
                jiraServiceContext.getErrorCollection().getErrorMessages().iterator().next());
    }

    @Test
    public void testCreateDraftWorkflowWithNullUser()
    {
        when(mockWorkflowManager.getWorkflow(anyString())).thenReturn(mockJiraWorkflow);
        when(mockWorkflowManager.isActive(mockJiraWorkflow)).thenReturn(true);
        when(mockWorkflowManager.createDraftWorkflow("", "testworkflow")).thenReturn(mockDraftWorkflow);

        final ErrorCollection errorCollection = new SimpleErrorCollection();
        final JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(null, errorCollection);
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);

        assertSame(mockDraftWorkflow, workflowService.createDraftWorkflow(jiraServiceContext, "testworkflow"));
        assertFalse(jiraServiceContext.getErrorCollection().hasAnyErrors());
    }

    @Test
    public void testCreateDraftWorkflowWithNoParentWorkflow()
    {
        when(mockWorkflowManager.getWorkflow(anyString())).thenReturn(null);

        final User testUser = new MockUser("testuser");
        final ErrorCollection errorCollection = new SimpleErrorCollection();
        final JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(testUser, errorCollection);
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);

        assertNull(workflowService.createDraftWorkflow(jiraServiceContext, "testworkflow"));
        assertTrue(jiraServiceContext.getErrorCollection().hasAnyErrors());
        assertEquals("admin.workflows.service.error.no.parent", getFirstMessage(jiraServiceContext));

        jiraServiceContext.getErrorCollection().setErrorMessages(Lists.<String>newArrayList());
        workflowService.createDraftWorkflow(jiraServiceContext, null);
        assertEquals("admin.workflows.service.error.no.parent", getFirstMessage(jiraServiceContext));

        jiraServiceContext.getErrorCollection().setErrorMessages(Lists.<String>newArrayList());
        workflowService.createDraftWorkflow(jiraServiceContext, "");
        assertEquals("admin.workflows.service.error.no.parent", getFirstMessage(jiraServiceContext));

    }

    @Test
    public void testCreateDraftWorkflowWithInactiveParentWorkflow()
    {
        when(mockWorkflowManager.getWorkflow("testworkflow")).thenReturn(mockJiraWorkflow);
        when(mockWorkflowManager.isActive(mockJiraWorkflow)).thenReturn(false);

        final User testUser = new MockUser("testuser");
        final ErrorCollection errorCollection = new SimpleErrorCollection();
        final JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(testUser, errorCollection);
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);

        assertNull(workflowService.createDraftWorkflow(jiraServiceContext, "testworkflow"));
        assertTrue(jiraServiceContext.getErrorCollection().hasAnyErrors());
        assertEquals("admin.workflows.service.error.parent.not.active", getFirstMessage(jiraServiceContext));
    }

    @Test
    public void testCreateDraftWorkflowIllegalState()
    {
        when(mockWorkflowManager.getWorkflow("testworkflow")).thenReturn(mockJiraWorkflow);
        when(mockWorkflowManager.isActive(mockJiraWorkflow)).thenReturn(true);
        //this may happen if two users try to create 2 drafts at the same time.
        when(mockWorkflowManager.createDraftWorkflow("testuser", "testworkflow")).thenThrow(new IllegalStateException("Draft already exists"));

        final User testUser = new MockUser("testuser");
        final ErrorCollection errorCollection = new SimpleErrorCollection();
        final JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(testUser, errorCollection);
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);

        assertNull(workflowService.createDraftWorkflow(jiraServiceContext, "testworkflow"));
        assertErrorMessage("admin.workflows.service.error.draft.exists.or.workflow.not.active", jiraServiceContext);
    }

    @Test
    public void testDeleteDraftWorkflow()
    {
        when(mockWorkflowManager.deleteDraftWorkflow("testWorkflow")).thenReturn(true);
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);
        assertTrue(workflowService.deleteDraftWorkflow(null, "testWorkflow"));
        verify(mockWorkflowManager).deleteDraftWorkflow("testWorkflow");
        verifyNoMoreInteractions(mockWorkflowManager);
    }

    @Test
    public void testDeleteDraftWorkflowWithNoAdminPermission()
    {
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(false);

        final User testUser = new MockUser("testuser");
        final ErrorCollection errorCollection = new SimpleErrorCollection();
        final JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(testUser, errorCollection);
        assertFalse(workflowService.deleteDraftWorkflow(jiraServiceContext, "testWorkflow"));
        assertErrorMessage("admin.workflows.service.error.no.admin.permission", jiraServiceContext);
    }

    @Test
    public void testDeleteDraftWorkflowWithNullParent()
    {
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);
        final JiraServiceContext jiraServiceContext = createServiceContext(null);
        workflowService.deleteDraftWorkflow(jiraServiceContext, null);

        assertTrue(jiraServiceContext.getErrorCollection().hasAnyErrors());
        assertErrorMessage("admin.workflows.service.error.delete.no.parent", jiraServiceContext);
    }

    @Test
    public void testUpdateDraftWorkflow()
    {
        when(mockDraftWorkflow.getDescriptor()).thenReturn(new DescriptorFactory().createWorkflowDescriptor());
        when(mockDraftWorkflow.isEditable()).thenReturn(true);

        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);
        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);
        workflowService.updateWorkflow(jiraServiceContext, mockDraftWorkflow);

        assertFalse(jiraServiceContext.getErrorCollection().hasAnyErrors());
        verify(mockWorkflowManager).updateWorkflow("testuser", mockDraftWorkflow);
    }

    @Test
    public void testUpdateWorkflowNullWorkflow()
    {
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);

        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);
        workflowService.updateWorkflow(jiraServiceContext, null);
        assertErrorMessage("admin.workflows.service.error.update.no.workflow", jiraServiceContext);
    }

    @Test
    public void testUpateWorkflowNullDescriptor()
    {
        when(mockDraftWorkflow.getDescriptor()).thenReturn(null);
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);

        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);
        workflowService.updateWorkflow(jiraServiceContext, mockDraftWorkflow);

        assertErrorMessage("admin.workflows.service.error.update.no.workflow", jiraServiceContext);
    }

    @Test
    public void testUpdateWorkflowNoAdminPermission()
    {
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(false);

        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);
        workflowService.updateWorkflow(jiraServiceContext, mockDraftWorkflow);

        assertErrorMessage("admin.workflows.service.error.no.admin.permission", jiraServiceContext);
    }

    @Test
    public void testUpdateWorkflowNullUsername()
    {
        when(mockDraftWorkflow.getDescriptor()).thenReturn(new DescriptorFactory().createWorkflowDescriptor());
        when(mockDraftWorkflow.isEditable()).thenReturn(true);

        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);
        final JiraServiceContext jiraServiceContext = createServiceContext(null);
        workflowService.updateWorkflow(jiraServiceContext, mockDraftWorkflow);
        assertNoErrors(jiraServiceContext);
        verify(mockWorkflowManager).updateWorkflow("", mockDraftWorkflow);
    }

    @Test
    public void testUpdateWorkflowNotEditable()
    {
        when(mockDraftWorkflow.getDescriptor()).thenReturn(new DescriptorFactory().createWorkflowDescriptor());
        when(mockDraftWorkflow.isEditable()).thenReturn(false);
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);
        final JiraServiceContext jiraServiceContext = createServiceContext(null);
        workflowService.updateWorkflow(jiraServiceContext, mockDraftWorkflow);
        assertErrorMessage("admin.workflows.service.error.not.editable", jiraServiceContext);
    }

    @Test
    public void testOverwriteWorkflowNullName()
    {
        final DefaultWorkflowService defaultWorkflowService = createWorkflowServiceOverridingPermissionCheck(true);
        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);

        defaultWorkflowService.overwriteActiveWorkflow(jiraServiceContext, null);
        assertErrorMessage("admin.workflows.service.error.overwrite.no.parent", jiraServiceContext);
    }

    @Test
    public void testOverwriteWorkflowNoAdminPermission()
    {
        final DefaultWorkflowService defaultWorkflowService = createWorkflowServiceOverridingPermissionCheck(false);
        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);

        defaultWorkflowService.overwriteActiveWorkflow(jiraServiceContext, "jiraworkflow");
        assertErrorMessage("admin.workflows.service.error.no.admin.permission", jiraServiceContext);
    }

    @Test
    public void testOverwriteWorkflow()
    {
        final DefaultWorkflowService defaultWorkflowService = new DefaultWorkflowService(mockWorkflowManager, null, null)
        {
            @Override
            I18nHelper getI18nBean()
            {
                return mockI18nHelper;
            }

            @Override
            boolean hasAdminPermission(final JiraServiceContext jiraServiceContext)
            {
                return true;
            }

            @Override
            public void validateOverwriteWorkflow(final JiraServiceContext jiraServiceContext, final String workflowName)
            {
                // Don't do any validation for this test.
            }
        };
        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);

        defaultWorkflowService.overwriteActiveWorkflow(jiraServiceContext, "jiraworkflow");
        assertNoErrors(jiraServiceContext);
        verify(mockWorkflowManager).overwriteActiveWorkflow("testuser", "jiraworkflow");
    }

    @Test
    public void testOverwriteWorkflowNullUser()
    {
        final DefaultWorkflowService defaultWorkflowService = new DefaultWorkflowService(mockWorkflowManager, null, null)
        {
            @Override
            I18nHelper getI18nBean()
            {
                return mockI18nHelper;
            }

            @Override
            boolean hasAdminPermission(final JiraServiceContext jiraServiceContext)
            {
                return true;
            }

            @Override
            public void validateOverwriteWorkflow(final JiraServiceContext jiraServiceContext, final String workflowName)
            {
                // Don't do any validation for this test.
            }
        };
        final JiraServiceContext jiraServiceContext = createServiceContext(null);

        defaultWorkflowService.overwriteActiveWorkflow(jiraServiceContext, "jiraworkflow");

        assertNoErrors(jiraServiceContext);
        verify(mockWorkflowManager).overwriteActiveWorkflow("", "jiraworkflow");
    }

    @Test
    public void testValidateOverwriteWorkflowNoPermission()
    {
        when(mockPermissionManager.hasPermission(anyInt(), Matchers.any(User.class))).thenReturn(false);
        final JiraServiceContext jiraServiceContext = createServiceContext(null);
        final DefaultWorkflowService defaultWorkflowService = createWorkflowServiceWithRealPermissionCheck();

        // Null workflow name is invalid
        defaultWorkflowService.validateOverwriteWorkflow(jiraServiceContext, null);
        assertErrorMessage("admin.workflows.service.error.no.admin.permission", jiraServiceContext.getErrorCollection());
    }

    @Test
    public void testValidateOverwriteWorkflowInvalidWorkflowName()
    {
        stubHasAllPermisions();
        when(mockWorkflowManager.getWorkflow("Some Rubbish")).thenReturn(null);

        final DefaultWorkflowService defaultWorkflowService = createWorkflowServiceWithRealPermissionCheck();
        final JiraServiceContext serviceContext = createServiceContext(null);

        // Null workflow name is invalid
        defaultWorkflowService.validateOverwriteWorkflow(serviceContext, null);
        assertErrorMessage("admin.workflows.service.error.overwrite.no.parent", serviceContext.getErrorCollection());

        // Empty workflow name is invalid
        resetErrorCollection(serviceContext);
        defaultWorkflowService.validateOverwriteWorkflow(serviceContext, "");
        assertErrorMessage("admin.workflows.service.error.overwrite.no.parent", serviceContext.getErrorCollection());

        // Workflow name is unknown
        resetErrorCollection(serviceContext);
        defaultWorkflowService.validateOverwriteWorkflow(serviceContext, "Some Rubbish");
        assertErrorMessage("admin.workflows.service.error.overwrite.no.parent", serviceContext.getErrorCollection());
    }

    @Test
    public void testValidateOverwriteWorkflowNoDraftWorkflow() throws Exception
    {
        stubHasAllPermisions();
        when(mockWorkflowManager.getWorkflow("My Workflow")).thenReturn(mockJiraWorkflow);
        when(mockWorkflowManager.getDraftWorkflow("My Workflow")).thenReturn(null);
        when(mockJiraWorkflow.isActive()).thenReturn(true);
        final DefaultWorkflowService defaultWorkflowService = createWorkflowServiceWithRealPermissionCheck();
        final JiraServiceContext jiraServiceContext = createServiceContext(null);

        defaultWorkflowService.validateOverwriteWorkflow(jiraServiceContext, "My Workflow");
        assertErrorMessage("admin.workflows.service.error.overwrite.no.draft", jiraServiceContext.getErrorCollection());
    }


    @Test
    public void testValidateOverwriteWorkflowInactiveParent() throws Exception
    {
        stubHasAllPermisions();
        when(mockWorkflowManager.getWorkflow("My Workflow")).thenReturn(mockJiraWorkflow);
        when(mockJiraWorkflow.isActive()).thenReturn(false);

        final DefaultWorkflowService defaultWorkflowService = createWorkflowServiceWithRealPermissionCheck();
        final JiraServiceContext jiraServiceContext = createServiceContext(null);

        defaultWorkflowService.validateOverwriteWorkflow(jiraServiceContext, "My Workflow");
        assertErrorMessage("admin.workflows.service.error.overwrite.inactive.parent", jiraServiceContext);
    }

    @Test
    public void testValidateOverwriteWorkflow() throws Exception
    {
        stubHasAllPermisions();
        final MockJiraWorkflow oldJiraWorkflow = new MockJiraWorkflow();
        final MockJiraWorkflow newJiraWorkflow = new MockJiraWorkflow();
        when(mockWorkflowManager.getWorkflow("My Workflow")).thenReturn(oldJiraWorkflow);
        when(mockWorkflowManager.getDraftWorkflow("My Workflow")).thenReturn(newJiraWorkflow);
        final AtomicBoolean validateAddWorkflowTransitionCalled = new AtomicBoolean(false);

        final DefaultWorkflowService defaultWorkflowService = new DefaultWorkflowService(mockWorkflowManager, null, mockPermissionManager)
        {
            @Override
            I18nHelper getI18nBean()
            {
                // TODO bad, it actually loads translations. We don't want that in unit tests
                return new MockI18nBean();
            }

            @Override
            public void validateAddWorkflowTransitionToDraft(JiraServiceContext jiraServiceContext, JiraWorkflow workflow, int stepId)
            {
                validateAddWorkflowTransitionCalled.set(true);
                super.validateAddWorkflowTransitionToDraft(jiraServiceContext, workflow, stepId);
            }
        };
        final JiraServiceContext serviceContext = createServiceContext(null);

        // Set up a minimal workflow
        oldJiraWorkflow.addStep(1, "Open");
        newJiraWorkflow.addStep(1, "Closed");
        defaultWorkflowService.validateOverwriteWorkflow(serviceContext, "My Workflow");
        assertErrorMessage("The draft workflow does not contain required status 'Open'.", serviceContext);

        // Now make the workflow have the required status but with wrong step ID
        resetErrorCollection(serviceContext);
        oldJiraWorkflow.clear();
        oldJiraWorkflow.addStep(1, "Open");
        newJiraWorkflow.clear();
        newJiraWorkflow.addStep(2, "Open");
        defaultWorkflowService.validateOverwriteWorkflow(serviceContext, "My Workflow");
        assertErrorMessage("You cannot change the association between step '1' and status 'Open'.", serviceContext);

        // OK - now finally make a trivial valid change
        resetErrorCollection(serviceContext);
        oldJiraWorkflow.clear();
        oldJiraWorkflow.addStep(1, "Open");
        newJiraWorkflow.clear();
        newJiraWorkflow.addStep(1, "Open");
        newJiraWorkflow.addStep(2, "Closed");
        defaultWorkflowService.validateOverwriteWorkflow(serviceContext, "My Workflow");
        assertNoErrors(serviceContext);

        // Bigger set of steps
        resetErrorCollection(serviceContext);
        oldJiraWorkflow.clear();
        oldJiraWorkflow.addStep(1, "Open");
        oldJiraWorkflow.addStep(2, "Assigned");
        oldJiraWorkflow.addStep(3, "Resolved");
        oldJiraWorkflow.addStep(4, "Closed");
        newJiraWorkflow.clear();
        newJiraWorkflow.addStep(1, "Open");
        newJiraWorkflow.addStep(4, "Closed");
        newJiraWorkflow.addStep(2, "Assigned");
        newJiraWorkflow.addStep(3, "Resolved");
        newJiraWorkflow.addStep(5, "Re-Opened");
        defaultWorkflowService.validateOverwriteWorkflow(serviceContext, "My Workflow");
        assertNoErrors(serviceContext);

        // OK - now finally make a trivial valid change
        resetErrorCollection(serviceContext);
        oldJiraWorkflow.clear();
        oldJiraWorkflow.addStep(1, "Open");
        oldJiraWorkflow.addStep(2, "Assigned");
        oldJiraWorkflow.addStep(3, "Resolved");
        oldJiraWorkflow.addStep(4, "Closed");
        newJiraWorkflow.clear();
        newJiraWorkflow.addStep(1, "Open");
        newJiraWorkflow.addStep(2, "Assigned");
        newJiraWorkflow.addStep(4, "Closed");
        defaultWorkflowService.validateOverwriteWorkflow(serviceContext, "My Workflow");
        assertErrorMessage("The draft workflow does not contain required status 'Resolved'.", serviceContext);

        // OK - now finally make a trivial valid change
        resetErrorCollection(serviceContext);
        oldJiraWorkflow.clear();
        oldJiraWorkflow.addStep(1, "Open");
        oldJiraWorkflow.addStep(2, "Assigned");
        oldJiraWorkflow.addStep(3, "Resolved");
        oldJiraWorkflow.addStep(4, "Closed");
        newJiraWorkflow.clear();
        newJiraWorkflow.addStep(1, "Open");
        newJiraWorkflow.addStep(2, "Assigned");
        newJiraWorkflow.addStep(3, "Resolved");
        newJiraWorkflow.addStep(4, "Rubbish");
        defaultWorkflowService.validateOverwriteWorkflow(serviceContext, "My Workflow");
        assertErrorMessage("The draft workflow does not contain required status 'Closed'.", serviceContext);

        // OK - now finally make a trivial valid change
        resetErrorCollection(serviceContext);
        oldJiraWorkflow.clear();
        oldJiraWorkflow.addStep(1, "Open");
        oldJiraWorkflow.addStep(2, "Assigned");
        oldJiraWorkflow.addStep(3, "Resolved");
        oldJiraWorkflow.addStep(4, "Closed");
        newJiraWorkflow.clear();
        newJiraWorkflow.addStep(1, "Open");
        newJiraWorkflow.addStep(2, "Assigned");
        newJiraWorkflow.addStep(3, "Resolved");
        newJiraWorkflow.addStep(6, "Closed");
        defaultWorkflowService.validateOverwriteWorkflow(serviceContext, "My Workflow");
        assertErrorMessage("You cannot change the association between step '4' and status 'Closed'.", serviceContext);
        assertTrue(validateAddWorkflowTransitionCalled.get());
    }

    @Test
    public void testGetWorkflowNullName()
    {
        final DefaultWorkflowService workflowService = createWorkflowServiceWithRealPermissionCheck();

        final JiraServiceContext serviceContext = createServiceContext(null);
        workflowService.getWorkflow(serviceContext, null);
        assertErrorMessage("admin.workflows.service.error.null.name", serviceContext);

        resetErrorCollection(serviceContext);
        workflowService.getWorkflow(serviceContext, "");
        assertErrorMessage("admin.workflows.service.error.null.name", serviceContext);
    }

    @Test
    public void testValidateCopyWorkflowNoAdminRights()
    {
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(false);
        final JiraServiceContext serviceContext = createServiceContext(null);
        workflowService.validateCopyWorkflow(serviceContext, null);
        assertErrorMessage("admin.workflows.service.error.no.admin.permission", serviceContext);
    }

    @Test
    public void testValidateCopyWorkflow()
    {
        when(mockWorkflowManager.workflowExists("Copy of Workflow")).thenReturn(true);
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);

        final JiraServiceContext jiraServiceContext = createServiceContext(null);
        workflowService.validateCopyWorkflow(jiraServiceContext, null);
        assertEquals("admin.errors.you.must.specify.a.workflow.name", jiraServiceContext.getErrorCollection().getErrors().get("newWorkflowName"));

        resetErrorCollection(jiraServiceContext);
        //non-ascii chars
        workflowService.validateCopyWorkflow(jiraServiceContext, "The\u0192\u00e7WORKFLOW");
        assertEquals("admin.common.errors.use.only.ascii", jiraServiceContext.getErrorCollection().getErrors().get("newWorkflowName"));


        resetErrorCollection(jiraServiceContext);
        //already exists.
        workflowService.validateCopyWorkflow(jiraServiceContext, "Copy of Workflow");
        assertEquals("admin.errors.a.workflow.with.this.name.already.exists", jiraServiceContext.getErrorCollection().getErrors().get("newWorkflowName"));
    }

    @Test
    public void testValidateCopyWorkflowSuccess()
    {
        when(mockWorkflowManager.workflowExists("Copy of Workflow")).thenReturn(false);
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);
        final JiraServiceContext jiraServiceContext = createServiceContext(null);

        workflowService.validateCopyWorkflow(jiraServiceContext, "Copy of Workflow");
        assertNoErrors(jiraServiceContext);
        verify(mockWorkflowManager).workflowExists("Copy of Workflow");
    }

    @Test
    public void testCopyWorkflowSuccess()
    {
        final MockJiraWorkflow workflow = new MockJiraWorkflow();
        when(mockWorkflowManager.copyWorkflow("testuser", "Copy of Workflow", null, workflow)).thenReturn(workflow);
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);

        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);

        assertSame(workflow, workflowService.copyWorkflow(jiraServiceContext, "Copy of Workflow", null, workflow));
        assertNoErrors(jiraServiceContext);
    }

    @Test
    public void testCopyWorkflowSuccessWithNullUser()
    {
        final MockJiraWorkflow workflow = new MockJiraWorkflow();
        when(mockWorkflowManager.copyWorkflow("", "Copy of Workflow", null, workflow)).thenReturn(workflow);
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);
        final JiraServiceContext jiraServiceContext = createServiceContext(null);

        assertSame(workflow, workflowService.copyWorkflow(jiraServiceContext, "Copy of Workflow", null, workflow));
        assertNoErrors(jiraServiceContext);
    }

    @Test
    public void testCopyWorkflowNoAdminPermission()
    {
        final MockJiraWorkflow workflow = new MockJiraWorkflow();
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(false);

        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);

        workflowService.copyWorkflow(jiraServiceContext, "Copy of Workflow", null, workflow);
        assertErrorMessage("admin.workflows.service.error.no.admin.permission", jiraServiceContext);
    }


    @Test
    public void testValidateUpdateWorkflowNoAdminPermission()
    {
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(false);
        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);
        workflowService.validateUpdateWorkflowNameAndDescription(jiraServiceContext, null, null);
        assertErrorMessage("admin.workflows.service.error.no.admin.permission", jiraServiceContext);
    }

    @Test
    public void testValidateUpdateWorkflowThatIsNotModifiableIsNotAllowed() throws WorkflowException
    {
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);

        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);

        when(mockJiraWorkflow.isEditable()).thenReturn(false);
        when(mockJiraWorkflow.getDescriptor()).thenReturn(new DescriptorFactory().createWorkflowDescriptor());
        workflowService.validateUpdateWorkflowNameAndDescription(jiraServiceContext, mockJiraWorkflow, null);
        assertErrorMessage("admin.errors.workflow.cannot.be.edited.as.it.is.not.editable", jiraServiceContext);
    }

    @Test
    public void testValidateUpdateWorkflowWithNullNewName() throws WorkflowException
    {

        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);

        final User testUser = new MockUser("testuser");
        final JiraServiceContext serviceContext = createServiceContext(testUser);

        when(mockJiraWorkflow.isDraftWorkflow()).thenReturn(false);
        when(mockJiraWorkflow.isEditable()).thenReturn(true);
        when(mockJiraWorkflow.getDescriptor()).thenReturn(new DescriptorFactory().createWorkflowDescriptor());
        workflowService.validateUpdateWorkflowNameAndDescription(serviceContext, mockJiraWorkflow, null);
        assertTrue(serviceContext.getErrorCollection().getErrors().get("newWorkflowName").equals("admin.errors.you.must.specify.a.workflow.name"));
    }

    @Test
    public void testValidateUpdateWorkflowWithEmptyNewName() throws WorkflowException
    {
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);

        final JiraServiceContext jiraServiceContext = createServiceContext(new MockUser("testuser"));
        when(mockJiraWorkflow.isDraftWorkflow()).thenReturn(false);
        when(mockJiraWorkflow.isEditable()).thenReturn(true);
        when(mockJiraWorkflow.getDescriptor()).thenReturn(new DescriptorFactory().createWorkflowDescriptor());
        workflowService.validateUpdateWorkflowNameAndDescription(jiraServiceContext, mockJiraWorkflow, "");
        assertTrue(jiraServiceContext.getErrorCollection().getErrors().get("newWorkflowName").equals("admin.errors.you.must.specify.a.workflow.name"));
    }

    @Test
    public void testValidateUpdateWorkflowWithInvalidNewName() throws WorkflowException
    {
        final DefaultWorkflowService workflowService =createWorkflowServiceOverridingPermissionCheck(true);

        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);
        when(mockJiraWorkflow.isDraftWorkflow()).thenReturn(false);
        when(mockJiraWorkflow.isEditable()).thenReturn(true);
        when(mockJiraWorkflow.getDescriptor()).thenReturn(new DescriptorFactory().createWorkflowDescriptor());
        workflowService.validateUpdateWorkflowNameAndDescription(jiraServiceContext, mockJiraWorkflow, "InvalidNewName\u0192\u00e7");
        assertTrue(jiraServiceContext.getErrorCollection().getErrors().get("newWorkflowName").equals("admin.errors.please.use.only.ascii.characters"));
    }

    @Test
    public void testValidateUpdateWorkflowWithNewWorkflowExists() throws WorkflowException
    {
        when(mockWorkflowManager.workflowExists("newWorkflow")).thenReturn(true);

        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);
        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);

        when(mockJiraWorkflow.getName()).thenReturn("workflowName");
        when(mockJiraWorkflow.isDraftWorkflow()).thenReturn(false);
        when(mockJiraWorkflow.isEditable()).thenReturn(true);
        when(mockJiraWorkflow.getDescriptor()).thenReturn(new DescriptorFactory().createWorkflowDescriptor());
        workflowService.validateUpdateWorkflowNameAndDescription(jiraServiceContext, mockJiraWorkflow, "newWorkflow");
        assertTrue(jiraServiceContext.getErrorCollection().getErrors().get("newWorkflowName").equals(
            "admin.errors.a.workflow.with.this.name.already.exists"));
    }

    @Test
    public void testValidateUpdateWorkflow() throws WorkflowException
    {
        when(mockWorkflowManager.workflowExists("newWorkflow")).thenReturn(false);

        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);

        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);
        when(mockJiraWorkflow.getName()).thenReturn("workflowName");
        when(mockJiraWorkflow.isDraftWorkflow()).thenReturn(false);
        when(mockJiraWorkflow.isEditable()).thenReturn(true);
        when(mockJiraWorkflow.getDescriptor()).thenReturn(new DescriptorFactory().createWorkflowDescriptor());
        workflowService.validateUpdateWorkflowNameAndDescription(jiraServiceContext, mockJiraWorkflow, "newWorkflow");
        assertNoErrors(jiraServiceContext);
    }

    @Test
    public void testUpdateWorkflowNameNoAdminPermission()
    {
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(false);

        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);
        workflowService.updateWorkflowNameAndDescription(jiraServiceContext, null, null, null);
        assertErrorMessage("admin.workflows.service.error.no.admin.permission", jiraServiceContext);
    }

    @Test
    public void testUpdateWorkflowName()
    {
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);

        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);

        workflowService.updateWorkflowNameAndDescription(jiraServiceContext, mockJiraWorkflow, "newWorkflowName", "newDescription");
        assertNoErrors(jiraServiceContext);
        verify(mockWorkflowManager).updateWorkflowNameAndDescription("testuser", mockJiraWorkflow, "newWorkflowName", "newDescription");
    }

    @Test
    public void testValidateAddWorkflowTransitionToDraftNoAdminPermission()
    {
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(false);
        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);
        workflowService.validateAddWorkflowTransitionToDraft(jiraServiceContext, null, 1);
        assertErrorMessage("admin.workflows.service.error.no.admin.permission", jiraServiceContext);
    }

    @Test
    public void testValidateAddWorkflowTransitionToDraftActiveWorkflow()
    {
        final DefaultWorkflowService workflowService = createWorkflowServiceOverridingPermissionCheck(true);

        when(mockJiraWorkflow.isDraftWorkflow()).thenReturn(false);

        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);

        workflowService.validateAddWorkflowTransitionToDraft(jiraServiceContext, mockJiraWorkflow, 1);
        assertNoErrors(jiraServiceContext);
    }

    @Test
    public void testValidateAddWorkflowTransitionToDraftWorkflow()
    {
        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = createServiceContext(testUser);
        stubAndExecuteValidateAddWorkflowTransitionToDraftWorkflow(jiraServiceContext, Lists.newArrayList("Some dude", "blah"));
        assertNoErrors(jiraServiceContext);
    }

    @Test
    public void testValidateAddWorkflowTransitionToDraftWorkflowError()
    {
        final ErrorCollection errorCollection = new SimpleErrorCollection();
        final User testUser = new MockUser("testuser");
        final JiraServiceContext jiraServiceContext = new JiraServiceContextImpl(testUser, errorCollection);
        stubAndExecuteValidateAddWorkflowTransitionToDraftWorkflow(jiraServiceContext, Collections.<String>emptyList());
        assertErrorMessage("admin.workflowtransitions.error.add.transition.draft.step.without.transition", jiraServiceContext);
    }

    private void stubAndExecuteValidateAddWorkflowTransitionToDraftWorkflow(final JiraServiceContext jiraServiceContext,
            final List<String> actions)
    {
        final DefaultWorkflowService workflowService = new DefaultWorkflowService(null, null, null)
        {
            boolean hasAdminPermission(final JiraServiceContext jiraServiceContext)
            {
                return true;
            }

            public JiraWorkflow getWorkflow(final JiraServiceContext jiraServiceContext, final String name)
            {
                assertEquals("Hamlet", name);
                final StepDescriptor mockStepDescriptor = new DescriptorFactory().createStepDescriptor();
                final WorkflowDescriptor mockWorkflowDescriptor = new DescriptorFactory().createWorkflowDescriptor();
                mockStepDescriptor.setId(120);
                mockStepDescriptor.getActions().addAll(actions);
                mockWorkflowDescriptor.addStep(mockStepDescriptor);
                when(mockJiraWorkflow.getDescriptor()).thenReturn(mockWorkflowDescriptor);
                return mockJiraWorkflow;
            }

            I18nHelper getI18nBean()
            {
                return mockI18nHelper;
            }
        };

        when(mockDraftWorkflow.isDraftWorkflow()).thenReturn(true);
        when(mockDraftWorkflow.getName()).thenReturn("Hamlet");

        if (actions.isEmpty())
        {
            final StepDescriptor mockNewStepDescriptor = new DescriptorFactory().createStepDescriptor();
            mockNewStepDescriptor.getActions().addAll(Arrays.asList("Some", "Actions"));
            mockNewStepDescriptor.setName("Gretel");
            mockNewStepDescriptor.setId(120);

            final WorkflowDescriptor mockNewWorkflowDescriptor = new DescriptorFactory().createWorkflowDescriptor();
            mockNewWorkflowDescriptor.addStep(mockNewStepDescriptor);
            when(mockDraftWorkflow.getDescriptor()).thenReturn(mockNewWorkflowDescriptor);
        }

        workflowService.validateAddWorkflowTransitionToDraft(jiraServiceContext, mockDraftWorkflow, 120);
    }
}
