package com.atlassian.jira.event.issue.link;

/**
 * Fired when remote issue link has been updated.
 *
 * @since v5.0
 */
public class RemoteIssueLinkUpdateEvent extends AbstractRemoteIssueLinkEvent
{
    private final String applicationType;

    public RemoteIssueLinkUpdateEvent(Long remoteLinkId, String applicationType)
    {
        super(remoteLinkId);
        this.applicationType = applicationType;
    }

    public String getApplicationType()
    {
        return applicationType;
    }
}
