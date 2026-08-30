package com.strategyquant.plugin.Results.impl.PortfolioCorrelation.overlappingTrades;

import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OverlappingTradesTask implements Callable<OverlappingResult> {
   private static final Logger Log = LoggerFactory.getLogger(OverlappingTradesTask.class);
   private ResultsGroup strategy;
   private String symbol1;
   private String symbol2;
   private OverlappingTrades computer = new OverlappingTrades();
   private OrdersList orders1;
   private OrdersList orders2;

   public OverlappingTradesTask(String var1, String var2, ResultsGroup var3) {
      this.symbol1 = var1;
      this.symbol2 = var2;
      this.orders1 = null;
      this.orders2 = null;
      this.strategy = var3;
   }

   public OverlappingResult call() throws Exception {
      this.createOrdersFromStrategy();
      return this.computer.compute(this.strategy, this.symbol1, this.symbol2, this.orders1, this.orders2);
   }

   private void createOrdersFromStrategy() throws Exception {
      this.orders1 = new OrdersList("O1");
      this.orders2 = new OrdersList("O2");
      OrdersList var1 = this.strategy.orders();
      int var2 = this.symbol1.hashCode();
      int var3 = this.symbol2.hashCode();

      for (int var4 = 0; var4 < var1.size(); var4++) {
         Order var5 = var1.get(var4);
         if (var5.isRealOrder()) {
            if (var5.SetupName.hashCode() == var2) {
               this.orders1.add(var5);
            } else if (var5.SetupName.hashCode() == var3) {
               this.orders2.add(var5);
            }
         }
      }
   }
}
