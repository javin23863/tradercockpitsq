package com.strategyquant.plugin.CrossCheck.impl.WhatIf;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SampleTypes;
import com.strategyquant.tradinglib.WhatIf;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.crosscheck.CrossCheckDataNotExistException;
import com.strategyquant.tradinglib.crosscheck.CrossCheckMethod;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import java.util.ArrayList;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.jdom2.Element;

@PluginImplementation
public class WhatIfPlugin extends CrossCheckMethod implements IServletPlugin {
   private ServletContextHandler dataContext;

   public ICrossCheck clone(SettingsMap var1) {
      WhatIfPlugin var2 = new WhatIfPlugin();
      var2.settings = var1 != null ? var1 : this.settings.clone();
      return var2;
   }

   public String getName() {
      return NameWhatIf;
   }

   public String getShortName() {
      return L.t("What If", new Object[0]);
   }

   public String getDescription() {
      return L.tsq(
         "This cross check allows you to apply various What-If scenarios to the list of orders, for example what if the strategy trades only on particular days.<br/>It creates simulations by filtering the existing orders from the main backtest, it doesn't retest the strategies again."
      );
   }

   public String getSettingName() {
      return "WhatIf";
   }

   public int getType() {
      return 0;
   }

   public int getPreferredPosition() {
      return 220;
   }

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/whatIf/");
         this.dataContext.addServlet(new ServletHolder(new WhatIfServlet()), "/*");
      }

      return this.dataContext;
   }

   public void fixSettings(Element var1) {
      super.fixSettings(var1);
      Element var2 = var1.getChild("Settings");
      XMLUtil.tryAddElement(var2, "Methods");
   }

   public void readSettings(Element var1, TaskSettingsData var2) throws Exception {
      this.fixSettings(var1);
      Element var3 = var1.getChild("Settings");
      this.settings.set("WhatIfMethods", ProjectConfigHelper.getWhatIfMethods(var3.getChild("Methods")));
      this.readConditions(var1);
   }

   public boolean runTest(ResultsGroup var1, int var2, double var3, GridJob var5, boolean var6, ILastEventListener var7, String var8) throws Exception {
      this.checkSpecialTrialLimitation();
      ArrayList var9 = (ArrayList)this.settings.get("WhatIfMethods");
      String var10 = var1.getMainResultKey();
      Result var11 = var1.subResult(var10);
      SettingsMap var12 = var11.getSettings();
      SettingsMap var13 = this.cloneSettings(var12);
      OrdersList var14 = var1.orders();
      OrdersList var15 = var14.filterWithClone(var10, (byte)0, (byte)127);

      for (int var16 = 0; var16 < var9.size(); var16++) {
         ((WhatIf)var9.get(var16)).filter(var15);
      }

      String var19 = "CrossCheck_WhatIf";

      for (int var17 = 0; var17 < var15.size(); var17++) {
         Order var18 = var15.get(var17);
         var18.SetupName = var19;
         var18.IsInPortfolio = 0;
      }

      var1.addSubresult(var19, var13);
      var14.addAll(var15);
      Result var20 = var1.subResult(var19);
      var20.setSpecial(true);
      var20.computeAllStats(var1.specialValues(), var1.getOOS());
      if (this.backtestProgressListener != null) {
         this.backtestProgressListener.increaseProgressStep();
      }

      return this.checkConditions(var1, var2);
   }

   public boolean doesRetest() {
      return false;
   }

   public boolean doesForEverySetup() {
      return false;
   }

   public ChartSetups getChartSetups(ChartSetup var1) {
      return null;
   }

   public String printSettings(Element var1) throws Exception {
      if (var1 == null) {
         throw new NullPointerException();
      }

      if (!XMLUtil.elementIs(var1, "use")) {
         return "Not used";
      }

      Element var2 = var1.getChild("Settings");
      String var3 = "";
      ArrayList var4 = ProjectConfigHelper.getWhatIfMethods(var2.getChild("Methods"));

      for (int var5 = 0; var5 < var4.size(); var5++) {
         WhatIf var6 = (WhatIf)var4.get(var5);
         var3 = var3 + var6.printFormatedName();
         var3 = var3 + "\n";
      }

      var3 = var3 + "\n";
      Element var12 = var1.getChild("AcceptanceSettings").getChild("Conditions");
      ArrayList var13 = ProjectConfigHelper.getConditions(var12);

      for (int var7 = 0; var7 < var13.size(); var7++) {
         Condition var8 = (Condition)var13.get(var7);
         if (var8.isUsed()) {
            var3 = var3 + var8.toString();
            var3 = var3 + "\n";
         }
      }

      return var3;
   }

   public int getBadStrategyReason() {
      return 10037;
   }

   public int getNumberOfSimulations() {
      return 0;
   }

   public double getStatsValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      if (!var1.getResultKeys().contains("CrossCheck_WhatIf")) {
         throw new CrossCheckDataNotExistException(this.getName());
      }

      byte var5 = XMLUtil.getByteAttr(var3, "direction", (byte)0);
      byte var6 = this.getSampleType(var3, var4);
      byte var7 = XMLUtil.getByteAttr(var3, "plType", (byte)10);
      DatabankColumn var8 = (DatabankColumn)DatabankColumns.get().findClassByName(var2);
      return var8.getNumericValue(var1, "CrossCheck_WhatIf", var5, var7, var6);
   }

   public String printSpecialValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      if (!var1.getResultKeys().contains("CrossCheck_WhatIf")) {
         throw new CrossCheckDataNotExistException(this.getName());
      }

      byte var5 = XMLUtil.getByteAttr(var3, "direction", (byte)0);
      byte var6 = this.getSampleType(var3, var4);
      byte var7 = XMLUtil.getByteAttr(var3, "plType", (byte)10);
      DatabankColumn var8 = (DatabankColumn)DatabankColumns.get().findClassByName(var2);
      return var8.getValue(var1, var1.getMainResultKey(), var5, var7, var6);
   }

   public String getColumnTitle(String var1, Element var2, Object... var3) {
      byte var4 = XMLUtil.getByteAttr(var2, "direction", (byte)0);
      byte var5 = this.getSampleType(var2, var3);
      byte var6 = XMLUtil.getByteAttr(var2, "plType", (byte)10);
      String var7 = this.getColumnTitleTemplate();
      var7 = var7.replace("#columnName#", var1);
      var7 = var7.replace(", #sampleType#", var5 == 127 ? "" : ", " + SampleTypes.typeToString(var5));
      var7 = var7.replace(", #direction#", var4 == 0 ? "" : ", " + Directions.typeToString(var4));
      return var7.replace(", #plType#", var6 == 10 ? "" : ", " + PlTypes.typeToString(var6));
   }

   public String getColumnTitleTemplate() {
      return "#columnName# (" + this.getShortName() + ", #sampleType#, #direction#, #plType#)";
   }

   public boolean doesCreateSubjobs() {
      return false;
   }

   public boolean hasStatsValue(ResultsGroup var1, String var2, Element var3, Object... var4) throws Exception {
      return var1.getResultKeys().contains("CrossCheck_WhatIf");
   }

   public boolean disabledForSpecialTrial() {
      return true;
   }
}
