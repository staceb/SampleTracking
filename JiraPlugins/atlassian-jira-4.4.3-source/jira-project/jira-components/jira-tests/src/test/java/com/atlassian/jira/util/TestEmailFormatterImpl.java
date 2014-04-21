package com.atlassian.jira.util;

import com.atlassian.jira.local.MockControllerTestCase;
import org.junit.Test;
import static org.junit.Assert.*;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.config.properties.APKeys;
import org.easymock.MockControl;

/**
 * @since v3.13
 */
public class TestEmailFormatterImpl extends MockControllerTestCase
{
    private static final String USER_EXAMPLE_ORG = "user@example.org";
    private static final String USER_EXAMPLE_ORG_MASKED = "user at example dot org";

    @Test
    public void testFormatEmailAsLinkEncoding()
    {
        MockControl mockApplicationPropertiesControl = MockControl.createControl(ApplicationProperties.class);
        ApplicationProperties mockApplicationProperties = (ApplicationProperties) mockApplicationPropertiesControl.getMock();
        mockApplicationProperties.getString("jira.option.emailvisible");
        mockApplicationPropertiesControl.setDefaultReturnValue("show");
        mockApplicationPropertiesControl.replay();


        EmailFormatterImpl emailFormatter = new EmailFormatterImpl(mockApplicationProperties);

        String email = emailFormatter.formatEmailAsLink("test@example.com", null);
        assertEquals("<a href=\"mailto:test@example.com\">test@example.com</a>", email);

        email = emailFormatter.formatEmailAsLink("\"<script>alert('owned')</script>\"@localhost", null);
        assertEquals("<a href=\"mailto:&quot;&lt;script&gt;alert(&#39;owned&#39;)&lt;/script&gt;&quot;@localhost\">&quot;&lt;script&gt;alert(&#39;owned&#39;)&lt;/script&gt;&quot;@localhost</a>", email);

        mockApplicationPropertiesControl.verify();
    }

    @Test
    public void testFormatEmailHidden() throws Exception
    {
        final ApplicationProperties applicationProperties = getMock(ApplicationProperties.class);
        expect(applicationProperties.getString(APKeys.JIRA_OPTION_EMAIL_VISIBLE)).andReturn("hidden").times(2);

        final EmailFormatterImpl formatter = instantiate(EmailFormatterImpl.class);

        assertNull(formatter.formatEmail(USER_EXAMPLE_ORG, true));
        assertNull(formatter.formatEmail(USER_EXAMPLE_ORG, false));
    }

    @Test
    public void testFormatEmailPublic() throws Exception
    {
        final ApplicationProperties applicationProperties = getMock(ApplicationProperties.class);
        expect(applicationProperties.getString(APKeys.JIRA_OPTION_EMAIL_VISIBLE)).andReturn("show").times(2);

        final EmailFormatterImpl formatter = instantiate(EmailFormatterImpl.class);

        assertEquals(USER_EXAMPLE_ORG, formatter.formatEmail(USER_EXAMPLE_ORG, true));
        assertEquals(USER_EXAMPLE_ORG, formatter.formatEmail(USER_EXAMPLE_ORG, false));
    }

    @Test
    public void testFormatEmailMasked() throws Exception
    {
        final ApplicationProperties applicationProperties = getMock(ApplicationProperties.class);
        expect(applicationProperties.getString(APKeys.JIRA_OPTION_EMAIL_VISIBLE)).andReturn("mask").times(2);

        final EmailFormatterImpl formatter = instantiate(EmailFormatterImpl.class);

        assertEquals(USER_EXAMPLE_ORG_MASKED, formatter.formatEmail(USER_EXAMPLE_ORG, true));
        assertEquals(USER_EXAMPLE_ORG_MASKED, formatter.formatEmail(USER_EXAMPLE_ORG, false));
    }

    @Test
    public void testFormatEmailLoggedInOnly() throws Exception
    {
        final ApplicationProperties applicationProperties = getMock(ApplicationProperties.class);
        expect(applicationProperties.getString(APKeys.JIRA_OPTION_EMAIL_VISIBLE)).andReturn("user").times(2);

        final EmailFormatterImpl formatter = instantiate(EmailFormatterImpl.class);

        assertEquals(USER_EXAMPLE_ORG, formatter.formatEmail(USER_EXAMPLE_ORG, true));
        assertNull(formatter.formatEmail(USER_EXAMPLE_ORG, false));
    }
}
