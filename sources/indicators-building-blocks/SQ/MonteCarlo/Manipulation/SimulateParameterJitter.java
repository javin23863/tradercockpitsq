package SQ.MonteCarlo.Manipulation;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MonteCarloManipulation;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Parameter;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(
   name = "Simulate Parameter Jitter Effects",
   display = "Simulate Param Jitter: Skip Prob #SkipProbability#%, Adjust Prob #AdjustProbability#%, Max Price Adj #MaxPriceAdjustPercent#%"
)
@Help(
   "Simulates effects of minor strategy parameter instability by randomly skipping some trades and/or randomly adjusting the Close Price (better or worse) of others based on trade range."
)
public class SimulateParameterJitter extends MonteCarloManipulation {
   public static final Logger Log = LoggerFactory.getLogger(SimulateParameterJitter.class);
   @Parameter(name = "Skip Probability", defaultValue = "5", minValue = 0.0, maxValue = 100.0, step = 1.0)
   public int SkipProbability;
   @Parameter(name = "Adjust Probability", defaultValue = "20", minValue = 0.0, maxValue = 100.0, step = 1.0)
   public int AdjustProbability;
   @Parameter(name = "Max Price Adjust Percent", defaultValue = "15", minValue = 0.0, maxValue = 100.0, step = 1.0)
   public int MaxPriceAdjustPercent;

   public void modifyTrades(IRandomGenerator var1, OrdersList var2) throws Exception {
      if (var2 != null && !var2.isEmpty()) {
         double var3 = this.SkipProbability / 100.0;
         double var5 = this.AdjustProbability / 100.0;
         double var7 = this.MaxPriceAdjustPercent / 100.0;
         ArrayList var9 = new ArrayList();

         for (int var10 = 0; var10 < var2.size(); var10++) {
            if (var1.nextDouble() < var3) {
               var9.add(var10);
               if (Log.isTraceEnabled()) {
                  Log.trace("Trade index {} marked for removal (Skip Probability).", var10);
               }
            }
         }

         for (int var24 = var9.size() - 1; var24 >= 0; var24--) {
            int var11 = (Integer)var9.get(var24);
            var2.remove(var11);
            if (Log.isDebugEnabled()) {
               Log.debug("Removed trade originally at index {} due to simulated skip.", var11);
            }
         }

         for (int var25 = 0; var25 < var2.size(); var25++) {
            Order var26 = var2.get(var25);
            if (var26 != null && var1.nextDouble() < var5) {
               double var12 = var26.ClosePrice - var26.OpenPrice;
               if (Math.abs(var12) < 1.0E-9) {
                  if (Log.isTraceEnabled()) {
                     Log.trace("Skipping adjustment for trade {} - Open/Close price nearly identical.", var26.Ticket);
                  }
               } else {
                  double var14 = (var1.nextDouble() * 2.0 - 1.0) * var7;
                  double var16 = Math.abs(var12) * var14;
                  double var18 = var26.ClosePrice;
                  double var20;
                  if (var26.isLong()) {
                     var20 = var26.ClosePrice + var16;
                  } else {
                     if (!var26.isShort()) {
                        if (Log.isWarnEnabled()) {
                           Log.warn("Trade {} has unknown direction, skipping adjustment.", var26.Ticket);
                        }
                        continue;
                     }

                     var20 = var26.ClosePrice + var16;
                     double var22;
                     if (var26.isLong()) {
                        var22 = var16;
                     } else {
                        var22 = -var16;
                     }

                     var20 = var26.ClosePrice + var22;
                  }

                  var26.ClosePrice = (float)var20;
                  if (Log.isDebugEnabled()) {
                     Log.debug(
                        "Adjusted trade {} ({}): Factor {:.2f}%, Price Adj {:.5f}. ClosePrice {} -> {}",
                        new Object[]{var26.Ticket, var26.isLong() ? "Long" : "Short", var14 * 100.0, var16, var18, var26.ClosePrice}
                     );
                  }
               }
            }
         }
      }
   }
}
