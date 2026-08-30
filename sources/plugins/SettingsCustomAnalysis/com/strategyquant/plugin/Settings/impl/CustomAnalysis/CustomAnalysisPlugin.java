package com.strategyquant.plugin.Settings.impl.CustomAnalysis;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.CustomAnalysisMethod;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.customanalysis.CustomAnalysisInfo;
import com.strategyquant.tradinglib.customanalysis.CustomAnalysisMethodsList;
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
@Name(name = "Settings CustomAnalysis plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class CustomAnalysisPlugin implements ISettingTabPlugin {
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
         CustomAnalysisInfo var6 = new CustomAnalysisInfo();
         var6.loadTaskConfig(var5);
         var4.addParam("CustomAnalysis", var6);
         Databank var7 = ProjectConfigHelper.getInputDatabank(var1, var3);
         if (var7 == null) {
            throw new Exception(L.t("Source databank not found", new Object[0]));
         }

         var4.addParamUnchecked("DatabankSource", var7);
         Databank var8 = ProjectConfigHelper.getOutputDatabank(var1, var3);
         if (var8 == null) {
            throw new Exception(L.t("Target databank not found", new Object[0]));
         }

         var4.addParamUnchecked("DatabankTarget", var8);
      } catch (Exception var9) {
         var4.addError(this.getSettingName(), null, L.t("Cannot load 'Custom analysis' settings. ", new Object[0]) + var9.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getSettingName() {
      return "CustomAnalysis";
   }

   public String getName() {
      return L.tsq("Custom analysis");
   }

   public JSONObject getInitializationData() throws Exception {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();

      for (CustomAnalysisMethod var4 : CustomAnalysisMethodsList.get().getAvailableClasses()) {
         JSONObject var5 = new JSONObject();
         var5.put("name", var4.name);
         var5.put("value", var4.getClass().getSimpleName());
         var5.put("type", var4.getType());
         var2.put(var5);
      }

      var1.put("methods", var2);
      return var1;
   }
}
