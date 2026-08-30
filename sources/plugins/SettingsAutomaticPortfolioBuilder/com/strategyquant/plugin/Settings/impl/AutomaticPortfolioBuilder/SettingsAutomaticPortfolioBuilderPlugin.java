package com.strategyquant.plugin.Settings.impl.AutomaticPortfolioBuilder;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterSettings;
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

@Author(name = "Tomas Brynda")
@Name(name = "AutomaticPortfolioBuilder setting plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SettingsAutomaticPortfolioBuilderPlugin implements ISettingTabPlugin, IServletPlugin {
   public static final Logger Log = LoggerFactory.getLogger(SettingsAutomaticPortfolioBuilderPlugin.class);
   private ServletContextHandler dataContext;
   private SettingsAutomaticPortfolioBuilderServlet servlet;

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.servlet = new SettingsAutomaticPortfolioBuilderServlet();
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/pmsettings/");
         this.dataContext.addServlet(new ServletHolder(this.servlet), "/*");
      }

      return this.dataContext;
   }

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 6;
   }

   public void initPlugin() throws Exception {
   }

   private void fixSettings(Element var1) {
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      Element var5 = null;

      try {
         var5 = XMLUtil.getChildElem(var3, this.getSettingName());
      } catch (Exception var8) {
         var4.addError(this.getSettingName(), null, var8.getMessage());
         return;
      }

      this.fixSettings(var3);

      try {
         PortfolioMasterSettings var6 = new PortfolioMasterSettings();
         var6.loadFromXml(var5, var1);
         var4.addParam("Conditions", var6.conditions);
         var4.addParam("AutomaticPortfolioBuilder", var6);
      } catch (Exception var7) {
         var4.addError(this.getSettingName(), null, L.t("Cannot load PortfolioMaster settings. ", new Object[0]) + var7.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getSettingName() {
      return "AutomaticPortfolioBuilder";
   }

   public String getName() {
      return L.tsq("Automatic Portfolio Builder");
   }

   public JSONObject getInitializationData() throws Exception {
      JSONObject var1 = new JSONObject();
      var1.put("fitnessTypes", this.servlet.getFitnessTypes());
      return var1;
   }
}
