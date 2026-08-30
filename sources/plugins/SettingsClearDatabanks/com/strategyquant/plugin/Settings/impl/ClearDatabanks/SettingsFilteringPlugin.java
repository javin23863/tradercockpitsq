package com.strategyquant.plugin.Settings.impl.ClearDatabanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
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

@Author(name = "Tomas Brynda")
@Name(name = "Settings Clear databanks plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SettingsFilteringPlugin implements ISettingTabPlugin {
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
      } catch (Exception var11) {
         var4.addError(this.getSettingName(), null, var11.getMessage());
         return;
      }

      try {
         ArrayList var6 = new ArrayList();
         List var7 = var5.getChildren("Databank");

         for (int var8 = 0; var8 < var7.size(); var8++) {
            Element var9 = (Element)var7.get(var8);
            String var10 = var9.getAttributeValue("name");
            if (var10 != null) {
               var6.add(var10);
            }
         }

         var4.addParam("DatabanksToClear", var6);
      } catch (Exception var12) {
         var4.addError(this.getSettingName(), null, L.t("Cannot load Clear databanks settings. ", new Object[0]) + var12.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getSettingName() {
      return "ClearDatabanks";
   }

   public String getName() {
      return L.tsq("Clear databanks");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
