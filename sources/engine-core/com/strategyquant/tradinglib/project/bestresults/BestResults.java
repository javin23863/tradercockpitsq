package com.strategyquant.tradinglib.project.bestresults;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.databank.IDatabankListener;
import com.strategyquant.tradinglib.project.SQProject;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BestResults implements IDatabankListener {
   public static final Logger Log = LoggerFactory.getLogger("BestResults");
   private BestResultOverview overview = null;
   private BestResultMiniChart chart = null;
   public byte sampleType = 127;
   private SQProject project = null;
   private Databank databank = null;
   private boolean changed = true;
   private boolean disabled = false;
   private final Lock lock = new ReentrantLock();
   private JSONArray cachedBestResultsArray = null;
   private int cachedBestResultsHash = 0;

   public BestResults(SQProject var1) {
      this.project = var1;
      this.chart = new BestResultMiniChart();
      this.overview = new BestResultOverview();
   }

   public void setSampleType(byte var1) {
      this.sampleType = var1;
   }

   public JSONArray print() {
      try {
         this.lock.lock();
         if (this.disabled) {
            return null;
         }

         if (this.databank == null && this.project.containsDatabank()) {
            this.databank = this.project.getResultsDatabank();
            this.databank.addListener(this);
         }

         if (this.databank != null && this.changed) {
            JSONArray var1 = new JSONArray();
            ResultsGroup[] var2 = this.databank.getTopResults();
            int var3 = this.computeHash(var2);
            if (var3 == this.cachedBestResultsHash && this.cachedBestResultsArray != null) {
               this.changed = false;
               return this.cachedBestResultsArray;
            }

            for (int var4 = 0; var4 < 3; var4++) {
               JSONObject var5 = new JSONObject();
               ResultsGroup var6 = null;

               try {
                  var6 = var2[var4];
               } catch (Exception var11) {
               }

               var5.put("ranking", var4);
               if (var6 == null) {
                  var5.put("strategy", "");
               } else {
                  var5.put("strategy", var6.getName());
                  var5.put("overview", this.overview.print(var6, this.sampleType));
                  var5.put("chart", this.chart.print(var6, this.sampleType));
               }

               var1.put(var5);
            }

            this.cachedBestResultsArray = var1;
            this.cachedBestResultsHash = var3;
            this.changed = false;
            return var1;
         } else {
            return null;
         }
      } finally {
         this.lock.unlock();
      }
   }

   private int computeHash(ResultsGroup[] var1) {
      int var2 = 0;

      for (int var3 = 0; var3 < var1.length; var3++) {
         ResultsGroup var4 = null;

         try {
            var4 = var1[var3];
         } catch (Exception var6) {
         }

         if (var4 != null) {
            var2 += SQUtils.betterHashCode(var4.getName());
            var2 += this.getStatsValue(var4, (byte)11, "NetProfit");
            var2 += this.getStatsValue(var4, (byte)40, "NetProfit");
            var2 += this.getStatsValue(var4, (byte)20, "NetProfit");
         }
      }

      return var2;
   }

   private int getStatsValue(ResultsGroup var1, byte var2, String var3) {
      try {
         SQStats var4 = var1.portfolio().statsOrNull((byte)0, (byte)10, (byte)20);
         return var4 == null ? 0 : (int)var4.getDouble(var3, 0.0);
      } catch (Exception var5) {
         Log.error("Cannot get top result stats", var5);
         return 0;
      }
   }

   @Override
   public void databankChanged(Databank var1) {
      this.changed = true;
   }

   public boolean hasChanged() {
      return this.changed;
   }

   public void resetLastData() {
      this.changed = true;
      this.cachedBestResultsHash = 0;
      this.cachedBestResultsArray = null;
   }

   public void enable() {
      this.disabled = false;
   }

   public void disable() {
      this.disabled = true;
   }
}
