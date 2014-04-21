/*
 * Copyright (c) 2002-2006
 * All rights reserved.
 */

package com.atlassian.jira.notification;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.crowd.embedded.core.util.StaticCrowdServiceFactory;
import com.atlassian.jira.user.preferences.JiraUserPreferences;
import com.atlassian.jira.user.preferences.PreferenceKeys;
import com.atlassian.jira.user.util.OSUserConverter;
import com.opensymphony.util.TextUtils;

import static com.google.common.base.Preconditions.checkNotNull;

public class NotificationRecipient
{
    public static final String MIMETYPE_HTML = "html";
    public static final String MIMETYPE_HTML_DISPLAY = "HTML";
    public static final String MIMETYPE_TEXT = "text";
    public static final String MIMETYPE_TEXT_DISPLAY = "Text";

    private final User user;
    private final String email;
    private final String format;

    /**
     * The format is set to html or text as specified in jira-application.properties file.
     * If this setting is not configured correctly, default to text format.
     *
     * @param user recipient user
     */
    public NotificationRecipient(User user)
    {
        this.user = checkNotNull(user);
        this.email = user.getEmailAddress();

        JiraUserPreferences userPrefs = new JiraUserPreferences(user);
        String prefFormat = userPrefs.getString(PreferenceKeys.USER_NOTIFICATIONS_MIMETYPE);

        // Default to text if the property is not configured.
        if (TextUtils.stringSet(prefFormat) && (prefFormat.equals(MIMETYPE_HTML) || prefFormat.equals(MIMETYPE_TEXT)))
        {
            format = prefFormat;
        }
        else
        {
            format = MIMETYPE_TEXT;
        }
    }

    public NotificationRecipient(String pEmail)
    {
        user = null;
        email = pEmail;
        format = MIMETYPE_TEXT;
    }

    public String getEmail()
    {
        return email;
    }

    /**
     * Returns a user if this NotificationRecipient was constructed with a user.
     * Returns null if this NotificationRecipient was constructed with an e-mail address only.
     *
     * @return recipient user, can return null
     * @deprecated use {@link #getUserRecipient()} instead
     */
    public com.opensymphony.user.User getUser()
    {
        return OSUserConverter.convertToOSUser(user);
    }

    /**
     * Returns a user if this NotificationRecipient was constructed with a user.
     * Returns null if this NotificationRecipient was constructed with an e-mail address only
     *
     * @return user recipient of this notification, or <code>null</code>, if recipient is an email address.
     */
    public User getUserRecipient()
    {
        return user;
    }

    public boolean isHtml()
    {
        return "html".equals(format);
    }

    public String getFormat()
    {
        return format;
    }

    /**
     * Checks if the recipient is in the specified group. If this is only an email address they are not in any group.
     *
     * @param groupName group name
     * @return <code>true</code> if the user is set and is in the group, <code>false</code> otherwise
     */
    public boolean isInGroup(String groupName)
    {
        return user != null && StaticCrowdServiceFactory.getCrowdService().isUserMemberOfGroup(user.getName(), groupName);
    }

    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (!(o instanceof NotificationRecipient))
        {
            return false;
        }

        final NotificationRecipient notificationRecipient = (NotificationRecipient) o;

        if (email != null ? !email.equals(notificationRecipient.email) : notificationRecipient.email != null)
        {
            return false;
        }
        if (user != null ? !user.equals(notificationRecipient.user) : notificationRecipient.user != null)
        {
            return false;
        }

        return true;
    }

    public int hashCode()
    {
        int result;
        result = (user != null ? user.hashCode() : 0);
        result = 29 * result + (email != null ? email.hashCode() : 0);
        return result;
    }
}
