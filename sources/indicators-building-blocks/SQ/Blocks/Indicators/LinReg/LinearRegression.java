package SQ.Blocks.Indicators.LinReg;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(LinReg) Linear Regression", display = "LinReg(@Chart@#Period#)[#Shift#]", returnType = 2)
@ParameterSets(
   {
         @ParameterSet(set = "Period=13,ComputedFrom=0"),
         @ParameterSet(set = "Period=14,ComputedFrom=0"),
         @ParameterSet(set = "Period=20,ComputedFrom=0"),
         @ParameterSet(set = "Period=30,ComputedFrom=0"),
         @ParameterSet(set = "Period=40,ComputedFrom=0"),
         @ParameterSet(set = "Period=50,ComputedFrom=0")
   }
)
public class LinearRegression extends IndicatorBlock {
   @Parameter
   public DataSeries Input;
   @Parameter(defaultValue = "14")
   public int Period;
   @Output(name = "LinReg", color = "#FF0000")
   public DataSeries Value;

   protected void OnBarUpdate() throws TradingException {
      double var1 = (double)this.Period * (this.Period - 1) * 0.5;
      double var3 = var1 * var1 - (double)this.Period * this.Period * (this.Period - 1) * (2 * this.Period - 1) / 6.0;
      double var5 = 0.0;
      double var7 = 0.0;

      for (int var9 = 0; var9 < this.Period && this.CurrentBar - var9 >= 0; var9++) {
         double var10 = this.Input.get(var9);
         var5 += var9 * var10;
         var7 += var10;
      }

      double var13 = (this.Period * var5 - var1 * var7) / var3;
      double var11 = (var7 - var13 * var1) / this.Period;
      this.Value.set(0, var11 + var13 * (this.Period - 1));
   }
}
