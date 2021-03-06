package com.atlassian.jira.config.properties;

import com.atlassian.jira.bc.admin.ApplicationPropertyMetadata;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.validation.ApplicationPropertyEnumerator;
import com.atlassian.validation.EnumValidator;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Handles the loading and parsing of the application properties metadata into a map of keys to metadata objects.
 *
 * @since v4.4
 */
class MetadataLoader
{
    private static final Logger log = Logger.getLogger(MetadataLoader.class);

    /**
     * Load the ApplicationPropertyMetadata entries from the classpath resource with the given path name.
     *
     * @param xmlFilename path to classpath-loadable resource.
     * @return A map of key to {@link ApplicationPropertyMetadata} representing the config.
     * @throws DataAccessException only if there is a problem reading the resource.
     */
    LinkedHashMap<String, ApplicationPropertyMetadata> loadMetadata(String xmlFilename) throws DataAccessException
    {
        try
        {
            log.debug("Loading application properties metadata from " + xmlFilename);
            InputStream mxml = getClass().getClassLoader().getResourceAsStream(xmlFilename);
            return loadMetadata(mxml, xmlFilename);
        }
        catch (Exception e)
        {
            throw new DataAccessException("Cannot load the application properties metadata file " + xmlFilename, e);
        }
    }

    /**
     * Load the ApplicationPropertyMetadata entries from the given stream (and the given path name).
     *
     * @param stream the {@link InputStream} from which the XML is loaded.
     * @param streamDescriptor in the case of logging an error, what name should the resource at the stream be given.
     * @return A map of key to {@link ApplicationPropertyMetadata} representing the config.
     * @throws DocumentException only if there is a problem reading the resource.
     */
    LinkedHashMap<String, ApplicationPropertyMetadata> loadMetadata(InputStream stream, String streamDescriptor)
            throws DocumentException
    {
        LinkedHashMap<String, ApplicationPropertyMetadata> metadataMap = new LinkedHashMap<String, ApplicationPropertyMetadata>();

        SAXReader reader = new SAXReader();
        Document doc = reader.read(stream);
        Element root = doc.getRootElement();

        Iterator properties = root.element("properties").elementIterator();
        while (properties.hasNext())
        {
            Element property = (Element) properties.next();
            String key = property.elementText("key");
            String defaultValue = property.elementText("default-value");
            String type = property.elementText("type");
            if (type == null)
            {
                type = "string";
            }
            String validator = property.elementText("validator");
            Supplier<EnumValidator> validatorObject = null;
            Map<String, ApplicationPropertyEnumerator> enumerators = Maps.newHashMap();
            if (StringUtils.isBlank(type))
            {
                // let's see if it's a structured type, only one we support for now is enum
                Element typeElement = property.element("type");
                if (typeElement != null) {
                    Element enm = typeElement.element("enum");
                    if (enm != null)
                    {
                        type = "enum";
                        ArrayList<String> options = new ArrayList<String>();
                        Iterator optionIter = enm.elementIterator("option");
                        while (optionIter.hasNext())
                        {
                            final Element optionElem = (Element) optionIter.next();
                            final String optionValue = optionElem.getText();

                            if (optionValue == null || "".equals(optionValue.trim()))
                            {
                                throw new IllegalArgumentException("No option may be blank");
                            }
                            options.add(optionValue);
                        }

                        enumerators.put(key, ApplicationPropertyEnumerator.of(options));
                        validatorObject = Suppliers.ofInstance(new EnumValidator(enumerators.get(key)));
                    }
                }
            }

            String name = property.elementText("name");
            String desc = property.elementText("description");
            if (name == null)
            {
                name = key;
            }
            // defaults to true if absent
            boolean userEditable = !"false".equalsIgnoreCase(property.elementText("user-editable"));
            // defaults to true if absent
            boolean requiresRestart = !"false".equalsIgnoreCase(property.elementText("requires-restart"));
            if (key != null)
            {
                ApplicationPropertyMetadata metadata;
                if (validatorObject == null) {
                    metadata = new ApplicationPropertyMetadata(key, type, defaultValue, validator, userEditable, requiresRestart, name, desc, enumerators.get(key));
                } else {
                    metadata = new ApplicationPropertyMetadata(key, type, defaultValue, validatorObject, userEditable, requiresRestart, name, desc, enumerators.get(key));
                }
                metadataMap.put(key, metadata);
            }
            else
            {
                log.error(streamDescriptor + " contains null key");
            }
        }
        return metadataMap;
    }
}
