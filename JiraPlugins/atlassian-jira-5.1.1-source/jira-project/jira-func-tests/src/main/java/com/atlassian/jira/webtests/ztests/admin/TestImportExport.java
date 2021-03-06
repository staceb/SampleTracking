package com.atlassian.jira.webtests.ztests.admin;

import com.atlassian.jira.functest.framework.locator.WebPageLocator;
import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.webtests.JIRAWebTest;
import com.atlassian.jira.webtests.LicenseKeys;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.io.FileUtils;

import java.io.File;

/**
 * Test to see that importing an xml file into JIRA and then exporting from it results in an XML file
 *
 * Uses the following xml files:
 * TestImportExport.xml
 * TestImportExport2.xml
 */
@WebTest({Category.FUNC_TEST, Category.ADMINISTRATION, Category.IMPORT_EXPORT })
public class TestImportExport extends JIRAWebTest
{

    public TestImportExport(String name)
    {
        super(name);
    }

    @Override
    public void setUp()
    {
        super.setUp();
        turnOffDangerMode();
    }

    @Override
    public void tearDown()
    {
        turnOnDangerMode();
        super.tearDown();
    }

    public void testXmlImportFromNonImportDirectory() throws Exception
    {
        File data = new File(getEnvironmentData().getXMLDataLocation(), "EmptyJira.xml");

        // write new data to temp file
        File newData = File.createTempFile("testXmlImportFromNonImportDirectory", ".xml"); //This will be created in the /tmp directory
        try
        {
            FileUtils.copyFile(data, newData);

            tester.gotoPage("secure/admin/XmlRestore!default.jspa");
            tester.setWorkingForm("jiraform");
            tester.setFormElement("filename", newData.getAbsolutePath());
            tester.submit();

            tester.assertTextPresent("Could not find file at this location");
        }
        finally
        {
            newData.delete();
        }
    }

    public void testXmlImportWithInvalidIndexDirectory() throws Exception
    {
        //By creating a file for the index path, we'll force the failure of the index path directory creation
        File indexPath = File.createTempFile("testXmlImportWithInvalidIndexDirectory", null);
        indexPath.createNewFile();
        indexPath.deleteOnExit();

        final String absolutePath = indexPath.getAbsolutePath();
        try {
            administration.restoreDataWithReplacedTokens("TestSetupInvalidIndexPath.xml", ImmutableMap.of("@@INDEX_PATH@@", absolutePath));
        }
        catch(AssertionError e) {
            //We're expecting an AssertionError here to inform us that the import was not successful
        }
        tester.assertTextPresent("Cannot write to index directory. Check that the application server and JIRA have permissions to write to: " + absolutePath);
    }

    public void testXmlImportWithInvalidAttachmentsDirectory() throws Exception
    {
        //By creating a file for the index path, we'll force the failure of the index path directory creation
        File attachmentPath = File.createTempFile("testXmlImportWithInvalidAttachmentsDirectory", null);
        attachmentPath.createNewFile();
        attachmentPath.deleteOnExit();

        final String absolutePath = attachmentPath.getAbsolutePath();
        try {
            administration.restoreDataWithReplacedTokens("TestSetupInvalidAttachmentPath.xml", ImmutableMap.of("@@ATTACHMENT_PATH@@", absolutePath));
        }
        catch(AssertionError e) {
            //We're expecting an AssertionError here to inform us that the import was not successful
        }
        tester.assertTextPresent("Cannot write to attachment directory. Check that the application server and JIRA have permissions to write to: " + absolutePath);
    }

    public void testXmlImportFromFuture()
    {
        File file = new File(getEnvironmentData().getXMLDataLocation().getAbsolutePath() + "/" + "TestXmlImportFromFuture.xml");
        copyFileToJiraImportDirectory(file);
        gotoPage("secure/admin/XmlRestore!default.jspa");
        setWorkingForm("jiraform");
        setFormElement("filename", file.getName());
        setFormElement("license", LicenseKeys.V2_COMMERCIAL.getLicenseString());
        submit();
        administration.waitForRestore();
        assertTextNotPresent("Your project has been successfully imported");
        assertTextPresent("The xml data you are trying to import seems to be from a newer version of JIRA. This will not work.");
    }

    public void testXmlImportWithAV1LicenseInIt() throws Exception
    {
        File file = new File(getEnvironmentData().getXMLDataLocation().getAbsolutePath() + "/" + "oldlicense.xml");
        copyFileToJiraImportDirectory(file);
        gotoPage("secure/admin/XmlRestore!default.jspa");
        setWorkingForm("jiraform");
        setFormElement("filename", file.getName());
        submit();
        administration.waitForRestore();
        text.assertTextPresent(new WebPageLocator(tester), "Invalid license key specified.");
    }
}
