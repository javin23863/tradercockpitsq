package SQ.Blocks.Indicators.Vortex;

import SQ.Blocks.Indicators.MTATR.MTATR;
import SQ.Calculators.SumCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(VRX ) Vortex", display = "Vortex(@Chart@#Period#)[#Shift#]", returnType = 1)
@ParameterSets(
   {
         @ParameterSet(set = "Period=10"),
         @ParameterSet(set = "Period=20"),
         @ParameterSet(set = "Period=30"),
         @ParameterSet(set = "Period=40"),
         @ParameterSet(set = "Period=12"),
         @ParameterSet(set = "Period=24"),
         @ParameterSet(set = "Period=120")
   }
)
public class Vortex extends IndicatorBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "12", isPeriod = true, minValue = 2.0, maxValue = 1000.0, step = 1.0)
   public int Period;
   @Output(name = "VIPlusSumRge", color = "#0000FF")
   public DataSeries VIPlusSumRge;
   @Output(name = "VIMinusSumRge", color = "#FF0000")
   public DataSeries VIMinusSumRge;
   private SumCalculator sumVplusCalculator;
   private SumCalculator sumVminusCalculator;
   private SumCalculator sumTrCalculator;

   protected void OnInit() throws TradingException {
      this.sumVplusCalculator = new SumCalculator(this.Period);
      this.sumVminusCalculator = new SumCalculator(this.Period);
      this.sumTrCalculator = new SumCalculator(this.Period);
   }

   protected void OnBarUpdate() throws TradingException {
      MTATR var1 = this.Indicators.MTATR(this.Chart, 1);
      double var2 = var1.Value.getRounded(this.Shift);
      if (this.CurrentBar < this.Period) {
         this.VIPlusSumRge.set(0, 0.0);
         this.VIMinusSumRge.set(0, 0.0);
      } else {
         double var4 = Math.abs(this.Chart.High(0) - this.Chart.Low(1));
         double var6 = Math.abs(this.Chart.Low(0) - this.Chart.High(1));
         double var8 = var2;
         this.sumVplusCalculator.onBarUpdate(var4, this.getCurrentBar());
         this.sumVminusCalculator.onBarUpdate(var6, this.getCurrentBar());
         this.sumTrCalculator.onBarUpdate(var8, this.getCurrentBar());
         double var10 = this.sumVplusCalculator.getValue();
         double var12 = this.sumVminusCalculator.getValue();
         double var14 = this.sumTrCalculator.getValue();
         this.VIPlusSumRge.set(0, var10 / var14);
         this.VIMinusSumRge.set(0, var12 / var14);
      }
   }
}
