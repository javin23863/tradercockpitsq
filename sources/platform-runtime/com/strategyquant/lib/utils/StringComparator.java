/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

import java.util.Comparator;

public class StringComparator
implements Comparator<String> {
    int compareRight(String string, String string2) {
        int n = 0;
        int n2 = 0;
        int n3 = 0;
        while (true) {
            char c = StringComparator.charAt(string, n2);
            char c2 = StringComparator.charAt(string2, n3);
            if (!Character.isDigit(c) && !Character.isDigit(c2)) {
                return n;
            }
            if (!Character.isDigit(c)) {
                return -1;
            }
            if (!Character.isDigit(c2)) {
                return 1;
            }
            if (c < c2) {
                if (n == 0) {
                    n = -1;
                }
            } else if (c > c2) {
                if (n == 0) {
                    n = 1;
                }
            } else if (c == '\u0000' && c2 == '\u0000') {
                return n;
            }
            ++n2;
            ++n3;
        }
    }

    @Override
    public int compare(String string, String string2) {
        int n = 0;
        int n2 = 0;
        int n3 = 0;
        int n4 = 0;
        string = string.toLowerCase();
        string2 = string2.toLowerCase();
        while (true) {
            int n5;
            n4 = 0;
            n3 = 0;
            char c = StringComparator.charAt(string, n);
            char c2 = StringComparator.charAt(string2, n2);
            while (Character.isSpaceChar(c) || c == '0') {
                n3 = c == '0' ? ++n3 : 0;
                c = StringComparator.charAt(string, ++n);
            }
            while (Character.isSpaceChar(c2) || c2 == '0') {
                n4 = c2 == '0' ? ++n4 : 0;
                c2 = StringComparator.charAt(string2, ++n2);
            }
            if (Character.isDigit(c) && Character.isDigit(c2) && (n5 = this.compareRight(string.substring(n), string2.substring(n2))) != 0) {
                return n5;
            }
            if (c == '\u0000' && c2 == '\u0000') {
                return n3 - n4;
            }
            if (c < c2) {
                return -1;
            }
            if (c > c2) {
                return 1;
            }
            ++n;
            ++n2;
        }
    }

    static char charAt(String string, int n) {
        if (n >= string.length()) {
            return '\u0000';
        }
        return string.charAt(n);
    }
}

