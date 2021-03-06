package com.atlassian.jira.issue.managers;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.ModifiedValue;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.OperationContext;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.fields.OrderableField;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.renderer.IssueRenderContext;
import com.atlassian.jira.issue.fields.renderer.JiraRendererPlugin;
import com.atlassian.jira.issue.fields.renderer.RenderableField;
import com.atlassian.jira.issue.fields.renderer.text.DefaultTextRenderer;
import com.atlassian.jira.issue.fields.renderer.wiki.AtlassianWikiRenderer;
import com.atlassian.jira.issue.fields.screen.FieldScreenRenderLayoutItem;
import com.atlassian.jira.issue.fields.util.MessagedResult;
import com.atlassian.jira.issue.search.SearchHandler;
import com.atlassian.jira.issue.util.IssueChangeHolder;
import com.atlassian.jira.local.ListeningTestCase;
import com.atlassian.jira.plugin.renderer.JiraRendererModuleDescriptor;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.TestJiraKeyUtils;
import com.atlassian.jira.web.bean.BulkEditBean;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.renderer.v2.V2Renderer;
import com.google.common.collect.ImmutableList;
import org.easymock.EasyMock;
import org.easymock.MockControl;
import org.easymock.classextension.MockClassControl;
import org.junit.Before;
import org.junit.Test;
import webwork.action.Action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Unit tests for the DefaultRendererManager.
 */
public class TestDefaultRendererManager extends ListeningTestCase
{
    protected DefaultRendererManager rendererManager;

    private PluginAccessor mockPluginAccessor;
    private EventPublisher mockPublisher;
    private MockControl ctrlPluginAccessor;

    private List<JiraRendererModuleDescriptor> rendererDescriptors = new ArrayList<JiraRendererModuleDescriptor>();
    private List<JiraRendererPlugin> renderers;
    private JiraRendererModuleDescriptor mockTextRendererModuleDescriptor;
    private JiraRendererModuleDescriptor mockWikiRendererModuleDescriptor;
    private AtlassianWikiRenderer wikiRenderer;
    final DefaultTextRenderer textRenderer = new DefaultTextRenderer();

    @Before
    public void setUp() throws Exception
    {
        mockPublisher = EasyMock.createMock(EventPublisher.class);
        wikiRenderer = new AtlassianWikiRenderer(mockPublisher);
        renderers = ImmutableList.of(textRenderer, wikiRenderer);

        ctrlPluginAccessor = MockControl.createStrictControl(PluginAccessor.class);
        mockPluginAccessor = (PluginAccessor) ctrlPluginAccessor.getMock();


        MockControl ctrlTextRendererModuleDescriptor = MockClassControl.createControl(JiraRendererModuleDescriptor.class);
        mockTextRendererModuleDescriptor = (JiraRendererModuleDescriptor) ctrlTextRendererModuleDescriptor.getMock();
        mockTextRendererModuleDescriptor.getModule();
        ctrlTextRendererModuleDescriptor.setDefaultReturnValue(textRenderer);

        MockControl ctrlWikiRendererModuleDescriptor = MockClassControl.createControl(JiraRendererModuleDescriptor.class);
        mockWikiRendererModuleDescriptor = (JiraRendererModuleDescriptor) ctrlWikiRendererModuleDescriptor.getMock();
        mockWikiRendererModuleDescriptor.getModule();
        ctrlWikiRendererModuleDescriptor.setDefaultReturnValue(wikiRenderer);

        rendererDescriptors.add(mockTextRendererModuleDescriptor);
        rendererDescriptors.add(mockWikiRendererModuleDescriptor);

        ctrlTextRendererModuleDescriptor.replay();
        ctrlWikiRendererModuleDescriptor.replay();

        rendererManager = new DefaultRendererManager(mockPluginAccessor);

        TestJiraKeyUtils.setKeyMatcher(new TestJiraKeyUtils.MockKeyMatcher(""));
    }

    @Test
    public void testGetAllActiveRenderers()
    {
        setupToReturnAll();

        assertEquals(rendererManager.getAllActiveRenderers().size(), 2);
    }

    @Test
    public void testGetRendererForTypeGetsNamedRenderer()
    {
        setupToReturnTextOrWiki();

        JiraRendererPlugin renderer = rendererManager.getRendererForType(V2Renderer.RENDERER_TYPE);
        assertEquals(renderer.getRendererType(), V2Renderer.RENDERER_TYPE);
    }

    @Test
    public void testGetRendererForTypeFallsToDefault()
    {
        setupToReturnDefault();

        JiraRendererPlugin renderer = rendererManager.getRendererForType("noRenderer");
        assertEquals(renderer.getRendererType(), DefaultTextRenderer.RENDERER_TYPE);
    }

    @Test
    public void testGetRendererForTypeDefaultNotAvailable()
    {
        // simulate the default being disabled
        mockPluginAccessor.getEnabledModulesByClass(JiraRendererPlugin.class);
        ctrlPluginAccessor.setDefaultReturnValue(Collections.emptyList());
        ctrlPluginAccessor.replay();

        try
        {
            rendererManager.getRendererForType("noRenderer");
            fail();
        }
        catch (IllegalStateException ise)
        {
            // this is good
        }
    }

    @Test
    public void testGetRendererForFieldNullField()
    {
        setupToReturnDefault();

        assertEquals(rendererManager.getRendererForField(null).getRendererType(), DefaultTextRenderer.RENDERER_TYPE);
    }

    @Test
    public void testGetRendererForFieldValidField()
    {
        // Setup the fieldLayoutItem mock
        MockControl ctrlFieldLayoutItem = MockClassControl.createControl(FieldLayoutItem.class);
        FieldLayoutItem mockFieldLayoutItem = (FieldLayoutItem) ctrlFieldLayoutItem.getMock();
        mockFieldLayoutItem.getRendererType();
        ctrlFieldLayoutItem.setDefaultReturnValue(V2Renderer.RENDERER_TYPE);
        ctrlFieldLayoutItem.replay();

        setupToReturnTextOrWiki();

        assertEquals(rendererManager.getRendererForField(mockFieldLayoutItem).getRendererType(), V2Renderer.RENDERER_TYPE);
    }

    @Test
    public void testGetRenderedContentSimple()
    {
        MockControl ctrlIssueContext = MockClassControl.createControl(IssueRenderContext.class);
        IssueRenderContext mockIssueContext = (IssueRenderContext) ctrlIssueContext.getMock();

        setupToReturnTextOrWiki();

        assertEquals(rendererManager.getRenderedContent(DefaultTextRenderer.RENDERER_TYPE, "hello", mockIssueContext), "hello");
    }

    @Test
    public void testGetRenderedContentFromRenderableField()
    {
        // Create a mock issue
        MockControl ctrlIssue = MockClassControl.createControl(Issue.class);
        Issue mockIssue = (Issue) ctrlIssue.getMock();

        // fill with a mock issue context
        MockControl ctrlIssueContext = MockClassControl.createControl(IssueRenderContext.class);
        IssueRenderContext mockIssueContext = (IssueRenderContext) ctrlIssueContext.getMock();

        mockIssue.getIssueRenderContext();
        ctrlIssue.setDefaultReturnValue(mockIssueContext);

        ctrlIssue.replay();

        // setup a field config to return the render type and field
        MockControl ctrlFieldLayoutItem = MockClassControl.createControl(FieldLayoutItem.class);
        FieldLayoutItem mockFieldLayoutItem = (FieldLayoutItem) ctrlFieldLayoutItem.getMock();
        mockFieldLayoutItem.getRendererType();
        ctrlFieldLayoutItem.setDefaultReturnValue(DefaultTextRenderer.RENDERER_TYPE);
        mockFieldLayoutItem.getOrderableField();
        ctrlFieldLayoutItem.setDefaultReturnValue(new FakeOrderableRenderableField());
        ctrlFieldLayoutItem.replay();

        setupToReturnTextOrWiki();

        assertEquals(rendererManager.getRenderedContent(mockFieldLayoutItem, mockIssue), "test");
    }

    @Test
    public void testGetRenderedContentFromNonRenderableField()
    {
        // setup a field config to return the render type and field
        MockControl ctrlFieldLayoutItem = MockClassControl.createControl(FieldLayoutItem.class);
        FieldLayoutItem mockFieldLayoutItem = (FieldLayoutItem) ctrlFieldLayoutItem.getMock();
        mockFieldLayoutItem.getOrderableField();
        ctrlFieldLayoutItem.setDefaultReturnValue(new FakeOrderableField());
        ctrlFieldLayoutItem.replay();

        setupToReturnTextOrWiki();

        try
        {
            assertEquals(rendererManager.getRenderedContent(mockFieldLayoutItem, null), "test");
            fail();
        }
        catch(IllegalArgumentException iae)
        {
            // this means we passed the test
        }
    }

    private void setupToReturnDefault()
    {
        mockPluginAccessor.getEnabledModulesByClass(JiraRendererPlugin.class);
        ctrlPluginAccessor.setDefaultReturnValue(renderers);
        mockPluginAccessor.getEnabledPluginModule("com.atlassian.jira.plugin.system.jirarenderers:jira-text-renderer");
        ctrlPluginAccessor.setReturnValue(mockTextRendererModuleDescriptor, 1);
        ctrlPluginAccessor.replay();
    }

    private void setupToReturnTextOrWiki()
    {
        mockPluginAccessor.getEnabledModulesByClass(JiraRendererPlugin.class);
        ctrlPluginAccessor.setDefaultReturnValue(renderers);
        ctrlPluginAccessor.replay();
    }

    private void setupToReturnAll()
    {
        mockPluginAccessor.getEnabledModuleDescriptorsByClass(JiraRendererModuleDescriptor.class);
        ctrlPluginAccessor.setDefaultReturnValue(rendererDescriptors);
        ctrlPluginAccessor.replay();
    }

    private class FakeOrderableRenderableField extends FakeOrderableField implements RenderableField
    {

        public String getValueFromIssue(Issue issue)
        {
            return "test";
        }

        public boolean isRenderable()
        {
            return true;
        }
    }

    private class FakeOrderableField implements OrderableField
    {

        public String getCreateHtml(FieldLayoutItem fieldLayoutItem, OperationContext operationContext, Action action, Issue issue)
        {
            return null;
        }

        public String getCreateHtml(FieldLayoutItem fieldLayoutItem, OperationContext operationContext, Action action, Issue issue, Map displayParameters)
        {
            return null;
        }

        public String getEditHtml(FieldLayoutItem fieldLayoutItem, OperationContext operationContext, Action action, Issue issue)
        {
            return null;
        }

        public String getEditHtml(FieldLayoutItem fieldLayoutItem, OperationContext operationContext, Action action, Issue issue, Map displayParameters)
        {
            return null;
        }

        public String getBulkEditHtml(OperationContext operationContext, Action action, BulkEditBean bulkEditBean, Map displayParameters)
        {
            return null;
        }

        public String getViewHtml(FieldLayoutItem fieldLayoutItem, Action action, Issue issue)
        {
            return null;
        }

        public String getViewHtml(FieldLayoutItem fieldLayoutItem, Action action, Issue issue, Map displayParameters)
        {
            return null;
        }

        public String getViewHtml(FieldLayoutItem fieldLayoutItem, Action action, Issue issue, Object value, Map displayParameters)
        {
            return null;
        }

        public boolean isShown(Issue issue)
        {
            return false;
        }

        public void populateDefaults(Map fieldValuesHolder, Issue issue)
        {
            //do nothing.
        }

        public void populateFromParams(Map fieldValuesHolder, Map parameters)
        {
            //do nothing.
        }

        public void populateFromIssue(Map fieldValuesHolder, Issue issue)
        {
            //do nothing.
        }

        public void validateParams(OperationContext operationContext, ErrorCollection errorCollectionToAddTo, I18nHelper i18n, Issue issue, FieldScreenRenderLayoutItem fieldScreenRenderLayoutItem)
        {
            //do nothing.
        }

        public Object getDefaultValue(Issue issue)
        {
            return null;
        }

        public void createValue(Issue issue, Object value)
        {
            //do nothing.
        }

        public void updateValue(FieldLayoutItem fieldLayoutItem, Issue issue, ModifiedValue modifiedValue, IssueChangeHolder issueChangeHolder)
        {
            //do nothing.
        }

        public void updateIssue(FieldLayoutItem fieldLayoutItem, MutableIssue issue, Map fieldValueHolder)
        {
            //do nothing.
        }

        public MessagedResult needsMove(Collection originalIssues, Issue targetIssue, FieldLayoutItem targetFieldLayoutItem)
        {
            return null;
        }

        public void populateForMove(Map fieldValuesHolder, Issue originalIssue, Issue targetIssue)
        {
            //do nothing.
        }

        public void removeValueFromIssueObject(MutableIssue issue)
        {
            //do nothing.
        }

        public boolean canRemoveValueFromIssueObject(Issue issue)
        {
            return false;
        }

        public boolean hasValue(Issue issue)
        {
            return false;
        }

        public String availableForBulkEdit(BulkEditBean bulkEditBean)
        {
            return null;
        }

        public Object getValueFromParams(Map params) throws FieldValidationException
        {
            return null;
        }

        public void populateParamsFromString(Map fieldValuesHolder, String stringValue, Issue issue) throws FieldValidationException
        {
        }

        public String getId()
        {
            return null;
        }

        public String getNameKey()
        {
            return null;
        }

        public String getName()
        {
            return null;
        }

        public int compareTo(Object o)
        {
            return 0;
        }

        public SearchHandler createAssociatedSearchHandler()
        {
            return null;
        }
    }

}
