package com.strategyquant.datalib.historyData;

import com.strategyquant.lib.SQTime;

public class TickerRenameInfo {
   private String tickerFrom;
   private String tickerTo;
   private Long date;

   public String getTickerFrom() {
      return this.tickerFrom;
   }

   public void setTickerFrom(String var1) {
      this.tickerFrom = var1;
   }

   public String getTickerTo() {
      return this.tickerTo;
   }

   public void setTickerTo(String var1) {
      this.tickerTo = var1;
   }

   public Long getDate() {
      return this.date;
   }

   public void setDate(Long var1) {
      this.date = var1;
   }

   @Override
   public String toString() {
      return this.tickerFrom + " -> " + this.tickerTo + " at " + SQTime.toDateString(this.date);
   }
}
