package com.atlassian.jira.web.action.issue;

import com.atlassian.jira.issue.attachment.TemporaryAttachment;
import com.atlassian.jira.util.Predicate;
import com.atlassian.jira.util.collect.CollectionUtil;

import javax.servlet.http.HttpSessionBindingEvent;
import javax.servlet.http.HttpSessionBindingListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This class maintains a list of temporary attachments uploaded by a user.  When the user's session expires or the user
 * logs out, it is responsible of deleting any temporary files that were not converted to proper attachments.
 *
 * @since v4.2
 */
public class TemporaryAttachmentsMonitor implements HttpSessionBindingListener
{
    private final Map<Long, TemporaryAttachment> temporaryAttachments = new HashMap<Long, TemporaryAttachment>();

    /**
     * Adds temporary attachments to the interally maintained list of temporary attachements
     *
     * @param temporaryAttachment the attachment to add
     */
    public void add(final TemporaryAttachment temporaryAttachment)
    {
        temporaryAttachments.put(temporaryAttachment.getId(), temporaryAttachment);
    }

    /**
     * Returns matching temporary attachment by attachment id.
     *
     * @param id the id of the temporary attachment
     * @return The temporary attachment or null if no matching attachment was found
     */
    public TemporaryAttachment getById(final Long id)
    {
        return temporaryAttachments.get(id);
    }

    /**
     * Returns all currently matchign temporary attachments for a particular issue. If a null issue id is
     * provided, this should be interpreted as a newly created issue that doesn't have an id yet.
     *
     * @param issueId The id of the issue to get attachmetns for. May be null
     * @return a collection of temporary attachments for this issue sorted by creation date
     */
    public Collection<TemporaryAttachment> getByIssueId(final Long issueId)
    {
        final ArrayList<TemporaryAttachment> ret = new ArrayList<TemporaryAttachment>(CollectionUtil.filter(temporaryAttachments.values(), new Predicate<TemporaryAttachment>()
        {
            public boolean evaluate(final TemporaryAttachment input)
            {
                return issueId == null && input.getIssueId() == null || (issueId != null && issueId.equals(input.getIssueId()));
            }
        }));
        Collections.sort(ret);
        return ret;
    }

    /**
     * Removes all temporary attachments for the given issue.  The issueId may be null to indicate a
     * newly created issue, that doesn't have an id yet.
     *
     * @param issueId The id of the issue to remove entries for. May be null.
     */
    public void clearEntriesForIssue(final Long issueId)
    {
        final Collection<TemporaryAttachment> attachmentsForIssueId = getByIssueId(issueId);
        for (TemporaryAttachment temporaryAttachment : attachmentsForIssueId)
        {
            temporaryAttachment.getFile().delete();
            temporaryAttachments.remove(temporaryAttachment.getId());
        }
    }

    public void valueBound(final HttpSessionBindingEvent httpSessionBindingEvent)
    {
    }

    /**
     * When this object is unbount from the HttpSession, it's responsible for cleanup.  Any
     * temporary attachments not converted to real attachments by now should be deleted to save
     * disk space!
     *
     * @param httpSessionBindingEvent
     */
    public void valueUnbound(final HttpSessionBindingEvent httpSessionBindingEvent)
    {
        //delete any temporary attachments left over.
        for (TemporaryAttachment temporaryAttachment : temporaryAttachments.values())
        {
            temporaryAttachment.getFile().delete();
        }
        temporaryAttachments.clear();
    }
}
