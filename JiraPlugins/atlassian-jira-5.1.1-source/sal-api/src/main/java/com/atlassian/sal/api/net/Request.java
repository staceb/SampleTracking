package com.atlassian.sal.api.net;

import com.atlassian.sal.api.net.auth.Authenticator;

import java.util.List;
import java.util.Map;

/**
 * Interface Request represents a request to retrieve data. To execute a request call {@link Request#execute(ResponseHandler)}.
 *
 * @param <T> the type of request used for method chaining
 * @since 2.0
 */
public interface Request<T extends Request<?, ?>, RESP extends Response>
{
    /**
     * Represents type of network request
     */
    public static enum MethodType
    {
        GET, POST, PUT, DELETE, HEAD, TRACE, OPTIONS
    }

    /**
     * Setting connection timeout in milliseconds.
     *
     * @param connectionTimeout The timeout in milliseconds
     * @return a reference to this object.
     */
    T setConnectionTimeout(int connectionTimeout);

    /**
     * Setting socket timeout in milliseconds.
     *
     * @param soTimeout the timeout in milliseconds
     * @return a reference to this object.
     */
    T setSoTimeout(int soTimeout);

    /**
     * @param url the url to request
     * @return a reference to this object.
     */
    T setUrl(String url);

    /**
     * Sets the body of the request. In default implementation only requests of type {@link MethodType#POST} and {@link MethodType#PUT} can have a request body.
     *
     * @param requestBody the body of the request
     * @return a reference to this object.
     */
    T setRequestBody(String requestBody);

    /**
     * Sets file parts of the request. File parts can only be added if the request body is empty.
     * Only requests of type {@link MethodType#POST} and {@link MethodType#PUT} can have file parts.
     *
     * @param files the file parts, cannot be null.
     * @return a reference to this object.
     *
     * @throws IllegalArgumentException if the method of this request is not a POST or PUT or files is NULL.
     * @throws IllegalStateException if the request body for this request has already been set.
     *
     * @since 2.6
     */
    T setFiles(List<RequestFilePart> files);

    /**
     * Set an entity as the request body
     *
     * @param entity the request entity to be marshalled, not null
     * @return this Request object
     */
    T setEntity(Object entity);

    /**
     * Sets Content-Type of the body of the request. In default implementation only requests of type {@link MethodType#POST} and {@link MethodType#PUT} can have a request body.
     *
     * @param contentType the contentType of the request
     * @return a reference to this object.
     */
    T setRequestContentType(String contentType);

    /**
     * Sets parameters of the request. In default implementation only requests of type {@link MethodType#POST} can have parameters.
     * For other requests include parameters in url. This method accepts an even number of arguments, in form of name, value, name, value, name, value, etc
     *
     * @param params parameters of the request in as name, value pairs
     * @return a reference to this object.
     */
    T addRequestParameters(String... params);

    /**
     * Adds generic Authenticator to the request.
     *
     * @param authenticator the authenticator to use for authentication
     * @return a reference to this object.
     */
    T addAuthentication(Authenticator authenticator);

    /**
     * Adds TrustedTokenAuthentication to the request. Trusted token authenticator uses current user to make a trusted application call.
     *
     * @return a reference to this object.
     */
    T addTrustedTokenAuthentication();

    /**
     * Adds TrustedTokenAuthentication to the request. Trusted token authenticator uses the passed user to make a trusted application call.
     *
     * @param username The user to make the request with
     * @return this
     */
    T addTrustedTokenAuthentication(String username);

    /**
     * Adds basic authentication to the request.
     *
     * @param username The user name
     * @param password The password
     * @return a reference to this object.
     */
    T addBasicAuthentication(String username, String password);

    /**
     * Adds seraph authentication to the request.
     *
     * @param username The user name
     * @param password The password
     * @return a reference to this object.
     */
    T addSeraphAuthentication(String username, String password);

    /**
     * Adds the specified header to the request, not overwriting any previous value.
     * Support for this operation is optional.
     *
     * @param headerName  the header's name
     * @param headerValue the header's value
     * @return a reference to this object
     * @see RequestFactory#supportsHeader()
     */
    T addHeader(String headerName, String headerValue);

    /**
     * Sets the specified header to the request, overwriting any previous value.
     * Support for this operation is optional.
     *
     * @param headerName  the header's name
     * @param headerValue the header's value
     * @return a reference to this object
     * @see RequestFactory#supportsHeader()
     */
    T setHeader(String headerName, String headerValue);
    
    /**
     * Sets whether the request should transparently follow a redirect from the server. The 
     * default behavior is that when a response is received with a status code in the 300s, a new request 
     * is made using the location header of the response without notifying the client. Set this to false to 
     * turn this behavior off.
     * 
     * @param follow set this to false to have the request not transparently follow redirects.
     * @return a reference to this object.
     */    
    T setFollowRedirects(boolean follow);
    
    /**
     * @return an immutable Map of headers added to the request so far
     * @since 2.1
     */
    Map<String, List<String>> getHeaders();

    /**
     * Executes the request.
     *
     * @param responseHandler Callback handler of the response.
     * @throws ResponseProtocolException If the server returned a malformed response
     * @throws ResponseTimeoutException If a connection timeout or read timeout occurred
     * @throws ResponseTransportException If an I/O error occurred in request transport
     * @throws ResponseException For all errors not otherwise specified
     */
    void execute(ResponseHandler<RESP> responseHandler) throws ResponseException;

    /**
     * Executes a request and if response is successful, returns response as a string. @see {@link Response#getResponseBodyAsString()}
     *
     * @return response as String
     * @throws ResponseStatusException If the server returned a response that contained an error code
     * @throws ResponseProtocolException If the server returned a malformed response
     * @throws ResponseTimeoutException If a connection timeout or read timeout occurred
     * @throws ResponseTransportException If an I/O error occurred in request transport
     * @throws ResponseException For all errors not otherwise specified
     */
    String execute() throws ResponseException;

    /**
     * Executes the request and returns a result value.
     *
     * @param responseHandler Callback handler of the response.
     * @throws ResponseProtocolException If the server returned a malformed response
     * @throws ResponseTimeoutException If a connection timeout or read timeout occurred
     * @throws ResponseTransportException If an I/O error occurred in request transport
     * @throws ResponseException For all errors not otherwise specified
     * @since   v2.2.0
     */
    <RET> RET executeAndReturn(ReturningResponseHandler<RESP, RET> responseHandler) throws ResponseException;
}
