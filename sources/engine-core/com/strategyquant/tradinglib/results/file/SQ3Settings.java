package com.strategyquant.tradinglib.results.file;

import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.strategy.OutOfSample;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQ3Settings {
   public static final Logger Log = LoggerFactory.getLogger(SQ3Settings.class);

   public Element convertToSQ3Xml(ResultsGroup var1) throws Exception {
      Element var2 = new Element("Settings");
      SettingsMap var3 = var1.specialValues();
      String var4 = "3";
      String var5 = "false";
      String var6 = "false";
      String var7 = "0";
      String var8 = "5";
      String var9 = "1";
      String var10 = "3";
      String var11 = "false";
      String var12 = "100";

      try {
         String var13 = var1.getLastSettings();
         if (var13 == null) {
            Log.info("Strategy '{}' doesn't contain last settings", var1.getName());
            return var2;
         }

         Element var14 = XMLUtil.stringToElement(var13);
         Element var15 = XMLUtil.getChildElem(var14, "WhatToBuild");
         Element var16 = XMLUtil.getChildElem(var15, "MarketSides");
         var4 = this.directionToSQ3(var16.getAttributeValue("type"));
         if (var16.getAttributeValue("type").equals("both")) {
            var5 = var16.getChild("EntrySymmetry").getText();
            var6 = var16.getChild("ExitSymmetry").getText();
         }

         Element var17 = var14.getChild("Data");
         Element var18 = var17.getChild("Setups");
         Element var19 = var18.getChild("Setup");
         var7 = var19.getAttributeValue("slippage");
         var8 = var19.getAttributeValue("minDist");
         var9 = var19.getAttributeValue("testPrecision");
         if (Integer.parseInt(var9) == 4) {
            var11 = "true";
         }

         Element var20 = var19.getChild("Chart");
         var10 = var20.getAttributeValue("spread");
         Element var21 = var2.getChild("Rankings");
         if (var21 != null) {
            Element var22 = var21.getChild("MaxStrategies");
            var12 = var22.getText();
         }
      } catch (Exception var23) {
         Log.error("Exc.", var23);
      }

      SettingsMap var24 = var1.portfolio().getSettings();
      var2.addContent(new Element("generationType").addContent("1"));
      var2.addContent(new Element("tradingSides").addContent(var4));
      var2.addContent(new Element("symmetricLongShortEntry").addContent(var5));
      var2.addContent(new Element("symmetricLongShortExit").addContent(var6));
      var2.addContent(new Element("slRequired").addContent("true"));
      var2.addContent(new Element("ptRequired").addContent("true"));
      var2.addContent(new Element("slptBasedOnATR").addContent("true"));
      var2.addContent(new Element("slptBasedOnFixedPips").addContent("true"));
      var2.addContent(new Element("minimumPTSL").addContent("0"));
      var2.addContent(new Element("maximumPTSL").addContent("1000"));
      var2.addContent(new Element("minAtrMultiply").addContent("0"));
      var2.addContent(new Element("maxAtrMultiply").addContent("100"));
      var2.addContent(new Element("spread").addContent(var10));
      var2.addContent(new Element("useRealSpread").addContent(var11));
      var2.addContent(new Element("slippage").addContent(var7));
      var2.addContent(new Element("minDistance").addContent(var8));
      var2.addContent(new Element("testPrecision").addContent(var9));
      var2.addContent(new Element("dateFrom").addContent(var3.getLong(SpecialValues.HistoryFrom) / 1000L + ""));
      var2.addContent(new Element("dateTo").addContent(var3.getLong(SpecialValues.HistoryTo) / 1000L + ""));
      OutOfSample var25 = var24 == null ? null : (OutOfSample)var24.get("OutOfSample");
      if (var25 != null && var25.isEmpty()) {
         var2.addContent(new Element("oosDateFrom").addContent(var25.getDateFrom(0) / 1000L + ""));
         var2.addContent(new Element("oosDateTo").addContent(var25.getDateTo(0) / 1000L + ""));
      } else {
         var2.addContent(new Element("oosDateFrom").addContent("0"));
         var2.addContent(new Element("oosDateTo").addContent("0"));
      }

      var2.addContent(new Element("symbol").addContent(var1.mainResult().getString(SpecialValues.Symbol, "N/A")));
      String var26 = var1.mainResult().getString(SpecialValues.Timeframe, "N/A");
      var2.addContent(new Element("timeframe").addContent(translateSQ4TFToInt(var26) + ""));
      var2.addContent(new Element("rankingType").addContent("2"));
      var2.addContent(new Element("maxRecordsInDatabank").addContent(var12));
      var2.addContent(new Element("tradeTimeFrom").addContent(var3.getLong(SpecialValues.HistoryFrom) / 1000L + ""));
      var2.addContent(new Element("tradeTimeTo").addContent(var3.getLong(SpecialValues.HistoryTo) / 1000L + ""));
      var2.addContent(new Element("sliderOOS").addContent("0"));
      var2.addContent(new Element("testPrecision").addContent("1"));
      var2.addContent(new Element("capitalSize").addContent(Integer.toString((int)var24.getDouble("MoneyManagement.InitialCapital", 10000.0))));
      var2.addContent(new Element("mmType").addContent("0"));
      var2.addContent(new Element("mmLots").addContent("0.1"));
      var2.addContent(new Element("crossoverProbability").addContent("0.95"));
      var2.addContent(new Element("mutationProbability").addContent("0.1"));
      var2.addContent(new Element("exitEndOfDay").addContent(var24.containsKey("ExitAtEndOfDay.EODExitTime") + ""));
      var2.addContent(new Element("exitFriday").addContent(var24.containsKey("ExitOnFriday.FridayExitTime") + ""));
      var2.addContent(new Element("limitTradesPerDay").addContent(var24.get("MaxTradesPerDay", 0) + ""));
      return var2;
   }

   private String directionToSQ3(String var1) {
      if (var1.equals("both")) {
         return "1";
      } else if (var1.equals("long")) {
         return "2";
      } else {
         return var1.equals("short") ? "3" : "1";
      }
   }

   public Element convertToSQ4Xml(Element var1) {
      return null;
   }

   public static int translateSQ4TFToInt(String var0) {
      if (var0 == null) {
         return -2;
      }

      switch (var0) {
         case "TICK":
            return 0;
         case "M1":
            return 1;
         case "M5":
            return 5;
         case "M14":
            return 14;
         case "M15":
            return 15;
         case "M30":
            return 30;
         case "M53":
            return 53;
         case "H1":
            return 60;
         case "H2":
            return 120;
         case "H4":
            return 240;
         case "D1":
            return 1440;
         case "unknown":
            return -2;
         case "Intraday":
            return -10;
         default:
            return -2;
      }
   }
}
