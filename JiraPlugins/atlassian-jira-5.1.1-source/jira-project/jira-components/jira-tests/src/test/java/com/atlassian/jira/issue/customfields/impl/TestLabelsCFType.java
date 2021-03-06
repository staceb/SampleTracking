package com.atlassian.jira.issue.customfields.impl;

import com.atlassian.jira.issue.customfields.ProjectImportLabelFieldParser;
import com.atlassian.jira.issue.customfields.view.CustomFieldParams;
import com.atlassian.jira.issue.customfields.view.CustomFieldParamsImpl;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.atlassian.jira.issue.fields.rest.json.beans.JiraBaseUrls;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.local.ListeningTestCase;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.auth.trustedapps.MockI18nHelper;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.util.collect.CollectionBuilder;
import org.junit.Test;

import java.util.Collections;
import java.util.Set;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TestLabelsCFType extends ListeningTestCase
{
    @Test
    public void testValidate()
    {
        final ProjectImportLabelFieldParser projectImportLabelFieldParser = new  ProjectImportLabelFieldParserImpl();
        final JiraAuthenticationContext context = createMock(JiraAuthenticationContext.class);
        final JiraBaseUrls jiraBaseUrls = createMock(JiraBaseUrls.class);
        expect(context.getI18nHelper()).andReturn(new MockI18nHelper()).anyTimes();
        final FieldConfig mockFieldConfig = createMock(FieldConfig.class);
        final CustomField mockCustomField = createMock(CustomField.class);
        expect(mockCustomField.getId()).andReturn("customfield_10000").anyTimes();
        expect(mockFieldConfig.getCustomField()).andReturn(mockCustomField).anyTimes();

        replay(context, mockFieldConfig, mockCustomField);
        LabelsCFType labelsCFType = new LabelsCFType(context, null, null, null, null, projectImportLabelFieldParser, jiraBaseUrls);
        CustomFieldParams params = new CustomFieldParamsImpl();
        final ErrorCollection errors = new SimpleErrorCollection();

        labelsCFType.validateFromParams(params, errors, null);
        assertFalse(errors.hasAnyErrors());
        
        //try empty
        params.put(null, Collections.<String>emptyList());
        labelsCFType.validateFromParams(params, errors, null);
        assertFalse(errors.hasAnyErrors());

        //now let's provide some valid labels
        params.put(null, CollectionBuilder.list("blah", "dude", "awesome"));
        labelsCFType.validateFromParams(params, errors, null);
        assertFalse(errors.hasAnyErrors());

        params.put(null, CollectionBuilder.list("label", "reallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelr"));
        labelsCFType.validateFromParams(params, errors, mockFieldConfig);
        assertTrue(errors.hasAnyErrors());
        assertEquals("label.service.error.label.toolong reallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelreallylonglabelr", errors.getErrors().get("customfield_10000"));


        verify(context, mockFieldConfig, mockCustomField);
    }

    @Test
    public void testEmptySetAndNullValuesEqual()
    {
        Set<Label> emptySet = Collections.emptySet();
        Set<Label> nullSet = null;
        assertTrue(newEmptyLabelsCFType().valuesEqual(emptySet, nullSet));
        assertTrue(newEmptyLabelsCFType().valuesEqual(nullSet, emptySet));
        assertTrue(newEmptyLabelsCFType().valuesEqual(nullSet, nullSet));
        assertTrue(newEmptyLabelsCFType().valuesEqual(emptySet, emptySet));
    }

    private LabelsCFType newEmptyLabelsCFType()
    {
        final ProjectImportLabelFieldParser projectImportLabelFieldParser = new  ProjectImportLabelFieldParserImpl();
        final JiraBaseUrls jiraBaseUrls = createMock(JiraBaseUrls.class);
        return new LabelsCFType(null, null, null, null, null, projectImportLabelFieldParser, jiraBaseUrls);
    }
}
