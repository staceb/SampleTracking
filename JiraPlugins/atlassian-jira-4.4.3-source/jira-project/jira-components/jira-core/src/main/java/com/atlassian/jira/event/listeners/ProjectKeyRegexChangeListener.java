package com.atlassian.jira.event.listeners;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.bc.admin.ApplicationPropertyMetadata;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.event.config.ApplicationPropertyChangeEvent;
import com.atlassian.jira.extension.Startable;
import com.atlassian.jira.util.JiraKeyUtils;
import org.apache.log4j.Logger;

import java.util.Map;

/**
 * Listens for {@link com.atlassian.jira.event.config.ApplicationPropertyChangeEvent} events in case the project key
 * regex changes and resets the key matcher accordingly.
 *
 * @since v4.4
 */
public class ProjectKeyRegexChangeListener implements Startable
{

    private static final Logger log = Logger.getLogger(ProjectKeyRegexChangeListener.class);
    private EventPublisher eventPublisher;

    public ProjectKeyRegexChangeListener(EventPublisher eventPublisher)
    {
        this.eventPublisher = eventPublisher;
    }

    @EventListener
    public static void onApplicationPropertyChange(ApplicationPropertyChangeEvent event)
    {
        try
        {
            Map params = event.getParams();
            ApplicationPropertyMetadata metadata = (ApplicationPropertyMetadata) params.get(ApplicationPropertyChangeEvent.KEY_METADATA);
            if (APKeys.JIRA_PROJECTKEY_PATTERN.equals(metadata.getKey()))
            {
                log.info("Resetting the issue key matcher");
                JiraKeyUtils.resetKeyMatcher();
            }
        }
        catch (Exception e)
        {
            log.error("Unable to decide whether to reset the project key matcher because the event doesn't seem to contain the correct metadata");
        }
    }

    @Override
    public void start() throws Exception
    {
        eventPublisher.register(this);
    }
}
