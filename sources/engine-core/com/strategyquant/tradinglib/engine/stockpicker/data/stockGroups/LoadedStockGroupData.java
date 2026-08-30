package com.strategyquant.tradinglib.engine.stockpicker.data.stockGroups;

import com.strategyquant.datalib.basket.BasketDto;
import com.strategyquant.tradinglib.engine.stockpicker.data.symbols.LoadedSymbolData;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import java.util.Set;
import java.util.concurrent.locks.StampedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadedStockGroupData {
   public static final Logger Log = LoggerFactory.getLogger(LoadedStockGroupData.class);
   private Object2ObjectOpenHashMap<String, LoadedSymbolData> symbolsData = new Object2ObjectOpenHashMap();
   private StampedLock lock = new StampedLock();
   private long stamp = 0L;
   private BasketDto dto = null;
   private Exception loadException = null;

   public LoadedStockGroupData() {
   }

   public LoadedStockGroupData(BasketDto var1) {
      this.dto = var1;
   }

   public void clear() {
      this.symbolsData.clear();
   }

   public Exception getException() {
      return this.loadException;
   }

   public void setException(Exception var1) {
      this.loadException = var1;
   }

   public int getSymbolsCount() {
      return this.symbolsData.size();
   }

   public Set<String> getSymbols() {
      return this.symbolsData.keySet();
   }

   public void putSymbolData(String var1, LoadedSymbolData var2) {
      this.stamp = this.lock.writeLock();

      try {
         this.symbolsData.put(var1, var2);
      } finally {
         this.lock.unlock(this.stamp);
      }
   }

   public LoadedSymbolData getSymbolData(String var1) {
      return (LoadedSymbolData)this.symbolsData.get(var1);
   }

   public LoadedStockGroupData clone() {
      LoadedStockGroupData var1 = new LoadedStockGroupData();
      var1.symbolsData = new Object2ObjectOpenHashMap();
      this.symbolsData.forEach((var1x, var2) -> var1.symbolsData.put(var1x, var2 != null ? var2.clone() : null));
      var1.loadException = this.loadException;
      return var1;
   }
}
