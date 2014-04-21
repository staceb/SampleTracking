package com.atlassian.jira.issue.index.indexers.impl;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.index.indexers.FieldIndexer;
import com.atlassian.jira.web.FieldVisibilityManager;
import org.apache.lucene.document.Document;

/**
 * A base-class for custom field indexers that performs the logic of checking if the CustomField isVisibleAndInScope
 * and then delegates off to the base class to add the correct document fields.
 *
 * @since v4.0
 */
public abstract class AbstractCustomFieldIndexer implements FieldIndexer
{
    private final FieldVisibilityManager fieldVisibilityManager;
    protected final CustomField customField;

    protected AbstractCustomFieldIndexer(final FieldVisibilityManager fieldVisibilityManager, final CustomField customField)
    {
        this.fieldVisibilityManager = fieldVisibilityManager;
        this.customField = customField;
    }

    public String getId()
    {
        return customField.getId();
    }

    public String getDocumentFieldId()
    {
        return customField.getId();
    }

    public abstract void addDocumentFieldsSearchable(final Document doc, final Issue issue);

    public abstract void addDocumentFieldsNotSearchable(final Document doc, final Issue issue);

    public final void addIndex(final Document doc, final Issue issue)
    {
        if (isFieldVisibleAndInScope(issue))
        {
            addDocumentFieldsSearchable(doc, issue);
        }
        else
        {
            addDocumentFieldsNotSearchable(doc, issue);
        }
    }

    public boolean isFieldVisibleAndInScope(final Issue issue)
    {
        return isRelevantForIssue(issue) && !fieldVisibilityManager.isFieldHidden(getId(), issue);
    }

    protected boolean isRelevantForIssue(final Issue issue)
    {
        return customField.getRelevantConfig(issue) != null;
    }

}
