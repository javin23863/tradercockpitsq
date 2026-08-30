package com.strategyquant.plugin.Settings.impl.CreatePortfolio;

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
@Name(name = "Settings CreatePortfolio plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SettingsCreatePortfolio implements ISettingTabPlugin {
   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 50;
   }

   public void initPlugin() throws Exception {
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      Element var5 = XMLUtil.tryAddElement(var3, this.getSettingName());

      try {
         var4.addParam("MaxStrategies", XMLUtil.getInt(var5, "MaxStrategies", 100));
         Databank var6 = ProjectConfigHelper.getInputDatabank(var1, var5.getParentElement());
         if (var6 == null) {
            throw new Exception(L.t("Source databank not found", new Object[0]));
         }

         var4.addParam("DatabankSource", var6);
         Databank var7 = ProjectConfigHelper.getOutputDatabank(var1, var5.getParentElement());
         if (var7 == null) {
            throw new Exception(L.t("Target databank not found", new Object[0]));
         }

         var4.addParam("DatabankTarget", var7);
      } catch (Exception var8) {
         var4.addError(this.getSettingName(), null, "Cannot load SaveToFiles settings. " + var8.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getSettingName() {
      return "CreatePortfolio";
   }

   public String getName() {
      return L.tsq("Save to files");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
