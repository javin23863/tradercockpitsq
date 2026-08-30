package com.strategyquant.datalib.broker;

public class BrokerStockDto {
   private Integer id;
   private String ticker;

   public Integer getId() {
      return this.id;
   }

   public void setId(Integer var1) {
      this.id = var1;
   }

   public String getTicker() {
      return this.ticker;
   }

   public void setTicker(String var1) {
      this.ticker = var1;
   }

   @Override
   public String toString() {
      return this.id + ";" + this.ticker;
   }
}
