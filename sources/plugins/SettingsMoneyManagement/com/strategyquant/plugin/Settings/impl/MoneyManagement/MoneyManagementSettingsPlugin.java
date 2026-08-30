package com.strategyquant.plugin.Settings.impl.MoneyManagement;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.RiskManagementMethod;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.propertygrid.IPGParameter;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.eclipse.jetty.server.Handler;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Tomas Brynda")
@Name(name = "Money management servlet plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class MoneyManagementSettingsPlugin implements ISettingTabPlugin, IServletPlugin {
   public static final Logger Log = LoggerFactory.getLogger(MoneyManagementSettingsPlugin.class);
   private static final String tabTitle = "Money management";

   public Handler getHandler() {
      return null;
   }

   public String getProduct() {
      return "SQUANTAlgoWizardAlgoWizardStandaloneAlgoWizardCloud";
   }

   public int getPreferredPosition() {
      return 20;
   }

   public void initPlugin() throws Exception {
   }

   private void fixSettings(Element var1) {
      Element var2 = XMLUtil.tryAddElement(var1, this.getSettingName());
      Element var3 = XMLUtil.tryAddElement(var2, "MoneyManagement");

      try {
         XMLUtil.getChildElem(var3, "Method");
      } catch (Exception var10) {
         Element var5 = XMLUtil.tryAddElement(var3, "Method");
         var5.setAttribute("type", "FixedSize");
         var5.setAttribute("use", "true");
         Element var6 = XMLUtil.tryAddElement(var5, "Params");
         Element var7 = XMLUtil.tryAddElement(var6, "Param");
         var7.setAttribute("key", "Size");
         var7.setAttribute("className", "FixedSize");
         var7.setText("0.1");
      }

      try {
         XMLUtil.getChildElem(var3, "InitialCapital");
      } catch (Exception var9) {
         Element var11 = XMLUtil.tryAddElement(var3, "InitialCapital");
         var11.setText("10000");
      }

      Element var4 = XMLUtil.tryAddElement(var2, "RiskManagement");

      try {
         XMLUtil.getChildElem(var4, "Method");
      } catch (Exception var8) {
         Element var12 = XMLUtil.tryAddElement(var4, "Method");
         var12.setAttribute("type", "AllowAllTrades");
         var12.setAttribute("use", "true");
         var12.addContent(new Element("Params"));
      }
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      this.fixSettings(var3);
      Element var5 = null;
      Element var6 = null;

      try {
         Element var7 = XMLUtil.getChildElem(var3, this.getSettingName());
         var5 = XMLUtil.getChildElem(var7, "MoneyManagement");
         var6 = XMLUtil.getChildElem(var7, "RiskManagement");
      } catch (Exception var12) {
         var4.addError("Money management", null, var12.getMessage());
         return;
      }

      try {
         MoneyManagementMethod var15 = ProjectConfigHelper.getMoneyManagement(var5);
         var4.addParam("MoneyManagement.Method", var15);
         double var8 = Double.parseDouble(XMLUtil.getChildElem(var5, "InitialCapital").getValue());
         var4.addParam("MoneyManagement.InitialCapital", var8);
      } catch (Exception var11) {
         var4.addError("Money management", null, "Cannot load MoneyManagement settings. " + var11.getMessage());
      }

      try {
         var4.addParam("RiskManagement", ProjectConfigHelper.getRiskManagement(var6));
      } catch (Exception var10) {
         var4.addError("Money management", null, "Cannot load RiskManagement settings. " + var10.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
      Element var3 = null;
      Element var4 = null;

      try {
         Element var5 = XMLUtil.getChildElem(var1, this.getSettingName());
         var3 = XMLUtil.getChildElem(var5, "MoneyManagement");
         var4 = XMLUtil.getChildElem(var5, "RiskManagement");
      } catch (Exception var14) {
         return;
      }

      MoneyManagementMethod var17 = ProjectConfigHelper.getMoneyManagement(var3);
      RiskManagementMethod var6 = ProjectConfigHelper.getRiskManagement(var4);
      var2.put(new JSONObject().put("key", "Initial capital").put("value", var17.getInitialCapital()));
      String var7 = var17.getName();
      String var8 = "";

      for (IPGParameter var11 : var17.getParams()) {
         var8 = var8 + var11.getName() + " " + var11.print() + ",";
      }

      if (var8.length() > 0) {
         var7 = var7 + " - " + var8;
      }

      var2.put(new JSONObject().put("key", L.t("Money Management method", new Object[0])).put("value", var7));
      String var19 = var6.getName();
      String var20 = "";

      for (IPGParameter var13 : var6.getParams()) {
         var20 = var20 + var13.getName() + " " + var13.print() + ",";
      }

      if (var20.length() > 0) {
         var19 = var19 + " - " + var20;
      }

      var2.put(new JSONObject().put("key", L.t("Risk Management method", new Object[0])).put("value", var19));
   }

   public String getSettingName() {
      return "RiskMoneyManagement";
   }

   public String getName() {
      return L.tsq("Money management");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
