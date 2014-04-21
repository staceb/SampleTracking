package com.atlassian.jira.webtests.ztests.email;

import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.atlassian.jira.webtests.EmailFuncTestCase;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * @since v4.0
 */
@WebTest ({ Category.FUNC_TEST, Category.EMAIL })
// passing now
public class TestBulkHeaders extends EmailFuncTestCase
{
    /**
     * Verify that we actually add Precedence: Bulk and Auto-Submitted: auto-generated headers
     * to all outgoing mail by default
     */
    public void testBulkHeadersPresent() throws InterruptedException, MessagingException
    {
        administration.restoreData("TestIssueNotifications.xml");
        configureAndStartSmtpServer();

        //delete an issue
        final String issueId = "COW-1";
        navigation.issue().viewIssue(issueId);
        tester.clickLink("delete-issue");
        tester.submit("Delete");

        //there should 2 notifications, because of the issue's security level
        flushMailQueueAndWait(2);
        final MimeMessage[] mimeMessages = getGreenMail().getReceivedMessages();
        assertEquals(2, mimeMessages.length);

        for (MimeMessage msg : mimeMessages) {
            String[] prec = msg.getHeader("Precedence");
            String[] auto = msg.getHeader("Auto-Submitted");

            assertEquals("bulk", prec[0]);
            assertEquals("auto-generated", auto[0]);
        }
    }

    /**
     * Verify that we do not generate Precedence: Bulk and Auto-Submitted headers
     * when the jira.option.precedence.header.exclude property is set.
     */
    public void testBulkHeadersNotPresent() throws InterruptedException, MessagingException
    {
        administration.restoreData("TestStripBulk.xml");
        configureAndStartSmtpServer();

        //delete an issue
        final String issueId = "MKY-1";
        navigation.issue().viewIssue(issueId);
        tester.clickLink("delete-issue");
        tester.submit("Delete");

        //there should 2 notifications, because of the issue's security level
        flushMailQueueAndWait(3);
        final MimeMessage[] mimeMessages = getGreenMail().getReceivedMessages();
        assertEquals(3, mimeMessages.length);

        for (MimeMessage msg : mimeMessages) {
            assertNull(msg.getHeader("Precedence"));
            assertNull(msg.getHeader("Auto-Submitted"));
        }
    }
}
