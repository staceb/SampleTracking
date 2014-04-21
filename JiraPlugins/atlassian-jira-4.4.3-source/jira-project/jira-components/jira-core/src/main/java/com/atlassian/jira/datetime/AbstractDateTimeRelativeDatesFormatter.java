package com.atlassian.jira.datetime;

import com.atlassian.core.util.Clock;
import com.atlassian.jira.bc.JiraServiceContextImpl;
import com.atlassian.jira.config.properties.APKeys;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.timezone.TimeZoneInfo;
import com.atlassian.jira.timezone.TimeZoneService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Days;
import org.joda.time.LocalDate;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 *
 * @since v4.4
 */
abstract class AbstractDateTimeRelativeDatesFormatter implements DateTimeFormatStrategy
{
    private final DateTimeFormatterServiceProvider serviceProvider;
    private final ApplicationProperties applicationProperties;
    private final TimeZoneService timeZoneService;
    private final JiraAuthenticationContext jiraAuthenticationContext;
    private DateTimeFormatStrategy relativeDatesFormatter;
    private DateTimeFormatStrategy datesFormatter;
    private final Clock clock;

    public AbstractDateTimeRelativeDatesFormatter(
            DateTimeFormatterServiceProvider serviceProvider,
            ApplicationProperties applicationProperties,
            TimeZoneService timeZoneService,
            JiraAuthenticationContext jiraAuthenticationContext,
            DateTimeFormatStrategy relativeDateFormatter,
            DateTimeFormatStrategy datesFormatter,
            Clock clock)
    {
        this.serviceProvider = serviceProvider;
        this.applicationProperties = applicationProperties;
        this.timeZoneService = timeZoneService;
        this.jiraAuthenticationContext = jiraAuthenticationContext;
        this.relativeDatesFormatter = relativeDateFormatter;
        this.datesFormatter = datesFormatter;
        this.clock = clock;
    }

    @Override
    public String format(DateTime dateTime, Locale locale)
    {
        TimeZoneInfo userTimeZoneInfo = timeZoneService.getUserTimeZoneInfo(new JiraServiceContextImpl(jiraAuthenticationContext.getLoggedInUser()));
        DateTime referenceDateTime = new DateTime(clock.getCurrentDate(), DateTimeZone.forTimeZone(userTimeZoneInfo.toTimeZone()));

        final String dateI18nKey = RelativeFormatter.getDayI18nKey(dateTime, referenceDateTime);
        final boolean formatterRelative = applicationProperties.getOption(APKeys.JIRA_LF_DATE_RELATIVE);
        if (formatterRelative && (dateI18nKey != null))
        {
            String dateString = relativeDatesFormatter.format(dateTime, locale);
            return serviceProvider.getText(dateI18nKey, dateString);
        }
        return datesFormatter.format(dateTime, locale);
    }

    @Override
    public Date parse(String text, DateTimeZone timeZone, Locale locale)
    {
         throw new UnsupportedOperationException();
    }

    @Override
    public abstract DateTimeStyle style();

    @Override
    public String pattern()
    {
        throw new UnsupportedOperationException();
    }

    public static class RelativeFormatter
    {
        /**
         * Returns the appropriate i18n key for the "smart" representation of the date relative to the reference date.
         * E.g. if the date is 3rd October 2008 (Friday), and the reference date is 10th October 2008 (Friday), the
         * return value will be the i18n key for "Last Friday".
         *
         * The range of dates where smart representation will be applied is 1 week either side of the reference date. If
         * the date falls outside of this range, null will be returned to mean that the caller should fall back to DMY
         * formatting of the date.
         *
         * @param theDate       the date to format
         * @param referenceDate the date of reference
         * @return i18n key, or null
         */
        public static String getDayI18nKey(final DateTime theDate, final DateTime referenceDate)
        {
            // note: need to use LocalDate objects here instead of DateTime because otherwise the daysBetween calculation
            // can be affected by Daylight Savings Time.
            final LocalDate refDt = createLocalDate(referenceDate.toDate(), referenceDate.getZone().toTimeZone());
            final LocalDate timeDt = createLocalDate(theDate.toDate(), theDate.getZone().toTimeZone());
            final int daysBetween = Days.daysBetween(refDt, timeDt).getDays();

            if (daysBetween < -7)
            {
                // DMY in the past
                return null;
            }
            else if (daysBetween < -1)
            {
                // LAST WEEK DAY
                return "common.date.relative.days.last." + timeDt.dayOfWeek().get();
            }
            else if (daysBetween == -1)
            {
                // YESTERDAY
                return "common.concepts.yesterday";
            }
            else if (daysBetween == 0)
            {
                // TODAY
                return "common.concepts.today";
            }
            else if (daysBetween == 1)
            {
                // TOMORROW
                return "common.concepts.tomorrow";
            }
            else if (daysBetween <= 7)
            {
                // THIS (next) WEEK DAY
                return "common.date.relative.days.next." + timeDt.dayOfWeek().get();
            }
            else
            {
                // DMY in the future
                return null;
            }
        }

        /**
         * Note: can't simply use {@link LocalDate#LocalDate(Object)} to construct LocalDate because it returns
         * inconsistent results when Daylight Savings is a factor.
         *
         *
         * @param date the date to convert
         * @param timeZone
         * @return a LocalDate which represents the date.
         */
        private static LocalDate createLocalDate(final Date date, TimeZone timeZone)
        {
            final Calendar cal = Calendar.getInstance();
            cal.setTimeZone(timeZone);
            cal.setTime(date);
            cal.getTime();
            return new LocalDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1, cal.get(Calendar.DAY_OF_MONTH));
        }
    }
}
