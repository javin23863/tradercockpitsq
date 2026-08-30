package com.strategyquant.plugin.Settings.impl.CrossChecks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.crosscheck.CrossCheckComparator;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
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
import org.eclipse.jetty.servlet.ServletHolder;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "Cross checks setting plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "NA")
@PluginImplementation
public class CrossChecksPlugin implements ISettingTabPlugin, IServletPlugin {
   public static final Logger Log = LoggerFactory.getLogger(CrossChecksPlugin.class);
   private static final String tabTitle = "Cross checks";
   private static final CrossCheckComparator csComparator = new CrossCheckComparator();
   private ServletContextHandler dataContext;
   private CrossChecksServlet servlet;

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.servlet = new CrossChecksServlet();
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/crosschecks/");
         this.dataContext.addServlet(new ServletHolder(this.servlet), "/*");
      }

      return this.dataContext;
   }

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 60;
   }

   public void initPlugin() throws Exception {
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      try {
         Element var5 = XMLUtil.tryAddElement(var3, this.getSettingName());
         boolean var17 = XMLUtil.elementIs(var5, "use");
         boolean var7 = XMLUtil.elementIs(var5, "evaluateAll");
         ArrayList var8 = new ArrayList();
         if (var17) {
            String var9 = ProjectConfigHelper.getEngineKey(var3);
            if (var9 == null) {
               throw new Exception(L.t("Engine cannot be resolved from project settings.", new Object[0]));
            }

            for (Element var11 : var5.getChildren()) {
               String var12 = var11.getName();

               try {
                  boolean var13 = XMLUtil.elementIs(var11, "use");
                  if (var13 && MainApp.v571hfnsHw().gDmtOfRJBr()) {
                     ICrossCheck var18 = this.getCrossCheckPlugin(var11.getName());
                     if (var18 != null) {
                        if (!SQUtils.checkEngine(var9, var18.forEngine())) {
                           Log.info(
                              "Cross check '{}' is not compatible with engine '{}', supported engines '{}'. Skipping!!!",
                              new Object[]{var18.getName(), var9, var18.forEngine()}
                           );
                        } else {
                           var18.readSettings(var11, var4);
                           var8.add(var18);
                        }
                     } else {
                        Log.error("Cannot find cross check plugin '" + var11.getName() + "'");
                     }
                  }
               } catch (Exception var15) {
                  String var14 = L.t("Cannot load settings of Cross check '%s'.", new Object[]{var12}) + " " + var15.getMessage();
                  Log.error(var1 + " - " + var14, var15);
                  var4.addError("Cross checks", null, var14);
               }
            }
         }

         var4.addParam("UseRobustnessTests", MainApp.v571hfnsHw() != null && MainApp.v571hfnsHw().gDmtOfRJBr() ? var17 : false);
         var4.addParam("CrossChecks.EvaluateAll", var7);
         var8.sort(csComparator);
         var4.addParam("CrossChecks", var8);
      } catch (Exception var16) {
         String var6 = L.t("Cannot load CrossChecks settings. %s", new Object[0]) + " " + var16.getMessage();
         Log.error(var6, var16);
         var4.addError("Cross checks", null, var6);
      }
   }

   private ICrossCheck getCrossCheckPlugin(String var1) {
      ArrayList var2 = SQPluginManager.getPlugins(ICrossCheck.class);

      for (int var3 = 0; var3 < var2.size(); var3++) {
         if (((ICrossCheck)var2.get(var3)).getSettingName().equals(var1)) {
            return ((ICrossCheck)var2.get(var3)).clone(null);
         }
      }

      return null;
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
      ArrayList var3 = SQPluginManager.getPlugins(ICrossCheck.class);
      Element var4 = XMLUtil.tryAddElement(var1, this.getSettingName());
      boolean var5 = XMLUtil.elementIs(var4, "use");
      String var6 = "Not used";

      for (int var7 = 0; var7 < var3.size(); var7++) {
         ICrossCheck var8 = (ICrossCheck)var3.get(var7);
         if (var5) {
            for (Element var10 : var4.getChildren()) {
               String var11 = var10.getName();
               if (var8.getSettingName().equals(var11)) {
                  try {
                     ICrossCheck var12 = this.getCrossCheckPlugin(var10.getName());
                     var6 = var12.printSettings(var10);
                     if (var6 == null) {
                        throw new NullPointerException();
                     }
                  } catch (Exception var13) {
                     var6 = "N/A";
                  }
                  break;
               }
            }
         }

         var2.put(new JSONObject().put("key", var8.getName()).put("value", var6));
      }
   }

   public String getSettingName() {
      return "CrossChecks";
   }

   public String getName() {
      return L.tsq("Cross checks");
   }

   public JSONObject getInitializationData() throws Exception {
      JSONObject var1 = new JSONObject();
      var1.put("crosschecks", this.servlet.list());
      return var1;
   }
}
