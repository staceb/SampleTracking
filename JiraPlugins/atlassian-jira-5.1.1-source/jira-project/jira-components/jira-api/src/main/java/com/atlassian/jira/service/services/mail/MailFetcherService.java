/*
 * Copyright (c) 2002-2004
 * All rights reserved.
 */

package com.atlassian.jira.service.services.mail;

import com.atlassian.annotations.Internal;
import com.atlassian.configurable.ObjectConfigurable;
import com.atlassian.configurable.ObjectConfiguration;
import com.atlassian.configurable.ObjectConfigurationException;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.exception.DataAccessException;
import com.atlassian.jira.mail.Email;
import com.atlassian.jira.mail.MailLoggingManager;
import com.atlassian.jira.service.services.file.AbstractMessageHandlingService;
import com.atlassian.jira.service.util.handler.MessageHandler;
import com.atlassian.jira.service.util.handler.MessageHandlerContext;
import com.atlassian.jira.service.util.handler.MessageHandlerExecutionMonitor;
import com.atlassian.jira.template.VelocityTemplatingEngine;
import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.PortUtil;
import com.atlassian.mail.MailException;
import com.atlassian.mail.MailFactory;
import com.atlassian.mail.MailProtocol;
import com.atlassian.mail.server.MailServer;
import com.atlassian.mail.server.SMTPMailServer;
import com.google.common.annotations.VisibleForTesting;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.util.TextUtils;
import com.sun.mail.pop3.POP3Message;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.apache.velocity.exception.VelocityException;

import javax.annotation.Nullable;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import java.util.HashMap;
import java.util.Map;

import static com.atlassian.jira.template.TemplateSources.file;
import static org.apache.commons.lang.StringUtils.isBlank;

/**
 * Mail fetcher for both POP and IMAP protocols. This class is going to be hopefuly moved to JIRA Mail Plugin.
 */
@Internal
public class MailFetcherService extends AbstractMessageHandlingService implements ObjectConfigurable
{
    // introduced for at least some testability of this class
    interface MessageProvider
    {
        interface SingleMessageProcessor
        {
            boolean process(Message message, MessageHandlerContext context) throws MessagingException, MailException;
        }

        void getAndProcessMail(SingleMessageProcessor singleMessageProcessor, MailServer mailServer, MessageHandlerContext context);
    }

    interface ErrorEmailForwarder
    {
        boolean forwardEmail(Message message, MessageHandlerContext context, String toAddress, String errorsAsString, String exceptionsAsString)
                throws MailException;
    }


    private static final Logger log = ComponentAccessor.getComponent(MailLoggingManager.class).getIncomingMailChildLogger("mailfetcherservice");

    private static final String OLD_MAIL_DISABLED_KEY = "atlassian.mail.popdisabled";
    private static final String MAIL_DISABLED_KEY = "atlassian.mail.fetchdisabled";
    public static final String KEY_MAIL_SERVER = "popserver";
    protected Long mailserverId = null;
    public static final String FORWARD_EMAIL = "forwardEmail";
    protected static final String DEFAULT_FOLDER = "INBOX";
    public static final String FOLDER_NAME_KEY = "foldername";

    private static final String EMAIL_TEMPLATES = "templates/email/";
    private final ApplicationProperties applicationProperties = ComponentAccessor.getApplicationProperties();
    private final String baseUrl = applicationProperties.getString(APKeys.JIRA_BASEURL);
    private static final String ERROR_TEMPLATE = "errorinhandler.vm";
    private final ErrorEmailForwarder errorEmailForwarder;
    private final MessageProvider messageProvider;

    /**
     * Only to be used by unit tests
     */
    MailFetcherService(ErrorEmailForwarder errorEmailForwarder, MessageProvider messageProvider)
    {
        this.errorEmailForwarder = errorEmailForwarder;
        this.messageProvider = messageProvider;
    }

    public MailFetcherService()
    {
        errorEmailForwarder = new ErrorEmailForwarderImpl();
        messageProvider = new MessageProviderImpl();
    }

    public void init(PropertySet props) throws ObjectConfigurationException
    {
        super.init(props);
        if (hasProperty(KEY_MAIL_SERVER))
        {
            try
            {
                this.mailserverId = new Long(getProperty(KEY_MAIL_SERVER));
            }
            catch (Exception e)
            {
                log.error("Invalid mail server id: " + e, e);
            }
        }
    }

    protected int getPort(MailServer server)
    {
        final int parsedPort = PortUtil.parsePort(server.getPort());
        if (parsedPort >= 0)
        {
            return parsedPort;
        }
        else
        {
            log.error("Invalid port number: " + server.getPort() + " for mail service: " + getName() + ". Using the default port for this service type.");
            return -1;
        }
    }


    private class MessageProviderImpl implements MessageProvider
    {
        @Override
        public void getAndProcessMail(SingleMessageProcessor singleMessageProcessor, MailServer mailserver, MessageHandlerContext context)
        {
            log.debug("Using mail server [" + mailserver + "]");
            final String hostname = mailserver.getHostname();
            final String username = mailserver.getUsername();
            final String password = mailserver.getPassword();
            if (hostname == null || username == null || password == null)
            {
                context.getMonitor().warning("Cannot retrieve mail due to a missing parameter in Mail Server '" + mailserver.getName() + "': [host," + hostname + "],[username," + username + "],[password," + password + "]");
                return;
            }

            final Session session;
            try
            {
                session = mailserver.getSession();
            }
            catch (Exception e)
            {
                context.getMonitor().error("Cannot create mail session: " + e.getMessage(), e);
                return;
            }

            String protocol = null;
            Store store;
            try
            {
                protocol = mailserver.getMailProtocol().getProtocol();
                if (log.isDebugEnabled())
                {
                    log.debug("Getting store from the session using protocol [" + protocol + "]");
                }
                store = session.getStore(protocol);

            }
            catch (NoSuchProviderException e)
            {
                context.getMonitor().error("Error getting provider for protocol " + protocol + ": " + e, e);
                return;
            }

            try
            {
                final int port = getPort(mailserver);
                if (log.isDebugEnabled())
                {
                    log.debug("Connecting to mail store to host [" + hostname + "] and port [" + port + "]");
                }
                store.connect(hostname, port, username, password);
                log.debug("Successfully connected to mail store");
            }
            catch (MessagingException e)
            {
                context.getMonitor().error("Error connecting to host '" + hostname + "' as user '" + username + "' via protocol '"
                        + protocol + "': " + e, e);
                return;
            }

            Folder folder = null;
            try
            {
                final String folderName = getFolderName(mailserver);
                log.debug("Getting folder [" + folderName + "]");
                folder = store.getFolder(folderName);
                if (log.isDebugEnabled())
                {
                    log.debug("Got folder [" + folder + "], now opening it for read/write");
                }
                folder.open(!context.isRealRun() ? Folder.READ_ONLY : Folder.READ_WRITE);

                final Message[] messages = folder.getMessages();

                log.debug(addHandlerInfo("Found " + messages.length + " message(s) in the " + protocol + " folder"));
                if (!context.isRealRun())
                {
                    final StringBuilder sb = new StringBuilder();
                    sb.append("Found ");
                    sb.append(messages.length);
                    sb.append(" message(s) in the ");
                    sb.append(protocol);
                    sb.append(" folder.");
                    if (messages.length > MAX_READ_MESSAGES_DRY_RUN)
                    {
                        sb.append(" Only first " + MAX_READ_MESSAGES_DRY_RUN + " messages will be processed in test mode.");
                    }
                    context.getMonitor().info(sb.toString());
                }
                context.getMonitor().setNumMessages(messages.length);

                for (int i = 0, messagesLength = messages.length; i < messagesLength; i++)
                {
                    final Message message = messages[i];
                    context.getMonitor().nextMessage(message);
                    if (!context.isRealRun() && i >= MAX_READ_MESSAGES_DRY_RUN)
                    {
                        log.debug("In dry-run mode only first " + MAX_READ_MESSAGES_DRY_RUN + " messages are processed. Skipping the rest");
                        break;
                    }
                    log.debug("Processing message"); // nothing more here as any getter may throw an exception

                    boolean deleteThisMessage = false;
                    String msgId = null;
                    try
                    {
                        msgId = message.getHeader("Message-ID") != null ? message.getHeader("Message-ID")[0] : "null";
                        if (log.isDebugEnabled())
                        {
                            try
                            {
                                log.debug("Message Subject: " + message.getSubject());
                                log.debug("Message-ID: " + msgId);
                            }
                            catch (MessagingException e)
                            {
                                context.getMonitor().warning("Messaging exception thrown on getting message subject. Message may have corrupt headers.", e);
                            }
                        }

                        deleteThisMessage = singleMessageProcessor.process(message, context);
                    }
                    catch (Exception e)
                    {
                        context.getMonitor().error("Exception: " + e.getLocalizedMessage(), e);
                    }
                    finally
                    {
                        if (message != null)
                        {
                            // This fixes JRA-11046 - the pop messages hold onto the attachments in memory and since we
                            // process all the messages at once we need to make sure we only ever need one attachment
                            // in memory at a time. For IMAP this problem does not exist.
                            if (message instanceof POP3Message)
                            {
                                ((POP3Message) message).invalidate(true);
                            }

                            if (deleteThisMessage)
                            {
                                if (context.isRealRun())
                                {
                                    log.debug("Deleting Message: " + msgId);
                                    message.setFlag(Flags.Flag.DELETED, true);
                                }
                                else
                                {
                                    context.getMonitor().info("Deleting Message '" + message.getSubject() + "'");
                                    log.debug("Deleting Message: " + msgId + " (skipped due to dry-run mode)");
                                }
                            }
                        }
                    }
                }
            }
            catch (MessagingException e)
            {
                context.getMonitor().error("Messaging Exception in service '" + getClass().getName() + "' when getting mail: " + e.getMessage(), e);
            }
            finally
            {
                try
                {
                    if (folder != null)
                    {
                        log.debug("Closing folder");
                        folder.close(true); //expunge any deleted messages
                    }
                    log.debug("Closing store");
                    store.close();
                }
                catch (Exception e)
                {
                    log.debug(addHandlerInfo("Error whilst closing folder and store: " + e.getMessage()));
                }
            }
        }
    }

    /**
     * Connect to the POP / IMAPemail box and then handle each message.
     */
    @Override
    protected void runImpl(MessageHandlerContext context)
    {
        log.debug(getClass().getSimpleName() + " run() method has been called");
        if (isMailDisabled())
        {
            context.getMonitor().info("Mail is disabled.");
            return;
        }
        final MessageHandler messageHandler = getHandler();
        if (messageHandler == null)
        {
            log.error("Message Handler is not configured propertly for this service. Exiting.");
            return;
        }

        final MailServer mailserver = getMailServer(context.getMonitor());
        if (mailserver == null)
        {
            context.getMonitor().warning("no mail server returned from getMailServer(). Exiting run()");
            return;
        }

        messageProvider.getAndProcessMail(new MessageProvider.SingleMessageProcessor()
        {
            @Override
            public boolean process(Message message, final MessageHandlerContext context)
                    throws MessagingException, MailException
            {
                final ErrorAccumulatingMessageHandlerExecutionMonitor accumulatingMonitor =
                        new ErrorAccumulatingMessageHandlerExecutionMonitor(context.getMonitor());

                final MessageHandlerContext myMessageHandlerContext = new DelegatingMessageHandlerContext(context, accumulatingMonitor);

                log.debug("Calling handleMessage");
                boolean deleteThisMessage = messageHandler.handleMessage(message, myMessageHandlerContext);
                // if there is any error, forwarding is configured and we should not deleteThisMessage, then attempt a forward
                if (accumulatingMonitor.hasErrors() && forwardEmailParam() != null && !deleteThisMessage)
                {
                    final String toAddress = forwardEmailParam();
                    log.debug("Forwarding error message to '" + toAddress + "'");
                    // if the forward was successful we want to delete the email, otherwise not
                    deleteThisMessage = errorEmailForwarder.forwardEmail(message, myMessageHandlerContext, toAddress, accumulatingMonitor.getErrorsAsString(),
                            accumulatingMonitor.getExceptionsAsString());
                }
                return deleteThisMessage;
            }
        }, mailserver, context);
    }



    /**
     * Gets the mail server or null if none is defined. Will also return null if there is a problem getting the
     * mailserver.
     *
     * @return the mail server or null.
     */
    private MailServer getMailServer(MessageHandlerExecutionMonitor monitor)
    {
        MailServer mailserver = null;
        if (mailserverId != null)
        {
            try
            {
                // TODO resolve this weird inconsistency - we seem to assume that mailserverId and the return
                // TODO value from getProperty(KEY_MAIL_SERVER) will be in sync but this invariant is
                // TODO not controlled by this class. Shouldn't we use mailServerId here too?
                // TODO I don't have time to verify that this is OK to change right now
                mailserver = MailFactory.getServerManager().getMailServer(new Long(getProperty(KEY_MAIL_SERVER)));
            }
            catch (Exception e)
            {
                monitor.error("Could not retrieve mail server: " + e, e);
            }
        }
        else
        {
            monitor.error(getClass().getName() + " cannot run without a configured Mail Server");
        }
        return mailserver;
    }

    boolean isMailDisabled()
    {
        if (Boolean.getBoolean(OLD_MAIL_DISABLED_KEY))
        {
            log.info("Service disabled by '" + OLD_MAIL_DISABLED_KEY + "' property.");
            return true;
        }
        if (Boolean.getBoolean(MAIL_DISABLED_KEY))
        {
            log.info("Service disabled by '" + MAIL_DISABLED_KEY + "' property.");
            return true;
        }
        return false;
    }

    protected String getFolderName(MailServer server)
    {
        if (server.getMailProtocol().equals(MailProtocol.SECURE_IMAP)
                || server.getMailProtocol().equals(MailProtocol.IMAP))
        {
            try
            {
                return StringUtils.defaultString(getProperty(FOLDER_NAME_KEY), DEFAULT_FOLDER);
            }
            catch (ObjectConfigurationException e)
            {
                throw new DataAccessException("Error retrieving foldername.", e);
            }
        }
        else
        {
            return DEFAULT_FOLDER;
        }
    }


    private String forwardEmailParam()
    {
        try
        {
            return getProperty(FORWARD_EMAIL);
        }
        catch (ObjectConfigurationException e)
        {
            throw new DataAccessException(addHandlerInfo("Error retrieving Forward Email flag."), e);
        }
    }


    /**
     * JRA-13590 Small decorator to add the service handler name and the mail service ID to log messages to make it
     * easier if you have multiple services configured to determine which one is throwing exceptions.
     *
     * @param msg log message
     * @return log message decorated with handler name and mail server ID
     */
    protected String addHandlerInfo(String msg)
    {
        return getName() + "[" + mailserverId + "]: " + msg;
    }

    private static I18nHelper getI18nHelper()
    {
        // As this is run from a service, we do not have a user. So use the default system locale, i.e. specify null
        // for the user.
        return ComponentAccessor.getI18nHelperFactory().getInstance((User) null);
    }

    public ObjectConfiguration getObjectConfiguration() throws ObjectConfigurationException
    {
        return getObjectConfiguration("MAILFETCHERSERVICE", "services/com/atlassian/jira/service/services/mail/mailfetcherservice.xml", null);
    }

    @Override
    protected Logger getLogger()
    {
        return log;
    }

    private class ErrorEmailForwarderImpl implements ErrorEmailForwarder
    {

        /**
         * Forwards the email to the configured
         *
         * @param message to forward.
         * @param exceptionsAsString @return true if forwarding the email worked.
         * @throws MailException if there's a problem sending the mail or getting the server.
         */
        @Override
        public boolean forwardEmail(final Message message, final MessageHandlerContext context, final String toAddress,
                final String errorsAsString, final String exceptionsAsString) throws MailException
        {
            if (TextUtils.verifyEmail(toAddress))
            {
                try
                {
                    final Email email = createErrorForwardEmail(message, context.getMonitor(), toAddress, errorsAsString, exceptionsAsString);
                    sendMail(email, context, context.getMonitor());
                    return true;
                }
                catch (VelocityException e)
                {
                    context.getMonitor().error("Could not create email template for.", e);
                }
                catch (MessagingException e)
                {
                    context.getMonitor().error("Could not retrieve information from message.", e);
                }
            }
            else
            {
                context.getMonitor().warning("Forward Email is invalid.");
            }

            return false;
        }

        private void sendMail(Email email, MessageHandlerContext context, MessageHandlerExecutionMonitor messageHandlerExecutionMonitor)
                throws MailException
        {
            SMTPMailServer mailserver = MailFactory.getServerManager().getDefaultSMTPMailServer();
            if (mailserver == null)
            {
                messageHandlerExecutionMonitor.warning("You do not currently have a smtp mail server set up yet.");
            }
            else if (MailFactory.isSendingDisabled())
            {
                messageHandlerExecutionMonitor.warning("Sending mail is currently disabled in Jira.");
            }
            else
            {
                email.setFrom(mailserver.getDefaultFrom());
                if (context.isRealRun())
                {
                    log.debug("Sending mail to [" + email.getTo() + "]");
                    mailserver.send(email);
                }
                else
                {
                    messageHandlerExecutionMonitor.info("Sending mail to '" + email.getTo() + "'");
                    log.debug("Sending mail to [" + email.getTo() + "] skipped due to dry-run mode");
                }
            }
        }

        /**
         * Creates a message to be forwarded to the configured address that explains an error occurred sending the given
         * message and displays the errors.
         *
         *
         * @param message to be forwarded.
         * @param errorsAsString error message(s) which will be included in the message body (available for velocity template)
         * @param exceptionsAsString stacktrace (optional) which will be added as ErrorStackTrace.txt attachment to such issue
         * @return the email to be forwarded.
         * @throws VelocityException if there's a problem getting the email template.
         * @throws MessagingException if java mail decides so.
         */
        private Email createErrorForwardEmail(final Message message, final MessageHandlerExecutionMonitor monitor, final String toAddress,
                final String errorsAsString, @Nullable final String exceptionsAsString)
                throws VelocityException, MessagingException
        {
            final Email email = new Email(toAddress);

            email.setSubject(getI18nHelper().getText("template.errorinhandler.subject", message.getSubject()));
            final Map<String, Object> contextParams = new HashMap<String, Object>();
            contextParams.putAll(getVelocityParams(errorsAsString, monitor));

            final String body = getTemplatingEngine().
                    render(file(EMAIL_TEMPLATES + "text/" + ERROR_TEMPLATE)).applying(contextParams).asPlainText();

            // Set the error as the body of the mail
            email.setBody(body);
            final Multipart mp = new MimeMultipart();

            if (exceptionsAsString != null)
            {
                final MimeBodyPart exception = new MimeBodyPart();
                exception.setContent(exceptionsAsString, "text/plain");
                exception.setFileName("ErrorStackTrace.txt");
                mp.addBodyPart(exception);
            }

            // Attach the cloned message
            final MimeBodyPart messageAttachment = new MimeBodyPart(); //TODO add message as attachment that can be replied to and edited.
            messageAttachment.setContent(message, "message/rfc822");
            String subject = message.getSubject();
            if (isBlank(subject))
            {
                subject = "NoSubject";
            }
            messageAttachment.setFileName(subject + ".eml");
            mp.addBodyPart(messageAttachment);

            email.setMultipart(mp);

            return email;
        }


        /**
         * Creates Velocity parameters with baseline defaults as well as the given error parameter. If there's a problem
         * with retrieving the mail server name or base url, these will be absent from the returned Map.
         *
         * @param error The error to include in the parameters.
         * @return The parameters.
         */
        private Map<String, Object> getVelocityParams(String error, MessageHandlerExecutionMonitor messageHandlerExecutionMonitor)
        {
            Map<String, Object> params = new HashMap<String, Object>();

            final String handlerName = getHandler().getClass().toString();
            try
            {
                params.put("i18n", getI18nHelper());
                params.put("handlerName", handlerName);
                Long serverId = new Long(getProperty(KEY_MAIL_SERVER));
                params.put("serverName", MailFactory.getServerManager().getMailServer(serverId).getName());
                params.put("error", error);
                params.put("baseurl", ComponentAccessor.getApplicationProperties().getString(APKeys.JIRA_BASEURL));
            }
            catch (ObjectConfigurationException e)
            {
                messageHandlerExecutionMonitor.error("Could not retrieve mail server", e);
            }
            catch (MailException e)
            {
                messageHandlerExecutionMonitor.error("Could not retrieve mail server", e);
            }

            return params;
        }

        @VisibleForTesting
        VelocityTemplatingEngine getTemplatingEngine()
        {
            return ComponentAccessor.getComponent(VelocityTemplatingEngine.class);
        }
    }
}


