/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */

/*
 * WebWork, Web Application Framework
 *
 * Distributable under Apache license.
 * See terms of license at opensource.org
 */
package com.atlassian.jira.web.dispatcher;

import com.atlassian.jira.ComponentManager;
import com.atlassian.jira.action.ActionContextKit;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.xsrf.XsrfErrorAction;
import com.atlassian.jira.security.xsrf.XsrfFailureException;
import com.atlassian.jira.startup.JiraStartupChecklist;
import com.atlassian.jira.util.JiraUrlCodec;
import com.atlassian.sal.api.websudo.WebSudoSessionException;
import com.atlassian.seraph.util.RedirectUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import webwork.action.Action;
import webwork.action.ActionContext;
import webwork.action.ResultException;
import webwork.config.Configuration;
import webwork.dispatcher.ActionResult;
import webwork.dispatcher.GenericDispatcher;
import webwork.multipart.MultiPartRequest;
import webwork.multipart.MultiPartRequestWrapper;
import webwork.util.ServletValueStack;
import webwork.util.ValueStack;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.beans.Introspector;
import java.io.CharArrayWriter;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collections;

/**
 * This is the servlet that invokes WebWork actions and then dispatches to their appropriate view.
 * <p/>
 * This servlet is one of the key planks of JIRA architecture.
 * <p/>
 * This code was copied from the webwork ServletDispatcher originally and has since diverted some what for JIRA
 * purposes.  We no longer share any code.
 */
@SuppressWarnings ( { "ThrowableResultOfMethodCallIgnored", "ThrowableInstanceNeverThrown" })
public class JiraWebworkActionDispatcher extends HttpServlet
{
    private static final Logger log = Logger.getLogger(JiraWebworkActionDispatcher.class);

    /**
     * After a view is processed the value of the stack's head is put into the request attributes with this key.
     */
    public static final String STACK_HEAD = "webwork.valuestack.head";
    public static final String GD = "jira.webwork.generic.dispatcher";
    public static final String CLEANUP = "jira.webwork.cleanup";
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final String ACTION_EXTENSION = ".jspa";

    private String saveDir;

    // HttpServlet overrides -----------------------------------------

    /**
     * Initialize dispatcher servlet
     *
     * @param config ServletConfig
     */
    public void init(ServletConfig config) throws ServletException
    {
        super.init(config);

        if (JiraStartupChecklist.startupOK())
        {
            // Clear caches
            // RO: If not, then it will contain garbage after a couple of redeployments
            Introspector.flushCaches();

            // Clear ValueStack method cache
            // RO: If not, then it will contain garbage after a couple of redeployments
            ValueStack.clearMethods();

            // Find the save dir, which should be the servlet context temp directory
            // Use the default - the Servlet Context Temp Directory.
            File tempdir = (File) config.getServletContext().getAttribute("javax.servlet.context.tempdir");
            if (tempdir != null)
            {
                saveDir = tempdir.getAbsolutePath();
            }
            else
            {
                log.error("Servlet Context Temp Directory isn't set. No save directory set for file uploads.");
            }
            log.info("Setting Upload File Directory to '" + saveDir + "'");
            log.info("JiraWebworkActionDispatcher initialized");
        }
        else
        {
            // JIRA startup not OK
            String message = "JIRA startup failed, JIRA has been locked.";
            String line = StringUtils.repeat("*", message.length());
            log.fatal(NEW_LINE + NEW_LINE + line + NEW_LINE + message + NEW_LINE + line + NEW_LINE);
        }
    }

    /**
     * Service a request. The request is first checked to see if it is a multi-part. If it is, then the request is
     * wrapped so WW will be able to work with the multi-part as if it was a normal request. Next, we will process all
     * actions until an action returns a non-action which is usually a view. For each action in a chain, the action's
     * context will be first set and then the action will be instantiated. Next, the previous action if this action
     * isn't the first in the chain will have its attributes copied to the current action.
     *
     * @param httpServletRequest HttpServletRequest
     * @param httpServletResponse HttpServletResponse
     */
    public void service(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse)
            throws ServletException
    {
        //wrap request if needed
        httpServletRequest = wrapRequest(httpServletRequest);

        // If the CLEANUP attribute is NOT set or is set to true - do the cleanup
        //
        // This is set into the request by ActionCleanupDelayFilter always!
        //
        boolean doCleanup = (httpServletRequest.getAttribute(CLEANUP) == null || httpServletRequest.getAttribute(CLEANUP).equals(Boolean.TRUE));

        GenericDispatcher gd = null;

        try
        {
            String actionName = getActionName(httpServletRequest);

            gd = prepareDispatcher(httpServletRequest, httpServletResponse, actionName);

            ActionResult ar = null;
            try
            {
                gd.executeAction();
                ar = gd.finish();
            }
            catch (XsrfFailureException e)
            {
                // if we fail the XSRF check then we show a specific XSRF error page
                //
                // push the action onto the stack so the jsp can use it
                ActionContext.getValueStack().pushValue(new XsrfErrorAction(e));
                //
                // show a particular jsp view
                ar = new ActionResult(Action.ERROR, "/secure/security/xsrf/xsrf.jsp", Collections.EMPTY_LIST, null);
            }
            catch (WebSudoSessionException websudoException)
            {
                // We want websudo session for this action and we dont have it.
                ar = new ActionResult(Action.LOGIN,"/secure/admin/WebSudoAuthenticate!default.jspa?webSudoDestination="+ JiraUrlCodec.encode(httpServletRequest.getServletPath() + (httpServletRequest.getPathInfo() == null ? "" : httpServletRequest.getPathInfo()) + (httpServletRequest.getQueryString() == null ? "" : "?" + httpServletRequest.getQueryString())),Collections.EMPTY_LIST,null);
            }
            catch (Throwable e)
            {
                onActionThrowable(httpServletResponse, actionName, e);
            }

            if (ar != null && ar.getActionException() != null)
            {
                onActionException(ar);
            }

            // check if no view exists
            if (ar != null && ar.getResult() != null && ar.getView() == null && !ar.getResult().equals(Action.NONE))
            {
                onNoViewDefined(httpServletResponse, actionName, ar);
            }

            if (ar != null && ar.getView() != null && ar.getActionException() == null)
            {
                dispatchView(httpServletRequest, httpServletResponse, doCleanup, ar);
            }
        }
        finally
        {
            performFinallyCleanup(httpServletRequest, doCleanup, gd);
        }
    }

    private GenericDispatcher prepareDispatcher(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse, final String actionName)
    {
        final GenericDispatcher gd = new GenericDispatcher(actionName, false);
        gd.prepareContext();
        ActionContextKit.setContext(httpServletRequest, httpServletResponse, this.getServletContext(), actionName);
        gd.prepareValueStack();
        return gd;
    }

    private void onActionException(final ActionResult ar) throws ServletException
    {
        if (log.isDebugEnabled())
        {
            log.debug("Could not execute action", ar.getActionException());
        }

        // this previously sent a 500 response.
        // However, that meant that this exception wasn't being propagated to the error page.
        throw new ServletException(ar.getActionException());
    }

    private void onActionThrowable(final HttpServletResponse httpServletResponse, final String actionName, final Throwable e)
    {
        // This exception isn't the real one if it was thrown in a constructor
        if (log.isDebugEnabled())
        {
            log.debug("Could not execute action : ", e);
        }
        sendErrorImpl(httpServletResponse, 404, "Could not execute action [" + actionName + "]:" + e.getMessage() + getHTMLErrorMessage(e));
    }

    private void onNoViewDefined(final HttpServletResponse httpServletResponse, final String actionName, final ActionResult ar)
    {
        sendErrorImpl(httpServletResponse, 404, "No view for result [" + ar.getResult() + "] exists for action [" + actionName + "]");
    }

    private void sendErrorImpl(final HttpServletResponse httpServletResponse, final int statusCode, final String msg)
    {
        try
        {
            if (!httpServletResponse.isCommitted())
            {
                httpServletResponse.sendError(statusCode, msg);
            }
        }
        catch (IOException e1)
        {
            log.error("Error occurred while sending error response : " + statusCode + " - " + msg + " because of" + e1.getMessage());
        }
    }

    private void dispatchView(final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse, final boolean doCleanup, final ActionResult ar)
            throws ServletException
    {
        if (permissionViolationCheckHandled(ar, httpServletRequest, httpServletResponse, doCleanup))
        {
            return;
        }

        String view = ar.getView().toString();
        if (log.isDebugEnabled())
        {
            log.debug("Result:" + view);
        }
        RequestDispatcher dispatcher = null;
        try
        {
            dispatcher = httpServletRequest.getRequestDispatcher(view);
        }
        catch (Throwable e)
        {
            log.error("Error while dispatching request for view '" + view + "'.", e);
        }

        if (dispatcher == null)
        {
            throw new ServletException("No presentation file with name '" + view + "' found!");
        }

        try
        {
            // If we're included, then include the view
            // Otherwise do forward
            // This allow the page to, for example, set content type
            if (httpServletRequest.getAttribute("javax.servlet.include.servlet_path") == null)
            {
                httpServletRequest.setAttribute("webwork.view_uri", view);
                httpServletRequest.setAttribute("webwork.request_uri", httpServletRequest.getRequestURI());

                dispatcher.forward(httpServletRequest, httpServletResponse);
            }
            else
            {
                dispatcher.include(httpServletRequest, httpServletResponse);
            }
        }
        catch (IOException e)
        {
            throw new ServletException(e);
        }
        finally
        {
            cleanUpStack(httpServletRequest, doCleanup);
        }
    }

    private void cleanUpStack(HttpServletRequest httpServletRequest, boolean doCleanup)
    {
        if (doCleanup)
        {
            // Get last action from stack and and store it in request attribute STACK_HEAD
            // It is then popped from the stack.
            httpServletRequest.setAttribute(STACK_HEAD, ServletValueStack.getStack(httpServletRequest).popValue());
        }
    }


    private boolean permissionViolationCheckHandled(final ActionResult ar, final HttpServletRequest httpServletRequest, final HttpServletResponse httpServletResponse, final boolean doCleanup)
    {
        // special hard coded support for actions returning permission violation that require a login
        if (!isUserLoggedIn())
        {
            final String result = ar.getResult();
            if ("permissionviolation".equals(result) || "securitybreach".equals(result))
            {
                final String loginUrl = RedirectUtils.getLoginUrl(httpServletRequest);
                try
                {
                    httpServletResponse.sendRedirect(loginUrl);
                }
                catch (IOException e)
                {
                    log.error("Unable to redirect permission violation to " + loginUrl);
                }
                finally
                {
                    cleanUpStack(httpServletRequest, doCleanup);
                }
                return true;
            }
        }
        return false;
    }

    private void performFinallyCleanup(final HttpServletRequest httpServletRequest, final boolean doCleanup, final GenericDispatcher gd)
    {
        if (doCleanup)
        {
            if (gd != null)
            {
                gd.finalizeContext();
            }
        }
        else
        {
            // add the GD into the request to allow the ActionCleanupDelayFilter to clean actions up
            httpServletRequest.setAttribute(GD, gd);
        }
    }

    /**
     * Determine action name by extracting last string and removing extension. (/.../.../Foo.action -> Foo)
     *
     * @param httpServletRequest The HTTP request in play
     * @return the "simple" Action name.
     */
    private String getActionName(HttpServletRequest httpServletRequest)
    {
        // Get action
        String servletPath = (String) httpServletRequest.getAttribute("javax.servlet.include.servlet_path");
        if (servletPath == null)
        {
            servletPath = httpServletRequest.getServletPath();
        }

        // Get action name ("Foo.action" -> "Foo" action)
        int beginIdx = servletPath.lastIndexOf("/");
        int endIdx = servletPath.lastIndexOf(ACTION_EXTENSION);
        return servletPath.substring((beginIdx == -1 ? 0 : beginIdx + 1), endIdx == -1 ? servletPath.length() : endIdx);
    }

    /**
     * Wrap servlet request with the appropriate request. It will check to see if request is a multipart request and
     * wrap in appropriately.
     *
     * @param httpServletRequest HttpServletRequest
     * @return wrapped request or original request
     */
    private HttpServletRequest wrapRequest(HttpServletRequest httpServletRequest)
    {
        // don't wrap more than once
        if (httpServletRequest instanceof MultiPartRequestWrapper)
        {
            return httpServletRequest;
        }

        final String disableMultipartGetString = multipartDisableGetString();
        boolean disableMultipartGet = Boolean.valueOf(disableMultipartGetString);

        if (needsMultipartWrapper(httpServletRequest, disableMultipartGet))
        {
            try
            {
                httpServletRequest = new MultiPartRequestWrapper(httpServletRequest, saveDir, getMaxSize());
            }
            catch (IOException e)
            {
                httpServletRequest.setAttribute("webwork.action.ResultException", new ResultException(Action.ERROR, e.getLocalizedMessage()));
            }
        }
        return httpServletRequest;
    }

    private String multipartDisableGetString()
    {
        return ComponentAccessor.getApplicationProperties().
                getDefaultString(APKeys.JIRA_DISABLE_MULTIPART_GET_HTTP_REQUEST);
    }

    private boolean needsMultipartWrapper(final HttpServletRequest httpServletRequest, final boolean disableMultipartGet)
    {
        return MultiPartRequest.isMultiPart(httpServletRequest) &&
                ("POST".equals(httpServletRequest.getMethod()) || ("GET".equals(httpServletRequest.getMethod()) && !disableMultipartGet));
    }

    /**
     * Return an HTML error message for the throwable
     *
     * @param t Throwable
     * @return A String with an error message
     */
    protected String getHTMLErrorMessage(Throwable t)
    {
        return "<p><small><small><pre>" + getStackTrace(t) + "</pre></small></small></p>";
    }

    /**
     * Get the stack trace of the throwable and return as a String
     *
     * @param t Throwable
     * @return Stack trace of the Throwable
     */
    private static String getStackTrace(Throwable t)
    {
        CharArrayWriter sink = new CharArrayWriter(400);
        PrintWriter exceptionWriter = new PrintWriter(sink);
        t.printStackTrace(exceptionWriter);
        return sink.toString();
    }

    private Integer getMaxSize()
    {
        Integer maxSize;
        try
        {
            String maxSizeStr = Configuration.getString(APKeys.JIRA_ATTACHMENT_SIZE);
            if (maxSizeStr != null)
            {
                try
                {
                    maxSize = new Integer(maxSizeStr);
                }
                catch (NumberFormatException e)
                {
                    maxSize = Integer.MAX_VALUE;
                    log.warn("Property '" + APKeys.JIRA_ATTACHMENT_SIZE + "' with value '" + maxSizeStr + "' is not a number. Defaulting to Integer.MAX_VALUE");
                }
            }
            else
            {
                maxSize = Integer.MAX_VALUE;
                log.warn("Property '" + APKeys.JIRA_ATTACHMENT_SIZE + "' is not set. Defaulting to Integer.MAX_VALUE");
            }
        }
        catch (IllegalArgumentException e1)
        {
            maxSize = Integer.MAX_VALUE;
            log.warn("Failed getting string from Configuration for '" + APKeys.JIRA_ATTACHMENT_SIZE + "' property. Defaulting to Integer.MAX_VALUE", e1);
        }
        return maxSize;
    }

    public boolean isUserLoggedIn()
    {
        return ComponentManager.getComponent(JiraAuthenticationContext.class).getLoggedInUser() != null;
    }
}
