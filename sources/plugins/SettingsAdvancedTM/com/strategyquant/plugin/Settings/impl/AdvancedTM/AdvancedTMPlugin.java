package com.strategyquant.plugin.Settings.impl.AdvancedTM;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.ATMExit;
import com.strategyquant.tradinglib.atm.ATMConfigVerifier;
import com.strategyquant.tradinglib.atm.ATMGenerateConfig;
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

@Author(name = "Tomas Brynda")
@Name(name = "Settings AdvancedTM plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class AdvancedTMPlugin implements ISettingTabPlugin, IServletPlugin {
   private ServletContextHandler dataContext;
   private AdvancedTMServlet servlet;

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 50;
   }

   public void initPlugin() throws Exception {
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.servlet = new AdvancedTMServlet();
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/advancedtm/");
         this.dataContext.addServlet(new ServletHolder(this.servlet), "/*");
      }

      return this.dataContext;
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      try {
         Element var5 = var3.getChild(this.getSettingName());
         ATMConfigVerifier.verifyConfig(var3, var4);
         if (var5 == null) {
            var4.addParam("ATM", null);
         } else {
            ATM var6 = new ATM(var5);
            if (var6.getType() == 3) {
               ATMGenerateConfig var7 = var6.getGenerateConfig();
               var7.verifyConfig(var4);
               var4.addParam("ATMGenerateConfig", var7);
               var6.changeToStrategy();
            } else {
               var4.addParam("ATMGenerateConfig", null);
            }

            var4.addParam("ATM", var6);
         }
      } catch (Exception var8) {
         var4.addError(this.getSettingName(), null, var8.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
      Element var3 = XMLUtil.tryAddElement(var1, this.getSettingName());

      try {
         ATM var4 = new ATM(var3);
         int var5 = var4.getType();
         switch (var5) {
            case 0:
               var2.put(new JSONObject().put("key", "ATM").put("value", "Don't use"));
               return;
            case 1:
               var2.put(new JSONObject().put("key", "ATM").put("value", "Use from strategy"));
               var2.put(new JSONObject().put("key", "Size decimals").put("value", var4.getSizeDecimals()));
               var2.put(new JSONObject().put("key", "Minimum size").put("value", var4.getMinimalSize()));
               return;
            case 2:
               var2.put(new JSONObject().put("key", "ATM").put("value", "From config"));
               var2.put(new JSONObject().put("key", "Size decimals").put("value", var4.getSizeDecimals()));
               var2.put(new JSONObject().put("key", "Minimum size").put("value", var4.getMinimalSize()));

               for (int var6 = 0; var6 < var4.getExitsCount(); var6++) {
                  ATMExit var7 = var4.getExit(var6);
                  var2.put(new JSONObject().put("key", String.format("Exit #%d", var6 + 1)).put("value", var7.toString()));
               }
         }
      } catch (Exception var8) {
         var2.put(new JSONObject().put("key", "ATM").put("value", "Don't use"));
      }
   }

   public String getSettingName() {
      return "ATMs";
   }

   public String getName() {
      return L.tsq("Advanced Trading Management");
   }

   public JSONObject getInitializationData() throws Exception {
      return new JSONObject();
   }
}
