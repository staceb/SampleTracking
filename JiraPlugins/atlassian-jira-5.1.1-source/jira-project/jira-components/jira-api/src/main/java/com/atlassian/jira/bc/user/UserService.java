package com.atlassian.jira.bc.user;

import com.atlassian.annotations.PublicApi;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.ServiceResultImpl;
import com.atlassian.jira.exception.CreateException;
import com.atlassian.jira.exception.PermissionException;
import com.atlassian.jira.util.ErrorCollection;
import com.atlassian.jira.util.SimpleErrorCollection;

import javax.annotation.Nullable;

/**
 * UserService provides User manipulation methods exposed for remote API and actions.
 *
 * @since v4.0
 */
@PublicApi
public interface UserService
{
    /**
     * Validates creating a user during public signup.
     * This method checks that there is a writable User Directory available.
     * It also validates that all parameters (username, email, fullname, password) have been provided.
     * Email is also checked to ensure that it is a valid email address.  The username is required to be
     * lowercase characters only and unique. The confirmPassword has to match the password provided.
     * <p/>
     * This validation differs from the 'ForAdminPasswordRequired' and 'ForAdmin' validations as follows: <ul> <li>Does
     * not require global admin rights</li> <li>The password is required</li> <ul>
     *
     * @param loggedInUser The remote user trying to add a new user
     * @param username The username of the new user. Needs to be lowercase and unique.
     * @param password The password for the new user.
     * @param confirmPassword The password confirmation.  Needs to match password.
     * @param email The email for the new user.  Needs to be a valid email address.
     * @param fullname The full name for the new user
     * @return a validation result containing appropriate errors or the new user's details
     * @since 5.0
     */
    CreateUserValidationResult validateCreateUserForSignup(User loggedInUser, String username, String password,
            String confirmPassword, String email, String fullname);

    /**
     * Validates creating a user during setup of JIRA.
     * This method does not check for a writable User Directory because the embedded crowd subsystem will not be
     * initialised, and we know we will just have an Internal Directory during Setup.
     * It also validates that all parameters (username, email, fullname, password) have been
     * provided.  Email is also checked to ensure that it is a valid email address.  The username is required to be
     * lowercase characters only and unique. The confirmPassword has to match the password provided.
     * <p/>
     * This validation differs from the 'ForAdminPasswordRequired' and 'ForAdmin' validations as follows: <ul> <li>Does
     * not require global admin rights</li> <li>The password is required</li> <ul>
     *
     * @param loggedInUser The remote user trying to add a new user
     * @param username The username of the new user. Needs to be lowercase and unique.
     * @param password The password for the new user.
     * @param confirmPassword The password confirmation.  Needs to match password.
     * @param email The email for the new user.  Needs to be a valid email address.
     * @param fullname The full name for the new user
     * @return a validation result containing appropriate errors or the new user's details
     * @since 5.0
     */
    CreateUserValidationResult validateCreateUserForSetup(User loggedInUser, String username, String password,
            String confirmPassword, String email, String fullname);

    /**
     * Validates creating a user during setup of JIRA or during public signup.
     * This method checks that there is a writable User Directory available.
     * It also validates that all parameters (username, email, fullname, password) have been
     * provided.  Email is also checked to ensure that it is a valid email address.  The username is required to be
     * lowercase characters only and unique. The confirmPassword has to match the password provided.
     * <p/>
     * This validation differs from the 'ForAdminPasswordRequired' and 'ForAdmin' validations as follows: <ul> <li>Does
     * not require global admin rights</li> <li>The password is required</li> <ul>
     *
     * @param user The remote user trying to add a new user
     * @param username The username of the new user. Needs to be lowercase and unique.
     * @param password The password for the new user.
     * @param confirmPassword The password confirmation.  Needs to match password.
     * @param email The email for the new user.  Needs to be a valid email address.
     * @param fullname The full name for the new user
     * @return a validation result containing appropriate errors or the new user's details
     * @since 4.0
     *
     * @deprecated Use {@link #validateCreateUserForSignup(com.atlassian.crowd.embedded.api.User, String, String, String, String, String)} or
     * {@link #validateCreateUserForSetup(com.atlassian.crowd.embedded.api.User, String, String, String, String, String)} instead. Since v5.0.
     */
    CreateUserValidationResult validateCreateUserForSignupOrSetup(User user, String username, String password,
            String confirmPassword, String email, String fullname);

    /**
     * Validates creating a user for RPC calls.  This method checks that external user management is disabled and that
     * the user performing the operation has global admin rights.  It also validates that all parameters (username,
     * email, fullname, password) have been provided.  Email is also checked to ensure that it is a valid email address.
     * The username is required to be lowercase characters only and unique. The confirmPassword has to match the
     * password provided.
     * <p/>
     * This validation differs from the 'ForSetup' and 'ForAdmin' validations as follows: <ul> <li>Does require global
     * admin rights</li> <li>The password is required</li> <ul>
     *
     * @param user The remote user trying to add a new user
     * @param username The username of the new user. Needs to be lowercase and unique.
     * @param password The password for the new user.
     * @param confirmPassword The password confirmation.  Needs to match password.
     * @param email The email for the new user.  Needs to be a valid email address.
     * @param fullname The full name for the new user
     * @return a validation result containing appropriate errors or the new user's details
     * @since 4.0
     */
    CreateUserValidationResult validateCreateUserForAdminPasswordRequired(User user, String username, String password, String confirmPassword,
            String email, String fullname);

    /**
     * Validates creating a user for the admin section.  This method checks that external user management is disabled
     * and that the user performing the operation has global admin rights.  It also validates that all parameters
     * (username, email, fullname) except for the password have been provided.  Email is also checked to ensure that it
     * is a valid email address.  The username is required to be lowercase characters only and unique. The
     * confirmPassword has to match the password provided.
     * <p/>
     * This validation differs from the 'ForSetup' and 'ForAdminPasswordRequired' validations as follows: <ul> <li>Does
     * require global admin rights</li> <li>The password is NOT required</li> </ul>
     *
     * @param user The remote user trying to add a new user
     * @param username The username of the new user. Needs to be lowercase and unique.
     * @param password The password for the new user.
     * @param confirmPassword The password confirmation.  Needs to match password.
     * @param email The email for the new user.  Needs to be a valid email address.
     * @param fullname The full name for the new user
     * @return a validation result containing appropriate errors or the new user's details
     * @since 4.3
     */
    CreateUserValidationResult validateCreateUserForAdmin(User user, String username, String password, String confirmPassword,
            String email, String fullname);

    /**
     * Validates creating a user for the admin section.  This method checks that external user management is disabled
     * and that the user performing the operation has global admin rights.  It also validates that all parameters
     * (username, email, fullname) except for the password have been provided.  Email is also checked to ensure that it
     * is a valid email address.  The username is required to be lowercase characters only and unique. The
     * confirmPassword has to match the password provided.
     * <p/>
     * This method allows the caller to name a directory to create the user in and the directoryId must be valid and
     * represent a Directory with "create user" permission.
     * <p/>
     * This validation differs from the 'ForSetup' and 'ForAdminPasswordRequired' validations as follows: <ul> <li>Does
     * require global admin rights</li> <li>The password is NOT required</li> </ul>
     *
     * @param user The remote user trying to add a new user
     * @param username The username of the new user. Needs to be lowercase and unique.
     * @param password The password for the new user.
     * @param confirmPassword The password confirmation.  Needs to match password.
     * @param email The email for the new user.  Needs to be a valid email address.
     * @param fullname The full name for the new user
     * @param directoryId The User Directory
     * @return a validation result containing appropriate errors or the new user's details
     * @since 4.3.2
     */
    CreateUserValidationResult validateCreateUserForAdmin(User user, String username, String password, String confirmPassword,
            String email, String fullname, @Nullable Long directoryId);

    /**
     * Validates the username for a new user. The username is required to be lowercase characters only and unique across
     * all directories.
     *
     * @param loggedInUser The remote user trying to add a new user
     * @param username The username of the new user. Needs to be lowercase and unique.
     * @return a validation result containing appropriate errors
     * @since 5.0.4
     */
    CreateUsernameValidationResult validateCreateUsername(User loggedInUser, String username);

    /**
     * Validates the username for a new user. The username is required to be lowercase characters only and unique in the
     * given directory.
     *
     * @param loggedInUser The remote user trying to add a new user
     * @param username The username of the new user. Needs to be lowercase and unique.
     * @param directoryId The directory which the new user is intended to be created in.
     * @return a validation result containing appropriate errors
     * @since 5.0.4
     */
    CreateUsernameValidationResult validateCreateUsername(User loggedInUser, String username, Long directoryId);

    /**
     * Given a valid validation result, this will create the user using the details provided in the validation result.
     * Email notification will be send to created user - via UserEventType.USER_SIGNUP event.
     *
     * @param result The validation result
     * @return The new user object that was created
     * @since 4.3
     * @throws CreateException if the user could not be created, eg username already exists
     * @throws PermissionException If you cannot create users in this directory (it is read-only).
     */
    public User createUserFromSignup(final CreateUserValidationResult result)
            throws PermissionException, CreateException;

    /**
     * Given a valid validation result, this will create the user using the details provided in the validation result.
     * Email notification will be send to created user - via UserEventType.USER_CREATED event.
     *
     * @param result The validation result
     * @return The new user object that was created
     * @since 4.3
     * @throws CreateException if the user could not be created, eg username already exists
     * @throws PermissionException If you cannot create users in this directory (it is read-only).
     */
    User createUserWithNotification(CreateUserValidationResult result)
            throws PermissionException, CreateException;

    /**
     * Given a valid validation result, this will create the user using the details provided in the validation result.
     * No email notification will be send to created user.
     *
     * @param result The validation result
     * @return The new user object that was created
     * @since 4.3
     * @throws CreateException if the user could not be created, eg username already exists
     * @throws PermissionException If you cannot create users in this directory (it is read-only).
     */
    User createUserNoNotification(CreateUserValidationResult result)
            throws PermissionException, CreateException;

    /**
     * Validates updating a user's details.
     *
     * @param user The user details to update
     * @return A validation result containing any errors and all user details
     */
    UpdateUserValidationResult validateUpdateUser(User user);

    /**
     * Execute the update using the validation result from {@link #validateUpdateUser(User)}.
     *
     * @param updateUserValidationResult Result from the validation, which also contains all the user's details.
     * @throws IllegalStateException if the validation result contains any errors.
     */
    void updateUser(UpdateUserValidationResult updateUserValidationResult);

    /**
     * Validates removing a user for the admin section.  This method checks that external user management is disabled
     * and that the user performing the operation has global admin rights.  It also validates that username have been
     * provided.
     * <p>
     * Removing the user is not allowed if: <ul> <li>User is trying to remove himself</li> <li>User has any
     * issue assigned</li> <li>User reported any issue</li> <li>User is any project lead</li> <li>User performing
     * operation does not have SYSTEM_ADMIN rights and is trying to remove user having them </li> </ul>
     * <p>
     *
     * @param loggedInUser The remote user trying to remove a user
     * @param username The username of the user to remove. Needs to be valid
     * @return a validation result containing appropriate errors or the user object for delete
     * @since 4.0
     */
    DeleteUserValidationResult validateDeleteUser(final User loggedInUser, final String username);

    /**
     * Given a valid validation result, this will remove the user and removes the user from all the groups. All
     * components lead by user will have lead cleared.
     *
     * @param loggedInUser the user performing operation
     * @param result The validation result
     */
    void removeUser(User loggedInUser, final DeleteUserValidationResult result);

    @PublicApi
    static final class CreateUserValidationResult extends ServiceResultImpl
    {
        private final String username;
        private final String password;
        private final String email;
        private final String fullname;
        private final Long directoryId;

        CreateUserValidationResult(ErrorCollection errorCollection)
        {
            super(errorCollection);
            username = null;
            password = null;
            email = null;
            fullname = null;
            this.directoryId = null;
        }

        CreateUserValidationResult(final String username,
                final String password, final String email, final String fullname)
        {
            super(new SimpleErrorCollection());
            this.username = username;
            this.password = password;
            this.email = email;
            this.fullname = fullname;
            this.directoryId = null;
        }

        CreateUserValidationResult(final String username,
                final String password, final String email, final String fullname, Long directoryId)
        {
            super(new SimpleErrorCollection());
            this.username = username;
            this.password = password;
            this.email = email;
            this.fullname = fullname;
            this.directoryId = directoryId;
        }

        public String getUsername()
        {
            return username;
        }

        public String getPassword()
        {
            return password;
        }

        public String getEmail()
        {
            return email;
        }

        public String getFullname()
        {
            return fullname;
        }

        public Long getDirectoryId()
        {
            return directoryId;
        }
    }

    @PublicApi
    static final class CreateUsernameValidationResult extends ServiceResultImpl
    {
        private final String username;
        private final Long directoryId;

        public CreateUsernameValidationResult(final String username, final Long directoryId, final ErrorCollection errorCollection)
        {
            super(errorCollection);
            this.username = username;
            this.directoryId = directoryId;
        }

        public String getUsername()
        {
            return username;
        }

        public Long getDirectoryId()
        {
            return directoryId;
        }
    }

    @PublicApi
    static final class UpdateUserValidationResult extends ServiceResultImpl
    {
        private final User user;

        UpdateUserValidationResult(ErrorCollection errorCollection)
        {
            super(errorCollection);
            user = null;
        }

        UpdateUserValidationResult(final User user)
        {
            super(new SimpleErrorCollection());
            this.user = user;

        }

        public User getUser()
        {
            return user;
        }
    }

    @PublicApi
    static final class DeleteUserValidationResult extends ServiceResultImpl
    {
        private final User user;

        DeleteUserValidationResult(ErrorCollection errorCollection)
        {
            super(errorCollection);
            user = null;
        }

        DeleteUserValidationResult(final User user)
        {
            super(new SimpleErrorCollection());
            this.user = user;

        }

        public User getUser()
        {
            return user;
        }
    }

    @PublicApi
    static final class FieldName
    {
        private FieldName()
        {
        }

        /**
         * The default name of HTML fields containing a User's email. Validation methods on this service will return an
         * {@link com.atlassian.jira.util.ErrorCollection} with error messages keyed to this field name.
         */
        static String EMAIL = "email";
        /**
         * The default name of HTML fields containing a User's username. Validation methods on this service will return
         * an {@link com.atlassian.jira.util.ErrorCollection} with error messages keyed to this field name.
         */
        static String NAME = "username";
        /**
         * The default name of HTML fields containing a User's full name. Validation methods on this service will return
         * an {@link com.atlassian.jira.util.ErrorCollection} with error messages keyed to this field name.
         */
        static String FULLNAME = "fullname";
        /**
         * The default name of HTML fields containing a User's password. Validation methods on this service will return
         * an {@link com.atlassian.jira.util.ErrorCollection} with error messages keyed to this field name.
         */
        static String PASSWORD = "password";
        /**
         * The default name of HTML fields containing a User's password confirmation. Validation methods on this service
         * will return an {@link com.atlassian.jira.util.ErrorCollection} with error messages keyed to this field name.
         */
        static String CONFIRM_PASSWORD = "confirm";
    }    
}
