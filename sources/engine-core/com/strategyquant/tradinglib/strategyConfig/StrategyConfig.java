package com.strategyquant.tradinglib.strategyConfig;

import com.strategyquant.lib.L;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.simulator.Engines;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import java.util.List;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyConfig {
   private static final Logger Log = LoggerFactory.getLogger(StrategyConfig.class);

   public JSONArray print(List<String> var1, Element var2, Element var3) {
      JSONArray var4 = new JSONArray();
      JSONArray var5 = null;
      JSONArray var6 = null;
      int var7 = this.getEngine(var2);
      int var8 = this.getEngine(var3);

      for (ISettingTabPlugin var11 : SQPluginManager.getPlugins(ISettingTabPlugin.class)) {
         for (String var13 : var1) {
            String var14 = var11.getSettingName();
            if (var14.equals(var13)) {
               String var15 = var11.getName();

               try {
                  JSONObject var16 = new JSONObject();
                  var16.put("name", var15);
                  var16.put("key", var14);
                  JSONArray var17 = new JSONArray();
                  JSONArray var18 = new JSONArray();
                  if (var2 != null) {
                     var11.getStrategyConfigSettings(var2, var17);
                  }

                  if (var3 != null) {
                     var11.getStrategyConfigSettings(var3, var18);
                  }

                  if (var14.equals("Data")) {
                     try {
                        for (int var19 = 0; var19 < var17.length(); var19++) {
                           JSONObject var20 = var17.getJSONObject(var19);
                           String var21 = var20.getString("key");
                           if (var21 != null && var21.equals("Instruments")) {
                              var5 = ((JSONObject)var17.remove(var19)).getJSONArray("value");
                              break;
                           }
                        }

                        for (int var25 = 0; var25 < var18.length(); var25++) {
                           JSONObject var30 = var18.getJSONObject(var25);
                           String var33 = var30.getString("key");
                           if (var33 != null && var33.equals("Instruments")) {
                              var6 = ((JSONObject)var18.remove(var25)).getJSONArray("value");
                              break;
                           }
                        }
                     } catch (Exception var22) {
                        Log.error("Cannot load instrument settings", var22);
                     }
                  }

                  if (!var14.equals("Options")) {
                     if (!var14.equals("CrossChecks") && !var14.equals("ATMs")) {
                        JSONArray var29 = this.printSettings(var17, var18, var14);
                        if (var29.length() != 0) {
                           var16.put("settings", var29);
                           var4.put(var16);
                        }
                     } else {
                        JSONArray var28 = this.printSettings(
                           Engines.isAWCloudEngine(var7) ? new JSONArray() : var17, Engines.isAWCloudEngine(var8) ? new JSONArray() : var18, var14
                        );
                        if (var28.length() != 0) {
                           var16.put("settings", var28);
                           var4.put(var16);
                        }
                     }
                  } else {
                     if (Engines.isAWCloudEngine(var7) || Engines.isAWCloudEngine(var8)) {
                        JSONArray var26 = this.printSettings(
                           Engines.isAWCloudEngine(var7) ? (var17.length() > 0 ? var17.getJSONArray(0) : new JSONArray()) : new JSONArray(),
                           Engines.isAWCloudEngine(var8) ? (var18.length() > 0 ? var18.getJSONArray(0) : new JSONArray()) : new JSONArray(),
                           var14
                        );
                        if (var26.length() > 0) {
                           JSONObject var31 = new JSONObject();
                           var31.put("name", L.t("Trading options (AWCloud engines)", new Object[0]));
                           var31.put("key", "Trading options (AWCloud engines)");
                           var31.put("settings", var26);
                           var4.put(var31);
                        }
                     }

                     if (Engines.isStandardEngine(var7) || Engines.isStandardEngine(var8)) {
                        JSONArray var27 = this.printSettings(
                           Engines.isStandardEngine(var7) ? (var17.length() > 1 ? var17.getJSONArray(1) : new JSONArray()) : new JSONArray(),
                           Engines.isStandardEngine(var8) ? (var18.length() > 1 ? var18.getJSONArray(1) : new JSONArray()) : new JSONArray(),
                           var14
                        );
                        if (var27.length() > 0) {
                           JSONObject var32 = new JSONObject();
                           var32.put("name", L.t("Trading options (standard engines)", new Object[0]));
                           var32.put("key", "Trading options (standard engines)");
                           var32.put("settings", var27);
                           var4.put(var32);
                        }
                     }
                  }
               } catch (Exception var23) {
                  Log.error("Error while loading '" + var15 + "' settings. Exc.", var23);
               }
            }
         }
      }

      if (var5 != null || var6 != null) {
         JSONObject var24 = new JSONObject();
         var24.put("name", "Instruments");
         var24.put("key", "Instruments");
         var24.put("settings", this.printSettings(var5, var6, "Instruments"));
         var4.put(var24);
      }

      return var4;
   }

   private int getEngine(Element var1) {
      if (var1 != null) {
         try {
            Element var2 = var1.getChild("Data");
            if (var2 != null) {
               Element var3 = var2.getChild("Setups").getChild("Setup");
               if (var3 != null) {
                  return Engines.getEngine(var3.getAttributeValue("engine"));
               }
            }
         } catch (Exception var5) {
            Log.error("Failed to parse engine.", var5);
         }
      }

      return -1;
   }

   private JSONArray printSettings(JSONArray var1, JSONArray var2, String var3) {
      JSONArray var4 = new JSONArray();
      if (var1 != null) {
         for (int var5 = 0; var5 < var1.length(); var5++) {
            JSONObject var6 = var1.getJSONObject(var5);
            String var7 = var6.getString("key");
            String var8 = var6.get("value") + "";
            String var9 = var2 != null ? this.getValueByKey(var2, var7) : null;
            if (var9 == null) {
               var9 = !var3.equals("Blocks") && !var3.equals("CrossChecks") ? L.t("N/A", new Object[0]) : L.t("no", new Object[0]);
            }

            JSONObject var10 = new JSONObject();
            var10.put("key", var7);
            var10.put("value_str", var8.replaceAll("\n", "<br>"));
            var10.put("value_task", var9.replaceAll("\n", "<br>"));
            var10.put("match", var8.equals(var9));
            var4.put(var10);
         }
      }

      if (var2 != null) {
         for (int var11 = 0; var11 < var2.length(); var11++) {
            JSONObject var12 = var2.getJSONObject(var11);
            String var13 = var12.getString("key");
            if (!this.containsKey(var4, var13)) {
               String var14 = var12.get("value") + "";
               String var15 = !var3.equals("Blocks") && !var3.equals("CrossChecks") ? L.t("N/A", new Object[0]) : L.t("no", new Object[0]);
               JSONObject var16 = new JSONObject();
               var16.put("key", var13);
               var16.put("value_str", var15.replaceAll("\n", "<br>"));
               var16.put("value_task", var14.replaceAll("\n", "<br>"));
               var16.put("match", var15.equals(var14));
               if (var4.length() > var11) {
                  this.addToPos(var11, var16, var4);
               } else {
                  var4.put(var16);
               }
            }
         }
      }

      return var4;
   }

   public void addToPos(int var1, JSONObject var2, JSONArray var3) {
      for (int var4 = var3.length(); var4 > var1; var4--) {
         var3.put(var4, var3.get(var4 - 1));
      }

      var3.put(var1, var2);
   }

   private boolean containsKey(JSONArray var1, String var2) {
      for (int var3 = 0; var3 < var1.length(); var3++) {
         JSONObject var4 = var1.getJSONObject(var3);
         if (var4.getString("key").equals(var2)) {
            return true;
         }
      }

      return false;
   }

   private String getValueByKey(JSONArray var1, String var2) {
      for (int var3 = 0; var3 < var1.length(); var3++) {
         JSONObject var4 = var1.getJSONObject(var3);
         if (var4 != null && var4.getString("key") != null && var4.getString("key").equals(var2)) {
            return var4.get("value") + "";
         }
      }

      return null;
   }
}
