package com.strategyquant.tradinglib.project;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.broker.BrokerDto;
import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.datalib.data.DataCloner;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.instrument.InstrumentManager;
import com.strategyquant.datalib.session.Session;
import com.strategyquant.datalib.session.SessionElement;
import com.strategyquant.datalib.session.SessionManager;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.client.IGridMessageListener;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.utils.IProgressListener;
import com.strategyquant.tradinglib.data.download.DownloadDispatcher;
import com.strategyquant.tradinglib.file.SQFileManager;
import com.strategyquant.tradinglib.generator.StrategyTemplateGenerator;
import com.strategyquant.tradinglib.generator.StrategyTemplateNotFoundException;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.locks.StampedLock;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectResources {
   private static final Logger Log = LoggerFactory.getLogger(ProjectResources.class);
   public static final int StatusDoesntExist = 0;
   public static final int StatusExistsButDiffers = 1;
   public static final int StatusExistsButNotEnoughData = 2;
   public static final int StatusExistsWithDifferentName = 3;
   public static final int ActionTypeLoadProject = 0;
   public static final int ActionTypeLoadTaskConfig = 1;
   public static final int ActionTypeApplyStrategyConfig = 2;
   public static final int ActionTypeLoadTemplate = 3;
   public static final int ActionTypeLoadStrategyToOptimize = 4;
   public static final int ActionTypeResolveProjectResources = 5;
   public static final String ResourcesElementName = "Resources";
   public static final String SymbolsElementName = "Symbols";
   public static final String BrokersElementName = "Brokers";
   public static final String AllBrokersElementName = "AllBrokers";
   public static final String InstrumentsElementName = "Instruments";
   public static final String SessionsElementName = "Sessions";
   public static final String CustomIndicatorsElementName = "CustomIndicators";
   private static final ArrayList<String> symbolsToDownload = new ArrayList<>();
   private static final ArrayList<ProjectResources.CloneInfo> symbolsToClone = new ArrayList<>();
   private static final DataCloner dataCloner = new DataCloner();
   private static final DataToSend statusData = new DataToSend("ProjectImport", new JSONObject());
   private static final IGridMessageListener listener = new IGridMessageListener() {
      public void messageReceived(GridMessage var1) {
         if (var1.getMessageID() == 1) {
            String var2 = var1.getJobDetails().getJobID();
            String var3 = var2.substring("downloadjob_".length());
            long var4 = ProjectResources.lock.writeLock();

            try {
               ProjectResources.symbolsToDownload.remove(var3);
               if (ProjectResources.symbolsToDownload.isEmpty()) {
                  ProjectResources.cloneSymbols();
               }
            } finally {
               ProjectResources.lock.unlock(var4);
            }
         }
      }
   };
   private static final StampedLock lock = new StampedLock();

   public static void addResources(SQProject var0) throws Exception {
      for (int var1 = 0; var1 < var0.getTasks().size(); var1++) {
         ISQTask var2 = var0.getTask(var1);

         try {
            addResources(var2);
         } catch (Exception var4) {
            Log.error("Adding resources failed", var4);
         }
      }
   }

   public static void addResources(ISQTask var0) throws Exception {
      addDataResources(var0.getConfig());
      if (var0.getType().equals("Build")) {
         try {
            addTemplateResources(var0);
         } catch (Exception var2) {
            Log.error("Adding template resource failed", var2);
         }
      }

      ProjectCustomResources.addProjectResources(null, var0.getConfig());
   }

   public static void addResources(Element var0) throws Exception {
      addDataResources(var0);
      ProjectCustomResources.addProjectResources(null, var0);
   }

   private static void addTemplateResources(ISQTask var0) throws Exception {
      Element var1 = var0.getConfig();
      String var2 = var0.getCustomName();
      Element var3 = StrategyTemplateGenerator.getConfiguredTemplate(var1, var2);
      if (var3 != null) {
         Element var4 = var1.getChild("Resources");
         if (var4 == null) {
            var4 = new Element("Resources");
            var1.addContent(var4);
         }

         Element var5 = var4.getChild("BackupStrategyTemplate");
         if (var5 == null) {
            var5 = new Element("BackupStrategyTemplate");
            var4.addContent(var5);
         } else {
            var5.removeContent();
         }

         var5.addContent(var3.clone());
      }
   }

   public static void checkBuildTemplates(SQProject var0) throws Exception {
      for (int var1 = 0; var1 < var0.getTasks().size(); var1++) {
         boolean var2 = false;
         ISQTask var3 = var0.getTask(var1);
         Element var4 = var3.getConfig();

         try {
            if (var3.getType() != null && var3.getType().equals("Build")) {
               Element var5 = var4.getChild("WhatToBuild");
               if (var5 != null) {
                  Element var6 = var5.getChild("StrategyType");
                  if (var6 != null) {
                     String var7 = XMLUtil.getStringAttr(var6, "type", null);
                     if (var7 != null && var7.equals("template")) {
                        var2 = true;

                        try {
                           StrategyTemplateGenerator.getConfiguredTemplate(var4, var3.getCustomName());
                        } catch (StrategyTemplateNotFoundException var16) {
                           String var9 = var16.getTemplateFile();
                           String var10 = var16.getTaskName();
                           resolveBuildTemplate(var0, var10, var9, var4);
                        }
                     }
                  }
               }
            }
         } catch (Exception var17) {
            Log.error("Checking template build task failed", var17);
         } finally {
            if (!var2) {
               Element var12 = var4.getChild("Resources");
               if (var12 != null) {
                  var12.removeChild("BackupStrategyTemplate");
               }
            }
         }
      }
   }

   private static void resolveBuildTemplate(SQProject var0, String var1, String var2, Element var3) throws Exception {
      if (var2 == null) {
         throw new Exception(L.t("Template file of build task '%s' not specified", new Object[]{var1}));
      }

      Element var4 = var3.getChild("Resources");
      if (var4 == null) {
         throw new Exception(L.t("Template file '%s' of build task '%s' not found", new Object[]{var2, var1}));
      }

      Element var5 = var4.getChild("BackupStrategyTemplate");
      if (var5 == null) {
         throw new Exception(L.t("Template file '%s' of build task '%s' not found", new Object[]{var2, var1}));
      }

      String var6 = "Build task '"
         + var1
         + "' uses template<br>'"
         + var2
         + "',<br> but the template doesn't exist in this location.<br><br>Do you want to create it from backup? (Template will be stored in user/settings/StrategyTemplates)";
      var0.userConfirm.awaitConfirmation("Strategy template not found", var6);
      if (var0.userConfirm.isConfirmed()) {
         Element var7 = XMLUtil.getChildElem(var5, "StrategyFile");
         String var8 = SQUtils.stripExtension(new File(var2).getName()) + ".sqw";
         File var9 = new File(SQPaths.strategyTemplatesDirPath + "/" + var8);
         DataToSend var10 = new DataToSend();
         var10.setName("TemplatePathChange");
         Element var11 = ProjectConfigHelper.getTaskConfigByName(var0.getConfig(), var1);
         String var12 = var11.getAttributeValue("taskXMLFile");
         var10.setDataObject(new JSONObject().put("taskName", var1).put("taskXMLFile", var12).put("templateFile", var8));
         SQWebSocketManager.addToDataQueue(var10, SQWebSocketManager.getProductCode(var0.getName()));
         if (var9.exists()) {
            String var13 = var9.getAbsolutePath().replace("\\", "/").replace(MainApp.getDataPath(), "");
            var0.userConfirm
               .awaitConfirmation(
                  "Strategy build template already exists",
                  "Strategy template<br>'" + var13 + "'<br>already exists in this location.<br><br>Do you want to overwrite it?"
               );
            if (var0.userConfirm.isConfirmed()) {
               XMLUtil.xmlToFile(var7, var9);
            }
         } else {
            XMLUtil.xmlToFile(var7, var9);
         }

         Element var15 = XMLUtil.getChildElem(var3, "WhatToBuild");
         Element var14 = XMLUtil.getChildElem(var15, "StrategyType");
         var14.setAttribute("templateFile", var8);
         var0.saveConfig();
      } else {
         throw new TaskStoppedException();
      }
   }

   public static void addDataResources(Element var0) {
      var0.removeChildren("Resources");
      ArrayList var1 = XMLUtil.getNestedElements(var0, "Setup");
      Element var2 = new Element("Resources");
      Element var3 = new Element("Symbols");
      Element var4 = new Element("Sessions");
      Element var5 = new Element("Brokers");
      Element var6 = new Element("Instruments");
      ArrayList var7 = new ArrayList();
      ArrayList var8 = new ArrayList();
      HashMap var9 = new HashMap();
      HashMap var10 = new HashMap();
      HashSet var11 = new HashSet();
      HashSet var12 = new HashSet();
      String var13 = null;

      for (int var14 = 0; var14 < var1.size(); var14++) {
         Element var15 = (Element)var1.get(var14);
         if (var15.getParentElement().getParentElement().getName().equals("Data")) {
            var13 = var15.getAttributeValue("engine");
            break;
         }
      }

      for (int var36 = 0; var36 < var1.size(); var36++) {
         Element var38 = (Element)var1.get(var36);
         if (!var38.getParentElement().getParentElement().getName().equals("CustomData")
            && (
               !"Stockpicker".equals(var13) && !"Single-asset cloud strategy".equals(var13)
                  || !var38.getParentElement().getParentElement().getParentElement().getName().equals("RetestOnAdditionalMarkets")
            )) {
            String var16 = var38.getAttributeValue("session");
            if (checkIfSaveSession(var16, var7)) {
               try {
                  var4.addContent(createSessionElement(var16));
                  var7.add(var16);
               } catch (Exception var35) {
                  Log.error("Error while adding session to resources - " + var35.getMessage());
               }
            }

            List var17 = var38.getChildren("Chart");
            long var18 = 0L;

            try {
               var18 = SQTime.parseDateToMilis(var38.getAttributeValue("dateFrom"));
            } catch (Exception var34) {
            }

            long var20 = 0L;

            try {
               var20 = SQTime.parseDateToMilis(var38.getAttributeValue("dateTo"));
            } catch (Exception var33) {
            }

            for (int var22 = 0; var22 < var17.size(); var22++) {
               Element var23 = (Element)var17.get(var22);
               String var24 = var23.getAttributeValue("symbol");
               var18 = checkDate(var9, var24, var18, true);
               var20 = checkDate(var10, var24, var20, false);
               if (var24 != null && !var24.equals("Same as main chart")) {
                  DataInfo var25 = DataManager.getDataInfo("History", var24);
                  if (var25 == null) {
                     Log.error(String.format("Error while adding symbol to resources - Symbol '%s' doesn't exist", var24));
                  } else {
                     String var26 = null;
                     String var27 = null;
                     String var28 = null;
                     String var29 = null;
                     if (var25.sourceDataId > 0) {
                        DataInfo var30 = DataManager.getDataInfo("History", var25.sourceDataId);
                        if (var30 == null) {
                           Log.error(String.format("Error while adding symbol to resources - Source symbol with id '%d' doesn't exist", var25.sourceDataId));
                           continue;
                        }

                        var26 = var30.symbol;
                        var27 = var30.timezone;
                        var28 = var30.uSymbol;
                        var29 = var30.uSymbolName;
                     }

                     if (!var8.contains(var24)) {
                        try {
                           Element var44 = createSymbolElement(var25, var18, var20);
                           if (var26 != null) {
                              var44.setAttribute("cloneFrom", var26);
                              var44.setAttribute("sourceTimezone", var27);
                              if (var28 != null) {
                                 var44.setAttribute("uSymbol", var28);
                              }

                              if (var29 != null) {
                                 var44.setAttribute("uSymbolName", var29);
                              }
                           }

                           if (var25.symbolInfo.broker != -1) {
                              var12.add(var25.symbolInfo);
                              var11.add(var25.symbolInfo.broker);
                           }

                           if (var25.brokerId != -1) {
                              var11.add(var25.brokerId);
                           }

                           var3.addContent(var44);
                           var8.add(var24);
                        } catch (Exception var32) {
                           Log.error("Error while adding symbol to resources", var32);
                        }
                     }
                  }
               }
            }
         }
      }

      String var37 = getMarketOpenSession(var0);
      if (checkIfSaveSession(var37, var7)) {
         try {
            var4.addContent(createSessionElement(var37));
            var7.add(var37);
         } catch (Exception var31) {
            Log.error("Error while adding session (MarketOpenSession) to resources - " + var31.getMessage());
         }
      }

      for (int var41 : var11) {
         BrokerDto var43 = BrokerManager.getInstance().getBroker(var41);
         if (var43 == null) {
            Log.warn("Broker not found for id: {}", var41);
         } else {
            var5.addContent(createBrokerElement(var43));
         }
      }

      for (InstrumentInfo var42 : var12) {
         var6.addContent(var42.getXML());
      }

      var2.addContent(var3);
      var2.addContent(var5);
      var2.addContent(var6);
      var2.addContent(var4);
      var0.addContent(var2);
   }

   private static boolean checkIfSaveSession(String var0, List<String> var1) {
      return var0 != null
         && !var0.equals("No Session")
         && !var0.equals("Forex_245")
         && !var0.equals("Forex_247")
         && !var1.contains(var0)
         && !var0.equals("null");
   }

   public static String getSession(Element var0) {
      Element var1 = var0.getChild("Options");
      if (var1 == null) {
         return null;
      }

      Element var2 = var1.getChild("BuildTradingOptions");
      if (var2 == null) {
         return null;
      }

      Element var3 = var2.getChild("Params");
      if (var3 == null) {
         return null;
      }

      List var4 = var3.getChildren("Param");

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);
         String var7 = var6.getAttributeValue("key");
         if (var7.equals("Session")) {
            return var6.getValue();
         }
      }

      return null;
   }

   public static String getMarketOpenSession(Element var0) {
      Element var1 = var0.getChild("Options");
      if (var1 == null) {
         return null;
      }

      Element var2 = var1.getChild("BuildTradingOptions");
      if (var2 == null) {
         return null;
      }

      Element var3 = var2.getChild("Params");
      if (var3 == null) {
         return null;
      }

      List var4 = var3.getChildren("Param");

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);
         String var7 = var6.getAttributeValue("key");
         if (var7.equals("MarketOpenSession")) {
            return var6.getValue();
         }
      }

      return null;
   }

   public static Element createBrokerElement(BrokerDto var0) {
      Element var1 = new Element("Broker");
      var1.setAttribute("id", String.valueOf(var0.getId()));
      var1.setAttribute("name", var0.getName());
      var1.setAttribute("description", var0.getDesc());
      var1.setAttribute("timezone", var0.getMtTimezone());
      var1.setAttribute("postfix", var0.getPostfix());
      var1.setAttribute("mtUse", String.valueOf(var0.isMtUse()));
      var1.setAttribute("spUse", String.valueOf(var0.isStockPickerUse()));
      return var1;
   }

   private static long checkDate(HashMap<String, Long> var0, String var1, long var2, boolean var4) {
      if (var2 != 0L && var0.containsKey(var1)) {
         long var5 = (Long)var0.get(var1);
         if ((!var4 || var2 >= var5) && (var4 || var2 <= var5)) {
            return var5;
         }

         var0.put(var1, var2);
      }

      return var2;
   }

   public static Element checkProjectResources(File var0, Element var1) throws Exception {
      if (SQProject.projectIsOlderThan136(var1)) {
         return checkTaskResources(var1);
      }

      ArrayList var2 = new ArrayList();
      HashMap var3 = SQFileManager.loadTaskConfigs(var0);

      for (String var5 : var3.keySet()) {
         Element var6 = (Element)var3.get(var5);
         var2.add(var6);
      }

      Element var18 = null;
      Element var19 = null;
      Element var20 = null;
      ArrayList var7 = new ArrayList();
      ArrayList var8 = new ArrayList();

      for (int var9 = 0; var9 < var2.size(); var9++) {
         Element var10 = (Element)var2.get(var9);
         Element var11 = checkTaskResources(var10);
         if (var11 != null) {
            if (var18 == null) {
               var18 = new Element("Resources");
            }

            Element var12 = var11.getChild("Symbols");
            if (var12 != null) {
               if (var19 == null) {
                  var19 = new Element("Symbols");
                  var18.addContent(var19);
               }

               List var13 = var12.getChildren("Symbol");

               for (int var14 = var13.size() - 1; var14 >= 0; var14--) {
                  Element var15 = (Element)var13.get(var14);
                  String var16 = var15.getAttributeValue("name");
                  if (!var7.contains(var16)) {
                     var19.addContent(var15.clone());
                     var7.add(var16);
                  }
               }
            }

            Element var21 = var11.getChild("Sessions");
            if (var21 != null) {
               if (var20 == null) {
                  var20 = new Element("Sessions");
                  var18.addContent(var20);
               }

               List var22 = var21.getChildren("Session");

               for (int var23 = var22.size() - 1; var23 >= 0; var23--) {
                  Element var24 = (Element)var22.get(var23);
                  String var17 = var24.getAttributeValue("name");
                  if (!var8.contains(var17)) {
                     var20.addContent(var24.clone());
                     var8.add(var17);
                  }
               }
            }
         }
      }

      return var18;
   }

   public static Element checkTaskResources(Element var0) throws Exception {
      return checkTaskResources(var0, false);
   }

   public static Element checkTaskResources(Element var0, boolean var1) throws Exception {
      if (var0.getName().equals("Task") && var0.getChild("Settings") != null) {
         var0 = var0.getChild("Settings");
      }

      boolean var2 = true;
      boolean var3 = false;

      try {
         ArrayList var4 = XMLUtil.getNestedElements(var0, "Chart");

         for (int var5 = 0; var5 < var4.size(); var5++) {
            Element var6 = (Element)var4.get(var5);
            String var7 = var6.getAttributeValue("timeframe");
            if (var7 != null && !TimeframeManager.TFExists(var7)) {
               TimeframeManager.addTimeframe(var7);
               var3 = true;
            }
         }
      } catch (Exception var37) {
         Log.error("Error while checking missing timeframes", var37);
      }

      if (var3) {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getTimeframes(), "SQUANT", "QDM", "AlgoWizard");
      }

      Element var38 = var0.getChild("Resources");
      if (var38 != null && !var38.getChildren().isEmpty() && var38.getChild("Symbols") != null && !var38.getChild("Symbols").getChildren().isEmpty()) {
         List var41 = new ArrayList();
         List var44 = new ArrayList();
         if (var1) {
            var41 = extractAWSymbols(var0);
            var44 = findInstrumentsBySymbols(var38, var41);
         }

         try {
            Element var46 = var38.getChild("Symbols");
            Element var8 = var38.getChild("Sessions");
            Element var47 = var38.getChild("Instruments");
            Element var10 = var38.getChild("Brokers");
            Element var48 = var38.getChild("CustomIndicators");
            if (var46 != null) {
               List var49 = var46.getChildren("Symbol");

               for (int var54 = var49.size() - 1; var54 >= 0; var54--) {
                  String var59 = "NA";

                  try {
                     Element var64 = (Element)var49.get(var54);
                     Element var69 = var64.getChild("InstrumentInfo");
                     InstrumentInfo var73 = null;
                     if (var69 != null) {
                        var73 = new InstrumentInfo(var69);
                     }

                     var59 = XMLUtil.getAttr(var64, "name");
                     int var75 = XMLUtil.getIntAttr(var64, "source");
                     int var77 = XMLUtil.getIntAttr(var64, "barType");
                     String var78 = XMLUtil.getAttr(var64, "precision");
                     String var79 = XMLUtil.getAttr(var64, "timezone");
                     long var80 = XMLUtil.getLongAttr(var64, "dateFrom");
                     long var24 = XMLUtil.getLongAttr(var64, "dateTo");
                     DataInfo var83 = DataManager.getDataInfo("History", var59);
                     if (var83 == null) {
                        var64.setAttribute("status", String.valueOf(0));
                        var2 = false;
                     } else {
                        long var86 = SQTime.correctDayStart(var83.dateFrom);
                        long var88 = SQTime.correctDayEnd(var83.dateTo);
                        if (var83.source != var75
                           || var83.barTimeType != var77
                           || var83.timeframe == null
                           || !var83.timeframe.equals(var78)
                           || !checkTimeZone(var83.timezone, var79)
                           || !compareInstruments(var83.symbolInfo, var73)) {
                           var64.setAttribute("status", String.valueOf(1));
                           var2 = false;
                        } else if (var86 <= var80 && var88 >= var24) {
                           var46.removeContent(var64);
                        } else {
                           var64.setAttribute("status", String.valueOf(2));
                           var64.setAttribute("missingDays", "" + getMissingDays(var86, var88, var80, var24));
                           var2 = false;
                        }
                     }

                     if (var1 && !var41.contains(var59)) {
                        var46.removeContent(var64);
                     }
                  } catch (Exception var34) {
                     Log.error(String.format("Cannot check symbol '%s'", var59), var34);
                  }
               }
            }

            if (var8 != null) {
               List var50 = var8.getChildren("Session");

               for (int var55 = var50.size() - 1; var55 >= 0; var55--) {
                  Element var60 = (Element)var50.get(var55);
                  String var65 = var60.getAttributeValue("name");

                  try {
                     Session var70 = SessionManager.getSession(var65);
                     if (var70 == null) {
                        var60.setAttribute("status", String.valueOf(0));
                        var2 = false;
                     } else if (!XMLUtil.elementToString(createSessionElement(var65)).equals(XMLUtil.elementToString(var60))) {
                        var60.setAttribute("status", String.valueOf(1));
                        var2 = false;
                     } else {
                        var8.removeContent(var60);
                     }
                  } catch (Exception var33) {
                     Log.error("Cannot check session", var33);
                  }
               }
            }

            if (var10 != null) {
               List var51 = var10.getChildren("Broker");
               Element var56 = new Element("AllBrokers");

               for (int var61 = var51.size() - 1; var61 >= 0; var61--) {
                  Element var66 = (Element)var51.get(var61);
                  String var71 = var66.getAttributeValue("name");
                  String var74 = var66.getAttributeValue("timezone");
                  var56.addContent(var66.clone());

                  try {
                     BrokerDto var76 = BrokerManager.getInstance().getBroker(var71);
                     if (var76 == null) {
                        var66.setAttribute("status", String.valueOf(0));
                        var2 = false;
                     } else if (!var76.getMtTimezone().equals(var74)) {
                        var66.setAttribute("status", String.valueOf(1));
                        var2 = false;
                     } else {
                        var10.removeContent(var66);
                     }
                  } catch (Exception var32) {
                     Log.error("Cannot check broker", var32);
                  }
               }

               var38.addContent(var56);
            }

            if (var47 != null) {
               List var52 = var47.getChildren("InstrumentInfo");

               for (int var57 = var52.size() - 1; var57 >= 0; var57--) {
                  Element var62 = (Element)var52.get(var57);
                  String var67 = var62.getAttributeValue("instrument");

                  try {
                     if (!InstrumentManager.checkInstrumentExists(var67)) {
                        var62.setAttribute("status", String.valueOf(0));
                        var2 = false;
                     } else {
                        InstrumentInfo var72 = InstrumentManager.getInstrumentInfo(var67);
                        if (!XMLUtil.elementToString(var72.getXML()).equals(XMLUtil.elementToString(var62))) {
                           var62.setAttribute("status", String.valueOf(1));
                           var2 = false;
                        } else {
                           var47.removeContent(var62);
                        }
                     }

                     if (var1 && !var44.contains(var67)) {
                        var47.removeContent(var62);
                     }
                  } catch (Exception var31) {
                     Log.error("Cannot check instrument", var31);
                  }
               }
            }

            if (var1 && var48 != null) {
               List var53 = var48.getChildren("CustomIndicator");

               for (int var58 = var53.size() - 1; var58 >= 0; var58--) {
                  Element var63 = (Element)var53.get(var58);
                  String var68 = var63.getText();
                  if (CustomDataManager.checkDataExists(var68)) {
                     var48.removeContent(var63);
                  } else {
                     var63.setAttribute("status", "0");
                     var2 = false;
                  }
               }
            }
         } catch (Exception var35) {
            Log.error("Cannot check resources from XML config", var35);
            throw new Exception("Cannot check XML config resources - " + var35.getMessage());
         }
      } else {
         String var39 = null;
         String var42 = null;
         String var11 = null;
         String var12 = null;
         var38 = new Element("Resources");
         Element var13 = new Element("Symbols");
         var38.addContent(var13);

         try {
            Element var14 = var0.getChild("Data");
            if (var14 != null) {
               Element var15 = var14.getChild("Setups");
               List var16 = var15.getChildren("Setup");

               for (int var17 = 0; var17 < var16.size(); var17++) {
                  Element var18 = (Element)var16.get(var17);
                  long var45 = XMLUtil.parseDate(var18.getAttributeValue("dateFrom"));
                  long var9 = XMLUtil.parseDate(var18.getAttributeValue("dateTo"));
                  List var19 = var18.getChildren("Chart");

                  for (int var20 = 0; var20 < var19.size(); var20++) {
                     if (var20 == 0) {
                        var11 = var39 = ((Element)var19.get(var20)).getAttributeValue("symbol");
                        var12 = var42 = ((Element)var19.get(var20)).getAttributeValue("timeframe");
                     } else {
                        var39 = ((Element)var19.get(var20)).getAttributeValue("symbol");
                        var42 = ((Element)var19.get(var20)).getAttributeValue("timeframe");
                        if (var39.equals("Same as main chart")) {
                           var39 = var11;
                        }

                        if (var42.equals("Same as main data")) {
                           var42 = var12;
                        }
                     }

                     Element var21 = null;
                     DataInfo var22 = DataManager.getDataInfo("History", var39);
                     if (var22 == null) {
                        var21 = new Element("Symbol");
                        var21.setAttribute("name", var39);
                        var21.setAttribute("precision", var42);
                        var21.setAttribute("status", String.valueOf(0));
                     } else {
                        long var23 = SQTime.correctDayStart(var22.dateFrom);
                        long var25 = SQTime.correctDayEnd(var22.dateTo);
                        long var27 = TimeframeManager.getMillis(var42);
                        long var29 = TimeframeManager.getMillis(var22.timeframe);
                        if (var29 > var27) {
                           var21 = new Element("Symbol");
                           var21.setAttribute("name", var39);
                           var21.setAttribute("precision", var42);
                           var21.setAttribute("status", String.valueOf(1));
                        } else if (var23 > var45 || var25 < var9) {
                           var21 = new Element("Symbol");
                           var21.setAttribute("name", var39);
                           var21.setAttribute("status", String.valueOf(2));
                           var21.setAttribute("missingDays", "" + getMissingDays(var23, var25, var45, var9));
                        }
                     }

                     if (var21 != null) {
                        long var81 = 0L;
                        if (var42 != null && !var42.isEmpty()) {
                           var81 = TimeframeManager.getMillis(var42);
                        }

                        Element var82 = null;

                        for (Element var28 : var13.getChildren("Symbol")) {
                           if (var39.equals(var28.getAttributeValue("name"))) {
                              var82 = var28;
                              break;
                           }
                        }

                        if (var82 == null) {
                           var13.addContent(var21);
                           var2 = false;
                        } else {
                           String var85 = var82.getAttributeValue("precision");
                           if (var85 == null || var85.isEmpty()) {
                              var85 = var42;
                           }

                           long var87 = 0L;
                           if (var85 != null && !var85.isEmpty()) {
                              var87 = TimeframeManager.getMillis(var85);
                           }

                           if (var81 > 0L && (var87 == 0L || var81 < var87)) {
                              String var30 = var21.getAttributeValue("precision");
                              if (var30 != null) {
                                 var82.setAttribute("precision", var30);
                              }

                              var2 = false;
                           }
                        }
                     }
                  }
               }
            }
         } catch (Exception var36) {
            Log.error("Cannot check resources from XML config", var36);
         }
      }

      return var2 ? null : var38;
   }

   private static List<String> extractAWSymbols(Element var0) {
      ArrayList var1 = new ArrayList();
      Element var2 = var0.getChild("Data");
      if (var2 != null) {
         Element var3 = var2.getChild("Setups");
         List var4 = var3.getChildren("Setup");

         for (int var5 = 0; var5 < var4.size(); var5++) {
            Element var6 = (Element)var4.get(var5);
            List var7 = var6.getChildren("Chart");

            for (int var8 = 0; var8 < var7.size(); var8++) {
               String var9 = ((Element)var7.get(var8)).getAttributeValue("symbol");
               var1.add(var9);
            }
         }
      }

      return var1;
   }

   private static List<String> findInstrumentsBySymbols(Element var0, List<String> var1) {
      ArrayList var2 = new ArrayList();
      if (var0 == null) {
         return var2;
      }

      Element var3 = var0.getChild("Symbols");
      if (var3 == null) {
         return var2;
      }

      List var4 = var3.getChildren("Symbol");

      for (String var6 : var1) {
         for (Element var8 : var4) {
            if (var6.equals(var8.getAttributeValue("name"))) {
               Element var9 = var8.getChild("InstrumentInfo");
               if (var9 != null) {
                  String var10 = var9.getAttributeValue("instrument");
                  if (var10 != null && !var10.isEmpty()) {
                     var2.add(var10);
                  }
               }
               break;
            }
         }
      }

      return var2;
   }

   private static boolean checkTimeZone(String var0, String var1) {
      if (var0 != null && var1 != null) {
         boolean var2 = var0.equals("Etc/UCT") || var0.startsWith("(UTC)");
         boolean var3 = var1.equals("Etc/UCT") || var1.startsWith("(UTC)");
         if ((!var2 || !var3) && !var0.equals(var1)) {
            int var4 = var2 ? 0 : TimeZone.getTimeZone(var0).getRawOffset();
            int var5 = var3 ? 0 : TimeZone.getTimeZone(var1).getRawOffset();
            return var4 == var5;
         } else {
            return true;
         }
      } else {
         return true;
      }
   }

   private static int getMissingDays(long var0, long var2, long var4, long var6) {
      long var8 = 0L;
      if (var0 != 0L && var2 != 0L) {
         if (var0 > var4) {
            var8 += var0 - var4;
         }

         if (var2 < var6) {
            var8 += var6 - var2;
         }
      } else {
         var8 = var6 - var4;
      }

      return (int)(var8 / 86400000L);
   }

   public static boolean resolveResources(Element var0, Element var1, HashMap<String, Element> var2, JSONArray var3, Element var4) throws Exception {
      try {
         Element var5 = var0.getChild("Symbols");
         Element var6 = var0.getChild("Sessions");
         Element var7 = var0.getChild("Instruments");
         Element var8 = var0.getChild("Brokers");
         Element var9 = var0.getChild("AllBrokers");
         ArrayList var10 = new ArrayList();
         if (var5 != null) {
            var10.addAll(var5.getChildren("Symbol"));
         }

         ArrayList var11 = new ArrayList();
         var11.add(var1);
         if (var2 != null) {
            for (String var13 : var2.keySet()) {
               var11.add((Element)var2.get(var13));
            }
         }

         String var51 = "";
         String var52 = "";
         String var14 = "";
         String var15 = "";
         ArrayList var16 = new ArrayList();
         if (var9 != null) {
            var16.addAll(var9.getChildren("Broker"));
         }

         HashMap var17 = new HashMap();

         for (int var18 = var16.size() - 1; var18 >= 0; var18--) {
            Element var19 = (Element)var16.get(var18);
            BrokerDto var20 = loadBrokerElement(var19);
            var17.put(var20.getId(), var20);
         }

         ArrayList var53 = new ArrayList();
         if (var8 != null) {
            var53.addAll(var8.getChildren("Broker"));
         }

         boolean var54 = false;

         for (int var55 = var53.size() - 1; var55 >= 0; var55--) {
            Element var21 = (Element)var53.get(var55);
            String var22 = XMLUtil.getAttr(var21, "action", "ignore");
            BrokerDto var23 = loadBrokerElement(var21);
            if (!var22.equals("ignore")) {
               String var24 = var21.getAttributeValue("name");
               if (var22.equals("add")) {
                  String var25 = generateNewBrokerName(var24);
                  Integer var26 = var23.getId();
                  var23.setId(null);
                  var23.setName(var25);
                  BrokerManager.getInstance().saveBroker(var23);
                  var17.put(var26, var23);
                  var54 = true;
               } else {
                  if (!var22.equals("replace")) {
                     throw new Exception(L.t("Unknown broker action '%s'", new Object[]{var22}));
                  }

                  String var63 = var21.getAttributeValue("replaceBroker");
                  BrokerDto var67 = BrokerManager.getInstance().getBroker(var63);
                  if (var23 == null) {
                     throw new Exception(L.t("Replace broker '%s' doesn't exist", new Object[]{var63}));
                  }

                  var17.put(var23.getId(), var67);
               }
            }
         }

         ArrayList var56 = new ArrayList();
         if (var7 != null) {
            var56.addAll(var7.getChildren("InstrumentInfo"));
         }

         boolean var57 = false;
         HashMap var58 = new HashMap();

         for (int var59 = var56.size() - 1; var59 >= 0; var59--) {
            Element var61 = (Element)var56.get(var59);
            String var64 = XMLUtil.getAttr(var61, "action", "ignore");
            if (!var64.equals("ignore")) {
               InstrumentInfo var68 = loadInstrumentElement(var61);
               BrokerDto var27 = (BrokerDto)var17.get(var68.broker);
               BrokerDto var28 = var27 == null ? BrokerManager.getInstance().getDefaultBroker() : BrokerManager.getInstance().getBroker(var27.getName());
               Integer var29 = var28.getId();
               String var30 = var61.getAttributeValue("instrument");
               if (var64.equals("add")) {
                  String var31 = generateNewInstrumentName(var30);
                  var68.instrument = var31;
                  var68.broker = var29;
                  InstrumentManager.addInstrument(var68);
                  var57 = true;
                  var58.put(var30, var31);
               } else {
                  if (!var64.equals("replace")) {
                     throw new Exception(L.t("Unknown instrument action '%s'", new Object[]{var64}));
                  }

                  String var81 = var61.getAttributeValue("replaceInstrument");
                  if (!InstrumentManager.checkInstrumentExists(var81)) {
                     throw new Exception(L.t("Replace instrument '%s' doesn't exist", new Object[]{var81}));
                  }

                  var58.put(var30, var81);
               }
            }
         }

         symbolsToClone.clear();
         boolean var60 = false;
         boolean var62 = true;

         for (int var65 = var10.size() - 1; var65 >= 0; var65--) {
            Element var69 = (Element)var10.get(var65);
            String var71 = XMLUtil.getAttr(var69, "action", "ignore");
            if (!var71.equals("ignore")) {
               String var73 = var69.getAttributeValue("name");
               if (var71.equals("replace")) {
                  String var75 = var69.getAttributeValue("replaceSymbol");
                  if (!DataManager.checkDataExists("History", var75)) {
                     throw new Exception(L.t("Replace symbol '%s' doesn't exist", new Object[]{var75}));
                  }

                  DataInfo var78 = DataManager.getDataInfo("History", var75);
                  replaceSetupSymbols(var11, var73, var78);
                  replaceDatasSymbols(var4, var73, var78);
               } else {
                  Element var76 = XMLUtil.getChildElem(var69, "InstrumentInfo");
                  InstrumentInfo var79 = new InstrumentInfo(var76);
                  if (var79.broker != -1) {
                     String var82 = (String)var58.get(var79.instrument);
                     if (var82 != null && InstrumentManager.checkInstrumentExists(var82)) {
                        var79 = InstrumentManager.getInstrumentInfo(var82);
                     }
                  }

                  int var83 = XMLUtil.getIntAttr(var69, "broker", -1);
                  if (var83 != -1) {
                     BrokerDto var32 = (BrokerDto)var17.get(var83);
                     BrokerDto var33 = var32 == null ? BrokerManager.getInstance().getDefaultBroker() : BrokerManager.getInstance().getBroker(var32.getName());
                     var83 = var33.getId();
                  }

                  int var85 = XMLUtil.getIntAttr(var69, "barType");
                  int var88 = XMLUtil.getIntAttr(var69, "source");
                  String var34 = var69.getAttributeValue("precision");
                  String var35 = var69.getAttributeValue("uSymbol");
                  String var36 = var69.getAttributeValue("uSymbolName");
                  long var37 = XMLUtil.getLongAttr(var69, "dateFrom");
                  long var39 = XMLUtil.getLongAttr(var69, "dateTo");
                  String var41 = var69.getAttributeValue("timezone");
                  String var42 = var69.getAttributeValue("cloneFrom");
                  String var43 = var69.getAttributeValue("sourceTimezone");
                  boolean var44 = XMLUtil.getBooleanAttr(var69, "removeWeekends", false);
                  if (!var71.equals("add")) {
                     if (var71.equals("overwrite")) {
                        String var89 = checkInstrumentExists(var79);
                        DataInfo var90 = DataManager.getDataInfo("History", var73);
                        DataManager.updateData("History", var73, 0L, 0L, 0, 0L, var85, var34, var41, var90.sourceDataId, var44);
                        DataManager.updateInstrument("History", var73, var89);
                        var60 = true;
                        var62 = false;
                     } else {
                        if (!var71.equals("download")) {
                           throw new Exception(L.t("Unknown symbol action '%s'", new Object[]{var71}));
                        }

                        var60 = true;
                        var62 = false;
                     }
                  } else {
                     String var45 = checkInstrumentExists(var79);
                     String var46 = generateNewSymbolName(var73);
                     String var47 = null;
                     if (var42 != null) {
                        if (!DataManager.checkDataExists("History", var42)) {
                           DataManager.addData("History", var42, var45, var85, var88, var35, var36, -1, var83);
                           DataManager.updateData("History", var42, 0L, 0L, 0, 0L, var85, var34, var43);
                           symbolsToClone.add(new ProjectResources.CloneInfo(var46, var42, var41, var44));
                           var47 = var42;
                        } else {
                           DataInfo var48 = DataManager.getDataInfo("History", var42);
                           if (var48.source == var88
                              && var48.barTimeType == var85
                              && var48.timeframe != null
                              && var48.timeframe.equals(var34)
                              && compareInstruments(var48.symbolInfo, var79)) {
                              symbolsToClone.add(new ProjectResources.CloneInfo(var46, var42, var41, var44));
                              var47 = var42;
                           } else {
                              String var49 = generateNewSymbolName(var42);
                              DataManager.addData("History", var49, var45, var85, var88, var35, var36, -1, var83);
                              DataManager.updateData("History", var49, 0L, 0L, 0, 0L, var85, var34, var43);
                              symbolsToClone.add(new ProjectResources.CloneInfo(var46, var49, var41, var44));
                              var47 = var49;
                           }
                        }
                     }

                     var3.put(new JSONObject().put("originalName", var73).put("createdName", var46));
                     if (DataManager.isGroupAlias(var73)) {
                        BasketDto var91 = new BasketDto();
                        var91.setName(var73);
                        var91.setDesc(var69.getAttributeValue("groupDescription"));
                        BasketOfStocksManager.getInstance().saveGroup(var91);
                        SQWebSocketManager.addToDataQueue(WSDataObjects.getBaskets(), "SQUANT", "QDM");
                     } else {
                        DataManager.addData("History", var46, var45, var85, var88, var42 != null ? var47 : var35, var42 != null ? var47 : var36, -1, var83);
                        if (var42 != null) {
                           DataInfo var92 = DataManager.getDataInfo("History", var47);
                           DataManager.updateData("History", var46, 0L, 0L, 0, 0L, var85, var34, var41, var92.id, var44);
                        } else {
                           DataManager.updateData("History", var46, 0L, 0L, 0, 0L, var85, var34, var41);
                        }
                     }

                     if (!var46.equals(var73)) {
                        DataInfo var93 = DataManager.getDataInfo("History", var46);
                        replaceSetupSymbols(var11, var73, var93);
                        replaceDatasSymbols(var4, var73, var93);
                     }

                     var60 = true;
                     var62 = false;
                  }

                  var51 = var51 + (var51.isEmpty() ? "" : ",") + var73;
                  var52 = var52 + (var52.isEmpty() ? "" : ",") + var35;
                  var14 = var14 + (var14.isEmpty() ? "" : ",") + SQTime.formatDate(var37);
                  var15 = var15 + (var15.isEmpty() ? "" : ",") + SQTime.formatDate(var39);
               }
            }
         }

         if (var54) {
            SQWebSocketManager.addToDataQueue(WSDataObjects.getBrokers(), "SQUANT", "QDM");
         }

         if (var60 || var57) {
            SQWebSocketManager.addToDataQueue(WSDataObjects.getData(null, null), "SQUANT", "QDM");
            SQWebSocketManager.addToDataQueue(WSDataObjects.getInstruments(), "SQUANT", "QDM");
         }

         ArrayList var66 = new ArrayList();
         if (var6 != null) {
            var66.addAll(var6.getChildren("Session"));
         }

         boolean var70 = false;

         for (int var72 = var66.size() - 1; var72 >= 0; var72--) {
            Element var74 = (Element)var66.get(var72);
            String var77 = XMLUtil.getAttr(var74, "action", "ignore");
            if (var77 != null && !var77.isEmpty() && !var77.equals("ignore")) {
               String var80 = var74.getAttributeValue("name");
               ArrayList var84 = loadSessionElements(var74);
               if (var77.equals("add")) {
                  String var86 = generateNewSessionName(var80);
                  SessionManager.addSession(new Session(var86, var84));
                  if (!var86.equals(var80)) {
                     replaceSetupSessions(var11, var80, var86);
                  }

                  var70 = true;
               } else if (var77.equals("overwrite")) {
                  SessionManager.updateSession(var80, var84);
                  var70 = true;
               } else {
                  if (!var77.equals("replace")) {
                     throw new Exception(L.t("Unknown session action '%s'", new Object[]{var77}));
                  }

                  String var87 = var74.getAttributeValue("replaceSession");
                  if (!SessionManager.checkSessionExists(var87)) {
                     throw new Exception(L.t("Replace session '%s' doesn't exist", new Object[]{var87}));
                  }

                  replaceSetupSessions(var11, var80, var87);
               }
            }
         }

         cloneSymbols();
         if (var70) {
            SQWebSocketManager.addToDataQueue(WSDataObjects.getSessions(), "SQUANT", "QDM");
         }

         return var62;
      } catch (Exception var50) {
         Log.error("Cannot resolve resources", var50);
         throw new Exception("Cannot resolve resources - " + var50.getMessage());
      }
   }

   public static void cancelImport() {
      DownloadDispatcher.get().removeExtraListener(listener);
      long var0 = lock.readLock();

      try {
         for (int var2 = 0; var2 < symbolsToDownload.size(); var2++) {
            String var3 = symbolsToDownload.get(var2);
            DownloadDispatcher.get().stop(var3);
         }
      } finally {
         lock.unlock(var0);
      }

      dataCloner.cancel();
   }

   public static String checkInstrumentExists(InstrumentInfo var0) throws Exception {
      String var1 = var0.instrument;
      if (!InstrumentManager.checkInstrumentExists(var0.instrument)) {
         InstrumentManager.addInstrument(var0);
      } else {
         InstrumentInfo var2 = InstrumentManager.getInstrumentInfo(var0.instrument);
         if (!compareInstruments(var2, var0)) {
            int var3 = 2;
            var1 = String.format("%s(%d)", var0.instrument, var3++);

            while (InstrumentManager.checkInstrumentExists(var1)) {
               var1 = String.format("%s(%d)", var0.instrument, Integer.valueOf(var3++));
            }

            var0.instrument = var1;
            InstrumentManager.addInstrument(var0);
         }
      }

      return var1;
   }

   private static String generateNewSymbolName(String var0) {
      String var1 = var0;
      int var2 = 2;

      while (DataManager.checkDataExists("History", var1)) {
         var1 = String.format("%s(%d)", var0, var2++);
      }

      return var1;
   }

   private static String generateNewSessionName(String var0) {
      String var1 = var0;
      int var2 = 2;

      while (SessionManager.checkSessionExists(var1)) {
         var1 = String.format("%s(%d)", var0, var2++);
      }

      return var1;
   }

   private static String generateNewBrokerName(String var0) {
      var0 = var0.replace("[", "").replace("]", "");
      String var1 = var0;
      int var2 = 2;

      while (BrokerManager.getInstance().getBroker("[" + var1 + "]") != null) {
         var1 = String.format("%s(%d)", var0, var2++);
      }

      return "[" + var1 + "]";
   }

   private static String generateNewInstrumentName(String var0) {
      String var1 = var0;
      int var2 = 2;

      while (InstrumentManager.checkInstrumentExists(var1)) {
         var1 = String.format("%s(%d)", var0, var2++);
      }

      return var1;
   }

   private static ArrayList<SessionElement> loadSessionElements(Element var0) throws Exception {
      ArrayList var1 = new ArrayList();
      List var2 = var0.getChildren("Element");

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         String var5 = var4.getAttributeValue("dayFrom");
         String var6 = var4.getAttributeValue("dayTo");
         String var7 = var4.getAttributeValue("timeFrom");
         String var8 = var4.getAttributeValue("timeTo");
         boolean var9 = XMLUtil.getBooleanAttr(var4, "eod");
         var1.add(new SessionElement(var5, var7, var6, var8, var9));
      }

      return var1;
   }

   public static BrokerDto loadBrokerElement(Element var0) {
      BrokerDto var1 = new BrokerDto();
      var1.setId(Integer.valueOf(var0.getAttributeValue("id")));
      var1.setName(var0.getAttributeValue("name"));
      var1.setPostfix(var0.getAttributeValue("postfix"));
      var1.setDesc(var0.getAttributeValue("description"));
      var1.setMtTimezone(var0.getAttributeValue("timezone"));
      var1.setMtUse(Boolean.valueOf(var0.getAttributeValue("mtUse")));
      var1.setStockPickerUse(Boolean.valueOf(var0.getAttributeValue("spUse")));
      return var1;
   }

   private static InstrumentInfo loadInstrumentElement(Element var0) {
      return new InstrumentInfo(var0);
   }

   private static Element createSymbolElement(DataInfo var0, long var1, long var3) throws Exception {
      Element var5 = new Element("Symbol");
      var5.setAttribute("name", var0.symbol);
      var5.setAttribute("source", String.valueOf(var0.source));
      var5.setAttribute("barType", String.valueOf(var0.barTimeType));
      var5.setAttribute("precision", var0.timeframe);
      var5.setAttribute("timezone", var0.timezone != null ? var0.timezone : "Etc/UCT");
      var5.setAttribute("dateFrom", String.valueOf(var1 > 0L ? var1 : var0.dateFrom));
      var5.setAttribute("dateTo", String.valueOf(var3 > 0L ? var3 : var0.dateTo));
      var5.setAttribute("uSymbol", var0.uSymbol != null ? var0.uSymbol : "");
      var5.setAttribute("uSymbolName", var0.uSymbolName != null ? var0.uSymbolName : "");
      var5.setAttribute("removeWeekends", String.valueOf(var0.removeWeekends));
      if (DataManager.isGroupAlias(var0.symbol)) {
         String var6 = "";

         for (BasketDto var8 : BasketOfStocksManager.getInstance().getGroups()) {
            if (var8.getName().equals(var0.symbol)) {
               var6 = var8.getDesc() != null ? var8.getDesc() : "";
               break;
            }
         }

         var5.setAttribute("groupDescription", var6);
      }

      var5.setAttribute("broker", String.valueOf(var0.brokerId));
      var5.addContent(var0.symbolInfo.getXML());
      return var5;
   }

   private static Element createSessionElement(String var0) throws Exception {
      Session var1 = SessionManager.getSession(var0);
      if (var1 == null) {
         throw new Exception(String.format("Session '%s' doesn't exist", var0));
      } else {
         Element var2 = var1.getXML();
         if (var2 == null) {
            throw new Exception(String.format("Failed to convert session '%s' to xml.", var0));
         } else {
            return var2;
         }
      }
   }

   private static boolean compareInstruments(InstrumentInfo var0, InstrumentInfo var1) {
      return var0 != null && var1 != null
         ? var0.dataType == var1.dataType
            && var0.pointValue == var1.pointValue
            && var0.tickSize == var1.tickSize
            && var0.tickStep == var1.tickStep
            && var0.tickValueInMoney == var1.tickValueInMoney
         : true;
   }

   private static void replaceDatasSymbols(Element var0, String var1, DataInfo var2) {
      if (var0 != null) {
         try {
            Element var3 = var0.getChild("Strategy").getChild("Datas");
            if (var3 == null) {
               return;
            }

            for (Element var5 : var3.getChildren("data")) {
               Element var6 = var5.getChild("id");
               if (var6 == null || !"0".equals(var6.getText())) {
                  Element var7 = var5.getChild("symbol");
                  if (var7 != null && var1.equals(var7.getText())) {
                     var7.setText(var2.symbol);
                  }
               }
            }
         } catch (Exception var8) {
            Log.error("Updating Datas symbols failed", var8);
         }
      }
   }

   private static void replaceSetupSymbols(ArrayList<Element> var0, String var1, DataInfo var2) throws Exception {
      ArrayList var3 = getSettingsElements(var0);
      ArrayList var4 = new ArrayList();

      for (int var5 = 0; var5 < var3.size(); var5++) {
         Element var6 = (Element)var3.get(var5);
         Element var7 = var6.getChild("Data");
         if (var7 != null) {
            Element var8 = var7.getChild("Setups");
            if (var8 != null) {
               var4.addAll(var8.getChildren("Setup"));
            }
         }
      }

      for (int var21 = 0; var21 < var4.size(); var21++) {
         Element var24 = (Element)var4.get(var21);
         long var27 = XMLUtil.parseDate(var24.getAttributeValue("dateFrom"));
         long var9 = XMLUtil.parseDate(var24.getAttributeValue("dateTo"));
         List var11 = var24.getChildren("Chart");

         for (int var12 = 0; var12 < var11.size(); var12++) {
            Element var13 = (Element)var11.get(var12);
            String var14 = var13.getAttributeValue("symbol");
            if (var14 != null && var14.equals(var1)) {
               var13.setAttribute("symbol", var2.symbol);
               if (var12 == 0) {
                  var13.setAttribute("spread", var2.symbolInfo.defaultSpread + "");
                  var24.setAttribute("slippage", var2.symbolInfo.defaultSlippage + "");

                  try {
                     Element var15 = XMLUtil.stringToElement(var2.symbolInfo.commissions);
                     var24.getChild("Commissions").removeContent();
                     var24.getChild("Commissions").addContent(var15);
                  } catch (Exception var20) {
                     Log.error(L.t("Error while applying default commission.", new Object[0]) + " " + var20.getMessage(), var20);
                  }

                  try {
                     if (var2.symbolInfo.swap != null) {
                        Element var35 = XMLUtil.stringToElement(var2.symbolInfo.swap);
                        var24.removeChild("Swap");
                        var24.addContent(var35);
                     }
                  } catch (Exception var19) {
                     Log.error(L.t("Error while applying default swap.", new Object[0]) + " " + var19.getMessage(), var19);
                  }

                  long var36 = var27;
                  long var17 = var9;
                  if (var36 < var2.dateFrom) {
                     var36 = var2.dateFrom;
                  }

                  if (var17 > var2.dateTo) {
                     var17 = var2.dateTo;
                  }

                  if (var36 > var17) {
                     var36 = var2.dateFrom;
                     var17 = var2.dateTo;
                  }

                  var24.setAttribute("dateFrom", SQTime.formatDate(var36));
                  var24.setAttribute("dateTo", SQTime.formatDate(var17));
               }
            }
         }
      }

      var4.clear();

      for (int var22 = 0; var22 < var3.size(); var22++) {
         Element var25 = (Element)var3.get(var22);
         Element var28 = var25.getChild("CrossChecks");
         if (var28 != null) {
            Element var30 = var28.getChild("RetestOnAdditionalMarkets");
            if (var30 != null) {
               Element var32 = var30.getChild("Settings");
               if (var32 != null) {
                  Element var10 = var32.getChild("Setups");
                  if (var10 != null) {
                     var4.addAll(var10.getChildren("Setup"));
                  }
               }
            }
         }
      }

      for (int var23 = 0; var23 < var4.size(); var23++) {
         Element var26 = (Element)var4.get(var23);
         List var29 = var26.getChildren("Chart");

         for (int var31 = 0; var31 < var29.size(); var31++) {
            Element var33 = (Element)var29.get(var31);
            String var34 = var33.getAttributeValue("symbol");
            if (var34 != null && var34.equals(var1)) {
               var33.setAttribute("symbol", var2.symbol);
            }
         }
      }
   }

   private static void replaceSetupSessions(ArrayList<Element> var0, String var1, String var2) throws Exception {
      ArrayList var3 = getSettingsElements(var0);
      ArrayList var4 = new ArrayList();

      for (int var5 = 0; var5 < var3.size(); var5++) {
         Element var6 = (Element)var3.get(var5);
         Element var7 = var6.getChild("Data");
         if (var7 != null) {
            Element var8 = var7.getChild("Setups");
            if (var8 != null) {
               var4.addAll(var8.getChildren("Setup"));
            }
         }

         Element var20 = var6.getChild("CrossChecks");
         if (var20 != null) {
            Element var9 = var20.getChild("RetestOnAdditionalMarkets");
            if (var9 != null) {
               Element var10 = var9.getChild("Settings");
               if (var10 != null) {
                  Element var11 = var10.getChild("Setups");
                  if (var11 != null) {
                     var4.addAll(var11.getChildren("Setup"));
                  }
               }
            }
         }

         Element var21 = var6.getChild("Options");
         if (var21 != null) {
            Element var22 = var21.getChild("BuildTradingOptions");
            if (var22 != null) {
               Element var23 = var22.getChild("Params");
               if (var23 != null) {
                  List var12 = var23.getChildren("Param");

                  for (int var13 = 0; var13 < var12.size(); var13++) {
                     Element var14 = (Element)var12.get(var13);
                     String var15 = var14.getAttributeValue("key");
                     String var16 = var14.getText();
                     if (var15 != null && var15.equals("Session") && var16 != null && var16.endsWith(var1)) {
                        var14.setText(var2);
                        break;
                     }
                  }
               }
            }
         }
      }

      for (int var17 = 0; var17 < var4.size(); var17++) {
         Element var18 = (Element)var4.get(var17);
         String var19 = var18.getAttributeValue("session");
         if (var19 != null && var19.equals(var1)) {
            var18.setAttribute("session", var2);
         }
      }
   }

   private static ArrayList<Element> getSettingsElements(ArrayList<Element> var0) throws Exception {
      ArrayList var1 = new ArrayList();

      for (int var2 = 0; var2 < var0.size(); var2++) {
         Element var3 = (Element)var0.get(var2);
         String var4 = var3.getName();
         if (var4.equals("Project")) {
            Element var9 = var3.getChild("Tasks");
            if (var9 == null) {
               return var1;
            }

            List var6 = var9.getChildren("Task");

            for (int var7 = 0; var7 < var6.size(); var7++) {
               Element var8 = ((Element)var6.get(var7)).getChild("Settings");
               if (var8 != null) {
                  var1.add(var8);
               }
            }
         } else if (var4.equals("Task")) {
            Element var5 = var3.getChild("Settings");
            if (var5 == null) {
               return var1;
            }

            var1.add(var5);
         } else {
            if (!var4.equals("Settings")) {
               throw new Exception("Invalid XML config. Expected Project/Task/Settings, but got " + var4);
            }

            var1.add(var3);
         }
      }

      return var1;
   }

   private static void cloneSymbols() {
      ArrayList var0 = new ArrayList<>(symbolsToClone);

      for (int var1 = 0; var1 < var0.size(); var1++) {
         final ProjectResources.CloneInfo var2 = (ProjectResources.CloneInfo)var0.get(var1);
         dataCloner.cloneToTimezone(var2.sourceSymbol, var2.symbol, var2.timezone, 0, var2.removeWeekends, null, new IProgressListener() {
            public void onStart() {
            }

            public void onProgress(double var1) {
               System.out.println("Symbol '" + var2.symbol + "' - progress: " + var1);
            }

            public void onPause() {
            }

            public void onContinue() {
            }

            public void onFinish() {
               long var1x = ProjectResources.lock.writeLock();

               try {
                  for (int var3 = 0; var3 < ProjectResources.symbolsToClone.size(); var3++) {
                     if (ProjectResources.symbolsToClone.get(var3).symbol.equals(var2.symbol)) {
                        ProjectResources.symbolsToClone.remove(var3);
                        break;
                     }
                  }

                  if (ProjectResources.symbolsToClone.isEmpty()) {
                     try {
                        ProjectResources.sendInfoMessage("Project import finished");
                     } catch (Exception var8) {
                        ProjectResources.Log.error("Project import failed", var8);
                        ProjectResources.sendErrorMessage("Project import failed - " + var8.getMessage());
                     }
                  }
               } catch (Exception var9) {
                  ProjectResources.Log.error("Project import failed", var9);
                  ProjectResources.sendErrorMessage("Project import failed - " + var9.getMessage());
               } finally {
                  ProjectResources.lock.unlock(var1x);
               }
            }

            public void onError(String var1) {
               String var2x = "Project import failed while cloning symbol '" + var2.symbol + "' to another timeframe - " + var1;
               ProjectResources.Log.error(var2x);
               ProjectResources.sendErrorMessage(var2x);
            }

            public void onConfirm(String var1) {
            }

            public void setMessage(String var1) {
            }

            public void setStep(int var1) {
            }
         });
      }
   }

   private static void sendInfoMessage(String var0) {
      statusData.getDataObject().put("info", var0);
      statusData.getDataObject().remove("error");
      SQWebSocketManager.addToDataQueue(statusData, "TASKMANAGER");
   }

   private static void sendErrorMessage(String var0) {
      statusData.getDataObject().put("error", var0);
      SQWebSocketManager.addToDataQueue(statusData, "TASKMANAGER");
   }

   public static class CloneInfo {
      public String symbol;
      public String sourceSymbol;
      public String timezone;
      public boolean removeWeekends;

      public CloneInfo(String var1, String var2, String var3, boolean var4) {
         this.symbol = var1;
         this.sourceSymbol = var2;
         this.timezone = var3;
         this.removeWeekends = var4;
      }
   }
}
