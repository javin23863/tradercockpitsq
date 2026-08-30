package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.bartype.BarType;
import com.strategyquant.datalib.bartype.BarTypeFactory;
import com.strategyquant.datalib.bartype.BarTypeStatus;
import com.strategyquant.datalib.bartype.impl.FuturesTimeBar;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.dataseries.TimeDataSeries;
import com.strategyquant.datalib.session.Session;
import com.strategyquant.datalib.session.SessionManager;
import com.strategyquant.datalib.session.SessionStatus;
import com.strategyquant.datalib.ticksimulator.ITickSimulator;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.execution.ReservedBarsType;
import com.strategyquant.tradinglib.simulator.ITradingSimulator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BacktestRealDataStartTimeComputer {
   public static final Logger Log = LoggerFactory.getLogger("BacktestDataFeedEngine");
   private ChartDef chartDef;
   private ITradingSimulator tradingSimulator;
   private BarType barType;
   private final BarTypeStatus barTypeStatus = new BarTypeStatus();
   private int totalBars;
   private int minimumReservedBars;
   private int preferredReservedBars;
   public TimeDataSeries Time;

   public BacktestRealDataStartTimeComputer(int var1, ChartDef var2, ITradingSimulator var3) {
      this.chartDef = var2;
      this.tradingSimulator = var3;
      ReservedBarsType var4 = var3.getReservedBarsType();
      this.minimumReservedBars = Math.max(var4.minimumReservedBars, var1);
      this.preferredReservedBars = var4.preferredReservedBars;
      Log.debug("Reserved Bars Type: LOAD_BACK - minimum: " + this.minimumReservedBars + ", preferred: " + this.preferredReservedBars);
      this.Time = new TimeDataSeries();
   }

   public void computeRealDataStartTime(StartDataInfo var1, LoadDataProgressEngine var2) throws Exception {
      Session var3 = SessionManager.getSession(this.chartDef.getSession()).clone();
      ChartDef var4 = new ChartDef(
         this.chartDef.getConnectionName(),
         this.chartDef.getSymbol(),
         this.chartDef.getTimeframe(),
         SQTime.toLong(1000, 1, 1),
         this.chartDef.getHistoryTo(),
         this.chartDef.getSpread(),
         this.chartDef.getSession()
      );
      IDataLoader var5 = DataManager.getDataLoader(var4, 1, var2 != null ? var2.getProgressEngine() : null);
      var5.open();
      this.goThroughBars(var5, var3);
      var5.close();
      if (this.Time.size() == this.minimumReservedBars) {
         var1.realDataStartTime = this.Time.get(this.minimumReservedBars - 1);
         var1.realDataEndTime = this.Time.get(0);
         var1.reservedBars = this.minimumReservedBars;
      } else if (this.Time.size() < this.preferredReservedBars) {
         var1.realDataStartTime = this.Time.get(this.Time.size() - 1);
         var1.realDataEndTime = this.Time.get(1);
         var1.reservedBars = this.Time.size() - 1;
      } else {
         var1.realDataStartTime = this.Time.get(this.preferredReservedBars);
         var1.realDataEndTime = this.Time.get(1);
         var1.reservedBars = this.preferredReservedBars;
      }
   }

   private void goThroughBars(IDataLoader var1, Session var2) throws Exception {
      ITickSimulator var3 = this.tradingSimulator.getTickSimulator();
      VersatileData var4 = new VersatileData();
      TickEvent var5 = new TickEvent();
      TickEvent var6 = new TickEvent();
      ChartData var7 = new ChartData(null, this.chartDef, 0, this.tradingSimulator.getEngineId());
      SessionStatus var8 = new SessionStatus();
      this.barType = BarTypeFactory.getBarType(this.chartDef.getTimeframe(), this.chartDef.getBarTimeType()).clone();
      String var9 = null;

      try {
         var9 = DataManager.getDataInfo("History", var7.Symbol).timeframe;
      } catch (Exception var14) {
         Log.error("Cannot get original data timeframe");
      }

      if (this.barType instanceof FuturesTimeBar) {
         if (this.tradingSimulator.getEngineId() == -659455871
            || this.tradingSimulator.getEngineId() == 395961824
            || this.tradingSimulator.getEngineId() == 1441180233) {
            ((FuturesTimeBar)this.barType).MetaTraderEngineUsed = true;
         } else if (var9 != null && var9.equals(var7.Timeframe)) {
            ((FuturesTimeBar)this.barType).LoadAsIs = true;
         }
      }

      this.totalBars = 0;

      try {
         while (var1.hasNextTick()) {
            var1.getNextTick(var4);
            var2.checkTimeIsInSession(var4.time, var8, var7.Timeframe);
            if (var8.isInSession) {
               if (var4.type == 1) {
                  var6.set(
                     var4.time, var4.connectionHash, var4.symbolHash, var4.bid, var4.ask, var4.volume, var8.sessionStartTime, var8.sessionEndTime, false, false
                  );
                  if (this.processTick(var7, var6)) {
                     return;
                  }
               } else {
                  var3.init(var4);

                  while (var3.getNextTick(var5)) {
                     var5.setSessionStartTime(var8.sessionStartTime);
                     var5.setSessionEndTime(var8.sessionEndTime);
                     var6.set(var5);
                     if (this.processTick(var7, var6)) {
                        return;
                     }
                  }
               }
            }
         }
      } finally {
         var7.destroy();
      }
   }

   private boolean processTick(ChartData var1, TickEvent var2) throws Exception {
      this.barType.processTick(var2, this.barTypeStatus, 1);
      if (this.barTypeStatus.status == 0) {
         return false;
      }

      if (this.barTypeStatus.status != 1) {
         return false;
      }

      long var3 = this.barTypeStatus.barTime;
      this.totalBars++;
      this.Time.add(var2.getTime());
      int var5 = this.Time.size();
      return var3 >= this.chartDef.getHistoryFrom() && this.totalBars >= this.minimumReservedBars;
   }

   public void destroy() {
      if (this.Time != null) {
         this.Time.destroy();
         this.Time = null;
      }
   }
}
