package com.strategyquant.tradinglib.project;

import com.strategyquant.pluginlib.SQPluginManager;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;

public class ProjectConditions {
   private static ArrayList<IProjectCondition> conditionPlugins;

   public static ArrayList<IProjectCondition> list() {
      if (conditionPlugins == null) {
         conditionPlugins = SQPluginManager.getPlugins(IProjectCondition.class);
      }

      return conditionPlugins;
   }

   public static JSONArray toJSON() {
      JSONArray var0 = new JSONArray();
      ArrayList var1 = list();

      for (int var2 = 0; var2 < var1.size(); var2++) {
         IProjectCondition var3 = (IProjectCondition)var1.get(var2);
         JSONObject var4 = new JSONObject();
         var4.put("type", var3.getName());
         var4.put("title", var3.getTitle());
         var4.put("descriptionFormat", var3.getDescriptionFormat());
         JSONArray var5 = new JSONArray();
         ProjectConditionField[] var6 = var3.getFields();

         for (int var7 = 0; var7 < var6.length; var7++) {
            ProjectConditionField var8 = var6[var7];
            JSONObject var9 = new JSONObject();
            var9.put("name", var8.getName());
            var9.put("type", var8.getType());
            var9.put("value", var8.getDefaultValue());
            var9.put("defaultValue", var8.getDefaultValue());
            var5.put(var9);
         }

         var4.put("fields", var5);
         var0.put(var4);
      }

      return var0;
   }

   public static ArrayList<IProjectCondition> loadFromConfig(Element var0) throws Exception {
      ArrayList var1 = list();
      ArrayList var2 = new ArrayList();
      List var3 = var0.getChildren("Condition");

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         String var6 = var5.getAttributeValue("type");
         if (var6 != null) {
            for (int var7 = 0; var7 < var1.size(); var7++) {
               IProjectCondition var8 = (IProjectCondition)var1.get(var7);
               if (var6.equals(var8.getName())) {
                  IProjectCondition var9 = var8.clone();
                  var9.readSettings(var5);
                  var2.add(var9);
               }
            }
         }
      }

      return var2;
   }
}
