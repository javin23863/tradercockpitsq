package com.strategyquant.plugin.Settings.impl.CallExternalScript;

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
@Name(name = "Settings Call external script")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SettingsCallExternalScript implements ISettingTabPlugin {
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
      } catch (Exception var14) {
         var4.addError(this.getSettingName(), null, var14.getMessage());
         return;
      }

      try {
         String var6 = XMLUtil.getNodeValue(var5, "File");
         if (var6 == null || var6.trim().isEmpty()) {
            throw new Exception(L.t("File not set.", new Object[0]));
         }

         var4.addParam("File", var6);
         String var7 = XMLUtil.getStringAttr(var5.getChild("File"), "params", null);
         var4.addParam("Params", var7);
         Element var8 = XMLUtil.getChildElem(var5, "WaitFor");
         String var9 = var8.getText();
         var4.addParam("Type", var9);
         if (var9.equals("time")) {
            int var10 = Integer.parseInt(var8.getAttributeValue("hours"));
            int var11 = Integer.parseInt(var8.getAttributeValue("minutes"));
            long var12 = var10 * 3600 * 1000 + var11 * 60 * 1000;
            var4.addParam("MaxBuildRunTime", var12);
         } else {
            var4.addParam("MaxBuildRunTime", 0L);
         }
      } catch (Exception var15) {
         var4.addError(this.getSettingName(), null, L.t("Cannot load Call external script settings. ", new Object[0]) + var15.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getSettingName() {
      return "CallExternalScript";
   }

   public String getName() {
      return L.tsq("Call external script");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
