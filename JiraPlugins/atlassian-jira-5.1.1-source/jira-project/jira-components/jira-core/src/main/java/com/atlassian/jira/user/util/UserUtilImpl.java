package com.atlassian.jira.user.util;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.crowd.embedded.api.CrowdService;
import com.atlassian.crowd.embedded.api.Directory;
import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.PasswordCredential;
import com.atlassian.crowd.embedded.api.SearchRestriction;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.crowd.embedded.impl.ImmutableUser;
import com.atlassian.crowd.event.DirectoryEvent;
import com.atlassian.crowd.event.directory.RemoteDirectorySynchronisedEvent;
import com.atlassian.crowd.event.migration.XMLRestoreFinishedEvent;
import com.atlassian.crowd.exception.DirectoryNotFoundException;
import com.atlassian.crowd.exception.InvalidCredentialException;
import com.atlassian.crowd.exception.InvalidUserException;
import com.atlassian.crowd.exception.OperationNotPermittedException;
import com.atlassian.crowd.exception.UserAlreadyExistsException;
import com.atlassian.crowd.exception.runtime.OperationFailedException;
import com.atlassian.crowd.manager.directory.DirectoryManager;
import com.atlassian.crowd.manager.directory.DirectoryPermissionException;
import com.atlassian.crowd.model.user.UserTemplate;
import com.atlassian.crowd.search.EntityDescriptor;
import com.atlassian.crowd.search.builder.QueryBuilder;
import com.atlassian.crowd.search.query.entity.EntityQuery;
import com.atlassian.crowd.search.query.entity.UserQuery;
import com.atlassian.crowd.search.query.entity.restriction.NullRestrictionImpl;
import com.atlassian.crowd.util.SecureRandomStringUtils;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.bc.EntityNotFoundException;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.bc.filter.SearchRequestService;
import com.atlassian.jira.bc.license.JiraLicenseService;
import com.atlassian.jira.bc.portal.PortalPageService;
import com.atlassian.jira.bc.project.component.MutableProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponent;
import com.atlassian.jira.bc.project.component.ProjectComponentManager;
import com.atlassian.jira.bc.projectroles.ProjectRoleService;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.event.ClearCacheEvent;
import com.atlassian.jira.event.user.UserEventDispatcher;
import com.atlassian.jira.exception.AddException;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.exception.RemoveException;
import com.atlassian.jira.extension.Startable;
import com.atlassian.jira.issue.comparator.UserBestNameComparator;
import com.atlassian.jira.issue.search.SearchException;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.issue.security.IssueSecurityLevelManager;
import com.atlassian.jira.issue.subscription.SubscriptionManager;
import com.atlassian.jira.issue.vote.VoteManager;
import com.atlassian.jira.issue.watchers.WatcherManager;
import com.atlassian.jira.jql.builder.JqlQueryBuilder;
import com.atlassian.jira.license.LicenseDetails;
import com.atlassian.jira.notification.NotificationSchemeManager;
import com.atlassian.jira.notification.type.SingleUser;
import com.atlassian.jira.plugin.studio.StudioHooks;
import com.atlassian.jira.project.AssigneeTypes;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.GlobalPermissionManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.security.login.LoginManager;
import com.atlassian.jira.security.roles.actor.UserRoleActorFactory;
import com.atlassian.jira.user.UserHistoryManager;
import com.atlassian.jira.util.ComponentLocator;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.SimpleErrorCollection;
import com.atlassian.jira.util.dbc.Assertions;
import com.atlassian.jira.web.bean.I18nBean;
import com.atlassian.query.Query;
import com.atlassian.seraph.spi.rememberme.RememberMeTokenDao;
import com.atlassian.util.concurrent.Function;
import com.atlassian.util.concurrent.Nullable;
import com.atlassian.util.concurrent.ResettableLazyReference;
import com.google.common.collect.Sets;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static com.atlassian.jira.util.dbc.Assertions.notNull;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This is the default implementation of the UserUtil interface.
 */
public class UserUtilImpl implements UserUtil, Startable
{
    private static final Logger log = Logger.getLogger(UserUtilImpl.class);

    private final ComponentLocator componentLocator;
    private final IssueSecurityLevelManager issueSecurityLevelManager;
    private final GlobalPermissionManager globalPermissionManager;
    private final CrowdService crowdService;
    private final DirectoryManager directoryManager;
    private final PermissionManager permissionManager;
    private final ApplicationProperties applicationProperties;
    private final SearchProvider searchProvider;
    private final ProjectManager projectManager;
    private final ProjectRoleService projectRoleService;
    private final ProjectComponentManager componentManager;
    private final SubscriptionManager subscriptionManager;
    private final NotificationSchemeManager notificationSchemeManager;
    private final UserHistoryManager userHistoryManager;
    private final UserManager userManager;
    private final ResettableLazyReference<Integer> activeUsersCount = new ResettableLazyReference<Integer>()
    {
        @Override
        protected Integer create() throws Exception
        {
            final Set<String> groupsWithUsePermission = getGroupsWithUsePermission();
            final Set<User> allUsers = new HashSet<User>();

            for (final String groupName : groupsWithUsePermission)
            {
                Iterable<User> users = getGroupMembers(groupName);
                for (User user : users)
                {
                    if (user.isActive())
                    {
                        allUsers.add(user);
                    }
                }
            }
            return allUsers.size();
        }
    };

    private final EventPublisher eventPublisher;
    private final StudioHooks hooks;

    public UserUtilImpl(final ComponentLocator componentLocator, final IssueSecurityLevelManager issueSecurityLevelManager,
            final GlobalPermissionManager globalPermissionManager, final CrowdService crowdService,
            DirectoryManager directoryManager, final PermissionManager permissionManager, final ApplicationProperties applicationProperties,
            final SearchProvider searchProvider, final ProjectManager projectManager,
            final ProjectRoleService projectRoleService, final ProjectComponentManager componentManager,
            final SubscriptionManager subscriptionManager, final NotificationSchemeManager notificationSchemeManager,
            final UserHistoryManager userHistoryManager, final UserManager userManager, final EventPublisher eventPublisher,
            final StudioHooks hooks)
    {
        this.directoryManager = directoryManager;
        this.eventPublisher = eventPublisher;
        // we use a component locator to break the cyclic dependency within PICO
        this.componentLocator = notNull("componentLocator", componentLocator);
        this.issueSecurityLevelManager = issueSecurityLevelManager;
        this.globalPermissionManager = globalPermissionManager;
        this.permissionManager = permissionManager;
        this.applicationProperties = applicationProperties;
        this.searchProvider = searchProvider;
        this.projectManager = projectManager;
        this.projectRoleService = projectRoleService;
        this.componentManager = componentManager;
        this.subscriptionManager = subscriptionManager;
        this.notificationSchemeManager = notificationSchemeManager;
        this.userHistoryManager = userHistoryManager;
        this.userManager = userManager;

        this.crowdService = crowdService;
        this.hooks = hooks;
    }

    public void start() throws Exception
    {
        eventPublisher.register(this);
    }

    @SuppressWarnings ({ "UnusedDeclaration" })
    @EventListener
    public void onClearCache(final ClearCacheEvent event)
    {
        flushUserCaches();
    }

    @SuppressWarnings ({ "UnusedDeclaration" })
    @EventListener
    public void onDirectoryModified(final DirectoryEvent event)
    {
        activeUsersCount.reset();
    }

    @SuppressWarnings ({ "UnusedDeclaration" })
    @EventListener
    public void onDirectorySynchronisation(final RemoteDirectorySynchronisedEvent event)
    {
        activeUsersCount.reset();
    }

    public void flushUserCaches()
    {
        activeUsersCount.reset();
        // Fire a Crowd Embedded XMLRestoreFinishedEvent to clear Crowd caches.
        eventPublisher.publish(new XMLRestoreFinishedEvent(this));
    }

    /**
     * A Factory method to get the SearchRequestService.  This helps break the cyclic dependency of SearchRequestService
     * to UserUtils AND allows test to override the value used.
     *
     * @return a SearchRequestService
     */
    protected SearchRequestService getSearchRequestService()
    {
        return componentLocator.getComponentInstanceOfType(SearchRequestService.class);
    }

    /**
     * A Factory method to get the PortalPageService.  This helps break the cyclic dependency of PortalPageService to
     * UserUtils AND allows test to override the value used.
     *
     * @return a PortalPageService
     */
    protected PortalPageService getPortalPageService()
    {
        return componentLocator.getComponentInstanceOfType(PortalPageService.class);
    }

    /**
     * Protected level factory method to allow for better test integration
     *
     * @param user the user in action
     * @return a new JiraServiceContext
     */
    protected JiraServiceContext getServiceContext(final User user)
    {
        return new JiraServiceContextImpl(user);
    }

    public User createUserNoNotification(final String username, String password, final String emailAddress, final String displayName)
            throws PermissionException, CreateException
    {
        ImmutableUser.Builder builder = ImmutableUser.newUser().directoryId(-1l)
                .name(username).displayName(displayName).emailAddress(emailAddress).active(true);
        final User user;
        try
        {
            if (StringUtils.isEmpty(password))
            {
                password = generatePassword();
            }
            user = crowdService.addUser(builder.toUser(), password);
        }
        catch (OperationNotPermittedException e)
        {
            throw new PermissionException(e);
        }
        catch (InvalidCredentialException e)
        {
            throw new CreateException(e);
        }
        catch (InvalidUserException e)
        {
            throw new CreateException(e);
        }
        catch (OperationFailedException e)
        {
            throw new CreateException(e);
        }

        // add user to all groups with the 'USE' permission
        addToJiraUsePermission(user);

        return user;
    }

    @Override
    public User createUserNoNotification(String username, String password, String emailAddress, String displayName, Long directoryId)
            throws PermissionException, CreateException
    {
        if (directoryId == null)
        {
            // create in first directory
            return createUserNoNotification(username, password, emailAddress, displayName);
        }

        final UserTemplate userTemplate = new UserTemplate(username, directoryId);
        userTemplate.setEmailAddress(emailAddress);
        userTemplate.setDisplayName(displayName);
        userTemplate.setActive(true);
        // "Empty" password is allowed in UI and means create a User who cannot log in - we just generate a random
        // password as Crowd will not allow blank.
        if (StringUtils.isEmpty(password))
        {
            password = generatePassword();
        }
        try
        {
            User user = directoryManager.addUser(directoryId.longValue(), userTemplate, new PasswordCredential(password));
            // add user to all groups with the 'USE' permission
            addToJiraUsePermission(user);
    
            return user;
        }
        catch (InvalidCredentialException e)
        {
            throw new CreateException(e);
        }
        catch (InvalidUserException e)
        {
            throw new CreateException(e);
        }
        catch (DirectoryPermissionException e)
        {
            throw new PermissionException(e);
        }
        catch (DirectoryNotFoundException e)
        {
            throw new CreateException(e);
        }
        catch (UserAlreadyExistsException e)
        {
            throw new CreateException(e);
        }
        catch (com.atlassian.crowd.exception.OperationFailedException e)
        {
            throw new OperationFailedException(e);
        }
    }

    /**
     * Generates a Random Password that can be used when the user has entered a blank password.
     * <p>
     * The password is guaranteed to contain at least one upper-case letter, lower-case letter and number in case the
     * backend user Directory has password restrictions.
     *
     * @return a random password.
     */
    public static String generatePassword()
    {
        // Crowd requires a password, so we set it randomly
        // and so the user cannot ever log in with it.
        // We append ABab23 so as to pass most password REGEX type tests.
        //JRA-28827 - use secure random rather than random, preserve length (26 characters)
        return SecureRandomStringUtils.getInstance().randomAlphanumericString(26) + "ABab23";
    }

    public User createUserWithNotification(final String username, final String password, final String email, final String fullname, final int userEventType)
            throws PermissionException, CreateException
    {
        return createUserWithNotification(username, password, email, fullname, null, userEventType);
    }

    @Override
    public User createUserWithNotification(String username, String password, String email, String fullname, Long directoryId, int userEventType)
            throws PermissionException, CreateException
    {
        final User user = createUserNoNotification(username, password, email, fullname, directoryId);
        final Directory directory = userManager.getDirectory(user.getDirectoryId());

        final Map eventParams = EasyMap.build("username", username,
                    "email", email,
                    "fullname", fullname,
                    "directoryName", directory.getName());

        if (userManager.canUpdateUserPassword(user))
        {
            final UserUtil.PasswordResetToken passwordResetToken = generatePasswordResetToken(user);
            eventParams.put("password.token", passwordResetToken.getToken());
            eventParams.put("password.hours", passwordResetToken.getExpiryHours());
        }

        dispatchEvent(user, userEventType, eventParams);
        return user;
    }

    protected void dispatchEvent(final User user, final int userEventType, final Map<?, ?> args)
    {
        UserEventDispatcher.dispatchEvent(userEventType, user, args);
    }

    public void removeUser(final User loggedInUser, final User user)
    {
        final I18nHelper i18nBean = getI18nBean(loggedInUser);
        final ErrorCollection errors = new SimpleErrorCollection();

        try
        {
            final String userForDeleteName = user.getName();

            projectRoleService.removeAllRoleActorsByNameAndType(userForDeleteName, UserRoleActorFactory.TYPE);
            permissionManager.removeUserPermissions(userForDeleteName);
            removeWatchesForUser(user);
            removeVotesForUser(user);

            // Delete all the subscriptions for the user as well
            subscriptionManager.deleteSubscriptionsForUser(user);

            // Remove any notifications using this user
            notificationSchemeManager.removeEntities(SingleUser.DESC, user.getName());

            // Remove the user from the component lead of all components lead
            removeComponentLeadsForUser(user, i18nBean, errors);

            // Remove the filters
            getSearchRequestService().deleteAllFiltersForUser(getServiceContext(loggedInUser), user);
            getPortalPageService().deleteAllPortalPagesForUser(user);

            userHistoryManager.removeHistoryForUser(user);

            try
            {
                crowdService.removeUser(user);
            }
            catch (OperationNotPermittedException e)
            {
                throw new PermissionException(e);
            }
        }
        catch (final Exception e)
        {
            log.error("There was an error trying to remove user: " + user.getDisplayName(), e);
            throw new RuntimeException(e);
        }

        clearActiveUserCount();
    }

    public long getNumberOfReportedIssuesIgnoreSecurity(final User loggedInUser, final User user) throws SearchException
    {
        final Query query = JqlQueryBuilder.newBuilder().where().reporterUser(user.getName()).buildQuery();
        return searchProvider.searchCountOverrideSecurity(query, loggedInUser);
    }

    public long getNumberOfAssignedIssuesIgnoreSecurity(final User loggedInUser, final User user) throws SearchException
    {
        final Query query = JqlQueryBuilder.newBuilder().where().assigneeUser(user.getName()).buildQuery();
        return searchProvider.searchCountOverrideSecurity(query, loggedInUser);
    }

    public Collection<ProjectComponent> getComponentsUserLeads(final User user)
    {
        return componentManager.findComponentsByLead(user.getName());
    }

    public Collection<Project> getProjectsLeadBy(final User user)
    {
        return projectManager.getProjectsLeadBy(user);
    }

    public Collection<GenericValue> getProjectsUserLeads(final User user)
    {
        return projectManager.getProjectsByLead(user);
    }

    public boolean isNonSysAdminAttemptingToDeleteSysAdmin(final User loggedInUser, final User user)
    {
        return permissionManager.hasPermission(Permissions.SYSTEM_ADMIN, user) && !permissionManager.hasPermission(Permissions.SYSTEM_ADMIN,
                loggedInUser);
    }

    private void removeVotesForUser(final User userForDelete)
    {
        final VoteManager voteManager = ComponentAccessor.getVoteManager();
        // remove user's votes
        if (applicationProperties.getOption(APKeys.JIRA_OPTION_VOTING))
        {
            voteManager.removeVotesForUser(userForDelete);
        }
    }

    private void removeWatchesForUser(final User userForDelete)
    {
        final WatcherManager watcherManager = ComponentAccessor.getWatcherManager();
        // remove user's watches
        if (applicationProperties.getOption(APKeys.JIRA_OPTION_WATCHING))
        {
            watcherManager.removeAllWatchesForUser(userForDelete);
        }
    }

    private void removeComponentLeadsForUser(final User user, final I18nHelper i18nHelper, final ErrorCollection errorCollection)
    {
        for (final ProjectComponent component : getComponentsUserLeads(user))
        {
            MutableProjectComponent newProjectComponent = MutableProjectComponent.copy(component);
            newProjectComponent.setLead(null);
            if (component.getAssigneeType() == AssigneeTypes.COMPONENT_LEAD)
            {
                newProjectComponent.setAssigneeType(AssigneeTypes.PROJECT_DEFAULT);
            }
            try
            {
                componentManager.update(newProjectComponent);
            }
            catch (EntityNotFoundException e)
            {
                // We only just fetched this component, but perhaps someone deleted it concurrently ...
            }
        }
    }

    public void addUserToGroup(Group group, User userToAdd) throws PermissionException, AddException
    {
            doAddUserToGroup(group, userToAdd);
    }

    void doAddUserToGroup(final Group group, final User userToAdd) throws PermissionException, AddException
    {
        validateParameters(group, userToAdd);
        if (!crowdService.isUserMemberOfGroup(userToAdd, group))
        {
            try
            {
                crowdService.addUserToGroup(userToAdd, group);
            }
            catch (OperationNotPermittedException e)
            {
                throw new PermissionException(e);
            }
            catch (OperationFailedException e)
            {
                throw new AddException(e);
            }
        }
        clearUsersLevels();
        clearActiveUserCount();
    }

    public void addUserToGroups(Collection<Group> groups, User userToAdd) throws PermissionException, AddException
    {
        for (final Group group : groups)
        {
            addUserToGroup(group, userToAdd);
        }
        clearUsersLevels();
    }

    public void removeUserFromGroup(final Group group, final User userToRemove)
            throws PermissionException, RemoveException
    {
        validateParameters(group, userToRemove);
        if (crowdService.isUserDirectGroupMember(userToRemove, group))
        {
            try
            {
                crowdService.removeUserFromGroup(userToRemove, group);
            }
            catch (OperationNotPermittedException e)
            {
                throw new PermissionException(e);
            }
            catch (OperationFailedException e)
            {
                throw new RemoveException(e);
            }
        }
        clearUsersLevels();
        clearActiveUserCount();
    }

    public void removeUserFromGroups(final Collection<Group> groups, final User userToRemove)
            throws PermissionException, RemoveException
    {
        for (final Group group : groups)
        {
            removeUserFromGroup(group, userToRemove);
        }
        clearUsersLevels();
    }

    @Override
    public PasswordResetToken generatePasswordResetToken(final User user)
    {
        return new PasswordResetTokenBuilder(crowdService).generateToken(user);
    }

    public PasswordResetTokenValidation validatePasswordResetToken(final User user, final String token)
    {
        Assertions.notNull("user", user);

        final PasswordResetTokenValidation.Status status = new PasswordResetTokenBuilder(crowdService).validateToken(user, token);
        return new PasswordResetTokenValidation()
        {
            public Status getStatus()
            {
                return status;
            }
        };
    }

    public void changePassword(final User user, final String newPassword) throws PermissionException
    {
        Assertions.notNull("user", user);

        new PasswordResetTokenBuilder(crowdService).resetToken(user);
        
        componentLocator.getComponent(RememberMeTokenDao.class).removeAllForUser(user.getName());
        componentLocator.getComponent(LoginManager.class).resetFailedLoginCount(user);

        try
        {
            crowdService.updateUserCredential(user, newPassword);
        }
        catch (OperationNotPermittedException e)
        {
            throw new PermissionException(e);
        }
        catch (InvalidCredentialException e)
        {
            throw new RuntimeException(e);
        }
    }

    public int getActiveUserCount()
    {
        return activeUsersCount.get();
    }

    public void clearActiveUserCount()
    {
        hooks.getLicenseHooks().clearActiveUserCount(new Function<Void, Void>()
        {
            @Override
            public Void get(Void input)
            {
                activeUsersCount.reset();
                return null;
            }
        });
    }

    public boolean hasExceededUserLimit()
    {
        return hooks.getLicenseHooks().hasExceededUserLimit(new Function<Void, Boolean>()
        {
            @Override
            public Boolean get(Void input)
            {
                return hasExceededUserLimitInternal();
            }
        });
    }

    private boolean hasExceededUserLimitInternal()
    {
        final LicenseDetails licenseDetails = getLicenseDetails();
        if (!licenseDetails.isLicenseSet())
        {
            return false;
        }

        if (!licenseDetails.isUnlimitedNumberOfUsers())
        {
            return getActiveUserCount() > licenseDetails.getMaximumNumberOfUsers();
        }
        return false;
    }

    public boolean canActivateNumberOfUsers(final int numUsers)
    {
        return hooks.getLicenseHooks().canActivateNumberOfUsers(numUsers, new Function<Integer, Boolean>()
        {
            @Override
            public Boolean get(Integer input)
            {
                checkNotNull(input, "numUsers is null.");
                return canActivateNumberOfUsersInternal(input);
            }
        });
    }

    private boolean canActivateNumberOfUsersInternal(int numUsers)
    {
        if (numUsers < 0)
        {
            throw new IllegalArgumentException("numUsers must be non-negative");
        }
        if (numUsers == 0)
        {
            return true;
        }

        final LicenseDetails licenseDetails = getLicenseDetails();
        if (!licenseDetails.isLicenseSet())
        {
            return false;
        }

        if (!licenseDetails.isUnlimitedNumberOfUsers())
        {
            return getActiveUserCount() + numUsers <= licenseDetails.getMaximumNumberOfUsers();
        }
        return true;
    }

    public boolean canActivateUsers(final Collection<String> userNames)
    {
        return hooks.getLicenseHooks().canActivateUsers(userNames, new Function<Collection<String>, Boolean>()
        {
            @Override
            public Boolean get(Collection<String> input)
            {
                return canActivateUsersInternal(input);
            }
        });
    }

    private boolean canActivateUsersInternal(Collection<String> userNames)
    {
        Assertions.notNull("userNames", userNames);

        final LicenseDetails licenseDetails = getLicenseDetails();

        if (!licenseDetails.isLicenseSet())
        {
            return true;
        }

        if (!licenseDetails.isUnlimitedNumberOfUsers())
        {
            final Set<String> groupsWithUsePermission = getGroupsWithUsePermission();
            int numInactiveUsers = 0;

            for (final Object element : userNames)
            {
                final String userName = (String) element;
                // if user is in any group that is a group with use permissions, then they are already active and do

                // not count towards the limit
                Collection<String> groupNames = new ArrayList<String>();
                Iterable<Group> userGroups = getGroupsForUserFromCrowd(userName);
                for (Group group : userGroups)
                {
                    groupNames.add(group.getName());
                }
                if (!CollectionUtils.containsAny(groupNames, groupsWithUsePermission))
                {
                    numInactiveUsers++;
                }
            }

            final int userCount = getActiveUserCount();
            //only if we are trying to add new inactive users and they'll exceed the license limit do we
            //return false.  Otherwise, if we're not activating any new users, or we're under the license limit
            //we just return true.
            if ((numInactiveUsers != 0) && ((userCount + numInactiveUsers) > licenseDetails.getMaximumNumberOfUsers()))
            {
                return false;
            }
        }
        return true;
    }

    public Set<User> getAllUsers()
    {
        return (Set<User>) getUsers();
    }

    @Override
    public Collection<User> getUsers()
    {
        SearchRestriction restriction = NullRestrictionImpl.INSTANCE;
        final com.atlassian.crowd.embedded.api.Query<User> query = new UserQuery<User>(User.class, restriction, 0, -1);
        Iterable<User> crowdUsers = crowdService.search(query);
        if (crowdUsers instanceof Collection<?>)
        {
            return (Collection<User>) crowdUsers;
        }

        HashSet<User> allUsers = new HashSet<User>();
        for (com.atlassian.crowd.embedded.api.User crowdUser : crowdUsers)
        {
            allUsers.add(crowdUser);
        }
        return allUsers;
    }

    public int getTotalUserCount()
    {
        // TODO : Change to use UserManager, which duplicates this code.
        SearchRestriction restriction = NullRestrictionImpl.INSTANCE;
        final com.atlassian.crowd.embedded.api.Query<User> query = new UserQuery<User>(User.class, restriction, 0, -1);
        Iterable<User> crowdUsers = crowdService.search(query);
        // Optimise a little
        if (crowdUsers instanceof Collection)
        {
            return ((Collection) crowdUsers).size();
        }
        int count = 0;
        for (User crowdUser : crowdUsers)
        {
            count++;
        }
        return count;
    }

    private User getUserCwd(final String userName)
    {
        return crowdService.getUser(userName);
    }

    /**
     * Get a User by name.
     * @param userName the name of the user
     *
     * @return a User
     */
    public User getUser(final String userName)
    {
        if (StringUtils.isNotEmpty(userName))
        {
            return getUserCwd(userName);
        }
        return null;
    }

    @Override
    public User getUserObject(String userName)
    {
        return getUser(userName);
    }

    public boolean userExists(final String userName)
    {
        if (StringUtils.isNotEmpty(userName))
        {
            return getUserCwd(userName) != null;
        }
        return false;
    }

    public Collection<User> getAdministrators()
    {
        return getJiraAdministrators();
    }

    @Override
    public Collection<User> getJiraAdministrators()
    {
        return getAllUsersInGroups(globalPermissionManager.getGroupsWithPermission(Permissions.ADMINISTER));
    }

    public Collection<User> getSystemAdministrators()
    {
        return getJiraSystemAdministrators();
    }

    @Override
    public Collection<User> getJiraSystemAdministrators()
    {
        return getAllUsersInGroups(globalPermissionManager.getGroupsWithPermission(Permissions.SYSTEM_ADMIN));
    }

    public void addToJiraUsePermission(final User user)
    {
        // JRA-10393: only add user to USE groups if by doing so we will not exceed the user limit
        if (canActivateNumberOfUsers(1))
        {
            //JRA-22984 Prevent new users from being added to the sytem administrators group
            final Collection<Group> groups = getGroupsWithUsePermissionAndNoAdminsitrativePermissions();
            for (final Group group : groups)
            {
                try
                {
                    doAddUserToGroup(group, user);
                }
                catch (PermissionException e)
                {
                    // Ignore and try the rest (based on pre-Crowd behaviour)
                }
                catch (AddException e)
                {
                    // Ignore and try the rest (based on pre-Crowd behaviour)
                }
            }
        }
    }

    private Collection<Group> getGroupsWithUsePermissionAndNoAdminsitrativePermissions()
    {
        final Collection<Group> useGroups = new ArrayList<Group>(globalPermissionManager.getGroupsWithPermission(Permissions.USE));
        useGroups.removeAll(globalPermissionManager.getGroupsWithPermission(Permissions.ADMINISTER));
        useGroups.removeAll(globalPermissionManager.getGroupsWithPermission(Permissions.SYSTEM_ADMIN));
        return Collections.unmodifiableCollection(useGroups);
    }

    public String getDisplayableNameSafely(final User user)
    {
        if (user == null)
        {
            return null;
        }

        final String fullName = user.getDisplayName();
        if (StringUtils.isNotBlank(fullName))
        {
            return fullName;
        }
        return user.getName();
    }

    public SortedSet<User> getAllUsersInGroups(final Collection<Group> groups)
    {
        return getUsersInGroups(groups);
    }

    public SortedSet<User> getUsersInGroups(final Collection<Group> groups)
    {
        notNull("groups", groups);
        final Collection<String> groupNames = new ArrayList<String>();
        for (final Group group: groups)
        {
            if (group != null)
            {
                groupNames.add(group.getName());
            }
        }
        return getUsersInGroupNames(groupNames);
    }

    public SortedSet<User> getAllUsersInGroupNames(final Collection<String> groupNames)
    {
        Set<User> allUsersUnsorted = getAllUsersInGroupNamesUnsorted(groupNames);

        TreeSet<User> allUsersSorted = Sets.newTreeSet(new UserBestNameComparator());
        allUsersSorted.addAll(allUsersUnsorted);
        return allUsersSorted;
    }

    public Set<User> getAllUsersInGroupNamesUnsorted(Collection<String> groupNames)
    {
        notNull("groupNames", groupNames);
        Set<User> setOfUsers = Sets.newHashSet();
        for (final String groupName : groupNames)
        {
            if (groupName != null)
            {
                Iterable<User> users = getGroupMembers(groupName);
                for (User user : users)
                {
                    setOfUsers.add(user);
                }
            }
        }

        return Collections.unmodifiableSet(setOfUsers);
    }

    public SortedSet<User> getUsersInGroupNames(final Collection<String> groupNames)
    {
        return getAllUsersInGroupNames(groupNames);
    }

    public SortedSet<Group> getGroupsForUser(final String userName)
    {
        notNull("userName", userName);
        final SortedSet<Group> setOfGroups = new TreeSet<Group>();

        Iterable<Group> groups = getGroupsForUserFromCrowd(userName);
        for (Group group : groups)
        {
            setOfGroups.add(group);
        }
        return Collections.unmodifiableSortedSet(setOfGroups);
    }

    public SortedSet<String> getGroupNamesForUser(final String userName)
    {
        notNull("userName", userName);
        final SortedSet<String> setOfGroups = new TreeSet<String>();

        Iterable<String> groups = getGroupNamesForUserFromCrowd(userName);
        for (String groupName : groups)
        {
            setOfGroups.add(groupName);
        }
        return Collections.unmodifiableSortedSet(setOfGroups);
    }

    private Group getGroupCwd(final String groupName)
    {
        return crowdService.getGroup(groupName);
    }

    /**
     * Get a Group by name.
     * @param groupName the name of the group
     *
     * @return   a Group
     */
    public Group getGroup(final String groupName)
    {
        if (StringUtils.isNotEmpty(groupName))
        {
            return getGroupCwd(groupName);
        }
        return null;
    }

    @Override
    public Group getGroupObject(@Nullable String groupName)
    {
        return getGroup(groupName);
    }

    private void validateParameters(final Group group, final User userParam)
    {
        if (group == null)
        {
            throw new DataAccessException("Group must not be null if trying to add or delete a user from it.");
        }
        if (userParam == null)
        {
            throw new DataAccessException("User must not be null if trying to add or delete them from a group.");
        }
    }

    private void clearUsersLevels()
    {
        try
        {
            if (issueSecurityLevelManager != null)
            {
                issueSecurityLevelManager.clearUsersLevels();
            }
        }
        catch (final UnsupportedOperationException uoe)
        {
            log.debug("Unsupported operation was thrown when trying to clear the issue security level manager cache", uoe);
        }
    }

    LicenseDetails getLicenseDetails()
    {
        return componentLocator.getComponentInstanceOfType(JiraLicenseService.class).getLicense();
    }

    Set<String> getGroupsWithUsePermission()
    {
        final Set<String> groupsWithUsePermission = new HashSet<String>();
        for (final Integer permission : Permissions.getUsePermissions())
        {
            groupsWithUsePermission.addAll(globalPermissionManager.getGroupNames(permission));
        }
        return groupsWithUsePermission;
    }

    I18nHelper getI18nBean(final User user)
    {
        return new I18nBean(user);
    }

    private Iterable<User> getGroupMembers(final String groupName)
    {
        final com.atlassian.crowd.search.query.membership.MembershipQuery<User> membershipQuery =
                QueryBuilder.queryFor(User.class, EntityDescriptor.user()).childrenOf(EntityDescriptor.group()).withName(groupName).returningAtMost(EntityQuery.ALL_RESULTS);
        return crowdService.search(membershipQuery);
    }

    private Iterable<Group> getGroupsForUserFromCrowd(final String userName)
    {
        final com.atlassian.crowd.search.query.membership.MembershipQuery<Group> membershipQuery =
                QueryBuilder.queryFor(Group.class, EntityDescriptor.group()).parentsOf(EntityDescriptor.user()).withName(userName).returningAtMost(EntityQuery.ALL_RESULTS);

        return crowdService.search(membershipQuery);
    }

    private Iterable<String> getGroupNamesForUserFromCrowd(final String userName)
    {
        final com.atlassian.crowd.search.query.membership.MembershipQuery<String> membershipQuery =
                QueryBuilder.queryFor(String.class, EntityDescriptor.group()).parentsOf(EntityDescriptor.user()).withName(userName).returningAtMost(EntityQuery.ALL_RESULTS);

        return crowdService.search(membershipQuery);
    }

}
