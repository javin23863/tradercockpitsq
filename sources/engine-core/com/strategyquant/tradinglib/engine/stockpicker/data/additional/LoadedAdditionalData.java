package com.strategyquant.tradinglib.engine.stockpicker.data.additional;

import com.strategyquant.tradinglib.engine.stockpicker.data.symbols.LoadedSymbolData;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;

public class LoadedAdditionalData extends ObjectArrayList<LoadedSymbolData> {
   private Exception loadException = null;

   public void setException(Exception var1) {
      this.loadException = var1;
   }

   public Exception getException() {
      return this.loadException;
   }

   public LoadedAdditionalData clone() {
      LoadedAdditionalData var1 = new LoadedAdditionalData();
      ObjectListIterator var2 = this.iterator();

      while (var2.hasNext()) {
         LoadedSymbolData var3 = (LoadedSymbolData)var2.next();
         var1.add(var3 != null ? var3.clone() : null);
      }

      var1.loadException = this.loadException;
      return var1;
   }
}
