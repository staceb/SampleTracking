package com.atlassian.jira.rest.api;

import javax.annotation.Nullable;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Utility class for formatting and parsing date and date/time objects in ISO8601 format.
 *
 * @since v4.2
 */
public class Dates
{
    /**
     * The format used for dates in the REST plugin.
     */
    public static final String DATE_FORMAT = "yyyy-MM-dd";

    /**
     * The format used for times in the REST plugin.
     */
    public static final String TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";

    /**
     * Converts the given Date object to a String. The returned string is in the format <code>{@value
     * #TIME_FORMAT}</code>.
     *
     * @param date a Date
     * @return a String representation of the date and time
     */
    public static String asTimeString(@Nullable Date date)
    {
        return date != null ? new SimpleDateFormat(TIME_FORMAT).format(date) : null;
    }

    /**
     * Converts the given Timestamp object to a String. The returned string is in the format <code>{@value
     * #TIME_FORMAT}</code>.
     *
     * @param timestamp a Date
     * @return a String representation of the timestamp and time
     */
    public static String asTimeString(@Nullable Timestamp timestamp)
    {
        return timestamp != null ? Dates.asTimeString(new Date(timestamp.getTime())) : null;
    }

    /**
     * Converts the given date and time String to a Date object. The time parameter is expected to be in the format
     * <code>{@value #TIME_FORMAT}</code>.
     *
     * @param time a String representation of a date and time
     * @return a Date
     * @throws RuntimeException if there is an error parsing the date
     * @throws IllegalArgumentException if the input string is not in the expected format
     */
    public static Date fromTimeString(@Nullable String time) throws IllegalArgumentException
    {
        try
        {
            return time != null ? new SimpleDateFormat(TIME_FORMAT).parse(time) : null;
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException("Error parsing time: " + time, e);
        }
    }

    /**
     * Converts the given Date object to a String. The returned string is in the format <code>{@value
     * #DATE_FORMAT}</code>.
     *
     * @param date a Date
     * @return a String representation of the date
     */
    public static String asDateString(@Nullable Date date)
    {
        return date != null ? new SimpleDateFormat(DATE_FORMAT).format(date) : null;
    }

    /**
     * Converts the given Timestamp object to a String. The returned string is in the format <code>{@value
     * #DATE_FORMAT}</code>.
     *
     * @param timestamp a Date
     * @return a String representation of the timestamp
     */
    public static String asDateString(@Nullable Timestamp timestamp)
    {
        return timestamp != null ? asDateString(new Date(timestamp.getTime())) : null;
    }

    /**
     * Converts the given date String into a Date object. The date parameter is expected to be in the format
     * <code>{@value #DATE_FORMAT}</code>.
     *
     * @param date a String containing a date
     * @return a Date
     * @throws IllegalArgumentException if the input string is not in the expected format
     */
    public static Date fromDateString(@Nullable String date) throws IllegalArgumentException
    {
        try
        {
            return date != null ? new SimpleDateFormat(DATE_FORMAT).parse(date) : null;
        }
        catch (ParseException e)
        {
            throw new IllegalArgumentException("Error parsing date string: " + date, e);
        }
    }

    private Dates()
    {
        // prevent instantiation
    }
}
