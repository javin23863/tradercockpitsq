package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoRetestStrategyConfigModifier {
   private static final Logger Log = LoggerFactory.getLogger(AutoRetestStrategyConfigModifier.class);

   public static void modifySettings(Element var0, Element var1) throws Exception {
      AutoRetestSettings var2 = new AutoRetestSettings();
      var2.init(var1);
      Element var3 = var0.getChild("Data");
      modifyDataSettings(var3, var2);
      if (var2.useCustomMoneyManagement()) {
         var0.removeChild("RiskMoneyManagement");
         var0.addContent(var1.getChild("RiskMoneyManagement").clone());
      }

      if (var2.useCustomTradingOptions()) {
         var0.removeChild("Options");
         var0.addContent(var1.getChild("Options").clone());
      }

      var0.removeChild("Rankings");
      var0.addContent(var1.getChild("Rankings").clone());
      var0.removeChild("CrossChecks");
      var0.addContent(var1.getChild("CrossChecks").clone());
      modifyAdditionalMarketsSettings(var0.getChild("CrossChecks").getChild("RetestOnAdditionalMarkets"), var3);
      var0.removeChild("ATMs");
      if (var1.getChild("ATMs") != null) {
         var0.addContent(var1.getChild("ATMs").clone());
      }
   }

   private static void modifyDataSettings(Element var0, AutoRetestSettings var1) throws Exception {
      Element var2 = var0.getChild("Setups");
      List var3 = var2.getChildren("Setup");
      String var4 = null;
      String var5 = null;

      for (int var6 = 0; var6 < var3.size(); var6++) {
         Element var7 = (Element)var3.get(var6);
         Element var8 = var7.getChild("Chart");
         if (var8 != null) {
            String var9 = var8.getAttributeValue("symbol");
            if (var6 == 0) {
               var4 = var7.getAttributeValue("dateTo");
               modifySetup(var7, var1);
               var9 = var8.getAttributeValue("symbol");
            }

            DataInfo var10 = DataManager.getDataInfo("History", var9);
            if (var10 != null) {
               String var11 = SQTime.formatDate(var10.dateTo);
               if (var6 == 0) {
                  var5 = var1.useCustomDates() ? var1.getDateTo() : var11;
               }

               var7.setAttribute("dateTo", var5);
            }
         }
      }

      Element var16 = XMLUtil.getChildElem(var0, "OutOfSample", "<OutOfSample showGraph='false'/>");
      if (var1.useCustomDates()) {
         var16.removeContent();
      } else {
         try {
            if (var4 != null && var5 != null) {
               long var17 = SQTime.parseDateToMilis(var4);
               List var18 = var16.getChildren("Range");
               boolean var19 = false;

               for (int var20 = 0; var20 < var18.size(); var20++) {
                  Element var12 = (Element)var18.get(var20);
                  long var13 = SQTime.parseDateToMilis(var12.getAttributeValue("dateTo"));
                  if (var13 == var17) {
                     var12.setAttribute("dateTo", var5);
                     var19 = true;
                     break;
                  }
               }

               if (!var19) {
                  Element var21 = new Element("Range");
                  var21.setAttribute("dateFrom", var4);
                  var21.setAttribute("dateTo", var5);
                  var16.addContent(var21);
               }
            } else {
               Log.error("Cannot update OOS ranges. prevDateTo: " + var4 + ", newDateTo: " + var5);
            }
         } catch (Exception var15) {
            Log.error("Updating OOS ranges failed", var15);
         }
      }
   }

   private static void modifySetup(Element var0, AutoRetestSettings var1) {
      List var2 = var0.getChildren("Chart");
      Element var3 = (Element)var2.get(0);
      if (var1.useCustomEngine()) {
         var0.setAttribute("engine", var1.getEngine());
      }

      if (var1.useCustomSymbol()) {
         var3.setAttribute("symbol", var1.getSymbol());
      }

      if (var1.useCustomTimeframe()) {
         var3.setAttribute("timeframe", var1.getTimeframe());
      }

      if (var1.useCustomDates()) {
         var0.setAttribute("dateFrom", var1.getDateFrom());
         var0.setAttribute("dateTo", var1.getDateTo());
      }

      if (var1.useCustomPrecision()) {
         var0.setAttribute("testPrecision", "" + var1.getPrecision());
      }

      if (var1.useCustomSlippage()) {
         var0.setAttribute("slippage", "" + var1.getSlippage());
      }

      if (var1.useCustomMinDistance()) {
         var0.setAttribute("minDist", "" + var1.getMinDistance());
      }

      if (var1.useCustomSpread()) {
         var3.setAttribute("spread", "" + var1.getSpread());
      }

      ArrayList var4 = var1.getSubcharts();
      int var5 = Math.min(var2.size(), var4.size() + 1);

      for (int var6 = 1; var6 < var5; var6++) {
         Element var7 = (Element)var2.get(var6);
         AutoRetestSettings.Subchart var8 = (AutoRetestSettings.Subchart)var4.get(var6 - 1);
         var7.setAttribute("symbol", var8.symbol);
         var7.setAttribute("timeframe", var8.timeframe);
         var7.setAttribute("spread", "" + var1.getSpread());
      }

      if (var1.useCustomCommissions()) {
         var0.removeChild("Commissions");
         Element var9 = var1.getCommissions().getXML();
         var0.addContent(var9);
      }

      if (var1.useCustomSwap()) {
         var0.removeChild("Swap");
         Element var10 = var1.getSwap().getXML();
         var0.addContent(var10);
      }
   }

   private static void modifyAdditionalMarketsSettings(Element var0, Element var1) {
      if (XMLUtil.elementIs(var0, "use")) {
         try {
            Element var2 = XMLUtil.getChildElem(var0, "Settings");
            Element var3 = XMLUtil.getChildElem(var2, "Setups");
            List var4 = var3.getChildren("Setup");
            Element var5 = XMLUtil.getChildElem(XMLUtil.getChildElem(var1, "Setups"), "Setup");
            Element var6 = XMLUtil.getChildElem(var5, "Chart");

            for (int var7 = 0; var7 < var4.size(); var7++) {
               try {
                  Element var8 = (Element)var4.get(var7);
                  Element var9 = XMLUtil.getChildElem(var8, "MainTestValues");
                  List var10 = var8.getChildren("Chart");
                  if (XMLUtil.elementIs(var9, "dates")) {
                     var8.setAttribute("dateFrom", var5.getAttributeValue("dateFrom"));
                     var8.setAttribute("dateTo", var5.getAttributeValue("dateTo"));
                  }

                  if (XMLUtil.elementIs(var9, "precision")) {
                     var8.setAttribute("testPrecision", var5.getAttributeValue("testPrecision"));
                  }

                  if (XMLUtil.elementIs(var9, "distance")) {
                     var8.setAttribute("minDist", var5.getAttributeValue("minDist"));
                  }

                  if (XMLUtil.elementIs(var9, "slippage")) {
                     var8.setAttribute("slippage", var5.getAttributeValue("slippage"));
                  }

                  if (XMLUtil.elementIs(var9, "session")) {
                     var8.setAttribute("session", var5.getAttributeValue("session"));
                  }

                  if (XMLUtil.elementIs(var9, "commissions")) {
                     var8.removeChild("Commissions");
                     var8.addContent(var5.getChild("Commissions").clone());
                  }

                  for (int var11 = 0; var11 < var10.size(); var11++) {
                     Element var12 = (Element)var10.get(var11);
                     String var13 = var12.getAttributeValue("symbol");
                     if (XMLUtil.elementIs(var9, "timeframe")) {
                        DataInfo var14 = DataManager.getDataInfo("History", var13);
                        String var15 = var14.timeframe;
                        String var16 = var6.getAttributeValue("timeframe");
                        if (TimeframeManager.getMillis(var15) <= TimeframeManager.getMillis(var16)) {
                           var12.setAttribute("timeframe", var16);
                        }
                     }

                     if (XMLUtil.elementIs(var9, "spread")) {
                        var12.setAttribute("spread", var6.getAttributeValue("spread"));
                     }
                  }
               } catch (Exception var17) {
                  Log.error("Preparing RetestOnAdditionalMarkets setup #" + (var7 + 1) + " failed", var17);
               }
            }
         } catch (Exception var18) {
            Log.error("Preparing RetestOnAdditionalMarkets cross check settings failed", var18);
         }
      }
   }
}
