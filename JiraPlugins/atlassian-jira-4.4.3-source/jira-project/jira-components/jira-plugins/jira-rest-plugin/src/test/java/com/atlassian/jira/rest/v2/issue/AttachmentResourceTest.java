package com.atlassian.jira.rest.v2.issue;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.core.util.thumbnail.Thumbnail;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.bc.JiraServiceContext;
import com.atlassian.jira.bc.config.ConstantsService;
import com.atlassian.jira.bc.issue.attachment.AttachmentService;
import com.atlassian.jira.issue.AttachmentManager;
import com.atlassian.jira.issue.attachment.Attachment;
import com.atlassian.jira.issue.thumbnail.ThumbnailManager;
import com.atlassian.jira.local.testutils.MultiTenantContextTestUtils;
import com.atlassian.jira.mock.ofbiz.MockGenericValue;
import com.atlassian.jira.rest.v2.issue.builder.BeanBuilderFactory;
import com.atlassian.jira.rest.v2.issue.context.ContextI18n;
import com.atlassian.jira.rest.v2.issue.context.ContextUriInfo;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.user.MockUser;
import com.atlassian.jira.user.util.OSUserConverter;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.jira.util.velocity.VelocityRequestContextFactory;
import com.opensymphony.module.propertyset.memory.MemoryPropertySet;
import junit.framework.TestCase;
import org.apache.commons.httpclient.HttpStatus;
import org.easymock.EasyMock;
import org.ofbiz.core.entity.GenericValue;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;

import static com.atlassian.jira.util.dbc.Assertions.notNull;
import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.createMock;
import static org.easymock.classextension.EasyMock.replay;

/**
 * @since v4.2
 */
public class AttachmentResourceTest extends TestCase
{
        static final String JIRA_BASE_URI = "http://localhost:8090/jira/";

    protected UserManager userManager;
    private JiraAuthenticationContext authContext;
    private ConstantsService constantsService;
    private VelocityRequestContextFactory velocityReqCtxFactory;
    private ResourceUriBuilder uriBuilder;
    private BeanBuilderFactory beanBuilderFactory;
    private AttachmentManager attachmentManager;
    private ContextI18n i18n;
    private AttachmentService attachmentService;
    private ThumbnailManager thumbnailManager;
    private ContextUriInfo contextUriInfo;


    public void testAttachmentResourceWithAThumbnailWithSpacesInTheFileName() throws Exception
    {
        final long attachmentId = 100;
        final GenericValue attachment1 = new MockGenericValue("FileAttachment", EasyMap.build("id", attachmentId, "issue", new Long(1), "filename", "a file with spaces in the name",
                "filesize", 123L));

        final User user = new MockUser("mockUser", "Mock User", "mock@user.org");

        MemoryPropertySet propertySet = new MemoryPropertySet();
        propertySet.init(null, null);
        propertySet.setString("mime-type", "img/png");

        final Attachment attachment = new Attachment(null, attachment1, propertySet);

        AttachmentBeanBuilder attachmentBeanBuilder = new AttachmentBeanBuilder(
                new URI(JIRA_BASE_URI), userManager, thumbnailManager, attachment);

        final String thumbnailName = "my name with space.png";
        final Thumbnail thumbnail = new Thumbnail(100, 200, thumbnailName, attachmentId, Thumbnail.MimeType.PNG);

        expect(authContext.getLoggedInUser()).andReturn(user).anyTimes();
        expect(attachmentManager.attachmentsEnabled()).andReturn(Boolean.TRUE).anyTimes();
        expect(attachmentService.getAttachment(any(JiraServiceContext.class), EasyMock.eq(new Long(attachmentId)))).andReturn(attachment).anyTimes();
        expect(thumbnailManager.getThumbnail(attachment)).andReturn(thumbnail).anyTimes();
        expect(beanBuilderFactory.attachmentBean(attachment)).andReturn(attachmentBeanBuilder);
        expect(contextUriInfo.getBaseUriBuilder()).andReturn(UriBuilder.fromUri(JIRA_BASE_URI)).anyTimes();
        expect(userManager.getUser(any(String.class))).andReturn(OSUserConverter.convertToOSUser(user)).anyTimes();

        replayMocks();

        final AttachmentResource attachmentResource = new AttachmentResource(attachmentService, attachmentManager,
                authContext, beanBuilderFactory, i18n, contextUriInfo);
        final Response resp = attachmentResource.getAttachment("100");
        assertEquals(HttpStatus.SC_OK, resp.getStatus());
        final AttachmentBean attachmentBean = (AttachmentBean) resp.getEntity();

        assertTrue(attachmentBean.getThumbnail().startsWith(JIRA_BASE_URI));
        assertEquals("there should be now space characters", -1, attachmentBean.getThumbnail().indexOf(' '));
        assertEquals(123, attachmentBean.getSize());

    }

    @Override
    protected void setUp() throws Exception
    {
        MultiTenantContextTestUtils.setupMultiTenantSystem();
        authContext = createMock(JiraAuthenticationContext.class);
        constantsService = createMock(ConstantsService.class);
        velocityReqCtxFactory = createMock(VelocityRequestContextFactory.class);
        uriBuilder = createMock(ResourceUriBuilder.class);
        beanBuilderFactory = createMock(BeanBuilderFactory.class);
        contextUriInfo = createMock(ContextUriInfo.class);
        i18n = createMock(ContextI18n.class);
        userManager = createMock(UserManager.class);
        thumbnailManager = createMock(ThumbnailManager.class);
        attachmentManager = createMock(AttachmentManager.class);
        attachmentService = createMock(AttachmentService.class);
    }

    protected void replayMocks(Object... mocks)
    {
        replay(mocks);
        replay(
                beanBuilderFactory,
                contextUriInfo,
                i18n,
                userManager,
                thumbnailManager,
                attachmentManager,
                attachmentService,
                authContext,
                constantsService,
                velocityReqCtxFactory,
                uriBuilder
        );
    }

    /* copied from com.atlassian.jira.mock.matcher.EasyMockMatcherUtils which is not accessible by this project */
    private static <T> T any(Class<T> argumentType)
    {
        notNull("argumentType", argumentType);
        return argumentType.cast(EasyMock.anyObject());
    }


}
