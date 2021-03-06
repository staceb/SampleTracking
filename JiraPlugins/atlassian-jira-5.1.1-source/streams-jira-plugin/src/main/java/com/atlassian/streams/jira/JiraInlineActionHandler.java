package com.atlassian.streams.jira;

/**
 * Handles JIRA-specific inline action tasks.
 */
public interface JiraInlineActionHandler
{
    /**
     * Registers the current user as a watcher on the specified issue.
     *  
     * @param issueKey the issue to watch
     * @return true if the watcher was added, false if not
     */
    boolean startWatching(String issueKey);

    /**
     * Determines if the user has previously watched the issue
     *
     * @param issueKey the issue to watch
     * @return true if the watcher is already watching the issue, false it not
     */
    boolean hasPreviouslyWatched(String issueKey);

    /**
     * Registers the current user as a vote on the specified issue.
     *  
     * @param issueKey the issue to vote on
     * @return true if vote was successful, false if not
     */
    boolean voteOnIssue(String issueKey);

    /**
     * Determines if the user has previously voted on the issue
     *
     * @param issueKey the issue to vote on
     * @return true if user has previously voted on the issue, false if not
     */
    boolean hasPreviouslyVoted(String issueKey);
}
