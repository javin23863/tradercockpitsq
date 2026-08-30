package com.strategyquant.tradinglib.engine.stockpicker.data;

import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.datalib.basket.BasketOfStocksManager;
import com.strategyquant.datalib.basket.StockDto;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.engine.stockpicker.data.timeline.DefaultTimeline;
import com.strategyquant.tradinglib.engine.stockpicker.data.timeline.PickerDataTimeline;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.simplegrid.SimpleGridEngine;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PickerDataPreloader {
   public static final Logger Log = LoggerFactory.getLogger(PickerDataPreloader.class);
   protected static final String connectionName = "History";
   private int lastIndex;
   private long total;
   protected ConcurrentHashMap<String, String> symbolsMissing = new ConcurrentHashMap<>();
   public Set<String> uniqueSymbols = Collections.synchronizedSet(new ObjectOpenHashSet());
   public Set<String> loadedSymbols = Collections.synchronizedSet(new ObjectOpenHashSet());

   public int total() {
      return this.uniqueSymbols.size();
   }

   public int loaded() {
      return this.loadedSymbols.size();
   }

   public void clearStats() {
      this.symbolsMissing.clear();
      this.uniqueSymbols.clear();
      this.loadedSymbols.clear();
   }

   public void load() throws Exception {
      DataManager.get().refreshDataInfoCache();
      DefaultTimeline var1 = DefaultTimeline.getInstance();
      var1.generate();
      this.loadSymbolData(var1);
   }

   public void loadSymbolData(PickerDataTimeline var1) throws Exception {
      List var2 = BasketOfStocksManager.getInstance().getGroups();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         BasketDto var4 = (BasketDto)var2.get(var3);
         if (!var4.getName().equalsIgnoreCase("[[S&P 100 limited]]") && var4.isDefault()) {
            this.runOnGrid(var4, var1);
         }
      }

      this.writeMissingSymbolsToFile();
   }

   private void runOnGrid(final BasketDto var1, final PickerDataTimeline var2) throws Exception {
      final List var3 = BasketOfStocksManager.getInstance().getStocks(var1.getId());
      this.total = var3.size();
      this.lastIndex = 0;
      Log.info(String.format("Preloading %d symbols data of group %s.", this.total, var1.getName()));
      SimpleGridEngine var4 = new SimpleGridEngine<PickerDataPreloaderJob, String>(new StopPauseEngine(), null, false) {
         @Override
         protected ArrayList<PickerDataPreloaderJob> createJobsBatch(int var1x, GridClient var2x) throws Exception {
            return PickerDataPreloader.this.createBatch(var1x, var1, var3, var2);
         }

         protected void processResult(String var1x, JobDetails var2x) throws Exception {
            if (var1x != null) {
               PickerDataPreloader.this.loadedSymbols.add(var1x);
            }
         }

         @Override
         protected void onError(String var1x, Exception var2x) {
            Log.error("Data loader Error {}, Exception ", var1x, var2x);
         }
      };
      var4.start();
      var4.unregisterListeners();
   }

   protected ArrayList<PickerDataPreloaderJob> createBatch(int var1, BasketDto var2, List<StockDto> var3, PickerDataTimeline var4) throws Exception {
      ArrayList var5 = new ArrayList();
      int var6 = 0;

      while (this.lastIndex < this.total && var6 != var1) {
         StockDto var7 = (StockDto)var3.get(this.lastIndex);
         String var8 = var7.getTicker();
         if (!this.uniqueSymbols.contains(var8)) {
            this.uniqueSymbols.add(var8);
            PickerDataPreloaderJob var9 = new PickerDataPreloaderJob(var8, var8 + this.lastIndex + var2.getName(), var2, var4, this.symbolsMissing);
            var5.add(var9);
            var6++;
         }

         this.lastIndex++;
      }

      return var5.size() == 0 ? null : var5;
   }

   private void writeMissingSymbolsToFile() {
      File var1 = new File(MainApp.getDataPath() + "picker_symbols_missing.txt");
      if (var1.exists()) {
         var1.delete();
      }

      StringBuilder var2 = new StringBuilder();

      for (Entry var4 : this.symbolsMissing.entrySet()) {
         var2.append((String)var4.getKey() + "," + (String)var4.getValue() + "\n");
      }

      SQUtils.stringToFile(var1, var2.toString());
   }
}
