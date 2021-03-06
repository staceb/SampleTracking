package com.atlassian.jira.issue.comparator;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.search.SearchRequest;
import com.atlassian.jira.user.MockUser;
import com.atlassian.query.QueryImpl;
import org.junit.Test;

import java.util.Comparator;

public class TestFilterNameComparator extends AbstractComparatorTestCase
{

    @Test
    public void testCompareSearchRequest()
    {
        User user;
        user = new MockUser("admin");
        final Comparator filterNameComparator = FilterNameComparator.COMPARATOR;
        final SearchRequest filterA = new SearchRequest(new QueryImpl(), user.getName(), "filter A", "filter A Description");
        final SearchRequest filterA1 = new SearchRequest(new QueryImpl(), user.getName(), "Filter a", "filter a Description");
        final SearchRequest filterB = new SearchRequest(new QueryImpl(), user.getName(), "filter B", "filter B Description");
        final SearchRequest filterB1 = new SearchRequest(new QueryImpl(), user.getName(), "filter B", "filter B Description");
        final SearchRequest filterNullName = new SearchRequest(new QueryImpl(), user.getName(), null, "filter Null Description");
        //test null cases
        assertEqualTo(filterNameComparator, null, null);
        assertLessThan(filterNameComparator, null, filterA);
        assertGreaterThan(filterNameComparator, filterA, null);
        //test standard cases
        assertEqualTo(filterNameComparator, filterA, filterA);
        assertEqualTo(filterNameComparator, filterA, filterA1);
        assertEqualTo(filterNameComparator, filterB, filterB);
        assertEqualTo(filterNameComparator, filterB, filterB1);
        assertLessThan(filterNameComparator, filterA, filterB);
        assertLessThan(filterNameComparator, filterA1, filterB);
        assertGreaterThan(filterNameComparator, filterB, filterA);
        //test null name cases
        assertEqualTo(filterNameComparator, filterNullName, filterNullName);
        assertLessThan(filterNameComparator, filterNullName, filterA);
        assertGreaterThan(filterNameComparator, filterA, filterNullName);
    }
}
