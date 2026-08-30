/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib;

import com.strategyquant.lib.app.MainApp;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class L {
    public static final Logger Log = LoggerFactory.getLogger(L.class);
    public static final String SEPARATOR = ";";
    public static final String SemicolonReplacement = "#semicolon#";
    public static final String FILE_EXTENSION = ".csv";
    public static final String ENG_LANG = "English";
    public static ArrayList<String> langList = new ArrayList();
    public static TreeMap<String, String> translations = new TreeMap();
    public static Map<String, String> flagsMap = new HashMap<String, String>();
    public static Map<String, String> fontsMap = new HashMap<String, String>();
    public static String lang;
    public static File langDirFile;
    public static boolean debug;
    private static Pattern pattern;

    private L() {
    }

    public static boolean isAvailable(String string) {
        return langList.contains(string);
    }

    public static String tsq(String string) {
        return string;
    }

    public static String tnp(String string, Object ... objectArray) {
        return L.t(string, objectArray);
    }

    public static String t(String string, Object ... objectArray) {
        if (translations.containsKey(string)) {
            string = translations.get(string);
        }
        if (debug) {
            string = "*" + string + "*";
        }
        try {
            if (objectArray == null || objectArray.length == 0) {
                return string;
            }
            return String.format(string, objectArray);
        }
        catch (Exception exception) {
            String string2 = string + " [";
            for (int i = 0; i < objectArray.length; ++i) {
                Object object = objectArray[i];
                string2 = string2 + object;
                if (i >= objectArray.length - 1) continue;
                string2 = string2 + ",";
            }
            string2 = string2 + "]";
            if (!MainApp.isRelease()) {
                Log.error(String.format("Failed to translate text: %s", string2));
            }
            return string2;
        }
    }

    public static String tHtml(String string) {
        if (translations.containsKey(string)) {
            string = translations.get(string);
        }
        if (debug) {
            string = "*" + string + "*";
        }
        return "<html>" + string + "</html>";
    }

    public static String getFilePath(String string) {
        return langDirFile + "/" + string + FILE_EXTENSION;
    }

    public static void loadLangFileToMap(String string) {
        Object object;
        if (lang != null && lang.equals(string)) {
            return;
        }
        lang = string;
        String string2 = "";
        String string3 = "";
        ArrayList<String> arrayList = new ArrayList<String>();
        translations.clear();
        try {
            string3 = L.getFilePath(string);
            object = new File(string3);
            if (!((File)object).exists()) {
                return;
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream((File)object), StandardCharsets.UTF_8));
            String[] object2 = new String[2];
            int n = 1;
            while ((string2 = bufferedReader.readLine()) != null) {
                try {
                    if (string2.isEmpty()) continue;
                    if (!L.checkPlaceholders(string2)) {
                        throw new Exception("Placeholders misconfiguration.");
                    }
                    String[] stringArray = (string2 = string2.replaceAll(";$", "")).split(SEPARATOR);
                    if (stringArray.length == 2) {
                        translations.put(stringArray[0].replaceAll(SemicolonReplacement, SEPARATOR), stringArray[1].replaceAll(SemicolonReplacement, SEPARATOR));
                    } else {
                        if (stringArray[0].contains("#flag") || stringArray[0].contains("#font")) continue;
                        String string4 = "";
                        String string5 = "";
                        for (int i = 0; i < stringArray.length / 2; ++i) {
                            string4 = string4 + (i == 0 ? "" : SEPARATOR) + stringArray[i];
                            string5 = string5 + (i == 0 ? "" : SEPARATOR) + stringArray[stringArray.length / 2 + i];
                        }
                        translations.put(string4.replaceAll(SemicolonReplacement, SEPARATOR), string5.replaceAll(SemicolonReplacement, SEPARATOR));
                    }
                }
                catch (Exception exception) {
                    arrayList.add("SKIPPED Line" + n + "!!!: " + string2);
                }
                ++n;
            }
            bufferedReader.close();
        }
        catch (Exception exception) {
            Log.error("Cannot load lang file '" + string + "'. Exc.", (Throwable)exception);
        }
        if (!arrayList.isEmpty()) {
            object = "";
            for (String string6 : arrayList) {
                object = (String)object + string6 + "\n";
            }
            Log.warn(String.format("Lang file '%s' contains %d invalid lines.\n%s", lang, arrayList.size(), object));
        }
    }

    public static boolean checkPlaceholders(String string) {
        String[] stringArray = string.split(SEPARATOR);
        if (stringArray.length == 2) {
            String string2 = stringArray[0];
            String string3 = stringArray[1];
            Matcher matcher = pattern.matcher(string2);
            while (matcher.find()) {
                String string4 = matcher.group(0);
                if (string3.contains(string4)) continue;
                return false;
            }
        }
        return true;
    }

    public static void loadAvailableLangs(File file) {
        langDirFile = file;
        if (!file.exists() || !file.isDirectory()) {
            return;
        }
        langList.clear();
        flagsMap.clear();
        fontsMap.clear();
        File[] fileArray = file.listFiles();
        if (fileArray == null) {
            return;
        }
        for (int i = 0; i < fileArray.length; ++i) {
            File file2 = fileArray[i];
            if (!file2.isFile() || !file2.getName().endsWith(FILE_EXTENSION)) continue;
            String string = file2.getName().substring(0, file2.getName().lastIndexOf(FILE_EXTENSION));
            langList.add(string);
            try {
                String string2;
                int n = 0;
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(file2), StandardCharsets.UTF_8));
                while ((string2 = bufferedReader.readLine()) != null) {
                    String[] stringArray = string2.split(SEPARATOR);
                    if (stringArray[0].contains("#flag")) {
                        flagsMap.put(string, stringArray[1]);
                    } else if (stringArray[0].contains("#font")) {
                        fontsMap.put(string, stringArray[1]);
                    }
                    if (++n <= 10) continue;
                    break;
                }
                bufferedReader.close();
                continue;
            }
            catch (Exception exception) {
                Log.error("Cannot parse flag from lang file '" + file2.getName() + "'. Exc.", (Throwable)exception);
            }
        }
    }

    public static String getCurLangShortCut() {
        if (lang == null || flagsMap == null) {
            return null;
        }
        return flagsMap.get(lang);
    }

    public static String getFont() {
        if (lang == null || fontsMap == null) {
            return null;
        }
        return fontsMap.get(lang);
    }

    static {
        langDirFile = null;
        debug = new File(MainApp.getDataPath() + "/debuglangs.txt").exists();
        pattern = Pattern.compile("#(.+?\\S)#");
    }
}

