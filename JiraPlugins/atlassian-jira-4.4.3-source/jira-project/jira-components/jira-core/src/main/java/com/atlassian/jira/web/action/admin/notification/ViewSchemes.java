/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */

package com.atlassian.jira.web.action.admin.notification;

import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.scheme.AbstractViewSchemes;
import com.atlassian.jira.scheme.SchemeManager;
import com.atlassian.sal.api.websudo.WebSudoRequired;

@WebSudoRequired
public class ViewSchemes extends AbstractViewSchemes
{
    public SchemeManager getSchemeManager()
    {
        return ManagerFactory.getNotificationSchemeManager();
    }

    public String getRedirectURL()
    {
        return null;
    }
}
