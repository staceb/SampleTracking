package com.atlassian.jira.workflow.function.issue;

import com.atlassian.jira.local.ListeningTestCase;
import com.atlassian.jira.mock.ofbiz.MockGenericValue;
import com.atlassian.jira.user.MockCrowdService;
import org.junit.After;
import org.junit.Test;
import org.junit.Before;
import com.atlassian.jira.MockProviderAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.project.MockProject;
import com.atlassian.core.util.map.EasyMap;
import com.atlassian.core.util.collection.EasyList;
import com.mockobjects.dynamic.Mock;
import com.mockobjects.dynamic.P;
import com.opensymphony.user.User;
import org.ofbiz.core.entity.GenericValue;

import java.util.Collections;
import java.util.Map;
import java.util.List;

/*
 * required mocks
 * issue : getComponents(), getProjectObject(), isCreated(), setAssignee(), store()
 * project : getLead()

 * required overrides
 * getLead()
 */
public class TestAssignToLeadFunction extends ListeningTestCase
{
    private MockProject project;
    private User user;
    private User componentLead;

    @Before
    public void setUp() throws Exception
    {
        MockProviderAccessor mpa = new MockProviderAccessor();
        user = new User("admin", mpa, new MockCrowdService());
        componentLead = new User("testuser", mpa, new MockCrowdService());

        project = new MockProject();
        project.setLead(user);
    }

    @After
    public void tearDown() throws Exception
    {
        project = null;
        user = null;
        componentLead = null;
    }

    @Test
    public void testBrandNewIssueDoesntStore()
    {
        AssignToLeadFunction func = createFunction(user);

        Mock mockIssue = createMockIssue(Collections.EMPTY_LIST, false, user);
        Issue issue = (Issue) mockIssue.proxy();
        Map transientVars = EasyMap.build("issue", issue);

        func.execute(transientVars, null, null);
        mockIssue.verify();
    }

    @Test
    public void testExistingIssueDoesStore()
    {
        AssignToLeadFunction func = createFunction(user);

        Mock mockIssue = createMockIssue(Collections.EMPTY_LIST, true, user);
        Issue issue = (Issue) mockIssue.proxy();
        Map transientVars = EasyMap.build("issue", issue);

        func.execute(transientVars, null, null);
        mockIssue.verify();
    }

    @Test
    public void testComponentLeadChosen()
    {
        AssignToLeadFunction func = createFunction(componentLead);

        GenericValue component = new MockGenericValue("Component", EasyMap.build("lead", "testuser"));
        Mock mockIssue = createMockIssue(EasyList.build(component), true, componentLead);
        Issue issue = (Issue) mockIssue.proxy();
        Map transientVars = EasyMap.build("issue", issue);

        func.execute(transientVars, null, null);
        mockIssue.verify();
    }

    @Test
    public void testNoLeadsToChoose()
    {
        AssignToLeadFunction func = createFunction(null);
        project.setLead(null);

        Mock mockIssue = createMockIssue(Collections.EMPTY_LIST, false, null);
        Issue issue = (Issue) mockIssue.proxy();
        Map transientVars = EasyMap.build("issue", issue);

        func.execute(transientVars, null, null);
        mockIssue.verify();
    }

    private Mock createMockIssue(List components, boolean isCreated, User assignee)
    {
        Mock mockIssue = new Mock(MutableIssue.class);
        mockIssue.setStrict(true);
        mockIssue.expectAndReturn("getComponents", components);

        if (components.isEmpty())
        {
            mockIssue.expectAndReturn("getProjectObject", project);
        }
        else
        {
            mockIssue.expectNotCalled("getProjectObject");
        }

        if (assignee != null)
        {
            mockIssue.expectVoid("setAssignee", P.args(P.eq(assignee)));
            mockIssue.expectAndReturn("isCreated", (isCreated ? Boolean.TRUE : Boolean.FALSE));
        }
        else
        {
            mockIssue.expectNotCalled("setAssignee");
            mockIssue.expectNotCalled("isCreated");
        }

        if (isCreated && assignee != null)
        {
            mockIssue.expectVoid("store", P.ANY_ARGS);
        }
        else
        {
            mockIssue.expectNotCalled("store");
        }
        return mockIssue;
    }

    private AssignToLeadFunction createFunction(final User lead)
    {
        return new AssignToLeadFunction()
        {
            User getLead(String userName)
            {
                return lead;
            }
        };
    }
}
