package com.strategyquant.tradinglib.engine.stockpicker.data.timeline;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.basket.StockDto;
import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.simplegrid.SimpleGridEngine;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongAVLTreeSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PickerDataTimeline {
   public static final Logger Log = LoggerFactory.getLogger(PickerDataTimeline.class);
   private int SizeD;
   private int SizeW;
   private int SizeM;
   private long[] TimeD;
   private long[] TimeW;
   private long[] TimeM;
   private BasketDto dto;
   private long dateFrom = -1L;
   private long dateTo = -1L;
   private Long2IntOpenHashMap TimeIndexD = new Long2IntOpenHashMap();
   private Long2IntOpenHashMap TimeIndexW = new Long2IntOpenHashMap();
   private Long2IntOpenHashMap TimeIndexM = new Long2IntOpenHashMap();
   Set<Long> tl = Collections.synchronizedSet(new LongAVLTreeSet());
   private int lastIndex;
   private double total;
   private double processed = 0.0;

   public void init(BasketDto var1, StopPauseEngine var2) throws Exception {
      this.dto = var1;

      try {
         this.runOnGrid(var1, var2);
         this.SizeD = this.tl.size();
         this.TimeD = new long[this.SizeD];
         int var3 = 0;

         for (long var4 : this.tl) {
            this.TimeIndexD.put(var4, var3);
            this.TimeD[var3] = var4;
            if (this.dateFrom < 0L) {
               this.dateFrom = var4;
            }

            this.dateTo = var4;
            var3++;
         }

         if (this.dateFrom >= 0L && this.dateTo >= 0L) {
            this.SizeW = SQTime.getWeeksBetween(this.dateFrom, this.dateTo) + 10;
            this.TimeW = new long[this.SizeW];
            long var10 = SQTime.setFirstDayOfWeek(this.dateFrom);
            var10 = SQTime.getDateInMs(var10);
            var3 = 0;

            do {
               this.TimeIndexW.put(var10, var3);
               this.TimeW[var3] = var10;
               var10 = SQTime.addWeeks(var10, 1);
               var3++;
            } while (var10 <= this.dateTo);

            this.TimeIndexW.put(var10, var3);
            this.TimeW[var3] = var10;
            this.SizeM = SQTime.getMonthsBetween(this.dateFrom, this.dateTo) + 10;
            this.TimeM = new long[this.SizeM];
            var10 = SQTime.setDayOfMonth(this.dateFrom, 1);
            var10 = SQTime.getDateInMs(var10);
            var3 = 0;

            do {
               this.TimeIndexM.put(var10, var3);
               this.TimeM[var3] = var10;
               var10 = SQTime.addMonths(var10, 1);
               var3++;
            } while (var10 <= this.dateTo);

            this.TimeIndexM.put(var10, var3);
            this.TimeM[var3] = var10;
            this.tl.clear();
            this.tl = null;
            Log.info(
               "Created timeline for stockgroup {} from {} to {}, size {}",
               new Object[]{var1.getName(), SQTime.toDateString(this.dateFrom), SQTime.toDateString(this.dateTo), this.SizeD}
            );
         } else {
            throw new Exception(L.t("No data found for stock group %s. Please updade data and try it again ...", new Object[]{var1.getName()}));
         }
      } catch (Exception var7) {
         throw new Exception(L.t("Error while generating picker timeline - %s", new Object[]{var7.getMessage()}), var7);
      }
   }

   private void runOnGrid(BasketDto var1, final StopPauseEngine var2) throws Exception {
      this.lastIndex = 0;
      this.processed = 0.0;
      final List var3 = BasketOfStocksManager.getInstance().getStocks(var1.getId());
      this.total = var3.size();
      SimpleGridEngine var4 = new SimpleGridEngine<TimelineCalcJob, String>(var2, null, false) {
         @Override
         protected ArrayList<TimelineCalcJob> createJobsBatch(int var1, GridClient var2x) throws Exception {
            return PickerDataTimeline.this.createBatch(var1, var3, var2);
         }

         protected void processResult(String var1, JobDetails var2x) throws Exception {
            PickerDataTimeline.this.processed++;
            Log.debug("Loaded symbols {}/{}", PickerDataTimeline.this.processed, PickerDataTimeline.this.total);
         }

         @Override
         protected void onError(String var1, Exception var2x) {
            Log.error("Timeline calc Error {}, Exception ", var1, var2x);
         }
      };
      var4.start();
      var4.unregisterListeners();
   }

   protected ArrayList<TimelineCalcJob> createBatch(int var1, List<StockDto> var2, StopPauseEngine var3) throws Exception {
      ArrayList var4 = new ArrayList();
      int var5 = 0;

      for (int var6 = this.lastIndex; var6 < this.lastIndex + var1 && !(var6 >= this.total); var6++) {
         if (var3.isStopped()) {
            throw new TaskStoppedException();
         }

         StockDto var7 = (StockDto)var2.get(var6);
         TimelineCalcJob var8 = new TimelineCalcJob(var7.getTicker(), var7.getTicker() + var6, this.tl, var3);
         var4.add(var8);
         var5++;
      }

      this.lastIndex += var5;
      return var4.size() == 0 ? null : var4;
   }

   public boolean containsTimeD(long var1) throws TradingException {
      return this.TimeIndexD.containsKey(var1);
   }

   public boolean containsTimeW(long var1) throws TradingException {
      return this.TimeIndexW.containsKey(var1);
   }

   public boolean containsTimeM(long var1) throws TradingException {
      return this.TimeIndexM.containsKey(var1);
   }

   public int timeToIndexD(long var1) throws TradingException {
      if (!this.TimeIndexD.containsKey(var1)) {
         throw new TradingException(String.format("Picker timeline->timeToIndexD() - Time not found %s.", SQTime.toFullDateTimeString(var1)));
      } else {
         return this.TimeIndexD.get(var1);
      }
   }

   public int timeToIndexW(long var1) throws TradingException {
      if (!this.TimeIndexW.containsKey(var1)) {
         throw new TradingException(String.format("Picker timeline->timeToIndexW() - Time not found %s.", SQTime.toFullDateTimeString(var1)));
      } else {
         return this.TimeIndexW.get(var1);
      }
   }

   public int timeToIndexM(long var1) throws TradingException {
      if (!this.TimeIndexM.containsKey(var1)) {
         throw new TradingException(String.format("Picker timeline->timeToIndexM() - Time not found %s.", SQTime.toFullDateTimeString(var1)));
      } else {
         return this.TimeIndexM.get(var1);
      }
   }

   public long indexToTimeD(int var1) throws TradingException {
      return var1 >= 0 && var1 < this.TimeD.length ? this.TimeD[var1] : 0L;
   }

   public long indexToTimeW(int var1) throws TradingException {
      return var1 >= 0 && var1 < this.TimeW.length ? this.TimeW[var1] : 0L;
   }

   public long indexToTimeM(int var1) throws TradingException {
      return var1 >= 0 && var1 < this.TimeM.length ? this.TimeM[var1] : 0L;
   }

   public int SizeD() {
      return this.SizeD;
   }

   public int SizeW() {
      return this.SizeW;
   }

   public int SizeM() {
      return this.SizeM;
   }

   public int searchIndexByTimeD(long var1, boolean var3) throws TradingException {
      var1 = SQTime.getDateInMs(var1);

      while (!this.TimeIndexD.containsKey(var1)) {
         var1 = SQTime.addDays(var1, var3 ? 1 : -1);
         if (!var3 && var1 < this.dateFrom || var3 && var1 > this.dateTo) {
            throw new TradingException(String.format("Picker timeline->searchIndexByTimeD() - Time not found %s.", SQTime.toFullDateTimeString(var1)));
         }
      }

      return this.TimeIndexD.get(var1);
   }

   public long dateFrom() {
      return this.dateFrom;
   }

   public long dateTo() {
      return this.dateTo;
   }

   @Override
   public String toString() {
      return String.format("Timeline %s, date from %s to %s", this.dto.getName(), SQTime.toDateString(this.dateFrom), SQTime.toDateString(this.dateTo));
   }

   public PickerDataTimeline clone() {
      PickerDataTimeline var1 = new PickerDataTimeline();
      var1.TimeD = this.TimeD != null ? (long[])this.TimeD.clone() : null;
      var1.TimeW = this.TimeW != null ? (long[])this.TimeW.clone() : null;
      var1.TimeM = this.TimeM != null ? (long[])this.TimeM.clone() : null;
      var1.TimeIndexD = new Long2IntOpenHashMap(this.TimeIndexD);
      var1.TimeIndexW = new Long2IntOpenHashMap(this.TimeIndexW);
      var1.TimeIndexM = new Long2IntOpenHashMap(this.TimeIndexM);
      return var1;
   }

   public void clear() {
   }
}
