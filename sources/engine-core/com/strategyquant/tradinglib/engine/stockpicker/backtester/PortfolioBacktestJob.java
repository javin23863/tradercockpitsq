package com.strategyquant.tradinglib.engine.stockpicker.backtester;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.basket.StockDto;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.engine.stockpicker.Stockpicker;
import com.strategyquant.tradinglib.engine.stockpicker.backtester.log.StockpickerLog;
import com.strategyquant.tradinglib.engine.stockpicker.data.stockGroups.LoadedStockGroupData;
import com.strategyquant.tradinglib.engine.stockpicker.data.symbols.LoadedSymbolData;
import com.strategyquant.tradinglib.engine.stockpicker.signals.Signals;
import com.strategyquant.tradinglib.engine.stockpicker.signals.entry.PickerEntrySignal;
import com.strategyquant.tradinglib.engine.stockpicker.signals.exit.PickerExitSignal;
import com.strategyquant.tradinglib.project.ILastEventListener;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioBacktestJob {
   public static final Logger Log = LoggerFactory.getLogger(PortfolioBacktestJob.class);
   private final Element elStrategy;
   private final String uniqueId;
   private final int maxEntrySignalsLong;
   private final int maxEntrySignalsShort;
   private StopPauseEngine stopPauseEngine;
   private ILastEventListener lastEventListener;
   private StrategyBase strategy;
   private String symbol;
   private BacktestData data;
   private long dateFrom;
   private long dateTo;
   private StockDto si;
   private final Comparator<PickerEntrySignal> entrySignalComparatorByScore;
   private StockpickerLog pickerLog;

   public PortfolioBacktestJob(
      String var1,
      StopPauseEngine var2,
      ILastEventListener var3,
      StrategyBase var4,
      BacktestData var5,
      Element var6,
      String var7,
      long var8,
      long var10,
      StockDto var12,
      int var13,
      int var14,
      Comparator<PickerEntrySignal> var15,
      StockpickerLog var16
   ) {
      this.symbol = var1;
      this.stopPauseEngine = var2;
      this.lastEventListener = var3;
      this.strategy = var4;
      this.data = var5;
      this.elStrategy = var6;
      this.uniqueId = var7;
      this.dateFrom = var8;
      this.dateTo = var10;
      this.si = var12;
      this.maxEntrySignalsLong = var13;
      this.maxEntrySignalsShort = var14;
      this.entrySignalComparatorByScore = var15;
      this.pickerLog = var16;
   }

   public PortfolioBacktestJobResult call() throws Exception {
      try {
         long var1 = this.dateFrom;
         long var3 = this.dateTo;
         this.lastEventListener.setLastEvent(L.t("Collecting trading signals for symbol '%s'", new Object[]{this.symbol}));
         this.data.init(this.symbol);
         this.strategy.Stockpicker.setData(this.data);
         LoadedStockGroupData var5 = this.data.getStockGroupData();
         LoadedSymbolData var6 = var5.getSymbolData(this.symbol);
         long var7 = this.data.getPickerData().timeline.indexToTimeD(var6.getRealDataIndexStartD());
         long var9 = this.data.getPickerData().timeline.indexToTimeD(var6.getRealDataIndexEndD());
         if (var7 > var1) {
            var1 = var7;
         }

         if (var9 < var3) {
            var3 = var9;
         }

         long var11 = this.si.getDateFrom();
         long var13 = this.si.getDateTo();
         boolean var15 = false;
         if (var11 != -1L && var11 > var3) {
            Log.debug(
               String.format(
                  "Backtest SKIPPED!!! Symbol '%s' was listed %s later than backtest period %s - %s.",
                  this.symbol,
                  SQTime.toDateString(var11),
                  SQTime.toDateString(this.dateFrom),
                  SQTime.toDateString(this.dateTo)
               )
            );
            if (this.pickerLog.storeLogs() && Stockpicker.isDebugModeEnabled()) {
               this.pickerLog
                  .print(
                     String.format(
                        "Backtest SKIPPED!!! Symbol '%s' was listed %s later than backtest period %s - %s.",
                        this.symbol,
                        SQTime.toDateString(var11),
                        SQTime.toDateString(var1),
                        SQTime.toDateString(this.dateTo)
                     )
                  );
            }

            return null;
         } else if (var13 != -1L && var13 < var1) {
            Log.debug(
               String.format(
                  "Backtest SKIPPED!!! Symbol '%s' was delisted %s before backtest period %s - %s.",
                  this.symbol,
                  SQTime.toDateString(var13),
                  SQTime.toDateString(this.dateFrom),
                  SQTime.toDateString(this.dateTo)
               )
            );
            if (this.pickerLog.storeLogs() && Stockpicker.isDebugModeEnabled()) {
               this.pickerLog
                  .print(
                     String.format(
                        "Backtest SKIPPED!!! Symbol '%s' was delisted %s before backtest period %s - %s.",
                        this.symbol,
                        SQTime.toDateString(var13),
                        SQTime.toDateString(this.dateFrom),
                        SQTime.toDateString(this.dateTo)
                     )
                  );
            }

            return null;
         } else {
            if (var11 != -1L && var11 > var1) {
               var1 = var11;
            }

            if (var13 != -1L && var13 < var3) {
               var3 = SQTime.addDays(var13, 1);
               var15 = true;
            }

            if (var1 > var3) {
               Log.debug(
                  String.format(
                     "Backtest SKIPPED!!! Symbol '%s' has invalid date range %s - %s, start date must be smaller than end date.",
                     this.symbol,
                     SQTime.toDateString(var1),
                     SQTime.toDateString(var3)
                  )
               );
               if (this.pickerLog.storeLogs() && Stockpicker.isDebugModeEnabled()) {
                  this.pickerLog
                     .print(
                        String.format(
                           "Backtest SKIPPED!!! Symbol '%s' has invalid date range %s - %s, start date must be smaller than end date.",
                           this.symbol,
                           SQTime.toDateString(var1),
                           SQTime.toDateString(var3)
                        )
                     );
               }

               return null;
            } else {
               var1 = SQTime.getDateInMs(var1);
               var3 = SQTime.getDateInMs(var3);
               Log.debug(String.format("Backtesting symbol %s from %s to %s...", this.symbol, SQTime.toDateString(var1), SQTime.toDateString(var3)));
               if (this.pickerLog.storeLogs() && Stockpicker.isDebugModeEnabled()) {
                  this.pickerLog
                     .print(String.format("Backtesting symbol %s from %s to %s...", this.symbol, SQTime.toDateString(var1), SQTime.toDateString(var3)));
               }

               int var16 = this.data.getPickerData().timeline.searchIndexByTimeD(var1, true);
               int var17 = this.data.getPickerData().timeline.searchIndexByTimeD(var3, false);
               int var18 = 0;

               for (int var20 = var16; var20 <= var17; var20++) {
                  if (var6.dataExistsD(var20)) {
                     if (this.checkStopped()) {
                        throw new TaskStoppedException();
                     }

                     boolean var19 = var20 == var17;
                     var18++;
                     this.evaluateBar(var20, var19, var15);
                  }
               }

               return new PortfolioBacktestJobResult(this.symbol, var18);
            }
         }
      } catch (TaskStoppedException var21) {
         throw var21;
      } catch (Exception var22) {
         this.saveBacktestError(var22);
         Log.error("Error while running backtest for symbol " + this.symbol, var22);
         if (this.pickerLog.storeLogs() && Stockpicker.isDebugModeEnabled()) {
            this.pickerLog.print("Error while running backtest for symbol " + this.symbol + " - " + var22.getMessage());
         }

         if (var22.getMessage().contains("because \"this.Indicators\" is null")) {
            throw new Exception(
               L.t(
                  "Error while running backtest for symbol %s - %s - Most probably indicator used outside TA-Lib library.",
                  new Object[]{this.symbol, var22.getMessage()}
               )
            );
         } else {
            throw new Exception(L.t("Error while running backtest for symbol %s - %s", new Object[]{this.symbol, var22.getMessage()}));
         }
      }
   }

   private void evaluateBar(int var1, boolean var2, boolean var3) throws Exception {
      this.data.setCurrentBar(var1);

      try {
         if (var2 && var3) {
            this.data.Signals(this.data.getTime(0), true).addExitSignal(new PickerExitSignal(this.symbol, (byte)0, (byte)60));
            return;
         }

         this.strategy.evaluateEntryExit((byte)15);
         this.strategy.evaluateEntryExit((byte)5);
         this.strategy.evaluateEntryExit((byte)10);
         if (var2) {
            this.data.Signals(this.data.getTime(0), true).addExitSignal(new PickerExitSignal(this.symbol, (byte)0, (byte)4));
         }
      } catch (Exception var6) {
         if (!(var6 instanceof TradingException) || ((TradingException)var6).getErrorCode() != 55) {
            String var8 = SQTime.toDateString(this.strategy.Stockpicker.TimeCurrent());
            Log.info(String.format("Failed to evaluate entry/exit signals on bar %s for symbol %s - %s.", var8, this.symbol, var6.getMessage()), var6);
            throw new Exception(
               L.t("Failed to evaluate entry/exit signals on bar %s for symbol %s - %s.", new Object[]{var8, this.symbol, var6.getMessage()}), var6
            );
         }

         if (Log.isDebugEnabled()) {
            String var5 = SQTime.toDateString(this.strategy.Stockpicker.TimeCurrent());
            Log.debug(String.format("SKIPPED evaluating entry/exit signals on bar %s for symbol %s - %s.", var5, this.symbol, var6.getMessage()), var6);
         }
      }

      Signals var4 = this.data.Signals(this.data.getTime(0), false);
      if (var4 != null) {
         var4.consolidateEntryExitSignals(this.maxEntrySignalsLong, this.maxEntrySignalsShort, this.entrySignalComparatorByScore);
      }

      var4 = this.data.Signals(this.data.getTime(-1), false);
      if (var4 != null) {
         var4.consolidateEntryExitSignals(this.maxEntrySignalsLong, this.maxEntrySignalsShort, this.entrySignalComparatorByScore);
      }
   }

   private boolean checkStopped() {
      return this.stopPauseEngine.isStopped();
   }

   private void saveBacktestError(Exception var1) {
      if (!MainApp.isRelease()) {
         if (this.elStrategy != null) {
            String var2 = MainApp.getDataPath() + "/tests/stockpickergen/" + this.uniqueId + ".sqx";
            File var3 = new File(var2);
            if (var3.exists()) {
               var2 = MainApp.getDataPath() + "/tests/stockpickergen/" + this.uniqueId + ".sqx.sqx";
            }

            var3 = new File(var2);
            if (!var3.exists()) {
               try {
                  String var4 = XMLUtil.xmlToString(this.elStrategy);
                  ZipOutputStream var5 = new ZipOutputStream(new FileOutputStream(var3));
                  ZipEntry var6 = new ZipEntry("strategy_Portfolio.xml");
                  var5.putNextEntry(var6);
                  byte[] var7 = var4.getBytes();
                  var5.write(var7, 0, var7.length);
                  var5.closeEntry();
                  var5.close();
               } catch (IOException var8) {
                  var8.printStackTrace();
               }

               SQUtils.stringToFile(new File(var2 + ".txt"), SQUtils.getStackTrace(var1));
            }
         }
      }
   }
}
