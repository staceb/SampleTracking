package com.atlassian.jira.issue.changehistory;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.jira.ofbiz.OfBizListIterator;
import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import org.ofbiz.core.entity.EntityCondition;
import org.ofbiz.core.entity.EntityExpr;
import org.ofbiz.core.entity.EntityOperator;
import org.ofbiz.core.entity.GenericValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.ofbiz.core.entity.EntityOperator.IN;

/**
 * Change history class that performs batch loading of the the child ChangeItems. Instances of this class will keep a
 * reference to other instances of ChangeHistory that loaded in memory, and load all their change items in one go. This
 * reduces the 1+N selects problems that clients would otherwise encounter when iterating over all {@code ChangeItem}s
 * in a group of {@code ChangeHistory}.
 *
 * @since v5.1
 */
class ChangeHistoryBatch
{
    /**
     * Logger for ChangeHistoryBatch.
     */
    private static final Logger log = LoggerFactory.getLogger(ChangeHistoryBatch.class);

    /**
     * Creates a new ChangeHistoryBatch from a collection of ChangeHistory instances.
     *
     * @param issues the Issues to load the ChangeHistory items for
     * @param ofBizDelegator the OfBizDelegator to use for fetching change items
     * @param issueManager the IssueManager   @return a ChangeHistoryBatch
     * @throws NullPointerException if any element in {@code issues} is null
     */
    static ChangeHistoryBatch createBatchForIssue(@Nonnull Iterable<Issue> issues, OfBizDelegator ofBizDelegator, IssueManager issueManager)
    {
        return new ChangeHistoryBatch(issues, ofBizDelegator, issueManager);
    }

    /**
     * The OfBizDelegator to use for fetching change items.
     */
    @Nonnull
    private final OfBizDelegator ofBizDelegator;

    /**
     * The IssueManager to use in the ChangeHistory.
     */
    @Nonnull
    private final IssueManager issueManager;

    /**
     * The Issues that this ChangeHistoryBatch pertains to.
     */
    @Nonnull
    private ImmutableMap<Long, Issue> issues;

    /**
     * The underlying ChangeHistory items.
     */
    @Nonnull
    private final ImmutableList<ChangeHistory> changeHistories;

    /**
     * A lazily-loaded map of ChangeHistory to its ChangeItems.
     */
    @Nullable
    private ImmutableMap<ChangeHistory, List<GenericValue>> batchChangeItems;

    /**
     * Creates a new ChangeHistoryBatch from a collection of ChangeHistory instances.
     *
     * @param issues the Issues to load the ChangeHistory items for
     * @param ofBizDelegator the OfBizDelegator to use for fetching change items
     * @throws NullPointerException if any element in {@code issues} is null
     */
    private ChangeHistoryBatch(@Nonnull Iterable<Issue> issues, @Nonnull OfBizDelegator ofBizDelegator, @Nonnull IssueManager issueManager)
            throws NullPointerException
    {
        this.issues = ImmutableMap.copyOf(Maps.uniqueIndex(issues, new GetIssueIdFn()));
        this.issueManager = checkNotNull(issueManager, "issueManager");
        this.ofBizDelegator = checkNotNull(ofBizDelegator, "ofBizDelegator");
        this.changeHistories = fetchAllChangeGroups();
    }

    /**
     * Returns a list of ChangeHistory.
     *
     * @return a List of ChangeHistory
     */
    List<ChangeHistory> asList()
    {
        return Lists.newArrayList(changeHistories);
    }

    /**
     * Fetches all change history items for this batch's issue.
     *
     * @return a list of ChangeHistory
     */
    private ImmutableList<ChangeHistory> fetchAllChangeGroups()
    {
        if (issues.isEmpty())
        {
            return ImmutableList.of();
        }

        log.debug("About to fetch change groups for issues: {}", issues.keySet());
        OfBizListIterator changeGroups = ofBizDelegator.findListIteratorByCondition("ChangeGroup", new EntityExpr("issue", IN, issues.keySet()), null, null, ImmutableList.of("created ASC", "id ASC"), null);
        try
        {
            log.debug("Fetched {} change groups");
            return wrapChangeHistories(changeGroups);
        }
        finally
        {
            changeGroups.close();
        }
    }

    /**
     * Fetches all change items for this batch's change histories and returns them mapped by change history.
     *
     * @return a Map of ChangeHistory to change items (as GenericValues).
     */
    private ImmutableMap<ChangeHistory, List<GenericValue>> fetchAllChangeItems()
    {
        if (changeHistories.isEmpty())
        {
            return ImmutableMap.of();
        }

        // map all the change groups by id
        Map<Long, ChangeHistory> changeHistoriesById = Maps.newHashMapWithExpectedSize(changeHistories.size());
        for (ChangeHistory changeHistory : changeHistories)
        {
            changeHistoriesById.put(changeHistory.getId(), changeHistory);
        }

        log.debug("About to fetch change items for a batch of {} change groups", changeHistories.size());
        List<GenericValue> changeItems = ofBizDelegator.findByAnd("ChangeItem", ImmutableList.<EntityCondition>of(
                new EntityExpr("group", EntityOperator.IN, changeHistoriesById.keySet())
        ));
        log.debug("Fetched {} change items", changeItems.size());

        // now map the change items by change group for subsequent use
        Multimap<ChangeHistory, GenericValue> changeItemsByChangeGroup = ArrayListMultimap.create();
        for (GenericValue changeItem : changeItems)
        {
            Long parentId = changeItem.getLong("group");
            ChangeHistory parent = changeHistoriesById.get(parentId);
            if (parent == null)
            {
                log.error("Change item {} is not a child of change groups: {}", parentId, changeHistoriesById.keySet());
                continue;
            }

            changeItemsByChangeGroup.put(parent, changeItem);
        }

        return makeImmutable(changeItemsByChangeGroup);
    }

    /**
     * Transforms a list of GenericValue into a list of BatchingChangeHistory.
     *
     * @param changeHistories a list of GenericValue
     * @return an immutable list of BatchingChangeHistory
     */
    private ImmutableList<ChangeHistory> wrapChangeHistories(Iterable<GenericValue> changeHistories)
    {
        if (changeHistories == null)
        {
            return ImmutableList.of();
        }

        return ImmutableList.copyOf(Iterables.transform(changeHistories, new Function<GenericValue, ChangeHistory>()
        {
            @Override
            public ChangeHistory apply(@Nullable GenericValue changeHistory)
            {
                return changeHistory != null ? new BatchingChangeHistory(changeHistory) : null;
            }
        }));
    }

    /**
     * Turns a Multimap into an immutable map.
     *
     * @param multimap a Multimap
     * @return an ImmutableMap
     */
    private static <T, U> ImmutableMap<T, List<U>> makeImmutable(Multimap<? extends T, ? extends U> multimap)
    {
        ImmutableMap.Builder<T, List<U>> builder = ImmutableMap.builder();
        for (Map.Entry<? extends T, ? extends Collection<? extends U>> entry : multimap.asMap().entrySet())
        {
            builder.put(entry.getKey(), Lists.newArrayList(entry.getValue()));
        }

        return builder.build();
    }

    /**
     * Extracts the issue id.
     */
    private static class GetIssueIdFn implements Function<Issue, Long>
    {
        @Override
        public Long apply(Issue issue)
        {
            return issue.getId();
        }
    }

    /**
     * BatchingChangeHistory extends ChangeHistory to allow batch-loading of change items.
     */
    private class BatchingChangeHistory extends ChangeHistory
    {
        /**
         * Creates a new BatchingChangeHistory for the given change history generic value.
         *
         * @param changeHistoryGV a GenericValue corresponding to a ChangeHistory
         */
        public BatchingChangeHistory(GenericValue changeHistoryGV)
        {
            super(changeHistoryGV, issueManager);
        }

        /**
         * Returns this ChangeHistoryBatch's issue.
         *
         * @return an Issue
         */
        @Override
        public Issue getIssue()
        {
            return issues.get(getIssueId());
        }

        /**
         * Returns the ChangeItems for this BatchingChangeHistory. This method has the side effect of loading the child
         * change items of all other BatchingChangeHistory items that are in the same batch.
         *
         * @return the ChangeItem for this BatchingChangeHistory
         */
        @Override
        public List<GenericValue> getChangeItems()
        {
            if (batchChangeItems == null)
            {
                batchChangeItems = fetchAllChangeItems();
            }

            return batchChangeItems.get(this);
        }
    }
}
