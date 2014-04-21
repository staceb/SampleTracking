package com.atlassian.jira.jql.resolver;

import org.junit.Test;
import static org.junit.Assert.*;
import com.atlassian.core.test.util.DuckTypeProxy;
import com.atlassian.jira.issue.priority.Priority;
import com.atlassian.jira.issue.status.Status;
import com.atlassian.jira.util.EasyList;
import com.atlassian.jira.local.ListeningTestCase;
import org.easymock.MockControl;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for {@link IssueConstantInfoResolver}.
 *
 * @since v4.0
 */
public class TestIssueConstantInfoResolver extends ListeningTestCase
{
    @Test
    public void testConstructor()
    {
        try
        {
            new IssueConstantInfoResolver<Status>(null);
            fail("epxected problemo constructing from nulls");
        }
        catch (RuntimeException yay)
        {
            //expected
        }
    }

    @Test
    public void testGetIndexedValuesString()
    {
        final MockControl mockNameResolverControl = MockControl.createStrictControl(NameResolver.class);
        final NameResolver<Priority> mockNameResolver = (NameResolver<Priority>) mockNameResolverControl.getMock();
        mockNameResolver.getIdsFromName("somePriorityName");
        final List<String> priorityIds = EasyList.build("123", "91919");
        mockNameResolverControl.setReturnValue(priorityIds);

        mockNameResolverControl.replay();

        IssueConstantInfoResolver<Priority> resolver = new IssueConstantInfoResolver<Priority>(mockNameResolver);
        assertEquals(priorityIds, resolver.getIndexedValues("somePriorityName"));
        mockNameResolverControl.verify();
    }

    @Test
    public void testGetIndexedValuesStringFallBackToLong()
    {
        final MockControl mockNameResolverControl = MockControl.createStrictControl(NameResolver.class);
        final NameResolver<Priority> mockNameResolver = (NameResolver<Priority>) mockNameResolverControl.getMock();
        mockNameResolver.getIdsFromName("999");
        mockNameResolverControl.setReturnValue(new ArrayList<String>());
        mockNameResolver.idExists(new Long(999));
        mockNameResolverControl.setReturnValue(true);
        mockNameResolverControl.replay();

        IssueConstantInfoResolver<Priority> resolver = new IssueConstantInfoResolver<Priority>(mockNameResolver);
        assertEquals(EasyList.build("999"), resolver.getIndexedValues("999"));
        mockNameResolverControl.verify();
    }

    @Test
    public void testGetIndexedValuesLongExists()
    {
        final MockControl mockNameResolverControl = MockControl.createStrictControl(NameResolver.class);
        final NameResolver<Priority> mockNameResolver = (NameResolver<Priority>) mockNameResolverControl.getMock();
        mockNameResolver.idExists(4321L);
        mockNameResolverControl.setReturnValue(true);

        mockNameResolverControl.replay();

        IssueConstantInfoResolver<Priority> resolver = new IssueConstantInfoResolver<Priority>(mockNameResolver);
        assertEquals(EasyList.build("4321"), resolver.getIndexedValues(4321L));
        mockNameResolverControl.verify();
    }

    @Test
    public void testGetIndexedValuesLongDoesNotExist()
    {
        final MockControl mockNameResolverControl = MockControl.createStrictControl(NameResolver.class);
        final NameResolver<Priority> mockNameResolver = (NameResolver<Priority>) mockNameResolverControl.getMock();
        mockNameResolver.idExists(4321L);
        mockNameResolverControl.setReturnValue(false);
        mockNameResolver.getIdsFromName("4321");
        final List<String> priorityIds = EasyList.build("86", "99");
        mockNameResolverControl.setReturnValue(priorityIds);
        mockNameResolverControl.replay();

        IssueConstantInfoResolver<Priority> resolver = new IssueConstantInfoResolver<Priority>(mockNameResolver);
        assertEquals(priorityIds, resolver.getIndexedValues(4321L));
        mockNameResolverControl.verify();
    }

    @Test
    public void testGetIndexedValue()
    {
        final MockControl mockPriorityControl = MockControl.createStrictControl(Priority.class);
        final Priority mockPriority = (Priority) mockPriorityControl.getMock();
        mockPriority.getId();
        mockPriorityControl.setReturnValue("666666");
        mockPriorityControl.replay();

        final NameResolver<Priority> nameResolver = (NameResolver<Priority>) DuckTypeProxy.getProxy(NameResolver.class, new Object());
        IssueConstantInfoResolver<Priority> resolver = new IssueConstantInfoResolver<Priority>(nameResolver);
        assertEquals("666666", resolver.getIndexedValue(mockPriority));
        mockPriorityControl.verify();

        try
        {
            resolver.getIndexedValue(null);
            fail("expected RTE");
        }
        catch (RuntimeException fine)
        {

        }
    }

}
