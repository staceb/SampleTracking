/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */

package com.atlassian.jira.plugins.mail.webwork;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.CoreFeatures;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.config.properties.JiraSystemProperties;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.util.PortUtil;
import com.atlassian.jira.util.collect.MapBuilder;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.mail.MailException;
import com.atlassian.mail.MailFactory;
import com.atlassian.mail.MailProtocol;
import com.atlassian.mail.server.MailServer;
import com.atlassian.mail.server.MailServerManager;
import com.atlassian.mail.server.PopMailServer;
import com.atlassian.mail.server.SMTPMailServer;
import com.atlassian.plugin.webresource.WebResourceManager;
import com.opensymphony.util.TextUtils;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Do not put here @WebSudoRequired, if you do VeritySmtpServerConnection will not work
 */
public class MailServerActionSupport extends JiraWebActionSupport
{
    protected Long id;
    private String name = null;
    private String description;
    private String type;
    private String serverName;
    private String jndiLocation;
    private String username;
    private boolean changePassword;
    private String password;
    private String from;
    private String prefix;
    private String port;
    private String protocol;
    private Long timeout;
    private boolean tlsRequired;
    private boolean noemail;
    private String mailservertype;

    private Map<String, String> keysForPrefix =  MapBuilder.<String, String>newBuilder()
                                                    .add("name","common.words.name")
                                                    .add("port","admin.mailservers.smtp.port")
                                                    .add("serverName","admin.mailservers.host.name")
                                                    .add("username","common.words.username")
                                                    .add("password","common.words.password")
                                                    .add("from","admin.mailservers.from.address")
                                                    .add("prefix","admin.mailservers.email.prefix")
                                                    .toMap();


    public boolean canManageSmtpMailServers()
    {
        return getPermissionManager().hasPermission(Permissions.SYSTEM_ADMIN, getLoggedInUser());
    }

    public boolean canManagePopMailServers()
    {
        return getPermissionManager().hasPermission(Permissions.SYSTEM_ADMIN, getLoggedInUser()) || getPermissionManager().hasPermission(Permissions.ADMINISTER, getLoggedInUser());
    }
    public String doView()
    {
        return SUCCESS;
    }

    public String doDefault() throws Exception
    {
        includeResources();
        if (id == null || id <= 0)
        {
            return getRedirect(ViewMailServers.OUTGOING_MAIL_ACTION);
        }

        MailServer mailServer = MailFactory.getServerManager().getMailServer(id);

        if (mailServer == null)
        {
            return getRedirect(ViewMailServers.OUTGOING_MAIL_ACTION);
        }

        if ((isPop(mailServer) && canManagePopMailServers()) || (isSmtp(mailServer) && canManageSmtpMailServers()))
        {
            setDescription(mailServer.getDescription());
            setId(mailServer.getId());
            setName(mailServer.getName());
            setPassword(mailServer.getPassword());
            setServerName(mailServer.getHostname());
            setProtocol(mailServer.getMailProtocol().getProtocol());
            setPort(mailServer.getPort());
            setType(mailServer.getType());
            setTimeout(mailServer.getTimeout());

            if (MailServerManager.SERVER_TYPES[1].equals(mailServer.getType()))
            {
                if (mailServer instanceof SMTPMailServer)
                {
                    SMTPMailServer smtpMailServer = (SMTPMailServer) mailServer;

                    if (StringUtils.isNotBlank(smtpMailServer.getJndiLocation()))
                    {
                        setJndiLocation(smtpMailServer.getJndiLocation());
                    }

                    setFrom(smtpMailServer.getDefaultFrom());
                    setPrefix(smtpMailServer.getPrefix());
                    setTlsRequired(smtpMailServer.isTlsRequired());
                }
            }

            setUsername(mailServer.getUsername());
            return INPUT;
        }
        else
        {
            return "securitybreach";
        }
    }

    protected boolean isSmtp(MailServer mailServer)
    {
        return mailServer.getType().equals(MailServerManager.SERVER_TYPES[1]);
    }

    protected boolean isPop(MailServer mailServer)
    {
        return mailServer.getType().equals(MailServerManager.SERVER_TYPES[0]);
    }

    private void includeResources()
    {
        WebResourceManager webResourceManager = ComponentAccessor.getWebResourceManager();
        webResourceManager.requireResource("com.atlassian.jira.jira-mail-plugin:verifymailserverconnection");
    }

    protected void doValidation()
    {
        includeResources();

        if (getId() != null) {
            MailServer mailServer = null;
            try
            {
                mailServer = MailFactory.getServerManager().getMailServer(id);
            }
            catch (MailException e)
            {
                throw new RuntimeException(e);
            }
            if (mailServer != null && !StringUtils.defaultString(mailServer.getUsername()).equals(
                    StringUtils.defaultString(getUsername()))) {
                changePassword = true;
            }
        }


        if (StringUtils.isBlank(name))
        {
            addError("name", getText("admin.errors.must.specify.name.of.mail.server"));
        }
        if (StringUtils.isNotBlank(port) && !PortUtil.isValidPort(port))
        {
            addError("port", getText("admin.errors.smtp.port.must.be.a.number.between"));
        }

        if (getTypes()[0].equals(type))
        {
            //Pop server
            if (StringUtils.isBlank(serverName))
            {
                addError("serverName", getText("admin.errors.must.specify.location.of.server"));
            }
            if (StringUtils.isBlank(username))
            {
                addError("username", getText("admin.errors.must.specify.a.username"));
            }
            if (changePassword && StringUtils.isBlank(password))
            {
                addError("password", getText("admin.errors.must.specify.a.password"));
            }
        }
        else if (getTypes()[1].equals(type))
        {
            //SMTP server
            if (!TextUtils.verifyEmail(from))
            {
                addError("from", getText("admin.errors.must.specify.a.valid.from.address"));
            }
            if (StringUtils.isBlank(prefix))
            {
                addError("prefix", getText("admin.errors.must.specify.a.email.prefix"));
            }
            if (StringUtils.isBlank(serverName) && StringUtils.isBlank(jndiLocation))
            {
                addErrorMessage(getText("admin.errors.must.specify.a.host.name.or.jndi.location"));
            }
            else if (StringUtils.isNotBlank(jndiLocation) && StringUtils.isNotBlank(serverName))
            {
                addErrorMessage(getText("admin.errors.cannot.specify.both.a.host.name.and.jndi.location"));
            }
            else if (StringUtils.isNotBlank(jndiLocation) && (StringUtils.isNotBlank(username) || (changePassword && StringUtils.isNotBlank(password))))
            {
                addErrorMessage(getText("admin.errors.when.specifying.a.jndi.location"));
            }
            else
            {
                //If the username is filled in a password is required
                if (changePassword && StringUtils.isNotBlank(username) && StringUtils.isBlank(password))
                {
                    addError("password", getText("admin.errors.must.specify.password"));
                }
            }
        }
        else
        {
            addErrorMessage(getText("admin.errors.must.specify.the.server.type"));
        }

        //Check to see if there is already a Mail Server with this name
        try
        {
            MailServer existingMailServer = MailFactory.getServerManager().getMailServer(getName());
            if ((existingMailServer != null) && ((getId() == null) || (existingMailServer.getId().longValue() != getId().longValue())))
            {
                addError("name", getText("admin.errors.mail.server.with.this.name.exists"));
            }
        }
        catch (MailException e)
        {
            addErrorMessage(getText("admin.errors.an.error.occured.when.processing.the.mail.sever") + " " + e.getMessage());
            log.error("Error processing Mail Server", e);
        }
    }

    public List<PopMailServer> getPopMailServers() throws MailException
    {
        return MailFactory.getServerManager().getPopMailServers();
    }

    public List<SMTPMailServer> getSmtpMailServers() throws MailException
    {
        return MailFactory.getServerManager().getSmtpMailServers();
    }

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public Long getTimeout()
    {
        return timeout;
    }

    public void setTimeout(Long timeout)
    {
        this.timeout = timeout;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        if (MailServerManager.SERVER_TYPES[0].equals(type) || MailServerManager.SERVER_TYPES[1].equals(type))
        {
            this.type = type;
        }
        else
        {
            this.type = null;
        }
    }

    public String[] getTypes()
    {
        return MailServerManager.SERVER_TYPES;
    }

    public String getServerName()
    {
        return serverName;
    }

    public void setServerName(String serverName)
    {
        if (serverName != null)
        {
            this.serverName = serverName.trim();
        }
        else
        {
            this.serverName = null;
        }
    }

    public String getJndiLocation()
    {
        return jndiLocation;
    }

    public void setJndiLocation(String jndiLocation)
    {
        this.jndiLocation = StringUtils.trim(jndiLocation);
    }

    public String getUsername()
    {
        return username;
    }

    public void setUsername(String username)
    {
        this.username = username;
    }

    /**
     * Returns the password received from the UI if Change Password was selected or present in input hidden, or the one
     * already set for given server Id.
     * Returns null if existing server not found - normal condition outside UpdateServer context.
     *
     * @return password just set or the existing one if Change Password not selected in the UI
     */
    @Nullable
    public String getPassword()
    {
        if (changePassword) {
            return password;
        }

        if (id == null) {
            return null;
        }
        try
        {
            final MailServer mailServer = MailFactory.getServerManager().getMailServer(id);
            return mailServer == null ? null : mailServer.getPassword();
        }
        catch (MailException e)
        {
            log.warn("Cannot retrieve mail server with ID " + id, e);
            return null;
        }
    }

    public void setPassword(String password)
    {
        this.password = password;
    }

    // display the password in the password field only if it was just set with changePassword
    @Nullable
    public String getPasswordSet() {
        return changePassword ? password : null;
    }

    public boolean isChangePassword()
    {
        return changePassword || getErrors().containsKey("password");
    }

    public void setChangePassword(boolean changePassword)
    {
        this.changePassword = changePassword;
    }

    public String getFrom()
    {
        return from;
    }

    public void setFrom(String from)
    {
        this.from = from;
    }

    public String getPrefix()
    {
        return prefix;
    }

    public void setPrefix(String prefix)
    {
        this.prefix = prefix;
    }

    public String getPort()
    {
        return port;
    }

    public void setPort(String port)
    {
        if (port != null)
        {
            this.port = port.trim();
        }
        else
        {
            this.port = null;
        }
    }

    public String getProtocol()
    {
        return protocol;
    }

    public void setProtocol(final String protocol)
    {
        this.protocol = protocol;
    }

    public boolean isTlsRequired()
    {
        return tlsRequired;
    }

    public void setTlsRequired(final boolean tlsRequired)
    {
        this.tlsRequired = tlsRequired;
    }

    public boolean isValidMailParameters()
    {
        return JiraSystemProperties.isDecodeMailParameters();
    }


    public MailProtocol[] getSupportedClientProtocols(String type)
    {
        if (StringUtils.isNotBlank(type))
        {
            return MailProtocol.getMailProtocolsForServerType(type);
        }
        else
        {
            return new MailProtocol[0];
        }
    }

    public String getKeyForPrefix(String prefix)
    {
        final String key = keysForPrefix.get(prefix);
        return (key != null) ? key : prefix;
    }

    public boolean isNoemail()
    {
        return noemail;
    }

    public void setNoemail(boolean noemail)
    {
        this.noemail = noemail;
    }

    public String getMailservertype()
    {
        return mailservertype;
    }

    public void setMailservertype(String mailservertype)
    {
        this.mailservertype = mailservertype;
    }

    public String getActiveTab() {
        return "mail_servers";
    }

    /**
     * Configure SOCKS on the given mail server.
     *
     * @param mailServer The mail server.
     */
    protected void configureSocks(MailServer mailServer)
    {
        // Unfortunately this adds SOCKS configuration to OnDemand internal
        // servers but we can't identify them at the moment. In practice, this
        // just means that all test connections will go through the proxy.
        final FeatureManager featureManager = ComponentAccessor.getComponent(FeatureManager.class);
        if (featureManager.isEnabled(CoreFeatures.ON_DEMAND))
        {
            Properties systemProperties = System.getProperties();
            mailServer.setSocksHost(systemProperties.getProperty("studio.socks.host"));
            mailServer.setSocksPort(systemProperties.getProperty("studio.socks.port"));
        }
    }
}
