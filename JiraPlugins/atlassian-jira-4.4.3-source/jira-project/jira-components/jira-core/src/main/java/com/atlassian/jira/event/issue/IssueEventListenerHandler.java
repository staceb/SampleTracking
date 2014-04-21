package com.atlassian.jira.event.issue;

import com.atlassian.event.spi.ListenerHandler;
import com.atlassian.event.spi.ListenerInvoker;
import org.apache.log4j.Logger;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Used to invoke legacy issue events within atlassian-events.
 *
 * @since v4.1
 */
public class IssueEventListenerHandler implements ListenerHandler
{
    private static final Logger log = Logger.getLogger(IssueEventListenerHandler.class);

    public List<? extends ListenerInvoker> getInvokers(final Object o)
    {
        if (o instanceof IssueEventListener)
        {
            return Collections.singletonList(new IssueEventInvoker((IssueEventListener) o));
        }
        return Collections.emptyList();
    }

    class IssueEventInvoker implements ListenerInvoker
    {
        private final IssueEventListener issueEventListener;

        public IssueEventInvoker(final IssueEventListener issueEventListener)
        {
            this.issueEventListener = issueEventListener;
        }

        public Set<Class<?>> getSupportedEventTypes()
        {
            return Collections.<Class<?>>singleton(IssueEvent.class);
        }

        public void invoke(final Object o)
        {
            if (o instanceof IssueEvent)
            {
                try
                {
                    issueEventListener.workflowEvent((IssueEvent) o);
                }
                // NOTE: this is crap, but it is how JIRA used to behave, funny part is that we do not do this for UserEventListners
                catch (Throwable t)
                {
                    log.error("Exception thrown from listener [" + issueEventListener.getClass().getName() + "] : " + t.getMessage(), t);
                }
            }
        }

        public boolean supportAsynchronousEvents()
        {
            return false;
        }
    }
}
