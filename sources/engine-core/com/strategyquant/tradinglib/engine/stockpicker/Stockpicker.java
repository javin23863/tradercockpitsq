package com.strategyquant.tradinglib.engine.stockpicker;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.engine.stockpicker.backtester.BacktestData;
import com.strategyquant.tradinglib.talib.TALibIndicators;
import java.io.File;
import java.util.ArrayList;
import org.jdom2.Element;

public class Stockpicker {
   public TALibIndicators TALibIndicators = new TALibIndicators();
   public boolean LongEntryExists = false;
   public boolean ShortEntryExists = false;
   public int MaxOpenPositionsLong = 0;
   public int MaxOpenPositionsShort = 0;
   public byte entryType;
   public byte exitType;
   public byte eodLong;
   public byte eodShort;
   public byte strategyTriggeredAt;
   public BacktestData data;
   public boolean singleAsset = false;
   private static boolean debugMode = false;
   private static File debugModeFile = null;

   public int getCurrentBar(int var1) {
      return this.data.getCurrentBar(var1);
   }

   public byte strategyTriggeredAt() {
      return this.strategyTriggeredAt;
   }

   public byte entryType() {
      return this.entryType;
   }

   public byte exitType() {
      return this.exitType;
   }

   public boolean isSingleAssetStrategy() {
      return this.singleAsset;
   }

   public long TimeCurrent() throws TradingException {
      return this.data.getCurrentTime();
   }

   public int BarCurrent() {
      return this.data.getCurrentBar();
   }

   public void setData(BacktestData var1) {
      this.data = var1;
   }

   public static boolean isDebugModeEnabled() {
      if (debugModeFile == null) {
         debugModeFile = new File(MainApp.getDataPath() + "/pickerdebug.txt");
         debugMode = debugModeFile.exists();
      }

      return debugMode;
   }

   public void clear() {
      if (this.TALibIndicators != null) {
         this.TALibIndicators.clear();
      }
   }

   public int getMaxOpenPositions() {
      int var1 = 0;
      if (this.LongEntryExists) {
         var1 += this.MaxOpenPositionsLong;
      }

      if (this.ShortEntryExists) {
         var1 += this.MaxOpenPositionsShort;
      }

      return var1;
   }

   public static int getMaxOpenPosition(Element var0, int var1) {
      if (isEntryRuleEmpty(var0, var1)) {
         return 0;
      }

      ArrayList var2 = new ArrayList();
      XMLUtil.findAll(var0, "Rule", var2);

      for (Element var5 : var2) {
         String var6 = var5.getAttributeValue("type");
         if (var6 != null && var6.equals("StockpickerPS")) {
            String var7 = var5.getAttributeValue("name");
            byte var3;
            if (var7.equalsIgnoreCase("Position Score Long")) {
               var3 = 1;
            } else {
               var3 = -1;
            }

            if (var1 == var3) {
               ArrayList var8 = new ArrayList();
               XMLUtil.findAllWithKey(var5, "Item", "AssignVariable", var8);

               for (Element var10 : var8) {
                  if (XMLUtil.findFirstWithKey(var10, "Param", "#MaxOpenPositions#") != null) {
                     Element var11 = XMLUtil.findFirstWithKey(var10, "Param", "#Value#");
                     if (var11 != null) {
                        return Integer.valueOf(var11.getText());
                     }
                     break;
                  }
               }
            }
         }
      }

      return 0;
   }

   private static boolean isEntryRuleEmpty(Element var0, int var1) {
      ArrayList var2 = new ArrayList();
      XMLUtil.findAll(var0, "Rule", var2);

      for (Element var5 : var2) {
         String var6 = var5.getAttributeValue("type");
         if (var6 != null && var6.equals("StockpickerEntryExit")) {
            String var7 = var5.getAttributeValue("name");
            byte var3;
            if (var7.equalsIgnoreCase("Long")) {
               var3 = 1;
            } else {
               var3 = -1;
            }

            if (var1 == var3) {
               Element var8 = var5.getChild("Entry");
               return var8 == null || var8.getChildren().isEmpty();
            }
         }
      }

      return false;
   }
}
