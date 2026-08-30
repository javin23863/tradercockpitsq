package SQ.MonteCarlo.Retest;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.MonteCarloRetest;
import com.strategyquant.tradinglib.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Randomize min distance", display = "Randomize min distance from price from #Min# to #Max#")
public class RandomizeMinDistance extends MonteCarloRetest {
   public static final Logger Log = LoggerFactory.getLogger(RandomizeMinDistance.class);
   @Parameter(name = "Min", defaultValue = "0", minValue = 0.0, maxValue = 100.0, step = 0.1)
   public double Min;
   @Parameter(name = "Max", defaultValue = "10", minValue = 0.0, maxValue = 100.0, step = 0.1)
   public double Max;

   public RandomizeMinDistance() {
      super(1);
   }

   public void modifySettings(IRandomGenerator var1, SettingsMap var2) throws Exception {
      if (this.Min < 0.0) {
         this.Min = 0.0;
      }

      int var3 = (int)((this.Max - this.Min) / 0.1);
      double var4 = this.Min + var1.nextInt(var3) * 0.1;
      var2.set("MinDistance", var4);
   }

   public RandomizeMinDistance getClone() throws Exception {
      RandomizeMinDistance var1 = new RandomizeMinDistance();
      var1.Min = this.Min;
      var1.Max = this.Max;
      var1.setParams(this.getParams());
      return var1;
   }
}
