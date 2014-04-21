/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */
package com.atlassian.jira.web.action.admin;

import com.atlassian.jira.issue.index.IndexException;
import com.atlassian.jira.util.index.IndexLifecycleManager;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.websudo.WebSudoRequired;

@WebSudoRequired
public class IndexOptimize extends JiraWebActionSupport
{
    private long optimizeTime;

    private final IndexLifecycleManager indexLifecycleManager;

    public IndexOptimize(final IndexLifecycleManager indexLifecycleManager)
    {
        if (indexLifecycleManager == null)
        {
            throw new NullPointerException(getClass().getName() + " needs a non null instance of: " + IndexLifecycleManager.class.getName());
        }

        this.indexLifecycleManager = indexLifecycleManager;
    }

    public String doDefault() throws Exception
    {
        return super.doDefault();
    }

    protected void doValidation()
    {
        if (!isIndexing())
        {
            addErrorMessage(getText("admin.indexing.optimize.index.disabled"));
        }
        super.doValidation();
    }

    // Protected -----------------------------------------------------
    protected String doExecute()
    {
        long optimizeTime;
        try
        {
            optimizeTime = indexLifecycleManager.optimize();
        }
        catch (final IndexException e)
        {
            log.error("Error occurred while optimizing the index.", e);
            addErrorMessage(getText("admin.indexing.optimizing.error", e));
            return getResult();
        }
        if (optimizeTime < 0)
        {
            addErrorMessage(getText("admin.indexing.optimize.index.nolock"));
            return getResult();
        }
        setOptimizeTime(optimizeTime);
        return getRedirect("IndexOptimize!default.jspa?optimizeTime=" + getOptimizeTime());
    }

    public boolean isIndexing()
    {
        return indexLifecycleManager.isIndexingEnabled();
    }

    public long getOptimizeTime()
    {
        return optimizeTime;
    }

    public void setOptimizeTime(final long optimizeTime)
    {
        this.optimizeTime = optimizeTime;
    }
}