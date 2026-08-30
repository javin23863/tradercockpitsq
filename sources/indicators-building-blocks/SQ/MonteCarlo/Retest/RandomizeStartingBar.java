package SQ.MonteCarlo.Retest;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.MonteCarloRetest;
import com.strategyquant.tradinglib.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Randomize starting bar", display = "Randomize starting bar, with max change #MaxChange#")
public class RandomizeStartingBar extends MonteCarloRetest {
   public static final Logger Log = LoggerFactory.getLogger(RandomizeStartingBar.class);
   @Parameter(name = "Max change", defaultValue = "100", minValue = 10.0, maxValue = 5000.0, step = 100.0)
   public int MaxChange;

   public RandomizeStartingBar() {
      super(1);
   }

   public void modifySettings(IRandomGenerator var1, SettingsMap var2) throws Exception {
      int var3 = var1.nextInt(this.MaxChange);
      var2.set("StartingBar", var3);
   }

   public RandomizeStartingBar getClone() throws Exception {
      RandomizeStartingBar var1 = new RandomizeStartingBar();
      var1.MaxChange = this.MaxChange;
      var1.setParams(this.getParams());
      return var1;
   }
}
