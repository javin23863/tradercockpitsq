package com.strategyquant.plugin.CrossCheck.impl.MonteCarloRetest;

import com.strategyquant.tradinglib.backtestrunner.DurationStats;
import com.strategyquant.tradinglib.robustnesstests.RobustnessResults;
import java.io.Serializable;

public class MCJobResult implements Serializable {
   private RobustnessResults rr;
   private Object durationStats;
   private String dismissalMessage;
   private int dismissalReason;

   public MCJobResult(RobustnessResults var1, DurationStats var2) {
      this.rr = var1;
      this.durationStats = var2;
   }

   public MCJobResult(String var1, int var2, DurationStats var3) {
      this.dismissalMessage = var1;
      this.dismissalReason = var2;
      this.durationStats = var3;
   }

   public RobustnessResults getRR() {
      return this.rr;
   }

   public String getDismissalMessage() {
      return this.dismissalMessage;
   }
}
