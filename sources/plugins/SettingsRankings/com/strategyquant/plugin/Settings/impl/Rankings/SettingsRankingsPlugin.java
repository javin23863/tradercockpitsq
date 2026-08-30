package com.strategyquant.plugin.Settings.impl.Rankings;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.BadStrategyException;
import com.strategyquant.tradinglib.CorrelationType;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.correlation.CorrelationTypes;
import com.strategyquant.tradinglib.correlation.FitPortfolio;
import com.strategyquant.tradinglib.customanalysis.CustomAnalysisInfo;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.util.ArrayList;
import java.util.List;
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
@Name(name = "Ranking plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SettingsRankingsPlugin implements ISettingTabPlugin, IServletPlugin {
   public static final Logger Log = LoggerFactory.getLogger(SettingsRankingsPlugin.class);
   private ServletContextHandler dataContext;
   private SettingsRankingsServlet servlet;

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.servlet = new SettingsRankingsServlet();
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/rankingsettings/");
         this.dataContext.addServlet(new ServletHolder(this.servlet), "/*");
      }

      return this.dataContext;
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
      XMLUtil.tryAddIntNode(var2, "MaxStrategies", 500);

      try {
         XMLUtil.getChildElem(var2, "FitnessCriteria");
      } catch (Exception var6) {
         Element var4 = XMLUtil.tryAddElement(var2, "FitnessCriteria");
         var4.setAttribute("method", "ComputeFromStrategyResult");
         Element var5 = XMLUtil.tryAddElement(var4, "Ranking");
         var5.setAttribute("type", "NetProfit");
      }

      if (var2.getChild("StopCondition") == null) {
         Element var3 = XMLUtil.tryAddElement(var2, "StopCondition");
         var3.setAttribute("type", "never");
      }

      if (var2.getChild("FitPortfolio") == null) {
         Element var7 = XMLUtil.tryAddElement(var2, "FitPortfolio");
         XMLUtil.tryAddElement(var7, "Correlation");
      }

      XMLUtil.tryAddIntNode(var2, "ConditionsType", 1);
      XMLUtil.tryAddElement(var2, "Conditions");
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      this.fixSettings(var3);
      Element var5 = null;

      try {
         var5 = XMLUtil.getChildElem(var3, this.getSettingName());
      } catch (Exception var25) {
         var4.addError(this.getSettingName(), null, var25.getMessage());
         return;
      }

      String var6 = var2.getType();

      try {
         Element var7 = XMLUtil.getChildElem(var5, "MaxStrategies");
         int var8 = Integer.parseInt(var7.getText());
         var4.addParam("MaxStrategies", var8);
      } catch (Exception var24) {
         var4.addError(this.getSettingName(), "MaxStrategies", L.t("Max strategies count not set or invalid", new Object[0]));
         return;
      }

      if (var6.equals("Build") || var6.equals("Retest") || var6.equals("AutomaticRetest")) {
         try {
            var4.addParam("Conditions", ProjectConfigHelper.getConditions(var5.getChild("Conditions")));
         } catch (Exception var23) {
            var4.addError(this.getSettingName(), "Conditions", L.t("Cannot load custom filters.", new Object[0]));
            return;
         }
      }

      if (var6.equals("Retest") || var6.equals("AutomaticRetest")) {
         try {
            var4.addParam("DeleteFailedStrategies", XMLUtil.getBoolean(var5, "DeleteFailedStrategies", false));
         } catch (Exception var22) {
            var4.addError(this.getSettingName(), "DeleteFailedStrategies", L.t("Cannot load option DeleteFailedStrategies.", new Object[0]));
            return;
         }

         try {
            var4.addParam("ForceRunCrossChecks", XMLUtil.getBoolean(var5, "ForceRunCrossChecks", false));
         } catch (Exception var21) {
            var4.addError(this.getSettingName(), "ForceRunCrossChecks", L.t("Cannot load option ForceRunCrossChecks.", new Object[0]));
            return;
         }
      }

      if (var6.equals("Build")) {
         try {
            try {
               var4.addParam("DismissTooSimilarStrategies", XMLUtil.getBoolean(var5, "DismissTooSimilarStrategies", true));
            } catch (Exception var19) {
               var4.addError(this.getSettingName(), "DismissTooSimilarStrategies", L.t("Cannot load option DismissTooSimilarStrategies.", new Object[0]));
               return;
            }

            Element var29 = XMLUtil.getChildElem(var5, "StopCondition");
            String var34 = var29.getAttributeValue("type").toString();
            var4.addParam("StopConditionType", var34);
            if (var34.equals("passed-count")) {
               try {
                  int var9 = Integer.parseInt(var29.getAttributeValue("passedStrategies"));
                  var4.addParam("MaxStrategiesPassed", var9);
               } catch (Exception var18) {
                  var4.addError(this.getSettingName(), "PassedStrategies", L.t("Passed strategies count not set or has invalid value", new Object[0]));
                  return;
               }
            } else if (var34.equals("time-limit")) {
               try {
                  int var38 = Integer.parseInt(var29.getAttributeValue("days"));
                  int var10 = Integer.parseInt(var29.getAttributeValue("hours"));
                  int var11 = Integer.parseInt(var29.getAttributeValue("minutes"));
                  long var12 = var38 * 24 * 3600 * 1000 + var10 * 3600 * 1000 + var11 * 60 * 1000;
                  if (var12 <= 0L) {
                     throw new Exception("Build run time must be greater than 0 ms.");
                  }

                  var4.addParam("MaxBuildRunTime", var12);
               } catch (Exception var17) {
                  var4.addError(this.getSettingName(), "TimeLimit", L.t("Build run time limit not set or has invalid value", new Object[0]));
                  return;
               }
            }
         } catch (Exception var20) {
            var4.addError(this.getSettingName(), "StopCondition", L.t("Stop condition type not set", new Object[0]));
            return;
         }
      }

      if (var6.equals("Build") || var6.equals("Retest") || var6.equals("AutomaticRetest")) {
         try {
            var4.addParam("StrategyDismissSettings", ProjectConfigHelper.getDismissBadStrategiesSettings(var5));
            var4.addParam("StrategyDismissWarnings", ProjectConfigHelper.getDismissBadStrategiesWarnings(var5));
         } catch (Exception var16) {
            var4.addError(this.getSettingName(), "Conditions", L.t("Cannot load automatic filters.", new Object[0]) + " " + var16.getMessage());
            return;
         }
      }

      if (var6.equals("Build")) {
         try {
            FitPortfolio var30 = new FitPortfolio();
            Element var35 = XMLUtil.getChildElem(var5, "FitPortfolio");
            var30.active = XMLUtil.getBooleanAttr(var35, "active", false);
            String var39 = XMLUtil.getStringAttr(var35, "databank", "Existing portfolio");
            SQProject var40 = ProjectEngine.get(var1);
            if (var39.equals("") || var39.equals("null") || var40.isAppProject()) {
               var39 = "Existing portfolio";
            }

            var30.databank = (Databank)var40.getDatabanks().get(var39);
            if (var30.databank == null) {
               Log.error(String.format("Databank for Fit Portfolio not found('%s')", var39));
               if (!ProjectEngine.get(var1).getDatabanks().containsKey("Existing portfolio")) {
                  Log.debug("ExistingPortfolio databank does not exist. Creating it...");
                  var30.databank = ProjectEngine.get(var1).getDatabanks().add("Existing portfolio");
               }
            }

            if (var30.databank == null) {
               var4.addError(this.getSettingName(), "Databank", L.t("No suitable databank set for Fit Portfolio ('%s')", new Object[]{var39}));
               return;
            }

            Element var41 = XMLUtil.getChildElem(var35, "Correlation");
            var30.corrMax = XMLUtil.getDoubleAttr(var41, "max", 0.3);
            String var42 = XMLUtil.getStringAttr(var41, "type", "ProfitLoss");
            var30.corrType = (CorrelationType)CorrelationTypes.getInstance().findClassByName(var42);
            var30.corrPeriod = XMLUtil.getIntAttr(var41, "period", 10);
            var30.corrAllowNegative = XMLUtil.getBooleanAttr(var41, "allowNegative", false);
            var30.corrAddEmptyPeriods = XMLUtil.getBooleanAttr(var41, "addEmptyPeriods", false);
            var4.addParam("FitPortfolio", var30);
         } catch (Exception var27) {
            var4.addError(this.getSettingName(), null, var27.getMessage());
            return;
         }
      }

      try {
         var4.addParam("UseFitnessByIndex", false);
         Element var31 = XMLUtil.getChildElem(var5, "FitnessCriteria");
         String var36 = var31.getAttributeValue("useFitnessByIndex");
         if (var36 != null && var36.equals("true")) {
            var4.addParam("UseFitnessByIndex", true);
         }
      } catch (Exception var26) {
         var4.addError(this.getSettingName(), "FitnessFunction", L.t("Cannot load fitnessByInd function.", new Object[0]) + " " + var26.getMessage());
         return;
      }

      try {
         IFitnessFunction var32 = ProjectConfigHelper.getFitnessFunction(var5);
         var4.addParam("FitnessFunction", var32);
      } catch (Exception var15) {
         var4.addError(this.getSettingName(), "FitnessFunction", L.t("Cannot load fitness function.", new Object[0]) + " " + var15.getMessage());
         return;
      }

      try {
         Element var33 = var5.getChild("CustomAnalysis");
         CustomAnalysisInfo var37 = new CustomAnalysisInfo();
         var37.loadRankingConfig(var33);
         var4.addParam("CustomAnalysis", var37);
      } catch (Exception var14) {
         var4.addError(this.getSettingName(), "CustomAnalysis", L.t("Cannot load custom analysis.", new Object[0]) + " " + var14.getMessage());
      }
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
      Element var3 = XMLUtil.tryAddElement(var1, this.getSettingName());
      IFitnessFunction var4 = ProjectConfigHelper.getFitnessFunction(var3);
      Element var5 = XMLUtil.getChildElem(var3, "MaxStrategies");
      String var6 = var5 != null ? "" + Integer.parseInt(var5.getText()) : L.t("N/A", new Object[0]);
      var2.put(new JSONObject().put("key", L.t("Maximum top strategies to store", new Object[0])).put("value", var6));
      String var7 = var4.getFitnessDatabankColumnName();
      String var8 = var4.getFitnessName() + ": " + var7;
      if (var7.equals("Weighted")) {
         var8 = var8 + "\nGoals: " + var4.printWeightedGoals();
      }

      var2.put(new JSONObject().put("key", L.t("Fitness computation", new Object[0])).put("value", var8));
      String var9 = "";

      try {
         Element var10 = var3.getChild("AutomaticDismissal");
         List var11 = var10.getChildren("Problem");

         for (int var12 = 0; var12 < var11.size(); var12++) {
            Element var13 = (Element)var11.get(var12);
            if (XMLUtil.elementIs(var13, "dismiss")) {
               int var14 = Integer.parseInt(var13.getAttributeValue("code"));
               String var15 = BadStrategyException.reasonToShortText(var14);
               var9 = var9 + var15;
               var9 = var9 + "\n";
            }
         }

         var2.put(new JSONObject().put("key", L.t("Automatic filters", new Object[0])).put("value", var9.isEmpty() ? L.t("None", new Object[0]) : var9));
      } catch (Exception var17) {
         var2.put(new JSONObject().put("key", L.t("Automatic filters", new Object[0])).put("value", L.t("None", new Object[0])));
      }

      try {
         ArrayList var21 = ProjectConfigHelper.getConditions(var3.getChild("Conditions"));
         var9 = "";

         for (int var22 = 0; var22 < var21.size(); var22++) {
            Condition var23 = (Condition)var21.get(var22);
            if (var23.isUsed()) {
               var9 = var9 + var23.toString();
               var9 = var9 + "\n";
            }
         }

         var2.put(new JSONObject().put("key", L.t("Custom filters", new Object[0])).put("value", var9.isEmpty() ? L.t("None", new Object[0]) : var9));
      } catch (Exception var16) {
         var2.put(new JSONObject().put("key", L.t("Custom filters", new Object[0])).put("value", L.t("None", new Object[0])));
      }
   }

   public String getSettingName() {
      return "Rankings";
   }

   public String getName() {
      return L.tsq("Rankings");
   }

   public JSONObject getInitializationData() throws Exception {
      JSONObject var1 = new JSONObject();
      var1.put("fitnessMethods", this.servlet.getFitnessMethods());
      return var1;
   }
}
