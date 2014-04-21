/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */

package com.atlassian.jira.web.action.setup;

import com.atlassian.jira.bc.license.JiraLicenseService;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.extension.JiraStartedEvent;
import com.atlassian.jira.issue.fields.layout.field.EditableDefaultFieldLayout;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutItem;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.fields.renderer.RenderableField;
import com.atlassian.jira.issue.fields.renderer.wiki.AtlassianWikiRenderer;
import com.atlassian.jira.license.LicenseJohnsonEventRaiser;
import com.atlassian.jira.upgrade.UpgradeManager;
import com.atlassian.jira.user.util.SneakyAutoLoginUtil;
import com.atlassian.jira.util.FileFactory;
import com.atlassian.plugin.event.PluginEventManager;
import webwork.action.ActionContext;

import java.util.Collection;
import java.util.List;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * This setup step is used to complete setup.
 */
public class SetupComplete extends AbstractSetupAction
{
    private final UpgradeManager upgradeManager;
    private final LicenseJohnsonEventRaiser licenseJohnsonEventRaiser;
    private final JiraLicenseService licenseService;
    private final SubTaskManager subTaskManager;
    private final FieldLayoutManager fieldLayoutManager;
    private final PluginEventManager pluginEventManager;

    public SetupComplete(UpgradeManager upgradeManager, final LicenseJohnsonEventRaiser licenseJohnsonEventRaiser,
            final JiraLicenseService licenseService, final SubTaskManager subTaskManager,
            final FieldLayoutManager fieldLayoutManager, FileFactory fileFactory, final PluginEventManager pluginEventManager)
    {
        super(fileFactory);
        this.pluginEventManager = notNull("pluginEventManager", pluginEventManager);
        this.licenseService = notNull("licenseService", licenseService);
        this.upgradeManager = notNull("upgradeManager", upgradeManager);
        this.licenseJohnsonEventRaiser = notNull("licenseJohnsonEventRaiser", licenseJohnsonEventRaiser);
        this.subTaskManager = notNull("subTaskManager", subTaskManager);
        this.fieldLayoutManager = notNull("fieldLayoutManager", fieldLayoutManager);
    }

    protected String doExecute() throws Exception
    {
        if (setupAlready())
        {
            return SETUP_ALREADY;
        }

        // do anything here you want to setup (ie default settings)
        getApplicationProperties().setString(APKeys.JIRA_SETUP, "true");

        initialiseSystemPropertiesBeforeSetupUpgradeTasks();

        // check that if the current build is newer than the license by more than
        // 366 days
        // Ensure that this runs before the upgrade tasks so that if there is a licensing inconsistency
        // upgrade tasks do not run. After a new license is entered or the confirmation of proceding is made
        // the server will ask to be rebooted. On startup (after reboot) the upgrade tasks will run. The reboot
        // is in place to maintain consistency in data.
        if (licenseTooOld())
        {
            //need to redirect to the error page, otherwise a duplicate decorator is applied - JRA-11988
            return getRedirect("/secure/errors.jsp");
        }

        Collection<String> errors = upgradeManager.doSetupUpgrade();

        if (!errors.isEmpty())
        {
            for (final String error : errors)
            {
                addErrorMessage(error);
            }
        }
        else
        {
            initialiseSystemPropertiesAfterSetupUpgradeTasks();
            //this is here so that al SAL lifeCycleAware components get notified of JIRA
            //being started since this only happens when JIRA is setup.
            pluginEventManager.broadcast(new JiraStartedEvent());
        }

        if (logUserIn())
        {
            return "loggedin";
        }

        return getResult();
    }
    
    private boolean logUserIn()
    {
        // Try to log the user in automatically
        try
        {
            final SetupAdminUserSessionStorage sessionStorage = (SetupAdminUserSessionStorage) request.getSession().getAttribute(SetupAdminUserSessionStorage.SESSION_KEY);
            request.getSession().removeAttribute(SetupAdminUserSessionStorage.SESSION_KEY);
            if (sessionStorage == null)
            {
                log.warn("Unable to automatically login after setup complete: sessionStorage is null");
            }
            else
            {
                return SneakyAutoLoginUtil.logUserIn(sessionStorage.getUsername(), request);
            }
        }
        catch (final Exception e)
        {
            log.warn("Error with automatic login after setup complete. The user will need to login in manually.", e);
        }
        
        return false;
    }

    /**
     * Initialises application properties and other parts of the system <strong>before</strong> the setup upgrade tasks
     * are executed.
     */
    private void initialiseSystemPropertiesBeforeSetupUpgradeTasks()
    {
        getApplicationProperties().setOption(APKeys.JIRA_OPTION_ALLOWUNASSIGNED, false);
        getApplicationProperties().setOption(APKeys.JIRA_OPTION_USER_EXTERNALMGT, false);
        getApplicationProperties().setOption(APKeys.JIRA_OPTION_VOTING, true);
        getApplicationProperties().setOption(APKeys.JIRA_OPTION_WATCHING, true);
        getApplicationProperties().setOption(APKeys.JIRA_OPTION_ISSUELINKING, true);
        getApplicationProperties().setString(APKeys.JIRA_OPTION_EMAIL_VISIBLE, "show");
    }

    /**
     * Initialises application properties and other parts of the system <strong>after</strong> the setup upgrade tasks
     * are executed. This might be necessary for tasks which rely on default data being populated by those upgrade tasks.
     */
    private void initialiseSystemPropertiesAfterSetupUpgradeTasks()
    {
        // sub tasks must be enabled *AFTER* upgrade tasks have been run as they rely on the Default Issue Type Scheme being created.
        enableSubTasks();

        setWikiRendererOnAllRenderableFields();
    }

    private void enableSubTasks()
    {
        try
        {
            subTaskManager.enableSubTasks();
        }
        catch (CreateException e)
        {
            log.error("Error encountered when trying to enable sub tasks", e);
            throw new RuntimeException(e);
        }
    }

    ///CLOVER:OFF
    void setWikiRendererOnAllRenderableFields()
    {
        final EditableDefaultFieldLayout editableDefaultFieldLayout = fieldLayoutManager.getEditableDefaultFieldLayout();
        final List<FieldLayoutItem> fieldLayoutItems = editableDefaultFieldLayout.getFieldLayoutItems();
        for (FieldLayoutItem fieldLayoutItem : fieldLayoutItems)
        {
            if (fieldLayoutItem.getOrderableField() instanceof RenderableField)
            {
                RenderableField field = (RenderableField) fieldLayoutItem.getOrderableField();
                if (field.isRenderable())
                {
                    editableDefaultFieldLayout.setRendererType(fieldLayoutItem, AtlassianWikiRenderer.RENDERER_TYPE);
                }
            }
        }
        fieldLayoutManager.storeEditableDefaultFieldLayout(editableDefaultFieldLayout);
    }
    ///CLOVER:ON

    protected boolean licenseTooOld()
    {
        return licenseJohnsonEventRaiser.checkLicenseIsTooOldForBuild(ActionContext.getServletContext(), licenseService.getLicense());
    }

    public String doDefault() throws Exception
    {
        return doExecute();
    }
}
