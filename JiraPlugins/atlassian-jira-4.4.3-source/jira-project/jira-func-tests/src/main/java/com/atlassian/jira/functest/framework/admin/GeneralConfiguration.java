package com.atlassian.jira.functest.framework.admin;

/**
 * Allows you to perform admin actions that happen in the GeneralConfiguration section of the admin section.
 *
 * @since v3.13
 */
public interface GeneralConfiguration
{
    void setAllowUnassignedIssues(boolean enable);

    void setExternalUserManagement(boolean enable);

    void disableVoting();

    void enableVoting();

    void setUserSearchingByFullName(boolean enable);

    void setCommentVisibility(CommentVisibility commentVisibility);

    /**
     * Set the visibility level of user emails.
     *
     * @param emailVisibility one of the predefined visibility levels
     * @see com.atlassian.jira.functest.framework.admin.GeneralConfiguration.EmailVisibility
     */
    void setUserEmailVisibility(EmailVisibility emailVisibility);

    /**
     * @param enable enable or disable JQL Auto-complete globally
     */
    void setJqlAutocomplete(boolean enable);

    /**
     * Set the baseUrl of this JIRA instance to the String provided.
     *
     * @param baseUrl The baseurl to use for this instance
     */
    void setBaseUrl(String baseUrl);

    /**
     * Set the baseUrl to that of the functional base URL.
     */
    void fixBaseUrl();

    /**
     * Sets the default locale of this JIRA instance.
     *
     * @param locale The locale to use by default e.g. <code>"German (Germany)"</code>, <code>"English (UK)"</code>
     */
    void setJiraLocale(String locale);

    /**
     * Disable watching of issues
     */
    void disableWatching();

    /**
     * Enable watching of issues
     */
    void enableWatching();

    /**
     *  Turn on compression of web pages that JIRA sends to the browser.
     */
    void turnOnGZipCompression();

    /**
     * Sets the default user time zone for this JIRA instance. A null time zone means that JIRA will use the default JVM
     * time zone.
     *
     * @param timeZoneID
     */
    void setDefaultUserTimeZone(String timeZoneID);

    /**
     * Options for user email visibility
     *
     * @since v4.1
     */
    enum EmailVisibility
    {
        PUBLIC("show"), HIDDEN("hide"), MASKED("mask"), LOGGED_IN_ONLY("user");

        private final String value;

        EmailVisibility(final String value)
        {
            this.value = value;
        }

        @Override
        public String toString()
        {
            return value;
        }
    }

    enum CommentVisibility
    {
        GROUPS_PROJECT_ROLES("Groups & Project Roles")
                {
                    @Override
                    Boolean getCheckBoxValue()
                    {
                        return true;
                    }
                },
        PROJECT_ROLES("Project Roles only")
                {
                    @Override
                    Boolean getCheckBoxValue()
                    {
                        return false;
                    }

                };

        private final String value;

        CommentVisibility(final String value)
        {
            this.value = value;
        }

        @Override
        public String toString()
        {
            return value;
        }

        abstract Boolean getCheckBoxValue();
    }
}
