package com.atlassian.jira.oauth.serviceprovider;

import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.ofbiz.OfBizDelegator;
import com.atlassian.jira.ofbiz.OfBizListIterator;
import com.atlassian.jira.propertyset.JiraPropertySetFactory;
import com.atlassian.jira.user.util.UserUtil;
import com.atlassian.jira.util.collect.CollectionBuilder;
import com.atlassian.jira.util.collect.MapBuilder;
import static com.atlassian.jira.util.dbc.Assertions.notNull;
import com.atlassian.oauth.serviceprovider.InvalidTokenException;
import com.atlassian.oauth.serviceprovider.ServiceProviderConsumerStore;
import com.atlassian.oauth.serviceprovider.ServiceProviderToken;
import com.atlassian.oauth.serviceprovider.ServiceProviderTokenStore;
import com.atlassian.oauth.serviceprovider.StoreException;
import static com.atlassian.oauth.serviceprovider.ServiceProviderToken.Version;
import com.opensymphony.module.propertyset.PropertySet;
import net.jcip.annotations.GuardedBy;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.ofbiz.core.entity.GenericEntityException;
import org.ofbiz.core.entity.GenericModelException;
import org.ofbiz.core.entity.GenericValue;

import java.net.URI;
import java.security.Principal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides an OfBiz implementation of the OAuth Service Provider token store.  That is OAuth tokens that are used to
 * process incoming requests.
 *
 * @since v4.0
 */
public class OfBizServiceProviderTokenStore implements ServiceProviderTokenStore
{
    private static final Logger log = Logger.getLogger(OfBizServiceProviderTokenStore.class);

    public static final String TABLE = "OAuthServiceProviderToken";
    public static final String PROPERTY_SET_KEY = TABLE;
    private final OfBizDelegator delegator;
    private final UserUtil userUtil;
    private final ServiceProviderConsumerStore consumerStore;
    private final JiraPropertySetFactory propertySetFactory;

    public static final class Columns
    {
        public static final String ID = "id";
        public static final String CREATED = "created";
        public static final String TOKEN = "token";
        public static final String TOKEN_SECRET = "tokenSecret";
        public static final String TYPE = "tokenType";
        public static final String CONSUMER_KEY = "consumerKey";
        public static final String USERNAME = "username";
        public static final String TTL = "ttl";
        public static final String AUTHORIZATION = "auth";
        public static final String CALLBACK = "callback";
        public static final String VERIFIER = "verifier";
        public static final String VERSION = "version";
    }

    static enum TokenType
    {
        ACCESS, REQUEST
    }

    public OfBizServiceProviderTokenStore(final OfBizDelegator delegator, final UserUtil userUtil,
            final ServiceProviderConsumerStore consumerStore, final JiraPropertySetFactory propertySetFactory)
    {
        notNull("delegator", delegator);
        notNull("userUtil", userUtil);
        notNull("consumerStore", consumerStore);
        notNull("propertySetFactory", propertySetFactory);

        this.userUtil = userUtil;
        this.delegator = delegator;
        this.consumerStore = consumerStore;
        this.propertySetFactory = propertySetFactory;
    }


    public ServiceProviderToken get(final String token) throws StoreException
    {
        notNull("token", token);

        try
        {
            final List<GenericValue> consumerTokenGVs = delegator.findByAnd(TABLE, MapBuilder.<String, Object>newBuilder().
                    add(Columns.TOKEN, token).toMap());
            if (!consumerTokenGVs.isEmpty())
            {
                return createTokenFromGV(consumerTokenGVs.get(0));
            }
            else
            {
                return null;
            }
        }
        catch (DataAccessException e)
        {
            throw new StoreException(e);
        }
    }

    public Iterable<ServiceProviderToken> getAccessTokensForUser(final String username)
    {
        final List<GenericValue> userTokenGVs;
        try
        {
            userTokenGVs = delegator.findByAnd(TABLE,
                    MapBuilder.<String, Object>newBuilder().
                            add(Columns.USERNAME, username).
                            add(Columns.TYPE, TokenType.ACCESS.toString()).toMap());
        }
        catch (DataAccessException e)
        {
            throw new StoreException(e);
        }
        final List<ServiceProviderToken> ret = new ArrayList<ServiceProviderToken>();
        for (GenericValue userTokenGV : userTokenGVs)
        {
            ret.add(createTokenFromGV(userTokenGV));
        }

        return ret;
    }

    public ServiceProviderToken put(final ServiceProviderToken token) throws StoreException
    {
        notNull("token", token);

        final String username = token.getUser() == null ? null : token.getUser().getName();
        final Map<String, Object> fieldValues = MapBuilder.<String, Object>newBuilder().
                add(Columns.CREATED, new Timestamp(token.getCreationTime())).
                add(Columns.TOKEN, token.getToken()).
                add(Columns.TOKEN_SECRET, token.getTokenSecret()).
                add(Columns.TYPE, token.isAccessToken() ? TokenType.ACCESS.toString() : TokenType.REQUEST.toString()).
                add(Columns.CONSUMER_KEY, token.getConsumer().getKey()).
                add(Columns.USERNAME, username).
                add(Columns.AUTHORIZATION, token.getAuthorization() == null ? null : token.getAuthorization().toString()).
                add(Columns.TTL, token.getTimeToLive()).
                add(Columns.VERIFIER, token.getVerifier()).
                add(Columns.CALLBACK, token.getCallback() == null ? null : token.getCallback().toASCIIString()).
                add(Columns.VERSION, token.getVersion() == null ? null : token.getVersion().toString()).
                toMap();

        try
        {
            final List<GenericValue> consumerTokenGVs = delegator.findByAnd(TABLE, MapBuilder.<String, Object>newBuilder().
                    add(Columns.TOKEN, token.getToken()).toMap());
            if (!consumerTokenGVs.isEmpty())
            {
                final GenericValue gv = consumerTokenGVs.get(0);
                gv.setNonPKFields(fieldValues);
                try
                {
                    gv.store();
                    setTokenProperties(gv.getLong(Columns.ID), token.getProperties());
                }
                catch (GenericEntityException e)
                {
                    throw new DataAccessException(e);
                }
            }
            else
            {
                final GenericValue gv = delegator.createValue(TABLE, fieldValues);
                setTokenProperties(gv.getLong(Columns.ID), token.getProperties());
            }
        }
        catch (DataAccessException e)
        {
            throw new StoreException(e);
        }

        return get(token.getToken());
    }

    public void remove(final String token) throws StoreException
    {
        notNull("token", token);

        try
        {
            //need to lookup the tokenId so we can delete the token properties
            final List<GenericValue> consumerTokenGVs = delegator.findByAnd(TABLE, MapBuilder.<String, Object>newBuilder().
                    add(Columns.TOKEN, token).toMap());
            if (!consumerTokenGVs.isEmpty())
            {
                final GenericValue tokenGv = consumerTokenGVs.get(0);
                final Long tokenId = tokenGv.getLong(Columns.ID);
                delegator.removeValue(tokenGv);
                setTokenProperties(tokenId, Collections.<String, String>emptyMap());
            }
        }
        catch (DataAccessException e)
        {
            throw new StoreException(e);
        }
    }

    public void removeExpiredTokens() throws StoreException
    {
        final OfBizListIterator allTokens = delegator.findListIteratorByCondition(TABLE, null, null,
                CollectionBuilder.newBuilder(Columns.ID, Columns.CREATED, Columns.TTL).asList(), null, null);
        final List<Long> idsToRemove = new ArrayList<Long>();
        try
        {
            GenericValue tokenGV = allTokens.next();
            while (tokenGV != null)
            {
                final Timestamp created = tokenGV.getTimestamp(Columns.CREATED);
                final long ttl = tokenGV.getLong(Columns.TTL);
                if (System.currentTimeMillis() > created.getTime() + ttl)
                {
                    idsToRemove.add(tokenGV.getLong(Columns.ID));
                }
                tokenGV = allTokens.next();
            }
        }
        finally
        {
            allTokens.close();
        }

        try
        {
            final int rowsRemoved = delegator.removeByOr(TABLE, Columns.ID, idsToRemove);
            if (log.isDebugEnabled())
            {
                log.debug("Successfully removed " + rowsRemoved + " expired tokens.");
            }
        }
        catch (GenericModelException e)
        {
            throw new StoreException(e);
        }
        catch (DataAccessException e)
        {
            throw new StoreException(e);
        }
    }

    public void removeByConsumer(final String consumerKey)
    {
        notNull("consumerKey", consumerKey);

        try
        {
            delegator.removeByAnd(TABLE, MapBuilder.<String, Object>newBuilder().
                    add(Columns.CONSUMER_KEY, consumerKey).toMap());
        }
        catch (DataAccessException e)
        {
            throw new StoreException(e);
        }
    }

    private ServiceProviderToken createTokenFromGV(final GenericValue gv)
    {
        boolean isAccessToken = isAccessToken(gv.getString(Columns.TYPE));

        final String token = gv.getString(Columns.TOKEN);
        final Principal user = getUser(gv.getString(Columns.USERNAME));
        if (user == null && isAccessToken)
        {
            throw new InvalidTokenException("Token '" + token + "' is an access token, but has no user associated with it");
        }

        if (isAccessToken)
        {
            return ServiceProviderToken.newAccessToken(token)
                    .tokenSecret(gv.getString(Columns.TOKEN_SECRET))
                    .consumer(consumerStore.get(gv.getString(Columns.CONSUMER_KEY)))
                    .authorizedBy(user)
                    .creationTime(gv.getTimestamp(Columns.CREATED).getTime())
                    .timeToLive(gv.getLong(Columns.TTL))
                    .properties(getTokenProperties(gv.getLong(Columns.ID)))
                    .version(getVersion(gv.getString(Columns.VERSION)))
                    .build();
        }
        else
        {
            final String callBackUriString = gv.getString(Columns.CALLBACK);
            URI callbackURI = null;
            if (StringUtils.isNotBlank(callBackUriString))
            {
                callbackURI = URI.create(callBackUriString);
            }
            ServiceProviderToken.ServiceProviderTokenBuilder builder = ServiceProviderToken.newRequestToken(token)
                    .tokenSecret(gv.getString(Columns.TOKEN_SECRET))
                    .consumer(consumerStore.get(gv.getString(Columns.CONSUMER_KEY)))
                    .callback(callbackURI)
                    .creationTime(gv.getTimestamp(Columns.CREATED).getTime())
                    .timeToLive(gv.getLong(Columns.TTL))
                    .version(getVersion(gv.getString(Columns.VERSION)))
                    .properties(getTokenProperties(gv.getLong(Columns.ID)));

            final ServiceProviderToken.Authorization authorization = getAuthorization(gv.getString(Columns.AUTHORIZATION), user);
            if (ServiceProviderToken.Authorization.AUTHORIZED.equals(authorization))
            {
                builder = builder.authorizedBy(user).verifier(gv.getString(Columns.VERIFIER));
            }
            else if (ServiceProviderToken.Authorization.DENIED.equals(authorization))
            {
                builder = builder.deniedBy(user);
            }
            return builder.build();
        }
    }

    private ServiceProviderToken.Version getVersion(final String versionString)
    {
        if (StringUtils.isBlank(versionString))
        {
            return null;
        }
        return Version.valueOf(versionString);
    }

    private ServiceProviderToken.Authorization getAuthorization(String authorization, Principal user)
    {
        if (authorization != null)
        {
            return ServiceProviderToken.Authorization.valueOf(authorization);
        }
        return user != null ? ServiceProviderToken.Authorization.AUTHORIZED : ServiceProviderToken.Authorization.NONE;
    }


    Principal getUser(final String username)
    {
        return userUtil.getUser(username);
    }

    private Map<String, String> getTokenProperties(final Long tokenId)
    {
        final PropertySet propertySet = propertySetFactory.buildCachingPropertySet(PROPERTY_SET_KEY, tokenId, true);

        final MapBuilder<String, String> ret = MapBuilder.newBuilder();
        @SuppressWarnings ("unchecked")
        final Collection<String> keys = propertySet.getKeys();
        for (String key : keys)
        {
            ret.add(key, propertySet.getText(key));
        }
        return ret.toMap();
    }

    /**
     * We don't really worry too much about synchronisation here, since this should be called via the {@link
     * com.atlassian.jira.oauth.serviceprovider.CachingServiceProviderTokenStore} which ensures proper synchronisation.
     */
    @GuardedBy ("external-lock")
    private void setTokenProperties(final Long tokenId, Map<String, String> props)
    {
        final PropertySet propertySet = propertySetFactory.buildCachingPropertySet(PROPERTY_SET_KEY, tokenId, true);
        //first clear the existing properties
        @SuppressWarnings ("unchecked")
        final Collection<String> keys = propertySet.getKeys();
        for (String key : keys)
        {
            propertySet.remove(key);
        }

        //then add them in again!
        final Set<Map.Entry<String, String>> entries = props.entrySet();
        for (Map.Entry<String, String> prop : entries)
        {
            propertySet.setText(prop.getKey(), prop.getValue());
        }
    }

    private boolean isAccessToken(final String tokenType)
    {
        return TokenType.ACCESS.equals(TokenType.valueOf(tokenType));
    }
}
