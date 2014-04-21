package com.atlassian.jira.configurator.db;

public class ConfigField
{
    private String label;
    private String value;

    public ConfigField(String label)
    {
        this.label = label;
    }

    public String getLabel()
    {
        return label;
    }

    public void setLabel(String label)
    {
        this.label = label;
    }

    /**
     * Returns a non-null value for this ConfigField.
     * If the value is not initialised, or is explicitly set to null, it returns an empty string.
     *
     * @return a non-null value for this ConfigField.
     */
    public String getValue()
    {
        return value == null ? "" : value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }
}
