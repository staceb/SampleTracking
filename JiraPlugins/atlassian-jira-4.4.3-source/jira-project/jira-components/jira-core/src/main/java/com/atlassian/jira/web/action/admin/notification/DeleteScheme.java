/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */

package com.atlassian.jira.web.action.admin.notification;

import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.scheme.AbstractDeleteScheme;
import com.atlassian.jira.scheme.SchemeManager;
import com.atlassian.sal.api.websudo.WebSudoRequired;

@WebSudoRequired
public class DeleteScheme extends AbstractDeleteScheme
{
    public SchemeManager getSchemeManager()
    {
        return ManagerFactory.getNotificationSchemeManager();
    }

    public String getRedirectURL()
    {
        return "ViewNotificationSchemes.jspa";
    }
}
