/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.mail.SimpleEmail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmailSender {
    private static final Logger Log = LoggerFactory.getLogger(EmailSender.class);

    public static void sendTo(String string, int n, String string2, String string3, String string4, String string5, String string6, String string7, boolean bl) throws Exception {
        try {
            SimpleEmail simpleEmail = new SimpleEmail();
            simpleEmail.setHostName(string);
            simpleEmail.setSmtpPort(n);
            simpleEmail.setAuthentication(string2, string3);
            simpleEmail.setFrom(string6);
            simpleEmail.addTo(string7);
            simpleEmail.setSubject(string4);
            simpleEmail.setMsg(string5);
            simpleEmail.setSSLOnConnect(bl);
            simpleEmail.send();
        }
        catch (Exception exception) {
            throw new Exception("Email could not be sent to '" + string7 + "'. Exc.", exception);
        }
    }

    public static boolean isValidEmailAddress(String string) {
        boolean bl = true;
        String string2 = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
        String string3 = ".+@.+\\.[A-Za-z]{2}[A-Za-z]*";
        String string4 = bl ? string2 : string3;
        Pattern pattern = Pattern.compile(string4);
        Matcher matcher = pattern.matcher(string);
        return matcher.matches();
    }
}

