package com.atlassian.support.tools.salext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import junit.framework.TestCase;

import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.message.I18nResolver;
import com.atlassian.sal.api.user.UserManager;
import com.atlassian.sisyphus.SisyphusPatternSource;
import com.atlassian.support.tools.request.CreateSupportRequestAction;
import com.atlassian.templaterenderer.TemplateRenderer;

public class TestConfluenceApplicationInfo extends TestCase
{
	private ConfluenceApplicationInfo info;

	private final static I18nResolver mockResolver = mock(I18nResolver.class);
	private final static UserManager mockUserManager = mock(UserManager.class);
	private final static ApplicationProperties mockApplicationProperties = mock(ApplicationProperties.class);
	private final static TemplateRenderer mockTemplateRenderer = mock(TemplateRenderer.class);

	@Override
	protected void setUp() throws Exception
	{
		super.setUp();

		when(mockApplicationProperties.getHomeDirectory()).thenReturn(new File("/tmp"));
		when(mockApplicationProperties.getDisplayName()).thenReturn("MockApplication");

		this.info = new ConfluenceApplicationInfo(mockApplicationProperties, mockResolver, mockUserManager, mockTemplateRenderer, null, null, null, null);
	}

	public void testConfluenceRegexPath() throws Exception
	{
		SisyphusPatternSource source = this.info.getPatternSource();
		assertNotNull(source);
	}

	public void testApplicationLogFilePath()
	{
		File homeDir = mockApplicationProperties.getHomeDirectory();
		File logFile = new File(homeDir.getAbsolutePath() + "/logs/atlassian-confluence.log");

		try
		{
			logFile.mkdirs();
			logFile.createNewFile();
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}

		assertFalse("Log file path is null when a file exists.", this.info.getApplicationLogFilePaths().isEmpty());

		logFile.delete();

		assertTrue("Log file path is not null when no log file is found.", this.info.getApplicationLogFilePaths().isEmpty());
	}

	public void testCreateSupportRequestEmail()
	{
		assertTrue("Create support request email is not valid.", CreateSupportRequestAction.isValidEmail(this.info.getCreateSupportRequestEmail()));
	}

	public void testApplicationName()
	{
		assertNotNull("getApplicationName() returns null", this.info.getApplicationName());
	}

	public void testApplicationHome()
	{
		assertNotNull("getApplicationHome() returns null", this.info.getApplicationHome());
	}
}