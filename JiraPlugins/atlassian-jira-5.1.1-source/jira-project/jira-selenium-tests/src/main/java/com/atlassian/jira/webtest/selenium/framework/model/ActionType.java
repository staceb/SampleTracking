package com.atlassian.jira.webtest.selenium.framework.model;

import com.atlassian.jira.webtest.framework.core.Timeouts;
import com.atlassian.jira.webtest.framework.impl.selenium.core.SeleniumContext;
import com.atlassian.jira.webtest.framework.util.Timeout;
import com.atlassian.selenium.SeleniumClient;

/**
 * Represents type of actions possible after any trigger in Selenium is invoked (e.g. link clicked).
 *
 * @since 4.2
 */
public enum ActionType
{
    // TODO this is wrong, replace it!!!
    // TODO 1) AJAX is fleaky, we dont need it
    // TODO 2) we dont want another hardcoded timeouts, use selenium context for that

    /**
     * An action type that behaves as AJAX with Kickass enabled and as NEW_PAGE without.
     */
    KICKASS_DYNAMIC
            {
                @Override
                public void waitForAction(SeleniumContext ctx, long timeout)
                {
                    final String kaJs = "with(selenium.browserbot.getCurrentWindow()){JIRA.Issues && JIRA.Issues.InlineEdit}";
                    final SeleniumClient client = ctx.client();
                    final boolean kickass = client.getEval(kaJs) != null;
                    if (kickass)
                    {
                        client.waitForAjaxWithJquery(timeout);
                    }
                    else
                    {
                        client.waitForPageToLoad(timeout);
                    }
                }

                @Override
                public long defaultTimeout(SeleniumContext ctx)
                {
                    return ctx.timeoutFor(Timeouts.PAGE_LOAD);
                }
            },

    NEW_PAGE
            {
        @Override
        public void waitForAction(SeleniumContext ctx, long timeout)
        {
            ctx.client().waitForPageToLoad(timeout);
        }
        @Override
        public long defaultTimeout(final SeleniumContext ctx)
        {
            return ctx.timeoutFor(Timeouts.PAGE_LOAD);
        }
    },
    AJAX
            {
        @Override
        public void waitForAction(SeleniumContext ctx, long timeout)
        {
            ctx.client().waitForAjaxWithJquery(timeout);
        }
        @Override
        public long defaultTimeout(final SeleniumContext ctx)
        {
            return ctx.timeouts().ajax();
        }
    },
    JAVASCRIPT
            {
        @Override
        public void waitForAction(SeleniumContext ctx, long timeout)
        {
            Timeout.waitFor(timeout).milliseconds();
        }
        @Override
        public long defaultTimeout(final SeleniumContext ctx)
        {
            return ctx.timeouts().components();
        }
    };

    public void waitForAction(SeleniumContext ctx, long timeout)
    {
        throw new AbstractMethodError();
    }

    public long defaultTimeout(final SeleniumContext ctx)
    {
        throw new AbstractMethodError();
    }

    public void waitForAction(SeleniumContext ctx)
    {
        waitForAction(ctx, defaultTimeout(ctx));
    }

}
