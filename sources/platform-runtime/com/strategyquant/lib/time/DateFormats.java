/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.time;

import com.strategyquant.lib.SQUtils;
import java.util.ArrayList;
import java.util.List;

public class DateFormats {
    public static final String LONG_MILLIS = "Long (millis from epoch)";
    public static final String LONG_SECONDS = "Long (seconds from epoch)";
    private static DateFormats instance;
    private List<String> availableDateFormats = new ArrayList<String>();

    public List<String> getAvailableDateFormats() {
        return this.availableDateFormats;
    }

    public static DateFormats getInstance() {
        if (instance == null) {
            instance = new DateFormats();
            instance.init();
        }
        return instance;
    }

    private void init() {
        this.availableDateFormats.clear();
        this.availableDateFormats.add("yyyy.MM.dd HH:mm:ss");
        this.availableDateFormats.add("yyyy.MM.dd'T'HH:mm:ss");
        this.availableDateFormats.add("yyyy.MM.dd HH:mm:ss.SSS");
        this.availableDateFormats.add("MM.dd.yyyy HH:mm");
        this.availableDateFormats.add("dd.MM.yyyy HH:mm:ss");
        this.availableDateFormats.add("dd.MM.yyyy HH:mm:ss.SSS");
        this.availableDateFormats.add("yyyy.MM.dd HH:mm");
        this.availableDateFormats.add("dd.MM.yyyy HH:mm");
        this.availableDateFormats.add("yyyy.MM.dd");
        this.availableDateFormats.add("dd.MM.yyyy");
        this.availableDateFormats.add("MM.dd.yyyy");
        this.availableDateFormats.add("yyyy/MM/dd HH:mm:ss");
        this.availableDateFormats.add("yyyy/MM/dd'T'HH:mm:ss");
        this.availableDateFormats.add("yyyy/MM/dd HH:mm:ss.SSS");
        this.availableDateFormats.add("MM/dd/yyyy HH:mm");
        this.availableDateFormats.add("dd/MM/yyyy hh:mm:ss aa");
        this.availableDateFormats.add("dd/MM/yyyy HH:mm:ss");
        this.availableDateFormats.add("dd/MM/yyyy HH:mm:ss.SSS");
        this.availableDateFormats.add("yyyy/MM/dd HH:mm");
        this.availableDateFormats.add("dd/MM/yyyy HH:mm");
        this.availableDateFormats.add("yyyy/MM/dd");
        this.availableDateFormats.add("dd/MM/yyyy");
        this.availableDateFormats.add("MM/dd/yyyy");
        this.availableDateFormats.add("yyyy-MM-dd HH:mm:ss");
        this.availableDateFormats.add("yyyy-MM-dd'T'HH:mm:ss");
        this.availableDateFormats.add("yyyy-MM-dd HH:mm:ss.SSS");
        this.availableDateFormats.add("MM-dd-yyyy HH:mm");
        this.availableDateFormats.add("dd-MM-yyyy HH:mm:ss");
        this.availableDateFormats.add("dd-MM-yyyy HH:mm:ss.SSS");
        this.availableDateFormats.add("yyyy-MM-dd HH:mm");
        this.availableDateFormats.add("dd-MM-yyyy HH:mm");
        this.availableDateFormats.add("yyyy-MM-dd");
        this.availableDateFormats.add("dd-MM-yyyy");
        this.availableDateFormats.add("MM-dd-yyyy");
        this.availableDateFormats.add(LONG_MILLIS);
        this.availableDateFormats.add(LONG_SECONDS);
    }

    public String recognize(String string) {
        boolean bl = this.checkLong(string);
        for (String string2 : this.availableDateFormats) {
            try {
                if (bl && (string2.equals(LONG_MILLIS) || string2.equals(LONG_SECONDS))) {
                    boolean bl2 = this.detectSec(Long.valueOf(string));
                    if (string2.equals(LONG_MILLIS) && !bl2) {
                        return string2;
                    }
                    if (!string2.equals(LONG_SECONDS) || !bl) continue;
                    return string2;
                }
                if (!SQUtils.dateFormatValid(string, string2)) continue;
                return string2;
            }
            catch (Exception exception) {
            }
        }
        return null;
    }

    public boolean detectSec(long l) {
        return Math.abs(l) < 4000000000L;
    }

    private boolean checkLong(String string) {
        try {
            Long.parseLong(string);
            return true;
        }
        catch (Exception exception) {
            return false;
        }
    }
}

