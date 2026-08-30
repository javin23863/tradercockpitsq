package com.strategyquant.plugin.Settings.impl.Options;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.options.TradingOptions;
import com.strategyquant.tradinglib.options.TradingOptionsList;
import com.strategyquant.tradinglib.options.parameters.ReservedBars;
import com.strategyquant.tradinglib.propertygrid.IPGParameter;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tamas Takacs")
@Name(name = "Options plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SettingsOptionsPlugin implements ISettingTabPlugin, IServletPlugin {
   public static final Logger Log = LoggerFactory.getLogger(SettingsOptionsPlugin.class);
   private ServletContextHandler dataContext;

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/options/");
         this.dataContext.addServlet(new ServletHolder(new SettingsOptionsServlet()), "/*");
      }

      return this.dataContext;
   }

   public String getProduct() {
      return "SQUANTAlgoWizardAlgoWizardStandaloneAlgoWizardCloudBACKTESTNODE";
   }

   public int getPreferredPosition() {
      return 50;
   }

   public void initPlugin() throws Exception {
   }

   private void fixSettings(Element var1) {
      Element var2 = XMLUtil.tryAddElement(var1, this.getSettingName());
      this.fixBuildTradingOptions(var2);
   }

   private void fixBuildTradingOptions(Element var1) {
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      this.fixSettings(var3);
      Element var5 = null;

      try {
         var5 = XMLUtil.getChildElem(var3, "Options");
      } catch (Exception var9) {
         var4.addError(this.getSettingName(), null, L.t("Options settings not found in XML config", new Object[0]));
         return;
      }

      try {
         TradingOptions var6 = TradingOptionsList.getInstance().parseOptionsFromXml(var5.getChild("BuildTradingOptions"));
         var4.addParam("TradingOptions", var6);

         for (int var7 = 0; var7 < var6.size(); var7++) {
            TradingOption var8 = (TradingOption)var6.get(var7);
            if (var8 != null && var8 instanceof ReservedBars) {
               var4.addParam("ReservedBars", var8.isUsed() ? ((ReservedBars)var8).ReservedBars : 0);
               break;
            }
         }
      } catch (Exception var10) {
         var4.addError(this.getSettingName(), null, "Cannot parse Options settings - " + var10.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
      Element var3 = null;

      try {
         var3 = XMLUtil.getChildElem(var1, "Options");
      } catch (Exception var12) {
         return;
      }

      var2.put(new JSONArray());
      var2.put(new JSONArray());

      for (TradingOption var6 : TradingOptionsList.getInstance().parseOptionsFromXml(var3.getChild("BuildTradingOptions"))) {
         for (IPGParameter var9 : var6.getParams()) {
            try {
               if (var9.getKey().startsWith("Picker")) {
                  var2.getJSONArray(0).put(new JSONObject().put("key", var9.getName()).put("value", var9.print()));
               } else {
                  var2.getJSONArray(1).put(new JSONObject().put("key", var9.getName()).put("value", var9.print()));
               }
            } catch (Exception var11) {
               Log.error("Error while printing option value. Exc. ", var11);
            }
         }
      }
   }

   public String getSettingName() {
      return "Options";
   }

   public String getName() {
      return L.tsq("Trading options");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
