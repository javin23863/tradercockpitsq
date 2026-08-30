package com.strategyquant.plugin.Settings.impl.WaitFor;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;

@Author(name = "Tomas Brynda")
@Name(name = "Settings Wait for user/file plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SettingsWaitFor implements ISettingTabPlugin {
   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 10;
   }

   public void initPlugin() throws Exception {
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      Element var5 = null;

      try {
         var5 = XMLUtil.getChildElem(var3, this.getSettingName());
      } catch (Exception var8) {
         var4.addError(this.getSettingName(), null, var8.getMessage());
         return;
      }

      try {
         String var6 = XMLUtil.getNodeValue(var5, "Action", "user");
         String var7 = null;
         if (var6.equals("file")) {
            var7 = XMLUtil.getNodeValue(var5, "File");
            if (var7 == null || var7.trim().isEmpty()) {
               throw new Exception(L.t("File not set.", new Object[0]));
            }
         }

         var4.addParam("File", var7);
         var4.addParam("ActionType", var6);
      } catch (Exception var9) {
         var4.addError(this.getSettingName(), null, L.t("Cannot load Wait for user/file settings. ", new Object[0]) + var9.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getSettingName() {
      return "WaitFor";
   }

   public String getName() {
      return L.tsq("Wait for user/file");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
