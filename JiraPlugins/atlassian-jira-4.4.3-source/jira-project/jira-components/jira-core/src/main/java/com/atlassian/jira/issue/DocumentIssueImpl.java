package com.atlassian.jira.issue;

import com.atlassian.core.ofbiz.CoreFactory;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.project.component.ComponentConverter;
import com.atlassian.jira.bc.project.component.MutableProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.datetime.LocalDate;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.FieldManager;
import com.atlassian.jira.issue.fields.renderer.IssueRenderContext;
import com.atlassian.jira.issue.index.DocumentConstants;
import com.atlassian.jira.issue.label.Label;
import com.atlassian.jira.issue.label.LabelComparator;
import com.atlassian.jira.issue.search.LuceneFieldSorter;
import com.atlassian.jira.jql.util.JqlLocalDateSupport;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectFactory;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.util.ofbiz.ImmutableGenericValue;
import org.apache.lucene.document.Document;
import org.ofbiz.core.entity.GenericValue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DocumentIssueImpl extends AbstractIssue
{
    private final Document document;
    private final FieldManager fieldManager;
    private IssueFactory issueFactory;
    private ProjectFactory projectFactory;
    private final JqlLocalDateSupport jqlLocalDateSupport;
    private Map customFieldValues = new HashMap();

    public DocumentIssueImpl(Document document, ConstantsManager constantsManager, FieldManager fieldManager, IssueManager issueManager, IssueFactory issueFactory, AttachmentManager attachmentManager, ProjectFactory projectFactory, JqlLocalDateSupport jqlLocalDateSupport)
    {
        super(constantsManager, issueManager, attachmentManager);
        this.document = document;
        this.fieldManager = fieldManager;
        this.issueFactory = issueFactory;
        this.projectFactory = projectFactory;
        this.jqlLocalDateSupport = jqlLocalDateSupport;
    }

    public Long getId()
    {
        return Long.valueOf(document.get(DocumentConstants.ISSUE_ID));
    }

    public GenericValue getProject()
    {
        return (GenericValue) getSingleValueFromField(IssueFieldConstants.PROJECT);
    }

    public Project getProjectObject()
    {
        Project project = null;
        GenericValue projectGv = getProject();
        if (projectGv != null)
        {
            project = projectFactory.getProject(projectGv);
        }
        return project;
    }

    private Object getSingleValueFromField(String fieldName)
    {
        LuceneFieldSorter sorter = fieldManager.getNavigableField(fieldName).getSorter();
        return sorter.getValueFromLuceneField(document.get(sorter.getDocumentConstant()));
    }

    private List getMultipleValuesFromField(String fieldName)
    {
        LuceneFieldSorter sorter = fieldManager.getNavigableField(fieldName).getSorter();
        String[] documentValues = document.getValues(sorter.getDocumentConstant());
        if (documentValues == null || documentValues.length == 0)
        {
            return Collections.EMPTY_LIST;
        }

        List values = new ArrayList();
        for (int i = 0; i < documentValues.length; i++)
        {
            String documentValue = documentValues[i];
            Object value = sorter.getValueFromLuceneField(documentValue);
            if (value != null)
            {
                values.add(value);
            }
        }
        Collections.sort(values, sorter.getComparator());
        return values;
    }

    private List getIssuesFromField(String fieldName)
    {
        LuceneFieldSorter sorter = fieldManager.getNavigableField(fieldName).getSorter();
        String[] documentValues = document.getValues(sorter.getDocumentConstant());
        if (documentValues == null || documentValues.length == 0)
        {
            return Collections.EMPTY_LIST;
        }

        List values = new ArrayList();
        for (int i = 0; i < documentValues.length; i++)
        {
            String documentValue = documentValues[i];
            Object value = sorter.getValueFromLuceneField(documentValue);
            if (value != null)
            {
                values.add(value);
            }
        }
        Collections.sort(values, sorter.getComparator());
        return convertGenericValuesToIssues(values);
    }

    public GenericValue getIssueType()
    {
        return (GenericValue) getSingleValueFromField(IssueFieldConstants.ISSUE_TYPE);
    }

    public String getSummary()
    {
        // As the field uses a different value for sorting than storing - we need to hard code this here
        // return (String) getSingleValueFromField(IssueFieldConstants.SUMMARY);
        return document.get(DocumentConstants.ISSUE_SUMMARY);
    }

    public User getAssigneeUser()
    {
        return (User) getSingleValueFromField(IssueFieldConstants.ASSIGNEE);
    }

    public com.opensymphony.user.User getAssignee()
    {
        return (com.opensymphony.user.User) getSingleValueFromField(IssueFieldConstants.ASSIGNEE);
    }

    public String getAssigneeId()
    {
        //
        // JRA-15578 / JRA-15624 - The index code uses a very badly named placeholder issue
        // called "unassigned" to indicate issues that are Unassigned.  But we want this
        // method to act like the getAssignee() method where an assigned issue
        // returns null.

        final String assigneeUserId = document.get(DocumentConstants.ISSUE_ASSIGNEE);
        if (DocumentConstants.ISSUE_UNASSIGNED.equals(assigneeUserId))
        {
            return null;
        }
        return assigneeUserId;
    }

    public Collection<GenericValue> getComponents()
    {
        return getMultipleValuesFromField(IssueFieldConstants.COMPONENTS);
    }

    public Collection<ProjectComponent> getComponentObjects()
    {
        Collection<GenericValue> gvs = getComponents();
        final ComponentConverter converter = new ComponentConverter();
        final Collection<MutableProjectComponent> mutables = converter.convertToComponents(gvs);
        return converter.convertToProjectComponents(mutables);
    }

    public Set<Label> getLabels()
    {
        @SuppressWarnings("unchecked")
        final List<String> fields = getMultipleValuesFromField(IssueFieldConstants.LABELS);
        final List<Label> labels = new ArrayList<Label>();
        for (String field : fields)
        {
            labels.add(new Label(null, getId(), field));
        }
        Collections.sort(labels, LabelComparator.INSTANCE);
        return Collections.unmodifiableSet(new LinkedHashSet<Label>(labels));
    }

    public User getReporterUser()
    {
        return (User) getSingleValueFromField(IssueFieldConstants.REPORTER);
    }

    public com.opensymphony.user.User getReporter()
    {
        return (com.opensymphony.user.User) getSingleValueFromField(IssueFieldConstants.REPORTER);
    }

    public String getReporterId()
    {
        //
        // Also related to JRA-15578 / JRA-15624
        // Want to act the same as getReporter() and return null if there is in fact no reporter
        final String reporterUserId = document.get(DocumentConstants.ISSUE_AUTHOR);
        if (DocumentConstants.ISSUE_NO_AUTHOR.equals(reporterUserId))
        {
            return null;
        }
        return reporterUserId;
    }

    public String getDescription()
    {
        // As the field uses a different value for sorting than storing - we need to hard code this here
        // return (String) getSingleValueFromField(IssueFieldConstants.DESCRIPTION);
        return document.get(DocumentConstants.ISSUE_DESC);
    }

    public String getEnvironment()
    {
        // environment currently doesn't have a sorter associated with it
        return document.get(DocumentConstants.ISSUE_ENV);
    }

    public Collection<Version> getAffectedVersions()
    {
        return getMultipleValuesFromField(IssueFieldConstants.AFFECTED_VERSIONS);
    }

    public Collection<Version> getFixVersions()
    {
        return getMultipleValuesFromField(IssueFieldConstants.FIX_FOR_VERSIONS);
    }

    public Timestamp getDueDate()
    {
        // todo - change the method signature to be a date instead.
        LocalDate localDate = (LocalDate) getSingleValueFromField(IssueFieldConstants.DUE_DATE);
        if (localDate == null)
        {
            return null;
        }
        else
        {
            return new Timestamp(jqlLocalDateSupport.convertToDate(localDate).getTime());
        }
    }

    private Timestamp getDateField(String fieldName)
    {
        Date date = (Date) getSingleValueFromField(fieldName);
        if (date == null)
        {
            return null;
        }
        else
        {
            return new Timestamp(date.getTime());
        }
    }

    public GenericValue getSecurityLevel()
    {
        return (GenericValue) getSingleValueFromField(IssueFieldConstants.SECURITY);
    }

    public Long getSecurityLevelId()
    {
        String securityLevelId = document.get(DocumentConstants.ISSUE_SECURITY_LEVEL);
        //JRA-13744: Need to handle -1 case to ensure that permission checks work with a documentissue impl.
        return securityLevelId != null && !"-1".equals(securityLevelId) ? Long.valueOf(securityLevelId) : null;
    }

    public GenericValue getPriority()
    {
        return (GenericValue) getSingleValueFromField(IssueFieldConstants.PRIORITY);
    }

    public GenericValue getResolution()
    {
        return (GenericValue) getSingleValueFromField(IssueFieldConstants.RESOLUTION);
    }

    public String getKey()
    {
        return (String) getSingleValueFromField(IssueFieldConstants.ISSUE_KEY);
    }

    public Long getVotes()
    {
        return (Long) getSingleValueFromField(IssueFieldConstants.VOTES);
    }

    public Long getWatches()
    {
        return (Long) getSingleValueFromField(IssueFieldConstants.WATCHES);
    }

    public Timestamp getCreated()
    {
        // todo - change the method signature to be a date instead.
        return getDateField(IssueFieldConstants.CREATED);
    }

    public Timestamp getResolutionDate()
    {
        return getDateField(IssueFieldConstants.RESOLUTION_DATE);
    }

    public Timestamp getUpdated()
    {
        // todo - change the method signature to be a date instead.
        return getDateField(IssueFieldConstants.UPDATED);
    }

    public Long getWorkflowId()
    {
        throw new UnsupportedOperationException("We don't currently index workflowid - can't get it from document");
    }

    public Object getCustomFieldValue(CustomField customField)
    {
        if (!customFieldValues.containsKey(customField))
        {
            customFieldValues.put(customField, customField.getValue(this));
        }

        return customFieldValues.get(customField);
    }

    public GenericValue getStatus()
    {
        return (GenericValue) getSingleValueFromField(IssueFieldConstants.STATUS);
    }

    public Long getOriginalEstimate()
    {
        return (Long) getSingleValueFromField(IssueFieldConstants.TIME_ORIGINAL_ESTIMATE);
    }

    public Long getEstimate()
    {
        return (Long) getSingleValueFromField(IssueFieldConstants.TIME_ESTIMATE);
    }

    public Long getTimeSpent()
    {
        return (Long) getSingleValueFromField(IssueFieldConstants.TIME_SPENT);
    }

    public Object getExternalFieldValue(String fieldId)
    {
        // todo - implement this!
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public boolean isSubTask()
    {
        return getParentId() != null;
    }

    public Long getParentId()
    {
        String parentTaskId = document.get(DocumentConstants.ISSUE_PARENTTASK);
        return parentTaskId != null ? Long.decode(parentTaskId) : null;
    }

    public boolean isCreated()
    {
        return true; // if an issue is indexed - it must have been created
    }

    public Collection getSubTasks()
    {
        return getMultipleValuesFromField(IssueFieldConstants.SUBTASKS);
    }

    public Collection getSubTaskObjects()
    {
        return getIssuesFromField(IssueFieldConstants.SUBTASKS);
    }

    public IssueRenderContext getIssueRenderContext()
    {
        return new IssueRenderContext(this);
    }

    public String getString(String name)
    {
        throw new UnsupportedOperationException("This code or velocity template expects a GenericValue, but received an Issue.  We need to recode");
    }

    public Timestamp getTimestamp(String name)
    {
        throw new UnsupportedOperationException("This code or velocity template expects a GenericValue, but received an Issue.  We need to recode");
    }

    public Long getLong(String name)
    {
        throw new UnsupportedOperationException("This code or velocity template expects a GenericValue, but received an Issue.  We need to recode");
    }

    public GenericValue getGenericValue()
    {
        Map values = new HashMap();
        values.put("id", getId());
        values.put("project", getProject().getLong("id"));
        values.put("key", getKey());
        values.put("type", getIdFromGV(getIssueType()));
        values.put("status", getIdFromGV(getStatus()));
        values.put("priority", getIdFromGV(getPriority()));
        values.put("resolution", getIdFromGV(getResolution()));
        values.put("reporter", getReporterId());
        values.put("assignee", getAssigneeId());
        values.put("summary", getSummary());
        values.put("description", getDescription());
        values.put("environment", getEnvironment());
        values.put("created", getCreated());
        values.put("updated", getUpdated());
        values.put("duedate", getDueDate());
        values.put("votes", getVotes());
        values.put("timeoriginalestimate", getOriginalEstimate());
        values.put("timeestimate", getEstimate());
        values.put("timespent", getTimeSpent());
        // put ths back when we DO index workflowID
        //values.put("workflowid", getWorkflowId());
        values.put("security", getSecurityLevelId());

        // pass in a delegator that allows find by
        return new ImmutableGenericValue(CoreFactory.getGenericDelegator(), "Issue", values);
    }

    private String getIdFromGV(GenericValue targetGV)
    {
        return (targetGV != null) ? targetGV.getString("id") : null;
    }

    public void store()
    {
        throw new UnsupportedOperationException("Cannot store a DocumentIssueImpl");
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof Issue))
        {
            return false;
        }

        final Issue issue = (Issue) o;

        if (getKey() != null ? !getKey().equals(issue.getKey()) : issue.getKey() != null)
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        return (getKey() != null ? getKey().hashCode() : 0);
    }

    private List convertGenericValuesToIssues(List genericValues)
    {
        List issueObjects = new ArrayList();
        for (Iterator iterator = genericValues.iterator(); iterator.hasNext();)
        {
            GenericValue issueGV = (GenericValue) iterator.next();
            issueObjects.add(issueFactory.getIssue(issueGV));
        }
        return issueObjects;
    }
}
