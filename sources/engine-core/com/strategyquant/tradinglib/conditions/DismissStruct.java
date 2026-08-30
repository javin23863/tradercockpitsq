package com.strategyquant.tradinglib.conditions;

public class DismissStruct {
   public String dismissMessage;
   public int dismissReason;

   public DismissStruct(int var1, String var2) {
      this.dismissReason = var1;
      this.dismissMessage = var2;
   }
}
