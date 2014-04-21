package com.atlassian.core.util;

import org.junit.Test;
import static org.junit.Assert.*;
import com.atlassian.jira.local.ListeningTestCase;
import com.mockobjects.servlet.MockHttpServletRequest;

public class TestWebRequestUtils extends ListeningTestCase
{
    @Test
    public void testBadBrowserNoHeader()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        assertTrue(!WebRequestUtils.isGoodBrowser(request));
    }

    @Test
    public void testBadBrowserMozilla()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setupAddHeader("USER-AGENT", "Mozilla");
        assertTrue(!WebRequestUtils.isGoodBrowser(request));
    }

    @Test
    public void testGoodBrowser()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setupAddHeader("USER-AGENT", "Mozilla 5.0 (MSIE)");
        assertTrue(WebRequestUtils.isGoodBrowser(request));
    }

    @Test
    public void testLinuxBasedPlatform()
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setupAddHeader("USER-AGENT", "Mozilla/5.0 (X11; U; Linux x86_64; en-US) AppleWebKit/533.2 (KHTML, like Gecko) Chrome/5.0.342.9 Safari/533.2");
        assertEquals(WebRequestUtils.LINUX,WebRequestUtils.getBrowserOperationSystem(request));
        assertTrue(WebRequestUtils.isGoodBrowser(request));
    }
}
