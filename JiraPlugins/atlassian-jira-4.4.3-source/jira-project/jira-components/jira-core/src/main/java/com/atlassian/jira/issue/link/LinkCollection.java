package com.atlassian.jira.issue.link;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.ManagerFactory;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.comparator.IssueConstantsComparator;
import com.atlassian.jira.issue.comparator.IssueKeyComparator;
import com.atlassian.jira.issue.comparator.IssueStatusComparator;
import com.atlassian.jira.issue.comparator.IssueTypeComparator;
import com.atlassian.jira.issue.comparator.PriorityComparator;
import com.atlassian.jira.issue.comparator.ResolutionComparator;
import com.atlassian.jira.security.Permissions;
import com.google.common.collect.ImmutableList;
import org.apache.commons.collections.ComparatorUtils;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * This class represents the collection of links in to and out of a particular issue.
 */
public class LinkCollection
{
    private static final Logger log = Logger.getLogger(LinkCollection.class);

    private static final IssueKeyComparator KEY_COMPARATOR = new IssueKeyComparator();
    private static final IssueTypeComparator TYPE_COMPARATOR = new IssueTypeComparator();
    private static final IssueStatusComparator STATUS_COMPARATOR = new IssueStatusComparator();
    private static final PriorityComparator PRIORITY_COMPARATOR = new PriorityComparator();
    private static final ResolutionComparator RESOLUTION_COMPARATOR = new ResolutionComparator();
    private static final List<IssueConstantsComparator> DEFAULT_SORT_ORDER = ImmutableList.of(TYPE_COMPARATOR,
            STATUS_COMPARATOR, PRIORITY_COMPARATOR);
    private static final String DEFAULT_SORT_ORDER_STRING = "type, status, priority";

    final Set<IssueLinkType> linkTypes;
    final Map<String, List<Issue>> outwardLinkMap;
    final Map<String, List<Issue>> inwardLinkMap;
    final User remoteUser;
    final boolean overrideSecurity;
    private final Comparator sortOrder;
    private String thisIssueId;

    /**
     * Provides the allowable fields that can be used to specify link sort order.
     *
     * @return the list of allowable fields.
     */
    public static Collection<String> getSortableFields()
    {
        return Arrays.asList("type", "status", "priority", "key", "resolution");
    }

    /**
     * Creates a new instance of this class. Sort order can be specified in the application properties under {@link
     * APKeys#JIRA_VIEW_ISSUE_LINKS_SORT_ORDER} key.
     *
     * @param issueId issue generic value this link collection relates to
     * @param linkTypes a set of link types
     * @param outwardLinkMap a map of outward links
     * @param inwardLinkMap a map of inward links
     * @param remoteUser user, used to filter the linked issues
     * @param overrideSecurity false if permissions should be checked when retrieving links
     * @param applicationProperties application properties
     * @since v4.4
     */
    public LinkCollection(final Long issueId, final Set<IssueLinkType> linkTypes, final Map<String, List<Issue>> outwardLinkMap, final Map<String, List<Issue>> inwardLinkMap, final User remoteUser, final boolean overrideSecurity, final ApplicationProperties applicationProperties)
    {
        this.linkTypes = linkTypes;
        this.outwardLinkMap = outwardLinkMap;
        this.inwardLinkMap = inwardLinkMap;
        this.remoteUser = remoteUser;
        this.overrideSecurity = overrideSecurity;
        this.thisIssueId = issueId == null ? "" : "[id=" + issueId + "]";

        String sortOrderStr = applicationProperties.getDefaultBackedString(APKeys.JIRA_VIEW_ISSUE_LINKS_SORT_ORDER);
        List sortOrder = new ArrayList();
        if (sortOrderStr != null)
        {
            StringTokenizer st = new StringTokenizer(sortOrderStr, ", ");
            while (st.hasMoreTokens())
            {
                String field = st.nextToken();
                if ("type".equals(field))
                {
                    sortOrder.add(TYPE_COMPARATOR);
                }
                else if ("status".equals(field))
                {
                    sortOrder.add(STATUS_COMPARATOR);
                }
                else if ("priority".equals(field))
                {
                    sortOrder.add(PRIORITY_COMPARATOR);
                }
                else if ("key".equals(field))
                {
                    sortOrder.add(KEY_COMPARATOR);
                }
                else if ("resolution".equals(field))
                {
                    sortOrder.add(RESOLUTION_COMPARATOR);
                }
            }
        }
        if (sortOrder.isEmpty())
        {
            log.warn("Cannot get property value for '" + APKeys.JIRA_VIEW_ISSUE_LINKS_SORT_ORDER
                    + "'. Defaulting to: " + DEFAULT_SORT_ORDER_STRING);
            sortOrder = DEFAULT_SORT_ORDER;
        }
        this.sortOrder = ComparatorUtils.chainedComparator(sortOrder);
    }

    /**
     * Creates a new instance of this class. Sort order can be specified in the application properties under {@link
     * APKeys#JIRA_VIEW_ISSUE_LINKS_SORT_ORDER} key.
     *
     * @param issueId issue generic value this link collection relates to
     * @param linkTypes a set of link types
     * @param outwardLinkMap a map of outward links
     * @param inwardLinkMap a map of inward links
     * @param remoteUser user, used to filter the linked issues
     * @param applicationProperties application properties
     * @since v4.4
     */
    public LinkCollection(final Long issueId, final Set<IssueLinkType> linkTypes, final Map<String, List<Issue>> outwardLinkMap, final Map<String, List<Issue>> inwardLinkMap, final User remoteUser, final ApplicationProperties applicationProperties)
    {
        this(issueId, linkTypes, outwardLinkMap, inwardLinkMap, remoteUser, false, applicationProperties);
    }

    /**
     * Creates a new instance of this class. Sort order can be specified in the application properties under {@link
     * APKeys#JIRA_VIEW_ISSUE_LINKS_SORT_ORDER} key.
     *
     * @param issue issue generic value this link collection relates to
     * @param linkTypes a set of link types
     * @param outwardLinkMap a map of outward links
     * @param inwardLinkMap a map of inward links
     * @param remoteUser user, used to filter the linked issues
     * @param applicationProperties application properties
     * @deprecated use {@link #LinkCollection(Long, java.util.Set, java.util.Map, java.util.Map,
     *             com.atlassian.crowd.embedded.api.User, com.atlassian.jira.config.properties.ApplicationProperties)}
     *             }
     */
    @Deprecated
    public LinkCollection(GenericValue issue, Set<IssueLinkType> linkTypes, Map<String, List<Issue>> outwardLinkMap, Map<String, List<Issue>> inwardLinkMap, User remoteUser, ApplicationProperties applicationProperties)
    {
        this(issue == null ? null : issue.getLong("id"), linkTypes, outwardLinkMap, inwardLinkMap, remoteUser, false, applicationProperties);
    }

    /**
     * Creates a new instance of this class. Sort order can be specified in the application properties under {@link
     * APKeys#JIRA_VIEW_ISSUE_LINKS_SORT_ORDER} key.
     *
     * @param issue issue generic value this link collection relates to
     * @param linkTypes a set of link types
     * @param outwardLinkMap a map of outward links
     * @param inwardLinkMap a map of inward links
     * @param remoteUser user, used to filter the linked issues
     * @param overrideSecurity false if permissions should be checked when retrieving links
     * @param applicationProperties application properties
     * @since v4.0
     * @deprecated use {@link #LinkCollection(Long, java.util.Set, java.util.Map, java.util.Map,
     *             com.atlassian.crowd.embedded.api.User, boolean, com.atlassian.jira.config.properties.ApplicationProperties)}
     *             }
     */
    @Deprecated
    public LinkCollection(final GenericValue issue, final Set<IssueLinkType> linkTypes, final Map<String, List<Issue>> outwardLinkMap, final Map<String, List<Issue>> inwardLinkMap, final User remoteUser, final boolean overrideSecurity, final ApplicationProperties applicationProperties)
    {
        this(issue == null ? null : issue.getLong("id"), linkTypes, outwardLinkMap, inwardLinkMap, remoteUser, overrideSecurity, applicationProperties);
    }

    /**
     * Returns a set of link types, {@link IssueLinkType} objects.
     *
     * @return a set of {@link IssueLinkType} objects
     */
    public Set<IssueLinkType> getLinkTypes()
    {
        return linkTypes;
    }

    /**
     * Looks up and returns a {@link #sortOrder} sorted list of all outward linked issues by given link name. May return
     * null.
     *
     * @param linkName link name to lookup issues by
     * @return a sorted list of browsable outward linked issues or null (sorry)
     */
    public List<Issue> getOutwardIssues(String linkName)
    {
        if (outwardLinkMap == null)
        {
            return null;
        }
        else
        {
            List<Issue> outwardIssues = outwardLinkMap.get(linkName);
            if (outwardIssues == null)
            {
                return null;
            }

            return filterBrowseableIssues(outwardIssues);
        }
    }

    /**
     * Looks up and returns a {@link #sortOrder} sorted list of all inward linked issues by given link name. May return
     * null.
     *
     * @param linkName link name to lookup issues by
     * @return a sorted list of browsable inward linked issues
     */
    public List<Issue> getInwardIssues(String linkName)
    {
        if (inwardLinkMap == null)
        {
            return null;
        }
        else
        {
            List<Issue> inwardIssues = inwardLinkMap.get(linkName);
            if (inwardIssues == null)
            {
                return null;
            }

            return filterBrowseableIssues(inwardIssues);
        }
    }

    /**
     * Creates and returns a sorted list of linked issues that the user has permission to browse.
     *
     * @param issuesLinkedTo a collection of linked issues
     * @return list of browsable issues
     */
    private List<Issue> filterBrowseableIssues(Collection<Issue> issuesLinkedTo)
    {
        List<Issue> result = new ArrayList<Issue>();
        for (Issue issue : issuesLinkedTo)
        {
            if (issue == null)
            {
                log.warn("Issue " + thisIssueId + " links to a non-existing issue!");
            }
            else if (overrideSecurity || ManagerFactory.getPermissionManager().hasPermission(Permissions.BROWSE, issue.getGenericValue(), remoteUser))
            {
                result.add(issue);
            }
        }
        Collections.sort(result, sortOrder);
        return result;
    }

    /**
     * Returns a collection of issues that contains both inward and outward linking issues. The returned collection is
     * sorted by {@link #sortOrder} and does not contain duplicates.
     *
     * @return a collection of all linked issues
     */
    public Collection<Issue> getAllIssues()
    {
        Set<Issue> allIssues = new HashSet<Issue>();
        getValuesFromMapList(outwardLinkMap, allIssues);
        getValuesFromMapList(inwardLinkMap, allIssues);
        return filterBrowseableIssues(allIssues);
    }

    /**
     * Pass a map that contains key -> List, get all the values from the list, and add them to the set
     *
     * @param values map that contains the issues to add to the given allIssues set
     * @param allIssues set of all issues
     */
    private void getValuesFromMapList(final Map<String, List<Issue>> values, Set<Issue> allIssues)
    {
        if (values != null)
        {
            for (List<Issue> list : values.values())
            {
                allIssues.addAll(list);
            }
        }
    }

    /**
     * Determine whether there are links visible to this user - do not display link panel if not
     *
     * @return true is there are any outward or inward links, false otherwisel
     */
    public boolean isDisplayLinkPanel()
    {
        if (linkTypes != null && !linkTypes.isEmpty())
        {
            for (IssueLinkType issueLinkType : linkTypes)
            {
                List<Issue> outwardIssueLinks = getOutwardIssues(issueLinkType.getName());
                List<Issue> inwardIssues = getInwardIssues(issueLinkType.getName());

                // If any one set of links is visible - return true
                if ((outwardIssueLinks != null && !outwardIssueLinks.isEmpty()) || (inwardIssues != null && !inwardIssues.isEmpty()))
                {
                    return true;
                }
            }
        }
        return false;
    }
}
