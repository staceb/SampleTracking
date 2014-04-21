package com.atlassian.upm.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import com.atlassian.plugins.PacException;
import com.atlassian.plugins.domain.model.plugin.PluginVersion;
import com.atlassian.upm.pac.PacClient;
import com.atlassian.upm.rest.representations.RepresentationFactory;
import com.atlassian.upm.rest.resources.permission.PermissionEnforcer;

import com.google.common.collect.ImmutableList;

import static com.atlassian.upm.rest.MediaTypes.AVAILABLE_FEATURED_JSON;
import static com.atlassian.upm.permission.Permission.GET_AVAILABLE_PLUGINS;
import static com.google.common.base.Preconditions.checkNotNull;

@Path("/available/featured")
public class FeaturedPluginCollectionResource
{
    private final RepresentationFactory factory;
    private final PacClient client;
    private final PermissionEnforcer permissionEnforcer;

    public FeaturedPluginCollectionResource(RepresentationFactory factory, PacClient client, PermissionEnforcer permissionEnforcer)
    {
        this.permissionEnforcer = checkNotNull(permissionEnforcer, "permissionEnforcer");
        this.factory = checkNotNull(factory, "factory");
        this.client = checkNotNull(client, "client");
    }

    @GET
    @Produces(AVAILABLE_FEATURED_JSON)
    public Response get(@QueryParam("max-results") Integer maxResults,
        @QueryParam("start-index") Integer startIndex)
    {
        permissionEnforcer.enforcePermission(GET_AVAILABLE_PLUGINS);
        
        Iterable<PluginVersion> pluginVersions;
        boolean pacUnreachable = false;
        try
        {
            pluginVersions = client.getFeatured(maxResults, startIndex);
        }
        catch (PacException e)
        {
            pluginVersions = ImmutableList.of();
            pacUnreachable = true;
        }

        return Response.ok(factory.createFeaturedPluginCollectionRepresentation(pluginVersions, pacUnreachable)).build();
    }
}
