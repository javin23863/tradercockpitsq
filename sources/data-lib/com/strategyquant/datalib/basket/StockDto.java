package com.strategyquant.datalib.basket;

import com.strategyquant.lib.SQTime;

public class StockDto {
   private Integer id;
   private String ticker;
   private Long dateFrom = -1L;
   private Long dateTo = -1L;
   public static final long TimeNotDefined = -1L;

   public boolean isActive() {
      return this.dateTo == null || this.dateTo.equals(-1L);
   }

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

   public Long getDateFrom() {
      return this.dateFrom;
   }

   public void setDateFrom(Long var1) {
      this.dateFrom = var1;
   }

   public Long getDateTo() {
      return this.dateTo;
   }

   public void setDateTo(Long var1) {
      this.dateTo = var1;
   }

   @Override
   public String toString() {
      return this.id + ";" + this.ticker + ";" + SQTime.toDateString(this.dateFrom) + ";" + (this.dateTo == -1L ? "" : SQTime.toDateString(this.dateTo));
   }
}
