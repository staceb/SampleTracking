package com.atlassian.jira.util;

import org.apache.commons.lang.exception.NestableException;

/**
 * Thrown when a timeout has been reached while trying to obtain a lock
 */
public class LockException extends NestableException
{
    public LockException(String string)
    {
        super(string);
    }

    public LockException(String string, Throwable throwable)
    {
        super(string, throwable);
    }
}
