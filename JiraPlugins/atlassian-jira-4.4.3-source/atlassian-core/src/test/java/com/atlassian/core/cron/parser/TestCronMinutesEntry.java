package com.atlassian.core.cron.parser;

import junit.framework.TestCase;

/**
 *
 */
public class TestCronMinutesEntry extends TestCase
{


    public void testMinutes()
    {
        assertValidValue(5);
        assertValidValue(10);
        assertValidValue(15);
        assertValidValue(20);
        assertValidValue(25);
        assertValidValue(55);
    }

    private void assertValidValue(int value)
    {
        CronMinutesEntry cronMinutesEntry = new CronMinutesEntry(Integer.toString(value));
        assertTrue(cronMinutesEntry.isValid());
        assertEquals(value, cronMinutesEntry.getRunOnce());

    }

    public void testInvalids()
    {
        assertFalse(new CronMinutesEntry("0/16").isValid());
        assertFalse(new CronMinutesEntry("1/15").isValid());
        assertFalse(new CronMinutesEntry("5/15").isValid());
        assertFalse(new CronMinutesEntry("0/5").isValid());
        assertFalse(new CronMinutesEntry("0/0").isValid());
    }

    public void testOutOfRange()
    {
        assertFalse(new CronMinutesEntry("13").isValid());
        assertFalse(new CronMinutesEntry("1").isValid());
        assertFalse(new CronMinutesEntry("60").isValid());
        assertFalse(new CronMinutesEntry("80").isValid());
        assertFalse(new CronMinutesEntry("100").isValid());
        assertFalse(new CronMinutesEntry("-5").isValid());

    }

}
