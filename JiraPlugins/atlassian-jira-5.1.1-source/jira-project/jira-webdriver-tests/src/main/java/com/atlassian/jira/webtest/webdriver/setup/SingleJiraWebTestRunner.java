package com.atlassian.jira.webtest.webdriver.setup;

import com.atlassian.browsers.BrowserType;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.config.EnvironmentBasedProductInstance;
import com.atlassian.jira.pageobjects.config.junit4.DumpPageSourceListener;
import com.atlassian.jira.pageobjects.config.junit4.JiraWebTestRunner;
import com.atlassian.jira.pageobjects.config.junit4.LogPageSourceListener;
import com.atlassian.jira.pageobjects.config.junit4.ScreenshotListener;
import com.google.common.collect.Lists;
import org.junit.runner.notification.RunListener;
import org.junit.runners.model.InitializationError;

/**
 * Runner for JIRA web tests to run as single test classes, constructor-compatible with the
 * {@link org.junit.runner.RunWith} annotation
 *
 * @since v4.4
 */
public class SingleJiraWebTestRunner extends JiraWebTestRunner
{
    static
    {
        final String browserString = System.getProperty("webdriver.browser", BrowserType.FIREFOX.getName());
        System.setProperty("webdriver.browser", browserString);
    }

    private static final JiraTestedProduct product = newLocalTestedProduct();

    public SingleJiraWebTestRunner(Class<?> klass) throws InitializationError
    {
        super(klass, product, listeners());
    }

    private static JiraTestedProduct newLocalTestedProduct()
    {
        return new JiraTestedProduct(new EnvironmentBasedProductInstance());
    }

    private static Iterable<RunListener> listeners()
    {
        return Lists.newArrayList(
                LogPageSourceListener.asRunListener(),
                DumpPageSourceListener.asRunListener(),
                ScreenshotListener.asRunListener());
    }
}
