package com.strategyquant.datalib.customData;

import com.strategyquant.datalib.data.imports.AvailableDataFormats;
import com.strategyquant.datalib.data.imports.CustomDataFormat;
import com.strategyquant.datalib.data.imports.DataColumns;
import com.strategyquant.datalib.data.io.columns.DefaultCol;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AvailableCustomDataFormats {
   public static final Logger Log = LoggerFactory.getLogger(AvailableDataFormats.class);
   private static final String filePath = SQPaths.settingsDirPath + "/customDataFormats.xml";
   private static AvailableCustomDataFormats instance;
   protected ArrayList<CustomDataFormat> availableFileFormats = new ArrayList<>();

   private AvailableCustomDataFormats() {
      this.loadPredefinedFileFormats();
      this.loadCustomFileFormats();
   }

   public ArrayList<CustomDataFormat> getAvailableFileFormats() {
      return this.availableFileFormats;
   }

   public static AvailableCustomDataFormats getInstance() {
      if (instance == null) {
         instance = new AvailableCustomDataFormats();
      }

      return instance;
   }

   public void registerFileFormats() {
      this.availableFileFormats.clear();
      this.loadPredefinedFileFormats();
   }

   public boolean formatExists(String var1) {
      for (CustomDataFormat var3 : this.availableFileFormats) {
         if (var3.getName().equals(var1)) {
            return true;
         }
      }

      return false;
   }

   private void loadPredefinedFileFormats() {
   }

   private void loadCustomFileFormats() {
      try {
         File var1 = new File(filePath);
         if (!var1.exists()) {
            return;
         }

         Element var2 = XMLUtil.fileToXmlElement(var1);
         if (var2 != null) {
            for (Element var4 : var2.getChildren("CustomFormat")) {
               try {
                  CustomDataFormat var5 = new CustomDataFormat();
                  var5.setName(var4.getAttributeValue("name"));
                  var5.setSeparator(var4.getAttributeValue("separator"));
                  var5.setSkipColumns(Integer.parseInt(var4.getAttributeValue("skipColumns")));
                  var5.setSkipRows(Integer.parseInt(var4.getAttributeValue("skipRows")));
                  var5.setDateFormat(var4.getAttributeValue("dateFormat"));
                  var5.setPredefined(false);
                  Element var6 = var4.getChild("Columns");
                  if (var6 != null) {
                     HashMap var7 = new HashMap();
                     List var8 = var6.getChildren("Column");

                     for (int var9 = 0; var9 < var8.size(); var9++) {
                        String var10 = ((Element)var8.get(var9)).getText();
                        DefaultCol var11 = DataColumns.getInstance().findColTypeByName(var10);
                        if (var11 != null) {
                           var7.put(var9, var11);
                        }
                     }

                     var5.setColumns(var7);
                  }

                  this.availableFileFormats.add(var5);
               } catch (Exception var12) {
                  Log.error("Cannot load dataformat.", var12);
               }
            }
         }
      } catch (Exception var13) {
         Log.error("Cannot load custom data formats", var13);
      }
   }

   public void addDataFormat(CustomDataFormat var1) {
      this.availableFileFormats.add(var1);
      this.saveFormats();
   }

   public void deleteDataFormat(String var1) {
      this.remove(var1, true);
   }

   private void remove(String var1, boolean var2) {
      boolean var3 = false;

      for (CustomDataFormat var5 : this.availableFileFormats) {
         if (var5.getName().equals(var1)) {
            this.availableFileFormats.remove(var5);
            var3 = true;
            break;
         }
      }

      if (var3 && var2) {
         this.saveFormats();
      }
   }

   public void updateDataFormat(CustomDataFormat var1) {
      this.remove(var1.getName(), false);
      this.availableFileFormats.add(var1);
      this.saveFormats();
   }

   private void saveFormats() {
      Element var1 = new Element("CustomDataFormats");

      for (CustomDataFormat var3 : this.availableFileFormats) {
         if (!var3.isPredefined()) {
            Element var4 = new Element("CustomFormat");
            var4.setAttribute("name", var3.getName());
            var4.setAttribute("separator", var3.getSeparator());
            var4.setAttribute("skipColumns", String.valueOf(var3.getSkipColumns()));
            var4.setAttribute("skipRows", String.valueOf(var3.getSkipRows()));
            var4.setAttribute("dateFormat", var3.getDateFormat());
            Element var5 = new Element("Columns");

            for (int var6 = 0; var6 < var3.getColumns().size(); var6++) {
               String var7 = var3.getColumns().get(var6).getName();
               Element var8 = new Element("Column");
               var8.setText(var7);
               var5.addContent(var8);
            }

            var4.addContent(var5);
            var1.addContent(var4);
         }
      }

      try {
         File var10 = new File(filePath);
         if (!var10.exists()) {
            var10.getParentFile().mkdirs();
         }

         BufferedWriter var11 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(var10), StandardCharsets.UTF_8));
         XMLOutputter var12 = new XMLOutputter(Format.getPrettyFormat());
         var12.output(var1, var11);
         var11.close();
      } catch (Exception var9) {
         Log.error("Saving settings failed.", var9);
      }
   }

   public CustomDataFormat findFileFormatByName(String var1) {
      for (CustomDataFormat var3 : this.availableFileFormats) {
         if (var3.getName().equalsIgnoreCase(var1)) {
            return var3;
         }
      }

      return null;
   }
}
