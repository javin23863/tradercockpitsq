package com.strategyquant.plugin.Results.impl.PortfolioCorrelation.correlation;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.CorrelationType;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.correlation.CorrelationComputer;
import com.strategyquant.tradinglib.correlation.CorrelationPeriods;
import java.util.concurrent.Callable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CorrelationComputerTask implements Callable<CorrelationResult> {
   private static final Logger Log = LoggerFactory.getLogger(CorrelationComputerTask.class);
   private ResultsGroup strategy;
   private String symbol1;
   private String symbol2;
   private int period;
   private CorrelationType type;
   private boolean allowNegativeCorrelation;
   private boolean addEmptyPeriods;
   private CorrelationComputer computer = new CorrelationComputer();
   private OrdersList orders1;
   private OrdersList orders2;
   private CorrelationPeriods periods;

   public CorrelationComputerTask(
      ResultsGroup var1, String var2, String var3, int var4, CorrelationType var5, boolean var6, boolean var7, CorrelationPeriods var8
   ) {
      this.strategy = var1;
      this.symbol1 = var2;
      this.symbol2 = var3;
      this.orders1 = null;
      this.orders2 = null;
      this.period = var4;
      this.type = var5;
      this.allowNegativeCorrelation = var6;
      this.addEmptyPeriods = var7;
      this.periods = var8;
   }

   public CorrelationResult call() throws Exception {
      try {
         this.createOrdersFromStrategy();
         if (this.orders1 != null && this.orders1.size() != 0 && this.orders2 != null && this.orders2.size() != 0) {
            CorrelationResult var1 = new CorrelationResult(this.symbol1, this.symbol2);
            var1.value = this.computer
               .computeCorrelation(this.addEmptyPeriods, this.symbol1, this.symbol2, this.orders1, this.orders2, this.periods, this.type);
            if (var1.value == -99.0) {
               throw new Exception(L.t("Cannot get values for computing correlation.", new Object[0]));
            } else {
               return var1;
            }
         } else {
            return null;
         }
      } catch (Exception var6) {
         Log.error("Error while computing portfolio correlation.", var6);
         throw var6;
      } finally {
         this.orders1 = null;
         this.orders2 = null;
      }
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
