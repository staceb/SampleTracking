package com.atlassian.jira.plugins.mail.handlers;

import org.junit.Test;
import org.ofbiz.core.entity.GenericEntityException;

import javax.mail.MessagingException;

import static org.junit.Assert.assertEquals;

public class TestNonQuotedCommentHandler extends AbstractTestCommentHandler
{

    @Override
    protected AbstractMessageHandler createHandler()
    {
        return new NonQuotedCommentHandler(permissionManager, issueUpdater, userManager,
                applicationProperties, jiraApplicationContext, mailLoggingManager, messageUserProcessor);
    }

    @Test
    public void testMailWithPrecedenceBulkHeader() throws MessagingException, GenericEntityException
    {
        _testMailWithPrecedenceBulkHeader();
    }

    @Test
    public void testMailWithIllegalPrecedenceBulkHeader() throws Exception
    {
        _testMailWithIllegalPrecedenceBulkHeader();
    }

    @Test
    public void testMailWithDeliveryStatusHeader() throws MessagingException, GenericEntityException
    {
        _testMailWithDeliveryStatusHeader();
    }

    @Test
    public void testMailWithAutoSubmittedHeader() throws MessagingException, GenericEntityException
    {
        _testMailWithAutoSubmittedHeader();
    }

    @Test
    public void testMailWithCatchEmailMiss()
            throws Exception
    {
        _testCatchEmailSettings();
    }

    @Test
    public void testAddCommentOnly() throws MessagingException, GenericEntityException
    {
        _testAddCommentOnly();
    }

    @Test
    public void testAddCommentOnlyToMovedIssue() throws MessagingException, GenericEntityException
    {
        setupMovedIssue();
        _testAddCommentOnlyToMovedIssue();
    }

    @Test
    public void testAddCommentAndAttachment() throws MessagingException, GenericEntityException
    {
        _testAddCommentAndAttachment();
    }

    @Test
    public void testAddAttachmentWithInvalidFilename() throws MessagingException, GenericEntityException
    {
        _testAddMultipleAttachmentsWithInvalidAndValidFilenames();
    }

    @Test
    public void testAddCommentWithEmptyBodyAndAttachment() throws MessagingException, GenericEntityException
    {
        _testAddCommentWithNonMultipartAttachment();
    }

    @Test
    public void testAddCommentWithInlineAttachment() throws GenericEntityException, MessagingException
    {
        _testAddCommentWithNonMultipartInline();
    }

    @Test
    public void testStripQuoteLines()
    {
        NonQuotedCommentHandler nc = new NonQuotedCommentHandler();
        assertEquals("", nc.stripQuotedLines("> Real comment"));
        assertEquals("", nc.stripQuotedLines("> Line one\n> Line two"));
        assertEquals("", nc.stripQuotedLines("> Line one\n> Line two\n> Line three"));
        assertEquals("", nc.stripQuotedLines(""));
        assertEquals("Real comment\n", nc.stripQuotedLines("Real comment"));
        assertEquals("foo\nbar\n", nc.stripQuotedLines("foo\nbar"));
        assertEquals("foo\n\nbar\n", nc.stripQuotedLines("foo\n\nbar"));
        assertEquals("foo\nbar\n", nc.stripQuotedLines("foo\n> intervening\nbar"));
        assertEquals("two\n", nc.stripQuotedLines("Attribution:\n> foo\ntwo\n> bar\n> baz"));
        assertEquals("\nfoo\n\n\n", nc.stripQuotedLines("\nfoo\n\nOn Wed 12 Jul, Fred wrote:\n\n> blah blah\n> blah"));
        assertEquals("Real comment\n", nc.stripQuotedLines("On Wed, 8am, Joe Bloggs said:\n> First quoted line\nReal comment\n> Quoted line"));
        assertEquals("\nThe real comment\n", nc.stripQuotedLines("Joe Bloggs wrote:\n> blah\n> blah\n\nThe real comment"));
        assertEquals("Precomment\n\n\n\nThe real comment\n", nc.stripQuotedLines("Precomment\n\n\nJoe Bloggs wrote:\n> blah\n> blah\n\nThe real comment"));
        // Real-life example
        assertEquals("Another comment on TP-58\n\n--Jeff\n\n\n", nc.stripQuotedLines("Another comment on TP-58\n" +
                "\n" +
                "--Jeff\n" +
                "\n" +
                "\n" +
                "On Sat, Aug 07, 2004 at 06:37:39PM +1000, Test (JIRA) wrote:\n" +
                ">      [ http://localhost:8091/browse/TP-58?page=comments#comment-10770 ]\n" +
                ">      \n" +
                "> New Comment on TP-58 by Test\n" +
                "> ----------------------------\n" +
                "> \n" +
                "> A test modification to TP-58\n" +
                "> \n" +
                "> On Sat, Aug 07, 2004 at 06:32:50PM +1000, Test (JIRA) wrote:\n" +
                "> \n" +
                "> \n" +
                "> > Test Issue\n" +
                "> > ----------\n" +
                "> >\n" +
                "> >          Key: TP-58\n" +
                "> >          URL: http://localhost:8091/browse/TP-58\n" +
                "> >      Project: TestProj\n" +
                "> >         Type: Bug\n" +
                "> >   Components: foocomponent  \n" +
                "> >     Reporter: Test\n" +
                "> >     Assignee: Test\n" +
                "> >\n" +
                "> >\n" +
                "> > blahl\n" +
                "> > blah\n" +
                "> > bla\n" +
                "> \n" +
                "> -- \n" +
                "> This message is automatically generated by JIRA.\n" +
                "> -\n" +
                "> If you think it was sent incorrectly contact one of the administrators:\n" +
                ">    http://localhost:8091/secure/Administrators.jspa\n" +
                "> -\n" +
                "> If you want more information on JIRA, or have a bug to report see:\n" +
                ">    http://www.atlassian.com/software/jira"));
    }
}
