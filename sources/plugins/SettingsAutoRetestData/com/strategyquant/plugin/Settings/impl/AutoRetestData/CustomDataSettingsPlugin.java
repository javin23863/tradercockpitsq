package com.strategyquant.plugin.Settings.impl.AutoRetestData;

import com.strategyquant.lib.L;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.backtest.AutoRetestSettings;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "AutoRetest Data settings plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class CustomDataSettingsPlugin implements ISettingTabPlugin, IServletPlugin {
   public static final Logger Log = LoggerFactory.getLogger(CustomDataSettingsPlugin.class);

   public Handler getHandler() {
      return null;
   }

   public String getProduct() {
      return "SQUANTAlgoWizardAlgoWizardStandaloneBACKTESTNODE";
   }

   public int getPreferredPosition() {
      return 10;
   }

   public void initPlugin() throws Exception {
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      try {
         AutoRetestSettings var5 = new AutoRetestSettings();
         var5.init(var3);
         var4.addParam("AutoRetestDataSettings", var5);
      } catch (Exception var6) {
         Log.error("Exception while reading auto retest data", var6);
         var4.addError(this.getSettingName(), null, "Cannot initialize data settings - " + var6.getMessage());
      }
   }

   public String getSettingName() {
      return "AutoRetestData";
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getName() {
      return L.tsq("Custom data");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
