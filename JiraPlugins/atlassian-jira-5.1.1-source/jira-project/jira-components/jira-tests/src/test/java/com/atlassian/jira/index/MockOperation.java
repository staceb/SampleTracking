package com.atlassian.jira.index;

import com.atlassian.jira.index.Index.Operation;
import com.atlassian.jira.index.Index.UpdateMode;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

///CLOVER:OFF
class MockOperation extends Operation
{
    final AtomicBoolean performed = new AtomicBoolean();
    final UpdateMode mode;

    MockOperation()
    {
        this(UpdateMode.INTERACTIVE);
    }

    MockOperation(final UpdateMode mode)
    {
        this.mode = mode;
    }

    @Override
    UpdateMode mode()
    {
        return mode;
    }

    @Override
    void perform(final Writer writer) throws IOException
    {
        performed.set(true);
    }

    boolean isPerformed()
    {
        return performed.get();
    }
}
///CLOVER:ON

