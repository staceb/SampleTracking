package com.atlassian.jira.configurator.db;

import com.atlassian.jira.configurator.config.Settings;
import com.atlassian.jira.configurator.config.ValidationException;
import com.atlassian.jira.exception.ParseException;

import javax.swing.*;

/**
 * Holds config options like username and password that are common to most DB types.
 */
public abstract class CommonConfigPanel extends DatabaseConfigPanel
{
    protected JTextField tfHostname = new JTextField(TEXT_FIELD_COLUMNS);
    protected JTextField tfPort = new JTextField(TEXT_FIELD_COLUMNS);
    protected JTextField tfUsername = new JTextField(TEXT_FIELD_COLUMNS);
    protected JTextField tfPassword = new JTextField(TEXT_FIELD_COLUMNS);

    {
        tfHostname.setToolTipText("The hostname or IP address of the database server");
        tfPort.setToolTipText("The port number that the DB server is listening on");
        tfUsername.setToolTipText("The password used to log into the DB server");
        tfPassword.setToolTipText("The password used to log into the DB server");
    }

    private JPanel configPanel;
    private final DatabaseConfig databaseConfig;

    public CommonConfigPanel(final DatabaseConfig databaseConfig)
    {
        this.databaseConfig = databaseConfig;
    }

    @Override
    public String getUsername()
    {
        return tfUsername.getText();
    }

    @Override
    public String getPassword()
    {
        return tfPassword.getText();
    }

    @Override
    public String getClassName()
    {
        return databaseConfig.getClassName();
    }

    @Override
    public String getUrl(String jiraHome) throws ValidationException
    {
        return databaseConfig.getUrl(getHostname(), getPort(), getInstance());
    }

    protected abstract String getHostname();
    protected abstract String getPort();
    protected abstract String getInstance();

    @Override
    public final JPanel getPanel()
    {
        if (configPanel == null)
        {
            configPanel = buildConfigPanel();
        }
        return configPanel;
    }

    protected abstract JPanel buildConfigPanel();

    @Override
    public void setSettings(final Settings settings) throws ParseException
    {
        tfUsername.setText(settings.getDbUsername());
        tfPassword.setText(settings.getDbPassword());
        // parse the URL.
        setUrlSettings(databaseConfig.parseUrl(settings.getJdbcUrl()));
        // some DB's may be interested in the schema-name from the entity-engine.xml
        setSchemaName(settings.getSchemaName());
    }

    protected abstract void setUrlSettings(DatabaseInstance connectionProperties) throws ParseException;
    protected abstract void setSchemaName(String schemaName);

    @Override
    public void validate() throws ValidationException
    {
        // Port can be empty, but must be a valid number if not
        validatePortNumber(tfPort.getText());
    }

}
