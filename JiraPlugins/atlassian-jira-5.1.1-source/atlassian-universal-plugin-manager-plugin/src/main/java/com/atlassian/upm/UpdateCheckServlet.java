package com.atlassian.upm;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.google.common.base.Preconditions.checkNotNull;

public final class UpdateCheckServlet extends HttpServlet
{
    private final PluginManagerHandler handler;

    public UpdateCheckServlet(PluginManagerHandler handler)
    {
        this.handler = checkNotNull(handler, "handler");
    }

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException
    {
        handler.handle(request, response, "update-check.vm");
    }
}