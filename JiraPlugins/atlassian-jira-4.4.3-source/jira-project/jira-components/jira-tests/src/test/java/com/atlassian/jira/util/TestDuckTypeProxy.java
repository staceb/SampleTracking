package com.atlassian.jira.util;

import com.atlassian.jira.local.ListeningTestCase;
import org.junit.Test;
import static org.junit.Assert.*;

import com.atlassian.core.test.util.DuckTypeProxy;

public class TestDuckTypeProxy extends ListeningTestCase
{
    public interface MyInterface
    {
        String getString();
        Long getLong();
        Integer getInteger();
        String get(String string);
        String get(Integer integer);
    }

    @Test
    public void testProxyReturns() throws Exception
    {
        Object obj = new Object()
        {
            public String getString()
            {
                return "who's you're daddy?";
            }
        };
        MyInterface impl = (MyInterface) DuckTypeProxy.getProxy(MyInterface.class, EasyList.build(obj), DuckTypeProxy.THROW);
        assertEquals("who's you're daddy?", impl.getString());
    }

    @Test
    public void testProxyThrows() throws Exception
    {
        MyInterface impl = (MyInterface) DuckTypeProxy.getProxy(MyInterface.class, EasyList.build(new Object()), DuckTypeProxy.THROW);
        try
        {
            impl.getString();
            fail("should have thrown USOE");
        }
        catch (UnsupportedOperationException yay)
        {
        }
    }

    @Test
    public void testProxyDelegatesToSecond() throws Exception
    {
        Object obj = new Object()
        {
            public String getString()
            {
                return "who's you're daddy?";
            }
        };
        MyInterface impl = (MyInterface) DuckTypeProxy.getProxy(MyInterface.class, EasyList.build(new Object(), obj), DuckTypeProxy.THROW);
        assertEquals("who's you're daddy?", impl.getString());
    }

    @Test
    public void testNotNullParameter() throws Exception
    {
        Object obj = new Object()
        {
            public String get(String string)
            {
                return "how about: " + string;
            }
        };
        MyInterface impl = (MyInterface) DuckTypeProxy.getProxy(MyInterface.class, EasyList.build(obj), DuckTypeProxy.THROW);
        assertEquals("how about: me", impl.get("me"));
    }

    @Test
    public void testNullParameter() throws Exception
    {
        Object obj = new Object()
        {
            public String get(String string)
            {
                return "how about: " + string;
            }
        };
        MyInterface impl = (MyInterface) DuckTypeProxy.getProxy(MyInterface.class, EasyList.build(obj), DuckTypeProxy.THROW);
        assertEquals("how about: null", impl.get((String) null));
    }
}
