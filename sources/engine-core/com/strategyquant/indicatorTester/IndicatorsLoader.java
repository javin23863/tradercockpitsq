package com.strategyquant.indicatorTester;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.lib.snippets.CustomClassesLoader;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndicatorsLoader {
   private static final Logger Log = LoggerFactory.getLogger(IndicatorsLoader.class);
   private static ArrayList<IndicatorInfo> indicators = new ArrayList<>();
   private static CustomClassesLoader loader;

   public static ArrayList<IndicatorInfo> getIndicators() {
      CalibrationConfig.load();
      loadAll();
      return indicators;
   }

   public static void loadAll() {
      indicators.clear();
      loader = new CustomClassesLoader("Blocks/Indicators");

      while (loader.hasNext()) {
         IndicatorInfo var0 = new IndicatorInfo();
         var0.className = loader.getNext();
         String[] var1 = var0.className.split("\\.");
         var0.simpleName = var1[var1.length - 1];

         try {
            Object var2 = loader.createInstance(var0.className);
            var0.returnType = 0;
            if (var2.getClass().isAnnotationPresent(BuildingBlock.class)) {
               BuildingBlock var3 = var2.getClass().getAnnotation(BuildingBlock.class);
               var0.returnType = var3.returnType();
               var0.title = var3.name();
            }

            if (var2.getClass().isAnnotationPresent(IgnoreInBuilder.class) || var0.className.endsWith("FixedPips")) {
               var0.ignored = true;
            }

            for (Field var6 : var2.getClass().getDeclaredFields()) {
               CalibrationData var7 = CalibrationConfig.getData(var0.simpleName, var6.getName());
               if (var6.isAnnotationPresent(Output.class)) {
                  if (var7 == null || !var7.disabled) {
                     String var8 = var6.getName();
                     var0.outputBuffers.add(var8);
                  }
               } else if (var6.isAnnotationPresent(Parameter.class)) {
                  IndyParameter var15 = new IndyParameter();
                  Parameter var9 = var6.getAnnotation(Parameter.class);
                  var15.name = var6.getName();
                  var15.className = var6.getType().getSimpleName();
                  var15.options = null;

                  try {
                     Editor var10 = var6.getAnnotation(Editor.class);
                     if (var10.type() == 40) {
                        var15.options = var10.values();
                     }
                  } catch (Exception var12) {
                  }

                  try {
                     var15.defaultValue = var9.defaultValue();
                     if (var15.defaultValue == null && var15.className.equals(DataSeries.class.getSimpleName())) {
                        var15.defaultValue = "Close";
                     }

                     if (var15.defaultValue == null) {
                        var15.defaultValue = String.valueOf(var6.get(var2));
                     }
                  } catch (Exception var11) {
                     var15.defaultValue = null;
                     Log.error("Error getting default parameter of indicator '" + var0.className + "'", var11);
                  }

                  if (var7 != null && var7.values != null && !var7.values.isEmpty()) {
                     var15.calibrationValues = var7.values;
                  }

                  var0.parameters.add(var15);
               }
            }

            indicators.add(var0);
         } catch (Exception var13) {
            Log.error("Cannot instantiate indicator '" + var0.simpleName + "'", var13);
         }
      }
   }

   public static JSONArray toJSONArray() {
      JSONArray var0 = new JSONArray();

      for (IndicatorInfo var2 : getIndicators()) {
         for (JSONObject var4 : var2.getJSONList()) {
            var0.put(var4);
         }
      }

      return var0;
   }

   public static String correctIndyName(String var0) {
      return var0.split("\\.")[0];
   }

   public static String getBufferName(String var0) {
      if (var0.contains(".")) {
         String[] var1 = var0.split("\\.");
         return var1[var1.length - 1];
      } else {
         return "Value";
      }
   }

   public static IndicatorInfo getIndyByName(String var0) {
      String var1 = correctIndyName(var0);

      for (IndicatorInfo var3 : getIndicators()) {
         if (var3.simpleName.equals(var1)) {
            return var3;
         }
      }

      return null;
   }
}
