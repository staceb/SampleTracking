package com.atlassian.jira.charts.report;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.local.LegacyJiraMockTestCase;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.web.action.ProjectActionSupport;
import com.mockobjects.constraint.Constraint;
import com.mockobjects.constraint.IsAnything;
import com.mockobjects.constraint.IsEqual;
import com.mockobjects.dynamic.Mock;

import java.util.Map;

public abstract class AbstractChartReportTestCase extends LegacyJiraMockTestCase
{
    protected Mock projectManager;
    protected Mock searchRequestService;
    protected Mock applicationProperties;
    protected AbstractChartReport report;
    private ProjectActionSupport action;

    private static final String VALID_PROJECT_ID = "10000";
    private static final String MAX_DAYS_PREVIOUS = String.valueOf(Integer.MAX_VALUE);
    private static final String MAX_PROJECT_OR_FILTER_ID = String.valueOf(Long.MAX_VALUE);

    protected void setUp() throws Exception
    {
        super.setUp();
        applicationProperties = new Mock(ApplicationProperties.class);
        projectManager = new Mock(ProjectManager.class);
        searchRequestService = new Mock(SearchRequestService.class);
        action = new ProjectActionSupport(null, null)
        {
            public String getText(final String key)
            {
                return key;
            }

            public String getText(final String key, final String value1)
            {
                return key;
            }

            public String getText(final String key, final Object value1)
            {
                return key;
            }
        };
        report = getChartReport();
    }

    /**
     * This is used to initialise {@link #report} to the instance of the extending {@link AbstractChartReport}
     * so that the tests defined in this test case can be re-used.
     * @return {@link AbstractChartReport} instance of the extending report.
     */
    public abstract AbstractChartReport getChartReport();

    public void _testDaysPreviousValidation()
    {
        //setup valid projectOrFilterId so its not included in the errors
        Map params = EasyMap.build("projectOrFilterId", "project-" + VALID_PROJECT_ID);
        params.put("periodName", "daily");
        projectManager.expectAndReturn("getProjectObj", new Long(VALID_PROJECT_ID), new Mock(Project.class).proxy());

        //null
        params.put("daysprevious", null);
        validateAndAssertOnlyErrorForFieldIs(params, "daysprevious", "report.error.days.previous.not.a.number");

        //no input
        params.put("daysprevious", "");
        validateAndAssertOnlyErrorForFieldIs(params, "daysprevious", "report.error.days.previous.not.a.number");

        //negative
        params.put("daysprevious", "-1");
        validateAndAssertOnlyErrorForFieldIs(params, "daysprevious", "report.error.days.previous");

        //overflow
        params.put("daysprevious", MAX_DAYS_PREVIOUS + "0");
        validateAndAssertOnlyErrorForFieldIs(params, "daysprevious", "report.error.days.previous.not.a.number");

        //max valid value for daily period
        params.put("daysprevious", "300");
        validateAndAssertNoErrorForField(params);

        params.put("daysprevious", "301");
        validateAndAssertOnlyErrorForFieldIs(params, "daysprevious", "report.error.days.previous.period");

        //now try a nother period
        params.put("daysprevious", "301");
        params.put("periodName", "monthly");
        validateAndAssertNoErrorForField(params);

        //exceed the monthly limit
        params.put("daysprevious", "17000");
        params.put("periodName", "monthly");
        validateAndAssertOnlyErrorForFieldIs(params, "daysprevious", "report.error.days.previous.period");

        params.put("daysprevious", "30");
        validateAndAssertNoErrorForField(params);
    }

    public void _testProjectOrFilterIdValidation()
    {
        //setup valid projectOrFilterId so its not included in the errors
        Map params = EasyMap.build("daysprevious", "30", "periodName", "daily");

        params.put("projectOrFilterId", null);
        validateAndAssertOnlyErrorForFieldIs(params, "projectOrFilterId", "report.error.no.filter.or.project");

        params.put("projectOrFilterId", "");
        validateAndAssertOnlyErrorForFieldIs(params, "projectOrFilterId", "report.error.no.filter.or.project");

        params.put("projectOrFilterId", "-");
        validateAndAssertOnlyErrorForFieldIs(params, "projectOrFilterId", "report.error.no.filter.or.project");

        params.put("projectOrFilterId", "random");
        validateAndAssertOnlyErrorForFieldIs(params, "projectOrFilterId", "report.error.no.filter.or.project");

        params.put("projectOrFilterId", "random-");
        validateAndAssertOnlyErrorForFieldIs(params, "projectOrFilterId", "report.error.no.filter.or.project");

        params.put("projectOrFilterId", "random-1000");
        validateAndAssertOnlyErrorForFieldIs(params, "projectOrFilterId", "report.error.no.filter.or.project");
    }

    public void _testFilterIdValidation()
    {
        //setup valid projectOrFilterId so its not included in the errors
        Map params = EasyMap.build("daysprevious", "30", "periodName", "daily");

        params.put("projectOrFilterId", "filter");
        validateAndAssertOnlyErrorForFieldIs(params, "projectOrFilterId", "report.error.no.filter.or.project");

        params.put("projectOrFilterId", "filter-");
        validateAndAssertOnlyErrorForFieldIs(params, "projectOrFilterId", "report.error.no.filter.or.project");

        //invalid filter (ie. filter is null)
        String filterId = "10000";
        params.put("projectOrFilterId", "filter-" + filterId);
        searchRequestService.expectAndReturn("getFilter", new Constraint[]{new IsAnything(), new IsEqual(new Long(filterId))}, null);
        validateAndAssertOnlyErrorForFieldIs(params, "projectOrFilterId", "report.error.no.filter");
        searchRequestService.verify();

        //filter id overflow
        filterId = MAX_PROJECT_OR_FILTER_ID + "0";
        params.put("projectOrFilterId", "filter-" + filterId);
        validateAndAssertOnlyErrorForFieldIs(params, "projectOrFilterId", "report.error.filter.id.not.a.number");
    }

    public void _testProjectIdValidation()
    {
        //setup valid projectOrFilterId so its not included in the errors
        Map params = EasyMap.build("daysprevious", "30", "periodName", "daily");

        params.put("projectOrFilterId", "project");
        validateAndAssertOnlyErrorForFieldIs(params, "projectOrFilterId", "report.error.no.filter.or.project");

        params.put("projectOrFilterId", "project-");
        validateAndAssertOnlyErrorForFieldIs(params, "projectOrFilterId", "report.error.no.filter.or.project");

        //invalid filter (ie. filter is null)
        String projectId = "10000";
        params.put("projectOrFilterId", "project-" + projectId);
        projectManager.expectAndReturn("getProjectObj", new Long(projectId), null);
        validateAndAssertOnlyErrorForFieldIs(params, "projectOrFilterId", "report.error.project.id.not.found");
        projectManager.verify();

        //filter id overflow
        projectId = MAX_PROJECT_OR_FILTER_ID + "0";
        params.put("projectOrFilterId", "project-" + projectId);
        validateAndAssertOnlyErrorForFieldIs(params, "projectOrFilterId", "report.error.project.id.not.a.number");
        action.getErrors().clear();//clear the errors for the next test

        //valid project id
        projectId = "10000";
        params.put("projectOrFilterId", "project-" + projectId);
        projectManager.expectAndReturn("getProjectObj", new Long(projectId), new Mock(Project.class).proxy());
        validateAndAssertNoErrorForField(params);
        projectManager.verify();
    }

    private void validateAndAssertOnlyErrorForFieldIs(Map params, String field, String expectedErrorMsg)
    {
        report.validate(action, params);
        assertEquals(1, action.getErrors().size());
        assertEquals(expectedErrorMsg, action.getErrors().get(field));
        action.getErrors().clear();//clear the errors for the next test
    }

    private void validateAndAssertNoErrorForField(Map params)
    {
        report.validate(action, params);
        assertTrue(action.getErrors().isEmpty());
        assertFalse(action.hasAnyErrors());
    }
}