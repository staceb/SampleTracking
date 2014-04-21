package com.atlassian.jira.web.component;

import com.atlassian.core.user.preferences.Preferences;
import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.layout.column.ColumnLayoutItem;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.preferences.PreferenceKeys;
import com.atlassian.jira.util.JiraVelocityUtils;
import com.atlassian.jira.util.collect.CompositeMap;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.jira.web.component.subtask.ColumnLayoutItemFactory;
import com.atlassian.util.profiling.UtilTimerStack;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class displays a table of issues, and works together with issuetable.vm.
 * <p/>
 * The layout of the table is configured by an {@link IssueTableLayoutBean}
 */
public class IssueTableWebComponent extends AbstractWebComponent
{
    private final JiraAuthenticationContext authenticationContext = ComponentManager.getComponentInstanceOfType(JiraAuthenticationContext.class);

    private final ColumnLayoutItemFactory columnLayoutItemFactory = ComponentManager.getComponentInstanceOfType(ColumnLayoutItemFactory.class);

    private static final String ISSUETABLE_HEADER = "templates/jira/issue/table/issuetable-header.vm";
    private static final String ISSUETABLE_FOOTER = "templates/jira/issue/table/issuetable-footer.vm";
    private static final String ISSUETABLE_SINGLE_ISSUE = "templates/jira/issue/table/issuetable-issue.vm";
    private static final String CARROT_HIDDEN = "class=\"hide-carrot\"";

    public IssueTableWebComponent()
    {
        super(ManagerFactory.getVelocityManager(), ComponentManager.getComponentInstanceOfType(ApplicationProperties.class));
    }

    public String getHtml(final IssueTableLayoutBean layout, final List<Issue> issues, final IssuePager pager)
    {
        return getHtml(layout, issues, pager, null);
    }

    /**
     * Get a table of issues.
     *
     * @param layout The layout describing how this table should look
     * @param issues The issues to display.  These should be a list of {@link com.atlassian.jira.issue.Issue} objects
     * @param pager  An optional pager which will be used for displaying the next / previous paging at the top and bottom
     * @param selectedIssueId the issue to mark as selected in the table of issues; if null, marks the first issue as selected
     * @return An HTML table of issues
     */
    public String getHtml(final IssueTableLayoutBean layout, final List<Issue> issues, final IssuePager pager, final Long selectedIssueId)
    {
        try
        {
            UtilTimerStack.push("IssueTableHtml");
            final StringWriter writer = new StringWriter();
            try
            {
                final ColumnLayoutItem actionColumn;
                if (layout.isShowActionColumn() && (authenticationContext.getUser() != null))
                {
                    actionColumn = columnLayoutItemFactory.getActionsAndOperationsColumn();
                }
                else
                {
                    actionColumn = null;
                }

                final IssueTableWriter issueTableWriter = getHtmlIssueWriter(writer, layout, pager, actionColumn, selectedIssueId);

                for (final Issue issue : issues)
                {
                    issueTableWriter.write(issue);
                }
                issueTableWriter.close();
            }
            catch (final IOException e)
            {
                e.printStackTrace();
            }
            return writer.toString();
        }
        finally
        {
            UtilTimerStack.pop("IssueTableHtml");
        }
    }

    public IssueTableWriter getHtmlIssueWriter(final Writer writer, final IssueTableLayoutBean layout, final IssuePager pager, final ColumnLayoutItem actionColumn)
    {
        return getHtmlIssueWriter(writer, layout, pager, actionColumn, null);
    }

    /**
     * Constructs a new {@link com.atlassian.jira.web.component.IssueTableWriter} that can be used to write
     * a table of issues.
     *
     * @param writer The writer to write the table to
     * @param layout The layout describing how this table should look
     * @param pager  An optional pager which will be used for displaying the next / previous paging at the top and bottom
     * @param actionColumn The column to display on the right hand side of the table.  If null, no column is shown.
     * @param selectedIssueId the issue to mark as selected in the table; if null, marks the first issue as selected
     * @return An IssueTableWriter which the caller should use to write each issue to.
     */
    public IssueTableWriter getHtmlIssueWriter(final Writer writer, final IssueTableLayoutBean layout, final IssuePager pager, final ColumnLayoutItem actionColumn, final Long selectedIssueId)
    {
        String carrothiddenString = "";
        boolean keyboadShortcutsEnabled = true;
        if (authenticationContext.getUser() != null)
        {
            Preferences userPrefs = ComponentAccessor.getUserPreferencesManager().getPreferences(authenticationContext.getUser());
            keyboadShortcutsEnabled = !userPrefs.getBoolean(PreferenceKeys.USER_KEYBOARD_SHORTCUTS_DISABLED);
        }
        if (!keyboadShortcutsEnabled)
            carrothiddenString= CARROT_HIDDEN;

        final Map<String, Object> params = getDefaultParams(MapBuilder.<String, Object>newBuilder()
                .add("layout", layout)
                .add("i18n", authenticationContext.getI18nHelper())
                .add("pager", pager)
                .add("columnTotals", null)
                .add("actionColumn", actionColumn)
                .add("selectedIssueId", selectedIssueId)
                .add("carrothidden",carrothiddenString)
                .toMap());

        return new IssueTableWriter()
        {
            int issueCount = 0;

            public void write(final Issue issue) throws IOException
            {
                issueCount++;
                if (issueCount == 1)
                {
                    writer.write(getHtml(ISSUETABLE_HEADER, params));
                }
                final Map<String, Object> issueParams = new HashMap<String, Object>();
                issueParams.put("issue", issue);
                issueParams.put("issueCount", issueCount);
                writer.write(getHtml(ISSUETABLE_SINGLE_ISSUE, CompositeMap.of(issueParams, params)));
            }

            public void close() throws IOException
            {
                if (issueCount > 0)
                {
                    writer.write(getHtml(ISSUETABLE_FOOTER, params));
                }
            }
        };
    }

    private Map<String, Object> getDefaultParams(final Map<String, Object> startingParams)
    {
        return JiraVelocityUtils.getDefaultVelocityParams(startingParams, authenticationContext);
    }

}
