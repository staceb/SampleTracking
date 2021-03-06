package com.atlassian.jira.functest.framework.admin;

import com.atlassian.jira.functest.framework.LocatorFactory;
import com.atlassian.jira.functest.framework.Navigation;
import com.atlassian.jira.functest.framework.locator.CssLocator;
import com.atlassian.jira.functest.framework.locator.Locator;
import com.atlassian.jira.functest.framework.locator.XPathLocator;
import net.sourceforge.jwebunit.WebTester;
import org.apache.commons.lang.NotImplementedException;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @since v4.3
 */
public class DefaultMailServerAdministration implements MailServerAdministration
{
    private static final String MAIL_SERVERS_ADMINISTRATION_LINK_ID = "mail_servers";
    private final Navigation navigation;

    private final SmtpMailServerAdministration smtpMailServerAdministration;
    private final PopMailServerAdministration popMailServerAdministration;

    public DefaultMailServerAdministration(final WebTester tester, final Navigation navigation, final LocatorFactory locator)
    {
        this.navigation = navigation;

        this.smtpMailServerAdministration = new DefaultSmtpMailServerAdministration(this, tester, locator);
        this.popMailServerAdministration = new DefaultPopMailServerAdministration(this, locator, tester);
    }

    @Override
    public MailServerAdministration goTo()
    {
        navigation.gotoAdminSection(MAIL_SERVERS_ADMINISTRATION_LINK_ID);
        return this;
    }

    @Override
    public SmtpMailServerAdministration Smtp()
    {
        return smtpMailServerAdministration;
    }

    @Override
    public PopMailServerAdministration Pop()
    {
        return popMailServerAdministration;
    }

    /**
     *
     * @since v4.3
     */
    public static class DefaultSmtpMailServerAdministration implements SmtpMailServerAdministration
    {
        private final MailServerAdministration mailServerAdministration;

        private final WebTester tester;
        private final LocatorFactory locator;

        public DefaultSmtpMailServerAdministration(final MailServerAdministration mailServerAdministration,
                final WebTester tester, final LocatorFactory locator)
        {
            this.mailServerAdministration = mailServerAdministration;
            this.tester = tester;
            this.locator = locator;
        }

        @Override
        public boolean isPresent()
        {
            return locator.css("#smtp-mail-servers-panel").exists();
        }

        @Override
        public boolean isConfigured()
        {
            if (isPresent())
            {
                return locator.css("#smtp-mail-servers-panel tr td").getText().equals
                        ("You do not currently have an SMTP server configured. No outgoing mail will be sent.");
            }
            return false;
        }

        @Override
        public MailServerAdministration add(final String name, final String fromAddress, final String emailPrefix,
                final String hostName)
        {
            tester.clickLink("add-new-smtp-server");
            tester.setWorkingForm("jiraform");
            tester.setFormElement("name", name);
            tester.setFormElement("from", fromAddress);
            tester.setFormElement("prefix", emailPrefix);
            tester.setFormElement("serverName", hostName);
            tester.submit("Add");
            return mailServerAdministration;
        }

        @Override
        public MailServerConfiguration get()
        {
            throw new NotImplementedException("Not implemented yet");
        }
    }

    /**
     *
     * @since v4.3
     */
    public static class DefaultPopMailServerAdministration implements PopMailServerAdministration
    {
        private MailServerAdministration mailServerAdministration;

        private final LocatorFactory locator;
        private WebTester tester;

        public DefaultPopMailServerAdministration(final MailServerAdministration mailServerAdministration,
                final LocatorFactory locator, WebTester tester)
        {
            this.mailServerAdministration = mailServerAdministration;
            this.locator = locator;
            this.tester = tester;
        }

        @Override
        public MailServerAdministration add(String name, String hostName, String userName, String password)
        {
            tester.clickLink("add-pop-mail-server");
            tester.setWorkingForm("jiraform");
            tester.setFormElement("name", name);
            tester.setFormElement("serverName", hostName);
            tester.setFormElement("username", userName);
            tester.setFormElement("password", password);
            tester.submit("Add");
            return mailServerAdministration;
        }

        @Override
        public boolean isPresent()
        {
            return locator.css("#pop-mail-servers-panel").exists();
        }

        @Override
        public List<MailServerConfiguration> list()
        {
            final Locator popMailServersItemsLocator = locator.css("#pop-mail-servers-table tr");

            final List<MailServerConfiguration> results = new ArrayList<MailServerConfiguration>();

            if (isEmpty())
            {
                return Collections.emptyList();
            }

            for (Node mailServerRow : popMailServersItemsLocator.getNodes())
            {
                if (!isMailServerTableHeader(mailServerRow))
                {
                    final MailServerConfiguration mailServerConfigurationForRow =
                            new MailServerConfiguration
                                    (
                                            extractServerName(mailServerRow), extractHost(mailServerRow),
                                            extractUserName(mailServerRow)
                                    );
                    results.add(mailServerConfigurationForRow);
                }
            }
            return results;
        }

        @Override
        public EditPopServerConfiguration edit(String mailServerName)
        {
            final Node mailServerRow = discoverRowInServersTableFor(mailServerName);

            final Locator editServerLinkLocator = new XPathLocator(mailServerRow, "//*[contains(@id,'edit-pop')]");

            final String editServerLinkId = editServerLinkLocator.getNode().getAttributes().getNamedItem("id").
                    getNodeValue();

            tester.clickLink(editServerLinkId);

            return new DefaultEditPopServerConfiguration(mailServerAdministration, tester);
        }

        @Override
        public MailServerAdministration delete(String mailServerName)
        {
            final Node mailServerRow = discoverRowInServersTableFor(mailServerName);

            final Locator deleteServerLinkLocator = new XPathLocator(mailServerRow, "//*[contains(@id,'delete-pop')]");

            final String deleteServerLinkId = deleteServerLinkLocator.getNode().getAttributes().getNamedItem("id").
                    getNodeValue();

            tester.clickLink(deleteServerLinkId);
            tester.submit("Delete");
            return mailServerAdministration;
        }

        private Node discoverRowInServersTableFor(String mailServerName)
        {
            final XPathLocator serverRowsLocator = locator.xpath("//*[@id='pop-mail-servers-table']//*[@class='mail-server-name']");

            Node serverRowNodeToFind = null;

            for (Node serverRow : serverRowsLocator.getNodes())
            {
                if(serverRowsLocator.getText(serverRow).equals(mailServerName))
                {
                    serverRowNodeToFind = serverRow.getParentNode().getParentNode();
                    break;
                }
            }

            if(serverRowNodeToFind == null)
            {
                throw new CouldNotFindPopMailServerException(
                        String.format
                                (
                                        "The specified POP/IMAP Server could not be found on the page. Server Name: %s",
                                        mailServerName
                                )
                );
            }

            return serverRowNodeToFind;
        }

        private boolean isEmpty()
        {
            return locator.css("#pop-mail-servers-table tr td").getText().equals("You do not currently have any POP / IMAP servers configured.");
        }

        private boolean isMailServerTableHeader(Node mailServerRow)
        {
            return new CssLocator(mailServerRow, "th").exists();
        }

        private String extractServerName(final Node mailServerRow)
        {
            return new CssLocator(mailServerRow, "td .mail-server-name").getText();
        }

        private String extractHost(final Node mailServerRow)
        {
            return new CssLocator(mailServerRow, "td .mail-server-host").getText();
        }

        private String extractUserName(final Node mailServerRow)
        {
            return new CssLocator(mailServerRow, "td .mail-server-username").getText();
        }

        private class CouldNotFindPopMailServerException extends RuntimeException
        {
            public CouldNotFindPopMailServerException(String message)
            {
                super(message);
            }
        }
    }
}
