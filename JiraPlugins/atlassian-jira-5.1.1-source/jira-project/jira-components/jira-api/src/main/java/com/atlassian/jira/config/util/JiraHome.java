package com.atlassian.jira.config.util;

import com.atlassian.jira.util.NotNull;
import com.atlassian.jira.util.PathUtils;
import com.atlassian.jira.util.collect.CollectionBuilder;

import java.io.File;
import java.util.Set;

/**
 * Get the location of JIRA's home directory.
 */
public interface JiraHome
{
    /**
     * Name of the "Caching" directory in JIRA home.
     */
    public final String CACHES = "caches";

    /**
     * Name of the "Data" directory in JIRA home.
     */
    public final String DATA = "data";

    /**
     * Name of the "Plugins" directory in JIRA home.
     */
    public final String PLUGINS = "plugins";

    /**
     * Name of the "Export" directory in JIRA home.
     */
    public final String EXPORT = "export";

    /**
     * Name of the "Import" directory in JIRA home.
     */
    public final String IMPORT = "import";

    /**
     * Name of the "Import/Attachments" directory in JIRA home.
     */
    public final String IMPORT_ATTACHMENTS = "attachments";

    /**
     * Name of the "Log" directory in JIRA home.
     */
    public final String LOG = "log";

    /**
     * The subdirectories of jira-home. See https://extranet.atlassian.com/display/DEV/Product+Home+Directory+Specification+v1.1
     * Currently JIRA does not use tmp or config
     */
    final Set<String> subdirs = CollectionBuilder.<String>newBuilder(CACHES, DATA, PLUGINS, EXPORT, IMPORT, PathUtils.joinPaths(IMPORT, IMPORT_ATTACHMENTS), LOG ).asSet();

    /**
     * Get the canonical path to the JIRA home directory.
     *
     * @return the String path
     * @throws IllegalStateException if the JIRA home is not set.
     */
    String getHomePath();

    /**
     * Get the {@link File} object representing the JIRA home directory.
     *
     * @return the file path, must be a directory
     * @throws IllegalStateException if the JIRA home is not set.
     */
    @NotNull
    File getHome();

    /**
     * Get the {@link File} object representing the log directory in JIRA home.
     *
     * @return the file path, must be a directory.
     * @throws IllegalStateException if the JIRA home is not set.
     */
    File getLogDirectory();

    /**
     * Get the {@link File} object representing the caches directory in JIRA home.
     *
     * @return the file path, must be a directory.
     * @throws IllegalStateException if the JIRA home is not set.
     */
    File getCachesDirectory();

    /**
     * Get the {@link File} object representing the export directory in JIRA home.
     *
     * @return the file path, must be a directory.
     * @throws IllegalStateException if the JIRA home is not set.
     */
    File getExportDirectory();

    /**
     * Get the {@link File} object representing the import directory in JIRA home.
     *
     * @return the file path, must be a directory.
     * @throws IllegalStateException if the JIRA home is not set.
     */
    File getImportDirectory();

    /**
     * Get the {@link File} object representing the import/attachments directory in JIRA home.
     *
     * @return the file path, must be a directory.
     * @throws IllegalStateException if the JIRA home is not set.
     */
    File getImportAttachmentsDirectory();

    /**
     * Get the {@link File} object representing the plugins directory in JIRA home.
     *
     * @return the file path, must be a directory.
     * @throws IllegalStateException if the JIRA home is not set.
     */
    File getPluginsDirectory();

    /**
     * Get the {@link File} object representing the data directory in JIRA home.
     *
     * @return the file path, must be a directory.
     * @throws IllegalStateException if the JIRA home is not set.
     */
    File getDataDirectory();
}
