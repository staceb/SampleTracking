package com.atlassian.jira.plugin.navigation;

import com.atlassian.jira.bc.license.JiraLicenseService;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.license.LicenseDetails;
import com.atlassian.jira.plugin.AbstractJiraModuleDescriptor;
import com.atlassian.jira.plugin.util.ModuleDescriptorXMLUtils;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.util.BuildUtilsInfo;
import com.atlassian.jira.web.util.ExternalLinkUtilImpl;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.module.ModuleFactory;
import com.atlassian.util.profiling.UtilTimerStack;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Element;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

import static com.atlassian.jira.config.properties.APKeys.JIRA_SHOW_CONTACT_ADMINISTRATORS_FORM;
import static com.atlassian.jira.security.Permissions.SYSTEM_ADMIN;
import static com.atlassian.jira.util.dbc.Assertions.notNull;

/**
 * Module descriptor for footer modules.
 *
 * @since v3.12
 */
public class FooterModuleDescriptorImpl extends AbstractJiraModuleDescriptor<PluggableFooter> implements FooterModuleDescriptor
{
    private static final String VIEW_TEMPLATE = "view";

    private final JiraLicenseService jiraLicenseService;
    private final BuildUtilsInfo buildUtilsInfo;
    private final PermissionManager permissionManager;
    private final ApplicationProperties applicationProperties;

    private int order;

    public FooterModuleDescriptorImpl(JiraAuthenticationContext authenticationContext, final JiraLicenseService jiraLicenseService, final BuildUtilsInfo buildUtilsInfo, final ModuleFactory moduleFactory, final PermissionManager permissionManager, final
ApplicationProperties applicationProperties)
    {
        super(authenticationContext, moduleFactory);
        this.jiraLicenseService = notNull("jiraLicenseService", jiraLicenseService);
        this.buildUtilsInfo = notNull("buildUtilsInfo", buildUtilsInfo);
        this.permissionManager = permissionManager;
        this.applicationProperties = applicationProperties;
    }

    public void init(Plugin plugin, Element element) throws PluginParseException
    {
        super.init(plugin, element);
        order = ModuleDescriptorXMLUtils.getOrder(element);
    }

    public void enabled()
    {
        super.enabled();
    }

    public int getOrder()
    {
        return order;
    }

    /**
     * This method will setup the params related to the license information and render the html for the footer.
     *
     * @param request the servlet request
     * @param startingParams any parameters that you want to have available in the context when rendering the footer.
     * @return html representing the footer.
     */
    public String getFooterHtml(HttpServletRequest request, Map startingParams)
    {
        Map params = createVelocityParams(request, startingParams);
        return getHtml(VIEW_TEMPLATE, params);
    }

    protected Map createVelocityParams(HttpServletRequest request, Map startingParams)
    {
        Map<String, Object> params = (startingParams != null) ? new HashMap(startingParams) : new HashMap();


        String licenseMessageClass = null;
        boolean longFooterMessage = true;

        final LicenseDetails licenseDetails = jiraLicenseService.getLicense();
        if (!licenseDetails.isLicenseSet() || licenseDetails.isEvaluation() || !licenseDetails.isCommercial() || licenseDetails.hasLicenseTooOldForBuildConfirmationBeenDone())
        {
            params.put("notfull", Boolean.TRUE);

            if (!licenseDetails.isLicenseSet()) // unlicensed
            {
                params.put("unlicensed", Boolean.TRUE);
                licenseMessageClass = "licensemessagered";
            }
            else if (licenseDetails.isEvaluation())
            {
                longFooterMessage = false;
                params.put("evaluation", Boolean.TRUE);
                licenseMessageClass = "licensemessagered";
            }
            else if (licenseDetails.hasLicenseTooOldForBuildConfirmationBeenDone())
            {
                params.put("confirmedWithOldLicense", Boolean.TRUE);
                licenseMessageClass = "licensemessagered";
            }
            else if (licenseDetails.isCommunity())
            {
                longFooterMessage = false;
                params.put("community", Boolean.TRUE);
                licenseMessageClass = "licensemessage";
            }
            else if (licenseDetails.isOpenSource())
            {
                longFooterMessage = false;
                params.put("opensource", Boolean.TRUE);
                licenseMessageClass = "licensemessage";
            }
            else if (licenseDetails.isNonProfit())
            {
                longFooterMessage = false;
                params.put("nonprofit", Boolean.TRUE);
                licenseMessageClass = "licensemessage";
            }
            else if (licenseDetails.isDemonstration())
            {
                params.put("demonstration", Boolean.TRUE);
                licenseMessageClass = "licensemessage";
            }
            else if (licenseDetails.isDeveloper())
            {
                params.put("developer", Boolean.TRUE);
                licenseMessageClass = "licensemessage";
            }
            else if (licenseDetails.isPersonalLicense())
            {
                licenseMessageClass = "licensemessage";
                params.put("personal", licenseDetails.isPersonalLicense());
            }
        }

        if (licenseMessageClass != null)
        {
            params.put("licenseMessageClass", licenseMessageClass);
        }
        params.put("organisation", licenseDetails.getOrganisation());
        params.put("longFooterMessage", longFooterMessage);
        params.put("serverid", jiraLicenseService.getServerId());
        params.put("externalLinkUtil", ExternalLinkUtilImpl.getInstance());
        params.put("utilTimerStack", new UtilTimerStack());
        params.put("version", buildUtilsInfo.getVersion());
        params.put("buildNumber", buildUtilsInfo.getCurrentBuildNumber());
        params.put("req", request);
        params.put("build", buildUtilsInfo);
        params.put("string", new StringUtils());
        params.put("isSysAdmin", permissionManager.hasPermission(SYSTEM_ADMIN, getAuthenticationContext().getLoggedInUser()));
        params.put("showContactAdminForm", applicationProperties.getOption(JIRA_SHOW_CONTACT_ADMINISTRATORS_FORM));

        return params;
    }
}
