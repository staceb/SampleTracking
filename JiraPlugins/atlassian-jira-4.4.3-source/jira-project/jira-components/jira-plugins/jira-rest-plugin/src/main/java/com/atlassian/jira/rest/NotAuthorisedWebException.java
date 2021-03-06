package com.atlassian.jira.rest;

import com.atlassian.jira.rest.api.util.ErrorCollection;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import static com.atlassian.jira.rest.api.http.CacheControl.never;

/**
 * This exception is thrown when a caller is not authorised to access a JIRA REST resource.
 *
 * @since v4.2
 */
public class NotAuthorisedWebException extends WebApplicationException
{
    /**
     * Creates a new NotAuthorisedWebException for the given issue. Whenever possible it is preferable to use {@link
     * #NotAuthorisedWebException(com.atlassian.jira.rest.api.util.ErrorCollection)} constructor, passing a collection
     * of errors.
     */
    public NotAuthorisedWebException()
    {
        this(ErrorCollection.builder().build());
    }

    /**
     * Creates a new NotAuthorisedWebException for the given issue, with a collection of errors.
     *
     * @param errors an ErrorCollection containing the errors
     */
    public NotAuthorisedWebException(ErrorCollection errors)
    {
        super(createResponse(errors));
    }

    /**
     * Creates a new HTTP response with status 404 (not found), returning the errors in the provided ErrorCollection.
     *
     * @param errors an ErrorCollection containing errors
     * @return a Response
     */
    private static Response createResponse(ErrorCollection errors)
    {
        // the issue key is not used yet, but should make it into the entity in the future...
        return Response.status(Response.Status.UNAUTHORIZED).entity(errors).cacheControl(never()).build();
    }
}
