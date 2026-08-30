package SQ.MonteCarlo.Retest;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.MonteCarloRetest;
import com.strategyquant.tradinglib.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Randomize slippage", display = "Randomize slippage from #Min# to #Max#")
public class RandomizeSlippage extends MonteCarloRetest {
   public static final Logger Log = LoggerFactory.getLogger(RandomizeSlippage.class);
   @Parameter(name = "Min", defaultValue = "0", minValue = 0.0, maxValue = 10000.0, step = 0.1)
   public double Min;
   @Parameter(name = "Max", defaultValue = "5", minValue = 0.0, maxValue = 10000.0, step = 0.1)
   public double Max;

   public RandomizeSlippage() {
      super(1);
   }

   public void modifySettings(IRandomGenerator var1, SettingsMap var2) throws Exception {
      if (this.Min < 0.0) {
         this.Min = 0.0;
      }

      int var3 = (int)((this.Max - this.Min) / 0.1);
      double var4 = this.Min + var1.nextInt(var3) * 0.1;
      var2.set("Slippage", var4);
   }

   public RandomizeSlippage getClone() throws Exception {
      RandomizeSlippage var1 = new RandomizeSlippage();
      var1.Min = this.Min;
      var1.Max = this.Max;
      var1.setParams(this.getParams());
      return var1;
   }
}
