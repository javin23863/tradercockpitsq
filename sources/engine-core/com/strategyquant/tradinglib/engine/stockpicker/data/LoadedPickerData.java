package com.strategyquant.tradinglib.engine.stockpicker.data;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.engine.stockpicker.data.additional.LoadedAdditionalData;
import com.strategyquant.tradinglib.engine.stockpicker.data.stockGroups.LoadedStockGroupData;
import com.strategyquant.tradinglib.engine.stockpicker.data.symbols.LoadedSymbolData;
import com.strategyquant.tradinglib.engine.stockpicker.data.timeline.PickerDataTimeline;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoadedPickerData {
   public static final Logger Log = LoggerFactory.getLogger(LoadedPickerData.class);
   public PickerDataTimeline timeline;
   public LoadedStockGroupData stockGroupData;
   public LoadedAdditionalData additionalData;
   private Exception loadException = null;

   public Exception getException() {
      return this.loadException;
   }

   public void setException(Exception var1) {
      this.loadException = var1;
   }

   public long getSymbolDataFrom(String var1) throws TradingException {
      LoadedSymbolData var2 = this.stockGroupData.getSymbolData(var1);
      int var3 = var2.getRealDataIndexStartD();
      return this.timeline.indexToTimeD(var3);
   }

   public long getSymbolDataTo(String var1) throws TradingException {
      LoadedSymbolData var2 = this.stockGroupData.getSymbolData(var1);
      int var3 = var2.getRealDataIndexEndD();
      return this.timeline.indexToTimeD(var3);
   }

   public boolean symbolDataExists(String var1) throws TradingException {
      return this.stockGroupData.getSymbolData(var1) != null;
   }

   public LoadedPickerData clone() {
      LoadedPickerData var1 = new LoadedPickerData();
      var1.timeline = this.timeline.clone();
      var1.stockGroupData = this.stockGroupData.clone();
      var1.additionalData = this.additionalData.clone();
      var1.loadException = this.loadException;
      return var1;
   }

   public void clear() {
      this.timeline.clear();
      this.stockGroupData.clear();
      this.additionalData.clear();
   }
}
