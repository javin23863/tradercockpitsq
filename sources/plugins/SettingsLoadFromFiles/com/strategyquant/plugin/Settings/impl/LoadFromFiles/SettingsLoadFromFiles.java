package com.strategyquant.plugin.Settings.impl.LoadFromFiles;

import com.strategyquant.datalib.instrument.InstrumentManager;
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
import java.io.File;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;

@Author(name = "Tomas Brynda")
@Name(name = "Settings LoadFromFiles plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SettingsLoadFromFiles implements ISettingTabPlugin {
   private static final String tabTitle = "Load from files";

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
         String var6 = XMLUtil.getChildElem(var5, "SourceDirectory").getText();
         var4.addParam("SourceDirectory", var6);
         if (var6.isEmpty()) {
            throw new Exception(L.t("Source directory is not set.", new Object[0]));
         }

         if (!new File(var6).exists()) {
            throw new Exception(L.t("Source directory doesn't exist.", new Object[0]));
         }

         var4.addParam("IncludeSubdirectories", Boolean.parseBoolean(XMLUtil.getChildElem(var5, "IncludeSubdirectories").getText()));
         Databank var7 = ProjectConfigHelper.getInputDatabank(var1, var5.getParentElement());
         if (var7 == null) {
            throw new Exception(L.t("Target databank not found", new Object[0]));
         }

         var4.addParam("DatabankTarget", var7);
         int var8 = XMLUtil.getInt(var5, "UnrecognizedInstrumentAction", 0);
         var4.addParam("UnrecognizedInstrumentAction", var8);
         if (var8 == 1) {
            String var9 = XMLUtil.getChildElem(var5, "UnrecognizedInstrumentValue").getText();
            if (!InstrumentManager.checkInstrumentExists(var9)) {
               throw new Exception(L.t("Instrument doesn't exist.", new Object[0]));
            }

            var4.addParam("Instrument", var9);
         }
      } catch (Exception var10) {
         var4.addError(this.getSettingName(), null, "Cannot load LoadFromFiles settings. " + var10.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getSettingName() {
      return "LoadFromFiles";
   }

   public String getName() {
      return L.tsq("Load from files");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
