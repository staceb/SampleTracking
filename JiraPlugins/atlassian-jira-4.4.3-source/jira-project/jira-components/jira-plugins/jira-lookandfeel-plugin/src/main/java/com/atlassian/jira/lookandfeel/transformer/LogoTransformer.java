package com.atlassian.jira.lookandfeel.transformer;

import com.atlassian.jira.lookandfeel.LookAndFeelProperties;
import com.atlassian.plugin.elements.ResourceLocation;
import com.atlassian.plugin.servlet.DownloadableResource;
import com.atlassian.plugin.webresource.UrlMode;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.atlassian.plugin.webresource.transformer.AbstractStringTransformedDownloadableResource;
import com.atlassian.plugin.webresource.transformer.WebResourceTransformer;
import org.dom4j.Element;

/**
 *
 *
 * @since v4.4
 */
public class LogoTransformer implements WebResourceTransformer
{
    private final LookAndFeelProperties lookAndFeelProperties;
    private final WebResourceManager webResourceManager;

    public LogoTransformer(LookAndFeelProperties lookAndFeelProperties, WebResourceManager webResourceManager)
    {
        this.lookAndFeelProperties = lookAndFeelProperties;
        this.webResourceManager = webResourceManager;
    }

    @Override
    public DownloadableResource transform(Element configElement, ResourceLocation location, String filePath, DownloadableResource nextResource)
    {
        return new AbstractStringTransformedDownloadableResource(nextResource)
        {
            protected String transform(String originalContent)
            {
                String prefix = webResourceManager.getStaticResourcePrefix(UrlMode.AUTO);
                // JRADEV-6590 If this is the root app, then ignore the slash
                if ("/".equals(prefix))
                {
                    prefix = "";
                }
                String defaultLogoUrl = lookAndFeelProperties.getDefaultCssLogoUrl();
                if (defaultLogoUrl != null && !defaultLogoUrl.startsWith("http://") && !defaultLogoUrl.startsWith("."))
                {
                    defaultLogoUrl = prefix+defaultLogoUrl;
                }
                return originalContent.replace("@defaultLogoUrl", defaultLogoUrl);
            }
        };
    }
}
