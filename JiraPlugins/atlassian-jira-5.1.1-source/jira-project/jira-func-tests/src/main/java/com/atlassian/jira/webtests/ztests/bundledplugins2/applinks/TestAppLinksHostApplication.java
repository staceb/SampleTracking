package com.atlassian.jira.webtests.ztests.bundledplugins2.applinks;

import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.jira.webtests.ztests.bundledplugins2.rest.RestFuncTest;
import com.meterware.httpunit.WebResponse;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.internal.matchers.IsCollectionContaining.hasItems;

/**
 * Functional tests for the JIRA's InternalHostApplication implementation. These tests are mostly smoke tests.
 *
 * @since v4.3
 */
@WebTest({ Category.FUNC_TEST, Category.APP_LINKS })
public class TestAppLinksHostApplication extends RestFuncTest
{
    /**
     * The name of the plugin that must be present.
     */
    private static final String FUNC_TEST_PLUGIN_NAME = "com.atlassian.jira.dev.func-test-plugin";
    private static final String PROJECT_ENTITY_TYPE_NAME = "com.atlassian.applinks.api.application.jira.JiraProjectEntityType";
    private static final Logger log = Logger.getLogger(TestAppLinksHostApplication.class);

    public void testDocumentationBaseUrlShouldPointToAppLinksDocs() throws Exception
    {
        if (!isBackdoorAvailable()) { return; }

        WebResponse docBaseUrl = GET("rest/func-test/1.0/applinks/getDocumentationBaseUrl");
        assertThat(docBaseUrl.getText(), equalTo("http://confluence.atlassian.com/display/APPLINKS"));
    }

    public void testApplicationNameShouldReturnJiraName() throws Exception
    {
        if (!isBackdoorAvailable()) { return; }

        WebResponse name = GET("rest/func-test/1.0/applinks/getName");
        assertThat(name.getText(), equalTo("jWebTest JIRA installation"));
    }

    public void testApplicationTypeShouldReturnJira() throws Exception
    {
        if (!isBackdoorAvailable()) { return; }

        WebResponse name = GET("rest/func-test/1.0/applinks/getType");
        JSONObject type = new JSONObject(name.getText());
        assertThat(type.getString("i18nKey"), equalTo("applinks.jira"));
    }

    public void testLocalEntitiesShouldReturnAllProjects() throws Exception
    {
        if (!isBackdoorAvailable()) { return; }
        
        WebResponse response = GET("rest/func-test/1.0/applinks/getLocalEntities");
        JSONArray entities = new JSONObject(response.getText()).getJSONArray("entities");
        assertThat(entities.length(), equalTo(2));

        List<String> projectKeys = new ArrayList<String>();
        for (int i = 0; i < entities.length(); i++)
        {
            JSONObject project = entities.getJSONObject(i);
            projectKeys.add(project.getString("key"));
        }

        assertThat(projectKeys, hasItems("HSP", "MKY"));
    }

    public void testHasPublicSignup() throws Exception
    {
        if (!isBackdoorAvailable()) { return; }

        WebResponse response = GET("rest/func-test/1.0/applinks/hasPublicSignup");
        assertThat(response.getText(), equalTo("true"));
    }

    @Override
    protected void setUpTest()
    {
        super.setUpTest();
        administration.restoreBlankInstance();
        navigation.gotoAdmin();
    }

    protected final boolean isBackdoorAvailable()
    {
        
        boolean available = administration.plugins().isPluginInstalled(FUNC_TEST_PLUGIN_NAME);
        if (!available)
        {
            log.info(String.format("Plugin '%s' is not installed", FUNC_TEST_PLUGIN_NAME));
        }

        return available;
    }
}
