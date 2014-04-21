package com.atlassian.jira.issue.vote;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.util.NotNull;

/**
 * Get all issue ids someone voted for.
 *
 * @since v4.1
 */
public interface VotedIssuesAccessor
{
    enum Security
    {
        /**
         * Only return issue ids the user can see.
         */
        RESPECT,

        /**
         * return all issues the user
         */
        OVERRIDE
    }

    boolean isVotingEnabled();

    /**
     * Get the issues a particular user has voted for.
     *
     * @param voter the user whose votes we are searching for.
     * @param searcher the user who is searching for the voted issues.
     * @param security whether to respect or override security.
     * @return the ids of the found issues.
     */
    @NotNull
    Iterable<Long> getVotedIssueIds(@NotNull User voter, @NotNull User searcher, @NotNull Security security);
}
