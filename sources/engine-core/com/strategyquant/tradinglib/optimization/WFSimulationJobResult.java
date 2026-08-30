package com.strategyquant.tradinglib.optimization;

import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.backtestrunner.DurationStats;
import java.io.Serializable;

public class WFSimulationJobResult implements Serializable {
   private Object durationStats;
   private String dismissalMessage;
   private int dismissalReason;
   private OrdersList orders;
   private int index;

   public WFSimulationJobResult(OrdersList var1, int var2, DurationStats var3) {
      this.orders = var1;
      this.index = var2;
      this.durationStats = var3;
   }

   public WFSimulationJobResult(String var1, int var2, DurationStats var3) {
      this.dismissalMessage = var1;
      this.dismissalReason = var2;
      this.durationStats = var3;
   }

   public OrdersList getOrders() {
      return this.orders;
   }

   public int getIndex() {
      return this.index;
   }
}
