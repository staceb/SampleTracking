package com.atlassian.jira.issue.customfields.manager;

import com.atlassian.jira.local.MockControllerTestCase;
import org.junit.Test;
import org.junit.Before;
import static org.junit.Assert.*;
import com.atlassian.jira.action.issue.customfields.option.MockOption;
import com.atlassian.jira.issue.customfields.option.Option;
import com.atlassian.jira.issue.fields.config.FieldConfigImpl;
import com.atlassian.jira.issue.fields.config.manager.FieldConfigManager;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.jira.util.CollectionReorderer;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.atlassian.jira.util.collect.MapBuilder;
import org.easymock.EasyMock;
import org.ofbiz.core.entity.GenericValue;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @since v4.0
 */
public class TestDefaultOptionsManager extends MockControllerTestCase
{
    private OfBizDelegator ofBizDelegator;
    private CollectionReorderer<Option> collectionReorderer;
    private FieldConfigManager fieldConfigManager;

    @Before
    public void setUp() throws Exception
    {
        ofBizDelegator = mockController.getMock(OfBizDelegator.class);
        collectionReorderer = mockController.getMock(CollectionReorderer.class);
        fieldConfigManager = mockController.getMock(FieldConfigManager.class);
    }

    @Test
    public void testGetAllOptions() throws Exception
    {
        final Option option = new MockOption(null, null, null, null, null, 10L);
        ofBizDelegator.findAll(EasyMock.eq("CustomFieldOption"), (List<String>)EasyMock.anyObject());
        mockController.setReturnValue(Collections.emptyList());
        mockController.replay();

        final AtomicBoolean called = new AtomicBoolean(false);
        final DefaultOptionsManager optionsManager = new DefaultOptionsManager(ofBizDelegator, collectionReorderer, fieldConfigManager)
        {
            @Override
            List<Option> convertGVsToOptions(final List<GenericValue> optionGvs)
            {
                called.set(true);
                return CollectionBuilder.newBuilder(option).asList();
            }
        };

        final List<Option> result = optionsManager.getAllOptions();
        assertTrue(called.get());
        assertTrue(result.contains(option));
        assertEquals(1, result.size());
        mockController.verify();
    }

    @Test
    public void testFindByOptionValue() throws Exception
    {
        final Option option1 = new MockOption(null, null, null, "value", null, 10L);
        mockController.replay();
        final DefaultOptionsManager manager = new DefaultOptionsManager(ofBizDelegator, collectionReorderer, fieldConfigManager)
        {
            @Override
            public List<Option> getAllOptions()
            {
                return CollectionBuilder.<Option>newBuilder(option1, new MockOption(null, null, null, "DiffValue", null, 10L)).asList();
            }
        };

        final List<Option> result = manager.findByOptionValue("VALUE");
        assertEquals(1, result.size());
        assertTrue(result.contains(option1));
        mockController.verify();
    }

    @Test
    public void testRemoveCustomFieldConfigOptions() throws Exception
    {
        ofBizDelegator.removeByAnd("CustomFieldOption", MapBuilder.newBuilder("customfieldconfig", 1L).toMap());
        mockController.setReturnValue(1L);

        mockController.replay();

        final DefaultOptionsManager manager = new DefaultOptionsManager(ofBizDelegator, collectionReorderer, fieldConfigManager);
        final FieldConfigImpl fieldConfig = new FieldConfigImpl(1L, "", "", null, "");

        manager.removeCustomFieldConfigOptions(fieldConfig);

        mockController.verify();
    }
}
