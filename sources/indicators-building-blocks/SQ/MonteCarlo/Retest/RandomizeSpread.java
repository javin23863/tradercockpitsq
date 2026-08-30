package SQ.MonteCarlo.Retest;

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.MonteCarloRetest;
import com.strategyquant.tradinglib.Parameter;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Randomize spread", display = "Randomize spread from #Min# to #Max#")
public class RandomizeSpread extends MonteCarloRetest {
   public static final Logger Log = LoggerFactory.getLogger(RandomizeSpread.class);
   @Parameter(name = "Min", defaultValue = "1", minValue = 0.0, maxValue = 10000.0, step = 0.1)
   public double Min;
   @Parameter(name = "Max", defaultValue = "5", minValue = 0.0, maxValue = 10000.0, step = 0.1)
   public double Max;

   public RandomizeSpread() {
      super(1);
   }

   public void modifySettings(IRandomGenerator var1, SettingsMap var2) throws Exception {
      if (this.Min < 0.0) {
         this.Min = 0.0;
      }

      ChartSetup var3 = (ChartSetup)var2.get("BacktestChart");
      if (var3 != null) {
         ChartDef var4 = var3.getMainChart();
         double var5 = var4.getSpread();
         int var7 = (int)((this.Max - this.Min) / 0.1);
         int var10 = 0;

         while (true) {
            double var8 = this.Min + var1.nextInt(var7) * 0.1;
            if (var8 != var5 || var10 > 10) {
               var4.modifySpread(var8);
               this.modifyAlsoSubchartSpreads(var4.getSymbol(), var3, var8);
               break;
            }

            var10++;
         }
      }
   }

   private void modifyAlsoSubchartSpreads(String var1, ChartSetup var2, double var3) {
      ArrayList var5 = var2.getCharts();
      if (var5.size() != 1) {
         for (int var6 = 1; var6 < var5.size(); var6++) {
            ChartDef var7 = (ChartDef)var5.get(var6);
            if (var7.getSymbol().equals(var1)) {
               var7.modifySpread(var3);
            }
         }
      }
   }

   public RandomizeSpread getClone() throws Exception {
      RandomizeSpread var1 = new RandomizeSpread();
      var1.Min = this.Min;
      var1.Max = this.Max;
      var1.setParams(this.getParams());
      return var1;
   }
}
