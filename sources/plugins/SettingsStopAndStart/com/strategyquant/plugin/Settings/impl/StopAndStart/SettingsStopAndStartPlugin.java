package com.strategyquant.plugin.Settings.impl.StopAndStart;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.project.ProjectConditions;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.util.ArrayList;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "StopAndStart settings plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SettingsStopAndStartPlugin implements ISettingTabPlugin, IServletPlugin {
   public static final Logger Log = LoggerFactory.getLogger(SettingsStopAndStartPlugin.class);
   private ServletContextHandler dataContext;

   public Handler getHandler() {
      return null;
   }

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 70;
   }

   public void initPlugin() throws Exception {
   }

   private void fixSettings(Element var1) {
      Element var2 = XMLUtil.tryAddElement(var1, this.getSettingName());
      XMLUtil.tryAddElement(var2, "Conditions");
      Element var3 = XMLUtil.tryAddElement(var2, "StartProject");
      if (var3.getAttributeValue("use") == null) {
         var3.setAttribute("use", "false");
      }
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      this.fixSettings(var3);
      Element var5 = var3.getChild(this.getSettingName());
      Element var6 = var5.getChild("Conditions");

      try {
         ArrayList var7 = ProjectConditions.loadFromConfig(var6);
         var4.addParam("ProjectConditions", var7);
      } catch (Exception var11) {
         var4.addError(this.getName(), null, var11.getMessage());
      }

      Element var12 = var5.getChild("StartProject");

      try {
         if (XMLUtil.elementIs(var12, "use")) {
            var4.addParam("ProjectToStart", var12.getText());
         }
      } catch (Exception var10) {
         var4.addError(this.getName(), null, var10.getMessage());
      }

      try {
         var4.addParam("SkipToFirstProject", XMLUtil.getBooleanAttr(var12, "skipToFirstProject", false));
      } catch (Exception var9) {
         var4.addError(this.getName(), null, var9.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
      Element var3 = XMLUtil.tryAddElement(var1, this.getSettingName());
      IFitnessFunction var4 = ProjectConfigHelper.getFitnessFunction(var3);
      Element var5 = XMLUtil.getChildElem(var3, "MaxStrategies");
      String var6 = var5 != null ? "" + Integer.parseInt(var5.getText()) : "N/A";
      var2.put(new JSONObject().put("key", L.t("Maximum top strategies to store", new Object[0])).put("value", var6));
      var2.put(new JSONObject().put("key", L.t("Fitness computation", new Object[0])).put("value", var4.getFitnessName()));
   }

   public String getSettingName() {
      return "StopAndStart";
   }

   public String getName() {
      return L.tsq("Stop & Start");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
