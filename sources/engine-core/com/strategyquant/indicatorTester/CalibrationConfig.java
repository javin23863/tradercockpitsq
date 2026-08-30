package com.strategyquant.indicatorTester;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.constants.SQPaths;
import java.util.ArrayList;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CalibrationConfig {
   private static final Logger Log = LoggerFactory.getLogger(CalibrationConfig.class);
   private static final String configFilePath = SQPaths.settingsDirPath + "/calibrationSettings.txt";
   private static HashMap<String, CalibrationData> map = new HashMap<>();
   public static boolean disableRounding = false;

   public static void load() {
      map.clear();
      disableRounding = false;
      ArrayList var0 = new ArrayList();

      try {
         SQUtils.fileToArrayList(var0, configFilePath, "\n");

         for (int var1 = 0; var1 < var0.size(); var1++) {
            String var2 = ((String)var0.get(var1)).trim();
            if (!var2.startsWith("#") && !var2.isEmpty() && var2.contains("=")) {
               try {
                  String[] var3 = var2.split("=");
                  if (var3.length != 2) {
                     throw new Exception(L.t("Invalid syntax", new Object[0]));
                  }

                  String var4 = var3[0];
                  String var5 = var3[1];
                  if (var4.equals("rounding")) {
                     disableRounding = var5.equals("disabled");
                  } else if (var5.contains(",")) {
                     map.put(var4, new CalibrationData(var5));
                  } else {
                     map.put(var4, new CalibrationData(var5.equals("disabled")));
                  }
               } catch (Exception var6) {
                  Log.error("Error while parsing line '" + var2 + "'", var6);
               }
            }
         }
      } catch (Exception var7) {
         Log.error("Error while loading calibration settings", var7);
      }
   }

   public static CalibrationData getData(String var0, String var1) {
      return map.get(var0 + "." + var1);
   }
}
