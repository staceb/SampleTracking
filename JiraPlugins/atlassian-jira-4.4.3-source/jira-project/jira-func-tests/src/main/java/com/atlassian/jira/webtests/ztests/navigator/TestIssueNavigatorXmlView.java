package com.atlassian.jira.webtests.ztests.navigator;

import com.atlassian.jira.functest.framework.assertions.TextAssertions;
import com.atlassian.jira.functest.framework.assertions.TextAssertionsImpl;
import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.webtests.AbstractTestIssueNavigatorXmlView;
import org.apache.commons.lang.StringUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.dom4j.DocumentException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@WebTest ({ Category.FUNC_TEST, Category.ISSUE_NAVIGATOR })
public class TestIssueNavigatorXmlView extends AbstractTestIssueNavigatorXmlView
{
    public TestIssueNavigatorXmlView(String name)
    {
        super(name);
    }

    public void testIssueNavigatorXmlViewAbsoluteIcon()
    {
        TextAssertions text = new TextAssertionsImpl();
        restoreData("TestIssueXmlViewAbsoluteIcon.xml");

        gotoFilterById(10000);
        tester.clickLink("xml");

        text.assertRegexNoMatch(getDialog().getResponseText(), "iconUrl=\"http://.*http://.*\"");

        // JRA-19099: url must also be escaped!
        text.assertRegexNoMatch(getDialog().getResponseText(), "iconUrl=\".*\\?x=y&z=a");
        text.assertRegexMatch(getDialog().getResponseText(), "iconUrl=\".*\\?x=y&amp;z=a");
    }

    /*
     * Checks that the issue navigators XML view has a link back to the original filter search and displays
     * the expected issues. (Checks 'reset=true' is not part of the url for a filter search as it is ignored JRA-12036)
     */
    public void testIssueNavigatorXmlViewLinksToFilter() throws IOException, ParserConfigurationException, SAXException, TransformerException, DocumentException
    {
        //make a filter search
        getNavigation().issueNavigator().displayAllIssues();
        selectOption("type", "New Feature");
        submit("show");
        assertIssueNavigatorDisplaying("1", "1", "1");
        assertLinkPresentWithText("Feature 00");
        String filterId = Integer.toString(saveFilter("xmlview", ""));
        gotoFilter("xmlview");
        assertIssueNavigatorDisplaying("1", "1", "1");
        assertLinkPresentWithText("Feature 00");

        //goto the xml view
        clickLinkWithText("XML");
        String previousViewUrl = assertAndGetLinkToFilterWithId(filterId);
        //follow the link and check correct results
        gotoPage(previousViewUrl);
        assertIssueNavigatorDisplaying("1", "1", "1");
        assertLinkPresentWithText("Feature 00");
    }

    public void testIssueXmlView()
    {
        reconfigureTimetracking(FORMAT_PRETTY);
        execIssueXmlView();
    }

    public void testIssueXmlViewDaysTimeFormat()
    {
        reconfigureTimetracking(FORMAT_DAYS);
        execIssueXmlView();
    }

    public void testIssueXmlViewHoursTimeFormat()
    {
        reconfigureTimetracking(FORMAT_HOURS);
        execIssueXmlView();
    }

    private void execIssueXmlView()
    {
        log("Issue Navigator: Test that the IssueView XML page correctly shows the custom field information.");

        try
        {
            for (Iterator i = items.iterator(); i.hasNext();)
            {
                TestIssueNavigatorXmlView.Item item = (TestIssueNavigatorXmlView.Item) i.next();
                String key = (String) item.getAttributeMap().get("key");
                log("testing item key = " + key + "...");

                beginAt("/secure/Dashboard.jspa");
                clickLink("find_link");
                submit("show");
                clickLinkWithText(key);
                clickLinkWithText("XML");

                String responseText = getDialog().getResponse().getText();
                Document doc = XMLUnit.buildControlDocument(responseText);
                String xpath = "//channel[title='" + XML_TITLE + "'][contains(link,'" + getEnvironmentData().getBaseUrl() + "')][description='"
                               + XML_DESCRIPTION_SINGLE + "'][language='" + XML_LANGUAGE + "']";

                log("Searching for existence of xpath " + xpath);
                XMLAssert.assertXpathExists(xpath, doc);

                execTestOnAttributes(item, doc);
                execTestOnComments(item, doc);
                execTestOnLinks(item, doc);
                execTestOnComponents(item, doc);
                execTestOnCustomFields(item, doc);
                execTestOnAttachements(item, doc);
            }
        }
        catch (Exception e)
        {
            log("Failed to parse the XML for the custom field.", e);
            fail();
        }

    }

    public void testXMLViewAllItems()
    {
        reconfigureTimetracking(FORMAT_PRETTY);
        execXMLViewAllItems();
    }

    public void testXMLViewAllItemsDaysTimeFormat()
    {
        reconfigureTimetracking(FORMAT_DAYS);
        execXMLViewAllItems();
    }

    public void testXMLViewAllItemsHoursTimeFormat()
    {
        reconfigureTimetracking(FORMAT_HOURS);
        execXMLViewAllItems();
    }

    public void execXMLViewAllItems()
    {
        log("Issue Navigator: Test that the XML page correctly shows the custom field information.");
        beginAt("/secure/IssueNavigator!switchView.jspa?navType=simple&reset=true");
//        beginAt("/secure/Dashboard.jspa");
//        clickLink("find_link");
        submit("show");
        clickLink("xml");
        try
        {
            String responseText = getDialog().getResponse().getText();
            Document doc = XMLUnit.buildControlDocument(responseText);
            String xpath = "//channel[title='" + XML_TITLE + "'][contains(link,'" + getEnvironmentData().getBaseUrl() + XML_LINK_MULTIPLE + "')][description='" +
                           XML_DESCRIPTION_MULTIPLE + "'][language='" + XML_LANGUAGE + "']";
            log("Searching for existence of xpath " + xpath);
            XMLAssert.assertXpathExists(xpath, doc);
            for (Iterator i = items.iterator(); i.hasNext();)
            {
                log("testing item...");
                TestIssueNavigatorXmlView.Item item = (TestIssueNavigatorXmlView.Item) i.next();
                execTestOnAttributes(item, doc);
                execTestOnComments(item, doc);
                execTestOnLinks(item, doc);
                execTestOnComponents(item, doc);
                execTestOnCustomFields(item, doc);
                execTestOnAttachements(item, doc);
            }

        }
        catch (Exception e)
        {
            log("Failed to parse the XML for the custom field.", e);
            fail();
        }
    }

    /* Executes test on issue tag
     * JRA-15127
     */
    public void testIssueXMLViewForIssueCountTag()
    {
        log("Issue Navigator: Test that the XML page shows correct values for the <issue> tag");

        int tempMax, start = 0;
        // iterate and check some combinations of start and tempMax
        for (tempMax = 0; tempMax < items.size() + 3; tempMax++)
        {
            // when tempMax is more than the item size, test different start issue
            if (tempMax > items.size())
            {
                start = items.size() / 2;
            }

            checkIssueCountTag(start, tempMax);
        }
    }

    /**
     * Executes the assertions on the attributes of an item of the XML
     *
     * @param item item
     * @param doc  document
     * @throws javax.xml.transform.TransformerException if exceptional condition happens during the transformation process
     */
    private void execTestOnLinks(TestIssueNavigatorXmlView.Item item, Document doc) throws TransformerException
    {
        TestIssueNavigatorXmlView.IssueLinks links = item.getLinks();
        if (links != null)
        {
            String key = item.getAttribute("key");
            String xpath = "//item[key='" + key + "']/issuelinks/issuelinktype[@id='" + links.getId() + "'][name='"
                           + links.getName() + "']";
            log("Searching for existence of xpath " + xpath);
            XMLAssert.assertXpathExists(xpath, doc);

            if (links.getOutLinks() != null && !links.getOutLinks().isEmpty())
            {
                xpath = "//item[key='" + key + "']/issuelinks/issuelinktype[@id='" + links.getId()
                        + "']/outwardlinks[@description='" + links.getOutDesc() + "']";
                for (Iterator i = links.getOutLinks().iterator(); i.hasNext();)
                {
                    TestIssueNavigatorXmlView.IssueLink link = (TestIssueNavigatorXmlView.IssueLink) i.next();
                    String xxpath = xpath + "/issuelink/issuekey[@id='" + link.getId() + "'][.='" + link.getLink() + "']";
                    log("Searching for existence of xpath " + xxpath);
                    XMLAssert.assertXpathExists(xxpath, doc);
                }
            }
            else
            {
                xpath = "//item[key='" + key + "']/issuelinks/issuelinktype[@id='" + links.getId() + "']/outwardlinks";
                log("Searching for existence of xpath " + xpath);
                XMLAssert.assertXpathNotExists(xpath, doc);
            }
            if (links.getInLinks() != null && !links.getInLinks().isEmpty())
            {
                xpath = "//item[key='" + key + "']/issuelinks/issuelinktype[@id='" + links.getId()
                        + "']/inwardlinks[@description='" + links.getInDesc() + "']";
                for (Iterator i = links.getInLinks().iterator(); i.hasNext();)
                {
                    TestIssueNavigatorXmlView.IssueLink link = (TestIssueNavigatorXmlView.IssueLink) i.next();
                    String xxpath = xpath + "/issuelink/issuekey[@id='" + link.getId() + "'][.='" + link.getLink() + "']";
                    log("Searching for existence of xpath " + xxpath);
                    XMLAssert.assertXpathExists(xxpath, doc);
                }
            }
            else
            {
                xpath = "//item[key='" + key + "']/issuelinks/issuelinktype[@id='" + links.getId() + "']/inwardlinks";
                log("Searching for existence of xpath " + xpath);
                XMLAssert.assertXpathNotExists(xpath, doc);
            }
        }
    }

    /**
     * Executes the assertions on the attributes of an item of the XML
     *
     * @param item item
     * @param doc  document
     * @throws javax.xml.transform.TransformerException if exceptional condition happens during the transformation process
     */
    private void execTestOnAttributes(TestIssueNavigatorXmlView.Item item, Document doc) throws TransformerException
    {
        Map attributeMap = item.getAttributeMap();
        if (!attributeMap.isEmpty())
        {
            StringBuffer xpath = new StringBuffer("//item");
            for (Iterator i = attributeMap.entrySet().iterator(); i.hasNext();)
            {
                Map.Entry entry = (Map.Entry) i.next();
                String key = (String) entry.getKey();
                if (TestIssueNavigatorXmlView.ATT_DATE_CREATED.equals(key) || TestIssueNavigatorXmlView.ATT_DATE_UPDATED.equals(key)
                    || TestIssueNavigatorXmlView.ATT_DATE_DUE.equals(key) || TestIssueNavigatorXmlView.ATT_DATE_RESOLVED.equals(key))
                {
                    xpath.append("[");
                    xpath.append(key);
                    xpath.append("=*");
                    xpath.append("]");
                }
                else if (TestIssueNavigatorXmlView.ATT_TIMEORIGINALESTIMATE_DAYS.equals(key)
                         || TestIssueNavigatorXmlView.ATT_TIMEORIGINALESTIMATE_HOURS.equals(key)
                         || TestIssueNavigatorXmlView.ATT_TIMEORIGINALESTIMATE.equals(key))
                {
                    if (TestIssueNavigatorXmlView.ATT_TIMEORIGINALESTIMATE_DAYS.equals(key) && FORMAT_DAYS.equals(timeFormat)
                        || TestIssueNavigatorXmlView.ATT_TIMEORIGINALESTIMATE_HOURS.equals(key) && FORMAT_HOURS.equals(timeFormat)
                        || TestIssueNavigatorXmlView.ATT_TIMEORIGINALESTIMATE.equals(key) && FORMAT_PRETTY.equals(timeFormat))
                    {
                        xpath.append("[");
                        xpath.append(TestIssueNavigatorXmlView.ATT_TIMEORIGINALESTIMATE);
                        xpath.append("='");
                        xpath.append(entry.getValue());
                        xpath.append("'");
                        xpath.append("]");
                    }
                }
                else if (TestIssueNavigatorXmlView.ATT_TIMESPENT_DAYS.equals(key)
                         || TestIssueNavigatorXmlView.ATT_TIMESPENT_HOURS.equals(key)
                         || TestIssueNavigatorXmlView.ATT_TIMESPENT.equals(key))
                {
                    if (TestIssueNavigatorXmlView.ATT_TIMESPENT_DAYS.equals(key) && FORMAT_DAYS.equals(timeFormat)
                        || TestIssueNavigatorXmlView.ATT_TIMESPENT_HOURS.equals(key) && FORMAT_HOURS.equals(timeFormat)
                        || TestIssueNavigatorXmlView.ATT_TIMESPENT.equals(key) && FORMAT_PRETTY.equals(timeFormat))
                    {
                        xpath.append("[");
                        xpath.append(TestIssueNavigatorXmlView.ATT_TIMESPENT);
                        xpath.append("='");
                        xpath.append(entry.getValue());
                        xpath.append("'");
                        xpath.append("]");
                    }
                }
                else if (TestIssueNavigatorXmlView.ATT_REMAINING_ESTIMATE_DAYS.equals(key)
                         || TestIssueNavigatorXmlView.ATT_REMAINING_ESTIMATE_HOURS.equals(key)
                         || TestIssueNavigatorXmlView.ATT_REMAINING_ESTIMATE.equals(key))
                {
                    if (TestIssueNavigatorXmlView.ATT_REMAINING_ESTIMATE_DAYS.equals(key) && FORMAT_DAYS.equals(timeFormat)
                        || TestIssueNavigatorXmlView.ATT_REMAINING_ESTIMATE_HOURS.equals(key) && FORMAT_HOURS.equals(timeFormat)
                        || TestIssueNavigatorXmlView.ATT_REMAINING_ESTIMATE.equals(key) && FORMAT_PRETTY.equals(timeFormat))
                    {
                        xpath.append("[");
                        xpath.append(TestIssueNavigatorXmlView.ATT_REMAINING_ESTIMATE);
                        xpath.append("='");
                        xpath.append(entry.getValue());
                        xpath.append("'");
                        xpath.append("]");
                    }
                }
                else
                {
                    xpath.append("[");
                    xpath.append(key);
                    xpath.append("='");
                    xpath.append(entry.getValue());
                    xpath.append("'");
                    xpath.append("]");
                }
            }
            log("Searching for existence of xpath " + xpath);
            XMLAssert.assertXpathExists(xpath.toString(), doc);

            final Map allAttributesMap = item.getAllAttributeAttributesMap();
            for (Iterator i = allAttributesMap.entrySet().iterator(); i.hasNext();)
            {
                xpath = new StringBuffer("//item/");
                Map.Entry attributeEntry = (Map.Entry) i.next();
                xpath.append(attributeEntry.getKey()); // attribute (tag) name
                final Map attributeAttributes = (Map) attributeEntry.getValue();
                for (Iterator j = attributeAttributes.entrySet().iterator(); j.hasNext();)
                {
                    Map.Entry attEntry = (Map.Entry) j.next();
                    xpath.append("[@");
                    xpath.append(attEntry.getKey()); // attribute name
                    xpath.append("='");
                    xpath.append(attEntry.getValue()); // attribute value
                    xpath.append("']");
                }
                log("Searching for existence of xpath " + xpath);
                XMLAssert.assertXpathExists(xpath.toString(), doc);
            }
        }
    }

    /**
     * Executes the assertions on the comment part of an item of the XML
     *
     * @param item item
     * @param doc  document
     * @throws javax.xml.transform.TransformerException if exceptional condition happens during the transformation process
     */
    private void execTestOnComments(TestIssueNavigatorXmlView.Item item, Document doc) throws TransformerException
    {
        List commentList = item.getComments();
        if (commentList != null && !commentList.isEmpty())
        {
            for (Iterator i = commentList.iterator(); i.hasNext();)
            {
                TestIssueNavigatorXmlView.Comment comment = (TestIssueNavigatorXmlView.Comment) i.next();
                StringBuffer xpath = new StringBuffer();
                xpath.append("//item[key='");
                xpath.append(item.getAttribute("key"));
                xpath.append("']/comments/comment");
                xpath.append("[@author='");
                xpath.append(comment.getAuthor());
                xpath.append("'][@created]");
                if (StringUtils.isNotBlank(comment.getLevel()))
                {
                    xpath.append("[@grouplevel='");
                    xpath.append(comment.getLevel());
                    xpath.append("']");
                }
                xpath.append("[. = '");
                xpath.append(comment.getValue());
                xpath.append("']");
                log("Searching for existence of xpath " + xpath);
                XMLAssert.assertXpathExists(xpath.toString(), doc);
            }
        }
        else
        {
            String xpath = "//item[key='" + item.getAttribute("key") + "']/comments";
            log("Searching for existence of xpath " + xpath);
            XMLAssert.assertXpathNotExists(xpath, doc);
        }
    }

    /**
     * Executes the assertions on the attachment part of an item of the XML
     *
     * @param item item
     * @param doc  document
     * @throws javax.xml.transform.TransformerException if exceptional condition happens during the transformation process
     */
    private void execTestOnAttachements(TestIssueNavigatorXmlView.Item item, Document doc) throws TransformerException
    {
        List attachmentList = item.getAttachments();
        if (attachmentList != null && !attachmentList.isEmpty())
        {
            for (Iterator i = attachmentList.iterator(); i.hasNext();)
            {
                String attachment = (String) i.next();
                StringBuffer xpath = new StringBuffer();
                xpath.append("//item[key='");
                xpath.append(item.getAttribute("key"));
                xpath.append("']/attachments/attachment");
                xpath.append("[@name='");
                xpath.append(attachment);
                xpath.append("']");
                log("Searching for existence of xpath " + xpath);
                XMLAssert.assertXpathExists(xpath.toString(), doc);
            }
        }
        else
        {
            String xpath = "//item[key='" + item.getAttribute("key") + "']/attachments/attachment";
            log("Searching for existence of xpath " + xpath);
            XMLAssert.assertXpathNotExists(xpath, doc);
        }
    }


    /**
     * Executes the assertions on the components part of an item of the XML
     *
     * @param item item
     * @param doc  document
     * @throws javax.xml.transform.TransformerException if exceptional condition happens during the transformation process
     */
    private void execTestOnComponents(TestIssueNavigatorXmlView.Item item, Document doc) throws TransformerException
    {
        List componentList = item.getComponents();
        if (componentList != null && !componentList.isEmpty())
        {
            for (Iterator i = componentList.iterator(); i.hasNext();)
            {
                String component = (String) i.next();
                StringBuffer xpath = new StringBuffer();
                xpath.append("//item[key='");
                xpath.append(item.getAttribute("key"));
                xpath.append("'][component='");
                xpath.append(component);
                xpath.append("']");
                log("Searching for existence of xpath " + xpath);
                XMLAssert.assertXpathExists(xpath.toString(), doc);
            }
        }
        else
        {
            String xpath = "//item[key='" + item.getAttribute("key") + "']/component";
            log("Searching for existence of xpath " + xpath);
            XMLAssert.assertXpathNotExists(xpath, doc);
        }
    }

    /**
     * Executes the assertions on the custom field part on an item of the XML
     *
     * @param item item
     * @param doc  document
     * @throws javax.xml.transform.TransformerException if exceptional condition happens during the transformation process
     */
    private void execTestOnCustomFields(TestIssueNavigatorXmlView.Item item, Document doc) throws TransformerException
    {
        List customFieldList = item.getCustomFields();
        for (Iterator i = customFieldList.iterator(); i.hasNext();)
        {
            TestIssueNavigatorXmlView.CustomField customField = (TestIssueNavigatorXmlView.CustomField) i.next();
            StringBuffer xpath = new StringBuffer();
            xpath.append("//item/customfields/customfield[@id='");
            xpath.append(customField.getId());
            xpath.append("'][customfieldname='");
            xpath.append(customField.getName());
            xpath.append("'][customfieldvalues");
            String customFieldKey = customField.getKey();
            if (TestIssueNavigatorXmlView.TYPE_DATETIME.equals(customFieldKey) || TestIssueNavigatorXmlView.TYPE_DATEPICKER.equals(customFieldKey))
            {
                for (Iterator j = customField.getValues().iterator(); j.hasNext(); j.next())
                {
                    xpath.append("[customfieldvalue=*]");
                }
            }
            else
            {
                for (Iterator j = customField.getValues().iterator(); j.hasNext();)
                {
                    CustomField.Value value = (CustomField.Value) j.next();
                    xpath.append("[customfieldvalue='");
                    xpath.append(value.getValue());
                    xpath.append("']");
                }
            }
            xpath.append("]");
            log("Searching for existence of xpath " + xpath);
            XMLAssert.assertXpathExists(xpath.toString(), doc);
        }
    }

    // JRA-15127
    private void checkIssueCountTag(int start, int tempMax)
    {
        gotoPage("/sr/jira.issueviews:searchrequest-xml/temp/SearchRequest.xml?sorter/field=issuekey&sorter/order=DESC&pager/start=" + start + "&tempMax=" + tempMax);
        try
        {
            String responseText = getDialog().getResponse().getText();

            int end = Math.min(start + tempMax, items.size());
            Document doc = XMLUnit.buildControlDocument(responseText);
            String xpath = "//issue[@start='" + start + "']" +
                           "[@end='" + end + "']" +
                           "[@total='" + items.size() + "']";
            log("Searching for existence of xpath " + xpath);
            XMLAssert.assertXpathExists(xpath, doc);
        }
        catch (Exception e)
        {
            log("Failed to parse the rss for issue counts", e);
            fail();
        }
    }

}
