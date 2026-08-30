package com.strategyquant.plugin.Servlet.impl.RenameTool;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RenameToolSettings {
   private static final Logger Log = LoggerFactory.getLogger(RenameToolSettings.class);
   private static final String configFilePath = MainApp.getDataPath() + "user/settings/RenameToolConfig.xml";
   public static LinkedHashMap<String, String> SN_Markets = new LinkedHashMap<>();
   public static LinkedHashMap<String, String> SN_TimeFrames = new LinkedHashMap<>();
   public static LinkedHashMap<String, String> SN_OrderTypes = new LinkedHashMap<>();
   public static LinkedHashMap<String, String> SN_Indicators = new LinkedHashMap<>();
   public static LinkedHashMap<String, String> MN_Markets = new LinkedHashMap<>();
   public static LinkedHashMap<String, String> MN_TimeFrames = new LinkedHashMap<>();

   public static void load() {
      File var0 = new File(configFilePath);
      if (!var0.exists()) {
         Log.error("Rename tool config file not found");
      } else {
         try {
            Element var1 = XMLUtil.fileToXmlElement(var0);
            Element var2 = XMLUtil.getChildElem(var1, "StrategyName");
            loadValues(var2, "Markets", SN_Markets);
            loadValues(var2, "TimeFrames", SN_TimeFrames);
            loadValues(var2, "OrderTypes", SN_OrderTypes);
            loadValues(var2, "Indicators", SN_Indicators);
            Element var3 = XMLUtil.getChildElem(var1, "MagicNumber");
            loadValues(var3, "Markets", MN_Markets);
            loadValues(var3, "TimeFrames", MN_TimeFrames);
         } catch (Exception var4) {
            Log.error("Loading config failed", var4);
         }
      }
   }

   private static void loadValues(Element var0, String var1, HashMap<String, String> var2) throws Exception {
      Element var3 = XMLUtil.getChildElem(var0, var1);
      List var4 = var3.getChildren("Assign");

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);
         String var7 = var6.getAttributeValue("value");
         String var8 = var6.getTextTrim();
         if (var7 != null && var8 != null) {
            var2.put(var7, var8);
         }
      }
   }
}
