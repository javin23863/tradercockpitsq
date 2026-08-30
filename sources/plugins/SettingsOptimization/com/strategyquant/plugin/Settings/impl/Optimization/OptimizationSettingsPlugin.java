package com.strategyquant.plugin.Settings.impl.Optimization;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.optimization.OptimizationSettings;
import com.strategyquant.tradinglib.optimization.Parameter;
import com.strategyquant.tradinglib.optimization.ParametersSettings;
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
@Name(name = "Optimization setting plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class OptimizationSettingsPlugin implements ISettingTabPlugin, IServletPlugin {
   public static final Logger Log = LoggerFactory.getLogger(OptimizationSettingsPlugin.class);
   private ServletContextHandler connectionContext;

   public Handler getHandler() {
      if (this.connectionContext == null) {
         this.connectionContext = new ServletContextHandler(1);
         this.connectionContext.setContextPath("/optimization/");
         this.connectionContext.addServlet(new ServletHolder(new OptimizationServlet()), "/*");
      }

      return this.connectionContext;
   }

   public String getProduct() {
      return "SQMANAGERSQUANTSQTRADERTESTAPPSQEDITOR";
   }

   public int getPreferredPosition() {
      return 5;
   }

   public void initPlugin() throws Exception {
   }

   private void fixSettings(Element var1) {
      Element var2 = XMLUtil.tryAddElement(var1, this.getSettingName());
      String var3 = var2.getAttributeValue("type");
      if (var3 == null) {
         var2.setAttribute("type", String.valueOf(1));
         Element var4 = XMLUtil.tryAddElement(var2, "SimpleOptimization");
         var4.setAttribute("type", String.valueOf(10));
      }

      Element var10 = XMLUtil.tryAddElement(var2, "Source");
      String var5 = var10.getAttributeValue("type");
      if (var5 == null) {
         var10.setAttribute("type", String.valueOf(1));
      }

      Element var6 = XMLUtil.tryAddElement(var2, "OptimizationMethod");
      if (var6.getAttributeValue("settings") == null) {
         var6.setAttribute("settings", "automatic");
      }

      if (var6.getAttributeValue("method") == null) {
         var6.setAttribute("method", "brute-force");
      }

      if (var6.getAttributeValue("maxSteps") == null) {
         var6.setAttribute("maxSteps", "20");
      }

      if (var6.getAttributeValue("symmetricVariables") == null) {
         var6.setAttribute("symmetricVariables", "true");
      }

      if (var6.getAttributeValue("symmetryDisabled") == null) {
         var6.setAttribute("symmetryDisabled", "false");
      }

      Element var7 = XMLUtil.tryAddElement(var2, "AutomaticSettings");
      if (var7.getAttributeValue("distribution") == null) {
         var7.setAttribute("distribution", "20");
      }

      if (var7.getAttributeValue("maxSteps") == null) {
         var7.setAttribute("maxSteps", "4");
      }

      Element var8 = XMLUtil.tryAddElement(var2, "WhatToParametrize");
      XMLUtil.tryAddBooleanNode(var8, "Periods", true);
      XMLUtil.tryAddBooleanNode(var8, "Shifts", true);
      XMLUtil.tryAddBooleanNode(var8, "ExitParamsUsed", true);
      XMLUtil.tryAddBooleanNode(var8, "ExitParamsUnused", true);
      XMLUtil.tryAddBooleanNode(var8, "Constants", true);
      XMLUtil.tryAddBooleanNode(var8, "OtherParams", true);
      XMLUtil.tryAddBooleanNode(var8, "BooleanParams", true);
      XMLUtil.tryAddBooleanNode(var8, "TradingOptions", true);
      Element var9 = XMLUtil.tryAddElement(var2, "ManualSettings");
      XMLUtil.tryAddElement(var9, "Params");
      XMLUtil.tryAddElement(var9, "SpecialRelations");
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      this.fixSettings(var3);
      OptimizationSettings var5 = new OptimizationSettings(var1);

      try {
         var5.setFromXML(var3, var3.getChild("Optimization"));
         var5.verify();
         var4.addParam("OptimizationSettings", var5);
         var4.addParam("DatabankTarget", var5.outputDatabank);
      } catch (Exception var19) {
         var4.addError(this.getSettingName(), null, var19.getMessage());
      }

      Element var6 = null;
      Element var7 = null;
      boolean var8 = false;

      try {
         var6 = XMLUtil.getChildElem(var3, "Optimization");
         var7 = XMLUtil.getChildElem(var6, "OptimizationMethod");
      } catch (Exception var18) {
         var4.addError(this.getSettingName(), null, var18.getMessage());
         return;
      }

      try {
         var8 = var7.getAttributeValue("settings").equals("manual");
      } catch (Exception var17) {
         var4.addError(this.getSettingName(), null, "Cannot get Parameter settings type");
         return;
      }

      if (var7.getAttributeValue("method") == null) {
         var4.addError(this.getSettingName(), "method", "Method not set");
      } else if (!var5.isOptimizationConfigurable()) {
         if (var8) {
            boolean var9 = false;

            for (Element var11 : XMLUtil.getNestedElements(var6, "Param")) {
               boolean var12 = XMLUtil.elementIs(var11, "use");
               String var13 = var11.getAttributeValue("name").trim();
               if (var12) {
                  var9 = true;

                  try {
                     this.checkParameter(var11, var4);
                  } catch (Exception var16) {
                     var4.addError(this.getSettingName(), "", "Invalid manual settings of Parameter '" + var13 + "' - " + var16.getMessage());
                  }
               }
            }

            if (!var9) {
            }
         } else {
            try {
               Element var23 = XMLUtil.getChildElem(var6, "AutomaticSettings");
               if (XMLUtil.tryGetIntAttr(var23, "distribution") < 0) {
                  var4.addError(this.getSettingName(), "distribution", L.t("Cannot get distribution value", new Object[0]));
               }

               if (XMLUtil.tryGetIntAttr(var23, "maxSteps") < 0) {
                  var4.addError(this.getSettingName(), "maxSteps", L.t("Cannot get maxSteps value", new Object[0]));
               }
            } catch (Exception var15) {
               var4.addError(this.getSettingName(), "AutomaticSettings", var15.getMessage());
            }
         }
      }
   }

   private void checkParameter(Element var1, TaskSettingsData var2) throws Exception {
      byte var3 = Parameter.getType(var1.getAttributeValue("type"));
      if (var3 == 4 || var3 == 1) {
         double var10 = Double.parseDouble(var1.getAttributeValue("start"));
         double var11 = Double.parseDouble(var1.getAttributeValue("stop"));
         double var8 = Double.parseDouble(var1.getAttributeValue("step"));
         Double.parseDouble(var1.getAttributeValue("value"));
         ParametersSettings.checkParamValid(var10, var11, var8);
      } else if (var3 == 5) {
         int var4 = (int)Double.parseDouble(var1.getAttributeValue("start"));
         int var5 = (int)Double.parseDouble(var1.getAttributeValue("stop"));
         int var6 = (int)Double.parseDouble(var1.getAttributeValue("step"));
         if (var4 > var5) {
            throw new Exception(L.t("Start time must be before stop time", new Object[0]));
         }

         ParametersSettings.checkTimeParamValid(var4, var5, var6);
      } else {
         var1.getAttributeValue("value").equals("true");
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
   }

   public String getSettingName() {
      return "Optimization";
   }

   public String getName() {
      return L.tsq("Optimization");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
