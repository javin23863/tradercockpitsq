package com.strategyquant.plugin.Servlet.impl.Project;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TaskConfigWorker {
   private static final Logger Log = LoggerFactory.getLogger(TaskConfigWorker.class);

   public static boolean isCustomSettings(String var0, Element var1) throws Exception {
      if (var0 == null || var0.equals("Custom")) {
         return true;
      }

      if (var1 == null) {
         throw new Exception(L.t("Config has no settings", new Object[0]));
      }

      Element var2 = null;

      try {
         var2 = getTemplateSettings(var0);
      } catch (Exception var26) {
         return true;
      }

      try {
         if (!XMLUtil.elementsEqual(XMLUtil.getChildElem(var1, "Blocks"), XMLUtil.getChildElem(var2, "Blocks"))) {
            return true;
         }

         Element var3 = XMLUtil.getChildElem(var1, "WhatToBuild");
         Element var4 = XMLUtil.getChildElem(var2, "WhatToBuild");
         Element var5 = XMLUtil.getChildElem(var3, "StrategyType");
         Element var6 = XMLUtil.getChildElem(var4, "StrategyType");
         if (var5.getAttributeValue("architecture").equals(var6.getAttributeValue("architecture"))
            && XMLUtil.elementsEqual(XMLUtil.getChildElem(var3, "MarketSides"), XMLUtil.getChildElem(var4, "MarketSides"))
            && XMLUtil.elementsEqual(XMLUtil.getChildElem(var3, "SLPTOptions"), XMLUtil.getChildElem(var4, "SLPTOptions"))
            && XMLUtil.elementsEqual(XMLUtil.getChildElem(var3, "BuildMode"), XMLUtil.getChildElem(var4, "BuildMode"))) {
            Element var7 = XMLUtil.getChildElem(var3, "RulesComplexity");
            Element var8 = XMLUtil.getChildElem(var4, "RulesComplexity");
            List var9 = var7.getChildren("Chart");
            List var10 = var8.getChildren("Chart");
            boolean var11 = false;

            try {
               var11 = Boolean.parseBoolean(var7.getAttributeValue("useDifferentSettings"));
            } catch (Exception var25) {
            }

            boolean var12 = false;

            try {
               var12 = Boolean.parseBoolean(var8.getAttributeValue("useDifferentSettings"));
            } catch (Exception var24) {
            }

            if (var11 != var12) {
               return true;
            }

            for (int var13 = 0; var13 < var9.size() && var13 < var10.size() && (var11 || var13 <= 0); var13++) {
               if (!XMLUtil.elementsEqual((Element)var9.get(var13), (Element)var10.get(var13))) {
                  return true;
               }
            }

            Element var29 = XMLUtil.getChildElem(var1, "CrossChecks");
            Element var14 = XMLUtil.getChildElem(var2, "CrossChecks");
            List var15 = var29.getChildren();
            List var16 = var14.getChildren();

            for (int var17 = 0; var17 < var16.size(); var17++) {
               Element var18 = (Element)var16.get(var17);
               boolean var19 = false;

               for (int var20 = 0; var20 < var15.size(); var20++) {
                  Element var21 = (Element)var15.get(var20);
                  if (var21.getName().equals(var18.getName())) {
                     String var22 = var21.getAttributeValue("use", "false");
                     String var23 = var18.getAttributeValue("use", "false");
                     if (!var22.equals(var23)) {
                        return true;
                     }

                     var19 = true;
                  }
               }

               if (!var19) {
                  return true;
               }
            }

            return false;
         } else {
            return true;
         }
      } catch (Exception var27) {
         Log.error("Comapring config with template failed", var27);
         return true;
      }
   }

   public static void correctDataSettings(Element var0, String var1) throws Exception {
      Element var2 = XMLUtil.getChildElem(var0, "Data");
      Element var3 = XMLUtil.tryAddElement(var2, "Setups");
      ArrayList var4 = DataManager.list();
      DataInfo var5 = var4.isEmpty() ? null : (DataInfo)var4.get(0);
      Element var6 = var3.getChild("Setup");
      if (var6 == null) {
         var6 = new Element("Setup");
         var6.setAttribute("dateFrom", SQTime.toUIDateString(var5 != null ? var5.dateFrom : 0L));
         var6.setAttribute("dateTo", SQTime.toUIDateString(var5 != null ? var5.dateTo : 0L));
         var6.setAttribute("testPrecision", "Selected timeframe only (fastest)");
         var6.setAttribute("session", "No Session");
         var6.setAttribute("slippage", "0");
         var6.setAttribute("minDist", "0");
         var6.setAttribute("engine", "MetaTrader4");
         var3.addContent(var6);
      }

      Element var7 = var6.getChild("Chart");
      if (var7 == null) {
         addDefaultChart(var6, var5, var1);
      } else {
         String var8 = var7.getAttributeValue("symbol");
         DataInfo var9 = DataManager.getDataInfo("History", var8);
         if (var9 != null) {
            try {
               long var10 = SQTime.parseDateToMilis(var6.getAttributeValue("dateFrom"));
               if (var10 < var9.dateFrom) {
                  var6.setAttribute("dateFrom", SQTime.toUIDateString(var9.dateFrom));
               }
            } catch (Exception var13) {
               var6.setAttribute("dateFrom", SQTime.toUIDateString(var9.dateFrom));
            }

            try {
               long var14 = SQTime.parseDateToMilis(var6.getAttributeValue("dateTo"));
               if (var14 > var9.dateTo) {
                  var6.setAttribute("dateTo", SQTime.toUIDateString(var9.dateTo));
               }
            } catch (Exception var12) {
               var6.setAttribute("dateTo", SQTime.toUIDateString(var9.dateTo));
            }

            if (var1 != null) {
               var7.setAttribute("timeframe", var1);
            }
         } else {
            addDefaultChart(var6, var5, var1);
         }
      }
   }

   private static void addDefaultChart(Element var0, DataInfo var1, String var2) throws Exception {
      var0.removeChildren("Chart");
      var0.removeChildren("Commissions");
      Element var3 = new Element("Chart");
      var3.setAttribute("symbol", var1 != null ? var1.symbol : "undefined");
      var3.setAttribute("timeframe", var2 != null ? var2 : (var1 != null ? var1.timeframe : "undefined"));
      var3.setAttribute("spread", "0");
      Element var4 = new Element("Commissions");
      Element var5 = null;
      if (var1 != null && var1.symbolInfo != null) {
         var5 = XMLUtil.stringToElement(var1.symbolInfo.commissions);
      } else {
         var5 = new Element("Method");
         var5.setAttribute("type", "None");
         var5.setAttribute("use", "true");
      }

      var4.addContent(var5);
      var0.addContent(var4);
      var0.addContent(var3);
      var0.setAttribute("dateFrom", SQTime.toUIDateString(var1 != null ? var1.dateFrom : 0L));
      var0.setAttribute("dateTo", SQTime.toUIDateString(var1 != null ? var1.dateTo : 0L));
      var0.setAttribute("testPrecision", "Selected timeframe only (fastest)");
   }

   public static Element getTemplateSettings(String var0) throws Exception {
      File var1 = new File(SQPaths.simpleTemplatesDirPath + "/" + var0 + ".xml");
      if (!var1.exists()) {
         throw new Exception("Template file '" + var1.getName() + "' not found");
      }

      Element var2 = XMLUtil.fileToXmlElement(var1);
      return XMLUtil.getChildElem(var2, "Settings");
   }
}
