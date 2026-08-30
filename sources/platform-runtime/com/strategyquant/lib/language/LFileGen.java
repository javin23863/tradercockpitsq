/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.language;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.snippets.compile.SQStructure;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LFileGen {
    public static final Logger Log = LoggerFactory.getLogger(LFileGen.class);
    private LinkedHashMap<String, String> translationsFromLangFile = new LinkedHashMap();
    private LinkedHashMap<String, String> translationsParsed = new LinkedHashMap();
    public int totalErrors = 0;
    public String appPath = null;
    public String AWLangPath = SQStructure.INTERNAL_DIR_PATH + "web/AlgoWizard/translations.csv";
    private List<String> skipFoldersPaths = new ArrayList<String>();

    public void start() {
        Log.info("Regenerating lang files");
        long l = System.currentTimeMillis();
        String string = this.getDataPath();
        this.skipFoldersPaths.add(new File(string + "internal/web/AlgoWizard").getAbsolutePath());
        L.loadAvailableLangs(new File(string + "internal/langs"));
        for (String string2 : L.langList) {
            this.translationsFromLangFile.clear();
            this.translationsParsed.clear();
            String string3 = L.getFilePath(string2);
            this.loadLangFileToList(string3, this.translationsFromLangFile);
            this.loadLangFileToList(this.AWLangPath, this.translationsFromLangFile);
            this.loadLangFileToList(this.AWLangPath, this.translationsParsed);
            File file = new File(string + "/../../projects");
            if (file.exists()) {
                this.parseFiles(file.getAbsolutePath());
            }
            this.parseFiles(string + "internal/web");
            this.parseFiles(string + "plugins");
            this.parseFiles(string + "internal/extend/Snippets");
            this.createLangFile(string3);
        }
        long l2 = System.currentTimeMillis() - l;
        Log.info("Lang files regenerated in " + l2 + "ms. Total errors: " + this.totalErrors);
    }

    private String getDataPath() {
        if (this.appPath == null) {
            File file = new File("");
            this.appPath = file.getAbsolutePath();
            this.appPath = this.appPath.replaceAll(Pattern.quote("\\"), "/");
            this.appPath = this.appPath + "/";
        }
        return this.appPath;
    }

    private void parseFiles(String string) {
        File[] fileArray;
        File file = new File(string);
        if (this.skipFoldersPaths.contains(file.getAbsolutePath())) {
            return;
        }
        if (file.exists() && null != (fileArray = file.listFiles())) {
            for (File file2 : fileArray) {
                String string2 = file2.getAbsolutePath();
                if (file2.isDirectory()) {
                    this.parseFiles(string2);
                    continue;
                }
                String string3 = SQUtils.getExtension(string2);
                if (string3.equalsIgnoreCase("java")) {
                    this.parseJavaFile(file2);
                    continue;
                }
                if (string3.equalsIgnoreCase("html")) {
                    this.parseHtmlFile(file2);
                    this.parseJSFile(file2);
                    continue;
                }
                if (!string3.equalsIgnoreCase("js") && !string3.equalsIgnoreCase("vue")) continue;
                this.parseHtmlFile(file2);
                this.parseJSFile(file2);
            }
        }
    }

    private void parseHtmlFile(File file) {
        try {
            String string = SQUtils.fileToString(file);
            this.parseFilterContent(file, string);
            int n = 0;
            int n2 = 0;
            try {
                while ((n = string.indexOf("tsq", n)) != -1) {
                    if (string.charAt(n - 1) == '/' || string.charAt(n + 3) != ' ' && string.charAt(n + 3) != '>') {
                        n += 3;
                        continue;
                    }
                    int n3 = n;
                    if ((n = string.indexOf(">", n)) != -1 && n - n3 <= 100 && (n2 = string.indexOf("<", n)) != -1) {
                        String string2 = string.substring(n + 1, n2);
                        n = n2 + 1;
                        string2 = string2.trim();
                        string2 = string2.replaceAll("\\s*\r\n\\s*", " ");
                        string2 = string2.replaceAll("\\s*\n\\s*", " ");
                        string2 = string2.replaceAll(" +", " ");
                        if ((string2 = this.trim(string2)).isEmpty() || this.translationsParsed.containsKey(string2)) continue;
                        this.safePut(this.translationsParsed, string2, string2);
                        continue;
                    }
                    break;
                }
            }
            catch (Exception exception) {
                ++this.totalErrors;
                Log.error("Error while parsing html file '" + file.getName() + "'. " + exception.getMessage());
            }
        }
        catch (Exception exception) {
            ++this.totalErrors;
            Log.error("Error while parsing html file '" + file.getName() + "'. " + exception.getMessage());
        }
    }

    private void parseJavaFile(File file) {
        try {
            String string = SQUtils.fileToString(file);
            this.parseFunctionCallContent(file, string, "L.t(");
            this.parseFunctionCallContent(file, string, "L.tsq(");
            this.parseFunctionCallContent(file, string, "L.tHtml(");
            this.parseAnnotation(file, string, "@Help", null);
            this.parseAnnotation(file, string, "@ClassConfig", new String[]{"name", "display"});
            this.parseAnnotation(file, string, "@Parameter", new String[]{"name", "category"});
            this.parseAnnotation(file, string, "@Description", null);
        }
        catch (Exception exception) {
            ++this.totalErrors;
            Log.error("Error while parsing java file '" + file.getName() + "'. " + exception.getMessage());
        }
    }

    private void parseJSFile(File file) {
        try {
            String string = SQUtils.fileToString(file);
            this.parseFunctionCallContent(file, string, "L.tsq(");
            this.parseFunctionCallContent(file, string, "Ltsq(");
        }
        catch (Exception exception) {
            ++this.totalErrors;
            Log.error("Error while parsing js file '" + file.getName() + "'. " + exception.getMessage());
        }
    }

    private void parseFunctionCallContent(File file, String string, String string2) {
        int n = 0;
        String string3 = null;
        int n2 = 0;
        try {
            block2: while ((n = string.indexOf(string2, n)) != -1) {
                char c = string.charAt(n += string2.length());
                ++n;
                string3 = "";
                n2 = 1;
                while (n < string.length()) {
                    char c2 = string.charAt(n);
                    string3 = string3 + c2;
                    if (c2 == '(') {
                        ++n2;
                    } else if (c2 == ')' && --n2 == 0) {
                        int n3 = this.getApostrophePos(string3, c, 0);
                        if (n3 == -1) continue block2;
                        string3 = string3.substring(0, n3);
                        if (this.translationsParsed.containsKey(string3 = this.trim(string3))) continue block2;
                        this.safePut(this.translationsParsed, string3, string3);
                        continue block2;
                    }
                    ++n;
                }
            }
        }
        catch (Exception exception) {
            ++this.totalErrors;
            Log.error("Error while parsing file '" + file.getName() + "'. " + exception.getMessage(), (Throwable)exception);
        }
    }

    private int getApostrophePos(String string, char c, int n) {
        int n2 = -1;
        int n3 = n;
        while (n2 < 0 && (n3 = string.indexOf(c, n3)) >= 0) {
            if (n3 == 0 || string.charAt(n3 - 1) != '\\') {
                n2 = n3;
                break;
            }
            ++n3;
        }
        return n2;
    }

    private void parseAnnotation(File file, String string, String string2, String[] stringArray) {
        int n = 0;
        int n2 = 0;
        try {
            while ((n = string.indexOf(string2, n)) != -1) {
                if (string.charAt(n += string2.length()) != '(') continue;
                String string3 = string.substring(n, string.indexOf("\n", n)).trim();
                n2 = string3.lastIndexOf(41);
                string3 = string3.substring(1, n2);
                if (stringArray == null) {
                    string3 = string3.substring(1, string3.length() - 1);
                    if (!this.translationsParsed.containsKey(string3 = this.trim(string3))) {
                        this.safePut(this.translationsParsed, string3, string3);
                    }
                } else {
                    for (int i = 0; i < stringArray.length; ++i) {
                        String string4 = stringArray[i] + "=";
                        int n3 = string3.indexOf(string4);
                        if (n3 < 0) continue;
                        int n4 = string3.indexOf("\"", n3);
                        int n5 = this.getApostrophePos(string3, '\"', n4 + 1);
                        String string5 = string3.substring(n4 + 1, n5);
                        if (this.translationsParsed.containsKey(string5 = this.trim(string5))) continue;
                        this.safePut(this.translationsParsed, string5, string5);
                    }
                }
                n += n2;
            }
        }
        catch (Exception exception) {
            ++this.totalErrors;
            Log.error("Error while parsing annotation '" + string2 + "' from java file '" + file.getName() + "'. " + exception.getMessage());
        }
    }

    private void parseFilterContent(File file, String string) {
        int n = 0;
        int n2 = 0;
        String string2 = " | tsq";
        try {
            while ((n = string.indexOf(string2, n)) != -1) {
                char c;
                n2 = n - 1;
                if (n2 >= 0 && ((c = string.charAt(n2)) == '\'' || c == '\"')) {
                    int n3 = n2;
                    while (n3-- > 0) {
                        if (string.charAt(n3) != c || n3 <= 0 || string.charAt(n3 - 1) == '\\') continue;
                        String string3 = string.substring(n3 + 1, n2);
                        if (this.translationsParsed.containsKey(string3 = this.trim(string3))) break;
                        this.safePut(this.translationsParsed, string3, string3);
                        break;
                    }
                }
                n += string2.length();
            }
        }
        catch (Exception exception) {
            ++this.totalErrors;
            Log.error("Error while parsing html filter from html file '" + file.getName() + "'. " + exception.getMessage());
        }
    }

    private void createLangFile(String string) {
        try {
            String string2;
            String string3;
            BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter((OutputStream)new FileOutputStream(string), StandardCharsets.UTF_8));
            for (Map.Entry<String, String> entry : this.translationsFromLangFile.entrySet()) {
                string3 = entry.getKey();
                string2 = entry.getValue();
                if (string3.contains(";") || string2.contains(";") || string3.trim().isEmpty() || string2.trim().isBlank()) continue;
                bufferedWriter.write(string3 + ";" + string2);
                bufferedWriter.write("\r\n");
            }
            for (Map.Entry<String, String> entry : this.translationsParsed.entrySet()) {
                string3 = entry.getKey();
                string2 = entry.getValue();
                if (string3.contains(";") || string2.contains(";") || string3.trim().isEmpty() || string2.trim().isBlank() || this.translationsFromLangFile.containsKey(string3)) continue;
                bufferedWriter.write(string3 + ";" + string2);
                bufferedWriter.write("\r\n");
            }
            bufferedWriter.close();
        }
        catch (Exception exception) {
            ++this.totalErrors;
            Log.error("Error while creating lang file '" + string + "'. " + exception.getMessage());
        }
    }

    private void loadLangFileToList(String string, LinkedHashMap<String, String> linkedHashMap) {
        try {
            String string2 = "";
            String string3 = "";
            File file = new File(string);
            if (!file.exists()) {
                return;
            }
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((InputStream)new FileInputStream(file), StandardCharsets.UTF_8));
            String[] stringArray = new String[2];
            int n = 1;
            while ((string2 = bufferedReader.readLine()) != null) {
                string3 = "";
                boolean bl = L.checkPlaceholders(string2);
                try {
                    if (!string2.contains(";")) {
                        throw new Exception("Incorrect line definition!");
                    }
                    stringArray = string2.split(";");
                    if (stringArray.length % 2 == 0 && !stringArray[1].trim().equals("")) {
                        string3 = stringArray[1];
                    } else {
                        string3 = stringArray[0];
                        ++this.totalErrors;
                        Log.debug("Error while loading lang file. Line " + n + ": " + string2);
                    }
                    String string4 = stringArray[0];
                    string4 = this.trim(string4);
                    String string5 = string3 = bl ? this.trim(string3) : string4;
                    if (!linkedHashMap.containsKey(string4)) {
                        this.safePut(linkedHashMap, string4, string3);
                    }
                }
                catch (Exception exception) {
                    ++this.totalErrors;
                    Log.debug("Error while loading lang file. Line " + n + ": " + string2);
                }
                ++n;
            }
            bufferedReader.close();
        }
        catch (Exception exception) {
            Log.error("Errow while loading lang file " + string, (Throwable)exception);
        }
    }

    private void safePut(LinkedHashMap<String, String> linkedHashMap, String string, String string2) {
        string = string.replaceAll(";", "#semicolon#").trim();
        string2 = string2.replaceAll(";", "#semicolon#").trim();
        if (string.startsWith("\"") && string.endsWith("\"")) {
            string = string.replaceFirst("\"", "");
            string = string.substring(0, string.length() - 1);
        }
        if (string2.startsWith("\"") && string2.endsWith("\"")) {
            string2 = string2.replaceFirst("\"", "");
            string2 = string2.substring(0, string2.length() - 1);
        }
        linkedHashMap.put(string, string2);
    }

    private String trim(String string) {
        if ((string = string.trim()).startsWith("\"") && string.endsWith("\"")) {
            string = string.replaceFirst("\"", "");
            string = string.substring(0, string.length() - 1);
        }
        return string;
    }
}

