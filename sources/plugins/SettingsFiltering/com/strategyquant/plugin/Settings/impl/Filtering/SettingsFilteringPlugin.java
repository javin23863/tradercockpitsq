package com.strategyquant.plugin.Settings.impl.Filtering;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;

@Author(name = "Tomas Brynda")
@Name(name = "Settings Filtering plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SettingsFilteringPlugin implements ISettingTabPlugin {
   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 50;
   }

   public void initPlugin() throws Exception {
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      Element var5 = null;

      try {
         var5 = XMLUtil.getChildElem(var3, this.getSettingName());
      } catch (Exception var10) {
         var4.addError(this.getSettingName(), null, var10.getMessage());
         return;
      }

      try {
         int var6 = Integer.parseInt(XMLUtil.getChildElem(var5, "ActionType").getText());
         Databank var7 = ProjectConfigHelper.getDatabankByType(var1, "Source", var3);
         Databank var8 = ProjectConfigHelper.getDatabankByType(var1, "Target", var3);
         if (var7 == null) {
            throw new Exception(L.t("Source databank not found", new Object[0]));
         }

         if (var6 != 0 && var8 == null) {
            throw new Exception(L.t("Target databank not found", new Object[0]));
         }

         var4.addParam("DatabankSource", var7);
         var4.addParam("DatabankTarget", var8);
         var4.addParam("ActionType", var6);
         var4.addParam("ConditionsType", Integer.parseInt(XMLUtil.getChildElem(var5, "ConditionsType").getText()));
         var4.addParam("Conditions", ProjectConfigHelper.getConditions(var5.getChild("Conditions")));
         var4.addParam("MaxStrategiesFilter", XMLUtil.getInt(var5, "MaxStrategies", 0));
      } catch (Exception var9) {
         var4.addError(this.getSettingName(), null, L.t("Cannot load Filtering settings. ", new Object[0]) + var9.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getSettingName() {
      return "Filtering";
   }

   public String getName() {
      return L.tsq("Filtering");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
