package com.atlassian.jira.issue.util;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.plugin.webfragment.SimpleLinkManager;
import com.atlassian.jira.plugin.webfragment.model.JiraHelper;
import com.atlassian.jira.plugin.webfragment.model.SimpleLink;
import com.atlassian.jira.plugin.webfragment.model.SimpleLinkImpl;
import com.atlassian.jira.plugin.webfragment.model.SimpleLinkSection;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.seraph.util.RedirectUtils;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import org.apache.commons.lang.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility class to help with the creation of the View Issues Ops Bar.
 * <p/>
 * Does a lot of caching (only request scope) as we need constantly need to check the sizes of different lists and it is
 * hard to keep track of a lot of these things in the view layer.
 * <p/>
 * Could possibly refactor this in the future to make it more generic to make any Ops bar for any page.  E.g Issue Nav
 *
 * @since v4.1
 */
public class IssueOperationsBarUtil
{
    private static final String VIEW_ISSUE_OPSBAR = "view.issue.opsbar";
    public static final String EDIT_LINK_ID = "edit-issue";

    private List<SimpleLinkSection> groups;
    private Map<String, List<SimpleLinkSection>> sectionsByGroup = new HashMap<String, List<SimpleLinkSection>>();
    private Map<String, List<SimpleLinkSection>> nonEmptySectionsByGroup = new HashMap<String, List<SimpleLinkSection>>();
    private Map<String, List<SimpleLink>> linksBySection = new HashMap<String, List<SimpleLink>>();
    private Map<String, List<SimpleLink>> promoteLinksByGroup = new HashMap<String, List<SimpleLink>>();

    private final JiraHelper helper;
    private final User user;
    private final SimpleLinkManager simpleLinkManager;
    private final ApplicationProperties applicationProperties;
    private final IssueManager issueManager;
    private final I18nHelper i18n;
    private Integer defaultNumberOfLinksToShow;

    public IssueOperationsBarUtil(JiraHelper helper, User user, SimpleLinkManager simpleLinkManager,
            ApplicationProperties applicationProperties, IssueManager issueManager, I18nHelper i18n)
    {
        this.helper = helper;
        this.user = user;
        this.simpleLinkManager = simpleLinkManager;
        this.applicationProperties = applicationProperties;
        this.issueManager = issueManager;
        this.i18n = i18n;
    }


    /**
     * Gets all the groups for the Ops bar - Operations, Transitions, ...
     *
     * @return A list of sections that contain other sections.
     */
    public List<SimpleLinkSection> getGroups()
    {
        if (groups == null)
        {
            groups = simpleLinkManager.getSectionsForLocation(VIEW_ISSUE_OPSBAR, user, helper);
        }
        return groups;
    }

    /**
     * Gets all the links that have been promoted out of the sub-sections, into the top level.
     * <p/>
     * The number of links promoted is determined by the jira-application.properties property: ops.bar.group.size = 2
     * <p/>
     * Though this can be overridden for individual groups: ops.bar.group.size.opsbar-operations = 3
     *
     * @param group The group for which these links belong.  E.g. opbsbar-operations
     * @return A list of links that contain all promoted links
     */
    public List<SimpleLink> getPromotedLinks(SimpleLinkSection group)
    {
        if (!promoteLinksByGroup.containsKey(group.getId()))
        {

            final int numberToShow = getRawNumberOfLinksToShow(group);
            final List<SimpleLink> promotedLinks = new ArrayList<SimpleLink>();
            final List<SimpleLinkSection> sections = getSectionsForGroup(group);

            int conjoinedCount = 0;
            // Go through sub sections and get links
            for (SimpleLinkSection section : sections)
            {
                if (promotedLinks.size() < numberToShow)
                {
                    final List<SimpleLink> links = getLinksForSection(section);

                    for (SimpleLink link : links)
                    {
                        // conjoined items means links that MUST reside next to the previous one.  E.g. if the first gets promoted,
                        // the second MUST be promoted as well.
                        final boolean isConjoined = link.getStyleClass() != null && link.getStyleClass().contains("conjoined");
                        if (promotedLinks.size() - conjoinedCount < numberToShow || (isConjoined && promotedLinks.size() - conjoinedCount <= numberToShow))
                        {
                            if (isConjoined)
                            {
                                conjoinedCount++;
                            }
                            promotedLinks.add(link);
                        }
                    }
                }
            }
            promoteLinksByGroup.put(group.getId(), promotedLinks);

        }

        return promoteLinksByGroup.get(group.getId());

    }

    /**
     * Get all the links for a section that were not promoted.
     *
     * @param group The group which section belongs to.
     * @param section The section to get the links for.
     * @return a list of links for the section minus the promoted links
     */
    public List<SimpleLink> getNonPromotedLinksForSection(SimpleLinkSection group, SimpleLinkSection section)
    {
        final List<SimpleLink> links = getLinksForSection(section);
        final List<SimpleLink> promotedLinks = getPromotedLinks(group);

        links.removeAll(promotedLinks);

        return links;

    }

    /**
     * Get a list of sections that contain links that have not be promoted.
     *
     * @param group The group to get the sections for.
     * @return a list of sections that contain 1 or more links that have not been promoted.
     */
    public List<SimpleLinkSection> getNonEmptySectionsForGroup(SimpleLinkSection group)
    {
        if (!nonEmptySectionsByGroup.containsKey(group.getId()))
        {

            final List<SimpleLinkSection> returnSections = new ArrayList<SimpleLinkSection>();

            final List<SimpleLinkSection> sections = getSectionsForGroup(group);

            for (SimpleLinkSection section : sections)
            {
                final List<SimpleLink> links = getNonPromotedLinksForSection(group, section);
                if (!links.isEmpty())
                {
                    returnSections.add(section);
                }

            }
            nonEmptySectionsByGroup.put(group.getId(), returnSections);
        }
        return nonEmptySectionsByGroup.get(group.getId());

    }


    /**
     * Whether we need to display the more link for a group. We display the more link if the group has a a label or if
     * there are more items to show in a drop down.
     *
     * @param group The group to get the label for.
     * @return true if twe should display a more link, otherwise false.
     */
    public boolean showMoreLinkforGroup(SimpleLinkSection group)
    {
        return group.getLabel() != null || !getNonEmptySectionsForGroup(group).isEmpty();
    }


    private List<SimpleLinkSection> getSectionsForGroup(SimpleLinkSection group)
    {
        if (!sectionsByGroup.containsKey(group.getId()))
        {
            final List<SimpleLinkSection> sections = simpleLinkManager.getSectionsForLocation(group.getId(), user, helper);
            sectionsByGroup.put(group.getId(), sections);
        }

        return sectionsByGroup.get(group.getId());
    }

    private List<SimpleLink> getLinksForSection(SimpleLinkSection section)
    {
        if (!linksBySection.containsKey(section.getId()))
        {
            final List<SimpleLink> sections = simpleLinkManager.getLinksForSection(section.getId(), user, helper);
            final Collection<SimpleLink> filteredSections = Collections2.filter(sections, new Predicate<SimpleLink>()
            {
                @Override
                public boolean apply(@Nullable SimpleLink input)
                {
                    //in the ops bar the edit link is displayed in a separate group from the rest of the
                    //issue operations.
                    return !EDIT_LINK_ID.equals(input.getId());
                }
            });

            linksBySection.put(section.getId(), new ArrayList<SimpleLink>(filteredSections));
        }

        return linksBySection.get(section.getId());
    }

    /*
     * The number of links promoted is determined by the jira-application.properties propterty:
     * ops.bar.group.size = 2
     * <p/>
     * Though this can be overriden for indiviudal groups:
     * ops.bar.group.size.opsbar-operations = 3
     */
    private int getRawNumberOfLinksToShow(final SimpleLinkSection group)
    {
        String numberToShow = applicationProperties.getDefaultBackedString("ops.bar.group.size." + group.getId().toLowerCase());
        if (StringUtils.isNotBlank(numberToShow) && StringUtils.isNumeric(numberToShow))
        {
            final int i = Integer.parseInt(numberToShow);
            if (i > 0)
            {
                return i;
            }
        }
        if (defaultNumberOfLinksToShow == null)
        {
            final String defaultNumberOfLinksToShowStr = applicationProperties.getDefaultBackedString("ops.bar.group.size");
            if (StringUtils.isNotBlank(defaultNumberOfLinksToShowStr) && StringUtils.isNumeric(defaultNumberOfLinksToShowStr))
            {
                final int i = Integer.parseInt(defaultNumberOfLinksToShowStr);
                if (i > 0)
                {
                    defaultNumberOfLinksToShow = i;
                    return defaultNumberOfLinksToShow;
                }
                else
                {
                    // we need to return *something* here, otherwise we'd end up dereferencing a null pointer
                    // when defaultNumberOfLinksToShow gets auto-unboxed
                    return 2;
                }
            }
            else
            {
                defaultNumberOfLinksToShow = 2;
            }
        }

        return defaultNumberOfLinksToShow;
    }

    /**
     * Get a display label for a link Shows a maximum of 25 characters.
     *
     * @param link the link to get the label for.
     * @return the links label, abbreviated to 25 chars
     */
    public String getLabelForLink(SimpleLink link)
    {
        return StringUtils.abbreviate(link.getLabel(), 25);
    }

    /**
     * Get the title for the link. If the link label has been abbreviated and there is no set title, use the full
     * label.
     *
     * @param link The link to get the title for.
     * @return The title for the link
     */
    public String getTitleForLink(SimpleLink link)
    {
        final String label = link.getLabel();
        final String title = link.getTitle();

        if (StringUtils.isBlank(title))
        {
            if (label.length() > 25)
            {
                return label;
            }

            return "";
        }

        return title;

    }

    public SimpleLink getEditOrLoginLink(Issue issue)
    {
        SimpleLink ret = null;
        if (showEdit(issue))
        {
            ret = getEditLink();
        }
        else if (showLogin(issue))
        {
            ret = new SimpleLinkImpl("ops-login-lnk", i18n.getText("common.concepts.login"), i18n.getText("common.concepts.login"), null, null, RedirectUtils.getLinkLoginURL(helper.getRequest()), null);
        }

        return ret;
    }

    private boolean showEdit(final Issue issue)
    {
        return issueManager.isEditable(issue, user);
    }

    private boolean showLogin(final Issue issue)
    {
        if (user != null)
        {
            return false;
        }
        // We are only showing the login when no other buttons are available.
        if (showEdit(issue))
        {
            return false;
        }

        final List<SimpleLinkSection> groups = getGroups();
        for (SimpleLinkSection group : groups)
        {
            final List<SimpleLink> links = getPromotedLinks(group);
            if (!links.isEmpty())
            {
                return false;
            }
        }
        return true;
    }


    private SimpleLink getEditLink()
    {
        final List<SimpleLink> links = simpleLinkManager.getLinksForSection("operations-top-level", user, helper);
        for (SimpleLink link : links)
        {
            if (EDIT_LINK_ID.equals(link.getId()))
            {
                return link;
            }
        }
        return null;
    }
}
