package com.atlassian.jira.util;

import net.jcip.annotations.NotThreadSafe;
import org.apache.commons.lang.StringUtils;

import java.net.URI;
import java.util.Map;

import static com.atlassian.jira.util.dbc.Assertions.notBlank;
import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Build a URL from parameters. 
 *
 * <p>NOTE: This class does not HTML escape the URLS. Be very careful if using this class to output a URL on the UI.</p>
 *
 * @since v4.0
 */
@NotThreadSafe
public final class UrlBuilder
{
    private final StringBuilder builder;
    private final String encoding;

    private boolean hasQuery = false;

    /**
     * Create a builder with a blank URL.
     *
     * @param snippet whether or not this Url is complete or just a query snippet
     * @throws IllegalArgumentException if the base url is null.
     */
    public UrlBuilder(boolean snippet)
    {
        this("", snippet);
    }

    /**
     * Create a copy of the passed builder. The state of the two builders will be independent after the copy.
     *
     * @param source the builder to copy, cannot be null.
     * @throws IllegalArgumentException if source is null.
     */
    public UrlBuilder(final UrlBuilder source)
    {
        notNull("source", source);
        builder = new StringBuilder(source.builder);
        this.encoding = source.encoding;
        this.hasQuery = source.hasQuery;
    }

    /**
     * Create a builder with the specified base URL. The base URL will be used as the start or the URL generated by this
     * builder.
     *
     * @param baseUrl the basedUrl for the builder. This parameter will not be escaped in the resulting URL.
     * @throws IllegalArgumentException if the base url is null.
     */
    public UrlBuilder(final String baseUrl)
    {
        this(baseUrl, null, false);
    }

    /**
     * Create a builder with the specified base URL. The base URL will be used as the start or the URL generated by this
     * builder.
     *
     * @param baseUrl the basedUrl for the builder. This parameter will not be escaped in the resulting URL.
     * @param snippet whether or not this Url is complete or just a query snippet
     * @throws IllegalArgumentException if the base url is null.
     */
    public UrlBuilder(final String baseUrl, final boolean snippet)
    {
        this(baseUrl, null, snippet);
    }

    /**
     * Create a builder with the specified base URL. The base URL will be used as the start or the URL generated by this
     * builder.
     *
     * @param baseUrl the basedUrl for the builder. This parameter will not be escaped in the resulting URL.
     * @param snippet whether or not this Url is complete or just a query snippet.
     * @param encoding the character encoding to use for parameter names and values. Can be left null (recommended) to indicate JIRA default encoding.
     * @throws IllegalArgumentException if the base url is null.
     */
    public UrlBuilder(final String baseUrl, String encoding, boolean snippet)
    {
        builder = new StringBuilder(notNull("baseUrl", baseUrl));
        this.encoding = encoding;
        this.hasQuery = snippet;
    }

    /**
     * Add the passed parameter to the URL without URL encoding them. This is UNSAFE as it may allow XSS attacks. If you
     * use this mehtod, you must ensure that the parameters are already safe.
     *
     * @param name the name of the parameter. This parameter name is not escaped before it is added to the URL. This
     * value cannot be blank.
     * @param value the value of the parameter. This value is not escaped before it is added to the URL.
     * @return this builder so that calls may be chained.
     * @throws IllegalArgumentException if name is blank.
     */
    public UrlBuilder addParameterUnsafe(final String name, final String value)
    {
        notBlank("name", name);

        addParameterSeparator();
        builder.append(name).append('=').append(value);

        return this;
    }

    /**
     * Add the passed parameter to the URL while URL encoding them.
     *
     * @param name the name of the parameter.This value cannot be blank.
     * @param value the value of the parameter. 
     * @return this builder so that calls may be changed.
     * @throws IllegalArgumentException if name is blank.
     */
    public UrlBuilder addParameter(final String name, final String value)
    {
        notBlank("name", name);

        final String safeName = encode(name);
        final String safeValue = value == null ? "" : encode(value);

        addParameterUnsafe(safeName, safeValue);

        return this;
    }

    /**
     * Add the passed parameter to the URL while URL encoding them.
     *
     * @param name the name of the parameter.This value cannot be blank.
     * @param value the value of the parameter.
     * @return this builder so that calls may be changed.
     * @throws IllegalArgumentException if name is blank.
     */
    public UrlBuilder addParameter(final String name, final Object value)
    {
        return addParameter(name, value == null ? null : value.toString());
    }

    /**
     * Add the passed anchor value to the URL while URL encoding it. The result will be something like <code>#myAnchor</code>.
     * Note that to be compliant with standards, you will want to call this only <em>after</em> adding all your parameters.
     *
     * @param value the value of the anchor.
     * @return this builder so that calls may be changed.
     * @throws IllegalArgumentException if name is blank.
     */
    public UrlBuilder addAnchor(final String value)
    {
        notBlank("value", value);

        final String safeValue = encode(value);

        builder.append("#").append(safeValue);

        return this;
    }

    /**
     * Add multiple parameters from a map safely. Any keys which are null or blank will be ignored.
     * The parameters will be added in order as given by the passed map's entrySet.
     *
     * @param params map containing parameters to add. Must not be null.
     * @return this builder
     */
    public UrlBuilder addParametersFromMap(final Map<?, ?> params)
    {
        notNull("params", params);
        for (Map.Entry<?, ?> entry : params.entrySet())
        {
            if (entry.getKey() == null || StringUtils.isBlank(entry.getKey().toString()))
            {
                continue;
            }
            addParameter(entry.getKey().toString(), entry.getValue());
        }
        return this;
    }


    private void addParameterSeparator()
    {
        if (hasQuery || builder.indexOf("?") > -1)
        {
            builder.append("&");
        }
        else
        {
            builder.append("?");
        }
        hasQuery = true;
    }

    public String asUrlString()
    {
        return builder.toString();
    }

    private String encode(final String str)
    {
        if (str != null)
        {
            if (encoding == null)
            {
                return JiraUrlCodec.encode(str);
            }
            else
            {
                return JiraUrlCodec.encode(str, encoding);
            }
        }
        else
        {
            return str;
        }
    }

    public URI asURI()
    {
        return URI.create(asUrlString());
    }
}
