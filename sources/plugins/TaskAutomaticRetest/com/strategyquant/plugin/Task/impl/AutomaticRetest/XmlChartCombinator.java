package com.strategyquant.plugin.Task.impl.AutomaticRetest;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.basket.StockDto;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XmlChartCombinator {
   public static final Logger Log = LoggerFactory.getLogger(XmlChartCombinator.class);

   private List<XmlChartCombinator.Variation> getVariations(String var1, String var2) {
      LinkedList var3 = new LinkedList();
      String[] var4 = var1.split(",");
      String[] var5 = var2.split(",");
      var4 = this.transformStockGroups(var4);

      for (String var9 : var4) {
         var9 = var9.trim();
         if (!var9.equals("") && DataManager.getDataInfo("History", var9) == null) {
            throw new RuntimeException(L.t("Symbol not found", new Object[0]) + " " + var9);
         }

         for (String var13 : var5) {
            var13 = var13.trim();
            var3.add(new XmlChartCombinator.Variation(var9, var13));
         }
      }

      return var3;
   }

   private String[] transformStockGroups(String[] var1) {
      HashSet var2 = new HashSet();

      for (String var6 : var1) {
         var6 = var6.trim();
         if (!var6.startsWith("[")) {
            var2.add(var6);
         } else {
            BasketDto var7 = BasketOfStocksManager.getInstance().getBasket(var6);
            if (var7 == null) {
               throw new RuntimeException(L.t("Unknown stockgroup", new Object[0]) + " " + var6);
            }

            for (StockDto var10 : BasketOfStocksManager.getInstance().getStocks(var7.getId())) {
               DataInfo var11 = DataManager.getDataInfo("History", var10.getTicker());
               if (var11 != null && var11.dateFrom > 0L) {
                  var2.add(var10.getTicker());
               } else {
                  Log.debug("Symbol from group {} not found or is without any data", var10.getTicker());
               }
            }
         }
      }

      return var2.toArray(new String[0]);
   }

   private <T> List<List<T>> getCartesian(List<List<T>> var1) {
      ArrayList var2 = new ArrayList();
      var2.add(new ArrayList());

      for (List var4 : var1) {
         ArrayList var5 = new ArrayList();

         for (List var7 : var2) {
            for (Object var9 : var4) {
               ArrayList var10 = new ArrayList(var7);
               var10.add(var9);
               var5.add(var10);
            }
         }

         var2 = var5;
      }

      return var2;
   }

   private String getMainSymbol(Element var1) {
      if (var1 == null) {
         return null;
      }

      Element var2 = var1.getChild("Data");
      if (var2 == null) {
         return null;
      }

      Element var3 = var2.getChild("Setups");
      if (var3 == null) {
         return null;
      }

      Element var4 = var3.getChild("Setup");
      if (var4 == null) {
         return null;
      }

      Element var5 = var4.getChild("Chart");
      return var5 == null ? null : var5.getAttributeValue("symbol");
   }

   public List<Element> getSettingXMLVariations(Element var1, Element var2) throws Exception {
      Element var3 = var1.clone();
      Element var4 = var3.getChild("CustomData");
      Element var5 = var4.getChild("Setups");
      List var6 = var5.getChildren("Setup");
      ArrayList var7 = new ArrayList();

      for (int var8 = 0; var8 < var6.size(); var8++) {
         Element var9 = (Element)var6.get(var8);
         Element var10 = var9.getChild("MainTestValues");
         String var11 = var10.getAttributeValue("symbol", "false");
         String var12 = var10.getAttributeValue("timeframe", "false");
         List var13 = var9.getChildren("Chart");
         LinkedList var14 = new LinkedList();

         for (int var15 = 0; var15 < var13.size(); var15++) {
            Element var16 = (Element)var13.get(var15);
            String var17 = var16.getAttributeValue("symbol");
            String var18 = var16.getAttributeValue("timeframe");
            if (var15 == 0) {
               if ("true".equals(var11)) {
                  var17 = "";
               }

               if ("true".equals(var12)) {
                  var18 = "H1";
               }
            }

            List var19 = this.getVariations(var17, var18);
            var14.add(var19);
         }

         String var34 = null;

         for (List var37 : this.getCartesian(var14)) {
            Element var38 = var1.clone();
            Element var20 = var38.getChild("CustomData");
            Element var21 = var20.getChild("Setups");
            Element var22 = var21.getChild("Setup");
            List var23 = var22.getChildren("Chart");

            for (int var24 = 0; var24 < var23.size(); var24++) {
               Element var25 = (Element)var23.get(var24);
               XmlChartCombinator.Variation var26 = (XmlChartCombinator.Variation)var37.get(var24);
               var25.setAttribute("symbol", var26.symbol);
               var25.setAttribute("timeframe", var26.timeframe);
               if (var24 == 0) {
                  var34 = var26.symbol;
               }
            }

            DataInfo var39 = DataManager.getDataInfo("History", var34);
            if ("".equals(var34)) {
               String var40 = this.getMainSymbol(var2);
               if (var40 != null) {
                  var39 = DataManager.getDataInfo("History", var40);
               }
            }

            if (var39 != null) {
               Element var41 = var22.getChild("MainTestValues");
               String var42 = var41.getAttributeValue("distance", "strategy");
               String var27 = var41.getAttributeValue("spread", "strategy");
               String var28 = var41.getAttributeValue("commissions", "strategy");
               String var29 = var41.getAttributeValue("slippage", "strategy");
               String var30 = var41.getAttributeValue("swap", "strategy");
               InstrumentInfo var31 = var39.symbolInfo;
               if (var42.equals("instrument")) {
                  var22.setAttribute("minDist", String.valueOf(var31.minDistance));
               }

               if (var29.equals("instrument")) {
                  var22.setAttribute("slippage", String.valueOf(var31.defaultSlippage));
               }

               if (var27.equals("instrument")) {
                  ((Element)var23.get(0)).setAttribute("spread", String.valueOf(var31.defaultSpread));
               }

               if (var28.equals("instrument")) {
                  Element var32 = XMLUtil.stringToElement(var31.commissions);
                  Element var33 = new Element("Commissions");
                  var33.addContent(var32);
                  var22.removeChild("Commissions");
                  var22.addContent(var33);
               }

               if (var30.equals("instrument")) {
                  Element var43 = XMLUtil.stringToElement(var31.swap);
                  var22.removeChild("Swap");
                  var22.addContent(var43);
               }
            }

            var7.add(var38);
         }
      }

      return var7;
   }

   private static class Variation {
      String symbol;
      String timeframe;

      public Variation(String var1, String var2) {
         this.symbol = var1;
         this.timeframe = var2;
      }
   }
}
