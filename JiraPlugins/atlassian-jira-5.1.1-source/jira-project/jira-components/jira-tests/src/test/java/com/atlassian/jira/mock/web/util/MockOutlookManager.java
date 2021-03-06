package com.atlassian.jira.mock.web.util;

import com.atlassian.jira.web.util.OutlookDate;
import com.atlassian.jira.web.util.OutlookDateManager;

import java.util.Locale;

/**
 * Simple mock for the OutlookManager.
 *
 * @since v3.13
 */
public class MockOutlookManager implements OutlookDateManager
{
    public void refresh()
    {
    }

    public OutlookDate getOutlookDate(Locale locale)
    {
        return new MockOutlookDate(locale);
    }
}
