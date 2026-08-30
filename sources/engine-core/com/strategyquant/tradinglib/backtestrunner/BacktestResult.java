package com.strategyquant.tradinglib.backtestrunner;

import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.conditions.DismissStruct;
import java.io.Serializable;

public class BacktestResult implements Serializable {
   private ResultsGroup resultsGroup;
   private String dismissalMessage = null;
   private int dismissalReason = 0;
   private DurationStats durationStats;
   private int backtestsInJob = 0;

   public BacktestResult(ResultsGroup var1) {
      this.resultsGroup = var1;
   }

   public BacktestResult(ResultsGroup var1, DurationStats var2) {
      this.resultsGroup = var1;
      this.durationStats = var2;
   }

   public BacktestResult(String var1, int var2, DurationStats var3) {
      this.dismissalMessage = var1;
      this.dismissalReason = var2;
      this.durationStats = var3;
   }

   public BacktestResult(DismissStruct var1) {
      this.dismissalMessage = var1.dismissMessage;
      this.dismissalReason = var1.dismissReason;
   }

   public BacktestResult dismiss(String var1, int var2) {
      this.dismissalMessage = var1;
      this.dismissalReason = var2;
      return this;
   }

   public ResultsGroup getResult() {
      return this.resultsGroup;
   }

   public String getDismissalMessage() {
      return this.dismissalMessage;
   }

   public int getDismissalReason() {
      return this.dismissalReason;
   }

   public boolean isDismissed() {
      return this.dismissalMessage != null;
   }

   public DurationStats getDurationStats() {
      return this.durationStats;
   }

   public int getBacktestsInJob() {
      return this.backtestsInJob;
   }
}
