package com.atlassian.jira.plugin;

import com.atlassian.jira.config.properties.JiraSystemProperties;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.atlassian.plugin.event.PluginEventManager;
import com.atlassian.plugin.factories.PluginFactory;
import com.atlassian.plugin.loaders.PluginLoader;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.servlet.ServletContextFactory;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This is the bootstrap plugin loader factory of JIRA.  Its similair to {@link DefaultPluginLoaderFactory}
 * but only allows certain bundled plugin through to be loaded.  Just enought to get JIRA bootstrapped and
 * no more.
 */
public class BootstrapPluginLoaderFactory implements PluginLoaderFactory
{
    private static final Logger log = Logger.getLogger(BootstrapPluginLoaderFactory.class);

    private final PluginFactoryAndLoaderRegistrar pluginFactoryAndLoaderRegistrar;

    public BootstrapPluginLoaderFactory(final PluginEventManager pluginEventManager, final OsgiContainerManager osgiContainerManager, final PluginPath pathFactory, ServletContextFactory servletContextFactory)
    {
        this.pluginFactoryAndLoaderRegistrar = new PluginFactoryAndLoaderRegistrar(pluginEventManager, osgiContainerManager, pathFactory, servletContextFactory);
    }

    public List<PluginLoader> getPluginLoaders()
    {
        //
        // we only allow the aui plugin bundled plugin through at this stage and language packs
        ArrayList<Pattern> pluginWhitelist = Lists.newArrayList(
                Pattern.compile("auiplugin-[0-9.]+.*\\.jar"),
                Pattern.compile("JIRA-.*-language-pack-.*\\.jar")
        );

        final List<PluginFactory> pluginFactories = pluginFactoryAndLoaderRegistrar.getDefaultPluginFactories(pluginWhitelist);

        final CollectionBuilder<PluginLoader> pluginLoaderBuilder = CollectionBuilder.newBuilder();

        //
        // plugin loaders for our system-xxx plugins which is basically web-resources at this stage
        pluginLoaderBuilder.addAll(pluginFactoryAndLoaderRegistrar.getBootstrapSystemPluginLoaders());

        //
        // for loading plugin1 plugins the old way (ie. via WEB-INF/lib) which for bootstrapping
        // we dont want.  I have left the code here commented out so you know how it differs to normal
        // plugins loading
        //
        // pluginLoaderBuilder.add(new ClassPathPluginLoader());

        if (JiraSystemProperties.isBundledPluginsDisabled())
        {
            log.warn("Bundled plugins have been disabled. Removing bundled plugin loader.");
        }
        else
        {
            pluginLoaderBuilder.add(pluginFactoryAndLoaderRegistrar.getBundledPluginsLoader(pluginFactories));
        }

        pluginLoaderBuilder.add(pluginFactoryAndLoaderRegistrar.getDirectoryPluginLoader(pluginFactories));

        return pluginLoaderBuilder.asList();
    }

}
