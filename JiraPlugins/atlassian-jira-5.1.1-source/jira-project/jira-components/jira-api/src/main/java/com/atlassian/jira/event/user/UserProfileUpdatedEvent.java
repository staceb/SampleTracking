package com.atlassian.jira.event.user;

import com.atlassian.crowd.embedded.api.User;

/**
 * Event indicating that a user's profile has been updated.
 *
 * @since v5.0
 */
public class UserProfileUpdatedEvent
{
    private String username;
    private String editedByUsername;

    public UserProfileUpdatedEvent(User user, User editedBy)
    {
        if (user != null)
        {
            this.username = user.getName();
        }

        if (editedBy != null)
        {
            this.editedByUsername = editedBy.getName();
        }
    }

    public String getUsername()
    {
        return username;
    }

    public String getEditedByUsername()
    {
        return editedByUsername;
    }
}
