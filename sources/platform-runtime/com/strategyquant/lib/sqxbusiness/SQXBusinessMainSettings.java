/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.sqxbusiness;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.sqxbusiness.MQLMarketConst;
import java.io.File;
import java.util.concurrent.locks.StampedLock;
import org.jdom2.Content;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQXBusinessMainSettings {
    private static final Logger Log = LoggerFactory.getLogger(SQXBusinessMainSettings.class);
    private static String mt4Path = "";
    private static String mt5Path = "";
    private static StampedLock mt4Lock = new StampedLock();
    private static StampedLock mt5Lock = new StampedLock();

    public static void init() {
        Log.debug("Loading main settings...");
        File file = new File(MQLMarketConst.projectsPath + "/" + "config.xml");
        if (!file.exists()) {
            Log.debug("Config file '" + file.getAbsolutePath() + "' doesn't exist");
            return;
        }
        try {
            Element element;
            Element element2 = XMLUtil.stringToElement(SQUtils.fileToString(file));
            Element element3 = element2.getChild("MetaTrader4");
            if (element3 != null) {
                SQXBusinessMainSettings.setMT4Path(element3.getAttributeValue("path"));
            }
            if ((element = element2.getChild("MetaTrader5")) != null) {
                SQXBusinessMainSettings.setMT5Path(element.getAttributeValue("path"));
            }
        }
        catch (Exception exception) {
            Log.error("Error while loading config file", (Throwable)exception);
        }
    }

    public static String getMT4Path() {
        long l = mt4Lock.readLock();
        try {
            String string = mt4Path;
            return string;
        }
        finally {
            mt4Lock.unlock(l);
        }
    }

    public static String getMT5Path() {
        long l = mt5Lock.readLock();
        try {
            String string = mt5Path;
            return string;
        }
        finally {
            mt5Lock.unlock(l);
        }
    }

    public static void save(String string, String string2) {
        Log.info("Saving main settings...");
        SQXBusinessMainSettings.setMT4Path(string);
        SQXBusinessMainSettings.setMT5Path(string2);
        File file = new File(MQLMarketConst.projectsPath + "/" + "config.xml");
        try {
            Element element = new Element("Settings");
            Element element2 = new Element("MetaTrader4");
            element2.setAttribute("path", string);
            element.addContent((Content)element2);
            Element element3 = new Element("MetaTrader5");
            element3.setAttribute("path", string2);
            element.addContent((Content)element3);
            SQUtils.stringToFile(file, XMLUtil.elementToString(element, true));
        }
        catch (Exception exception) {
            Log.error("Error while saving main settings", (Throwable)exception);
            throw exception;
        }
    }

    private static void setMT4Path(String string) {
        long l = mt4Lock.writeLock();
        try {
            mt4Path = string;
        }
        finally {
            mt4Lock.unlock(l);
        }
    }

    private static void setMT5Path(String string) {
        long l = mt5Lock.writeLock();
        try {
            mt5Path = string;
        }
        finally {
            mt5Lock.unlock(l);
        }
    }
}

