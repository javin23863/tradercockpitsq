/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.app.impl;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.snippets.compile.SQStructure;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import org.jdom2.Content;
import org.jdom2.Element;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainAppSettings {
    public static final Logger Log = LoggerFactory.getLogger(MainAppSettings.class);
    protected HashMap<String, String> values = new HashMap();
    private String FilePath = SQStructure.INTERNAL_DIR_PATH + "AppSettings.txt";
    private static MainAppSettings instance = null;

    private MainAppSettings() {
        this.load();
    }

    public static MainAppSettings getInstance() {
        if (instance == null) {
            instance = new MainAppSettings();
        }
        return instance;
    }

    public String get(String string) {
        return this.get(string, null);
    }

    public boolean containsKey(String string) {
        return this.values.containsKey(string);
    }

    public String get(String string, String string2) {
        if (!this.values.containsKey(string)) {
            return string2;
        }
        return this.values.get(string);
    }

    public void set(String string, String string2) {
        if (string == null || string.trim().isEmpty() || string2 == null) {
            return;
        }
        this.values.put(string, string2);
        this.save();
    }

    public void load() {
        try {
            File file = new File(this.FilePath);
            if (file.exists()) {
                Element element = XMLUtil.fileToXmlElement(file);
                List list = element.getChildren();
                for (Element element2 : list) {
                    this.set(element2.getName(), element2.getValue());
                }
            }
        }
        catch (Exception exception) {
            Log.error("Cannot load settings. Exc.", (Throwable)exception);
        }
    }

    public void save() {
        try {
            File file = new File(this.FilePath);
            Element element = new Element("Settings");
            for (String string : this.values.keySet()) {
                try {
                    element.addContent((Content)new Element(string).setText(this.values.get(string)));
                }
                catch (Exception exception) {
                    Log.error("Cannot save setting. Exc.", (Throwable)exception);
                }
            }
            XMLUtil.xmlToFile(element, file);
        }
        catch (Exception exception) {
            Log.error("Error occurred while saving settings", (Throwable)exception);
        }
    }

    public JSONObject toJSON() {
        JSONObject jSONObject = new JSONObject();
        for (String string : this.values.keySet()) {
            jSONObject.put(string, (Object)this.get(string));
        }
        return jSONObject;
    }

    public boolean getBoolean(String string, boolean bl) {
        String string2 = this.get(string, null);
        if (string2 == null) {
            return bl;
        }
        return Boolean.parseBoolean(string2);
    }
}

