package com.strategyquant.tradinglib.strategy;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.engine.ChartSetups;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.StrategyXMLModifier;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyMerger {
   private static final Logger Log = LoggerFactory.getLogger(StrategyMerger.class);
   public static final String PortfolioPartsDir = "PortfolioParts";
   public static final String TempPath = MainApp.getDataPath() + "/internal/tmp/";
   public static final int TempBufferSize = 1024;

   public static ResultsGroup createSimulatedResult(SQProject var0, ArrayList<ResultsGroup> var1, String var2, String var3) throws Exception {
      ResultsGroup var4 = ResultsGroup.merge(var1, new String[]{"AdditionalMarket", "CrossCheck_"}, var0);
      var4.setName(var2);
      if (var0 != null) {
         var0.getProgress().update("Create Portfolio", -1.0, null, L.t("Creating portfolio result", new Object[0]));
      }

      if (var1.size() > 0) {
         var4.createPortfolioResult(var3, var0);
      }

      Element var5 = var4.portfolio().getStrategyXml();
      if (var5 != null) {
         var5.setAttribute("mergedResult", "true");
      }

      var4.setLastSettings(((ResultsGroup)var1.get(0)).getLastSettings());
      addStrategyConfigs(var4, var1);
      return var4;
   }

   public static ResultsGroup createSimulatedResult(SQProject var0, ArrayList<ResultsGroup> var1, String var2, float var3, MoneyManagementMethod var4) throws Exception {
      ResultsGroup var5 = ResultsGroup.merge(var1, new String[]{"AdditionalMarket", "CrossCheck_"}, var0);
      var5.setName(var2);
      if (var0 != null) {
         var0.getProgress().update("Create Portfolio", -1.0, null, L.t("Creating portfolio result", new Object[0]));
      }

      if (var1.size() > 0) {
         var5.createPortfolioResult(var0, var3, var4);
      }

      Element var6 = var5.portfolio().getStrategyXml();
      if (var6 != null) {
         var6.setAttribute("mergedResult", "true");
      }

      var5.setLastSettings(((ResultsGroup)var1.get(0)).getLastSettings());
      addStrategyConfigs(var5, var1);
      return var5;
   }

   public static ResultsGroup createMergedResult(SQProject var0, ArrayList<ResultsGroup> var1, String var2) throws Exception {
      ResultsGroup var3 = new ResultsGroup(var2);
      var3.addSubresult("MergedToOne", new SettingsMap());
      Element var4 = null;
      Element var5 = null;
      Element var6 = null;
      Element var7 = null;
      Element var8 = null;
      Element var9 = new Element("CustomBlocks");
      int var10 = 11111;
      ArrayList var11 = new ArrayList();
      ArrayList var12 = new ArrayList();
      ArrayList var13 = new ArrayList();
      ArrayList var14 = new ArrayList();
      String var15 = "";

      for (int var16 = 0; var16 < var1.size(); var16++) {
         Element var17 = StrategyXMLModifier.getUpdatedStrategyXML((ResultsGroup)var1.get(var16)).clone();
         StrategyBase var18 = StrategyBase.createXmlStrategy(var17);
         var18.transformToNumbers();
         HashMap var19 = generateUniqueVariableIDs(var17);
         String var20 = XMLUtil.elementToString(var17);

         for (String var22 : var19.keySet()) {
            var20 = var20.replace(">" + var22 + "<", ">" + (String)var19.get(var22) + "<")
               .replace("variable=\"" + var22 + "\"", "variable=\"" + (String)var19.get(var22) + "\"");
            var20 = var20.replace("<name>" + (String)var19.get(var22) + "</name>", "<name>" + var22 + "</name>");
         }

         var17 = XMLUtil.stringToElement(var20);
         String var45 = null;

         try {
            String var48 = ((ResultsGroup)var1.get(var16)).getLastSettings();
            Element var23 = XMLUtil.stringToElement(var48);
            Element var24 = var23.getChild("Data").getChild("Setups").getChild("Setup");
            List var25 = var24.getChildren("Chart");
            Element var26 = ((Element)var25.get(0)).clone();
            var26.setAttribute("timeframe", "0");
            var26.setAttribute("spread", "0");
            var25.add(0, var26);
            Element var27 = (Element)var25.get(1);
            var27.setAttribute("symbol", "Same as main chart");
            if (var16 == 0) {
               var15 = XMLUtil.elementToString(var23);
            }

            ChartSetups var28 = ProjectConfigHelper.getChartSetups(var23.getChild("Data"));
            ChartSetup var29 = var28.getMainSetup();
            ChartDef var30 = var29.getMainChart();
            var45 = var30.getSymbol() + "/" + TimeframeManager.translateToMTConstant(var30.getTimeframe());
            if (var8 == null) {
               try {
                  var8 = var23.getChild("Options").getChild("BuildTradingOptions").clone();
                  var8.setName("TradingOptions");
               } catch (Exception var34) {
               }
            }
         } catch (Exception var35) {
            throw new Exception("Cannot parse strategies last settings. Please make sure all of the selected strategies are backtested");
         }

         HashMap var49 = generateUniqueCharts(var17, var45, var13, var14);
         int var52 = var20.indexOf("key=\"#Chart#\"", 0);

         while (var52 > 0) {
            int var56 = var20.indexOf(">", var52) + 1;
            int var59 = var20.indexOf("<", var56);
            int var65 = 0;

            try {
               var65 = Integer.parseInt(var20.substring(var56, var59));
            } catch (Exception var33) {
            }

            var20 = var20.substring(0, var56) + var49.get(var65) + var20.substring(var59);
            var52 = var20.indexOf("key=\"#Chart#\"", var59);
         }

         int var57 = (Integer)var49.get(0);
         var52 = var20.indexOf("key=\"#Symbol#\"", 0);

         while (var52 > 0) {
            int var60 = var20.indexOf(">", var52) + 1;
            int var66 = var20.indexOf("<", var52);
            var20 = var20.substring(0, var60) + "Subchart" + var57 + "Symbol" + var20.substring(var66);
            var52 = var20.indexOf("key=\"#Symbol#\"", var66);
         }

         var17 = XMLUtil.stringToElement(var20);
         if (var16 == 0) {
            var4 = var17;
            var5 = getOBUElement(var4);
            var6 = var4.getChild("Strategy").getChild("Variables");
            var7 = var4.getChild("Strategy").getChild("Datas");
            Element var62 = processCustomComments((ResultsGroup)var1.get(var16), var17, var16);
            var6.addContent(var62);
         } else {
            Element var61 = processCustomComments((ResultsGroup)var1.get(var16), var17, var16);
            var6.addContent(var61);
            Element var67 = getOBUElement(var17);
            List var70 = var67.getChildren("Rule");

            for (int var74 = 0; var74 < var70.size(); var74++) {
               Element var77 = ((Element)var70.get(var74)).clone();
               var77.setAttribute("name", var77.getAttributeValue("name") + (var16 + 1));
               var5.addContent(var77);
            }
         }

         Element var63 = var17.getChild("Strategy").getChild("Variables");
         List var68 = var63.getChildren("variable");

         for (int var71 = 0; var71 < var68.size(); var71++) {
            Element var75 = ((Element)var68.get(var71)).clone();
            String var78 = var75.getChildText("name");
            if (var78.equals("MagicNumber")) {
               int var80 = -1;

               try {
                  var80 = Integer.parseInt(var75.getChild("value").getText());
               } catch (Exception var32) {
               }

               if (var11.contains(var80) || var80 <= 0) {
                  var80 = var10;
               }

               if (var80 == var10) {
                  var10++;
               }

               var75.getChild("value").setText("" + var80);
               var11.add(var80);
            } else if (var78.startsWith("CustomComment")) {
               Element var81 = var75.getChild("value");
               if (var81 != null) {
                  String var31 = var81.getText();
                  if (var31 != null && var31.length() > 30) {
                     var81.setText(var31.substring(0, 30));
                  }
               }
            }

            if (var16 > 0) {
               var75.getChild("name").setText(var75.getChildText("name") + (var16 + 1));
               var6.addContent(var75);
            }
         }

         Element var72 = var17.getChild("Strategy").getChild("CustomBlocks");
         if (var72 != null) {
            List var76 = var72.getChildren("Item");

            for (int var79 = 0; var79 < var76.size(); var79++) {
               Element var82 = (Element)var76.get(var79);
               String var83 = var82.getAttributeValue("key");
               if (var83 != null && !var12.contains(var83)) {
                  var9.addContent(var82.clone());
                  var12.add(var83);
               }
            }
         }
      }

      var7.removeContent();

      for (int var36 = -1; var36 < var13.size(); var36++) {
         String var40 = "" + (var36 + 1);
         String var41 = "Main chart";
         String var42 = "NULL";
         String var44 = "0";
         String var47 = "1.0E-4";
         if (var36 >= 0) {
            String var50 = (String)var13.get(var36);
            int var54 = var50.lastIndexOf("/");
            var41 = "SubChart " + (var36 + 1);
            var42 = var50.substring(0, var54);
            var44 = var50.substring(var54 + 1);
            var47 = (String)var14.get(var36);
         }

         Element var51 = new Element("data");
         Element var55 = new Element("id");
         var55.setText(var40);
         Element var58 = new Element("chart");
         var58.setText(var41);
         Element var64 = new Element("symbol");
         var64.setText(var42);
         Element var69 = new Element("timeFrame");
         var69.setText(var44);
         Element var73 = new Element("ticksize");
         var73.setText(var47);
         var51.addContent(var55);
         var51.addContent(var58);
         var51.addContent(var64);
         var51.addContent(var69);
         var51.addContent(var73);
         var7.addContent(var51);
      }

      if (var8 != null) {
         var4.removeChild("TradingOptions");
         var4.addContent(var8);
      }

      Element var37 = var4.getChild("Strategy");
      var37.removeChild("CustomBlocks");
      var37.addContent(var9);
      CustomBlocks.addCBlockSourceCodes(var4);
      var3.portfolio().addStrategyXml(var4);
      var3.setLastSettings(var15);
      addStrategyConfigs(var3, var1);
      return var3;
   }

   private static Element getOBUElement(Element var0) {
      Element var1 = var0.getChild("Strategy").getChild("Rules").getChild("Events");
      List var2 = var1.getChildren("Event");

      for (int var3 = 0; var3 < var2.size(); var3++) {
         if (((Element)var2.get(var3)).getAttributeValue("key").equals("OnBarUpdate")) {
            return (Element)var2.get(var3);
         }
      }

      return null;
   }

   private static HashMap<String, String> generateUniqueVariableIDs(Element var0) {
      HashMap var1 = new HashMap();
      Element var2 = var0.getChild("Strategy").getChild("Variables");
      List var3 = var2.getChildren("variable");

      for (int var4 = 0; var4 < var3.size(); var4++) {
         Element var5 = (Element)var3.get(var4);
         var1.put(var5.getChildText("id"), UUID.randomUUID().toString());
      }

      return var1;
   }

   private static HashMap<Integer, Integer> generateUniqueCharts(Element var0, String var1, ArrayList<String> var2, ArrayList<String> var3) {
      HashMap var4 = new HashMap();
      Element var5 = var0.getChild("Strategy").getChild("Datas");
      List var6 = var5.getChildren("data");

      for (int var7 = 0; var7 < var6.size(); var7++) {
         Element var8 = (Element)var6.get(var7);
         String var9 = var8.getChildText("symbol");
         String var10 = var8.getChildText("timeFrame");
         String var11 = var8.getChildText("ticksize");
         String var12 = var7 == 0 ? var1 : var9 + "/" + var10;
         int var13 = -1;

         for (int var14 = 0; var14 < var2.size(); var14++) {
            if (((String)var2.get(var14)).equals(var12)) {
               var13 = var14;
               break;
            }
         }

         if (var13 < 0) {
            var13 = var2.size();
            var2.add(var12);
            var3.add(var11);
         }

         var4.put(var7, var13 + 1);
      }

      return var4;
   }

   public static ResultsGroup createFuzzyResult(SQProject var0, ArrayList<ResultsGroup> var1, String var2, int var3, String var4) throws Exception {
      ResultsGroup var5 = new ResultsGroup(var2);
      var5.addSubresult("EnsembleStrategy", new SettingsMap());
      ResultsGroup var6 = null;
      Element var7 = null;

      for (int var8 = 0; var8 < var1.size(); var8++) {
         if (((ResultsGroup)var1.get(var8)).getName().equals(var4)) {
            var6 = (ResultsGroup)var1.get(var8);
            var7 = ((ResultsGroup)var1.get(var8)).getStrategyXml().clone();

            try {
               String var9 = var6.getLastSettings();
               if (var9 == null) {
                  Log.info("Signal strategy doesn't contain last settings - trading options not set");
               } else {
                  Element var10 = XMLUtil.stringToElement(var9).getChild("Options").getChild("BuildTradingOptions").clone();
                  var10.setName("TradingOptions");
                  var7.removeChild("TradingOptions");
                  var7.addContent(var10);
               }
            } catch (Exception var31) {
               Log.error("Unable to set trading options into XML", var31);
            }
            break;
         }
      }

      if (var7 == null) {
         throw new Exception(L.tsq("Selected signals strategy not found"));
      }

      Element var34 = getOBUElement(var7);
      Element var35 = null;
      Element var36 = null;
      Element var11 = null;
      Element var12 = null;
      Element var13 = new Element("CustomBlocks");
      ArrayList var14 = new ArrayList();
      List var15 = var34.getChildren("Rule");
      ArrayList var16 = new ArrayList<>(Arrays.asList("Long entry", "Short entry", "Long exit", "Short exit"));

      for (int var17 = 0; var17 < var15.size(); var17++) {
         Element var18 = (Element)var15.get(var17);
         String var19 = var18.getAttributeValue("name");
         if (var19.equals("Trading signals")) {
            var18.setAttribute("type", "SignalFuzzy");
            Element var20 = var18.getChild("signals");
            List var21 = var20.getChildren("signal");

            for (int var22 = 0; var22 < var21.size(); var22++) {
               Element var23 = (Element)var21.get(var22);
               String var24 = var23.getAttributeValue("variable");
               var23.setAttribute("minTrue", "" + var3);
               if (var24.equals("33333333-1111-1111-3333-333333333333")) {
                  var35 = var23;
               } else if (var24.equals("33333333-2222-1111-3333-333333333333")) {
                  var36 = var23;
               } else if (var24.equals("33333333-1111-2222-3333-333333333333")) {
                  var11 = var23;
               } else if (var24.equals("33333333-2222-2222-3333-333333333333")) {
                  var12 = var23;
               }
            }
         } else if (var16.contains(var19)) {
            var16.remove(var19);
         }
      }

      if (var16.size() != 0) {
         throw new Exception("Signals strategy doesn't contain all expected rules (Long entry, Short entry, Long exit, Short exit)");
      }

      ArrayList var37 = new ArrayList();

      for (int var38 = 0; var38 < var1.size(); var38++) {
         try {
            Element var40 = ((ResultsGroup)var1.get(var38)).getStrategyXml().clone();
            Element var42 = var40.getChild("Strategy").getChild("CustomBlocks");
            if (var42 != null) {
               List var44 = var42.getChildren("Item");

               for (int var50 = 0; var50 < var44.size(); var50++) {
                  Element var55 = (Element)var44.get(var50);
                  String var58 = var55.getAttributeValue("key");
                  if (var58 != null && !var14.contains(var58)) {
                     var13.addContent(var55.clone());
                     var14.add(var58);
                  }
               }
            }

            if (!((ResultsGroup)var1.get(var38)).getName().equals(var4)) {
               Element var45 = getOBUElement(var40);
               List var51 = var45.getChild("Rule").getChild("signals").getChildren("signal");

               for (int var56 = 0; var56 < var51.size(); var56++) {
                  Element var59 = (Element)var51.get(var56);
                  String var25 = var59.getAttributeValue("variable");
                  List var26 = var59.getChildren();
                  if (var26.size() != 0) {
                     processElement((Element)var59.getChildren().get(0), var37, var38);
                     if (var25.equals("33333333-1111-1111-3333-333333333333")) {
                        if (var35 != null) {
                           var35.addContent(((Element)var59.getChildren().get(0)).clone());
                        }
                     } else if (var25.equals("33333333-2222-1111-3333-333333333333")) {
                        if (var36 != null) {
                           var36.addContent(((Element)var59.getChildren().get(0)).clone());
                        }
                     } else if (var25.equals("33333333-1111-2222-3333-333333333333")) {
                        if (var11 != null) {
                           var11.addContent(((Element)var59.getChildren().get(0)).clone());
                        }
                     } else if (var25.equals("33333333-2222-2222-3333-333333333333") && var12 != null) {
                        var12.addContent(((Element)var59.getChildren().get(0)).clone());
                     }
                  }
               }
            }
         } catch (Exception var33) {
            throw new Exception("Cannot find signals definition of strategy '" + ((ResultsGroup)var1.get(var38)).getName() + "'");
         }
      }

      Element var39 = var7.getChild("Strategy").getChild("Variables");
      List var41 = var39.getChildren();
      Element var43 = new Element("Variables");

      for (Element var52 : var41) {
         var43.addContent(var52.clone());
      }

      try {
         for (int var47 = 0; var47 < var1.size(); var47++) {
            Element var53 = ((ResultsGroup)var1.get(var47)).getStrategyXml().clone();
            if (!((ResultsGroup)var1.get(var47)).getName().equals(var4)) {
               Element var57 = var53.getChild("Strategy").getChild("Variables");
               List var60 = var57.getChildren("variable");

               for (int var61 = 0; var61 < var60.size(); var61++) {
                  Element var62 = ((Element)var60.get(var61)).clone();
                  String var27 = var62.getChildText("id");
                  if (var37.contains(var27 + "St" + var47)) {
                     Element var28 = var62.getChild("id");
                     if (var28 != null) {
                        var28.removeAttribute("variable");
                        String var29 = var62.getChildText("id") + "St" + var47;
                        var28.setText(var29);
                     } else {
                        var28 = new Element("id");
                        var28.removeAttribute("variable");
                        var28.setText(var62.getChildText("id") + "St" + var47);
                        var62.addContent(var28);
                     }

                     Element var64 = var62.getChild("name");
                     if (var64 != null) {
                        var64.removeAttribute("variable");
                        String var30 = var62.getChildText("name") + "St" + var47;
                        var64.setText(var30);
                     } else {
                        var64 = new Element("id");
                        var64.removeAttribute("variable");
                        var64.setText(var62.getChildText("name") + "St" + var47);
                        var62.addContent(var64);
                     }

                     XMLUtil.xmlToFile(var62, new File(MainApp.getDataPath() + "tests/tmp/elVariable.xml"));
                     var43.addContent(var62);
                  }
               }
            }
         }

         Element var48 = var39.getParentElement();
         int var54 = var48.indexOf(var39);
         var48.setContent(var54, var43);
      } catch (Exception var32) {
         throw new Exception("Cannot find variables of a strategy");
      }

      Element var49 = var7.getChild("Strategy");
      var49.removeChild("CustomBlocks");
      var49.addContent(var13);
      CustomBlocks.addCBlockSourceCodes(var7);
      var5.portfolio().addStrategyXml(var7);
      var5.setLastSettings(var6.getLastSettings());
      addStrategyConfigs(var5, var1);
      return var5;
   }

   private static void processElement(Element var0, List<String> var1, int var2) {
      if ("Param".equals(var0.getName())) {
         String var3 = var0.getAttributeValue("variable");
         if (var3 != null && var3.equals("true")) {
            String var4 = var0.getText() + "St" + var2;
            if (var4 != null) {
               var0.setText(var4);
               var1.add(var4);
            }
         }
      }

      for (Element var5 : var0.getChildren()) {
         processElement(var5, var1, var2);
      }
   }

   public static List<ResultsGroup> getSplitResults(ResultsGroup var0) {
      List var1 = var0.getMergedResults();
      if (var1 == null) {
         var1 = loadSplitResultsFromSource(var0.getFilePath());
      }

      return var1;
   }

   public static List<ResultsGroup> loadSplitResultsFromSource(String var0) {
      ArrayList var1 = new ArrayList();
      JarFile var2 = null;
      IProgram var3 = null;

      try {
         var3 = Program.get("Loader");
         File var4 = new File(var0);
         if (var4.exists()) {
            var2 = new JarFile(var4);
            Enumeration var5 = var2.entries();

            while (var5.hasMoreElements()) {
               JarEntry var6 = (JarEntry)var5.nextElement();
               if (!var6.isDirectory()) {
                  String var7 = var6.getName();
                  if (var7.startsWith("PortfolioParts")) {
                     File var8 = new File(TempPath + UUID.randomUUID() + ".sqx");
                     String var9 = SQUtils.stripExtension(new File(var7).getName());

                     try {
                        InputStream var10 = null;
                        BufferedOutputStream var11 = null;

                        try {
                           var10 = var2.getInputStream(var6);
                           if (!var8.exists() && var8.getParentFile() != null) {
                              var8.getParentFile().mkdirs();
                           }

                           var11 = new BufferedOutputStream(new FileOutputStream(var8));
                           byte[] var12 = new byte[1024];

                           int var13;
                           while ((var13 = var10.read(var12)) != -1) {
                              var11.write(var12, 0, var13);
                           }
                        } finally {
                           if (var10 != null) {
                              var10.close();
                           }

                           if (var11 != null) {
                              var11.close();
                           }
                        }

                        ResultsGroup var54 = (ResultsGroup)var3.call("loadFile", new Object[]{var8.getAbsolutePath()});
                        var54.setName(var9);
                        var1.add(var54);
                     } catch (Exception var49) {
                        Log.error("Loading split strategy '" + var9 + "' from portfolio failed", var49);
                     } finally {
                        try {
                           var8.delete();
                        } catch (Exception var47) {
                           Log.error("Error while deleting temp file", var47);
                        }
                     }
                  }
               }
            }
         }
      } catch (Exception var51) {
         Log.error("Can't get merged results from portfolio", var51);
      } finally {
         if (var2 != null) {
            try {
               var2.close();
            } catch (Exception var46) {
               Log.error("Unable to close jarFile", var46);
            }
         }
      }

      return var1;
   }

   private static void addStrategyConfigs(ResultsGroup var0, ArrayList<ResultsGroup> var1) throws Exception {
      var0.setMergedResults(var1);
   }

   private static Element processCustomComments(ResultsGroup var0, Element var1, int var2) throws Exception {
      ArrayList var3 = XMLUtil.getNestedElements(var1, "Param", "key", "#Comment#");
      String var4 = "CustomComment" + (var2 > 0 ? var2 + 1 : "");

      for (int var5 = 0; var5 < var3.size(); var5++) {
         Element var6 = (Element)var3.get(var5);
         if (XMLUtil.elementIsNot(var6, "variable")) {
            var6.setAttribute("variable", "true");
            var6.setAttribute("variableType", "STRING");
            var6.setText(var4);
         }
      }

      Element var12 = new Element("variable");
      Element var13 = new Element("id");
      var13.setText(var4);
      var12.addContent(var13);
      Element var7 = new Element("name");
      var7.setText(var4);
      var12.addContent(var7);
      Element var8 = new Element("type");
      var8.setText("string");
      var12.addContent(var8);
      String var9 = var0.getName().replace("Optimized", "Opt.").replace("Optimization", "Opt.");
      var9 = var9.length() > 30 ? var9.substring(0, 30) : var9;
      Element var10 = new Element("value");
      var10.setText(var9);
      var12.addContent(var10);
      Element var11 = new Element("makeExternal");
      var11.setText("true");
      var12.addContent(var11);
      return var12;
   }

   public static ResultsGroup createSimulatedWFResult(SQProject var0, ArrayList<ResultsGroup> var1, String var2) throws Exception {
      ResultsGroup var3 = ResultsGroup.mergeWF(var1, new String[]{"AdditionalMarket"}, var0);
      var3.setName(var2);
      var0.getProgress().update("Create Portfolio", -1.0, null, L.t("Creating portfolio result", new Object[0]));
      if (var1.size() > 0) {
         var3.createPortfolioResult("sum", var0);
      }

      Element var4 = var3.portfolio().getStrategyXml();
      if (var4 != null) {
         var4.setAttribute("mergedResult", "true");
      }

      var3.setLastSettings(((ResultsGroup)var1.get(0)).getLastSettings());
      addStrategyConfigs(var3, var1);
      return var3;
   }
}
