package SQ.MonteCarlo.Manipulation;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MonteCarloManipulation;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(
   name = "Randomly degrade execution price",
   display = "Degrade close price for #Probability#% of trades, by max #MaxDegradationPercent#% of price range"
)
@Help(
   "Simulates execution issues like slippage or wider spreads by randomly worsening the Close Price for a subset of trades, based on a percentage of the trade's price range (Open to original Close)."
)
public class RandomlyDegradeExecution extends MonteCarloManipulation {
   public static final Logger Log = LoggerFactory.getLogger(RandomlyDegradeExecution.class);
   @Parameter(name = "Probability", defaultValue = "15", minValue = 1.0, maxValue = 100.0, step = 1.0)
   public int Probability;
   @Parameter(name = "Max Degradation Percent", defaultValue = "25", minValue = 0.0, maxValue = 100.0, step = 1.0)
   public int MaxDegradationPercent;

   public void modifyTrades(IRandomGenerator var1, OrdersList var2) throws Exception {
      double var3 = this.Probability / 100.0;
      double var5 = this.MaxDegradationPercent / 100.0;
      if (!(var5 <= 0.0)) {
         for (int var7 = 0; var7 < var2.size(); var7++) {
            if (var1.nextDouble() < var3) {
               Order var8 = var2.get(var7);
               double var9 = var8.ClosePrice - var8.OpenPrice;
               if (Math.abs(var9) < 1.0E-9) {
                  if (Log.isTraceEnabled()) {
                     Log.trace("Skipping degradation for trade {} as OpenPrice equals ClosePrice.", var8.Ticket);
                  }
               } else {
                  double var11 = var1.nextDouble() * var5;
                  double var13 = Math.abs(var9) * var11;
                  double var15 = var8.ClosePrice;
                  double var17;
                  if (var8.isLong()) {
                     var17 = var8.ClosePrice - var13;
                  } else {
                     if (!var8.isShort()) {
                        if (Log.isWarnEnabled()) {
                           Log.warn("Trade {} has unknown direction, cannot apply degradation.", var8.Ticket);
                        }
                        continue;
                     }

                     var17 = var8.ClosePrice + var13;
                  }

                  var8.ClosePrice = (float)var17;
                  if (Log.isDebugEnabled()) {
                     Log.debug(
                        "Degraded trade {} ({}): Price range {:.5f}, Factor {:.2f}%, Adjustment {:.5f}. ClosePrice changed from {:.5f} to {:.5f}",
                        new Object[]{
                           var8.Ticket, var8.isLong() ? "Long" : "Short", var9, var11 * 100.0, var8.isLong() ? -var13 : var13, var15, var8.ClosePrice
                        }
                     );
                  }
               }
            }
         }
      }
   }
}
