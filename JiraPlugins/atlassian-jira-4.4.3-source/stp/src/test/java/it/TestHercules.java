package it;

import com.atlassian.selenium.browsers.AutoInstallClient;
import com.thoughtworks.selenium.Wait;

public class TestHercules extends AbstractSupportTestCase
{
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
	}

	public void testStartPage()
	{
		this.client.open(this.baseURL);
		this.client.waitForPageToLoad();

		this.client.click("xpath=//a[@href='#hercules-module']");
		AutoInstallClient.assertThat().elementPresent("id=hercules-module");
		AutoInstallClient.assertThat().textPresent("Hercules searches");
	}

	public void testInvalidLogFile()
	{
		this.client.open(this.baseURL);
		this.client.waitForPageToLoad();

		this.client.click("xpath=//a[@href='#hercules-module']");
		this.client.select("id=herculesLogToggle", "value=Custom Log");
		this.client.type("id=logFilePath", "/tmp/foo/bar/baz");
		this.client.clickButton("Scan", false);	
		this.client.click("xpath=//a[@href='#hercules-module']");
		
		waitForHerculesResults();
		
		AutoInstallClient.assertThat().textPresent("You must provide the location of a valid log file.");
	}

	public void testScanEmptyLog()
	{
		this.client.open(this.baseURL);
		this.client.waitForPageToLoad();

		this.client.click("xpath=//a[@href='#hercules-module']");
		String sampleFile = System.getProperty("plugin.test.directory") + "/empty.log";
		this.client.select("id=herculesLogToggle", "value=Custom Log");
		this.client.type("id=logFilePath", sampleFile);
		this.client.clickButton("Scan", false);
		
		waitForHerculesResults();

		AutoInstallClient.assertThat().textPresent("Hercules didn't find any known problems in the following log file");
	}



// FIXME: Add a sample log with at least one match for each product, so that we can test positive results	
//	public void testScanDefaultLog()
//	{
//		client.open(baseURL);
//		client.waitForPageToLoad(30000);
//
//		client.click("xpath=//a[@href='#hercules-module']");
//		client.clickButton("Scan", false);
//
//		waitForHerculesResults();
//		AutoInstallClient.assertThat().textPresent("Showing entries");
//
//		client.clickButton("Start Again", false);
//		client.waitForAjaxWithJquery();
//
//		AutoInstallClient.assertThat().textPresent("Log Scanner");
//	}

	/*
	 * Test the javascript toggle between default and custom logs
	 */
	public void testToggleCustomLog()
	{
		this.client.open(this.baseURL);
		this.client.waitForPageToLoad();
		
		this.client.click("xpath=//a[@href='#hercules-module']");

//		<select id="herculesLogToggle" onchange="herculesToggleLogOptions()">
//		<option value="default" selected="selected">Your standard log file will be scanned.</option>
//		<option value="custom">Enter the location of a log file (on your application server).</option>
//		</select>
		
		AutoInstallClient.assertThat().textPresent("You will scan your standard log file.");

		// change the select that controls the log location
		this.client.select("id=herculesLogToggle", "value=Custom Log");

		AutoInstallClient.assertThat().elementVisible("id=logFilePath");

		// Click to toggle the wording back the other way
		this.client.select("id=herculesLogToggle", "value=regexp:.+\\.log");

		AutoInstallClient.assertThat().elementNotVisible("id=logFilePath");
	}

	// FIXME: This test can intermittently fail if the scan is run too quickly,
	// i.e. if the scan completes before the browser can hit the cancel button
	// public void testCancelScan()
	// {
	// String baseURL = System.getProperty("baseurl");
	// final SeleniumClient client = AutoInstallClient.seleniumClient();
	// client.open(baseURL + "/plugins/servlet/support-tools/view/hercules/");
	// client.clickButton("Scan", false);
	// client.waitForPageToLoad();
	// AutoInstallClient.assertThat().textPresent("Scanning Your Log");
	//
	// client.clickLinkWithText("Cancel", false);
	//
	// client.waitForPageToLoad();
	// AutoInstallClient.assertThat().textPresent("Hercules Log Scanner");
	// }
	
	protected void waitForHerculesResults()
	{
		// Wait for the script to finish and for the results to be displayed
		Wait wait = new Wait()
		{
			@Override
			public boolean until()
			{
				return (TestHercules.this.client.isElementPresent("id=stp-hercules-results") || TestHercules.this.client.isElementPresent("css=#hercules-module div.error") || TestHercules.this.client.isTextPresent("Hercules didn't find any known problems"));
			}
		};
		wait.wait("Timed out waiting for Hercules results");
	}
}
