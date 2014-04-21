package com.atlassian.jira.issue.customfields.impl;

import com.atlassian.jira.local.ListeningTestCase;
import org.junit.Test;
import static org.junit.Assert.*;
import com.atlassian.jira.imports.project.customfield.NoTransformationCustomFieldImporter;

/**
 * @since v3.13
 */
public class TestTextAreaCFType extends ListeningTestCase
{
    @Test
    public void testGetProjectImporter() throws Exception
    {
        TextAreaCFType textAreaCFType = new TextAreaCFType(null, null, null);
        assertTrue(textAreaCFType.getProjectImporter() instanceof NoTransformationCustomFieldImporter);
    }

}
