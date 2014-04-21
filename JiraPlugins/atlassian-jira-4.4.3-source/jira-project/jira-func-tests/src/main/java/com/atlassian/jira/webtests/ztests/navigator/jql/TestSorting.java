package com.atlassian.jira.webtests.ztests.navigator.jql;

import com.atlassian.jira.functest.framework.Splitable;
import com.atlassian.jira.functest.framework.SystemTenantOnly;
import com.atlassian.jira.functest.framework.locator.XPathLocator;
import com.atlassian.jira.functest.framework.navigation.IssueNavigatorNavigation;
import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.util.collect.MapBuilder;
import junit.framework.Assert;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * @since v4.0
 */
@Splitable
@WebTest ({ Category.FUNC_TEST, Category.JQL })
public class TestSorting extends AbstractJqlFuncTest
{
    public void testOrderByClauseNameResolvesToMultipleFields() throws Exception
    {
        administration.restoreData("TestOrderByClauseNameResolvesToMultipleFields.xml");
        // there are two fields with the name NF. check that order by sorts them in order of: cf[10000], cf[10001]

        // we can't use createSearchAndAssertIssuesAndSorts because it also validates the sorting arrows. It asserts
        // that there is exactly one sort arrow on the column headers. However, there is a small quirk when you have
        // two custom fields with the same name and do a JQL ORDER BY: they both end up sorted ascending. We don't
        // really like that behaviour but fixing it isn't a high priority.

        //createSearchAndAssertIssuesAndSorts("ORDER BY NF",
        //        "NF ascending, then NF ascending",
        //        "HSP-1", "HSP-3", "HSP-2", "HSP-4");

        navigation.issueNavigator().createSearch("ORDER BY NF");
        assertions.getIssueNavigatorAssertions().assertExactIssuesInResults("HSP-1", "HSP-3", "HSP-2", "HSP-4");
        tester.clickLink("viewfilter");
        assertions.getTextAssertions().assertTextSequence(new XPathLocator(tester, "//div[@id='filter-summary']"), "Sorted by", "NF ascending, then NF ascending");
    }

    public void testModifyOrderByClicking() throws Exception
    {
        administration.restoreData("TestCustomFieldDefaultSortOrderings.xml");

        for(int customFieldId = 10000; customFieldId < 10020; customFieldId++)
        {
            createSearchAndAssertIssuesAndSorts("project = two order by KEY DESC",
                    "Key descending",
                    "TWO-4", "TWO-3", "TWO-2", "TWO-1");

            _clickSortCustomField(customFieldId, "DESC");
            _clickSortCustomField(customFieldId, "ASC");
        }

        // start with a simple order by clause and the simulate clicking on various columns to see how the JQL order by changes
        createSearchAndAssertIssuesAndSorts("project = two order by KEY DESC",
                "Key descending",
                "TWO-4", "TWO-3", "TWO-2", "TWO-1");

        final String query = "project = two";

        // ensure that clicking on new columns adds the sorts to the JQL dialog
        final SortPair keySortDesc = new SortPair("KEY", SortPair.Direction.DESC);
        final SortPair assigneeSortAsc = new SortPair("assignee", SortPair.Direction.ASC);
        _clickSortSystemField(query, assigneeSortAsc, keySortDesc);

        final SortPair resolutionSortDesc = new SortPair("resolution", SortPair.Direction.DESC);
        _clickSortSystemField(query, resolutionSortDesc, assigneeSortAsc, keySortDesc);

        // this is our 4th sort but clicking on columns is limited to 3. ensure that we truncate it to 3 order by clauses.
        final SortPair prioritySortDesc = new SortPair("priority", SortPair.Direction.DESC);
        _clickSortSystemField(query, prioritySortDesc, resolutionSortDesc, assigneeSortAsc);

        // if we order by assignee again does it move from the end of the order by clauses to the beginning?
        _clickSortSystemField(query, assigneeSortAsc, prioritySortDesc, resolutionSortDesc);

        // what if we order by a column but in a different direction?
        final SortPair prioritySortAsc = new SortPair("priority", SortPair.Direction.ASC);
        _clickSortSystemField(query, prioritySortAsc, assigneeSortAsc, resolutionSortDesc);
    }

    public void testModifyOrderByClickingOldAliasPreserved() throws Exception
    {
        administration.restoreData("TestCustomFieldDefaultSortOrderings.xml");

        // start with a simple order by clause and the simulate clicking on various columns to see how the JQL order by changes
        createSearchAndAssertIssuesAndSorts("project = two order by issuekey ASC",
                "Key ascending",
                "TWO-1", "TWO-2", "TWO-3", "TWO-4");

        // ensure that clicking on the Key column does not update the field
        tester.gotoPage("/secure/IssueNavigator!executeAdvanced.jspa?sorter/field=issuekey&sorter/order=DESC");
        assertions.getIssueNavigatorAssertions().assertNoJqlErrors();
        String jql = tester.getDialog().getFormParameterValue("jqlQuery");

        Assert.assertEquals("project = two ORDER BY issuekey DESC", jql);

        // longer sorts queries
        createSearchAndAssertIssuesAndSorts("project = two order by assignee ASC, issuekey ASC",
                "Assignee ascending, then Key ascending",
                "TWO-1", "TWO-2", "TWO-3", "TWO-4");

        // ensure that clicking on the Key column does not update the field
        tester.gotoPage("/secure/IssueNavigator!executeAdvanced.jspa?sorter/field=issuekey&sorter/order=DESC");
        assertions.getIssueNavigatorAssertions().assertNoJqlErrors();
        jql = tester.getDialog().getFormParameterValue("jqlQuery");

        Assert.assertEquals("project = two ORDER BY issuekey DESC, assignee ASC", jql);

        // longer sorts queries
        createSearchAndAssertIssuesAndSorts("project = two order by status ASC, priority ASC, assignee ASC, issuekey ASC",
                "Status ascending, then Priority ascending, then Assignee ascending, then Key ascending",
                "TWO-1", "TWO-2", "TWO-3", "TWO-4");

        // ensure that clicking on the Key column does not update the field
        tester.gotoPage("/secure/IssueNavigator!executeAdvanced.jspa?sorter/field=issuekey&sorter/order=DESC");
        assertions.getIssueNavigatorAssertions().assertNoJqlErrors();
        jql = tester.getDialog().getFormParameterValue("jqlQuery");

        Assert.assertEquals("project = two ORDER BY issuekey DESC, status ASC, priority ASC", jql);
    }

    public void testOrderingByNonSearchableFields() throws Exception
    {
        administration.restoreData("TestOrderByNavigableFields.xml");

        createSearchAndAssertIssuesAndSorts("project = HSP ORDER BY progress ASC, key ASC",
                "Progress ascending",
                "HSP-2", "HSP-1", "HSP-3", "HSP-4");

        createSearchAndAssertIssuesAndSorts("project = HSP ORDER BY progress DESC, key ASC",
                "Progress descending",
                "HSP-3", "HSP-4", "HSP-1", "HSP-2");

        createSearchAndAssertIssuesAndSorts("project = HSP ORDER BY subtasks ASC, key ASC",
                "Sub-Tasks ascending",
                "HSP-2", "HSP-1", "HSP-3", "HSP-4");

        createSearchAndAssertIssuesAndSorts("project = HSP ORDER BY subtasks DESC, key ASC",
                "Sub-Tasks descending",
                "HSP-1", "HSP-3", "HSP-4", "HSP-2");
    }

    private static final class SortPair
    {
        private final Direction direction;
        private final String fieldName;

        public Direction getDirection()
        {
            return direction;
        }

        public String getFieldName()
        {
            return fieldName;
        }

        public enum Direction { ASC, DESC }

        public SortPair(String fieldName, Direction direction)
        {
            this.fieldName = fieldName;
            this.direction = direction;
        }
    }

    private void _clickSortCustomField(final int customField, final String orderDirection)
    {
        tester.gotoPage(String.format("/secure/IssueNavigator!executeAdvanced.jspa?sorter/field=customfield_%d&sorter/order=%s", customField, orderDirection));
        assertions.getIssueNavigatorAssertions().assertNoJqlErrors();
        String jql = tester.getDialog().getFormParameterValue("jqlQuery");
        Assert.assertEquals(String.format("project = two ORDER BY cf[%d] %s, KEY DESC", customField, orderDirection), jql);
    }

    private void _clickSortSystemField(final String query, final SortPair newSort, final SortPair... expectedSorts)
    {
        tester.gotoPage(String.format("/secure/IssueNavigator!executeAdvanced.jspa?sorter/field=%s&sorter/order=%s", newSort.getFieldName(), newSort.getDirection().toString()));
        assertions.getIssueNavigatorAssertions().assertNoJqlErrors();
        String jql = tester.getDialog().getFormParameterValue("jqlQuery");

        Assert.assertTrue(expectedSorts.length <= 2);
        StringBuilder sb = new StringBuilder(String.format("%s ORDER BY %s %s", query, newSort.getFieldName(), newSort.getDirection().toString()));
        for (SortPair expectedSort : expectedSorts)
        {
            sb.append(", ");
            sb.append(String.format("%s %s", expectedSort.getFieldName(), expectedSort.getDirection().toString()));
        }
        Assert.assertEquals(sb.toString(), jql);
    }

    public void testMultipleCustomFieldsWithSameDisplayName() throws Exception
    {
        // this is similar to testOrderByClauseNameResolvesToMultipleFields but here we're simulating a click on the column
        // header to make sure we do the right things.
        administration.restoreData("TestSortMultipleCustomFieldsWithSameDisplayName.xml");

        navigation.issueNavigator().createSearch("ORDER BY NF");

        // simulate sorting on a column. this is horrible. we suck.
        tester.gotoPage("/secure/IssueNavigator!executeAdvanced.jspa?sorter/field=customfield_10005&sorter/order=DESC");
        assertions.getIssueNavigatorAssertions().assertNoJqlErrors();

        String jql = tester.getDialog().getFormParameterValue("jqlQuery");
        Assert.assertEquals("ORDER BY cf[10005] DESC, cf[10004]", jql);

        tester.clickLink("viewfilter");
        assertions.getTextAssertions().assertTextSequence(new XPathLocator(tester, "//div[@id='filter-summary']"), "Sorted by", "NF descending, then NF ascending");
    }

    public void testOrderByInvisibleCustomField() throws Exception
    {
        // try ordering by a custom field that is only configured in a project that the user
        // does not have browse project on.
        administration.restoreData("TestOrderByInvisibleCustomField.xml");
        navigation.login(FRED_USERNAME);
        navigation.issueNavigator().createSearch("ORDER BY DT");
        assertions.getIssueNavigatorAssertions().assertJqlErrors("Not able to sort using field 'DT'.");
        loadFilterAndAssertNotAbleToSort("dt", 10000);
    }    

    @SystemTenantOnly
    public void testJqlBuiltOrderByClauses()
    {
        administration.restoreData("TestCustomFieldDefaultSortOrderings.xml");
        _testUnlimitedFieldsInOrderByClause();
        _testFieldCannotBeUsedTwice();
        _testDisabledCustomFieldsSorting();
        _testNonExistentField();
    }

    private void _testNonExistentField()
    {
        // verify that ordering by a fieldname that doesn't exist results in an error
        navigation.issueNavigator().createSearch("ORDER BY bleargh");
        assertions.getIssueNavigatorAssertions().assertJqlErrors("Not able to sort using field 'bleargh'.");
    }

    private void _testDisabledCustomFieldsSorting()
    {
        try
        {
            administration.plugins().disablePlugin("com.atlassian.jira.plugin.system.customfieldtypes");

            // verify that we can't sort by custom fields when they are disabled
            final String[] fields = {"CSF", "DP", "DT", "FTF", "GP", "II", "MC", "MGP", "MS", "MUP", "NF", "PP", "RB", "ROTF", "SL", "SVP", "TF", "UP", "URL", "VP",
                "cf[10000]", "cf[10001]", "cf[10002]", "cf[10003]", "cf[10004]", "cf[10005]", "cf[10006]", "cf[10007]", "cf[10008]", "cf[10009]",
                "cf[10010]", "cf[10011]", "cf[10012]", "cf[10013]", "cf[10014]", "cf[10015]", "cf[10016]", "cf[10017]", "cf[10018]", "cf[10019]", "cf[10020]" };
            for (String field : fields)
            {
                assertNotAbleToSort(field);
            }

            loadFilterAndAssertNotAbleToSort("CSF", 10000);
            loadFilterAndAssertNotAbleToSort("DP", 10001);
            loadFilterAndAssertNotAbleToSort("DT", 10002);
            loadFilterAndAssertNotAbleToSort("FTF", 10003);
        }
        finally
        {
            administration.plugins().enablePlugin("com.atlassian.jira.plugin.system.customfieldtypes");
        }
    }

    private void loadFilterAndAssertNotAbleToSort(final String fieldName, final int filterId)
    {
        navigation.issueNavigator().loadFilter(filterId, IssueNavigatorNavigation.NavigatorEditMode.ADVANCED);
        final String errorMessage = String.format("Not able to sort using field '%s'.", fieldName);
        assertions.getIssueNavigatorAssertions().assertJqlErrors(errorMessage);
    }

    private void _testUnlimitedFieldsInOrderByClause()
    {
        // (where "unlimited" == 11)
        createSearchAndAssertIssuesAndSorts(
                "project in (one, two, three, four, five, six, seven, eight, nine, ten) ORDER BY csf, dp, dt, ftf, gp, ii, mc, mgp, ms, mup, key",
                "CSF ascending, then DP ascending, then DT ascending, then FTF ascending, then GP ascending, then II ascending, then MC ascending, then MGP ascending, then MS ascending, then MUP ascending, then Key ascending",
                "ONE-2", "ONE-3", "ONE-4", "TWO-2", "TWO-3", "TWO-4", "THREE-2", "THREE-3", "THREE-4", "FOUR-3", "FOUR-2", "FIVE-3",
                "FIVE-2", "SIX-2", "SIX-3", "SIX-4", "SEVEN-2", "SEVEN-3", "EIGHT-3", "EIGHT-2", "NINE-2", "NINE-3", "TEN-3", "TEN-2",
                "EIGHT-1", "FIVE-1", "FOUR-1", "NINE-1", "ONE-1", "SEVEN-1", "SIX-1", "TEN-1", "THREE-1", "TWO-1");
    }

    private void _testFieldCannotBeUsedTwice()
    {
        // same field twice
        navigation.issueNavigator().createSearch("ORDER BY key, priority, key");
        assertions.getIssueNavigatorAssertions().assertJqlErrors("The sort field 'key' is referenced multiple times in the JQL sort.");

        // system field with alias
        navigation.issueNavigator().createSearch("ORDER BY issuekey, priority, key");
        assertions.getIssueNavigatorAssertions().assertJqlErrors("The sort field 'key' is referenced multiple times in the JQL sort. Field 'key' is an alias for field 'issuekey'.");

        // custom field with alias
        navigation.issueNavigator().createSearch("ORDER BY DP, priority, cf[10001]");
        assertions.getIssueNavigatorAssertions().assertJqlErrors("The sort field 'cf[10001]' is referenced multiple times in the JQL sort. Field 'cf[10001]' is an alias for field 'DP'.");
    }

    public void testDefaultOrderByClause() throws Exception
    {
        administration.restoreData("TestDefaultOrderByClause.xml");

        // specifying a query without an ORDER BY clause will sort by default on issuekey DESC
        navigation.issueNavigator().createSearch("project = HSP");
        assertions.getIssueNavigatorAssertions().assertExactIssuesInResults("HSP-3", "HSP-2", "HSP-1");
        tester.clickLink("viewfilter");
        assertions.getTextAssertions().assertTextSequence(new XPathLocator(tester, "//div[@id='filter-summary']"), "Sorted by", "Key descending");
        // JRA-17978 - We also want confirm that the Clear Sorts link is not available here
        tester.assertLinkNotPresentWithText("Clear Sorts");

        // if a free text field is searched however, no default sorts will be applied
        navigation.issueNavigator().createSearch("summary ~ 'quick brown fox'");
        assertions.getIssueNavigatorAssertions().assertExactIssuesInResults("HSP-1", "HSP-3", "HSP-2");
        tester.clickLink("viewfilter");
        assertions.getTextAssertions().assertTextNotPresent(new XPathLocator(tester, "//div[@id='filter-summary']"), "Sorted by");
        // JRA-17978 - We also want confirm that the Clear Sorts link is not available here
        tester.assertLinkNotPresentWithText("Clear Sorts");
    }

    public void testClearSorts() throws Exception
    {
        administration.restoreData("TestDefaultOrderByClause.xml");

        // specifying a query with an ORDER BY clause, we should get the Clear Sorts option
        navigation.issueNavigator().createSearch("project = hsp ORDER BY assignee ASC");
        // JRA-17978 - We also want confirm that the Clear Sorts link is available here
        tester.assertLinkPresentWithText("Clear Sorts");
        // Lets click it and make sure we lose the sorts
        tester.clickLinkWithText("Clear Sorts");
        assertJqlQueryInTextArea("project = hsp");
        tester.assertLinkNotPresentWithText("Clear Sorts");
    }

    public void testSystemFieldDefaultSortOrderings()
    {
        administration.restoreData("TestSystemFieldDefaultSortOrderings.xml");

        assertDoesNotSupportSorting("comment");

        assertNotAbleToSort("category");
        assertNotAbleToSort("parent");
        assertNotAbleToSort("savedFilter");
        assertNotAbleToSort("text");

        // for fields which are trying to sort empty values, the empties may not have a defined order inside them, so we must specify an additional field to sort by to concretely define it
        createSearchAndAssertIssuesAndSorts("ORDER BY affectedVersion, issue DESC", "Affects Version/s ascending, then Key descending", "HSP-1", "HSP-2", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY affectedVersion ASC, issue DESC", "Affects Version/s ascending, then Key descending", "HSP-1", "HSP-2", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY affectedVersion DESC, issue DESC", "Affects Version/s descending, then Key descending", "MKY-1", "HSP-4", "HSP-3", "HSP-2", "HSP-1");

        createSearchAndAssertIssuesAndSorts("ORDER BY assignee, issue DESC", "Assignee ascending, then Key descending", "MKY-1", "HSP-4", "HSP-1", "HSP-2", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY assignee ASC, issue DESC", "Assignee ascending, then Key descending", "MKY-1", "HSP-4", "HSP-1", "HSP-2", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY assignee DESC, issue DESC", "Assignee descending, then Key descending", "HSP-3", "HSP-2", "MKY-1", "HSP-4", "HSP-1");

        createSearchAndAssertIssuesAndSorts("ORDER BY component, issue DESC", "Component/s ascending, then Key descending", "HSP-1", "HSP-2", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY component ASC, issue DESC", "Component/s ascending, then Key descending", "HSP-1", "HSP-2", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY component DESC, issue DESC", "Component/s descending, then Key descending", "MKY-1", "HSP-4", "HSP-3", "HSP-2", "HSP-1");

        createSearchAndAssertIssuesAndSorts("ORDER BY created", "Created descending", "HSP-4", "MKY-1", "HSP-3", "HSP-2", "HSP-1");
        createSearchAndAssertIssuesAndSorts("ORDER BY created ASC", "Created ascending", "HSP-1", "HSP-2", "HSP-3", "MKY-1", "HSP-4");
        createSearchAndAssertIssuesAndSorts("ORDER BY created DESC", "Created descending", "HSP-4", "MKY-1", "HSP-3", "HSP-2", "HSP-1");

        createSearchAndAssertIssuesAndSorts("ORDER BY description, issue DESC", "Description ascending, then Key descending", "HSP-1", "HSP-2", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY description ASC, issue DESC", "Description ascending, then Key descending", "HSP-1", "HSP-2", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY description DESC, issue DESC", "Description descending, then Key descending", "MKY-1", "HSP-4", "HSP-3", "HSP-2", "HSP-1");

        createSearchAndAssertIssuesAndSorts("ORDER BY due, issue DESC", "Due Date descending, then Key descending", "MKY-1", "HSP-4", "HSP-3", "HSP-1", "HSP-2");
        createSearchAndAssertIssuesAndSorts("ORDER BY due ASC, issue DESC", "Due Date ascending, then Key descending", "HSP-2", "HSP-1", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY due DESC, issue DESC", "Due Date descending, then Key descending", "MKY-1", "HSP-4", "HSP-3", "HSP-1", "HSP-2");

        createSearchAndAssertIssuesAndSorts("ORDER BY environment, issue DESC", "Environment ascending, then Key descending", "HSP-1", "HSP-2", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY environment ASC, issue DESC", "Environment ascending, then Key descending", "HSP-1", "HSP-2", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY environment DESC, issue DESC", "Environment descending, then Key descending", "MKY-1", "HSP-4", "HSP-3", "HSP-2", "HSP-1");

        createSearchAndAssertIssuesAndSorts("ORDER BY fixVersion, issue DESC", "Fix Version/s ascending, then Key descending", "HSP-1", "HSP-2", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY fixVersion ASC, issue DESC", "Fix Version/s ascending, then Key descending", "HSP-1", "HSP-2", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY fixVersion DESC, issue DESC", "Fix Version/s descending, then Key descending", "MKY-1", "HSP-4", "HSP-3", "HSP-2", "HSP-1");

        createSearchAndAssertIssuesAndSorts("ORDER BY issue", "Key ascending", "HSP-1", "HSP-2", "HSP-3", "HSP-4", "MKY-1");
        createSearchAndAssertIssuesAndSorts("ORDER BY issue ASC", "Key ascending", "HSP-1", "HSP-2", "HSP-3", "HSP-4", "MKY-1");
        createSearchAndAssertIssuesAndSorts("ORDER BY issue DESC", "Key descending", "MKY-1", "HSP-4", "HSP-3", "HSP-2", "HSP-1");

        createSearchAndAssertIssuesAndSorts("ORDER BY level, issue DESC", "Security Level ascending, then Key descending", "HSP-1", "HSP-2", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY level ASC, issue DESC", "Security Level ascending, then Key descending", "HSP-1", "HSP-2", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY level DESC, issue DESC", "Security Level descending, then Key descending", "MKY-1", "HSP-4", "HSP-3", "HSP-2", "HSP-1");

        createSearchAndAssertIssuesAndSorts("ORDER BY originalEstimate, issue DESC", "Original Estimate descending, then Key descending", "MKY-1", "HSP-4", "HSP-3", "HSP-2", "HSP-1");
        createSearchAndAssertIssuesAndSorts("ORDER BY originalEstimate ASC, issue DESC", "Original Estimate ascending, then Key descending", "HSP-1", "HSP-2", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY originalEstimate DESC, issue DESC", "Original Estimate descending, then Key descending", "MKY-1", "HSP-4", "HSP-3", "HSP-2", "HSP-1");

        createSearchAndAssertIssuesAndSorts("ORDER BY priority, issue DESC", "Priority descending, then Key descending", "HSP-1", "MKY-1", "HSP-4", "HSP-2", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY priority ASC, issue DESC", "Priority ascending, then Key descending", "HSP-3", "MKY-1", "HSP-4", "HSP-2", "HSP-1");
        createSearchAndAssertIssuesAndSorts("ORDER BY priority DESC, issue DESC", "Priority descending, then Key descending", "HSP-1", "MKY-1", "HSP-4", "HSP-2", "HSP-3");

        createSearchAndAssertIssuesAndSorts("ORDER BY project, issue ASC", "Project ascending, then Key ascending", "HSP-1", "HSP-2", "HSP-3", "HSP-4", "MKY-1");
        createSearchAndAssertIssuesAndSorts("ORDER BY project ASC, issue ASC", "Project ascending, then Key ascending", "HSP-1", "HSP-2", "HSP-3", "HSP-4", "MKY-1");
        createSearchAndAssertIssuesAndSorts("ORDER BY project DESC, issue DESC", "Project descending, then Key descending", "MKY-1", "HSP-4", "HSP-3", "HSP-2", "HSP-1");

        createSearchAndAssertIssuesAndSorts("ORDER BY remainingEstimate, issue DESC", "Remaining Estimate descending, then Key descending", "MKY-1", "HSP-4", "HSP-3", "HSP-2", "HSP-1");
        createSearchAndAssertIssuesAndSorts("ORDER BY remainingEstimate ASC, issue DESC", "Remaining Estimate ascending, then Key descending", "HSP-1", "HSP-2", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY remainingEstimate DESC, issue DESC", "Remaining Estimate descending, then Key descending", "MKY-1", "HSP-4", "HSP-3", "HSP-2", "HSP-1");

        createSearchAndAssertIssuesAndSorts("ORDER BY reporter, issue DESC", "Reporter ascending, then Key descending", "MKY-1", "HSP-1", "HSP-2", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY reporter ASC, issue DESC", "Reporter ascending, then Key descending", "MKY-1", "HSP-1", "HSP-2", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY reporter DESC, issue DESC", "Reporter descending, then Key descending",  "HSP-4", "HSP-3", "HSP-2", "MKY-1", "HSP-1");

        createSearchAndAssertIssuesAndSorts("ORDER BY resolution, issue DESC", "Resolution ascending, then Key descending", "MKY-1", "HSP-3", "HSP-4", "HSP-2", "HSP-1");
        createSearchAndAssertIssuesAndSorts("ORDER BY resolution ASC, issue DESC", "Resolution ascending, then Key descending", "MKY-1", "HSP-3", "HSP-4", "HSP-2", "HSP-1");
        createSearchAndAssertIssuesAndSorts("ORDER BY resolution DESC, issue DESC", "Resolution descending, then Key descending", "HSP-4", "HSP-2", "HSP-1", "HSP-3", "MKY-1");

        createSearchAndAssertIssuesAndSorts("ORDER BY resolved, issue DESC", "Resolved descending, then Key descending", "HSP-4", "HSP-2", "HSP-1", "HSP-3", "MKY-1");
        createSearchAndAssertIssuesAndSorts("ORDER BY resolved ASC, issue DESC", "Resolved ascending, then Key descending", "MKY-1", "HSP-3", "HSP-4", "HSP-2", "HSP-1");
        createSearchAndAssertIssuesAndSorts("ORDER BY resolved DESC, issue DESC", "Resolved descending, then Key descending", "HSP-4", "HSP-2", "HSP-1", "HSP-3", "MKY-1");

        createSearchAndAssertIssuesAndSorts("ORDER BY status, issue DESC", "Status descending, then Key descending", "MKY-1", "HSP-3", "HSP-1", "HSP-4", "HSP-2");
        createSearchAndAssertIssuesAndSorts("ORDER BY status ASC, issue DESC", "Status ascending, then Key descending", "HSP-4", "HSP-2", "HSP-1", "MKY-1", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY status DESC, issue DESC", "Status descending, then Key descending", "MKY-1", "HSP-3", "HSP-1", "HSP-4", "HSP-2");

        createSearchAndAssertIssuesAndSorts("ORDER BY summary, issue DESC", "Summary ascending, then Key descending", "MKY-1", "HSP-3", "HSP-2", "HSP-1", "HSP-4");
        createSearchAndAssertIssuesAndSorts("ORDER BY summary ASC, issue DESC", "Summary ascending, then Key descending", "MKY-1", "HSP-3", "HSP-2", "HSP-1", "HSP-4");
        createSearchAndAssertIssuesAndSorts("ORDER BY summary DESC, issue DESC", "Summary descending, then Key descending", "HSP-4", "HSP-1", "HSP-2", "HSP-3", "MKY-1");

        createSearchAndAssertIssuesAndSorts("ORDER BY timeSpent, issue DESC", "Time Spent descending, then Key descending", "MKY-1", "HSP-4", "HSP-3", "HSP-2", "HSP-1");
        createSearchAndAssertIssuesAndSorts("ORDER BY timeSpent ASC, issue DESC", "Time Spent ascending, then Key descending", "HSP-1", "HSP-2", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY timeSpent DESC, issue DESC", "Time Spent descending, then Key descending", "MKY-1", "HSP-4", "HSP-3", "HSP-2", "HSP-1");

        createSearchAndAssertIssuesAndSorts("ORDER BY type, issue DESC", "Type descending, then Key descending", "HSP-1", "MKY-1", "HSP-4", "HSP-3", "HSP-2");
        createSearchAndAssertIssuesAndSorts("ORDER BY type ASC, issue DESC", "Type ascending, then Key descending", "HSP-4", "HSP-3", "HSP-2", "MKY-1", "HSP-1");
        createSearchAndAssertIssuesAndSorts("ORDER BY issuetype DESC, issue DESC", "Type descending, then Key descending", "HSP-1", "MKY-1", "HSP-4", "HSP-3", "HSP-2");

        createSearchAndAssertIssuesAndSorts("ORDER BY updated", "Updated descending", "HSP-4", "HSP-1", "HSP-3", "MKY-1", "HSP-2");
        createSearchAndAssertIssuesAndSorts("ORDER BY updated ASC", "Updated ascending", "HSP-2", "MKY-1", "HSP-3", "HSP-1", "HSP-4");
        createSearchAndAssertIssuesAndSorts("ORDER BY updated DESC", "Updated descending", "HSP-4", "HSP-1", "HSP-3", "MKY-1", "HSP-2");

        createSearchAndAssertIssuesAndSorts("ORDER BY votes, issue DESC", "Votes descending, then Key descending", "HSP-4", "HSP-1", "MKY-1", "HSP-3", "HSP-2");
        createSearchAndAssertIssuesAndSorts("ORDER BY votes ASC, issue DESC", "Votes ascending, then Key descending", "MKY-1", "HSP-3", "HSP-2", "HSP-1", "HSP-4");
        createSearchAndAssertIssuesAndSorts("ORDER BY votes DESC, issue DESC", "Votes descending, then Key descending", "HSP-4", "HSP-1", "MKY-1", "HSP-3", "HSP-2");

        createSearchAndAssertIssuesAndSorts("ORDER BY workratio, issue DESC", "Work Ratio ascending, then Key descending", "HSP-1", "HSP-2", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY workratio ASC, issue DESC", "Work Ratio ascending, then Key descending", "HSP-1", "HSP-2", "MKY-1", "HSP-4", "HSP-3");
        createSearchAndAssertIssuesAndSorts("ORDER BY workratio DESC, issue DESC", "Work Ratio descending, then Key descending", "MKY-1", "HSP-4", "HSP-3", "HSP-2", "HSP-1");
    }

    public void testCustomFieldDefaultSortOrderings()
    {
        administration.restoreData("TestCustomFieldDefaultSortOrderings.xml");

        _testCustomFieldDefaultSortOrderings("CSF", "cf[10000]", "ONE", "2", "3", "4", "1");
        _testCustomFieldDefaultSortOrderings("DP", "cf[10001]", "TWO", "2", "3", "4", "1");
        _testCustomFieldDefaultSortOrderings("DT", "cf[10002]", "THREE", "2", "3", "4", "1");
        _testCustomFieldDefaultSortOrderings("FTF", "cf[10003]", "FOUR", "3", "2", "1");
        _testCustomFieldDefaultSortOrderings("GP", "cf[10004]", "FIVE", "3", "2", "1");
        _testCustomFieldDefaultSortOrderings("II", "cf[10005]", "SIX", "2", "3", "4", "1");
        _testCustomFieldDefaultSortOrderings("MC", "cf[10006]", "SEVEN", "2", "3", "1");
        _testCustomFieldDefaultSortOrderings("MGP", "cf[10007]", "EIGHT", "3", "2", "1");
        _testCustomFieldDefaultSortOrderings("MS", "cf[10008]", "NINE", "2", "3", "1");
        _testCustomFieldDefaultSortOrderings("MUP", "cf[10009]", "TEN", "3", "2", "1");
        _testCustomFieldDefaultSortOrderings("NF", "cf[10010]", "ELEVEN", "2", "3", "4", "1");
        _testCustomFieldDefaultSortOrderings("PP", "cf[10011]", "TWELVE", "3", "2", "1");
        _testCustomFieldDefaultSortOrderings("RB", "cf[10012]", "THIRTEEN", "2", "3", "1");
        _testCustomFieldDefaultSortOrderings("ROTF", "cf[10013]", "FOURTEEN", "3", "2", "1");
        _testCustomFieldDefaultSortOrderings("SL", "cf[10014]", "FIFTEEN", "2", "3", "1");
        _testCustomFieldDefaultSortOrderings("SVP", "cf[10015]", "SIXTEEN", "2", "3", "4", "1");
        _testCustomFieldDefaultSortOrderings("TF", "cf[10016]", "SEVENTEEN", "3", "2", "1");
        _testCustomFieldDefaultSortOrderings("URL", "cf[10017]", "EIGHTEEN", "2", "3", "1");
        _testCustomFieldDefaultSortOrderings("UP", "cf[10018]", "NINETEEN", "3", "2", "1");
        _testCustomFieldDefaultSortOrderings("VP", "cf[10019]", "TWENTY", "2", "3", "4", "1");
    }

    private void _testCustomFieldDefaultSortOrderings(String cfName, String cfId, String projectKey, String... issueNumbers)
    {
        // construct issue keys for search results
        for (int i = 0; i < issueNumbers.length; i++)
        {
            issueNumbers[i] = projectKey + "-" + issueNumbers[i];
        }

        // reverse the expected issues for descending
        final List<String> issueNumbersList = new LinkedList<String>(Arrays.asList(issueNumbers));
        Collections.reverse(issueNumbersList);
        String[] reverseIssueNumbers = new String[issueNumbers.length];
        reverseIssueNumbers = issueNumbersList.toArray(reverseIssueNumbers);

        // test both the cf name and the id alias
        final String[] ids = new String[] {cfName, cfId};

        for (String id : ids)
        {
            // default is always the same as ascending for custom fields
            // note: all projects only have one issue with an empty value, so sorting additionally
            // on issue key to clarify orderings of empty values is not required
            createSearchAndAssertIssuesAndSorts(
                    String.format("project = %s ORDER BY %s", projectKey, id),
                    String.format("%s ascending", cfName),
                    issueNumbers);

            createSearchAndAssertIssuesAndSorts(
                    String.format("project = %s ORDER BY %s ASC", projectKey, id),
                    String.format("%s ascending", cfName),
                    issueNumbers);

            createSearchAndAssertIssuesAndSorts(
                    String.format("project = %s ORDER BY %s DESC", projectKey, id),
                    String.format("%s descending", cfName),
                    reverseIssueNumbers);
        }
    }

    private void assertDoesNotSupportSorting(final String fieldName)
    {
        final String jqlQuery = String.format("ORDER BY %s", fieldName);
        final String errorMessage = String.format("Field '%s' does not support sorting.", fieldName);
        navigation.issueNavigator().createSearch(jqlQuery);
        assertions.getIssueNavigatorAssertions().assertJqlErrors(errorMessage);
    }

    private void assertNotAbleToSort(final String fieldName)
    {
        final String jqlQuery = String.format("ORDER BY %s", fieldName);
        final String errorMessage = String.format("Not able to sort using field '%s'.", fieldName);
        navigation.issueNavigator().createSearch(jqlQuery);
        assertions.getIssueNavigatorAssertions().assertJqlErrors(errorMessage);
    }

    // sometimes we use gratuitously difference phrases for the column header vs. the filter summary description.
    private final Map<String /* description */, String /* column header */> columnHeaders = MapBuilder.<String, String>newBuilder()
            .add("Component/s", "Components")
            .add("Due Date", "Due")
            .add("Security Level", "Security")
            .add("Priority", "P")
            .add("Resolution", "Resolution")
            .add("Issue Type", "T")
            .add("Type", "T")
            .toMap();

    private void createSearchAndAssertIssuesAndSorts(String jqlQuery, final String sortDescription, String... keys)
    {
        navigation.issueNavigator().createSearch(jqlQuery);
        assertions.getIssueNavigatorAssertions().assertExactIssuesInResults(keys);
        tester.clickLink("viewfilter");
        assertions.getTextAssertions().assertTextSequence(new XPathLocator(tester, "//div[@id='filter-summary']"), "Sorted by", sortDescription);

        // check that the sort arrow is on the correct column
        final String sortedColumn = new XPathLocator(tester, "//table[@id='issuetable']//tr[@class='rowHeader']//th[contains(@class, 'active')]//child::span").getText();
        final String firstSort = sortDescription.split("\\s+(ascending|descending)")[0];
        final String columnHeader = columnHeaders.containsKey(firstSort) ? columnHeaders.get(firstSort) : firstSort;
        Assert.assertEquals(sortedColumn, columnHeader);

        // and is pointed the right way
        final String direction = sortDescription.substring(firstSort.length()).trim();
        if (direction.startsWith("ascending"))
        {
            final XPathLocator arrowLocator = new XPathLocator(tester, "//table[@id='issuetable']//tr[@class='rowHeader']//th[contains(@class, 'ascending')]");
            assertions.assertNodeExists(arrowLocator);
        }
        else if (direction.startsWith("descending"))
        {
            final XPathLocator arrowLocator = new XPathLocator(tester, "//table[@id='issuetable']//tr[@class='rowHeader']//th[contains(@class, 'descending')]");
            assertions.assertNodeExists(arrowLocator);
       }
        else
        {
            fail("Failed to determine sort direction on column.");
        }

    }
}
