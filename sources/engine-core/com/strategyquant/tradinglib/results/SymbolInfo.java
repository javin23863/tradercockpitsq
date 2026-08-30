package com.strategyquant.tradinglib.results;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.lib.utils.ISQCloneable;
import java.io.Serializable;

public class SymbolInfo implements Serializable, ISQCloneable<SymbolInfo> {
   private String symbolName;
   private int symbolHash;
   private String instrumentName;
   private InstrumentInfo instrumentInfo;

   public SymbolInfo(String var1, String var2, InstrumentInfo var3) {
      this.symbolName = var1;
      this.symbolHash = var1.hashCode();
      this.instrumentName = var2;
      this.instrumentInfo = var3;
   }

   public String Symbol() {
      return this.symbolName;
   }

   public String Instrument() {
      return this.instrumentName;
   }

   public InstrumentInfo InstrumentInfo() {
      return this.instrumentInfo;
   }

   public SymbolInfo getClone() throws Exception {
      return new SymbolInfo(this.symbolName, this.instrumentName, this.instrumentInfo);
   }
}
