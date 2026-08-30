/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.app;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import org.jdom2.Content;
import org.jdom2.Element;
import org.json.JSONObject;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectInput;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppSettings {
    public static final String SkinLight = "Light skin";
    public static final Logger Log = LoggerFactory.getLogger(AppSettings.class);
    protected HashMap<String, String> values = new HashMap();

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
    }

    public void load() {
        this.putDefaultValues();
        this.convertOldSettingsFormat();
        try {
            File file = new File(SQPaths.appSettingsFile);
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
        this.putInternalValues();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void convertOldSettingsFormat() {
        FilterInputStream filterInputStream = null;
        try {
            File file = new File(SQPaths.settingsDirPath + "/settings.dat");
            if (!file.exists()) {
                return;
            }
            filterInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(file)));
            FSTConfiguration fSTConfiguration = FSTConfiguration.createDefaultConfiguration();
            FSTObjectInput fSTObjectInput = fSTConfiguration.getObjectInput((InputStream)filterInputStream);
            HashMap hashMap = (HashMap)fSTObjectInput.readObject(new Class[]{HashMap.class});
            for (String string : hashMap.keySet()) {
                if (string.trim().isEmpty()) continue;
                this.set(string, hashMap.get(string) + "");
            }
            filterInputStream.close();
            this.save();
            file.delete();
        }
        catch (Exception exception) {
            Log.error("Error occurred while converting settings from old format. Exc.", (Throwable)exception);
        }
        finally {
            if (filterInputStream != null) {
                try {
                    filterInputStream.close();
                }
                catch (IOException iOException) {
                    iOException.printStackTrace();
                }
            }
        }
    }

    public void putDefaultValues() {
        this.values.put("skin", SkinLight);
        this.values.put("language", "English");
        this.values.put("dontStorePendingOrders", "true");
        this.values.put("dontStoreOP3DChartsData", "true");
        this.values.put("gpuAccelerated", "true");
        this.values.put("ComputePipsMetrics", "false");
        this.values.put("ComputePctsMetrics", "false");
        this.values.put("ComputeSeparateMetrics", "true");
    }

    private void putInternalValues() {
        this.values.put("appPath", MainApp.getDataPath());
        this.values.put("projectsPath", MainApp.checkProduct("QDM") ? SQPaths.exportDirPath : SQPaths.projectsDirPath);
        this.values.put("userPath", SQPaths.userDirPath);
        this.values.put("settingsPath", SQPaths.settingsDirPath);
        this.values.put("testsPath", SQPaths.testsDirPath);
        this.values.put("templatesPath", SQPaths.bbTemplatesDirPath);
        this.values.put("strategyTemplatesPath", SQPaths.strategyTemplatesDirPath);
        this.values.put("strategiesPath", SQPaths.strategiesDirPath);
        this.values.put("configsPath", SQPaths.configsDirPath);
        this.values.put("buildTemplatesPath", SQPaths.buildTemplatesDirPath);
    }

    public void save() {
        try {
            File file = new File(SQPaths.appSettingsFile);
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

