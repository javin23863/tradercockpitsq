package SQ.TALibIndicators.PercentRank;

import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.talib.TALibCustomIndicator;

public class PercentRank extends TALibCustomIndicator {
   @Parameter(defaultChartIndex = 0)
   public int Chart;
   @Parameter(category = "Default", name = "Period", minValue = 2.0, maxValue = 10000.0, defaultValue = "14", step = 1.0)
   public int Period;
   @Parameter(defaultValue = "0")
   @Editor(type = 40, values = "Close=0,Open=1,High=2,Low=3,Median=4,Typical=5,Weighted=6")
   public int ComputedFrom;
   @Output(name = "PercentRank")
   public double[] Value;

   public float[][] calculate() {
      float[][] var1 = new float[1][this.in0.length];

      for (int var2 = this.Period; var2 < this.in0.length; var2++) {
         int var3 = 0;

         for (int var4 = 1; var4 <= this.Period; var4++) {
            if (this.in0[var2] > this.in0[var2 - var4]) {
               var3++;
            }
         }

         var1[0][var2] = 100.0F * ((float)var3 / this.Period);
      }

      return var1;
   }

   protected void init(SettingsMap var1, StrategyBase var2, float[] var3, float[] var4, float[] var5, float[] var6, float[] var7, float[] var8, float[] var9) {
      super.init(var1, var2, var3, var4, var5, var6, var7, var8, var9);
      this.Period = var1.getInt("TimePeriod");
   }
}
