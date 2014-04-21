package com.atlassian.jira.issue.comments;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;

/**
 * A unified way of checking whether a user can see a comment or not
 */
public interface CommentPermissionManager
{
    /**
     * Return true if the user can see the comment.  It does not check if the user
     * has the permission to see the issue the comment is attached to however.
     *
     * @param comment comment
     * @param user    user
     * @return true if permission is granted, false otherwise
     *
     * @deprecated Use {@link #hasBrowsePermission(com.atlassian.crowd.embedded.api.User, Comment)}. Since v4.3
     */
    public boolean hasBrowsePermission(Comment comment, com.opensymphony.user.User user);

    /**
     * Return true if the user can see the comment.  It does not check if the user
     * has the permission to see the issue the comment is attached to however.
     *
     * @param user    user
     * @param comment comment
     * @return true if permission is granted, false otherwise
     */
    public boolean hasBrowsePermission(User user, Comment comment);

    /**
     * Determines whether the given user has permission to edit the given comment. The user is granted permission if
     * they have the {@link com.atlassian.jira.security.Permissions#COMMENT_EDIT_ALL} permission or the
     * {@link com.atlassian.jira.security.Permissions#COMMENT_EDIT_OWN} permission in case the user is the author of the
     * given comment.
     * <p/>
     * NOTE: The permissions will be determined by the permission scheme associated to the project the comment is
     * a part of and the entries for the above mentioned permissions.
     * <p/>
     * NOTE: This method does not check whether the Issue the comment belongs to is in an editable workflow state.
     *
     * @param comment comment
     * @param user    user
     * @return true if permission is granted, false otherwise
     * @deprecated Use {@link #hasEditPermission(com.atlassian.crowd.embedded.api.User, Comment)}. Since v4.3
     */
    public boolean hasEditPermission(Comment comment, com.opensymphony.user.User user);

    /**
     * Determines whether the given user has permission to edit the given comment. The user is granted permission if
     * they have the {@link com.atlassian.jira.security.Permissions#COMMENT_EDIT_ALL} permission or the
     * {@link com.atlassian.jira.security.Permissions#COMMENT_EDIT_OWN} permission in case the user is the author of the
     * given comment.
     * <p/>
     * NOTE: The permissions will be determined by the permission scheme associated to the project the comment is
     * a part of and the entries for the above mentioned permissions.
     * <p/>
     * NOTE: This method does not check whether the Issue the comment belongs to is in an editable workflow state.
     *
     * @param user    user
     * @param comment comment
     * @return true if permission is granted, false otherwise
     */
    public boolean hasEditPermission(User user, Comment comment);

    /**
     * Returns true if user has permission to edit all comments in the given Issue.
     * <p/>
     * NOTE: This method does not check whether the Issue the comment belongs to is in an editable workflow state.
     *
     * @param user  User. A null value represents an anonymous User.
     * @param issue Issue. Must not be null.
     * @return true if user has permission to edit all comments in the given Issue.
     */
    public boolean hasEditAllPermission(com.opensymphony.user.User user, Issue issue);

    /**
     * Returns true if user has permission to edit all comments in the given Issue.
     * <p/>
     * NOTE: This method does not check whether the Issue the comment belongs to is in an editable workflow state.
     *
     * @param user  User. A null value represents an anonymous User.
     * @param issue Issue. Must not be null.
     * @return true if user has permission to edit all comments in the given Issue.
     */
    public boolean hasEditAllPermission(User user, Issue issue);

    /**
     * Returns true if user has permission to edit her own comments in the given Issue.
     * <p/>
     * NOTE: This method does not check whether the Issue the comment belongs to is in an editable workflow state.
     *
     * @param user  User. A null value represents an anonymous User.
     * @param issue Issue. Must not be null.
     * @return true if user has permission to edit all comments in the given Issue.
     */
    public boolean hasEditOwnPermission(com.opensymphony.user.User user, Issue issue);

    /**
     * Returns true if user has permission to edit her own comments in the given Issue.
     * <p/>
     * NOTE: This method does not check whether the Issue the comment belongs to is in an editable workflow state.
     *
     * @param user  User. A null value represents an anonymous User.
     * @param issue Issue. Must not be null.
     * @return true if user has permission to edit all comments in the given Issue.
     */
    public boolean hasEditOwnPermission(User user, Issue issue);

    /**
     * Returns true if the given User can delete all comments for the given Issue.
     * <p/>
     * NOTE: This method does not check whether the Issue the comment belongs to is in an editable workflow state.
     *
     * @param user  User. A null value represents an anonymous User.
     * @param issue Issue. Must not be null.
     * @return true if user has permission to delete all comments in the given Issue.
     */
    boolean hasDeleteAllPermission(final com.opensymphony.user.User user, final Issue issue);

    /**
     * Returns true if the given User can delete all comments for the given Issue.
     * <p/>
     * NOTE: This method does not check whether the Issue the comment belongs to is in an editable workflow state.
     *
     * @param user  User. A null value represents an anonymous User.
     * @param issue Issue. Must not be null.
     * @return true if user has permission to delete all comments in the given Issue.
     */
    boolean hasDeleteAllPermission(final User user, final Issue issue);

    /**
     * Returns true if the given User can delete her own comments in the given Issue.
     * <p/>
     * NOTE: This method does not check whether the Issue the comment belongs to is in an editable workflow state.
     *
     * @param user  User. A null value represents an anonymous User.
     * @param issue Issue. Must not be null.
     * @return true if the given User can delete her own comments in the given Issue.
     */
    boolean hasDeleteOwnPermission(final com.opensymphony.user.User user, final Issue issue);

    /**
     * Returns true if the given User can delete her own comments in the given Issue.
     * <p/>
     * NOTE: This method does not check whether the Issue the comment belongs to is in an editable workflow state.
     *
     * @param user  User. A null value represents an anonymous User.
     * @param issue Issue. Must not be null.
     * @return true if the given User can delete her own comments in the given Issue.
     */
    boolean hasDeleteOwnPermission(final User user, final Issue issue);

    /**
     * Returns true if the given user is the author of the given comment.
     * <p>
     * If the given Comment has a null author, this represents that it was created anonymously, and no-one is the author (returns false).
     * If the given User is null, then this represents that the current user is anonymous, and the method will always return false.
     * </p>
     *
     * @param user The User. Can be null to represent current user is not logged in (anonymous).
     * @param comment The Comment. Cannot be null.
     * @return true if the given user is the author of the given comment.
     */
    public boolean isUserCommentAuthor(com.opensymphony.user.User user, Comment comment);

    /**
     * Returns true if the given user is the author of the given comment.
     * <p>
     * If the given Comment has a null author, this represents that it was created anonymously, and no-one is the author (returns false).
     * If the given User is null, then this represents that the current user is anonymous, and the method will always return false.
     * </p>
     *
     * @param user The User. Can be null to represent current user is not logged in (anonymous).
     * @param comment The Comment. Cannot be null.
     * @return true if the given user is the author of the given comment.
     */
    public boolean isUserCommentAuthor(User user, Comment comment);
}
