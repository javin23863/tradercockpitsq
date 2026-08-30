/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.utils.TimeLocalObjects;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.DateTimeField;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.chrono.ISOChronology;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQTime {
    public static final Logger Log = LoggerFactory.getLogger((String)"SQTime");
    public static final long DAY_MILLIS = 86400000L;
    public static final int MONDAY = 1;
    public static final int TUESDAY = 2;
    public static final int WEDNESDAY = 3;
    public static final int THURSDAY = 4;
    public static final int FRIDAY = 5;
    public static final int SATURDAY = 6;
    public static final int SUNDAY = 7;
    private static ThreadLocal<TimeLocalObjects> localObjects = ThreadLocal.withInitial(() -> new TimeLocalObjects());
    private static final DateTimeFormatter formaterDateMinute = DateTimeFormat.forPattern((String)"yyyy.MM.dd HH:mm");
    private static final DateTimeFormatter formaterFullDateTime = DateTimeFormat.forPattern((String)"yyyy.MM.dd HH:mm:ss.SSS");
    private static final DateTimeFormatter formaterFullDateMinute = DateTimeFormat.forPattern((String)"yyyy.MM.dd HH:mm:ss");
    private static final DateTimeFormatter formaterDate = DateTimeFormat.forPattern((String)"yyyy.MM.dd");
    private static final DateTimeFormatter formaterTime = DateTimeFormat.forPattern((String)"HH:mm");
    private static final DateTimeFormatter formaterLog = DateTimeFormat.forPattern((String)"HH:mm:ss");
    private static final DateTimeFormatter formaterFullTime = DateTimeFormat.forPattern((String)"e:HH:mm:ss:SSS");
    public static String shortDateFormat = "dd.MM.yyyy";
    private static final DateTimeFormatter uiDateFormatter = DateTimeFormat.forPattern((String)"yyyy.MM.dd");
    public static String longDateFormat = "dd.MM.yyyy HH:mm:ss";
    private static final DateTimeFormatter longDateFormatter = DateTimeFormat.forPattern((String)longDateFormat);

    public static void init() {
        SQTime.getChronology();
    }

    public static int getDateTime(long l) {
        return (int)(l / 1000L);
    }

    public static long getDateInMs(long l) {
        return l - (long)SQTime.getChronology().millisOfDay().get(l);
    }

    private static ISOChronology getChronology() {
        TimeLocalObjects timeLocalObjects = localObjects.get();
        if (!timeLocalObjects.defaultTimezoneSet) {
            timeLocalObjects.computerTimeZone = DateTimeZone.getDefault();
            TimeZone timeZone = TimeZone.getTimeZone("GMT");
            TimeZone.setDefault(timeZone);
            DateTimeZone dateTimeZone = DateTimeZone.forID((String)"GMT");
            DateTimeZone.setDefault((DateTimeZone)dateTimeZone);
            timeLocalObjects.chronology = ISOChronology.getInstance();
            timeLocalObjects.defaultTimezoneSet = true;
        }
        return timeLocalObjects.chronology;
    }

    public static int getYear(long l) {
        return SQTime.getFullYear(l) - 1900;
    }

    public static int getFullYear(long l) {
        return SQTime.getChronology().year().get(l);
    }

    public static int getMonthOriginal(long l) {
        return SQTime.getChronology().monthOfYear().get(l);
    }

    public static int getMonth(long l) {
        return SQTime.getMonthOriginal(l) - 1;
    }

    public static int getDayOfWeekOriginal(long l) {
        int n = SQTime.getDayOfWeek(l);
        if (n == 7) {
            n = 0;
        }
        return n;
    }

    public static int getDayOfWeek(long l) {
        return SQTime.getChronology().dayOfWeek().get(l);
    }

    public static int getDay(long l) {
        return SQTime.getChronology().dayOfMonth().get(l);
    }

    public static int getDaysInMonth(long l) {
        return SQTime.getChronology().dayOfMonth().getMaximumValue(l);
    }

    public static int getDayOfYear(long l) {
        return SQTime.getChronology().dayOfYear().get(l);
    }

    public static int getWeekOfYear(long l) {
        return SQTime.getChronology().weekOfWeekyear().get(l);
    }

    public static int getWeekOfMonth(long l) {
        long l2 = SQTime.getDateInMs(l);
        long l3 = SQTime.setDayOfMonth(l2, 1);
        long l4 = SQTime.setDayOfWeek(l3, 1);
        int n = SQTime.getDaysBetween(l2, l4) / 7 + 1;
        return n;
    }

    public static int getHour(long l) {
        return SQTime.getChronology().hourOfDay().get(l);
    }

    public static int getMinute(long l) {
        DateTimeField dateTimeField = SQTime.getChronology().minuteOfHour();
        return dateTimeField.get(l);
    }

    public static int getSecond(long l) {
        return SQTime.getChronology().secondOfMinute().get(l);
    }

    public static int getMiliSecond(long l) {
        return SQTime.getChronology().millisOfSecond().get(l);
    }

    public static int getYmd(long l) {
        return SQTime.getYear(l) * 10000 + SQTime.getMonthOriginal(l) * 100 + SQTime.getDay(l);
    }

    public static int getYmd(int n, int n2, int n3) {
        return n * 10000 + n2 * 100 + n3;
    }

    public static double getHms(int n, int n2, int n3) {
        return (double)(100 * n + n2) + 0.01 * (double)n3;
    }

    public static long setMinute(long l, int n) {
        return SQTime.getChronology().minuteOfHour().set(l, n);
    }

    public static long setHour(long l, int n) {
        return SQTime.getChronology().hourOfDay().set(l, n);
    }

    public static long setDayOfWeek(long l, int n) {
        return SQTime.getChronology().dayOfWeek().set(l, n);
    }

    public static long setFirstDayOfWeek(long l) {
        if (SQTime.getDayOfWeek(l) != 7) {
            return SQTime.setDayOfWeek(SQTime.addWeeks(l, -1), 7);
        }
        return l;
    }

    public static long setDayOfMonth(long l, int n) {
        return SQTime.getChronology().dayOfMonth().set(l, n);
    }

    public static long setMonthOfYear(long l, int n) {
        return SQTime.getChronology().monthOfYear().set(l, n);
    }

    public static long setSecond(long l, int n) {
        return SQTime.getChronology().secondOfMinute().set(l, n);
    }

    public static long setTime(long l, int n, int n2, int n3) {
        l = SQTime.setHour(l, n);
        l = SQTime.setMinute(l, n2);
        l = SQTime.setSecond(l, n3);
        return l;
    }

    public static long setTime(long l, int n, int n2, int n3, int n4) {
        l = SQTime.setHour(l, n);
        l = SQTime.setMinute(l, n2);
        l = SQTime.setSecond(l, n3);
        l = SQTime.setMiliSeconds(l, n4);
        return l;
    }

    public static long add(long l, int n, int n2) {
        if (n == 12) {
            return SQTime.addMinutes(l, n2);
        }
        if (n == 11) {
            return SQTime.addHours(l, n2);
        }
        if (n == 5) {
            return SQTime.addDays(l, n2);
        }
        if (n == 2) {
            return SQTime.addMonths(l, n2);
        }
        if (n == 1) {
            return SQTime.addYears(l, n2);
        }
        throw new IllegalArgumentException("Not sdupported field: " + n);
    }

    public static long minus(long l, int n, int n2) {
        return SQTime.add(l, n, -n2);
    }

    public static long addYears(long l, int n) {
        return SQTime.getChronology().years().add(l, n);
    }

    public static long addMonths(long l, int n) {
        return SQTime.getChronology().months().add(l, n);
    }

    public static long addWeeks(long l, int n) {
        return SQTime.getChronology().weeks().add(l, n);
    }

    public static long addDays(long l, int n) {
        return SQTime.getChronology().days().add(l, n);
    }

    public static long addHours(long l, int n) {
        return SQTime.getChronology().hours().add(l, n);
    }

    public static long addMinutes(long l, int n) {
        return SQTime.getChronology().minutes().add(l, n);
    }

    public static long addSeconds(long l, int n) {
        return SQTime.getChronology().seconds().add(l, n);
    }

    public static String toString(long l, String string) {
        return SQTime.toString(l, DateTimeFormat.forPattern((String)string));
    }

    public static String toString(long l, DateTimeFormatter dateTimeFormatter) {
        return dateTimeFormatter.print(l);
    }

    public static String toDateString(long l) {
        return SQTime.toString(l, shortDateFormat);
    }

    public static String toDateMinuteString(long l) {
        return SQTime.toString(l, formaterDateMinute);
    }

    public static String toFullDateMinuteString(long l) {
        return SQTime.toString(l, formaterFullDateMinute);
    }

    public static String toFullDateTimeString(long l) {
        return SQTime.toString(l, formaterFullDateTime);
    }

    public static String toFullTime(long l) {
        return SQTime.toString(l, formaterFullTime);
    }

    public static long parseDateToMilis(String string) throws ParseException {
        return formaterDate.parseMillis(string);
    }

    public static long parseToMilis(String string, String string2) {
        return DateTimeFormat.forPattern((String)string2).parseMillis(string);
    }

    public static String formatDate(long l) {
        return formaterDate.print(l);
    }

    public static String formatDate(long l, DateTimeFormatter dateTimeFormatter) {
        return dateTimeFormatter.print(l);
    }

    public static String formatDateQuick(long l) {
        return SQTime.getFullYear(l) + "." + SQTime.getMonthOriginal(l) + "." + SQTime.getDay(l);
    }

    public static String formatTime(long l) {
        return formaterTime.print(l);
    }

    public static Date toDate(long l) {
        l = SQTime.correctDayStart(l);
        return new Date(l);
    }

    public static Date toTime(long l) {
        l = SQTime.setSecond(l, 0);
        l = SQTime.setMiliSeconds(l, 0);
        return new Date(l);
    }

    public static String formatDateTime(long l) {
        long l2 = l / 86400L;
        long l3 = l / 3600L;
        long l4 = l / 60L;
        if (l2 > 0L) {
            return String.format("%dd %dh %dm", l / 86400L, l % 86400L / 3600L, l % 3600L / 60L);
        }
        if (l3 > 0L) {
            return String.format("%dh %dm", l / 3600L, l % 3600L / 60L);
        }
        if (l4 > 0L) {
            return String.format("%dm %ds", l % 3600L / 60L, l % 60L);
        }
        return String.format("%ds", l % 60L);
    }

    public static int dowToMetatrader(int n) {
        if (n == 1) {
            return 0;
        }
        if (n == 2) {
            return 1;
        }
        if (n == 3) {
            return 2;
        }
        if (n == 4) {
            return 3;
        }
        if (n == 5) {
            return 4;
        }
        if (n == 6) {
            return 5;
        }
        if (n == 7) {
            return 6;
        }
        return 0;
    }

    public static String dowToString(int n) {
        if (n == 1) {
            return "Sunday";
        }
        if (n == 2) {
            return "Monday";
        }
        if (n == 3) {
            return "Tuesday";
        }
        if (n == 4) {
            return "Wednesday";
        }
        if (n == 5) {
            return "Thursday";
        }
        if (n == 6) {
            return "Friday";
        }
        if (n == 7) {
            return "Saturday";
        }
        return "Unknown:" + n;
    }

    public static boolean timesInSameMinute(int n, int n2) {
        if (n == 0 || n2 == 0) {
            return false;
        }
        if (n == n2) {
            return true;
        }
        return n / 60 == n2 / 60;
    }

    public static int getMinutesBetween(long l, long l2) {
        long l3 = Math.abs(l2 - l);
        long l4 = l3 % 60000L;
        if (l4 > 0L) {
            return (int)(l3 / 60000L) + 1;
        }
        return (int)(l3 / 60000L);
    }

    public static int getDaysBetween(long l, long l2) {
        long l3 = Math.abs(l2 - l);
        long l4 = l3 % 86400000L;
        if (l4 > 0L && 86400000L - l4 > 1L) {
            return (int)(l3 / 86400000L) + 1;
        }
        return (int)(l3 / 86400000L);
    }

    public static int getWeeksBetween(long l, long l2) {
        long l3 = Math.abs(l2 - l);
        long l4 = l3 % 604800000L;
        if (l4 > 0L && 604800000L - l4 > 1L) {
            return (int)(l3 / 604800000L) + 1;
        }
        return (int)(l3 / 604800000L);
    }

    public static int getMonthsBetween(long l, long l2) {
        int n = (SQTime.getYear(l2) - SQTime.getYear(l)) * 12 + (SQTime.getMonth(l2) - SQTime.getMonth(l)) + (SQTime.getDay(l2) >= SQTime.getDay(l) ? 0 : -1);
        return n;
    }

    public static int getYearsBetween(long l, long l2) {
        return Math.abs(SQTime.getYear(l2) - SQTime.getYear(l));
    }

    public static int toNumberTime(long l) {
        return SQTime.getHour(l) * 10000 + SQTime.getMinute(l) * 100;
    }

    public static long setMiliSeconds(long l, int n) {
        return SQTime.getChronology().millisOfSecond().set(l, n);
    }

    public static long getMilis(long l) {
        return l;
    }

    public static int getTimeAsTSTime(long l) {
        int n = SQTime.getHour(l);
        int n2 = SQTime.getMinute(l);
        return n * 100 + n2;
    }

    public static long setTimeToTSTime(long l, int n) {
        int n2 = n / 100;
        int n3 = n - n2 * 100;
        l = SQTime.setHour(l, n2);
        l = SQTime.setMinute(l, n3);
        return l;
    }

    public static int getTSDiffInMinutes(int n, int n2) {
        int n3 = n / 100;
        int n4 = n - n3 * 100;
        int n5 = n2 / 100;
        int n6 = n2 - n5 * 100;
        return n4 - n6 + 60 * (n3 - n5);
    }

    public static long toLong(int n, int n2, int n3) {
        return SQTime.toLong(n, n2, n3, 0, 0, 0);
    }

    public static long toLong(int n, int n2, int n3, int n4, int n5, int n6) {
        long l = SQTime.getChronology().getDateTimeMillis(n, n2, n3, n4, n5, n6, 0);
        return l;
    }

    public static long correctDayEnd(long l) {
        return SQTime.setTime(l, 23, 59, 59, 999);
    }

    public static long correctDayEndMT(long l) {
        return SQTime.setTime(l, 23, 59, 59, 0);
    }

    public static long correctDayStart(long l) {
        return SQTime.setTime(l, 0, 0, 0, 0);
    }

    public static long setHHMM(long l, int n) {
        int n2 = n % 100;
        int n3 = n / 100;
        l = SQTime.setHour(l, n3);
        return SQTime.setMinute(l, n2);
    }

    public static String toHHMMString(long l) {
        return formaterTime.print(l);
    }

    public static String getLogTime(boolean bl, boolean bl2) {
        Object object;
        Object object2;
        TimeLocalObjects timeLocalObjects = localObjects.get();
        if (!timeLocalObjects.defaultTimezoneSet) {
            timeLocalObjects.computerTimeZone = DateTimeZone.getDefault();
            object2 = TimeZone.getTimeZone("GMT");
            TimeZone.setDefault((TimeZone)object2);
            object = DateTimeZone.forID((String)"GMT");
            DateTimeZone.setDefault((DateTimeZone)object);
            timeLocalObjects.chronology = ISOChronology.getInstance();
            timeLocalObjects.defaultTimezoneSet = true;
        }
        object2 = DateTime.now((DateTimeZone)timeLocalObjects.computerTimeZone);
        object2 = object2.plusMillis((int)MainApp.TimezoneDiff);
        object = (bl ? "yyyy.MM.dd " : "") + "HH:mm:ss" + (bl2 ? ".SSS" : "");
        return object2.toString((String)object);
    }

    public static long getLocalCurrentTimeInMs() {
        Object object;
        TimeLocalObjects timeLocalObjects = localObjects.get();
        if (!timeLocalObjects.defaultTimezoneSet) {
            timeLocalObjects.computerTimeZone = DateTimeZone.getDefault();
            object = TimeZone.getTimeZone("GMT");
            TimeZone.setDefault((TimeZone)object);
            DateTimeZone dateTimeZone = DateTimeZone.forID((String)"GMT");
            DateTimeZone.setDefault((DateTimeZone)dateTimeZone);
            timeLocalObjects.chronology = ISOChronology.getInstance();
            timeLocalObjects.defaultTimezoneSet = true;
        }
        object = DateTime.now((DateTimeZone)timeLocalObjects.computerTimeZone);
        object = object.plusMillis((int)MainApp.TimezoneDiff);
        return object.getMillis();
    }

    public static long toHours(long l) {
        l = SQTime.setMinute(l, 0);
        l = SQTime.setSecond(l, 0);
        l = SQTime.setMiliSeconds(l, 0);
        return l;
    }

    public static String toString(long l) {
        return longDateFormatter.print(l);
    }

    public static String toUIDateString(long l) {
        return uiDateFormatter.print(l);
    }

    public static int getHHMM(long l) {
        return SQTime.getHour(l) * 100 + SQTime.getMinute(l);
    }

    public static int minutesToHHMM(int n) {
        return n / 60 * 100 + n % 60;
    }

    public static int HHMMToMinutes(int n) {
        return n / 100 * 60 + n % 100;
    }

    public static int getDiffInDays(long l, long l2) {
        l -= l % 86400000L;
        l2 -= l2 % 86400000L;
        return (int)((l - l2) / 86400000L);
    }

    public static String getDurationText(long l) {
        return String.format("%.2f s.", (double)l / 1000.0);
    }

    public static int[] getHHMM(String string) throws Exception {
        try {
            String[] stringArray = string.split(":");
            int n = Integer.parseInt(stringArray[0]);
            int n2 = Integer.parseInt(stringArray[1]);
            return new int[]{n, n2};
        }
        catch (Exception exception) {
            throw new Exception("Invalid time format (" + string + "), must be HH:MM");
        }
    }

    public static boolean isSameDay(long l, long l2) {
        LocalDate localDate;
        LocalDate localDate2 = new DateTime(l).toLocalDate();
        return localDate2.equals((Object)(localDate = new DateTime(l2).toLocalDate()));
    }

    public static int isSameWeek(long l, long l2) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date(l));
        Calendar calendar2 = Calendar.getInstance();
        calendar2.setTime(new Date(l2));
        int n = calendar.get(1);
        int n2 = calendar.get(3);
        int n3 = calendar2.get(1);
        int n4 = calendar2.get(3);
        if (n2 == n4) {
            return 2;
        }
        if (n < n3 || n == n3 && n2 < n4 || n == n3 && n4 == 1 || n == n3 && n2 == 52) {
            return 1;
        }
        return 0;
    }

    public static boolean isSameMonth(long l, long l2) {
        DateTime dateTime = new DateTime(l);
        DateTime dateTime2 = new DateTime(l2);
        int n = dateTime.getYear();
        int n2 = dateTime.getMonthOfYear();
        int n3 = dateTime2.getYear();
        int n4 = dateTime2.getMonthOfYear();
        return n == n3 && n2 == n4;
    }
}

