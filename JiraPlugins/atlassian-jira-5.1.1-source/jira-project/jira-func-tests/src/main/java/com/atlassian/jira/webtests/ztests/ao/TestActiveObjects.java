package com.atlassian.jira.webtests.ztests.ao;

import com.atlassian.jira.functest.framework.FuncTestCase;
import com.atlassian.jira.functest.framework.suite.Category;
import com.atlassian.jira.functest.framework.suite.WebTest;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.client.apache.ApacheHttpClient;
import com.sun.jersey.client.apache.config.ApacheHttpClientConfig;
import com.sun.jersey.client.apache.config.DefaultApacheHttpClientConfig;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.codehaus.jackson.jaxrs.JacksonJaxbJsonProvider;
import org.codehaus.jackson.map.annotate.JsonSerialize;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

/**
 * Simple sanity test for AO. 
 *
 * @since v4.4
 */
@WebTest ({ Category.FUNC_TEST, Category.PLUGINS, Category.ACTIVE_OBJECTS })
public class TestActiveObjects extends FuncTestCase
{
    private static final GenericType<List<Blog>> BLOG_LIST = new GenericType<List<Blog>>(){};

    private Client client;

    @Override
    protected void setUpTest()
    {
        ApacheHttpClientConfig config = new DefaultApacheHttpClientConfig();
        config.getProperties().put(ApacheHttpClientConfig.PROPERTY_PREEMPTIVE_AUTHENTICATION, Boolean.TRUE);
        config.getState().setCredentials(null, null, -1, "admin", "admin");
        config.getClasses().add(JacksonJaxbJsonProvider.class);

        client = ApacheHttpClient.create(config);
    }

    @Override
    protected void tearDownTest()
    {
        client.destroy();
    }

    //AO-210: AO does not support Oracle with Long IDs.
    public void testCreateEntryWithLongId() throws Exception
    {
        final String AUTHOR = "bride";
        final String TEXT = "You an I have unfinished business!";

        final WebResource resource = createResource();
        deleteAll(resource);

        Blog newBlog = new Blog();
        newBlog.setAuthor(AUTHOR);
        newBlog.setText(TEXT);
        newBlog.setComments(Collections.<Comment>emptyList());

        ClientResponse response = resource.type(MediaType.APPLICATION_JSON_TYPE).post(ClientResponse.class, newBlog);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        List<Blog> blogs = resource.get(BLOG_LIST);
        assertEquals(1, blogs.size());

        final Blog savedBlog = blogs.get(0);
        assertEquals(AUTHOR, savedBlog.author);
        assertEquals(TEXT, savedBlog.text);
        assertNotNull(savedBlog.id);
    }

    //AO-178: AO does not cleanly drop tables on restore.
    public void testCheckAOTablesDeletedOnRestore() throws Exception
    {
        WebResource resource = createResource();

        deleteAll(resource);

        Blog newBlog = new Blog();
        newBlog.setAuthor("bill");
        newBlog.setText("I'm the man");
        newBlog.setComments(Collections.<Comment>emptyList());

        ClientResponse response = resource.type(MediaType.APPLICATION_JSON).post(ClientResponse.class, newBlog);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        //We need this slow restore as AO keeps a cache around that can make this test flakey. This cache is
        // kept in a weak reference such that:
        // - If GC occurs after the restore then the test will pass.
        // - If GC does not occur after the restore then the test will not pass.
        administration.restoreDataSlowOldWay("blankprojects.xml");

        List<Blog> blogs = resource.get(BLOG_LIST);
        assertTrue("Blogs should have been cleared on restore.", blogs.isEmpty());
    }

    private void deleteAll(WebResource resource)
    {
        final ClientResponse deleteResponse = resource.delete(ClientResponse.class);
        assertEquals(Response.Status.NO_CONTENT.getStatusCode(), deleteResponse.getStatus());
    }

    private WebResource createResource()
    {
        return client.resource(environmentData.getBaseUrl().toExternalForm()).path("rest")
            .path("func-test").path("latest").path("blog");
    }

    @JsonSerialize (include = JsonSerialize.Inclusion.NON_NULL)
    public static class Blog
    {
        private Long id;
        private String author;
        private String text;
        private List<Comment> comments;

        public String getAuthor()
        {
            return author;
        }

        public void setAuthor(String author)
        {
            this.author = author;
        }

        public String getText()
        {
            return text;
        }

        public void setText(String text)
        {
            this.text = text;
        }

        public List<Comment> getComments()
        {
            return comments;
        }

        public void setComments(List<Comment> comments)
        {
            this.comments = comments;
        }

        public Long getId()
        {
            return id;
        }

        public void setId(Long id)
        {
            this.id = id;
        }

        @Override
        public String toString()
        {
            return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
        }
    }

    public static class Comment
    {
    }
}
