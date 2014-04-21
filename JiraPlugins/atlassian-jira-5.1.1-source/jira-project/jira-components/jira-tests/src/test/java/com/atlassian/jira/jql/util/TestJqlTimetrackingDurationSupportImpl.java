package com.atlassian.jira.jql.util;

import com.atlassian.core.util.DateUtils;
import com.atlassian.core.util.InvalidDurationException;
import com.atlassian.jira.jql.operand.QueryLiteral;
import com.atlassian.jira.local.ListeningTestCase;
import com.google.common.base.Function;
import org.junit.Before;
import org.junit.Test;

import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @since v4.0
 */
public class TestJqlTimetrackingDurationSupportImpl extends ListeningTestCase
{
    private JqlTimetrackingDurationSupportImpl support;

    @Before
    public void setupTest()
    {
        support = new JqlTimetrackingDurationSupportImpl(new Function<String, Long>() {

            @Override
            public Long apply(@Nullable String from)
            {
                try
                {
                    return DateUtils.getDuration(from);
                }
                catch (InvalidDurationException e)
                {
                    return null;
                }
            }
        });
    }

    @Test
    public void testValidate() throws Exception
    {
        assertInvalid("aaa");
        assertInvalid("-aaa");
        assertInvalid("0.0");
        assertInvalid("-0.0");
        assertInvalid("");
        assertInvalid("     ");

        assertValid("30");
        assertValid("30");
        assertValid("30m");
        assertValid("30m");
        assertValid("1h");
        assertValid("1h");
        assertValid("1h 30m");
        assertValid("1h 30m");
        assertInvalid("-30");
        assertInvalid("-30m");
        assertInvalid("-1h");
        assertInvalid("-1h 30m");
    }

    @Test
    public void testConvertToDuration() throws Exception
    {
        assertConverted(30L * 60, 30L);
        assertConverted(30L * 60, "30m");
        assertConverted(60L * 60, "1h");
        assertConverted(90L * 60, "1h 30m");

        // bad value
        assertNull(support.convertToDuration("xxxx"));
        assertNull(support.convertToDuration(""));
        assertNull(support.convertToDuration("      "));
    }

    @Test
    public void testConvertToIndexValue() throws Exception
    {
        assertIndexValue("000000000001e0", 30L);
        assertIndexValue("000000000001e0", "30m");
        assertIndexValue("000000000002s0", "1h");
        assertIndexValue("00000000000460", "1h 30m");

        // bad value
        assertNull(support.convertToIndexValue("xxxx"));
        assertNull(support.convertToIndexValue(""));
        assertNull(support.convertToIndexValue("     "));
        assertNull(support.convertToIndexValue(new QueryLiteral()));
    }

    private void assertConverted(Long expected, Object input)
    {
        Long actual = null;
        if (input instanceof Long)
        {
            actual = support.convertToDuration((Long) input);
        }
        else if (input instanceof String)
        {
            actual = support.convertToDuration((String) input);
        }
        assertEquals(expected, actual);
    }

    private void assertIndexValue(String expected, Object input)
    {
        String actual = null;
        if (input instanceof Long)
        {
            actual = support.convertToIndexValue((Long) input);
        }
        else if (input instanceof String)
        {
            actual = support.convertToIndexValue((String) input);
        }
        assertEquals(expected, actual);
    }

    private void assertValid(String durationString) throws InvalidDurationException
    {
        assertTrue(String.format("'%s' should be valid.", durationString), support.validate(durationString));
    }

    private void assertInvalid(String durationString)
    {
        assertFalse(String.format("'%s' should not be valid.", durationString), support.validate(durationString));
    }
}
