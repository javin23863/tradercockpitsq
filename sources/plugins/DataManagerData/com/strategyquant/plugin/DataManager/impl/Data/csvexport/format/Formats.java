package com.strategyquant.plugin.DataManager.impl.Data.csvexport.format;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.plugin.DataManager.impl.Data.csvexport.items.AbstractItem;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Formats {
   public static final Logger Log = LoggerFactory.getLogger(Formats.class);
   private static final String FilePath = SQPaths.settingsDirPath + "/ExportCsvFileFormats.xml";
   private static Formats instance;
   public List<Format> available = new ArrayList<>();

   public static Formats get() {
      if (instance == null) {
         instance = new Formats();
      }

      return instance;
   }

   private Formats() {
      this.loadPredefinedFormats();
      this.loadCustomFormats();
   }

   private void loadPredefinedFormats() {
      this.addFormat(
         "Generic tick format (comma delimited)",
         ",",
         new String[]{"DateTime", "Bid", "Ask", "Volume"},
         new String[]{AbstractItem.format("DateTime", "yyyyMMdd HH:mm:ss.SSS"), "Bid", "Ask", "Volume"},
         true,
         true
      );
      this.addFormat(
         "Generic bar format (comma delimited)",
         ",",
         new String[]{"Date", "Time", "Open", "High", "Low", "Close", "Volume"},
         (String[])new String[]{AbstractItem.format("Date", "yyyyMMdd"), AbstractItem.format("Time", "HH:mm:ss"), "Open", "High", "Low", "Close", "Volume"}
            .clone(),
         true,
         true
      );
      this.addFormat(
         "Generic tick format (tab delimited)",
         "[Tab]",
         new String[]{"DateTime", "Bid", "Ask", "Volume"},
         new String[]{AbstractItem.format("DateTime", "yyyyMMdd HH:mm:ss.SSS"), "Bid", "Ask", "Volume"},
         true,
         true
      );
      this.addFormat(
         "Generic bar format (tab delimited)",
         "[Tab]",
         new String[]{"Date", "Time", "Open", "High", "Low", "Close", "Volume"},
         (String[])new String[]{AbstractItem.format("Date", "yyyyMMdd"), AbstractItem.format("Time", "HH:mm:ss"), "Open", "High", "Low", "Close", "Volume"}
            .clone(),
         true,
         true
      );
      this.addFormat(
         "MetaTrader4 tick format",
         ",",
         new String[]{"DateTime", "Bid", "Ask", "Volume", "Volume"},
         new String[]{AbstractItem.format("DateTime", "yyyy.MM.dd HH:mm:ss.SSS"), "Bid", "Ask", "Volume", "0"},
         false,
         true
      );
      this.addFormat(
         "MetaTrader4 bar format",
         ",",
         new String[]{"Date", "Time", "Open", "High", "Low", "Close", "Volume"},
         new String[]{AbstractItem.format("Date", "yyyy.MM.dd"), AbstractItem.format("Time", "HH:mm"), "Open", "High", "Low", "Close", "Volume"},
         false,
         true
      );
      this.addFormat(
         "Amibroker bar (aqi) format",
         ",",
         new String[]{"Date", "Time", "Open", "High", "Low", "Close", "Volume"},
         new String[]{AbstractItem.format("Date", "yyyyMMdd"), AbstractItem.format("Time", "HHmmss"), "Open", "High", "Low", "Close", "Volume"},
         false,
         true
      );
      this.addFormat(
         "Amibroker tick (aqi) format",
         ",",
         new String[]{"Date", "Time", "Open", "High", "Low", "Close", "Volume"},
         new String[]{AbstractItem.format("Date", "yyyyMMdd"), AbstractItem.format("Time", "HHmmss"), "Bid", "Bid", "Bid", "Bid", "Volume"},
         false,
         true
      );
      this.addFormat(
         "Birt's CSV2FXT format",
         ",",
         new String[]{"DateTime", "Bid", "Ask", "Volume", "Volume"},
         new String[]{AbstractItem.format("DateTime", "yyyy.MM.dd HH:mm:ss.SSS"), "Bid", "Ask", "Volume", "Volume"},
         true,
         true
      );
      this.addFormat(
         "Forex Tester bar format",
         ",",
         new String[]{"<TICKER>", "<DTYYYYMMDD>", "<TIME>", "<OPEN>", "<HIGH>", "<LOW>", "<CLOSE>", "<VOL>"},
         new String[]{"Symbol", AbstractItem.format("Date", "yyyyMMdd"), AbstractItem.format("Time", "HHmmss"), "Open", "High", "Low", "Close", "Volume"},
         true,
         true
      );
      this.addFormat(
         "Forex SB bar format",
         ",",
         new String[]{"Date", "Time", "Open", "High", "Low", "Close", "Volume"},
         new String[]{AbstractItem.format("Date", "yyyy-MM-dd"), AbstractItem.format("Time", "HH:mm"), "Open", "High", "Low", "Close", "Volume"},
         false,
         true
      );
      this.addFormat(
         "Ninja Trader tick format",
         ";",
         new String[]{"DateTime", "Bid", "Volume"},
         new String[]{AbstractItem.format("DateTime", "yyyyMMdd HHmmss SSS0000"), "Bid", "Volume"},
         false,
         true
      );
      this.addFormat(
         "Ninja Trader bar format",
         ";",
         new String[]{"DateTime", "Open", "High", "Low", "Close", "Volume"},
         new String[]{AbstractItem.format("DateTime", "yyyyMMdd HHmmss"), "Open", "High", "Low", "Close", "Volume"},
         false,
         true
      );
      this.addFormat(
         "Neuroshell Trader format",
         ",",
         new String[]{"Date", "Time", "Open", "High", "Low", "Close"},
         new String[]{AbstractItem.format("Date", "yyyyMMdd"), AbstractItem.format("Time", "HHmmss"), "Open", "High", "Low", "Close"},
         true,
         true
      );
      this.addFormat(
         "Tradestation bar format",
         ",",
         new String[]{"Date", "Time", "Open", "High", "Low", "Close", "Volume", "Volume"},
         new String[]{AbstractItem.format("Date", "MM/dd/yyyy"), AbstractItem.format("Time", "HH:mm"), "Open", "High", "Low", "Close", "Volume", "Volume"},
         true,
         true
      );
   }

   public JSONArray toJson() {
      JSONArray var1 = new JSONArray();

      for (Format var3 : this.available) {
         var1.put(var3.toJson());
      }

      return var1;
   }

   private void addFormat(String var1, String var2, String[] var3, String[] var4, boolean var5, boolean var6) {
      try {
         this.available.add(new Format(var1, var2, var3, var4, var5, var6));
      } catch (Exception var8) {
         Log.error(String.format("Cannot add format with name '%s'. Exc.", var1), var8);
      }
   }

   public Format findByName(String var1) {
      for (int var2 = 0; var2 < this.available.size(); var2++) {
         if (var1.equals(this.available.get(var2).name)) {
            return this.available.get(var2);
         }
      }

      return null;
   }

   public static Format parseCustomFormat(String var0, String var1, String var2, boolean var3) throws Exception {
      String var4 = ",";
      int var5 = split(var2, ",").length;
      if (var5 < split(var2, ";").length) {
         var4 = ";";
         var5 = split(var2, ";").length;
      }

      if (var5 < split(var2, "[Tab]").length) {
         var4 = "[Tab]";
         var5 = split(var2, "[Tab]").length;
      }

      return new Format(var0, var4, split(var1, var4), split(var2, var4), var3, false);
   }

   private static String[] split(String var0, String var1) {
      return var0.split(Pattern.quote(var1));
   }

   public void updateFormat(String var1, String var2, String var3, boolean var4) throws Exception {
      Format var5 = get().findByName(var1);
      if (var5 == null) {
         throw new Exception(L.t("Format with name '%s' doesn't exist.", new Object[]{var1}));
      }

      Format var6 = parseCustomFormat(var1, var2, var3, var4);
      this.available.remove(var5);
      this.available.add(var6);
      this.saveCustomFormats();
   }

   public void addFormat(String var1, String var2, String var3, boolean var4) throws Exception {
      Format var5 = parseCustomFormat(var1, var2, var3, var4);
      this.available.add(var5);
      this.saveCustomFormats();
   }

   private void saveCustomFormats() {
      Element var1 = new Element("CustomFormats");

      for (Format var3 : this.available) {
         if (!var3.predefined) {
            Element var4 = new Element("CustomFormat");
            var4.addContent(new Element("Name").setText(var3.name));
            var4.addContent(new Element("Separator").setText(var3.separator));
            var4.addContent(new Element("IncludeHeader").setText(var3.includeHeader + ""));
            var4.addContent(new Element("Header").setText(var3.header));
            var4.addContent(new Element("Items").setText(var3.items));
            var1.addContent(var4);
         }
      }

      try {
         File var6 = new File(FilePath);
         if (!var6.exists()) {
            var6.getParentFile().mkdirs();
         }

         BufferedWriter var7 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(var6), "UTF8"));
         XMLOutputter var8 = new XMLOutputter(org.jdom2.output.Format.getPrettyFormat());
         var8.output(var1, var7);
         var7.close();
      } catch (Exception var5) {
         Log.error("Cannot save custom formats.", var5);
      }
   }

   private void loadCustomFormats() {
      try {
         File var1 = new File(FilePath);
         if (!var1.exists()) {
            return;
         }

         Element var2 = XMLUtil.fileToXmlElement(var1);
         if (var2 != null) {
            for (Element var4 : var2.getChildren("CustomFormat")) {
               try {
                  String var5 = var4.getChildText("Name");
                  String var6 = var4.getChildText("Header");
                  String var7 = var4.getChildText("Items");
                  boolean var8 = Boolean.valueOf(var4.getChildText("CustomFormat"));
                  Format var9 = parseCustomFormat(var5, var6, var7, var8);
                  this.available.add(var9);
               } catch (Exception var10) {
                  Log.error("Cannot load custom format.", var10);
               }
            }
         }
      } catch (Exception var11) {
         Log.error("Cannot load custom formats", var11);
      }
   }

   public void deleteFormat(String var1) throws Exception {
      Format var2 = get().findByName(var1);
      if (var2 == null) {
         throw new Exception(L.t("Format with name '%s' doesn't exist.", new Object[]{var1}));
      }

      if (var2.predefined) {
         throw new Exception(L.t("Format with name '%s' is predefined.", new Object[]{var1}));
      }

      this.available.remove(var2);
      this.saveCustomFormats();
   }
}
