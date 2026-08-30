package com.strategyquant.datalib.instrument;

public class InstrumentAlias {
   public String alias;
   public String instrument;
   public String description;

   public InstrumentAlias() {
   }

   public InstrumentAlias(String var1, String var2, String var3) {
      this.alias = var1;
      this.instrument = var2;
      this.description = var3;
   }
}
