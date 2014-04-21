/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */

package com.atlassian.jira.portal;

import com.atlassian.configurable.ObjectConfigurationProperty;
import com.atlassian.configurable.ObjectDescriptor;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.web.bean.I18nBean;
import org.apache.commons.lang.StringUtils;

import java.util.Map;

public class TitleObjectDescriptor implements ObjectDescriptor
{
    public String getDescription(Map properties, Map values)
    {
        ObjectConfigurationProperty ocv = (ObjectConfigurationProperty) properties.get("title");
        I18nHelper i18n = new I18nBean();
        String text = i18n.getText("portlet.text.display.name");
        return StringUtils.replace(text, "{0}", ((String[]) values.get("title"))[0]);
    }

    public Map validateProperties(Map values)
    {
        // No validation required - return original values
        return values;
    }
}
