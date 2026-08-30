package com.strategyquant.tradinglib.engine.stockpicker;

import com.strategyquant.datalib.DataInfo;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.TradeSizeSmallerThanOneException;
import com.strategyquant.tradinglib.Trader;
import com.strategyquant.tradinglib.backtestrunner.IBacktestProgressListener;
import com.strategyquant.tradinglib.engine.stockpicker.backtester.log.StockpickerLog;
import com.strategyquant.tradinglib.engine.stockpicker.constants.PickerTriggerTypes;
import com.strategyquant.tradinglib.engine.stockpicker.data.LoadedPickerData;
import com.strategyquant.tradinglib.engine.stockpicker.data.symbols.LoadedSymbolData;
import com.strategyquant.tradinglib.engine.stockpicker.signals.Signals;
import com.strategyquant.tradinglib.engine.stockpicker.signals.entry.PickerEntrySignal;
import com.strategyquant.tradinglib.engine.stockpicker.signals.entry.PickerEntrySignals;
import com.strategyquant.tradinglib.engine.stockpicker.signals.exit.PickerExitSignal;
import com.strategyquant.tradinglib.engine.stockpicker.signals.exit.PickerExitSignals;
import com.strategyquant.tradinglib.strategy.LiveOrderObj;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioTrader {
   public static final Logger Log = LoggerFactory.getLogger(StockpickerBacktestEngine.class);
   public StockBroker broker;
   private int maxPosLong = 0;
   private int maxPosShort = 0;
   private TickEvent tick = new TickEvent();
   private MoneyManagementMethod mmMethod = null;
   private boolean mmMethodInitialized = false;
   private Trader tradeController = null;
   IBacktestProgressListener backtestProgressListener;
   private StockpickerLog pickerLog;

   public PortfolioTrader(Trader var1, StockpickerLog var2) {
      this.tradeController = var1;
      this.pickerLog = var2;
      this.broker = new StockBroker(var1, var2);
   }

   public void setMaxOpenPos(int var1, int var2) {
      this.maxPosLong = var1;
      this.maxPosShort = var2;
   }

   private boolean checkMaxOpenPositions(int var1) {
      return var1 == 1 ? this.broker.openPosCount(var1) < this.maxPosLong : this.broker.openPosCount(var1) < this.maxPosShort;
   }

   public String printOpenTrades(boolean var1) {
      return this.broker.printOpenTrades(var1);
   }

   public void printOpenTrades(StringBuilder var1, boolean var2) {
      this.broker.printOpenTrades(var1, var2);
   }

   public int openPosCount(boolean var1) {
      return this.broker.openPosCount(var1);
   }

   public void onStart(double var1, double var3, CommissionsMethod var5, SwapMethod var6, double var7, boolean var9) {
      this.broker.onStart(var1, var3, var5, var6, var7, var9);
   }

   public void onClose() {
      this.broker.onClose();
   }

   public OrdersList getHistoryOrders() {
      return this.broker.getHistoryOrders();
   }

   public double getAccountBalance() {
      return this.broker.getAccountBalance();
   }

   public double getAccountEquity() {
      return this.broker.getAccountEquity();
   }

   public ObjectArrayList<String> getTradedSymbols() {
      return this.broker.getTradedSymbols();
   }

   public void onBar(long var1, int var3, LoadedPickerData var4, Signals var5) throws Exception {
      try {
         this.broker.resetStats();
         StrategyBase var6 = this.tradeController.getStrategy();
         byte var7 = var6.Stockpicker.entryType;
         byte var8 = var6.Stockpicker.exitType;
         PickerTriggerTypes var9 = new PickerTriggerTypes();
         IntOpenHashSet var10 = new IntOpenHashSet();
         this.onTick(var4, var1, var3, var7, var8, StockBroker.TickBeforeBarOpen);
         if (var8 == 15 || var8 == 5) {
            this.evalExiSignals(var4, var1, var3, var5, true, var7, var8, var10);
         }

         if (var7 == 15 || var7 == 5) {
            this.evalEntrySignals(var4, var1, var3, var5, true, var10);
         }

         this.onTick(var4, var1, var3, var7, var8, StockBroker.TickOnBarOpen);
         if (var8 == 10) {
            this.evalExiSignals(var4, var1, var3, var5, false, var7, var8, var10);
         }

         if (var7 == 10) {
            this.evalEntrySignals(var4, var1, var3, var5, false, var10);
         }

         this.onTick(var4, var1, var3, var7, var8, StockBroker.TickOnBarClose);
      } catch (Exception var11) {
         throw new Exception(L.t("Error on bar %s - %s", new Object[]{SQTime.toDateString(var1), var11.getMessage()}), var11);
      }
   }

   private void evalExiSignals(LoadedPickerData var1, long var2, int var4, Signals var5, boolean var6, byte var7, byte var8, IntOpenHashSet var9) {
      if (var5 != null && var5.exitSignalExists()) {
         PickerExitSignals var11 = var5.getExitSignals();

         for (int var12 = 0; var12 < var11.size(); var12++) {
            PickerExitSignal var13 = (PickerExitSignal)var11.get(var12);
            LoadedSymbolData var10 = var1.stockGroupData.getSymbolData(var13.symbol);
            if (var10 == null) {
               if (Stockpicker.isDebugModeEnabled()) {
                  Log.debug(String.format("Missing %s data ...", var13.symbol));
               }
            } else {
               if (var13.closeType == 4 && var7 == var8) {
                  var9.add(var13.symbol.hashCode());
               }

               this.closeOrder(var13.symbol, var13.direction, var13.closeType, var2, var4, var6, var10);
            }
         }
      }
   }

   private void evalEntrySignals(LoadedPickerData var1, long var2, int var4, Signals var5, boolean var6, IntOpenHashSet var7) throws Exception {
      if (var5 != null) {
         PickerEntrySignals var8 = var5.getLongEntrySignals();
         PickerEntrySignals var9 = var5.getShortEntrySignals();
         if (var8 != null) {
            this.evalEntrySignals(var1, var2, var4, var6, var8.list(), var9 == null ? null : var9.list(), var7);
         }

         if (var9 != null) {
            this.evalEntrySignals(var1, var2, var4, var6, var9.list(), var8 == null ? null : var8.list(), var7);
         }
      }
   }

   private void evalEntrySignals(
      LoadedPickerData var1,
      long var2,
      int var4,
      boolean var5,
      ObjectArrayList<PickerEntrySignal> var6,
      ObjectArrayList<PickerEntrySignal> var7,
      IntOpenHashSet var8
   ) throws Exception {
      for (int var15 = 0; var15 < var6.size(); var15++) {
         PickerEntrySignal var16 = (PickerEntrySignal)var6.get(var15);
         String var9 = var16.symbol;
         byte var14 = var16.getDirection();
         int var13;
         if (var14 == 1) {
            var13 = this.maxPosLong;
         } else {
            var13 = this.maxPosShort;
         }

         if (var8.contains(var16.symbol.hashCode())) {
            if (this.pickerLog.storeLogs()) {
               this.pickerLog.print(String.format("Exit signal 'End Test', do not open a new position for symbol %s.", var9));
            }

            if (this.pickerLog.storeLogs() && Stockpicker.isDebugModeEnabled()) {
               Log.debug(String.format("Exit signal 'End Test', do not open a new position for symbol %s.", var9));
            }
         } else {
            if (!this.checkMaxOpenPositions(var16.getDirection())) {
               if (this.pickerLog.storeLogs()) {
                  this.pickerLog
                     .print(
                        String.format(
                           String.format("Maximum number of open trades (orders/positions) for %s is reached %d.", Directions.typeToString(var14), var13)
                        )
                     );
               }

               if (this.pickerLog.storeLogs() && Stockpicker.isDebugModeEnabled()) {
                  Log.debug(String.format("Maximum number of open trades (orders/positions) for %s is reached %d.", Directions.typeToString(var14), var13));
               }
               break;
            }

            if (!this.checkOppositeSignals(var9, var7)) {
               if (this.pickerLog.storeLogs()) {
                  this.pickerLog
                     .print(String.format("%s Signal for symbol %s skipped - Strategy generated two opposite signals.", Directions.typeToString(var14), var9));
               }

               if (this.pickerLog.storeLogs() && Stockpicker.isDebugModeEnabled()) {
                  Log.debug("%s Signal for symbol %s skipped - Strategy generated two opposite signals.", Directions.typeToString(var14), var9);
               }
            } else if (this.checkOpenPositionsExists(var9)) {
               if (this.pickerLog.storeLogs()) {
                  this.pickerLog
                     .print(
                        String.format(
                           "Order with symbol %s already exists, cannot open another one. Only one trade (order/position) per symbol is allowed.", var9
                        )
                     );
               }

               if (this.pickerLog.storeLogs() && Stockpicker.isDebugModeEnabled()) {
                  Log.debug("Order with symbol %s already exists, cannot open another one. Only one trade (order/position) per symbol is allowed.", var9);
               }
            } else if (!this.checkOpenSlot(var9, var5)) {
               if (this.pickerLog.storeLogs()) {
                  this.pickerLog.print(String.format("Order with symbol %s was closed on SL/PT during the day, cannot open a new position on Open.", var9));
               }

               if (this.pickerLog.storeLogs() && Stockpicker.isDebugModeEnabled()) {
                  Log.debug("Order with symbol %s was closed on SL/PT during the day, cannot open a new position on Open.", var9);
               }
            } else {
               LoadedSymbolData var10 = var1.stockGroupData.getSymbolData(var9);
               if (var10 == null) {
                  if (this.pickerLog.storeLogs() && Stockpicker.isDebugModeEnabled()) {
                     Log.debug(String.format("Missing %s data ...", var9));
                  }
               } else {
                  double var11;
                  try {
                     var11 = this.computeTradeSize(var9, this.broker.getAccountBalance(), var16);
                  } catch (TradeSizeSmallerThanOneException var18) {
                     if (this.pickerLog.storeLogs()) {
                        this.pickerLog.print(String.format("Not enough money to open position %s - %s", var9, var18.getMessage()));
                     }

                     if (this.pickerLog.storeLogs() && Stockpicker.isDebugModeEnabled()) {
                        Log.debug(String.format("Not enough money to open position %s - %s", var9, var18.getMessage()));
                     }
                     continue;
                  }

                  this.submitOrder(
                     var16.type,
                     var9,
                     var11,
                     var16.price,
                     var16.sl,
                     var16.pt,
                     var16.slptValidFrom,
                     var16.barsValid,
                     var16.exitAfterBars,
                     var16.eod,
                     var2,
                     var4,
                     var5,
                     var10
                  );
               }
            }
         }
      }
   }

   private boolean checkOpenSlot(String var1, boolean var2) {
      if (var2) {
         ObjectArrayList var3 = this.broker.statsClosedPositions;

         for (int var4 = 0; var4 < var3.size(); var4++) {
            LiveOrderObj var5 = (LiveOrderObj)var3.get(var4);
            if (var5.getSymbol().equals(var1)) {
               byte var6 = var5.getCloseType();
               if (var6 == 2 || var6 == 3) {
                  return false;
               }
            }
         }
      }

      return true;
   }

   private boolean checkOppositeSignals(String var1, ObjectArrayList<PickerEntrySignal> var2) {
      if (var2 != null) {
         for (int var3 = 0; var3 < var2.size(); var3++) {
            if (((PickerEntrySignal)var2.get(var3)).symbol.equals(var1)) {
               return false;
            }
         }
      }

      return true;
   }

   private void onTick(LoadedPickerData var1, long var2, int var4, byte var5, byte var6, int var7) throws TradingException {
      if (var7 != StockBroker.TickBeforeBarOpen && var7 != StockBroker.TickOnBarOpen && var7 != StockBroker.TickOnBarClose) {
         throw new TradingException("Stockpicker.onTick(), invalid onTick sequence " + var7);
      }

      boolean var8 = var7 == StockBroker.TickBeforeBarOpen || var7 == StockBroker.TickOnBarOpen;
      boolean var9 = var7 == StockBroker.TickOnBarClose;
      ObjectArrayList var21 = this.broker.getOpenPositions();
      int var22 = var21.size();

      for (int var23 = var22 - 1; var23 >= 0; var23--) {
         LiveOrderObj var24 = (LiveOrderObj)var21.get(var23);
         String var10 = var24.getSymbol();
         LoadedSymbolData var11 = var1.stockGroupData.getSymbolData(var10);
         if (var11 == null) {
            if (Stockpicker.isDebugModeEnabled()) {
               Log.debug(String.format("Missing %s data ...", var10));
            }
         } else {
            double var12 = var8 ? var11.OpenD(var4) : var11.CloseD(var4);
            if (var12 == 0.0) {
               if (Stockpicker.isDebugModeEnabled()) {
                  Log.debug(String.format("Missing %s data on bar %s...gap or end of data ?!", var10, SQTime.toDateString(var2)));
               }
            } else {
               double var14 = var11.CloseD(var4 - 1);
               double var18 = -1.0;
               double var16 = -1.0;
               if (var7 == StockBroker.TickBeforeBarOpen) {
                  var16 = var11.OpenD(var4);
                  var18 = var11.OpenD(var4);
               } else if (var7 == StockBroker.TickOnBarOpen) {
                  var16 = var11.HighD(var4);
                  var18 = var11.LowD(var4);
               } else if (var7 == StockBroker.TickOnBarClose) {
                  if (var5 == 10 && var2 == var24.getOriginalOpenTime()) {
                     var16 = var11.CloseD(var4);
                     var18 = var11.CloseD(var4);
                  } else {
                     var16 = var11.HighD(var4);
                     var18 = var11.LowD(var4);
                  }
               }

               var14 = -1.0;
               this.tick.set(var2, 0, var10.hashCode(), var12, var12, 0.0, 0L, 0L, var8, !var8);
               this.broker.onTick(this.tick, var14, var16, var18, var5, var6, var7);
               boolean var20 = var11.getRealDataIndexEndD() == var4;
               if (var9 && var20) {
                  this.closeOrder(var10, (byte)var24.getDirection(), (byte)4, var2, var4, var8, var11);
               }
            }
         }
      }
   }

   private double computeTradeSize(String var1, double var2, PickerEntrySignal var4) throws Exception {
      StrategyBase var5 = this.tradeController.getStrategy();
      double var6 = 1.0;
      if (!this.mmMethodInitialized) {
         this.mmMethodInitialized = true;
         SettingsMap var10 = var5.getSettings();
         if (var10.getBoolean("MoneyManagement.UseFromStrategy", false)) {
            this.mmMethod = var5.getGlobalMMMethod();
         }

         if (this.mmMethod == null) {
            this.mmMethod = (MoneyManagementMethod)var10.get("MoneyManagement.Method");
         }
      }

      if (this.mmMethod != null) {
         DataInfo var13 = DataManager.getDataInfo("History", var1, true);
         InstrumentInfo var11 = var13.symbolInfo;
         var5.accountBalance = this.broker.getAccountBalance();
         var5.accountEquity = this.broker.getAccountEquity();
         double var8 = var4.sl;
         if (var8 == -9.9999999E7) {
            var8 = 0.0;
         }

         int var12 = var5.Stockpicker.getMaxOpenPositions();
         if (var5.Stockpicker.isSingleAssetStrategy()) {
            var12 = 1;
         }

         this.mmMethod.setMaxOpenPositions(var12);
         var6 = this.mmMethod.computeTradeSize(var5, var1, var4.type, var4.price, var8, var11.tickSize, var11.pointValue);
      }

      return var6;
   }

   private LiveOrderObj submitOrder(
      byte var1,
      String var2,
      double var3,
      double var5,
      double var7,
      double var9,
      int var11,
      int var12,
      int var13,
      byte var14,
      long var15,
      int var17,
      boolean var18,
      LoadedSymbolData var19
   ) throws TradingException {
      if (var19 == null) {
         if (Stockpicker.isDebugModeEnabled()) {
            Log.debug(String.format("Missing %s data ...", var2));
         }

         return null;
      } else {
         double var20 = var18 ? var19.OpenD(var17) : var19.CloseD(var17);
         if (var20 == 0.0) {
            if (Stockpicker.isDebugModeEnabled()) {
               Log.debug(String.format("Missing %s data on bar %s...gap or end of data ?!", var2, SQTime.toDateString(var15)));
            }

            return null;
         } else {
            this.tick.set(var15, 0, var2.hashCode(), var20, var20, 0.0, 0L, 0L, var18, !var18);
            this.broker.setCurrentTick(this.tick);
            StrategyBase var22 = this.tradeController.getStrategy();
            return this.broker.open(var1, var2, var3, var5, var7, var9, var11, var12, var13, var14, var18, var22.Stockpicker.entryType == 15);
         }
      }
   }

   public void closeOrder(String var1, byte var2, byte var3, long var4, int var6, boolean var7, LoadedSymbolData var8) {
      if (var8 == null) {
         if (Stockpicker.isDebugModeEnabled()) {
            Log.debug(String.format("Missing %s data ...", var1));
         }
      } else {
         double var9 = var7 ? var8.OpenD(var6) : var8.CloseD(var6);
         if (var9 == 0.0) {
            if (Stockpicker.isDebugModeEnabled()) {
               Log.debug(String.format("Missing %s data on bar %s...gap or end of data ?!", var1, SQTime.toDateString(var4)));
            }
         } else {
            this.tick.set(var4, 0, var1.hashCode(), var9, var9, 0.0, 0L, 0L, var7, !var7);
            this.broker.setCurrentTick(this.tick);
            this.broker.close(var1, var2, var3);
         }
      }
   }

   public boolean checkOpenPositionsExists(String var1) {
      ObjectArrayList var2 = this.broker.getOpenPositions();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         LiveOrderObj var4 = (LiveOrderObj)var2.get(var3);
         if (var4.getSymbol().equals(var1)) {
            return true;
         }
      }

      return false;
   }

   public void registerProgressListener(IBacktestProgressListener var1) {
      this.backtestProgressListener = var1;
   }

   public ObjectArrayList<LiveOrderObj> getOpenPositions() {
      return this.broker.getOpenPositions();
   }

   public Long2FloatRBTreeMap getWorstDailyEquity() {
      return this.broker.getWorstDailyEquity();
   }

   public Long2FloatRBTreeMap getMaxDailyDD() {
      return this.broker.getMaxDailyDDValues();
   }

   public Long2FloatRBTreeMap getMaxDailyDDPct() {
      return this.broker.getMaxDailyDDPctValues();
   }

   public double getMaxOpenDrawdown() {
      return this.broker.getMaxOpenDrawdown();
   }

   public double getMaxOpenDrawdownPct() {
      return this.broker.getMaxOpenDrawdownPct();
   }
}
