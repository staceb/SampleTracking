package com.atlassian.jira.pageobjects.pages;


import com.atlassian.jira.pageobjects.components.JiraHeader;
import com.atlassian.pageobjects.DelayedBinder;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.binder.WaitUntil;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.query.TimedCondition;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.atlassian.webdriver.AtlassianWebDriver;

import javax.inject.Inject;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.atlassian.pageobjects.elements.query.Poller.waitUntilTrue;


/**
 * <p>
 * Provides a set of common functions that all JIRA pages can do, such as getting the admin menu.
 * Sets the base url for the WebDrivePage class to use which is defined in the jira-base-url system property.
 *
 * @since 4.4
 */
public abstract class AbstractJiraPage implements Page
{
    private static final Pattern XSRF = Pattern.compile("atl_token=([^&?])+");

    @Inject
    protected PageBinder pageBinder;

    @Inject
    protected PageElementFinder elementFinder;

    @Inject
    protected AtlassianWebDriver driver;

    @ElementBy(tagName = "body", timeoutType = TimeoutType.PAGE_LOAD)
    protected PageElement body;

    @ElementBy(className = "footer", timeoutType = TimeoutType.PAGE_LOAD)
    protected PageElement footerElement;

    @ElementBy(className = "atlassian-token", timeoutType = TimeoutType.PAGE_LOAD)
    protected PageElement metaElement;

    public JiraHeader getHeader()
    {
        return pageBinder.bind(JiraHeader.class);
    }

    /**
     * <p>
     * The default doWait for JIRA is defined in terms of {@link #isAt()}.
     */
    @WaitUntil
    public void doWait()
    {
        waitUntilTrue(isAt());
    }

    public void execKeyboardShortcut(final CharSequence... keys)
    {
        body.type(keys);
    }

    public String getXsrfToken()
    {
        if (!metaElement.isPresent())
        {
            throw new IllegalStateException("Can't find the XSRF token on the current page.");
        }
        else
        {
            return metaElement.getText();
        }
    }

    public String createXsrfUrl(String origUrl)
    {
        final StringBuilder builder = new StringBuilder(origUrl);
        final Matcher matcher = XSRF.matcher(builder);
        if (matcher.find())
        {
            builder.replace(matcher.start(1), matcher.end(1), getXsrfToken());
        }
        else
        {
            if (builder.indexOf("?") >= 0)
            {
                builder.append("&");
            }
            else
            {
                builder.append("?");
            }
            builder.append("atl_token=").append(getXsrfToken());
        }
        return builder.toString();
    }

    /**
     * Timed condition checking if we're at given page.
     *
     * @return timed condition checking, if the test is at given page
     */
    public abstract  TimedCondition isAt();

    public boolean isLoggedIn()
    {
        DelayedBinder<JiraHeader> header = pageBinder.delayedBind(JiraHeader.class);
        return header.canBind() ? header.bind().isLoggedIn() : false;
    }

    public boolean isAdmin()
    {
        DelayedBinder<JiraHeader> header = pageBinder.delayedBind(JiraHeader.class);
        return header.canBind() ? header.bind().isAdmin() : false;
    }

    public <P> P back(Class<P> binder, Object ... arguments)
    {
        driver.navigate().back();
        return pageBinder.bind(binder, arguments);
    }
}