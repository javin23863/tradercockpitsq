package com.strategyquant.plugin.Settings.impl.WhatToBuild;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.generator.StrategyTemplateGenerator;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.servlet.IServletPlugin;
import com.strategyquant.tradinglib.task.settings.ISettingTabPlugin;
import com.strategyquant.tradinglib.task.settings.TaskSettingsData;
import com.strategyquant.tradinglib.task.settings.buildmode.BuildMode;
import com.strategyquant.tradinglib.task.settings.buildtype.BuildType;
import com.strategyquant.tradinglib.task.settings.buildtype.ChartSettings;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.io.File;
import java.io.Serializable;
import java.util.Iterator;
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

@Author(name = "Tomas Brynda")
@Name(name = "Build type servlet plugin")
@Category(name = "Settings")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class WhatToBuildSettingsPlugin implements ISettingTabPlugin, IServletPlugin {
   public static final Logger Log = LoggerFactory.getLogger(WhatToBuildSettingsPlugin.class);
   private static final String tabTitle = "What to build";
   private ServletContextHandler dataContext;
   private WhatToBuildServlet servlet;

   public Handler getHandler() {
      if (this.dataContext == null) {
         this.servlet = new WhatToBuildServlet();
         this.dataContext = new ServletContextHandler(1);
         this.dataContext.setContextPath("/buildType/");
         this.dataContext.addServlet(new ServletHolder(this.servlet), "/*");
      }

      return this.dataContext;
   }

   public String getProduct() {
      return "SQUANT";
   }

   public int getPreferredPosition() {
      return 5;
   }

   public void initPlugin() throws Exception {
   }

   private void fixSettings(Element var1) {
      Element var2 = XMLUtil.tryAddElement(var1, "WhatToBuild");
      Element var3 = XMLUtil.tryAddElement(var2, "MarketSides");
      String var4 = XMLUtil.tryGetAttr(var3, "type", "both");
      if (var4.equals("both")) {
         Element var5 = XMLUtil.tryAddElement(var3, "EntrySymmetry");
         String var6 = var5.getText();
         if (var6 == null || var6.isEmpty()) {
            var5.setText("true");
         }

         Element var7 = XMLUtil.tryAddElement(var3, "ExitSymmetry");
         String var8 = var7.getText();
         if (var8 == null || var8.isEmpty()) {
            var7.setText("true");
         }
      }

      Element var11 = XMLUtil.tryAddElement(var2, "RulesComplexity");
      List var12 = var11.getChildren("Chart");
      if (var12.isEmpty()) {
         Element var13 = XMLUtil.tryAddElement(var11, "Chart");
         var13.setAttribute("name", "Main chart");
         var13.setAttribute("minConditions", "1");
         var13.setAttribute("maxConditions", "4");
         var13.setAttribute("minExitConditions", "1");
         var13.setAttribute("maxExitConditions", "4");
         var13.setAttribute("maxExitTypes", "5");
         var13.setAttribute("minPeriod", "2");
         var13.setAttribute("maxPeriod", "100");
         var13.setAttribute("minShift", "0");
         var13.setAttribute("maxShift", "10");
      }

      Element var14 = XMLUtil.tryAddElement(var2, "BuildMode");
      if (var14.getAttributeValue("generationType") == null) {
         var14.setAttribute("generationType", "random-generation");
      }

      try {
         Element var15 = XMLUtil.getChildElem(var2, "StrategyType");
         if (var15 != null) {
            String var9 = var15.getAttributeValue("architecture");
            if (var9 != null && var9.equals("sq4fuzzy")) {
               if (var15.getAttributeValue("minTrue") == null) {
                  var15.setAttribute("minTrue", "60");
               }

               if (var15.getAttributeValue("maxTrue") == null) {
                  var15.setAttribute("maxTrue", "80");
               }
            }
         }
      } catch (Exception var10) {
         Log.error("Exception ", var10);
         return;
      }

      this.fixSLPTOptions(var2);
   }

   private void fixSLPTOptions(Element var1) {
      Element var2 = XMLUtil.tryAddElement(var1, "SLPTOptions");
      XMLUtil.tryAddBooleanNode(var2, "SLRequired", true);
      XMLUtil.tryAddBooleanNode(var2, "PTRequired", true);
      XMLUtil.tryAddNode(var2, "SLValueType", "pips");
      XMLUtil.tryAddBooleanNode(var2, "SLATR", true);
      XMLUtil.tryAddBooleanNode(var2, "SLFixedPips", true);
      XMLUtil.tryAddBooleanNode(var2, "SLPercent", true);
      XMLUtil.tryAddIntNode(var2, "MinSLInPips", 40);
      XMLUtil.tryAddDoubleNode(var2, "MinSLInMoney", 50.0);
      XMLUtil.tryAddDoubleNode(var2, "MinSLInPercent", 1.0);
      XMLUtil.tryAddIntNode(var2, "MaxSLInPips", 200);
      XMLUtil.tryAddDoubleNode(var2, "MaxSLInMoney", 300.0);
      XMLUtil.tryAddDoubleNode(var2, "MaxSLInPercent", 10.0);
      XMLUtil.tryAddDoubleNode(var2, "MinSLATRMultiple", 1.0);
      XMLUtil.tryAddDoubleNode(var2, "MaxSLATRMultiple", 15.0);
      XMLUtil.tryAddIntNode(var2, "MinSLATRPeriod", 20);
      XMLUtil.tryAddIntNode(var2, "MaxSLATRPeriod", 20);
      XMLUtil.tryAddBooleanNode(var2, "SeparatedSettings", false);
      XMLUtil.tryAddNode(var2, "PTValueType", "pips");
      XMLUtil.tryAddBooleanNode(var2, "PTFixedPips", true);
      XMLUtil.tryAddBooleanNode(var2, "PTATR", true);
      XMLUtil.tryAddBooleanNode(var2, "PTPercent", true);
      XMLUtil.tryAddIntNode(var2, "MinPTInPips", 40);
      XMLUtil.tryAddIntNode(var2, "MinPTInMoney", 50);
      XMLUtil.tryAddDoubleNode(var2, "MinPTInPercent", 1.0);
      XMLUtil.tryAddIntNode(var2, "MaxPTInPips", 200);
      XMLUtil.tryAddIntNode(var2, "MaxPTInMoney", 300);
      XMLUtil.tryAddDoubleNode(var2, "MaxPTInPercent", 10.0);
      XMLUtil.tryAddDoubleNode(var2, "MinPTATRMultiple", 1.0);
      XMLUtil.tryAddDoubleNode(var2, "MaxPTATRMultiple", 15.0);
      XMLUtil.tryAddIntNode(var2, "MinPTATRPeriod", 20);
      XMLUtil.tryAddIntNode(var2, "MaxPTATRPeriod", 20);
      XMLUtil.tryAddBooleanNode(var2, "LimitSLPTRRR", false);
      XMLUtil.tryAddIntNode(var2, "LimitSLPTRRRFrom", 1);
      XMLUtil.tryAddIntNode(var2, "LimitSLPTRRRTo", 1);
   }

   public void readSettings(String var1, ISQTask var2, Element var3, TaskSettingsData var4) {
      this.fixSettings(var3);
      Element var5 = null;
      Element var6 = null;
      Element var7 = null;
      Element var8 = null;
      Element var9 = null;
      Element var10 = null;

      try {
         var5 = XMLUtil.getChildElem(var3, this.getSettingName());
         var6 = XMLUtil.getChildElem(var5, "StrategyType");
         var7 = XMLUtil.getChildElem(var5, "MarketSides");
         var8 = XMLUtil.getChildElem(var5, "RulesComplexity");
         var9 = XMLUtil.getChildElem(var5, "BuildMode");
         var10 = XMLUtil.getChildElem(var5, "SLPTOptions");
      } catch (Exception var25) {
         Log.error("Exception ", var25);
         var4.addError("What to build", null, var25.getMessage());
         return;
      }

      BuildType var11 = new BuildType();
      var11.strategyType = var6.getAttributeValue("type");
      if (var11.strategyType == null) {
         var4.addError("What to build", "StrategyType", "Strategy type not filled");
      } else {
         try {
            var4.addParam("BuildType", var11.strategyType);
         } catch (Exception var24) {
            Log.error("Cannot add build type", var24);
         }

         try {
            switch (var11.strategyType) {
               case "simple":
                  break;
               case "multi-tf":
                  this.checkMultiTFType(var6, var8, var11, var4);
                  break;
               case "template":
                  var11.templateFile = var6.getAttributeValue("templateFile");
                  if (var11.templateFile == null) {
                     var4.addError("What to build", "TemplateFile", "Template file not set");
                     return;
                  }

                  this.checkTemplateType(var3, var2, var4);
                  break;
               case "improve":
                  this.checkImproveType(var6, var11, var4, var1);
                  break;
               default:
                  var4.addError("What to build", "StrategyType", "Unknown strategy type: '" + var11.strategyType + "'");
                  return;
            }
         } catch (Exception var23) {
            Log.error("Exception ", var23);
            return;
         }

         try {
            var11.direction = var7.getAttributeValue("type");
            switch (var11.direction) {
               case "both":
               case "long":
               case "short":
                  break;
               default:
                  throw new Exception();
            }
         } catch (Exception var26) {
            Log.error("Exception ", var26);
            var4.addError("What to build", "StrategyDirections", "Strategy directions type not set or invalid");
            return;
         }

         if (var11.direction.equals("both")) {
            try {
               var11.entrySymmetry = Boolean.parseBoolean(var7.getChild("EntrySymmetry").getText());
            } catch (Exception var22) {
               var4.addError("What to build", "EntrySymmetry", "Entry symmetry not set or invalid");
               return;
            }

            try {
               var11.exitSymmetry = Boolean.parseBoolean(var7.getChild("ExitSymmetry").getText());
            } catch (Exception var21) {
               var4.addError("What to build", "ExitSymmetry", "Exit symmetry not set or invalid");
               return;
            }
         }

         try {
            var4.addParam("WhatToBuild", var11);
         } catch (Exception var20) {
            Log.error("StrategyType param is already defined", var20);
         }

         try {
            if (ProjectEngine.isDefaulProject(var1)) {
               Databank var34 = (Databank)ProjectEngine.get(var1).getDatabanks().get("Results");
               if (var34 == null) {
                  var4.addParam("DatabankTarget", ProjectConfigHelper.getOutputDatabank(var1, var3));
               } else {
                  var4.addParam("DatabankTarget", var34);
               }
            } else {
               var4.addParam("DatabankTarget", ProjectConfigHelper.getOutputDatabank(var1, var3));
            }
         } catch (Exception var19) {
            Log.error("Exception ", var19);
            var4.addError(this.getSettingName(), null, var19.getMessage());
         }

         try {
            BuildMode var35 = new BuildMode();
            var35.generationType = var9.getAttributeValue("generationType");
            switch (var35.generationType) {
               case "genetic-evolution":
                  this.checkGeneticEvolution(var9, var35, var4);
                  if (var35.initGenerationType == 2) {
                     try {
                        var4.addParam("DatabankSource", (Serializable)ProjectEngine.get(var1).getDatabanks().get("Initial population"));
                     } catch (Exception var17) {
                        var4.addError(this.getSettingName(), null, var17.getMessage());
                     }
                  }

                  var4.addParam("DatabankLastGeneration", (Serializable)ProjectEngine.get(var1).getDatabanks().get("Last generation"));
               case "random-generation":
                  var4.addParam("BuildMode", var35);
                  break;
               default:
                  var4.addError("What to build", "GenerationType", "Unknown generation type: '" + var35.generationType + "'");
                  return;
            }
         } catch (Exception var18) {
            Log.error("Exception ", var18);
            var4.addError("What to build", null, var18.getMessage());
         }

         try {
            this.readBuildSettings(var10, var6, var4);
            this.checkNumberOfExitTypes(var3, var4, var11);
         } catch (Exception var16) {
            Log.error("Exception", var16);
            var4.addError(this.getSettingName(), "SL/PT settings", var16.getMessage());
         }
      }
   }

   private void readBuildSettings(Element var1, Element var2, TaskSettingsData var3) throws Exception {
      boolean var4 = Boolean.parseBoolean(var1.getChild("SLRequired").getText());
      boolean var5 = Boolean.parseBoolean(var1.getChild("PTRequired").getText());
      int var6 = Integer.parseInt(var1.getChild("MinSLInPips").getText());
      int var7 = Integer.parseInt(var1.getChild("MaxSLInPips").getText());
      double var8 = Double.parseDouble(var1.getChild("MinSLInPercent").getText());
      double var10 = Double.parseDouble(var1.getChild("MaxSLInPercent").getText());
      double var12 = Double.parseDouble(var1.getChild("MinSLATRMultiple").getText());
      double var14 = Double.parseDouble(var1.getChild("MaxSLATRMultiple").getText());
      int var16 = Integer.parseInt(var1.getChild("MinSLATRPeriod").getText());
      int var17 = Integer.parseInt(var1.getChild("MaxSLATRPeriod").getText());
      int var18 = Integer.parseInt(var1.getChild("MinPTInPips").getText());
      int var19 = Integer.parseInt(var1.getChild("MaxPTInPips").getText());
      double var20 = Double.parseDouble(var1.getChild("MinPTInPercent").getText());
      double var22 = Double.parseDouble(var1.getChild("MaxPTInPercent").getText());
      double var24 = Double.parseDouble(var1.getChild("MinPTATRMultiple").getText());
      double var26 = Double.parseDouble(var1.getChild("MaxPTATRMultiple").getText());
      int var28 = Integer.parseInt(var1.getChild("MinPTATRPeriod").getText());
      int var29 = Integer.parseInt(var1.getChild("MaxPTATRPeriod").getText());
      boolean var30 = Boolean.parseBoolean(var1.getChild("LimitSLPTRRR").getText());
      int var31 = Integer.parseInt(var1.getChild("LimitSLPTRRRFrom").getText());
      int var32 = Integer.parseInt(var1.getChild("LimitSLPTRRRTo").getText());
      boolean var33 = XMLUtil.getBooleanFromElText(var1.getChild("SLATR"), false);
      boolean var34 = XMLUtil.getBooleanFromElText(var1.getChild("SLFixedPips"), false);
      boolean var35 = XMLUtil.getBooleanFromElText(var1.getChild("SLPercent"), false);
      boolean var36 = XMLUtil.getBooleanFromElText(var1.getChild("PTATR"), false);
      boolean var37 = XMLUtil.getBooleanFromElText(var1.getChild("PTFixedPips"), false);
      boolean var38 = XMLUtil.getBooleanFromElText(var1.getChild("PTPercent"), false);
      String var39 = var2.getAttributeValue("architecture");
      int var40 = 60;
      int var41 = 80;
      if (var39 != null && var39.equals("sq4fuzzy")) {
         var40 = Integer.parseInt(var2.getAttributeValue("minTrue"));
         var41 = Integer.parseInt(var2.getAttributeValue("maxTrue"));
      }

      boolean var42 = false;
      if (var34 && var7 < var6) {
         var3.addError("What to build", "Min/Max SL", "Max SL in pips cannot be smaller than Min SL in pips");
         var42 = true;
      }

      if (var35 && var10 < var8) {
         var3.addError("What to build", "Min/Max SL", "Max SL in percent cannot be smaller than Min SL in percent");
         var42 = true;
      }

      if (var33 && var14 < var12) {
         var3.addError("What to build", "Min/Max SL", "Max SL ATR coeficient cannot be smaller than Min SL ATR coeficient");
         var42 = true;
      }

      if (var33 && var17 < var16) {
         var3.addError("What to build", "Min/Max SL", "Max SL ATR period cannot be smaller than Min SL ATR period");
         var42 = true;
      }

      if (var37 && var19 < var18) {
         var3.addError("What to build", "Min/Max PT", "Max PT in pips cannot be smaller than Min PT in pips");
         var42 = true;
      }

      if (var38 && var22 < var20) {
         var3.addError("What to build", "Min/Max SL", "Max PT in percent cannot be smaller than Min PT in percent");
         var42 = true;
      }

      if (var36 && var26 < var24) {
         var3.addError("What to build", "Min/Max PT", "Max PT ATR coeficient cannot be smaller than Min PT ATR coeficient");
         var42 = true;
      }

      if (var36 && var29 < var28) {
         var3.addError("What to build", "Min/Max PT", "Max PT ATR period cannot be smaller than Min PT ATR period");
         var42 = true;
      }

      if (var30 && var31 > var32) {
         var3.addError("What to build", "Limit Risk-reward ratio", "Minumum must be smaller or equal to maximum!");
         var42 = true;
      }

      if (!var42) {
         boolean var43 = Boolean.parseBoolean(var1.getChild("SeparatedSettings").getText());
         if (!var43) {
            var18 = var6;
            var19 = var7;
            var24 = var12;
            var26 = var14;
            var28 = var16;
            var29 = var17;
            var36 = var33;
         }

         if (var30) {
            double var44 = var31 / 100.0;
            double var46 = var32 / 100.0;
            if (var34 && var37) {
               var19 = (int)this.setCorrectMax(var18, var19, var46 * var7);
               var18 = (int)this.setCorrectMin(var18, var19, var44 * var6);
               var6 = (int)this.setCorrectMin(var6, var7, var18 / var44);
               var7 = (int)this.setCorrectMax(var6, var7, var18 / var46);
            }

            if (var33 && var36) {
               var26 = this.setCorrectMax(var24, var26, var46 * var14);
               var24 = this.setCorrectMin(var24, var26, var44 * var12);
               var12 = this.setCorrectMin(var12, var14, var24 / var44);
               var14 = this.setCorrectMax(var12, var14, var26 / var46);
            }
         }

         SettingsMap var48 = new SettingsMap();
         var48.set("SLRequired", var4);
         var48.set("PTRequired", var5);
         var48.set("MinSLInPips", var6);
         var48.set("MaxSLInPips", var7);
         var48.set("MinSLInPercent", var8);
         var48.set("MaxSLInPercent", var10);
         var48.set("MinSLATRMultiple", var12);
         var48.set("MaxSLATRMultiple", var14);
         var48.set("MinSLATRPeriod", var16);
         var48.set("MaxSLATRPeriod", var17);
         var48.set("MinPTInPips", var18);
         var48.set("MaxPTInPips", var19);
         var48.set("MinPTInPercent", var20);
         var48.set("MaxPTInPercent", var22);
         var48.set("MinPTATRMultiple", var24);
         var48.set("MaxPTATRMultiple", var26);
         var48.set("MinPTATRPeriod", var28);
         var48.set("MaxPTATRPeriod", var29);
         var48.set("LimitSLPTRRR", var30);
         var48.set("LimitSLPTRRRFrom", var31);
         var48.set("LimitSLPTRRRTo", var32);
         var48.set("SLATR", var33);
         var48.set("PTATR", var36);
         var48.set("SLFixedPips", var34);
         var48.set("PTFixedPips", var37);
         var48.set("SLPercent", var35);
         var48.set("PTPercent", var38);
         var48.set("SeparateSLPT", var43);
         var48.set("FuzzyMinTrue", var40);
         var48.set("FuzzyMaxTrue", var41);
         var48.set("Architecture", var39);
         var3.addParam("BuildSettings", var48);
      }
   }

   private double setCorrectMin(double var1, double var3, double var5) {
      if (var5 < var1) {
         return var1;
      } else {
         return var5 < var3 ? var5 : var3;
      }
   }

   private double setCorrectMax(double var1, double var3, double var5) {
      if (var5 > var3) {
         return var3;
      } else {
         return var5 > var1 ? var5 : var3;
      }
   }

   private void checkGeneticEvolution(Element var1, BuildMode var2, TaskSettingsData var3) {
      try {
         XMLUtil.tryAddIntNode(var1, "PopulationSize", 5);
         XMLUtil.tryAddIntNode(var1, "MaxGenerations", 10);
         XMLUtil.tryAddIntNode(var1, "CrossoverProbability", 50);
         XMLUtil.tryAddIntNode(var1, "MutationProbability", 20);
         XMLUtil.tryAddBooleanNode(var1, "ShowAdvancedGeneticSettings", false);
         XMLUtil.tryAddIntNode(var1, "Islands", 10);
         XMLUtil.tryAddIntNode(var1, "MigrationModulo", 50);
         XMLUtil.tryAddIntNode(var1, "MigrationRate", 20);
         XMLUtil.tryAddBooleanNode(var1, "ShowLastGenerationDatabank", false);
         XMLUtil.tryAddIntNode(var1, "InitGenerationType", 1);
         XMLUtil.tryAddIntNode(var1, "DecimationCoef", 1);
         XMLUtil.tryAddBooleanNode(var1, "FilterInitialPopulation", true);
         XMLUtil.tryAddElement(var1, "Conditions");
         XMLUtil.tryAddBooleanNode(var1, "EvoRestartOnStagnation", true);
         XMLUtil.tryAddIntNode(var1, "EvoFitnessRestartType", 0);
         XMLUtil.tryAddIntNode(var1, "EvoStagnationRestartGenerations", 3);
         XMLUtil.tryAddBooleanNode(var1, "ShowLastGenerationDatabank", false);
         XMLUtil.tryAddBooleanNode(var1, "FreshBloodReplaceSimilar", true);
         XMLUtil.tryAddBooleanNode(var1, "FreshBloodReplaceWeakest", true);
         XMLUtil.tryAddIntNode(var1, "FreshBloodWeakestPct", 10);
         XMLUtil.tryAddIntNode(var1, "FreshBloodWeakestGenerations", 2);
         var2.populationSize = Integer.parseInt(XMLUtil.getNodeValue(var1, "PopulationSize"));
         var2.maxGenerations = Integer.parseInt(XMLUtil.getNodeValue(var1, "MaxGenerations"));
         var2.crossoverProbability = Integer.parseInt(XMLUtil.getNodeValue(var1, "CrossoverProbability"));
         var2.mutationProbability = Integer.parseInt(XMLUtil.getNodeValue(var1, "MutationProbability"));
         var2.islands = Integer.parseInt(XMLUtil.getNodeValue(var1, "Islands"));
         if (var2.islands < 1 || var2.islands > 100) {
            var3.addError("What to build", "Islands", L.t("Islands number must be between 1 and 100", new Object[0]));
         }

         var2.migrationModulo = Integer.parseInt(XMLUtil.getNodeValue(var1, "MigrationModulo"));
         var2.migrationRate = Integer.parseInt(XMLUtil.getNodeValue(var1, "MigrationRate"));
         var2.initGenerationType = Integer.parseInt(XMLUtil.getNodeValue(var1, "InitGenerationType"));
         var2.decimationCoef = Integer.parseInt(XMLUtil.getNodeValue(var1, "DecimationCoef"));

         try {
            var2.conditions = ProjectConfigHelper.getConditions(var1.getChild("Conditions"));
         } catch (Exception var8) {
            throw new Exception(L.t("Invalid initial population conditions: %s", new Object[]{var8.getMessage()}));
         }

         var3.addParam("InitialGenerationConditions", var2.conditions);
         var2.evoRestartOnStagnation = XMLUtil.getNodeBooleanValue(var1, "EvoRestartOnStagnation", false);
         var2.evoFitnessRestartType = XMLUtil.getNodeIntValue(var1, "EvoFitnessRestartType", 40);
         var2.evoStagnationRestartGenerations = Integer.parseInt(XMLUtil.getNodeValue(var1, "EvoStagnationRestartGenerations", "0"));
         var2.showLastGeneration = Boolean.parseBoolean(XMLUtil.getNodeValue(var1, "ShowLastGenerationDatabank", "false"));
         Element var4 = var1.getChild("EvoRestartOnFinish");
         if (var4 != null) {
            String var5 = var4.getAttributeValue("status");
            if (var5 != null && var5.equals("true")) {
               var2.evoRestartOnFinish = true;
            }
         }

         Element var10 = var1.getChild("EvoRestartOnStagnation");
         if (var10 != null) {
            String var6 = var10.getAttributeValue("status");
            if (var6 != null && var6.equals("true")) {
               var2.evoRestartOnStagnation = true;
            }

            String var7 = var10.getAttributeValue("generations");
            if (var7 != null && !var7.equals("")) {
               var2.evoStagnationRestartGenerations = Integer.parseInt(var7);
            }

            var2.evoFitnessRestartType = XMLUtil.getIntAttr(var10, "fitnessType", (byte)10);
         }

         Element var11 = var1.getChild("EvoInSamplePeriod");
         if (var11 != null) {
            var2.evoDivideInSamplePeriod = XMLUtil.getBooleanAttr(var11, "divide", true);
            var2.evoTrainingValidationRatio = XMLUtil.getIntAttr(var11, "ratio", 50) / 100;
         } else {
            var2.evoDivideInSamplePeriod = true;
            var2.evoTrainingValidationRatio = 0.5;
         }

         var2.freshBloodReplaceSimilar = Boolean.parseBoolean(XMLUtil.getNodeValue(var1, "FreshBloodReplaceSimilar", "true"));
         var2.freshBloodReplaceWeakest = Boolean.parseBoolean(XMLUtil.getNodeValue(var1, "FreshBloodReplaceWeakest", "true"));
         var2.freshBloodWeakestPct = Integer.parseInt(XMLUtil.getNodeValue(var1, "FreshBloodWeakestPct", "10"));
         var2.freshBloodWeakestGenerations = Integer.parseInt(XMLUtil.getNodeValue(var1, "FreshBloodWeakestGenerations", "2"));
      } catch (Exception var9) {
         var3.addError("What to build", null, "Cannot load GeneticEvolution settings. " + var9.getMessage());
      }
   }

   private void checkImproveType(Element var1, BuildType var2, TaskSettingsData var3, String var4) throws Exception {
      var2.improveType = var1.getAttributeValue("improveType");
      if (var2.improveType == null) {
         var3.addError("What to build", "ImproveType", L.t("Improve type not set", new Object[0]));
         throw new Exception();
      }

      switch (var2.improveType) {
         case "strategy":
            var2.strategyFile = var1.getAttributeValue("strategyFile");
            if (var2.strategyFile == null) {
               var3.addError("What to build", "Strategy file", L.t("Improve strategy file not set", new Object[0]));
               throw new Exception();
            }

            if (!var2.strategyFile.contains("/") && !var2.strategyFile.contains("\\")) {
               var2.strategyFile = MainApp.getDataPath() + "user/strategies/" + var2.strategyFile;
            }

            if (!new File(var2.strategyFile).exists()) {
               var3.addError("What to build", "Strategy file", "Strategy file '" + var2.strategyFile + "' doesn't exist");
               throw new Exception();
            }
            break;
         case "databank":
            var2.improveDatabank = var1.getAttributeValue("improveDatabank");
            if (var2.improveDatabank == null || var2.improveDatabank.isEmpty()) {
               var2.improveDatabank = ProjectEngine.isDefaulProject(var4) ? "Strategies to improve" : "Results";
            }
            break;
         default:
            var3.addError("What to build", "ImproveType", "Unknown improve type: '" + var2.improveType + "'");
            throw new Exception();
      }
   }

   private void checkTemplateType(Element var1, ISQTask var2, TaskSettingsData var3) throws Exception {
      if (MainApp.runInConsoleWithoutActiveWebserver()) {
         Element var4 = StrategyTemplateGenerator.getConfiguredTemplate(var1, var2.getCustomName());
         if (var4 != null) {
            List var5 = ((Element)var1.getChild("Data").getChild("Setups").getChildren("Setup").get(0)).getChildren("Chart");
            int var6 = var5.size();
            Element var7 = (Element)var5.get(0);
            String var8 = var7.getAttributeValue("symbol");
            String var9 = var7.getAttributeValue("timeframe");
            String var10 = var7.getAttributeValue("spread");
            Element var11 = var4.getChild("Strategy").getChild("Datas");
            List var12 = var11.getChildren("data");
            int var13 = var12.size();
            if (var13 > var6) {
               for (int var14 = var6; var14 < var12.size(); var14++) {
                  Element var15 = (Element)var12.get(var14);
                  String var16 = XMLUtil.getNodeValue(var15, "symbol");
                  String var17 = XMLUtil.getNodeValue(var15, "timeFrame");

                  try {
                     TimeframeManager.getMillis(var17);
                  } catch (Exception var20) {
                     var17 = TimeframeManager.translateSQ3TFToString(Integer.parseInt(var17));
                  }

                  DataInfo var18 = DataManager.getDataInfo("History", var16);
                  if (var18 == null) {
                     return;
                  }

                  if (TimeframeManager.getMillis(var17) < TimeframeManager.getMillis(var18.timeframe)) {
                     return;
                  }

                  Element var19 = new Element("Chart");
                  var19.setAttribute("symbol", var16);
                  var19.setAttribute("timeframe", var17);
                  var19.setAttribute("spread", var10);
                  var5.add(var19);
               }
            }
         }
      }
   }

   private void checkMultiTFType(Element var1, Element var2, BuildType var3, TaskSettingsData var4) throws Exception {
      try {
         var3.additionalCharts = Integer.parseInt(var1.getAttributeValue("additionalCharts"));
      } catch (Exception var23) {
         var4.addError("What to build", "AdditionalCharts", L.t("Additional charts number not set or invalid", new Object[0]));
         throw new Exception();
      }

      boolean var5 = false;

      try {
         var5 = Boolean.parseBoolean(XMLUtil.getAttr(var2, "useDifferentSettings"));
      } catch (Exception var22) {
      }

      int var6 = 0;

      for (Element var8 : var2.getChildren("Chart")) {
         ChartSettings var9 = new ChartSettings();
         String var10 = this.getChartName(var6);

         try {
            var9.minConditions = Integer.parseInt(var8.getAttributeValue("minConditions"));
            if (var9.minConditions < 0) {
               throw new Exception();
            }
         } catch (Exception var21) {
            var4.addError("What to build", "AdditionalCharts", var10 + " - Min conditions not set or invalid. Min allowed value: 1");
         }

         try {
            var9.maxConditions = Integer.parseInt(var8.getAttributeValue("maxConditions"));
         } catch (Exception var20) {
            var4.addError("What to build", "AdditionalCharts", var10 + " - Max conditions not set or invalid");
         }

         if (var9.minConditions > var9.maxConditions) {
            var4.addError("What to build", "AdditionalCharts", var10 + " - Min conditions must be lower than Max conditions");
         }

         boolean var11 = false;

         try {
            var9.minExitConditions = Integer.parseInt(var8.getAttributeValue("minExitConditions"));
            if (var9.minExitConditions < 0) {
               throw new Exception();
            }

            var11 = true;
         } catch (Exception var19) {
            var9.minExitConditions = var9.minConditions;
         }

         try {
            var9.maxExitConditions = Integer.parseInt(var8.getAttributeValue("maxExitConditions"));
            if (var9.maxExitConditions < 0) {
               throw new Exception();
            }

            var11 = true;
         } catch (Exception var18) {
            var9.maxExitConditions = var9.maxExitConditions;
         }

         if (var11 && var9.minExitConditions > var9.maxExitConditions) {
            var4.addError("What to build", "AdditionalCharts", var10 + " - Min exit conditions must be lower than Max exit conditions");
         }

         if (var6 == 0) {
            try {
               var9.maxExitTypes = XMLUtil.getIntAttr(var8, "maxExitTypes", 5);
               if (var9.maxExitTypes < 1) {
                  throw new Exception();
               }
            } catch (Exception var17) {
               var4.addError("What to build", "AdditionalCharts", var10 + " - Max Exit Types not set or invalid. Min allowed value: 1");
            }
         }

         try {
            var9.minPeriod = Integer.parseInt(var8.getAttributeValue("minPeriod"));
            if (var9.minPeriod < 2) {
               throw new Exception();
            }
         } catch (Exception var16) {
            var4.addError("What to build", "AdditionalCharts", var10 + " - Min period not set or invalid. Min allowed value: 2");
         }

         try {
            var9.maxPeriod = Integer.parseInt(var8.getAttributeValue("maxPeriod"));
         } catch (Exception var15) {
            var4.addError("What to build", "AdditionalCharts", var10 + " - Max period not set or invalid");
         }

         if (var9.minPeriod > var9.maxPeriod) {
            var4.addError("What to build", "AdditionalCharts", var10 + " - Min period must be lower than Max period");
         }

         try {
            var9.minShift = Integer.parseInt(var8.getAttributeValue("minShift"));
            if (var9.minShift < 0) {
               throw new Exception();
            }
         } catch (Exception var14) {
            var4.addError("What to build", "AdditionalCharts", var10 + " - Min shift not set or invalid. Min allowed value: 0");
         }

         try {
            var9.maxShift = Integer.parseInt(var8.getAttributeValue("maxShift"));
         } catch (Exception var13) {
            var4.addError("What to build", "AdditionalCharts", var10 + " - Max shift not set or invalid");
         }

         var6++;
      }
   }

   private void checkNumberOfExitTypes(Element var1, TaskSettingsData var2, BuildType var3) throws Exception {
      Element var5;
      try {
         Element var4 = XMLUtil.getChildElem(var1, this.getSettingName());
         var5 = XMLUtil.getChildElem(var4, "RulesComplexity");
      } catch (Exception var12) {
         Log.error("Exception ", var12);
         var2.addError("What to build", null, var12.getMessage());
         return;
      }

      boolean var6 = false;
      Iterator var7 = var5.getChildren("Chart").iterator();
      if (var7.hasNext()) {
         Element var8 = (Element)var7.next();
         if (!var6) {
            int var9 = 0;

            try {
               var9 = XMLUtil.getIntAttr(var8, "maxExitTypes", 5);
               if (var9 < 0) {
                  throw new Exception();
               }
            } catch (Exception var11) {
               var2.addError("What to build", "WhatToBuild", " - Max Exit Types number not set or invalid");
            }

            SettingsMap var10 = (SettingsMap)var2.getParams().get("BuildSettings");
            var10.set("MaxExitTypes", var9);
            var10.set("ExitSymmetry", var3.exitSymmetry);
         }
      }
   }

   private String getChartName(int var1) {
      return var1 == 0 ? "Main chart" : "Additional chart " + var1;
   }

   public void getStrategyConfigSettings(Element var1, JSONArray var2) throws Exception {
      Element var3 = XMLUtil.tryAddElement(var1, "WhatToBuild");
      Element var4 = XMLUtil.tryAddElement(var3, "StrategyType");
      String var5 = XMLUtil.getStringAttr(var4, "type", "N/A");
      switch (var5) {
         case "simple":
            var5 = L.t("Simple strategies", new Object[0]);
            break;
         case "multi-tf":
            var5 = L.t("Multi-symbol/Multi-TF strategies", new Object[0]);
            break;
         case "template":
            var5 = L.t("Strategies using template", new Object[0]);
            break;
         case "improve":
            var5 = L.t("Improve existing strategy", new Object[0]);
      }

      var2.put(new JSONObject().put("key", L.t("Strategy type", new Object[0])).put("value", var5));
   }

   public String getSettingName() {
      return "WhatToBuild";
   }

   public String getName() {
      return L.tsq("What to build");
   }

   public JSONObject getInitializationData() throws Exception {
      return null;
   }
}
