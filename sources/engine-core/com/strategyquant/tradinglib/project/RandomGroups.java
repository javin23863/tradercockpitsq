package com.strategyquant.tradinglib.project;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import java.io.File;
import java.util.List;
import org.jdom2.Element;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RandomGroups {
   private static final Logger Log = LoggerFactory.getLogger(RandomGroups.class);

   public static Element getXml() {
      Element var0 = new Element("Groups");

      try {
         File var1 = new File(SQPaths.algoWizardBlockGroupsPath);
         if (var1.exists()) {
            Element var2 = XMLUtil.stringToElement(SQUtils.fileToString(var1));
            if ("RandomGroups".equals(var2.getName())) {
               var2.setName("Groups");
            }

            return var2;
         }
      } catch (Exception var3) {
         Log.error("Error while loading Block groups.", var3);
      }

      return var0;
   }

   public static List<Element> listGroups() {
      Element var0 = getXml();
      return var0.getChildren("Group");
   }

   public static Element findByID(String var0, List<Element> var1) {
      if (var1 == null) {
         return null;
      }

      for (int var2 = 0; var2 < var1.size(); var2++) {
         Element var3 = (Element)var1.get(var2);
         if (var0.equals(var3.getAttributeValue("id"))) {
            return var3;
         }
      }

      return null;
   }

   public static Element findByName(String var0, Element var1) {
      if (var1 == null) {
         return null;
      }

      for (Element var4 : var1.getChildren("Group")) {
         if (var0.equals(var4.getAttributeValue("name"))) {
            return var4;
         }
      }

      return null;
   }

   public static void save(String var0) throws Exception {
      if (var0 == null) {
         throw new Exception("Groups XML cannot be null.");
      }

      Element var1;
      try {
         var1 = XMLUtil.stringToElement(var0);
      } catch (Exception var3) {
         throw new Exception("Invalid XML. " + var3.getMessage(), var3);
      }

      save(var1);
   }

   public static void save(Element var0) throws Exception {
      if (var0 == null) {
         throw new Exception("Groups XML cannot be null.");
      }

      String var1 = XMLUtil.xmlToString(var0);
      SQUtils.createAndHandleBackupFiles(SQPaths.algoWizardBlockGroupsPath, var1, 50);
      SQUtils.stringToFile(SQPaths.algoWizardBlockGroupsPath, var1);
   }

   public static JSONArray listBackupFiles() {
      return SQUtils.listBackupFiles(SQPaths.algoWizardBlockGroupsPath);
   }
}
