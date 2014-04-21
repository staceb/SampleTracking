package com.atlassian.jira.plugins.mail.handlers;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.junit.Assert;

/**
 * A helper that contains various static methods that are used by multiple different tests. To avoid duplication they are lumped in here...
 *
 * @since v3.13
 */
public class HandlerTestUtil
{

    public static void assertSubjectNotEmpty(final Message message)
    {
        try
        {
            final String subject = message.getSubject();
            Assert.assertNotNull("The message must have a subject", subject);
            Assert.assertFalse("The message subject is empty", subject.trim().length() == 0);
        }
        catch (final Exception e)
        {
            Assert.fail("Unable to get the subject from the provided message, message: \"" + e.getMessage() + "\"...");
        }
    }

    /**
     * Creates a message from the given filename.
     *
     * @param filename
     * @return a Message object built from the file
     * @throws IOException
     * @throws MessagingException
     */
    public static Message createMessageFromFile(final String filename) throws Exception
    {
        return createMimeMessageFromFile(filename);
    }

    /**
     * This helper creates a message with an option to override its subject.
     *
     * @param filename
     * @param subject  The new subject which will override the one actually found in the msg file.
     * @return a Message object built from the file
     * @throws Exception
     */
    static Message createMessageFromFile(final String filename, final String subject) throws Exception
    {
        if (StringUtils.isBlank(subject))
        {
            throw new IllegalArgumentException("The parameter:subject is null or empty.");
        }

        final MimeMessage mimeMessage = createMimeMessageFromFile(filename);
        mimeMessage.setSubject(subject);
        return mimeMessage;
    }

    private static MimeMessage createMimeMessageFromFile(final String filename) throws Exception
    {
        if (StringUtils.isBlank(filename))
        {
            throw new IllegalArgumentException("The parameter:filename is null or empty.");
        }
        if (!filename.endsWith(".msg"))
        {
            throw new IllegalArgumentException("The parameter:filename \"" + filename + "\" should end in \".msg\" .");
        }

        InputStream fis = HandlerTestUtil.class.getResourceAsStream(filename);
        try
        {
            final String testDirectory = new File(HandlerTestUtil.class.getResource("/" + HandlerTestUtil.class.getName().replace('.', '/') + ".class").getFile()).getParent();
            fis = new FileInputStream(new File(testDirectory, filename));
            return new MimeMessage(Session.getDefaultInstance(new Properties()), fis);
        }
        finally
        {
            IOUtils.closeQuietly(fis);
        }
    }

    public static MimeBodyPart createPartFromFile(String filename) throws IOException, MessagingException
    {
        if (StringUtils.isBlank(filename))
        {
            throw new IllegalArgumentException("The parameter:filename is null or empty.");
        }
        if (!filename.endsWith(".part"))
        {
            throw new IllegalArgumentException("The parameter:filename \"" + filename + "\" should end in \".part\" .");
        }

        InputStream fis = HandlerTestUtil.class.getResourceAsStream(filename);
        try
        {
            return new MimeBodyPart(fis);
        }
        finally
        {
            IOUtils.closeQuietly(fis);
        }
    }

}
