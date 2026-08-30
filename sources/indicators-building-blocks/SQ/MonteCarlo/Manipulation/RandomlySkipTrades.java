package SQ.MonteCarlo.Manipulation;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MonteCarloManipulation;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Randomly skip trades", display = "Randomly skip trades, with probability #Probability# %")
@Help("Randomly skip trades")
public class RandomlySkipTrades extends MonteCarloManipulation {
   public static final Logger Log = LoggerFactory.getLogger(RandomlySkipTrades.class);
   @Parameter(name = "Probability", defaultValue = "10", minValue = 1.0, maxValue = 100.0, step = 1.0)
   public int Probability;

   public void modifyTrades(IRandomGenerator var1, OrdersList var2) throws Exception {
      double var3 = this.Probability / 100.0;
      int var5 = (int)Math.round(var2.size() * var3);

      for (int var6 = 0; var6 < var5; var6++) {
         int var7 = var2.size();
         if (var7 != 0) {
            int var8 = var1.nextInt(var7);
            var2.remove(var8);
         }
      }
   }
}
