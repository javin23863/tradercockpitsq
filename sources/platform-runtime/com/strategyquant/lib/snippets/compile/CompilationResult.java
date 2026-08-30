/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.snippets.compile;

import com.strategyquant.lib.snippets.compile.CompilationMessage;
import com.strategyquant.lib.snippets.compile.SourceFile;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompilationResult {
    public static final Logger Log = LoggerFactory.getLogger(CompilationResult.class);
    private boolean isRecompiled;
    public SourceFile sourceFile;
    public boolean success = true;
    public String sourceCodePath = null;
    public String logTabTitle = "Log";
    public List<CompilationMessage> messageList = new ArrayList<CompilationMessage>();

    public void setRecompiled(boolean bl) {
        this.isRecompiled = bl;
    }

    public boolean isRecompiled() {
        return this.isRecompiled;
    }

    public void addCompilationMessage(int n, String string) {
        this.messageList.add(new CompilationMessage(this.sourceCodePath, n, string));
    }

    public void writeCompilationInfoToFile(File file) {
        try {
            if (file == null || file.isDirectory()) {
                return;
            }
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            try (ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);){
                objectOutputStream.writeObject(this.logTabTitle);
                objectOutputStream.writeObject(this.messageList);
            }
            fileOutputStream.close();
        }
        catch (Exception exception) {
            Log.error("Exc.", (Throwable)exception);
        }
    }

    public void readCompilationInfoFromFile(File file) {
        try {
            if (file == null || !file.exists()) {
                return;
            }
            FileInputStream fileInputStream = new FileInputStream(file);
            try (ObjectInputStream objectInputStream = new ObjectInputStream(fileInputStream);){
                this.logTabTitle = (String)objectInputStream.readObject();
                this.messageList = (ArrayList)objectInputStream.readObject();
            }
            fileInputStream.close();
        }
        catch (Exception exception) {
            Log.error("Exc.", (Throwable)exception);
        }
    }

    public String getAsString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (CompilationMessage compilationMessage : this.messageList) {
            String string;
            String string2 = string = compilationMessage.clazz == null ? "" : compilationMessage.clazz + ", ";
            if (compilationMessage.type == 20) {
                stringBuilder.append("Error: ").append(string).append(compilationMessage.message);
            } else {
                stringBuilder.append("Info: ").append(string).append(compilationMessage.message);
            }
            if (compilationMessage.line != null && compilationMessage.line > 0L) {
                stringBuilder.append(", LINE: ").append(compilationMessage.line);
            }
            if (compilationMessage.column != null && compilationMessage.column > 0L) {
                stringBuilder.append(", COLUMN: ").append(compilationMessage.column);
            }
            stringBuilder.append("\n\n");
        }
        return stringBuilder.toString();
    }

    private static String stringToHTMLString(String string) {
        StringBuilder stringBuilder = new StringBuilder(string.length());
        boolean bl = false;
        int n = string.length();
        for (int i = 0; i < n; ++i) {
            char c = string.charAt(i);
            if (c == ' ') {
                if (bl) {
                    bl = false;
                    stringBuilder.append("&nbsp;");
                    continue;
                }
                bl = true;
                stringBuilder.append(' ');
                continue;
            }
            bl = false;
            if (c == '\"') {
                stringBuilder.append("&quot;");
                continue;
            }
            if (c == '&') {
                stringBuilder.append("&amp;");
                continue;
            }
            if (c == '<') {
                stringBuilder.append("&lt;");
                continue;
            }
            if (c == '>') {
                stringBuilder.append("&gt;");
                continue;
            }
            if (c == '\n') {
                stringBuilder.append("<br/>");
                continue;
            }
            int n2 = 0xFFFF & c;
            if (n2 < 160) {
                stringBuilder.append(c);
                continue;
            }
            stringBuilder.append("&#");
            stringBuilder.append(n2);
            stringBuilder.append(';');
        }
        return stringBuilder.toString();
    }

    public String getAsHTMLString() {
        return CompilationResult.stringToHTMLString(this.getAsString());
    }

    public String getAsSimpleString() {
        return this.getAsString().replaceAll("<[^>]++>", "");
    }

    public JSONObject toJSON() {
        JSONObject jSONObject = new JSONObject();
        JSONArray jSONArray = new JSONArray();
        for (int i = 0; i < this.messageList.size(); ++i) {
            CompilationMessage compilationMessage = this.messageList.get(i);
            JSONArray jSONArray2 = new JSONArray();
            jSONArray2.put((Object)compilationMessage.message);
            jSONArray2.put((Object)compilationMessage.kind);
            jSONArray2.put((Object)compilationMessage.clazz);
            jSONArray2.put(compilationMessage.line == null || compilationMessage.line < 0L ? 0L : compilationMessage.line);
            jSONArray2.put(compilationMessage.column == null || compilationMessage.column < 0L ? 0L : compilationMessage.column);
            JSONObject jSONObject2 = new JSONObject();
            jSONObject2.put("file", (Object)compilationMessage.sourceCodePath);
            JSONObject jSONObject3 = new JSONObject();
            jSONObject3.put("id", (Object)("r" + i));
            jSONObject3.put("data", (Object)jSONArray2);
            jSONObject3.put("userdata", (Object)jSONObject2);
            jSONArray.put((Object)jSONObject3);
        }
        jSONObject.put("rows", (Object)jSONArray);
        jSONObject.put("success", this.success);
        return jSONObject;
    }
}

