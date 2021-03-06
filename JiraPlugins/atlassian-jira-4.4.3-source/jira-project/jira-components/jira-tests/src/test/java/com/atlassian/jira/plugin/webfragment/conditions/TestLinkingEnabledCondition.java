package com.atlassian.jira.plugin.webfragment.conditions;

import com.atlassian.jira.local.ListeningTestCase;
import org.junit.Test;
import static org.junit.Assert.*;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import org.easymock.EasyMock;
import static org.easymock.EasyMock.expect;
import org.easymock.IMocksControl;

public class TestLinkingEnabledCondition extends ListeningTestCase
{
    @Test
    public void testTrue()
    {
        final IMocksControl mocksControl = EasyMock.createControl();
        final ApplicationProperties applicationProperties = mocksControl.createMock(ApplicationProperties.class);

        expect(applicationProperties.getOption(APKeys.JIRA_OPTION_ISSUELINKING)).andReturn(true);

        final LinkingEnabledCondition condition = new LinkingEnabledCondition(applicationProperties);

        mocksControl.replay();
        assertTrue(condition.shouldDisplay(null, null));
        mocksControl.verify();

    }

    @Test
    public void testFalse()
    {
        final IMocksControl mocksControl = EasyMock.createControl();
        final ApplicationProperties applicationProperties = mocksControl.createMock(ApplicationProperties.class);

        expect(applicationProperties.getOption(APKeys.JIRA_OPTION_ISSUELINKING)).andReturn(false);

        final LinkingEnabledCondition condition = new LinkingEnabledCondition(applicationProperties);

        mocksControl.replay();
        assertFalse(condition.shouldDisplay(null, null));
        mocksControl.verify();

    }

}
