package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.datalib.data.DataException;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.datalib.data.io.IDataLoader;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.session.Session;
import com.strategyquant.datalib.session.SessionManager;
import com.strategyquant.datalib.session.SessionStatus;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.TaskStoppedException;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeedDataCreator {
   public static final Logger Log = LoggerFactory.getLogger("FeedDataCreator");

   public void loadFeed(ArrayList<ChartDef> var1, int var2, DataLoadStats var3, IFeedDataSaver var4, LoadDataProgressEngine var5) throws Exception {
      IDataLoader[] var6 = this.initializeDataLoaders(var1, var2, var5);
      String var7 = ((ChartDef)var1.get(0)).getSession();
      Int2ObjectOpenHashMap var8 = this.createChartDefsMap(var1);
      Session var9 = SessionManager.getSession(var7).clone();
      SessionStatus var10 = new SessionStatus();
      long var11 = System.currentTimeMillis();
      VersatileData var13 = new VersatileData();
      int var14 = 0;
      long var15 = -1L;
      int var17 = -1;
      long var18 = 0L;
      long var20 = 0L;
      if (var5 != null) {
         var5.printToLog(L.t("Creating backtest data feed", new Object[0]) + "...");
      }

      Log.debug("DataLoaders: " + var6.length);
      int var22 = var6.length;
      Log.debug("FEED DATA CREATOR START");

      while (var5 == null || !var5.getProgressEngine().isStopped()) {
         var15 = -1L;
         var17 = -1;

         for (int var23 = 0; var23 < var22; var23++) {
            var14++;
            IDataLoader var24 = var6[var23];
            if (var24.hasNextTick() && (var15 == -1L || var24.nextTickTime() < var15)) {
               var17 = var23;
               var15 = var24.nextTickTime();
            }
         }

         if (var17 < 0) {
            for (IDataLoader var26 : var6) {
               var26.close();
            }

            long var33 = System.currentTimeMillis();
            if (Log.isDebugEnabled()) {
               Log.debug("Loading backtest data FEED TOOK: " + (var33 - var11) + " ms, ticks: " + var14);
               Log.debug("Very first tick time: " + SQTime.toDateMinuteString(var18));
               Log.debug("Very last tick time: " + SQTime.toDateMinuteString(var20));
            }

            if (var5 != null) {
               double var39 = (var33 - var11) / 1000.0;
               var5.printToLog(L.t("Created in %.4f s., ticks: %d", new Object[]{var39, var14}));
               var5.onDataPreparationFinished(var39, var14);
            }

            var3.loadingTime = var33 - var11;
            var3.ticks = var14;
            return;
         }

         var6[var17].getNextTick(var13);
         ChartDef var31 = (ChartDef)var8.get(var13.symbolHash);
         if (var31 != null) {
            String var35 = var31.getTimeframe();
            if (var35.equals("D1") || var35.equals("Weekly") || var35.equals("Monthly")) {
               var9.fixD1DataTime(var13);
            }
         }

         var9.checkTimeIsInSession(var13.time, var10, var31.getTimeframe());
         if (var10.isInSession) {
            boolean var36 = var6[var17].isOHLCData();
            int var25 = var6[var17].getDecimalPlaces();
            if (var10.sessionStartTime == Long.MIN_VALUE) {
               var10.sessionStartTime = SQTime.correctDayStart(var13.time);
            }

            var13.sessionStartTime = var10.sessionStartTime;
            var4.setParams(var36, var25);
            var4.saveTick(var13);
            if (var18 == 0L) {
               var18 = var13.time;
            }

            var20 = var13.time;
         }
      }

      var5.printToLog(L.t("Backtest data feed creation stopped", new Object[0]));
      double var34 = (System.currentTimeMillis() - var11) / 1000.0;
      var5.onDataPreparationFinished(var34, var14);

      for (IDataLoader var28 : var6) {
         var28.close();
      }

      throw new TaskStoppedException();
   }

   private Int2ObjectOpenHashMap<ChartDef> createChartDefsMap(ArrayList<ChartDef> var1) throws DataException {
      Int2ObjectOpenHashMap var2 = new Int2ObjectOpenHashMap();

      for (ChartDef var4 : var1) {
         int var5 = var4.getSymbolHash();
         if (var2.containsKey(var5)) {
            ChartDef var6 = (ChartDef)var2.get(var5);
            if (var6.isSmallerThan(var4)) {
               continue;
            }
         }

         var2.put(var5, var4);
      }

      return var2;
   }

   public IDataLoader[] initializeDataLoaders(ArrayList<ChartDef> var1, int var2, LoadDataProgressEngine var3) throws Exception {
      ArrayList var4 = this.getUniqueChartDefs(var1, var2, var3);
      IDataLoader[] var5 = new IDataLoader[var4.size()];

      for (int var6 = 0; var6 < var5.length; var6++) {
         var5[var6] = (IDataLoader)var4.get(var6);
      }

      return var5;
   }

   private ArrayList<IDataLoader> getUniqueChartDefs(ArrayList<ChartDef> var1, int var2, LoadDataProgressEngine var3) throws Exception {
      ArrayList var4 = new ArrayList();

      for (int var5 = 0; var5 < var1.size(); var5++) {
         ChartDef var6 = (ChartDef)var1.get(var5);
         boolean var7 = true;

         for (int var8 = 0; var8 < var1.size(); var8++) {
            if (var5 != var8) {
               ChartDef var9 = (ChartDef)var1.get(var8);
               if (var2 == 1) {
                  if (var6.canBeComputedFrom(var9)) {
                     var7 = false;
                     var9.setLoadedHistoryFrom(Math.min(var9.getLoadedHistoryFrom(), var6.getLoadedHistoryFrom()));
                     var9.setHistoryTo(Math.max(var9.getHistoryTo(), var6.getHistoryTo()));
                  }
               } else if (var8 > var5 && var6.isSameConnectionSymbol(var9)) {
                  var7 = false;
                  var9.setLoadedHistoryFrom(Math.min(var9.getLoadedHistoryFrom(), var6.getLoadedHistoryFrom()));
                  var9.setHistoryTo(Math.max(var9.getHistoryTo(), var6.getHistoryTo()));
               }
            }
         }

         if (var7) {
            IDataLoader var11 = DataManager.getDataLoader(var6, var2, var3 != null ? var3.getProgressEngine() : null);
            var11.open();
            if (var6.getHistoryTo() < var11.getDateFrom()) {
               for (IDataLoader var10 : var4) {
                  var10.close();
               }

               throw new DataException(
                  2,
                  "Date range for "
                     + var6.getConnectionName()
                     + " / "
                     + var6.getSymbol()
                     + " is incorrect! Requested range: "
                     + SQTime.toDateString(var6.getHistoryFrom())
                     + "- "
                     + SQTime.toDateString(var6.getHistoryTo())
                     + ", but the data start on "
                     + SQTime.toDateString(var11.getDateFrom())
               );
            }

            var4.add(var11);
         }
      }

      return var4;
   }
}
