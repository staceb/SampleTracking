package com.atlassian.jira.config.properties;

import org.apache.log4j.Logger;

public class PropertiesUtil
{
    private static final Logger log = Logger.getLogger(PropertiesUtil.class);

    /**
     * Utility method for getting parameters.
     * <p>
     * Parses the defined application property to an int if possible.
     * If the defined value is missing or invalid, then it returns the given defaultValue instead.
     * </p>
     *
     * @param applicationProperties application properties
     * @param defaultValue The default value to use if we can't find an int for the given key.
     * @param propertyKey property key
     * @return the int value for the given property key, or the default value if a valid value was not found.
     */
    public static int getIntProperty(final ApplicationProperties applicationProperties, final String propertyKey, final int defaultValue)
    {
        try
        {
            return Integer.parseInt(applicationProperties.getDefaultBackedString(propertyKey));
        }
        catch (final Exception e)
        {
            log.error("Exception whilst trying to find value for property '" + propertyKey + "'. Defaulting to " + defaultValue + ". " + e);
            return defaultValue;
        }
    }

    private PropertiesUtil()
    {
        throw new AssertionError("cannot instantiate");
    }
}
