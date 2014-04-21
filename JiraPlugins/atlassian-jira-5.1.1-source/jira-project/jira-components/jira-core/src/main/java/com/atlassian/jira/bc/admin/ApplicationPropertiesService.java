package com.atlassian.jira.bc.admin;

import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.validation.Validated;

import java.util.List;

/**
 * Service level implementation of CRUD for {@link ApplicationProperty ApplicationProperties} which are administrative
 * level configuration settings that boil down to a key and a value. In addition to key and value are other metadata
 * properties as modelled by {@link ApplicationPropertyMetadata} including default value, type and i18n keys etc.
 *
 * @since v4.4
 */
public interface ApplicationPropertiesService
{
    /**
     * Retrieves all properties that can be edited dynamically by the user.
     *
     * @return the properties.
     * @throws DataAccessException if there is a problem with the backing store.
     */
    List<ApplicationProperty> getEditableApplicationProperties() throws DataAccessException;

    /**
     * Retrieves a property with the given key.
     *
     * @param key the property's key.
     * @return the property or null if there is no property with that key.
     * @throws DataAccessException if there is a problem with the backing store.
     */
    ApplicationProperty getApplicationProperty(String key) throws DataAccessException;

    /**
     * Stores the given value for the given key in the underlying data store.
     *
     * @param key the key.
     * @param value the string representation of the value.
     * @return the property in full that corresponds to the new state of the property.
     * @throws DataAccessException if the key cannot be stored due to a problem with the backing store.
     */
    Validated<ApplicationProperty> setApplicationProperty(String key, String value) throws DataAccessException;

}
