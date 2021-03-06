package com.atlassian.jira.issue.issuelink;

import com.atlassian.annotations.PublicApi;
import com.atlassian.jira.issue.Issue;

/**
 * Represents a link between two issues
 *
 * @since v4.4
 * @deprecated Use {@link com.atlassian.jira.issue.link.IssueLink} instead. Since v5.0.
 */
@PublicApi
public interface IssueLink
{
    /**
     * @return the source issue that is the issue that the link goes from
     */
    Issue getSourceIssue();

    /**
     * @return the destination issue, that is the issue that the link goes to
     */
    Issue getDestinationIssue();

    /**
     * @return the type of link
     */
    IssueLinkType getIssueLinkType();
}
