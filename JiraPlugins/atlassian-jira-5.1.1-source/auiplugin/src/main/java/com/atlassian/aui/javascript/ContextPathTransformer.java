package com.atlassian.aui.javascript;

import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformer;
import com.atlassian.sal.api.ApplicationProperties;
import com.google.common.base.Function;
import org.dom4j.Element;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A transformer that is used to do the appropriate search-and-replaces into context-path.cp.js
 *
 * @since 3.5
 */
public class ContextPathTransformer implements WebResourceTransformer
{
    private final Pattern PATTERN = Pattern.compile("(%CONTEXT_PATH%)");

    private final String contextPath;
    private final SearchAndReplacer grep;

    public ContextPathTransformer(ApplicationProperties appProps) throws URISyntaxException
    {
        Function<Matcher, String> fn = new Function<Matcher, String>()
        {
            public String apply(Matcher from)
            {
                return doReplace(from);
            }
        };
        this.grep = new SearchAndReplacer(PATTERN, fn);

        String baseUrl = stripTrailingSlash(appProps.getBaseUrl());
        contextPath = new URI(baseUrl).getPath();
    }

    private static String stripTrailingSlash(String base) {
        if (base.endsWith("/"))
        {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private String doReplace(Matcher match)
    {
        StringBuilder result = new StringBuilder();
        String key = match.group(1);
        if ("%CONTEXT_PATH%".equals(key))
        {
            result.append(JavaScriptUtil.escape(contextPath));
        }
        else
        {
            result.append("%CONTEXT_PATH%");
        }
        return result.toString();
    }

    public DownloadableResource transform(Element configElement, ResourceLocation location,
                                          String filePath, DownloadableResource nextResource)
    {
        return new SearchAndReplaceDownloadableResource(nextResource, grep);
    }
}
