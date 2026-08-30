package com.strategyquant.plugin.Settings.impl.LogDatabankStats;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.util.ArrayList;
import java.util.List;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "Log databank stats setting plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SettingsLogDatabankStats implements ISettingTabPlugin {
   public static final Logger Log = LoggerFactory.getLogger(SettingsLogDatabankStats.class);

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 500;
   }

   public void initPlugin() throws Exception {
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      Element var5 = XMLUtil.tryAddElement(var3, this.getSettingName());

      try {
         String var6 = "Results";
         if (var5.getChild("Databank") != null) {
            var6 = XMLUtil.getNodeValue(var5, "Databank", "Results");
         }

         Databank var7 = (Databank)ProjectEngine.get(var1).getDatabanks().get(var6);
         var4.addParam("DatabankSource", var7);
         ArrayList var8 = new ArrayList();
         List var9 = var5.getChildren("LogStats");

         for (int var10 = 0; var10 < var9.size(); var10++) {
            Element var11 = (Element)var9.get(var10);
            String var12 = XMLUtil.getAttr(var11, "type", "sum");
            Object[] var13 = new Object[]{
               var12,
               ProjectConfigHelper.getConditions(var11.getChild("Conditions")),
               var11.getChild("Column") != null ? ProjectConfigHelper.getConditionValue(var11.getChild("Column")) : null
            };
            if (!var12.equals("sum") && !var12.equals("avg") && !var12.equals("count")) {
               throw new Exception(L.t("Invalid stats type. Must be one of (sum, avg, count).", new Object[0]));
            }

            if ((var12.equals("sum") || var12.equals("avg")) && var13[2] == null) {
               throw new Exception(L.t("Missing column (stats name) definition.", new Object[0]));
            }

            var8.add(var13);
         }

         var4.addParam("LogStats", var8);
      } catch (Exception var14) {
         var4.addError(this.getName(), null, "Cannot load settings. " + var14.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getSettingName() {
      return "LogDatabankStats";
   }

   public String getName() {
      return L.tsq("Log databank stats");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
