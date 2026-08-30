package com.strategyquant.tradinglib.results.file;

public class MissingInstrumentException extends Exception {
   private String symbol;

   public MissingInstrumentException(String var1) {
      super(
         "Instrument for symbol '" + var1 + "' cannot be recognized by InstrumentManager!\nAdd instrument alias to Instrument manager to load this strategy."
      );
      this.symbol = var1;
   }

   public String getSymbol() {
      return this.symbol;
   }
}
