package com.atlassian.jira.plugin.customfield;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.issue.IssueImpl;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.customfields.CustomFieldSearcher;
import com.atlassian.jira.issue.customfields.CustomFieldUtils;
import com.atlassian.jira.issue.customfields.CustomFieldValueProvider;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.search.SearchContext;
import com.atlassian.jira.issue.transport.FieldValuesHolder;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.module.ModuleFactory;
import org.dom4j.Element;
import webwork.action.Action;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


public class CustomFieldSearcherModuleDescriptorImpl extends CustomFieldSearcherModuleDescriptor
{
    private CustomFieldSearcher cfSearcher;
    private Set validCustomFieldKeys = new HashSet();

    public CustomFieldSearcherModuleDescriptorImpl(JiraAuthenticationContext authenticationContext, final ModuleFactory moduleFactory)
    {
        super(authenticationContext, moduleFactory);
    }

    @Override
    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);

        List elements = element.elements("valid-customfield-type");
        final String defaultPackageName = plugin.getKey();
        for (Iterator iterator = elements.iterator(); iterator.hasNext();)
        {
            Element customFieldTypeElement = (Element) iterator.next();
            String packageName = customFieldTypeElement.attributeValue("package");
            if (packageName == null)
                packageName = defaultPackageName;

            validCustomFieldKeys.add(packageName + ":" + customFieldTypeElement.attributeValue("key"));
        }

    }

    @Override
    public void enabled()
    {
        super.enabled();
        assertModuleClassImplements(CustomFieldSearcher.class);
    }

    @Override
    public String getSearchHtml(CustomField customField,
                                CustomFieldValueProvider provider,
                                SearchContext searchContext,
                                FieldValuesHolder fieldValuesHolder,
                                Map displayParameters,
                                Action action,
                                Map velocityParams)
    {
        Map params = prepareSearchParams(customField, searchContext, fieldValuesHolder, action, displayParameters, provider);
        if (velocityParams != null)
        {
            params.putAll(velocityParams);
        }
        return getHtml("search", params);
    }

    private Map prepareSearchParams(CustomField customField, SearchContext searchContext, FieldValuesHolder fieldValuesHolder, Action action, Map displayParameters, CustomFieldValueProvider provider)
    {
        FieldConfig config = customField.getReleventConfig(searchContext);

        MutableIssue issue = null;
        if (searchContext.isSingleProjectContext())
        {
            issue = IssueImpl.getIssueObject(null);
            issue.setProject(searchContext.getOnlyProject());
        }

        Map params = CustomFieldUtils.buildParams(customField, config, issue, null, null, fieldValuesHolder, action, displayParameters);
        params.put("searchContext", searchContext);
        params.put("value", provider.getStringValue(customField, fieldValuesHolder));
        return params;
    }

    @Override
    public String getViewHtml(CustomField customField,
                                CustomFieldValueProvider provider,
                                SearchContext searchContext,
                                FieldValuesHolder fieldValuesHolder,
                                Map displayParameters,
                                Action action,
                                Map velocityParams)
    {
        Map params = prepareSearchParams(customField, searchContext, fieldValuesHolder, action, displayParameters, provider);
        params.put("valueObject", provider.getValue(customField, fieldValuesHolder));
        params.putAll(velocityParams);
        return getHtml("view", params);
    }

    @Override
    public String getViewHtml(CustomField field, Object value)
    {
        return getHtml("view", EasyMap.build("customField", field, "value", value));
    }

    @Override
    public String getStatHtml(CustomField field, Object value, String urlPrefix)
    {
        return getHtml("label", EasyMap.build("customField", field, "value", value, "urlPrefix", urlPrefix));
    }

    @Override
    public Set getValidCustomFieldKeys()
    {
        return validCustomFieldKeys;
    }

}
