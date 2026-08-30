package com.strategyquant.tradinglib.applyMassConfig;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.file.SQFileManager;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApplyMassConfig implements Serializable {
   private static final Logger Log = LoggerFactory.getLogger(ApplyMassConfig.class);
   public String loadFromType;
   public String sourceTaskName;
   private Element elSourceTask;
   public boolean applyData;
   public boolean applyDataTradingEngine;
   public boolean applyDataBacktestData;
   public boolean applyDataTestParameters;
   public boolean applyDataDataRangeParts;
   public boolean applyTradingOptions;
   public boolean applyBlocks;
   public boolean applyBlocksSignals;
   public boolean blocksIndicators;
   public boolean blocksapplyBlocksStopLimitEntry;
   public boolean applyBlocksOrderTypes;
   public boolean applyBlocksExitTypes;
   public boolean applyAtm;
   public boolean applyMoneyManagement;
   public boolean applyCrossChecks;
   public boolean whatToBuild;
   public boolean whatToRetest;
   public boolean partsToImprove;
   public boolean geneticOptions;
   public boolean optimization;
   public boolean ranking;
   public List<String> targetTasks = new ArrayList<>();

   public void init(SQProject var1, Element var2) throws Exception {
      if (var2.getChild("LoadFrom") != null) {
         Element var3 = XMLUtil.getChildElem(var2, "LoadFrom");
         this.loadFromType = XMLUtil.getNodeValue(var3, "Type", "file");
         if (this.loadFromType.equals("file")) {
            String var4 = XMLUtil.getNodeValue(var3, "File", null);
            if (var4.isBlank()) {
               throw new Exception(L.t("Source file not selected.", new Object[0]));
            }

            File var5 = new File(var4);
            if (!var5.exists()) {
               throw new Exception(L.t("Source file '%s' doesn't exist.", new Object[]{var5.getAbsolutePath()}));
            }

            if (!var5.getName().endsWith("cfx")) {
               throw new Exception("Source file must be '.cfx'.");
            }

            this.elSourceTask = XMLUtil.stringToXmlElement(SQFileManager.loadFile(var5.getAbsolutePath()));
         } else {
            this.sourceTaskName = XMLUtil.getNodeValue(var3, "Task", null);
            if (!var1.containsTask(this.sourceTaskName)) {
               throw new Exception(L.t("Source task '%s' doesn't exist.", new Object[]{this.sourceTaskName}));
            }
         }

         Element var9 = XMLUtil.getChildElem(var2, "WhatToApply");
         this.applyData = XMLUtil.getNodeBooleanValue(var9, "Data", false);
         this.applyDataTradingEngine = XMLUtil.getNodeBooleanValue(var9, "DataTradingEngine", false);
         this.applyDataBacktestData = XMLUtil.getNodeBooleanValue(var9, "DataBacktestData", false);
         this.applyDataTestParameters = XMLUtil.getNodeBooleanValue(var9, "DataTestParameters", false);
         this.applyDataDataRangeParts = XMLUtil.getNodeBooleanValue(var9, "DataDataRangeParts", false);
         this.applyTradingOptions = XMLUtil.getNodeBooleanValue(var9, "TradingOptions", false);
         this.applyBlocks = XMLUtil.getNodeBooleanValue(var9, "Blocks", false);
         this.applyBlocksSignals = XMLUtil.getNodeBooleanValue(var9, "BlocksSignals", false);
         this.blocksIndicators = XMLUtil.getNodeBooleanValue(var9, "BlocksIndicators", false);
         this.blocksapplyBlocksStopLimitEntry = XMLUtil.getNodeBooleanValue(var9, "BlocksStopLimitEntry", false);
         this.applyBlocksOrderTypes = XMLUtil.getNodeBooleanValue(var9, "BlocksOrderTypes", false);
         this.applyBlocksExitTypes = XMLUtil.getNodeBooleanValue(var9, "BlocksExitTypes", false);
         this.applyAtm = XMLUtil.getNodeBooleanValue(var9, "Atm", false);
         this.applyMoneyManagement = XMLUtil.getNodeBooleanValue(var9, "MoneyManagement", false);
         this.applyCrossChecks = XMLUtil.getNodeBooleanValue(var9, "CrossChecks", false);
         this.whatToBuild = XMLUtil.getNodeBooleanValue(var9, "WhatToBuild", false);
         this.whatToRetest = XMLUtil.getNodeBooleanValue(var9, "WhatToRetest", false);
         this.partsToImprove = XMLUtil.getNodeBooleanValue(var9, "PartsToImprove", false);
         this.geneticOptions = XMLUtil.getNodeBooleanValue(var9, "GeneticOptions", false);
         this.optimization = XMLUtil.getNodeBooleanValue(var9, "Optimization", false);
         this.ranking = XMLUtil.getNodeBooleanValue(var9, "Ranking", false);
         String var10 = XMLUtil.getNodeValue(var2, "TargetTasks", "");
         this.targetTasks.clear();
         String[] var6 = var10.split(",");

         for (int var7 = 0; var7 < var6.length; var7++) {
            String var8 = var6[var7];
            if (!var8.trim().isBlank()) {
               this.targetTasks.add(var8);
            }
         }

         if (this.targetTasks.isEmpty()) {
            throw new Exception(L.t("Target task not selected.", new Object[0]));
         }
      }
   }

   private void applyChangesToTask(Element var1, String var2, String var3) throws Exception {
      try {
         if (this.elSourceTask == null) {
            throw new NullPointerException("Source task is null.");
         }

         Element var4 = this.elSourceTask;
         if (this.applyData && var4.getChild("Data") != null && var1.getChild("Data") != null) {
            Element var5 = (Element)var4.getChild("Data").getChild("Setups").getChildren("Setup").get(0);
            Element var6 = (Element)var1.getChild("Data").getChild("Setups").getChildren("Setup").get(0);
            if (this.applyDataTradingEngine) {
               var6.setAttribute("engine", XMLUtil.getAttr(var5, "engine"));
            }

            String var7 = ((Element)var5.getChildren("Chart").get(0)).getAttributeValue("spread");
            if (this.applyDataBacktestData) {
               var6.setAttribute("dateFrom", XMLUtil.getAttr(var5, "dateFrom"));
               var6.setAttribute("dateTo", XMLUtil.getAttr(var5, "dateTo"));
               List var8 = var6.getChildren("Chart");
               var8.clear();
               List var9 = var5.getChildren("Chart");

               for (int var10 = 0; var10 < var9.size(); var10++) {
                  Element var11 = (Element)var9.get(var10);
                  var6.addContent(var11.clone());
               }
            }

            if (this.applyDataTestParameters) {
               var6.setAttribute("testPrecision", XMLUtil.getAttr(var5, "testPrecision"));
               var6.setAttribute("slippage", XMLUtil.getAttr(var5, "slippage"));
               var6.setAttribute("minDist", XMLUtil.getAttr(var5, "minDist"));
               var6.removeChild("Commissions");
               var6.addContent(var5.getChild("Commissions").clone());
               var6.removeChild("Swap");
               var6.addContent(var5.getChild("Swap").clone());
               List var25 = var6.getChildren("Chart");

               for (int var30 = 0; var30 < var25.size(); var30++) {
                  Element var37 = (Element)var25.get(var30);
                  var37.setAttribute("spread", var7);
               }
            }

            if (this.applyDataDataRangeParts && var1.getChild("Data").getChild("OutOfSample") != null && var4.getChild("Data").getChild("OutOfSample") != null) {
               var1.getChild("Data").removeChild("OutOfSample");
               var1.getChild("Data").addContent(var4.getChild("Data").getChild("OutOfSample").clone());
            }
         }

         if (this.applyTradingOptions && var4.getChild("Options") != null && var1.getChild("Options") != null) {
            var1.removeChild("Options");
            var1.addContent(var4.getChild("Options").clone());
         }

         if (this.applyBlocks) {
            Element var13 = var4.getChild("Blocks");
            Element var17 = var1.getChild("Blocks");
            if (var13 != null && var17 != null) {
               if (var13.getChild("BuildingBlocks") != null && var17.getChild("BuildingBlocks") != null) {
                  if (this.applyBlocksSignals) {
                     Element var26 = var17.getChild("BuildingBlocks");
                     List var31 = var26.getChildren("Block");

                     for (int var38 = var31.size() - 1; var38 >= 0; var38--) {
                        Element var44 = (Element)var31.get(var38);
                        if (XMLUtil.getAttr(var44, "category", "NA").equals("signals")) {
                           var31.remove(var44);
                        }
                     }

                     Element var21 = var13.getChild("BuildingBlocks");
                     var31 = var21.getChildren("Block");

                     for (int var39 = 0; var39 < var31.size(); var39++) {
                        Element var45 = (Element)var31.get(var39);
                        if (XMLUtil.getAttr(var45, "category", "NA").equals("signals")) {
                           var26.addContent(var45.clone());
                        }
                     }
                  }

                  if (this.blocksIndicators) {
                     Element var27 = var17.getChild("BuildingBlocks");
                     List var33 = var27.getChildren("Block");

                     for (int var40 = var33.size() - 1; var40 >= 0; var40--) {
                        Element var46 = (Element)var33.get(var40);
                        if (XMLUtil.getAttr(var46, "category", "NA").equals("indicators")) {
                           var33.remove(var46);
                        }
                     }

                     Element var22 = var13.getChild("BuildingBlocks");
                     var33 = var22.getChildren("Block");

                     for (int var41 = 0; var41 < var33.size(); var41++) {
                        Element var47 = (Element)var33.get(var41);
                        if (XMLUtil.getAttr(var47, "category", "NA").equals("indicators")) {
                           var27.addContent(var47.clone());
                        }
                     }
                  }

                  if (this.blocksapplyBlocksStopLimitEntry) {
                     Element var28 = var17.getChild("BuildingBlocks");
                     List var35 = var28.getChildren("Block");

                     for (int var42 = var35.size() - 1; var42 >= 0; var42--) {
                        Element var48 = (Element)var35.get(var42);
                        if (XMLUtil.getAttr(var48, "category", "NA").equals("stopLimitBlocks")) {
                           var35.remove(var48);
                        }
                     }

                     Element var23 = var13.getChild("BuildingBlocks");
                     var35 = var23.getChildren("Block");

                     for (int var43 = 0; var43 < var35.size(); var43++) {
                        Element var49 = (Element)var35.get(var43);
                        if (XMLUtil.getAttr(var49, "category", "NA").equals("stopLimitBlocks")) {
                           var28.addContent(var49.clone());
                        }
                     }
                  }
               }

               if (this.applyBlocksOrderTypes && var13.getChild("OrderTypes") != null && var17.getChild("OrderTypes") != null) {
                  var17.removeChild("OrderTypes");
                  var17.addContent(var13.getChild("OrderTypes").clone());
               }

               if (this.applyBlocksExitTypes && var13.getChild("ExitTypes") != null && var17.getChild("ExitTypes") != null) {
                  var17.removeChild("ExitTypes");
                  var17.addContent(var13.getChild("ExitTypes").clone());
               }
            }
         }

         if (this.applyAtm) {
            Element var14 = var4.getChild("ATMs");
            if (var14 != null && ("Build".equals(var2) || "Retest".equals(var2) || "Optimize".equals(var2))) {
               var1.removeChild("ATMs");
               var1.addContent(var4.getChild("ATMs").clone());
               if (!"Build".equals(var2)) {
                  Element var18 = var1.getChild("ATMs");
                  var18.setAttribute("scaleOutType", "1");
                  var18.removeChild("GenerateConfig");
               }
            }
         }

         if (this.applyMoneyManagement && var4.getChild("RiskMoneyManagement") != null && var1.getChild("RiskMoneyManagement") != null) {
            var1.removeChild("RiskMoneyManagement");
            var1.addContent(var4.getChild("RiskMoneyManagement").clone());
         }

         if (this.applyCrossChecks && var4.getChild("CrossChecks") != null && var1.getChild("CrossChecks") != null) {
            var1.removeChild("CrossChecks");
            var1.addContent(var4.getChild("CrossChecks").clone());
         }

         if (this.whatToBuild) {
            if (var4.getChild("WhatToBuild") != null && var1.getChild("WhatToBuild") != null) {
               Element var15 = XMLUtil.getChildElem(var4.getChild("WhatToBuild"), "BuildMode").clone();
               Element var19 = XMLUtil.getChildElem(var1.getChild("WhatToBuild"), "BuildMode").clone();
               var1.removeChild("WhatToBuild");
               var1.addContent(var4.getChild("WhatToBuild").clone());
               if (!this.geneticOptions) {
                  var1.getChild("WhatToBuild").removeChild("BuildMode");
                  var19.setAttribute("generationType", var15.getAttributeValue("generationType"));
                  var1.getChild("WhatToBuild").addContent(var19);
               }
            }

            if (var4.getChild("Databanks") != null && var1.getChild("Databanks") != null && "Build".equals(var2)) {
               List var16 = var1.getChild("Databanks").getChildren();
               List var20 = var4.getChild("Databanks").getChildren();

               for (int var24 = 0; var24 < var20.size(); var24++) {
                  if (((Element)var20.get(var24)).getAttributeValue("name").equals("Output")) {
                     for (int var29 = 0; var29 < var16.size(); var29++) {
                        if (((Element)var16.get(var29)).getAttributeValue("name").equals("Output")) {
                           ((Element)var1.getChild("Databanks").getChildren().get(var29))
                              .setAttribute("value", ((Element)var20.get(var24)).getAttributeValue("value"));
                        }
                     }
                  }
               }
            }
         }

         if (this.whatToRetest && var4.getChild("Databanks") != null && var1.getChild("Databanks") != null && "Retest".equals(var2)) {
            var1.removeChild("Databanks");
            var1.addContent(var4.getChild("Databanks").clone());
         }

         if (this.partsToImprove && var4.getChild("PartsToImprove") != null && var1.getChild("PartsToImprove") != null) {
            var1.removeChild("PartsToImprove");
            var1.addContent(var4.getChild("PartsToImprove").clone());
         }

         if (this.optimization && var4.getChild("Optimization") != null && var1.getChild("Optimization") != null) {
            var1.removeChild("Optimization");
            var1.addContent(var4.getChild("Optimization").clone());
         }

         if (this.ranking && var4.getChild("Rankings") != null && var1.getChild("Rankings") != null) {
            var1.removeChild("Rankings");
            var1.addContent(var4.getChild("Rankings").clone());
         }
      } catch (Exception var12) {
         Log.error(String.format("Failed to apply changes to task %s.", var3), var12);
         throw new Exception(L.t("Failed to apply changes to task %s.", new Object[]{var3}) + " " + var12.getMessage(), var12);
      }
   }

   public void applyChangesToTasks(SQProject var1, ProgressEngine var2) throws Exception {
      if (!this.loadFromType.equals("file")) {
         this.elSourceTask = var1.getTaskSettingsByName(this.sourceTaskName);
      }

      if (this.elSourceTask == null) {
         throw new Exception(L.t("Source task is null.", new Object[0]));
      }

      for (int var3 = 0; var3 < this.targetTasks.size(); var3++) {
         String var4 = this.targetTasks.get(var3);
         if (var1.containsTaskByName(var4)) {
            ISQTask var5 = var1.getTaskByName(var4);
            Element var6 = var5.getConfig();
            String var7 = var5.getTitle();
            Log.info(L.t("Applying config to task '%s'", new Object[]{var7}));
            if (var2 != null) {
               var2.printToLog(L.t("Applying config to task '%s'", new Object[]{var7}));
            }

            this.applyChangesToTask(var6, var5.getType(), var7);
            var5 = var1.getTaskByName(var4);
            var5.setConfig(var6, false, true);
         }
      }

      var1.saveConfig();
      if (!MainApp.runInConsoleWithoutActiveWebserver()) {
         this.updateProjecOnFrontend(var1, var2 == null);
      }
   }

   private void updateProjecOnFrontend(SQProject var1, boolean var2) {
      JSONObject var3 = new JSONObject().put("projectConfig", var1.toJSON());
      DataToSend var4 = new DataToSend("UpdateProject", var3);
      SQWebSocketManager.addToDataQueue(var4, "TASKMANAGER");
   }
}
