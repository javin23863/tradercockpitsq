package com.strategyquant.plugin.Settings.impl.DeleteFile;

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
@Name(name = "Settings Delete file plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SettingsDeleteFile implements ISettingTabPlugin {
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
      } catch (Exception var7) {
         var4.addError(this.getSettingName(), null, var7.getMessage());
         return;
      }

      try {
         String var6 = XMLUtil.getNodeValue(var5, "File");
         if (var6 == null || var6.trim().isEmpty()) {
            throw new Exception(L.t("File not set.", new Object[0]));
         }

         var4.addParam("File", var6);
      } catch (Exception var8) {
         var4.addError(this.getSettingName(), null, L.t("Cannot load Delete file settings. ", new Object[0]) + var8.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getSettingName() {
      return "DeleteFile";
   }

   public String getName() {
      return L.tsq("Delete file");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
