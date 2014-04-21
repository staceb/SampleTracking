/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */

package com.atlassian.jira.web.action.admin.notification;

import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.notification.NotificationType;
import com.atlassian.jira.scheme.AbstractSchemeAwareAction;
import com.atlassian.jira.scheme.SchemeManager;

public class SchemeAwareNotificationAction extends AbstractSchemeAwareAction
{
    public NotificationType getType(String id)
    {
        return ManagerFactory.getNotificationTypeManager().getNotificationType(id);
    }

    public SchemeManager getSchemeManager()
    {
        return ManagerFactory.getNotificationSchemeManager();
    }

    public String getRedirectURL()
    {
        return null;
    }
}
