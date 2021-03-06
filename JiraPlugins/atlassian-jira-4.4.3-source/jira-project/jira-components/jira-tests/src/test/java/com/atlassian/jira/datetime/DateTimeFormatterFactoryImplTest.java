package com.atlassian.jira.datetime;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.easymock.EasyMockAnnotations;
import com.atlassian.jira.easymock.Mock;
import com.atlassian.jira.event.ClearCacheEvent;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.timezone.TimeZoneService;
import com.atlassian.jira.util.I18nHelper;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static com.atlassian.com.atlassian.jira.event.HasEventListenerFor.hasEventListenerFor;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.classextension.EasyMock.replay;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.matchers.JUnitMatchers.hasItems;
import static org.powermock.api.easymock.PowerMock.verify;

/**
 * Tests for DateTimeFormatterFactoryImpl.
 *
 * @since 4.4
 */
public class DateTimeFormatterFactoryImplTest
{
    @Mock
    private TimeZoneService timeZoneService;

    @Mock
    private JiraAuthenticationContext jiraAuthenticationContext;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private I18nHelper.BeanFactory beanFactory;

    @Test
    public void factoryShouldSupportAllStyles() throws Exception
    {
        DateTimeFormatterFactoryImpl factory = newDateTimeFormatterImpl();

        // make sure that all possible styles are handled
        assertThat(factory.formatters.keySet(), hasItems(DateTimeStyle.values()));
        for (Map.Entry<DateTimeStyle, DateTimeFormatStrategy> entry : factory.formatters.entrySet())
        {
            assertThat(entry.getValue().style(), equalTo(entry.getKey()));
        }
    }

    @Test
    public void factoryShouldRegisterItselfInEventPublisherWhenStartIsCalled() throws Exception
    {
        DateTimeFormatterFactoryImpl dateTimeFormatterFactory = newDateTimeFormatterImpl();
        
        eventPublisher.register(dateTimeFormatterFactory);
        expectLastCall();
        replay(eventPublisher);
        dateTimeFormatterFactory.start();
        verify(eventPublisher);
    }

    @Test
    public void factoryShouldHandleOnClearCacheEvent() throws Exception
    {
        assertThat(DateTimeFormatterFactoryImpl.class, hasEventListenerFor(ClearCacheEvent.class));
    }

    @Test
    public void formatterDefaultsToRelative() throws Exception
    {
        final DateTimeFormatter formatter = newDateTimeFormatterImpl().formatter();
        assertThat(formatter.getZone(), equalTo(null));
        assertThat(formatter.getLocale(), equalTo(null));
        assertThat(formatter.getStyle(), equalTo(DateTimeStyle.RELATIVE));
    }

    @Before
    public void setUp() throws Exception
    {
        EasyMockAnnotations.initMocks(this);
    }

    protected DateTimeFormatterFactoryImpl newDateTimeFormatterImpl()
    {
        return new DateTimeFormatterFactoryImpl(timeZoneService, jiraAuthenticationContext, applicationProperties, eventPublisher, beanFactory);
    }
}
