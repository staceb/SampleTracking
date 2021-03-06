/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */

package com.atlassian.jira;

import com.atlassian.core.AtlassianCoreException;

///CLOVER:OFF
public class JiraException extends AtlassianCoreException
{
    public JiraException()
    {
    }

    public JiraException(String s)
    {
        super(s);
    }

    public JiraException(Throwable throwable)
    {
        super(throwable);
    }

    public JiraException(String s, Throwable throwable)
    {
        super(s, throwable);
    }
}
