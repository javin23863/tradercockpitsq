package com.strategyquant.tradinglib.options.parameters;

import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;
import com.strategyquant.tradinglib.engine.stockpicker.constants.PickerEodTypes;
import com.strategyquant.tradinglib.engine.stockpicker.constants.PickerTriggerTypes;
import com.strategyquant.tradinglib.strategy.xml.XmlStrategyException;

public class StockpickerOptions extends TradingOption {
   @Parameter(name = "Entry type", defaultValue = "5", category = "Stockpicker")
   @Help("Entry type - when the entry condition is checked")
   @Editor(type = 40, values = "Randomly generated=0,From strategy=1,On Bar Open=5,On Bar Close=10,Before Bar Open=15")
   @ForEngine("SP,SA")
   public int PickerEntryType;
   @Parameter(name = "Exit type", defaultValue = "5", category = "Stockpicker")
   @Help("Exit type - when the exit condition is checked")
   @Editor(type = 40, values = "Randomly generated=0,From strategy=1,On Bar Open=5,On Bar Close=10,Before Bar Open=15")
   @ForEngine("SP,SA")
   public int PickerExitType;
   @Parameter(name = "Exit End Of Day Long", defaultValue = "5", category = "Stockpicker")
   @Help("When to place close at end of day - it can be at real day close or next market open")
   @Editor(type = 40, values = "Randomly generated=0,From strategy=1,Yes=5,No=10")
   @ForEngine("SP,SA")
   public int PickerEndOfDayLong;
   @Parameter(name = "Exit End Of Day Short", defaultValue = "5", category = "Stockpicker")
   @Help("When to place close at end of day - it can be at real day close or next market open")
   @Editor(type = 40, values = "Randomly generated=0,From strategy=1,Yes=5,No=10")
   @ForEngine("SP,SA")
   public int PickerEndOfDayShort;
   @Parameter(name = "Max Open Positions Long", defaultValue = "8", minValue = 0.0, maxValue = 500.0, category = "Stockpicker")
   @Help("Max Open Positions configuration for Position score rule - how many stocks will be open at the same time")
   @ForEngine("SP")
   public int PickerMaxOpenPositionsLong;
   @Parameter(name = "Max Open Positions Short", defaultValue = "8", minValue = 0.0, maxValue = 500.0, category = "Stockpicker")
   @Help("Max Open Positions configuration for Position score rule - how many stocks will be open at the same time")
   @ForEngine("SP")
   public int PickerMaxOpenPositionsShort;
   @Parameter(name = "Store Stockpicker Logs", defaultValue = "false", category = "Stockpicker")
   @Help("Store stockpicker logs in the backtest result?")
   @ForEngine("SP,SA")
   public boolean PickerStoreLogs;
   @Parameter(name = "Allow Better Limit Fill", defaultValue = "false", category = "Stockpicker")
   @Help("Fills limit orders at a better market price when available.")
   @ForEngine("SP,SA")
   public boolean PickerAllowBetterLimitFill;

   @Override
   public boolean OnBarUpdate(StrategyBase var1) throws Exception {
      return true;
   }

   @Override
   public boolean isUsedInTrading() {
      return false;
   }

   @Override
   public TradingOption getClone() {
      StockpickerOptions var1 = new StockpickerOptions();
      var1.PickerEntryType = this.PickerEntryType;
      var1.PickerExitType = this.PickerExitType;
      var1.PickerEndOfDayLong = this.PickerEndOfDayLong;
      var1.PickerEndOfDayShort = this.PickerEndOfDayShort;
      var1.PickerMaxOpenPositionsLong = this.PickerMaxOpenPositionsLong;
      var1.PickerMaxOpenPositionsShort = this.PickerMaxOpenPositionsShort;
      var1.PickerStoreLogs = this.PickerStoreLogs;
      var1.PickerAllowBetterLimitFill = this.PickerAllowBetterLimitFill;
      return var1;
   }

   public byte getEntryType(byte var1) throws XmlStrategyException {
      return this.optionValue("PickerEntryType", (byte)this.PickerEntryType, var1, PickerTriggerTypes.AvailableOptions);
   }

   public byte getExitType(byte var1) throws XmlStrategyException {
      return this.optionValue("PickerExitType", (byte)this.PickerExitType, var1, PickerTriggerTypes.AvailableOptions);
   }

   public byte getEndOfDayLong(byte var1) throws XmlStrategyException {
      return this.optionValue("PickerEndOfDayLong", (byte)this.PickerEndOfDayLong, var1, PickerEodTypes.AvailableOptions);
   }

   public byte getEndOfDayShort(byte var1) throws XmlStrategyException {
      return this.optionValue("PickerEndOfDayShort", (byte)this.PickerEndOfDayShort, var1, PickerEodTypes.AvailableOptions);
   }

   private byte optionValue(String var1, byte var2, byte var3, byte[] var4) throws XmlStrategyException {
      if (var2 != 0 && var2 != 1) {
         byte var5 = -1;

         for (int var6 = 0; var6 < var4.length; var6++) {
            if (var4[var6] == var2) {
               var5 = var2;
               break;
            }
         }

         if (var5 == -1) {
            throw new XmlStrategyException(String.format("Unexpected value %d for option %s.", var2, var1));
         } else {
            return var5;
         }
      } else {
         return var3;
      }
   }

   public int getMaxOpenPositionsLong(int var1) {
      return this.PickerMaxOpenPositionsLong == 0 ? var1 : this.PickerMaxOpenPositionsLong;
   }

   public int getMaxOpenPositionsShort(int var1) {
      return this.PickerMaxOpenPositionsShort == 0 ? var1 : this.PickerMaxOpenPositionsShort;
   }

   public boolean storeLogs() {
      return this.PickerStoreLogs;
   }

   public boolean allowBetterLimitFill() {
      return this.PickerAllowBetterLimitFill;
   }
}
