package SQ.Blocks.Indicators.Ichimoku;

import SQ.Calculators.HighestCalculator;
import SQ.Calculators.LowestCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;

@BuildingBlock(display = "Ichimoku(@Chart@#TenkanPeriod#,#KijunPeriod#,#SenkouPeriod#,#Line#)[#Shift#]", returnType = 2)
@ParameterSet(set = "TenkanPeriod=9,KijunPeriod=26,SenkouPeriod=52")
public class Ichimoku extends IndicatorBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "9", name = "Tenkan", isPeriod = true)
   public int TenkanPeriod;
   @Parameter(defaultValue = "26", name = "Kijun", isPeriod = true)
   public int KijunPeriod;
   @Parameter(defaultValue = "52", name = "Senkou", isPeriod = true)
   public int SenkouPeriod;
   @Output(name = "TenkanSen", color = "#008000")
   public DataSeries TenkanSen;
   @Output(name = "KijunSen", color = "#008000")
   public DataSeries KijunSen;
   @Output(name = "SenkouSpanA", color = "#008000")
   public DataSeries SenkouSpanA;
   @Output(name = "SenkouSpanB", color = "#008000")
   public DataSeries SenkouSpanB;
   @Buffer
   public DataSeries SumForSenkouA;
   @Buffer
   public DataSeries SumForSenkouB;
   private HighestCalculator tenkanHighestCalculator;
   private LowestCalculator tenkanLowestCalculator;
   private HighestCalculator kijunHighestCalculator;
   private LowestCalculator kijunLowestCalculator;
   private HighestCalculator senkouHighestCalculator;
   private LowestCalculator senkouLowestCalculator;

   protected void OnInit() throws TradingException {
      this.tenkanHighestCalculator = new HighestCalculator(this.TenkanPeriod);
      this.tenkanLowestCalculator = new LowestCalculator(this.TenkanPeriod);
      this.kijunHighestCalculator = new HighestCalculator(this.KijunPeriod);
      this.kijunLowestCalculator = new LowestCalculator(this.KijunPeriod);
      this.senkouHighestCalculator = new HighestCalculator(this.SenkouPeriod);
      this.senkouLowestCalculator = new LowestCalculator(this.SenkouPeriod);
   }

   protected void OnBarUpdate() throws TradingException {
      this.tenkanHighestCalculator.onBarUpdate(this.Chart.High.get(0), this.getCurrentBar());
      this.tenkanLowestCalculator.onBarUpdate(this.Chart.Low.get(0), this.getCurrentBar());
      this.kijunHighestCalculator.onBarUpdate(this.Chart.High.get(0), this.getCurrentBar());
      this.kijunLowestCalculator.onBarUpdate(this.Chart.Low.get(0), this.getCurrentBar());
      this.senkouHighestCalculator.onBarUpdate(this.Chart.High.get(0), this.getCurrentBar());
      this.senkouLowestCalculator.onBarUpdate(this.Chart.Low.get(0), this.getCurrentBar());
      double var9 = this.Chart.Median.get(0);
      double var5 = var9;
      double var7 = var9;
      double var1;
      if (this.CurrentBar >= this.TenkanPeriod) {
         var1 = (this.tenkanHighestCalculator.getHighestValue() + this.tenkanLowestCalculator.getLowestValue()) / 2.0;
      } else {
         var1 = var9;
      }

      double var3;
      if (this.CurrentBar >= this.KijunPeriod) {
         var3 = (this.kijunHighestCalculator.getHighestValue() + this.kijunLowestCalculator.getLowestValue()) / 2.0;
      } else {
         var3 = var9;
      }

      this.SumForSenkouA.set(0, (var1 + var3) / 2.0);
      if (this.CurrentBar >= this.SenkouPeriod) {
         this.SumForSenkouB.set(0, (this.senkouHighestCalculator.getHighestValue() + this.senkouLowestCalculator.getLowestValue()) / 2.0);
      } else {
         this.SumForSenkouB.set(0, var9);
      }

      if (this.CurrentBar >= this.SenkouPeriod && this.CurrentBar >= this.KijunPeriod) {
         var5 = this.SumForSenkouA.get(this.KijunPeriod);
         var7 = this.SumForSenkouB.get(this.KijunPeriod);
      }

      this.TenkanSen.set(0, var1);
      this.KijunSen.set(0, var3);
      this.SenkouSpanA.set(0, var5);
      this.SenkouSpanB.set(0, var7);
   }
}
