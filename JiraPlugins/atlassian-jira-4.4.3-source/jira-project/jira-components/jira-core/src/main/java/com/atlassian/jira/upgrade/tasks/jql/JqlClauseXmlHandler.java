package com.atlassian.jira.upgrade.tasks.jql;

import com.atlassian.jira.jql.parser.JqlParseException;
import com.atlassian.jira.jql.parser.JqlQueryParser;
import com.atlassian.query.Query;
import electric.xml.Element;

/**
 * This is only here to support the in-between state of data that would have been generated by running a dev version
 * of JIRA.
 *
 * @since v4.0
 */
public class JqlClauseXmlHandler implements ClauseXmlHandler
{
    private final JqlQueryParser jqlQueryParser;

    public JqlClauseXmlHandler(final JqlQueryParser jqlQueryParser)
    {
        this.jqlQueryParser = jqlQueryParser;
    }

    public ConversionResult convertXmlToClause(final Element el)
    {
        final String xmlFieldId = el.getName();
        if (!"query".equals(xmlFieldId))
        {
            return new FailedConversionResult(xmlFieldId);
        }

        // Lets get the value and parse it
        String jql = el.getTextString();

        try
        {
            // If there was an empty query the XML will return null, but we don't parse null, so lets make it empty string
            if (jql == null)
            {
                jql = "";
            }
            final Query query = jqlQueryParser.parseQuery(jql);
            return new FullConversionResult(query.getWhereClause());
        }
        catch (JqlParseException e)
        {
            return new FailedConversionNoValuesResult(xmlFieldId);
        }
    }

    public boolean isSafeToNamifyValue()
    {
        return false;
    }
}
