package com.atlassian.jira.ofbiz;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * Helper class to log the call stack of an Ofbiz SQL statement and format SQL statements.
 *
 * @since v4.4
 */
public class OfBizLogHelper
{

    /**
     * Format the SQL string with the parameters provided
     * @param sqlString Raw sql string with no parameters substituded in
     * @param parameterValues List of parameters to substitute in
     * @return SQL string with parameters in place.
     */
    public static String formatSQL(final String sqlString, final List<String> parameterValues)
    {
        StringBuilder sb = new StringBuilder("\"");
        int index = sqlString.indexOf('?');
        int startIndex = 0;
        int i = 0;
        while (index != -1 && i < parameterValues.size())
        {
            sb.append(sqlString.substring(startIndex, index))
                    .append("'")
                    .append(parameterValues.get(i))
                    .append("'");
            startIndex = index + 1;
            index = sqlString.indexOf('?', startIndex);
            i++;
        }
        // If we have extra superfluous parameters left over, we tack them on the end.  But this is not likely to happen.  Is it even possible?
        while (i < parameterValues.size())
        {
            sb.append(" ")
                    .append(parameterValues.get(i))
                    .append(",");
            i++;
        }
        sb.append("\"");
        return sb.toString();
    }

    /**
     * Returns a string with the call stack excluding filters and generic ofbiz statements.
     *
     * @return the call stack
     */
    public static String logTheCallStack()
    {
        final RuntimeException rte = new CallStack();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        rte.printStackTrace(pw);

        return shortenStackTrace(sw);
    }

    /**
     * Shortens the output stack trace by only going back to the context initialisation or to the filter chain calls.
     *
     * @param stackTraceWriter the stack trace as written
     * @return a shortened string or all of it if the call context is not what we think
     */
    private static String shortenStackTrace(StringWriter stackTraceWriter)
    {
        String stackTrace = stackTraceWriter.toString();
        int filterIndex = stackTrace.indexOf(".doFilter");
        if (filterIndex == -1)
        {
            filterIndex = stackTrace.indexOf(".contextInitialized(");
        }
        if (filterIndex != -1)
        {
            // ok move back to the start of the line
            int lineIndex = stackTrace.lastIndexOf("\n", filterIndex);
            if (lineIndex != -1)
            {
                stackTrace = stackTrace.substring(0, lineIndex) + "\n\t...";
            }
        }
        return filterStacktraceStart(stackTrace);
    }

    /**
     * At the top of the stack trace is going to be useless method calls like :
     * <p/>
     * <pre>
     *
     * <<------------- we dont want this  ------------>>
     * at com.atlassian.jira.ofbiz.LoggingSQLInterceptor.afterExecutionImpl(LoggingSQLInterceptor.java:57)
     * at com.atlassian.jira.ofbiz.LoggingSQLInterceptor.afterSuccessfulExecution(LoggingSQLInterceptor.java:33)
     * at com.atlassian.jira.ofbiz.ChainedSQLInterceptor.afterSuccessfulExecution(ChainedSQLInterceptor.java:70)
     * at org.ofbiz.core.entity.jdbc.SQLProcessor.afterExecution(SQLProcessor.java:464)
     * at org.ofbiz.core.entity.jdbc.SQLProcessor.executeQuery(SQLProcessor.java:494)
     * at org.ofbiz.core.entity.GenericDAO.selectListIteratorByCondition(GenericDAO.java:1050)
     * at org.ofbiz.core.entity.GenericDAO.selectByAnd(GenericDAO.java:595)
     * at org.ofbiz.core.entity.GenericHelperDAO.findByAnd(GenericHelperDAO.java:134)
     * at org.ofbiz.core.entity.GenericDelegator.findByAnd(GenericDelegator.java:792)
     * at org.ofbiz.core.entity.GenericDelegator.findByAnd(GenericDelegator.java:777)
     * at org.ofbiz.core.entity.GenericDelegator.findByAnd(GenericDelegator.java:754)
     * <<--------- up till here ------------>>
     * at com.atlassian.jira.issue.managers.DefaultIssueManager.getIssue(DefaultIssueManager.java:79)
     * at com.atlassian.jira.issue.managers.DefaultIssueManager.getIssueObject(DefaultIssueManager.java:237)
     * at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
     * at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:39)
     * at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:25)
     * at java.lang.reflect.Method.invoke(Method.java:597)
     * at com.atlassian.util.profiling.object.ObjectProfiler.profiledInvoke(ObjectProfiler.java:70)
     * at com.atlassian.jira.config.component.SwitchingInvocationHandler.invoke(SwitchingInvocationHandler.java:28)
     * at $Proxy30.getIssueObject(Unknown Source)
     * at com.atlassian.jira.servlet.QuickLinkServlet.service(QuickLinkServlet.java:47)
     * at javax.servlet.http.HttpServlet.service(HttpServlet.java:729)
     * at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:269)
     * ...
     * </pre>
     * <p/>
     * We want some of them gone to improve clarity
     *
     * @param stackTrace the stack trace to filter
     * @return the filtered stack trace string
     */
    private static String filterStacktraceStart(final String stackTrace)
    {
        StringBuilder sb = new StringBuilder(stackTrace.length());

        boolean doneFilteringFirstBit = false;
        String[] lines = stackTrace.split("\\n");
        for (String line : lines)
        {
            if (!doneFilteringFirstBit)
            {
                String tl = line.trim();
                if (tl.indexOf("com.atlassian.jira.ofbiz") != -1 || tl.startsWith("at org.ofbiz.core.entity"))
                {
                    continue;
                }
                else
                {
                    sb.append("call stack\n\t...\n");
                    doneFilteringFirstBit = true;
                    // once we fall out of here we never filter again.  So just the first lines are filtered!
                }
            }
            sb.append(line).append("\n");
        }
        return sb.toString();
    }

    private static class CallStack extends RuntimeException
    {
    }
}
