/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.time;

import com.strategyquant.lib.time.DateFormats;
import com.strategyquant.lib.time.SQTimeOld;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.SimpleTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DateTimeParser {
    public static final Logger Log = LoggerFactory.getLogger((String)"DateTimeParser");
    private SimpleDateFormat sdf;
    private List<String> availableDateFormats = DateFormats.getInstance().getAvailableDateFormats();
    private String pattern = null;

    public String getPattern() {
        return this.pattern;
    }

    public DateTimeParser() {
        this.sdf = new SimpleDateFormat();
        this.sdf.setTimeZone(new SimpleTimeZone(0, "GMT"));
        this.sdf.setLenient(false);
    }

    public void recognizeDateFormat(List<String> list) throws Exception {
        this.pattern = null;
        for (String string : this.availableDateFormats) {
            try {
                this.checkDateFormat(list, string);
                this.pattern = string;
                Log.debug("Date format recognized to: \"{}\"", (Object)string);
                return;
            }
            catch (Exception exception) {
            }
        }
        throw new Exception("Date format not recognized(1).");
    }

    public void recognizeDateFormat(String string) throws Exception {
        this.pattern = null;
        for (String string2 : this.availableDateFormats) {
            try {
                this.checkDateFormat(string, string2);
                this.pattern = string2;
                Log.debug("Date format recognized to: \"{}\"", (Object)string2);
                return;
            }
            catch (Exception exception) {
            }
        }
        throw new Exception("Date format not recognized(1).");
    }

    private void checkDateFormat(List<String> list, String string) throws ParseException {
        this.sdf.applyPattern(string);
        for (String string2 : list) {
            this.sdf.parse(this.checkDate(string2));
        }
    }

    private void checkDateFormat(String string, String string2) throws ParseException {
        this.sdf.applyPattern(string2);
        this.sdf.parse(this.checkDate(string));
    }

    public SQTimeOld parse(String string) throws Exception {
        if (this.pattern == null) {
            throw new Exception("Date format not recognized(2).");
        }
        string = this.checkDate(string);
        try {
            return new SQTimeOld(this.sdf.parse(string));
        }
        catch (Exception exception) {
            if (this.pattern.contains("HH")) {
                this.sdf.applyPattern(this.pattern.substring(0, this.pattern.indexOf("HH")).trim());
                Date date = this.sdf.parse(string);
                this.sdf.applyPattern(this.pattern);
                return new SQTimeOld(date);
            }
            throw exception;
        }
    }

    private String checkDate(String string) {
        try {
            if (!string.matches("^/d{2}//d{2}//d{4}.*") && string.matches("^/d{2}//d{2}//d{2}.*")) {
                return this.normalizeDate(string, "/");
            }
            if (!string.matches("^/d{2}-/d{2}-/d{4}.*") && string.matches("^/d{2}-/d{2}-/d{2}.*")) {
                return this.normalizeDate(string, "-");
            }
            if (!string.matches("^/d{2}/./d{2}/./d{4}.*") && string.matches("^/d{2}/./d{2}/./d{2}.*")) {
                return this.normalizeDate(string, ".");
            }
        }
        catch (Exception exception) {
            Log.error("Exc. Date='" + string + "'", (Throwable)exception);
        }
        return string;
    }

    private String normalizeDate(String string, String string2) {
        int n;
        String string3;
        int n2 = string.lastIndexOf(string2);
        String string4 = string3 = string.substring(n2 + 1);
        if (string3.length() > 2) {
            string4 = string3.substring(0, 2);
        }
        string = (n = Integer.parseInt(string4)) < 50 ? string.substring(0, n2 + 1) + "20" + string4 : string.substring(0, n2 + 1) + "19" + string4;
        if (string3.length() > 2) {
            return string + string3.substring(2);
        }
        return string;
    }
}

