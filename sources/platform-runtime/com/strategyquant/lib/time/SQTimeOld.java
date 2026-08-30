/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.time;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.ReadableInstant;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class SQTimeOld
implements Serializable {
    public static final int MONDAY = 1;
    public static final int TUESDAY = 2;
    public static final int WEDNESDAY = 3;
    public static final int THURSDAY = 4;
    public static final int FRIDAY = 5;
    public static final int SATURDAY = 6;
    public static final int SUNDAY = 7;
    private DateTime dateTime = new DateTime();
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat();
    private static final SimpleDateFormat standardDateFormat = new SimpleDateFormat("yyyy.MM.dd");
    private SimpleDateFormat customDateFormat = null;
    private static final DateTimeFormatter formaterDateMinute = DateTimeFormat.forPattern((String)"yyyy.MM.dd HH:mm");
    private static final DateTimeFormatter formaterFullDMS = DateTimeFormat.forPattern((String)"yyyy.MM.dd HH:mm:ss");
    private static final DateTimeFormatter formaterFullDateMinute = DateTimeFormat.forPattern((String)"yyyy.MM.dd HH:mm:ss.SSS");
    private static final DateTimeFormatter formaterDate = DateTimeFormat.forPattern((String)"yyyy.MM.dd");
    private static final DateTimeFormatter formaterDateFast = DateTimeFormat.forPattern((String)"yy.MM.dd");
    private static final DateTimeFormatter formaterTime = DateTimeFormat.forPattern((String)"HH:mm");
    private static final DateTimeFormatter formaterFullTime = DateTimeFormat.forPattern((String)"e:HH:mm:ss:SSS");
    public static String shortDateFormat = "dd.MM.yyyy";
    public static String longDateFormat = "dd.MM.yyyy HH:mm:ss";

    public SQTimeOld() {
        dateFormat.applyPattern("yyyy.MM.dd HH:mm");
        this.dateTime = this.dateTime.withSecondOfMinute(0);
    }

    public SQTimeOld(Date date) {
        this();
        if (date != null) {
            this.dateTime = new DateTime(date.getTime());
        }
    }

    public SQTimeOld(long l) {
        this.set(l);
    }

    private DateTime getDateTimeObj() {
        return this.dateTime;
    }

    public SQTimeOld clone() {
        return new SQTimeOld(this.getMilis());
    }

    public SQTimeOld(int n, int n2, int n3, int n4, int n5, int n6, int n7) {
        this.dateTime = new DateTime(n, n2, n3, n4, n5, n6, n7);
    }

    public SQTimeOld(int n, int n2, int n3, int n4, int n5, int n6) {
        this.dateTime = new DateTime(n, n2, n3, n4, n5, n6);
    }

    public SQTimeOld(int n, int n2, int n3, int n4, int n5) {
        this(n, n2, n3, n4, n5, 0);
    }

    public SQTimeOld(int n, int n2, int n3) {
        this(n, n2, n3, 0, 0);
    }

    public void set(long l) {
        this.dateTime = new DateTime(l);
    }

    public int getDateTime() {
        return (int)(this.dateTime.getMillis() / 1000L);
    }

    public int getDate() {
        DateTime dateTime = new DateTime(this.dateTime.getYear(), this.dateTime.getMonthOfYear(), this.dateTime.getDayOfMonth(), 0, 0, 0);
        return (int)(dateTime.getMillis() / 1000L);
    }

    public long getDateInMs() {
        long l = this.dateTime.getMillis();
        return l -= (long)this.dateTime.getMillisOfDay();
    }

    public int getYear() {
        return this.dateTime.getYear() - 1900;
    }

    public int getFullYear() {
        return this.dateTime.getYear();
    }

    public int getMonthOriginal() {
        return this.dateTime.getMonthOfYear();
    }

    public int getMonth() {
        return this.dateTime.getMonthOfYear() - 1;
    }

    public int getDayOfWeekOriginal() {
        int n = this.dateTime.getDayOfWeek();
        if (n == 7) {
            n = 0;
        }
        return n;
    }

    public int getDayOfWeek() {
        int n = this.dateTime.getDayOfWeek();
        return n;
    }

    public int getDay() {
        return this.dateTime.getDayOfMonth();
    }

    public int getDayOfYear() {
        return this.dateTime.getDayOfYear();
    }

    public int getWeekOfYear() {
        return this.dateTime.getWeekOfWeekyear();
    }

    public int getHour() {
        return this.dateTime.getHourOfDay();
    }

    public int getMinute() {
        return this.dateTime.getMinuteOfHour();
    }

    public int getSecond() {
        return this.dateTime.getSecondOfMinute();
    }

    public int getMiliSecond() {
        return this.dateTime.getMillisOfSecond();
    }

    public void setMinute(int n) {
        this.dateTime = this.dateTime.withMinuteOfHour(n);
    }

    public void setHour(int n) {
        this.dateTime = this.dateTime.withHourOfDay(n);
    }

    public void setDayOfWeek(int n) {
        this.dateTime = this.dateTime.withDayOfWeek(n);
    }

    public void setDayOfMonth(int n) {
        this.dateTime = this.dateTime.withDayOfMonth(n);
    }

    public void setSecond(int n) {
        this.dateTime = this.dateTime.withSecondOfMinute(n);
    }

    public void setTime(int n, int n2, int n3) {
        this.setHour(n);
        this.setMinute(n2);
        this.setSecond(n3);
    }

    public void add(int n, int n2) {
        if (n == 12) {
            this.dateTime = this.dateTime.plusMinutes(n2);
        } else if (n == 11) {
            this.dateTime = this.dateTime.plusHours(n2);
        } else if (n == 5) {
            this.dateTime = this.dateTime.plusDays(n2);
        }
    }

    public void minus(int n, int n2) {
        if (n == 12) {
            this.dateTime = this.dateTime.minusMinutes(n2);
        } else if (n == 11) {
            this.dateTime = this.dateTime.minusHours(n2);
        } else if (n == 5) {
            this.dateTime = this.dateTime.minusDays(n2);
        } else if (n == 2) {
            this.dateTime = this.dateTime.minusMonths(n2);
        } else if (n == 1) {
            this.dateTime = this.dateTime.minusYears(n2);
        }
    }

    public void addYears(int n) {
        this.dateTime = this.dateTime.plusYears(n);
    }

    public void addMonths(int n) {
        this.dateTime = this.dateTime.plusMonths(n);
    }

    public void addWeeks(int n) {
        this.dateTime = this.dateTime.plusWeeks(n);
    }

    public void addDays(int n) {
        this.dateTime = this.dateTime.plusDays(n);
    }

    public void addHours(int n) {
        this.dateTime = this.dateTime.plusHours(n);
    }

    public void addMinutes(int n) {
        this.dateTime = this.dateTime.plusMinutes(n);
    }

    public SQTimeOld addDaysReturnDate(int n) {
        SQTimeOld sQTimeOld = new SQTimeOld(this.getMilis());
        sQTimeOld.addDays(n);
        return sQTimeOld;
    }

    public String toString() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern((String)longDateFormat);
        return this.dateTime.toString(dateTimeFormatter);
    }

    public String toString(String string) {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern((String)string);
        return this.dateTime.toString(dateTimeFormatter);
    }

    public String toDateString() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern((String)shortDateFormat);
        return this.dateTime.toString(dateTimeFormatter);
    }

    public String toDateMinuteString() {
        return this.dateTime.toString(formaterDateMinute);
    }

    public static String toString(int n) {
        SQTimeOld sQTimeOld = new SQTimeOld(n);
        return sQTimeOld.toString();
    }

    public static String toString(long l) {
        SQTimeOld sQTimeOld = new SQTimeOld(l);
        return sQTimeOld.toString();
    }

    public static String toDateString(long l) {
        SQTimeOld sQTimeOld = new SQTimeOld(l);
        return sQTimeOld.toDateString();
    }

    public static String toDateString(int n) {
        SQTimeOld sQTimeOld = new SQTimeOld(n);
        return sQTimeOld.toDateString();
    }

    public static String toDateMinuteString(int n) {
        SQTimeOld sQTimeOld = new SQTimeOld(n);
        return sQTimeOld.toDateMinuteString();
    }

    public static String toDateMinuteString(long l) {
        SQTimeOld sQTimeOld = new SQTimeOld(l);
        return sQTimeOld.toDateMinuteString();
    }

    public static String toFullDateMinuteString(long l) {
        SQTimeOld sQTimeOld = new SQTimeOld(l);
        return sQTimeOld.toFullDateMinuteString();
    }

    public static String toFullDMSString(long l) {
        SQTimeOld sQTimeOld = new SQTimeOld(l);
        return sQTimeOld.toFullDMSString();
    }

    public String toFullDMSString() {
        return this.dateTime.toString(formaterFullDMS);
    }

    public String toFullDateMinuteString() {
        return this.dateTime.toString(formaterFullDateMinute);
    }

    public String toFullTime() {
        return this.dateTime.toString(formaterFullTime);
    }

    public static String toTimeString(int n) {
        SQTimeOld sQTimeOld = new SQTimeOld(n);
        return sQTimeOld.formatTime();
    }

    public void setDateFormat(String string, boolean bl) {
        if (this.customDateFormat == null) {
            this.customDateFormat = new SimpleDateFormat();
        }
        this.customDateFormat.applyPattern(string);
        this.customDateFormat.setLenient(bl);
    }

    public SQTimeOld parseDateTime(String string) throws ParseException {
        if (this.customDateFormat == null) {
            this.customDateFormat = new SimpleDateFormat();
            this.customDateFormat.applyPattern("yyyy.MM.dd HH:mm");
        }
        this.dateTime = new DateTime((Object)this.customDateFormat.parse(string));
        return this;
    }

    public SQTimeOld parseDateTime(String string, String string2) throws ParseException {
        dateFormat.applyPattern(string2);
        dateFormat.setLenient(true);
        this.dateTime = new DateTime((Object)dateFormat.parse(string));
        return this;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static long parseDateToMilis(String string) throws ParseException {
        SimpleDateFormat simpleDateFormat = standardDateFormat;
        synchronized (simpleDateFormat) {
            if (!standardDateFormat.isLenient()) {
                standardDateFormat.setLenient(true);
            }
            standardDateFormat.setTimeZone(TimeZone.getDefault());
            DateTime dateTime = new DateTime((Object)standardDateFormat.parse(string));
            return dateTime.getMillis();
        }
    }

    public String formatDate() {
        return this.dateTime.toString(formaterDate);
    }

    public String formatDateQuick() {
        return this.dateTime.getYear() + "." + this.dateTime.getMonthOfYear() + "." + this.dateTime.getDayOfMonth();
    }

    public static String formatDateQuick(int n) {
        SQTimeOld sQTimeOld = new SQTimeOld(n);
        return sQTimeOld.formatDateQuick();
    }

    public String formatTime() {
        return this.dateTime.toString(formaterTime);
    }

    public Date toDate() {
        return new Date(this.getYear(), this.getMonth(), this.getDay());
    }

    public Date toTime() {
        return new Date(this.getYear(), this.getMonth(), this.getDay(), this.getHour(), this.getMinute());
    }

    public static String timeToString(long l) {
        return String.format("%02d:%02d", l / 3600L, l % 3600L / 60L);
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

    public static Date toDate(int n) {
        SQTimeOld sQTimeOld = new SQTimeOld(n);
        return new Date(sQTimeOld.getMilis());
    }

    public static Date toDate(long l) {
        SQTimeOld sQTimeOld = new SQTimeOld(l);
        return new Date(sQTimeOld.getMilis());
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

    public static int getYear(int n) {
        SQTimeOld sQTimeOld = new SQTimeOld(n);
        return sQTimeOld.getYear();
    }

    public static int getDaysBetween(long l, long l2) {
        return SQTimeOld.getDaysBetween(new DateTime(l), new DateTime(l2));
    }

    public static int getDaysBetween(SQTimeOld sQTimeOld, SQTimeOld sQTimeOld2) {
        return SQTimeOld.getDaysBetween(sQTimeOld.getDateTimeObj(), sQTimeOld2.getDateTimeObj());
    }

    public static int getDaysBetween(DateTime dateTime, DateTime dateTime2) {
        Duration duration = new Duration((ReadableInstant)dateTime, (ReadableInstant)dateTime2);
        long l = duration.getMillis() % 86400000L;
        if (l > 0L) {
            return (int)duration.getStandardDays() + 1;
        }
        return (int)duration.getStandardDays();
    }

    public static int getMonthsBetween(long l, long l2) {
        return SQTimeOld.getMonthsBetween(new DateTime(l), new DateTime(l2));
    }

    public static int getMonthsBetween(SQTimeOld sQTimeOld, SQTimeOld sQTimeOld2) {
        return SQTimeOld.getMonthsBetween(sQTimeOld.getDateTimeObj(), sQTimeOld2.getDateTimeObj());
    }

    public static int getMonthsBetween(DateTime dateTime, DateTime dateTime2) {
        int n = (dateTime2.getYear() - dateTime.getYear()) * 12 + (dateTime2.getMonthOfYear() - dateTime.getMonthOfYear()) + (dateTime2.getDayOfMonth() >= dateTime.getDayOfMonth() ? 0 : -1);
        return n;
    }

    public static int getYearsBetween(long l, long l2) {
        return SQTimeOld.getYearsBetween(new DateTime(l), new DateTime(l2));
    }

    public static int getYearsBetween(SQTimeOld sQTimeOld, SQTimeOld sQTimeOld2) {
        return SQTimeOld.getYearsBetween(sQTimeOld.getDateTimeObj(), sQTimeOld2.getDateTimeObj());
    }

    public static int getYearsBetween(DateTime dateTime, DateTime dateTime2) {
        return Math.abs(dateTime.getYear() - dateTime2.getYear());
    }

    public int toNumberTime() {
        return this.getHour() * 10000 + this.getMinute() * 100;
    }

    public static void setDefaultTimeZone() {
    }

    public int compareTo(SQTimeOld sQTimeOld) {
        long l;
        long l2 = this.dateTime.getMillis();
        if (l2 < (l = sQTimeOld.getMilis())) {
            return -1;
        }
        if (l2 > l) {
            return 1;
        }
        return 0;
    }

    public void setMiliSeconds(int n) {
        this.dateTime = this.dateTime.withMillisOfSecond(n);
    }

    public long getMilis() {
        return this.dateTime.getMillis();
    }

    public int getTimeAsTSTime() {
        int n = this.dateTime.getHourOfDay();
        int n2 = this.dateTime.getMinuteOfHour();
        return n * 100 + n2;
    }

    public void setTimeToTSTime(int n) {
        int n2 = n / 100;
        int n3 = n - n2 * 100;
        this.setHour(n2);
        this.setMinute(n3);
    }

    public static int getTSDiffInMinutes(int n, int n2) {
        int n3 = n / 100;
        int n4 = n - n3 * 100;
        int n5 = n2 / 100;
        int n6 = n2 - n5 * 100;
        return n4 - n6 + 60 * (n3 - n5);
    }

    public static long toLong(int n, int n2, int n3) {
        DateTime dateTime = new DateTime(n, n2, n3, 0, 0, 0);
        return dateTime.getMillis();
    }

    public static long toLong(int n, int n2, int n3, int n4, int n5, int n6) {
        DateTime dateTime = new DateTime(n, n2, n3, n4, n5, n6);
        return dateTime.getMillis();
    }

    public static String getTimeAsStr(long l, DateTimeFormatter dateTimeFormatter) {
        DateTime dateTime = new DateTime(l);
        return dateTime.toString(dateTimeFormatter);
    }

    public static long correctDayEnd(long l) {
        DateTime dateTime = new DateTime(l);
        return new DateTime(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(), 23, 59, 59, 999).getMillis();
    }

    public static long correctDayStart(long l) {
        DateTime dateTime = new DateTime(l);
        return new DateTime(dateTime.getYear(), dateTime.getMonthOfYear(), dateTime.getDayOfMonth(), 0, 0, 0, 0).getMillis();
    }

    public void setHHMM(int n) {
        int n2 = n % 100;
        int n3 = n / 100;
        this.setHour(n3);
        this.setMinute(n2);
    }

    public static String toHHMMString(long l) {
        SQTimeOld sQTimeOld = new SQTimeOld(l);
        return sQTimeOld.formatTime();
    }
}

