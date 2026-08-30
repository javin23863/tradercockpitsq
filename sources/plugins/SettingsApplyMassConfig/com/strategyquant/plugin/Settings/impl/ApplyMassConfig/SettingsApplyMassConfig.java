package com.strategyquant.plugin.Settings.impl.ApplyMassConfig;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.applyMassConfig.ApplyMassConfig;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "Settings ApplyMassConfig plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SettingsApplyMassConfig implements ISettingTabPlugin {
   public static final Logger Log = LoggerFactory.getLogger(SettingsApplyMassConfig.class);

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
      } catch (Exception var9) {
         var4.addError(this.getSettingName(), null, var9.getMessage());
         return;
      }

      try {
         SQProject var6 = ProjectEngine.get(var1);
         ApplyMassConfig var7 = new ApplyMassConfig();
         var7.init(var6, var5);
         var4.addParam("ApplyMassConfig", var7);
      } catch (Exception var8) {
         Log.error(var1 + " - Exception while reading 'Applying mass config' settings", var8);
         var4.addError(this.getSettingName(), null, L.t("Cannot load 'Applying mass config' settings. ", new Object[0]) + var8.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getSettingName() {
      return "ApplyMassConfig";
   }

   public String getName() {
      return L.tsq("Apply mass config");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
