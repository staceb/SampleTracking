package com.atlassian.jira.issue.customfields.impl;

import com.atlassian.jira.datetime.DateTimeFormatterFactoryStub;
import com.atlassian.jira.imports.project.customfield.NoTransformationCustomFieldImporter;
import com.atlassian.jira.local.ListeningTestCase;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @since v3.13
 */
public class TestDateTimeCFType extends ListeningTestCase
{
    @Test
    public void testGetProjectImporter() throws Exception
    {
        DateTimeCFType dateTimeCFType = new DateTimeCFType(null, null, new DateTimeFormatterFactoryStub(), null, null, null);
        assertTrue(dateTimeCFType.getProjectImporter() instanceof NoTransformationCustomFieldImporter);
    }
}
