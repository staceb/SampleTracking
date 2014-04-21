package com.atlassian.jira.workflow;

import com.atlassian.jira.local.ListeningTestCase;
import org.junit.Test;
import static org.junit.Assert.*;
import com.opensymphony.user.User;
import com.opensymphony.workflow.loader.DescriptorFactory;
import com.opensymphony.workflow.loader.WorkflowDescriptor;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TestAbstractJiraWorkflow extends ListeningTestCase
{
    @Test
    public void testGetUpdated()
    {
        final Date now = new Date();
        final Map metaAtts = new HashMap();
        metaAtts.put(JiraWorkflow.JIRA_META_UPDATED_DATE, now.getTime() + "");
        JiraWorkflow jiraWorkflow = getJiraWorkflow(metaAtts);

        assertEquals(now, jiraWorkflow.getUpdatedDate());
    }

    @Test
    public void testInvalidUpdatedString()
    {
         final Map metaAtts = new HashMap();
        metaAtts.put(JiraWorkflow.JIRA_META_UPDATED_DATE, "imaninvalidstring");

        JiraWorkflow jiraWorkflow = getJiraWorkflow(metaAtts);

        assertNull(jiraWorkflow.getUpdatedDate());
    }

    @Test
    public void testGetUpdateAuthorName()
    {
        final Map metaAtts = new HashMap();
        metaAtts.put(JiraWorkflow.JIRA_META_UPDATE_AUTHOR_NAME, "tom");

        JiraWorkflow jiraWorkflow = getJiraWorkflow(metaAtts);

        assertEquals("tom", jiraWorkflow.getUpdateAuthorName());
    }

    private WorkflowDescriptor getMockWorkflowDescriptor(Map metaAtts)
    {
        final MockControl mockWorkflowDescriptorControl = MockClassControl.createControl(WorkflowDescriptor.class);
        final WorkflowDescriptor mockWorkflowDescriptor = (WorkflowDescriptor) mockWorkflowDescriptorControl.getMock();

        mockWorkflowDescriptor.getMetaAttributes();
        mockWorkflowDescriptorControl.setDefaultReturnValue(metaAtts);
        mockWorkflowDescriptorControl.replay();
        return mockWorkflowDescriptor;
    }

    private JiraWorkflow getJiraWorkflow(final Map metaAtts)
    {
        JiraWorkflow jiraWorkflow = new AbstractJiraWorkflow(null, getMockWorkflowDescriptor(metaAtts))
        {

            public String getName()
            {
                return "bsname";
            }

            public void store(User user) throws WorkflowException
            {
                //noop
            }

            public boolean isDraftWorkflow()
            {
                return false;
            }

            public void reset()
            {
                //noop
            }
        };
        return jiraWorkflow;
    }

    @Test
    public void testGetModeLive()
    {
        AbstractJiraWorkflow workflow = new AbstractJiraWorkflow(null, new DescriptorFactory().createWorkflowDescriptor())
        {
            public String getName()
            {
                return null;
            }

            public void store(User user) throws WorkflowException
            {
            }

            public boolean isDraftWorkflow()
            {
                return false;
            }
        };
        
        assertEquals("live", workflow.getMode());
    }

    @Test
    public void testGetModeDraft()
    {
        AbstractJiraWorkflow workflow = new AbstractJiraWorkflow(null, new DescriptorFactory().createWorkflowDescriptor())
        {
            public String getName()
            {
                return null;
            }

            public void store(User user) throws WorkflowException
            {
            }

            public boolean isDraftWorkflow()
            {
                return true;
            }
        };

        assertEquals("draft", workflow.getMode());
    }

    @Test
    public void testHasDraftWorkflow()
    {
        MockWorkflowManager mockWorkflowManager = new MockWorkflowManager();


        AbstractJiraWorkflow workflow = new AbstractJiraWorkflow(mockWorkflowManager, new DescriptorFactory().createWorkflowDescriptor())
        {
            public String getName()
            {
                return "Peter";
            }

            public void store(User user) throws WorkflowException
            {
            }

            public boolean isDraftWorkflow()
            {
                return false;
            }
        };

        // there is no draft in the mockWorkflowManager
        assertFalse(workflow.hasDraftWorkflow());
        // Now add a draft to the mockWorkflowManager
        mockWorkflowManager.updateDraftWorkflow("admin", "Peter", new MockJiraWorkflow());
        assertTrue(workflow.hasDraftWorkflow());

    }
}
