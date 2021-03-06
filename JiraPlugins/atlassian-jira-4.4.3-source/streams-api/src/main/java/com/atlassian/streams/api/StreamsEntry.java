package com.atlassian.streams.api;

import java.io.Serializable;
import java.net.URI;

import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.streams.api.common.ImmutableNonEmptyList;
import com.atlassian.streams.api.common.NonEmptyIterable;
import com.atlassian.streams.api.common.Option;

import com.google.common.base.Function;
import com.google.common.base.Supplier;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;

import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.atlassian.streams.api.common.Functions.trimToNone;
import static com.atlassian.streams.api.common.Option.none;
import static com.atlassian.streams.api.common.Option.some;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Suppliers.memoize;
import static com.google.common.collect.Iterables.concat;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * <p>Represents an entry in the activity stream.  Every entry must contain at least a {@code URI} to identify the entry
 * by, a {@code DateTime} of when the entry was posted, an application type ("com.atlassian.jira" for example),
 * a {@code StreamsEntry.Renderer} to generate the contents of the feed entry, at least one
 * {@code StreamsEntry.ActivityObject} and a {@code ActivityVerb}.</p>
 *
 * <p>To correctly build {@code StreamsEntry}s, the {@code StreamsEntry.Parameters} object must be used.  It uses a
 * type-parameter state-machine pattern to ensure that the required attributes are provided without having to use a
 * massive constructor.</p>
 */
public final class StreamsEntry
{
    private final static Logger log = LoggerFactory.getLogger(StreamsEntry.class);
    private final Parameters<HasId, HasPostedDate, HasAlternateLinkUri, HasApplicationType, HasRenderer, HasActivityObject, HasVerb, HasAuthors> params;
    private final I18nResolver i18nResolver;

    /**
     * Construct a new entry from the parameter object.
     *
     * @param params parameter object containing entry values
     */
    public StreamsEntry(Parameters<HasId, HasPostedDate, HasAlternateLinkUri, HasApplicationType, HasRenderer, HasActivityObject, HasVerb, HasAuthors> params, I18nResolver i18nResolver)
    {
        this.params = checkNotNull(params, "params");
        this.i18nResolver = checkNotNull(i18nResolver, "i18nResolver");
    }

    /**
     * Static factory method for a new {@code Parameters} object.
     *
     * @return new {@code Parameters} object
     */
    public static Parameters<NeedsId, NeedsPostedDate, NeedsAlternateLinkUri, NeedsApplicationType, NeedsRenderer, NeedsActivityObject, NeedsVerb, NeedsAuthors> params()
    {
        return new Parameters<NeedsId, NeedsPostedDate, NeedsAlternateLinkUri, NeedsApplicationType, NeedsRenderer, NeedsActivityObject, NeedsVerb, NeedsAuthors> (
                null, null, null, null, ImmutableList.<ActivityObject>of(), null, none(ActivityObject.class), null,
                null, ImmutableList.<String>of(), ImmutableMultimap.<String, Link>of(), none(URI.class));
    }

    /**
     * Static factory method for a new {@code Parameters} object, initialized with the values from an existing
     * {@code StreamsEntry} object.
     *
     * @param entry {@code StreamsEntry} object to use as basis for new {@code Parameters} object
     * @return new {@code Parameters} object initialized with the values from the {@code entry}
     */
    public static Parameters<HasId, HasPostedDate, HasAlternateLinkUri, HasApplicationType, HasRenderer, HasActivityObject, HasVerb, HasAuthors> params(StreamsEntry entry)
    {
        return entry.params;
    }

    /**
     * Returns a {@code Map} of {@code Link}s that should appear in the feed.  The {@code Map} is keyed by the link
     * relationship.
     *
     * @return {@code Link}s that should appear in the feed
     */
    public Multimap<String, Link> getLinks()
    {
        return params.links;
    }

    /**
     * Optionally returns the {@code URI} identifying a {@code StreamEntry} that this entry is in response to.
     *
     * @return {@code URI} identifying a {@code StreamEntry} that this entry is in response to
     */
    public Option<URI> getInReplyTo()
    {
        return params.inReplyTo;
    }

    /**
     * Returns the {@code DateTime} the entry was posted.
     *
     * @return {@code DateTime} the entry was posted
     */
    public DateTime getPostedDate()
    {
        return params.postedDate;
    }

    /**
     * Returns the {@code URI} to be used as the alternate link in the feed.
     *
     * @return the {@code URI} to be used as the alternate link in the feed
     */
    public URI getAlternateLink()
    {
        return params.alternateLinkUri;
    }

    /**
     * Returns the id of the entry.
     *
     * @return the id of the entry
     */
    public URI getId()
    {
        return params.id;
    }

    /**
     * Returns zero or more categories that the entry should be labeled with.
     *
     * @return zero or more categories that the entry should be labeled with
     */
    public Iterable<String> getCategories()
    {
        return params.categories;
    }

    /**
     * Returns one or more authors responsible for creating the entry.
     *
     * @return one or more authors responsible for creating the entry
     */
    public NonEmptyIterable<UserProfile> getAuthors()
    {
        return params.authors;
    }

    /**
     * Returns the {@code ActivityVerb} of the entry.
     *
     * @return the {@code ActivityVerb} of the entry
     */
    public ActivityVerb getVerb()
    {
        return params.verb;
    }

    /**
     * Returns the optional {@code ActivityObject} target that the activity verb was done to.
     *
     * @return the optional {@code ActivityObject} target that the activity verb was done to
     */
    public Option<ActivityObject> getTarget()
    {
        return params.target;
    }

    /**
     * Returns the type of application which generated the entry - for example, "com.atlassian.jira".
     *
     * @return the type of application which generated the entry
     */
    public String getApplicationType()
    {
        return params.applicationType;
    }

    /**
     * Returns one or more {@code ActivityObject}s identifying the main objects involved in the activity.
     *
     * @return one or more {@code ActivityObject}s of the entry
     */
    public Iterable<ActivityObject> getActivityObjects()
    {
        return params.activityObjects;
    }

    /**
     * Returns the title of the entry rendered as HTML.
     *
     * @return the title of the entry rendered as HTML
     */
    public Html renderTitleAsHtml()
    {
        try
        {
            return titleAsHtml.get();
        }
        catch (Exception e)
        {
            return getErrorHtml(e);
        }
    }
    private final Supplier<Html> titleAsHtml = memoize(new Supplier<Html>()
    {
        public Html get()
        {
            return params.renderer.renderTitleAsHtml(StreamsEntry.this);
        }
    });

    /**
     * Returns the optional summary of the entry rendered as HTML
     *
     * @return the optional summary of the entry rendered as HTML
     */
    public Option<Html> renderSummaryAsHtml()
    {
        try
        {
            return summaryAsHtml.get();
        }
        catch (Exception e)
        {
            return some(getErrorHtml(e));
        }
    }
    private final Supplier<Option<Html>> summaryAsHtml = memoize(new Supplier<Option<Html>>()
    {
        public Option<Html> get()
        {
            return params.renderer.renderSummaryAsHtml(StreamsEntry.this);
        }
    });

    /**
     * Returns the content of the entry rendered as HTML
     *
     * @return the content of the entry rendered as HTML
     */
    public Option<Html> renderContentAsHtml()
    {
        try
        {
            return contentAsHtml.get();
        }
        catch (Exception e)
        {
            return some(getErrorHtml(e));
        }
    }
    private final Supplier<Option<Html>> contentAsHtml = memoize(new Supplier<Option<Html>>()
    {
        public Option<Html> get()
        {
            return params.renderer.renderContentAsHtml(StreamsEntry.this);
        }
    });
    
    private Html getErrorHtml(Exception e)
    {
        log.error("An unknown error occurred while rendering a Streams entry", e);
        return new Html(i18nResolver.getText("stream.error.unexpected.rendering.error"));
    }

    /**
     * Returns the default ordering for entries: by posted date, most recent first.
     *
     * @return an {@link Ordering}
     */
    public static Ordering<StreamsEntry> byPostedDate()
    {
        return byPostedDate;
    }
    private static final Ordering<StreamsEntry> byPostedDate = new Ordering<StreamsEntry>()
    {
        public int compare(StreamsEntry entry1, StreamsEntry entry2)
        {
            return entry2.getPostedDate().compareTo(entry1.getPostedDate());
        }
    };

    /**
     * Phantom-type indicating that the ID parameter has been provided.
     */
    public static abstract class HasId { HasId() {} }

    /**
     * Phantom-type indicating that the ID parameter has not been provided.
     */
    public static abstract class NeedsId { NeedsId() {} }

    /**
     * Phantom-type indicating that the posted date parameter has been provided.
     */
    public static abstract class HasPostedDate { HasPostedDate() {} }

    /**
     * Phantom-type indicating that the posted date parameter has not been provided.
     */
    public static abstract class NeedsPostedDate { NeedsPostedDate() {} }

    /**
     * Phantom-type indicating that the alternate link URI parameter has been provided.
     */
    public static abstract class HasAlternateLinkUri { HasAlternateLinkUri() {} }

    /**
     * Phantom-type indicating that the alternate link URI parameter has not been provided.
     */
    public static abstract class NeedsAlternateLinkUri { NeedsAlternateLinkUri() {} }

    /**
     * Phantom-type indicating that the application type parameter has been provided.
     */
    public static abstract class HasApplicationType { HasApplicationType() {} }

    /**
     * Phantom-type indicating that the application type parameter has not been provided.
     */
    public static abstract class NeedsApplicationType { NeedsApplicationType() {} }

    /**
     * Phantom-type indicating that the renderer parameter has been provided.
     */
    public static abstract class HasRenderer { HasRenderer() {} }

    /**
     * Phantom-type indicating that the renderer parameter has not been provided.
     */
    public static abstract class NeedsRenderer { NeedsRenderer() {} }

    /**
     * Phantom-type indicating that the activity object parameter has been provided.
     */
    public static abstract class HasActivityObject { HasActivityObject() {} }

    /**
     * Phantom-type indicating that the activity object parameter has not been provided.
     */
    public static abstract class NeedsActivityObject { NeedsActivityObject() {} }

    /**
     * Phantom-type indicating that the verb parameter has been provided.
     */
    public static abstract class HasVerb { HasVerb() {} }

    /**
     * Phantom-type indicating that the verb parameter has not been provided.
     */
    public static abstract class NeedsVerb { NeedsVerb() {} }


    /**
     * Phantom-type indicating that the verb parameter has not been provided.
     */
    public static abstract class HasAuthors { HasAuthors() {} }

    public static abstract class NeedsAuthors { NeedsAuthors() {} }

    /**
     * A parameter object for building up a {@code StreamsEntry}.  Provided to make constructing a correct entry easier.
     * It is an immutable object and uses a type-parameter based state-machine to track whether or not a
     * {@code StreamsEntry} can be constructed from the values provided or if more information is needed.
     *
     * @param <IdStatus> one of {@code HasId} or {@code NeedsId}
     * @param <PostedDateStatus> one of {@code HasPostedDate} or {@code NeedsPostedDate}
     * @param <AlternateLinkUriStatus> one of {@code HasAlternateLinkUri} or {@code NeedsAlternateLinkUri}
     * @param <ApplicationTypeStatus> one of {@code HasApplicationType} or {@code NeedsApplicationType}
     * @param <RendererStatus> one of {@code HasRenderer} or {@code NeedsRenderer}
     * @param <ActivityObjectStatus> one of {@code HasActivityObject} or {@code NeedsActivityObject}
     * @param <VerbStatus> one of {@code HasVerb} or {@code NeedsVerb}
     */
    public static class Parameters<IdStatus, PostedDateStatus, AlternateLinkUriStatus, ApplicationTypeStatus, RendererStatus, ActivityObjectStatus, VerbStatus, AuthorsStatus>
    {
        private final URI id;
        private final URI alternateLinkUri;
        private final Renderer renderer;
        private final DateTime postedDate;
        private final NonEmptyIterable<UserProfile> authors;
        private final Iterable<String> categories;
        private final Multimap<String, Link> links;
        private final String applicationType;
        private final Iterable<ActivityObject> activityObjects;
        private final ActivityVerb verb;
        private final Option<ActivityObject> target;
        private final Option<URI> inReplyTo;

        private Parameters(
                URI id,
                DateTime postedDate,
                String applicationType,
                Renderer renderer,
                Iterable<ActivityObject> activityObjects,
                ActivityVerb verb,
                Option<ActivityObject> target,
                URI alternateLinkUri,
                NonEmptyIterable<UserProfile> authors,
                Iterable<String> category,
                Multimap<String, Link> links,
                Option<URI> inReplyTo)
        {
            this.id = id;
            this.postedDate = postedDate;
            this.applicationType = applicationType;
            this.renderer = renderer;
            this.activityObjects = activityObjects;
            this.verb = verb;
            this.target = target;
            this.alternateLinkUri = alternateLinkUri;
            this.authors = authors;
            this.categories = category;
            this.links = links;
            this.inReplyTo = inReplyTo;
        }

        private static <IdStatus, PostedDateStatus, AlternateLinkUriStatus, ApplicationTypeStatus, RendererStatus, ActivityObjectStatus, VerbStatus, AuthorsStatus>
            Parameters<IdStatus, PostedDateStatus, AlternateLinkUriStatus, ApplicationTypeStatus, RendererStatus, ActivityObjectStatus, VerbStatus, AuthorsStatus>
            newParams(
                URI id,
                DateTime postedDate,
                String applicationType,
                Renderer renderer,
                Iterable<ActivityObject> activityObjects,
                ActivityVerb verb,
                Option<ActivityObject> target,
                URI alternateLinkUri,
                NonEmptyIterable<UserProfile> authors,
                Iterable<String> categories,
                Multimap<String, Link> links,
                Option<URI> inReplyTo)
        {
            return new Parameters<IdStatus, PostedDateStatus, AlternateLinkUriStatus, ApplicationTypeStatus, RendererStatus, ActivityObjectStatus, VerbStatus, AuthorsStatus>(
                    id, postedDate, applicationType, renderer, activityObjects, verb, target, alternateLinkUri, authors, categories,
                    links, inReplyTo);
        }
        /**
         * Specify the ID of the entry
         *
         * @param id ID of the entry
         * @return new {@code Parameters} object with the id set
         */
        public Parameters<HasId, PostedDateStatus, AlternateLinkUriStatus, ApplicationTypeStatus, RendererStatus, ActivityObjectStatus, VerbStatus, AuthorsStatus> id(URI id)
        {
            return newParams(
                    checkNotNull(id, "id"), postedDate, applicationType, renderer, activityObjects, verb, target, alternateLinkUri,
                    authors, categories, links, inReplyTo);
        }

        /**
         * Specify the date the entry was posted
         *
         * @param postedDate date the entry was posted
         * @return new {@code Parameters} object with the posted date set
         */
        public Parameters<IdStatus, HasPostedDate, AlternateLinkUriStatus, ApplicationTypeStatus, RendererStatus, ActivityObjectStatus, VerbStatus, AuthorsStatus> postedDate(DateTime postedDate)
        {
            return newParams(
                    id, checkNotNull(postedDate, "postedDate"), applicationType, renderer, activityObjects, verb,
                    target, alternateLinkUri, authors, categories, links, inReplyTo);
        }

        /**
         * Specify the type of the application that created the entry
         *
         * @param applicationType type of the application that created the entry
         * @return new {@code Parameters} object with the application type set
         */
        public Parameters<IdStatus, PostedDateStatus, AlternateLinkUriStatus, HasApplicationType, RendererStatus, ActivityObjectStatus, VerbStatus, AuthorsStatus> applicationType(String applicationType)
        {
            return newParams(
                    id, postedDate, checkNotNull(applicationType, "applicationType"), renderer, activityObjects, verb,
                    target, alternateLinkUri, authors, categories, links, inReplyTo);
        }

        /**
         * Specify the renderer to use to render the title, summary and content
         *
         * @param renderer renderer to use to render the title, summary and content
         * @return new {@code Parameters} object with the renderer set
         */
        public Parameters<IdStatus, PostedDateStatus, AlternateLinkUriStatus, ApplicationTypeStatus, HasRenderer, ActivityObjectStatus, VerbStatus, AuthorsStatus> renderer(Renderer renderer)
        {
            return newParams(
                    id, postedDate, applicationType, checkNotNull(renderer, "renderer"), activityObjects, verb, target,
                    alternateLinkUri, authors, categories, links, inReplyTo);
        }

        /**
         * Add activity objects to the entry
         *
         * @param activityObjects activity objects to add to the entry
         * @return new {@code Parameters} object with the activity objects added
         */
        public Parameters<IdStatus, PostedDateStatus, AlternateLinkUriStatus, ApplicationTypeStatus, RendererStatus, HasActivityObject, VerbStatus, AuthorsStatus> addActivityObjects(Iterable<ActivityObject> activityObjects)
        {
            return newParams(
                    id, postedDate, applicationType, renderer, concat(this.activityObjects, activityObjects), verb,
                    target, alternateLinkUri, authors, categories, links, inReplyTo);
        }

        /**
         * Add an activity object to the entry
         *
         * @param activityObject activity object to add to the entry
         * @return new {@code Parameters} object with the activity object added
         */
        public Parameters<IdStatus, PostedDateStatus, AlternateLinkUriStatus, ApplicationTypeStatus, RendererStatus, HasActivityObject, VerbStatus, AuthorsStatus> addActivityObject(ActivityObject activityObject)
        {
            return addActivityObjects(ImmutableList.of(activityObject));
        }

        /**
         * Specify the activity verb for the entry
         *
         * @param verb activity verb for the entry
         * @return new {@code Parameters} object with the verb set
         */
        public Parameters<IdStatus, PostedDateStatus, AlternateLinkUriStatus, ApplicationTypeStatus, RendererStatus, ActivityObjectStatus, HasVerb, AuthorsStatus> verb(ActivityVerb verb)
        {
            return newParams(
                    id, postedDate, applicationType, renderer, activityObjects, checkNotNull(verb, "verb"), target,
                    alternateLinkUri, authors, categories, links, inReplyTo);
        }

        /**
         * Specify the alternate link {@code URI} for the entry
         *
         * @param alternateLinkUri {@code URI} to use as the entry's alternate link
         * @return new {@code Parameters} object with the alternate link URI set
         */
        public Parameters<IdStatus, PostedDateStatus, HasAlternateLinkUri, ApplicationTypeStatus, RendererStatus, ActivityObjectStatus, VerbStatus, AuthorsStatus> alternateLinkUri(URI alternateLinkUri)
        {
            return newParams(
                    id, postedDate, applicationType, renderer, activityObjects, verb, target, checkNotNull(alternateLinkUri, "alternateLinkUri"), authors, categories,
                    links, inReplyTo);
        }

        /**
         * Specify one or more authors of the entry.
         *
         * @param authors authors responsible for creating the entry
         * @return new {@code Parameters} object with the authors set
         */
        public Parameters<IdStatus, PostedDateStatus, AlternateLinkUriStatus, ApplicationTypeStatus, RendererStatus, ActivityObjectStatus, VerbStatus, HasAuthors> authors(NonEmptyIterable<UserProfile> authors)
        {
            return newParams(
                    id, postedDate, applicationType, renderer, activityObjects, verb, target, alternateLinkUri, authors, categories,
                    links, inReplyTo);
        }

        /**
         * Specify zero or more categories for the entry.
         *
         * @param category categories the entry belongs in
         * @return new {@code Parameters} object with the categories set
         */
        public Parameters<IdStatus, PostedDateStatus, AlternateLinkUriStatus, ApplicationTypeStatus, RendererStatus, ActivityObjectStatus, VerbStatus, AuthorsStatus> categories(Iterable<String> category)
        {
            return newParams(
                    id, postedDate, applicationType, renderer, activityObjects, verb, target, alternateLinkUri, authors, category,
                    links, inReplyTo);
        }

        /**
         * Add zero or more {@code Link}s to the entry.
         *
         * @param links additional links to appear in the entry
         * @return new {@code Parameters} object with the additional links added
         */
        public Parameters<IdStatus, PostedDateStatus, AlternateLinkUriStatus, ApplicationTypeStatus, RendererStatus, ActivityObjectStatus, VerbStatus, AuthorsStatus> addLinks(Iterable<Link> links)
        {
            Multimap<String, Link> newLinks = ImmutableMultimap.<String, Link>builder().
                putAll(this.links).
                putAll(Multimaps.index(links, new Function<Link, String>()
                {
                    public String apply(Link link)
                    {
                        return link.getRel();
                    }
                })).build();
            return newParams(
                    id, postedDate, applicationType, renderer, activityObjects, verb, target, alternateLinkUri, authors, categories,
                    newLinks, inReplyTo);
        }

        /**
         * Add zero or more {@code Link}s to the entry.
         *
         * @param links additional links to appear in the entry
         * @return new {@code Parameters} object with the additional links added
         */
        public Parameters<IdStatus, PostedDateStatus, AlternateLinkUriStatus, ApplicationTypeStatus, RendererStatus, ActivityObjectStatus, VerbStatus, AuthorsStatus> addLinks(Link... links)
        {
            return addLinks(asList(links));
        }

        /**
         * Add a {@code Link} to the entry.
         *
         * @param uri an additional link to appear in the entry
         * @param rel the rel of the link
         * @return new {@code Parameters} object with the additional link added
         */
        public Parameters<IdStatus, PostedDateStatus, AlternateLinkUriStatus, ApplicationTypeStatus, RendererStatus, ActivityObjectStatus, VerbStatus, AuthorsStatus> addLink(URI uri, String rel)
        {
            return addLinks(new Link(uri, rel));
        }

        /**
         * Add a {@code Link} to the entry if the {@code URI} is not {@code none}.
         *
         * @param uri optional {@code URI} to be added as a link
         * @param rel relation of the link to the entry
         * @return new {@code Parameters} object with the additional link added if the {@code URI} is not {@code none}
         */
        public Parameters<IdStatus, PostedDateStatus, AlternateLinkUriStatus, ApplicationTypeStatus, RendererStatus, ActivityObjectStatus, VerbStatus, AuthorsStatus> addLink(Option<URI> uri, String rel)
        {
            for(URI u : uri)
            {
                return addLink(u, rel);
            }
            return this;
        }

        /**
         * Specify the optional target of the activity.
         *
         * @param target Optional target of the activity
         * @return new {@code Parameters} object with the target set
         */
        public Parameters<IdStatus, PostedDateStatus, AlternateLinkUriStatus, ApplicationTypeStatus, RendererStatus, ActivityObjectStatus, VerbStatus, AuthorsStatus> target(Option<ActivityObject> target)
        {
            return newParams(
                    id, postedDate, applicationType, renderer, activityObjects, verb, target, alternateLinkUri, authors, categories,
                    links, inReplyTo);
        }

        /**
         * Specify the optional in-reply-to ID of the entry.
         *
         * @param inReplyTo Optional in-reply-to ID of the entry
         * @return new {@code Parameters} object with the in-reply-to ID set
         */
        public Parameters<IdStatus, PostedDateStatus, AlternateLinkUriStatus, ApplicationTypeStatus, RendererStatus, ActivityObjectStatus, VerbStatus, AuthorsStatus> inReplyTo(Option<URI> inReplyTo)
        {
            return newParams(
                    id, postedDate, applicationType, renderer, activityObjects, verb, target, alternateLinkUri, authors, categories,
                    links, inReplyTo);
        }
    }

    /**
     * Contains the information about the main object of the activity.
     */
    public static final class ActivityObject
    {
        private final Parameters<HasId, HasAlternateLinkUri, HasType> params;

        public ActivityObject(Parameters<HasId, HasAlternateLinkUri, HasType> params)
        {
            this.params = checkNotNull(params, "params");
        }

        /**
         * Static factory method for creating an {@code ActivityObject.Parameters} object.
         *
         * @return new parameters object
         */
        public static Parameters<NeedsId, NeedsAlternateLinkUri, NeedsType> params()
        {
            return Parameters.newParams(null, none(String.class), none(Html.class), null, null, none(String.class));
        }

        /**
         * Returns the ID of the activity object.
         *
         * @return the ID of the activity object
         */
        public String getId()
        {
            return params.id;
        }

        /**
         * Returns the optional title of the activity object.
         *
         * @return the title of the activity object
         */
        public Option<String> getTitle()
        {
            return params.title;
        }

        /**
         * Returns the optional title of the activity object rendered as Html.
         *
         * @return the title of the activity object rendered as Html
         */
        public Option<Html> getTitleAsHtml()
        {
            return params.titleAsHtml;
        }

        /**
         * Returns the alternate link {@code URI} for the activity object.
         *
         * @return the alternate link {@code URI} for the activity object
         */
        public URI getAlternateLinkUri()
        {
            return params.alternateLinkUri;
        }

        /**
         * Returns the type of the activity object.
         *
         * @return the type of the activity object
         */
        public ActivityObjectType getActivityObjectType()
        {
            return params.activityObjectType;
        }

        /**
         * Return the optional summary of the activity object.
         *
         * @return the optional summary of the activity object
         */
        public Option<String> getSummary()
        {
            return params.summary;
        }

        /**
         * Phantom-type indicating that the ID parameter has been provided.
         */
        public static abstract class HasId { HasId() {} }

        /**
         * Phantom-type indicating that the ID parameter has not been provided.
         */
        public static abstract class NeedsId { NeedsId() {} }

        /**
         * Phantom-type indicating that the alternate link URI parameter has been provided.
         */
        public static abstract class HasAlternateLinkUri { HasAlternateLinkUri() {} }

        /**
         * Phantom-type indicating that the alternate link URI parameter has not been provided.
         */
        public static abstract class NeedsAlternateLinkUri { NeedsAlternateLinkUri() {} }
        /**
         * Phantom-type indicating that the activity object type parameter has been provided.
         */
        public static abstract class HasType { HasType() {} }

        /**
         * Phantom-type indicating that the activity object type parameter has not been provided.
         */
        public static abstract class NeedsType { NeedsType() {} }

        /**
         * Parameters object used to construct {@code ActivityObject}s.
         */
        public static class Parameters<IdStatus, AlternateLinkUriStatus, TypeStatus>
        {
            private final String id;
            private final Option<String> title;
            private final Option<Html> titleAsHtml;
            private final URI alternateLinkUri;
            private final ActivityObjectType activityObjectType;
            private final Option<String> summary;

            private Parameters(String id, Option<String> title, Option<Html> titleAsHtml, URI alternateLinkUri, ActivityObjectType type, Option<String> summary)
            {
                this.id = id;
                this.title = title;
                this.titleAsHtml = titleAsHtml;
                this.alternateLinkUri = alternateLinkUri;
                this.activityObjectType = type;
                this.summary = summary;
            }

            private static <IdStatus, AlternateLinkUriStatus, TypeStatus>
                Parameters<IdStatus, AlternateLinkUriStatus, TypeStatus>
                newParams(String id, Option<String> title, Option<Html> titleAsHtml, URI alternateLinkUri, ActivityObjectType type, Option<String> summary)
            {
                return new Parameters<IdStatus, AlternateLinkUriStatus, TypeStatus>(id, title, titleAsHtml, alternateLinkUri, type, summary);
            }

            /**
             * Specify the id for the activity object
             *
             * @param id idfor the activity object
             * @return new {@code Parameters} object with the id set
             */
            public Parameters<HasId, AlternateLinkUriStatus, TypeStatus> id(String id)
            {
                return newParams(checkNotNull(id, "id"), title, titleAsHtml, alternateLinkUri, activityObjectType, summary);
            }

            /**
             * Specify the title for the activity object
             *
             * @param title title for the activity object
             * @return new {@code Parameters} object with the title set
             */
            public Parameters<IdStatus, AlternateLinkUriStatus, TypeStatus> title(Option<String> title)
            {
                return newParams(id, checkNotNull(title, "title"), titleAsHtml, alternateLinkUri, activityObjectType, summary);
            }

            /**
             * Specify the title for the activity object rendered as Html
             *
             * @param titleAsHtml title for the activity object rendered as Html
             * @return new {@code Parameters} object with the title set
             */
            public Parameters<IdStatus, AlternateLinkUriStatus, TypeStatus> titleAsHtml(Option<Html> titleAsHtml)
            {
                return newParams(id, title, checkNotNull(titleAsHtml, "titleAsHtml"), alternateLinkUri, activityObjectType, summary);
            }

            /**
             * Specify the alternate link {@code URI} for the activity object
             *
             * @param alternateLinkUri the alternate link {@code URI} for the activity object
             * @return new {@code Parameters} object with the alternate link {@code URI} set
             */
            public Parameters<IdStatus, HasAlternateLinkUri, TypeStatus>alternateLinkUri(URI alternateLinkUri)
            {
                return newParams(id, title, titleAsHtml, checkNotNull(alternateLinkUri, "alternateLinkUri"), activityObjectType, summary);
            }

            /**
             * Set the type and return {@code this} builder.
             *
             * @param activityObjectType the type of the activity object being built
             * @return {@code this} builder
             */
            public Parameters<IdStatus, AlternateLinkUriStatus, HasType> activityObjectType(ActivityObjectType activityObjectType)
            {
                return newParams(id, title, titleAsHtml, alternateLinkUri, checkNotNull(activityObjectType, "activityObjectType"), summary);
            }

            public Parameters<IdStatus, AlternateLinkUriStatus, TypeStatus> summary(Option<String> summary)
            {
                return newParams(id, title, titleAsHtml, alternateLinkUri, activityObjectType, summary);
            }
        }
    }

    /**
     * Represents an ATOM {@code &lt;link&gt;} element.
     */
    public static final class Link
    {
        private final URI href;
        private final String rel;

        /**
         * Construct a new {@code Link}
         *
         * @param href {@code URI} to use as the {@code href} attribute of the {@code &lt;link&gt;} element
         * @param rel value to use as the {@code rel} attribute of the {@code &lt;link&gt;} element
         */
        public Link(URI href, String rel)
        {
            if (isBlank(checkNotNull(rel, "rel")))
            {
                throw new IllegalArgumentException("rel cannot be blank");
            }
            this.rel = rel;
            this.href = href;
        }

        /**
         * Returns value to use as the {@code rel} attribute of the {@code &lt;link&gt;} element.
         *
         * @return value to use as the {@code rel} attribute of the {@code &lt;link&gt;} element
         */
        public String getRel()
        {
            return rel;
        }

        /**
         * Returns the {@code URI} to use as the {@code href} attribute of the {@code &lt;link&gt;} element
         * @return the {@code URI} to use as the {@code href} attribute of the {@code &lt;link&gt;} element
         */
        public URI getHref()
        {
            return href;
        }
    }

    /**
     * Renderers contain the methods needed to display entry titles, summaries and contents in different forms.
     */
    public interface Renderer
    {
        static final int SUMMARY_LIMIT = 250;

        /**
         * Render the title of the {@code StreamsEntry} as HTML.
         *
         * @param entry entry whose title is to be rendered
         * @return the title of the {@code StreamsEntry} rendered as HTML
         */
        Html renderTitleAsHtml(StreamsEntry entry);

        /**
         * Render the optional summary of the {@code StreamsEntry} as HTML.
         *
         * @param entry entry whose summary is to be rendered
         * @return the summary of the {@code StreamsEntry} optionally rendered as HTML
         */
        Option<Html> renderSummaryAsHtml(StreamsEntry entry);

        /**
         * Render the content of the {@code StreamsEntry} as HTML.
         *
         * @param entry entry whose content is to be rendered
         * @return the content of the {@code StreamsEntry} rendered as HTML
         */
        Option<Html> renderContentAsHtml(StreamsEntry entry);
    }

    /**
     * A wrapper around {@code String}s that have been through an HTML entity escaping process.
     */
    public static final class Html implements Serializable
    {
        private static final Logger log = LoggerFactory.getLogger(Html.class);

        private final String value;

        public Html(String value)
        {
            this.value = stripControlChars(checkNotNull(value));
        }

        public static Function<String, Html> html()
        {
            return HtmlF.INSTANCE;
        }

        public static Html html(String s)
        {
            return html().apply(s);
        }

        private enum HtmlF implements Function<String, Html>
        {
            INSTANCE;

            public Html apply(String s)
            {
                return new Html(s);
            }
        }

        public static Function<Html, String> htmlToString()
        {
            return HtmlToString.INSTANCE;
        }

        private enum HtmlToString implements Function<Html, String>
        {
            INSTANCE;

            public String apply(Html h)
            {
                return h.toString();
            }
        }

        public static Function<Html, Option<Html>> trimHtmlToNone()
        {
            return TrimHtmlToNone.INSTANCE;
        }

        private enum TrimHtmlToNone implements Function<Html, Option<Html>>
        {
            INSTANCE;

            public Option<Html> apply(Html h)
            {
                return trimToNone().apply(h.toString()).map(html());
            }
        }

        @Override
        public String toString()
        {
            return value;
        }

        @Override
        public int hashCode()
        {
            return value.hashCode();
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
            {
                return true;
            }
            if (obj == null || getClass() != obj.getClass())
            {
                return false;
            }
            Html other = (Html) obj;
            return value.equals(other.value);
        }

        private String stripControlChars(final String s)
        {
            StringBuilder sb = new StringBuilder();
            for (char c : s.toCharArray())
            {
                if (illegal(c))
                {
                    log.debug("Invalid character encountered: codePoint = {}", String.valueOf(c).codePointAt(0));
                }
                else
                {
                    sb.append(c);
                }
            }
            return sb.toString();
        }

        private boolean illegal(final char c)
        {
            return between(c, '\u0000', '\u0008')
                || between(c, '\u000B', '\u001F')
                || between(c, '\uFFFE', '\uFFFF');
        }

        private boolean between(char c, char minInclusive, char maxInclusive)
        {
            return c >= minInclusive && c <= maxInclusive;
        }
    }
}
