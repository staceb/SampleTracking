package com.atlassian.jira.dashboard.permission;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.gadgets.GadgetState;
import com.atlassian.gadgets.Vote;
import com.atlassian.gadgets.dashboard.DashboardState;
import com.atlassian.gadgets.dashboard.spi.DashboardPermissionService;
import com.atlassian.gadgets.plugins.PluginGadgetSpec;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.plugin.ModuleDescriptor;
import com.atlassian.plugin.PluginAccessor;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.atlassian.jira.util.dbc.Assertions.notNull;

public class JiraGadgetPermissionManager implements GadgetPermissionManager
{
    private static final Logger log = Logger.getLogger(JiraGadgetPermissionManager.class);

    private static final String LOGIN_GADGET_PLUGIN_KEY = "com.atlassian.jira.gadgets:login-gadget";
    //match two groups in a string starting with anything, then 'rest/gadgets/' then anything then '/g/' then match all except '/' then ':' then match all except '/' then anything.
    private static final Pattern PLUGIN_KEY_PATTERN = Pattern.compile(".*rest\\/gadgets\\/.*\\/g\\/([^\\/]+):([^\\/]+).*", Pattern.CASE_INSENSITIVE);

    private final PermissionManager permissionManager;
    private final PluginAccessor pluginAccessor;
    private final DashboardPermissionService permissionService;

    public JiraGadgetPermissionManager(final PermissionManager permissionManager,
            final PluginAccessor pluginAccessor, final DashboardPermissionService permissionService)
    {
        this.permissionManager = permissionManager;
        this.pluginAccessor = pluginAccessor;
        this.permissionService = permissionService;
    }

    public Vote voteOn(final PluginGadgetSpec pluginGadgetSpec, final User remoteUser)
    {
        notNull("pluginGadgetSpec", pluginGadgetSpec);

        final String completeGadgetKey = pluginGadgetSpec.getPluginKey() + ":" + pluginGadgetSpec.getModuleKey();
        return voteOn(completeGadgetKey, remoteUser);
    }

    public DashboardState filterGadgets(final DashboardState dashboardState, final User remoteUser)
    {
        notNull("dashboardState", dashboardState);

        boolean isWritable = permissionService.isWritableBy(dashboardState.getId(), remoteUser == null ? null : remoteUser.getName());
        if (isWritable)
        {
            return dashboardState;
        }

        //read only dashboard.  Remove any gadgets user doesn't have permission to see.
        final List<List<GadgetState>> columns = new ArrayList<List<GadgetState>>();
        for (Iterable<GadgetState> columnIterable : dashboardState.getColumns())
        {
            final List<GadgetState> column = new ArrayList<GadgetState>();
            for (GadgetState state : columnIterable)
            {
                final String gadgetKey = extractModuleKey(state.getGadgetSpecUri().toASCIIString());
                if (gadgetKey != null)
                {
                    final Vote vote = voteOn(gadgetKey, remoteUser);
                    if (vote.equals(Vote.DENY))
                    {
                        //skip this gadget.
                        continue;
                    }
                }
                column.add(state);
            }
            columns.add(column);
        }
        return DashboardState.dashboard(dashboardState).columns(columns).build();
    }

    public String extractModuleKey(final String gadgetUri)
    {
        final Matcher matcher = PLUGIN_KEY_PATTERN.matcher(gadgetUri);
        if (matcher.matches() && matcher.groupCount() == 2)
        {
            return matcher.group(1) + ":" + matcher.group(2);
        }
        return null;
    }

    public Vote voteOn(final String completeGadgetKey, final User remoteUser)
    {
        //special case for the login gadget.  It should be hidden for logged in users and shown for logged out user.
        if (completeGadgetKey.equals(LOGIN_GADGET_PLUGIN_KEY))
        {
            if (remoteUser != null)
            {
                return Vote.DENY;
            }
            else
            {
                return Vote.ALLOW;
            }
        }

        final ModuleDescriptor<?> moduleDescriptor = pluginAccessor.getEnabledPluginModule(completeGadgetKey);
        if (moduleDescriptor == null)
        {
            //If the gadget can't be found in our plugins allow it.  Might be an external gadget that happens to have
            //a url with a plugin key.
            return Vote.ALLOW;
        }

        final String roleString = moduleDescriptor.getParams().get("roles-required");
        if (StringUtils.isBlank(roleString))
        {
            return Vote.ALLOW;
        }

        //admins get to see all gadgets, so that they'll show up in the 'Default Dashboard' section in the admin section
        if (permissionManager.hasPermission(Permissions.ADMINISTER, remoteUser))
        {
            return Vote.ALLOW;
        }

        final String[] roles = StringUtils.split(roleString);
        for (String role : roles)
        {
            final int permission = Permissions.getType(role);
            if (permission == -1)
            {
                log.warn("Invalid role-required specified for gadget '" + completeGadgetKey + "': '" + role + "'");
                return Vote.PASS;
            }
            if (Permissions.isGlobalPermission(permission))
            {
                if (!permissionManager.hasPermission(permission, remoteUser))
                {
                    return Vote.DENY;
                }
            }
            else
            {
                if (!hasProjectsPermission(permission, remoteUser))
                {
                    return Vote.DENY;
                }
            }
        }
        return Vote.ALLOW;
    }

    private boolean hasProjectsPermission(int permission, User user)
    {
        try
        {
            return permissionManager.hasProjects(permission, user);
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}
