package com.atlassian.jira.util.index;

import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.task.context.Context;

import java.util.Collection;
import java.util.Collections;

public class MockIndexLifecycleManager implements IndexLifecycleManager
{

    public long activate(final Context ctx)
    {
        return 0;
    }

    public void deactivate()
    {}

    public Collection<String> getAllIndexPaths()
    {
        return Collections.emptyList();
    }

    public boolean isIndexingEnabled()
    {
        return true;
    }

    public long optimize() throws IndexException
    {
        return 0;
    }

    public long reIndexAll(final Context ctx) throws IndexException
    {
        return 0;
    }

    public int size()
    {
        return 0;
    }

    public boolean isEmpty()
    {
        return true;
    }

    public void shutdown()
    {}
}
