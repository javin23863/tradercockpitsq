package SQ.Blocks.Indicators.Stochastic;

import SQ.Calculators.AverageCalculator;
import SQ.Calculators.HighestCalculator;
import SQ.Calculators.LowestCalculator;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(STOCH) Stochastic", display = "Stoch(@Chart@#KPeriod#, #DPeriod#, #Slowing#).#Line#[#Shift#]", returnType = 1)
@Indicator(oscillator = true, middleValue = 50.0, min = 0.0, max = 100.0, step = 1.0)
@ParameterSets(
   {
         @ParameterSet(set = "KPeriod=5,DPeriod=3,Slowing=3,MAMethod=0,PriceField=0"),
         @ParameterSet(set = "KPeriod=14,DPeriod=3,Slowing=3,MAMethod=0,PriceField=0"),
         @ParameterSet(set = "KPeriod=21,DPeriod=7,Slowing=7,MAMethod=0,PriceField=0")
   }
)
public class Stochastic extends IndicatorBlock {
   @Parameter(defaultChartIndex = 0)
   public ChartData Input;
   @Parameter(name = "%K Period", defaultValue = "9", minValue = 2.0, maxValue = 10000.0, step = 1.0, isPeriod = true)
   public int KPeriod;
   @Parameter(name = "%D Period", defaultValue = "3", minValue = 2.0, maxValue = 10000.0, step = 1.0, isPeriod = true)
   public int DPeriod;
   @Parameter(defaultValue = "3", minValue = 2.0, maxValue = 10000.0, step = 1.0, isPeriod = true)
   public int Slowing;
   @Parameter(name = "MA Method", defaultValue = "0")
   @Editor(type = 40, values = "Simple=0,Exponential=1,Smoothed=2,Linear weighted=3")
   public int MAMethod;
   @Parameter(defaultValue = "0")
   @Editor(type = 40, values = "Low/High=0,Close/Close=1")
   public int PriceField;
   @Output(name = "Fast %K", color = "#008000")
   public DataSeries FastK;
   @Output(name = "Slow %D", color = "#FF0000")
   public DataSeries SlowD;
   private HighestCalculator highestCalculator;
   private LowestCalculator lowestCalculator;
   private AverageCalculator fastKCalculator;
   private AverageCalculator slowDCalculator;
   private double curK;
   private double lastK;

   protected void OnInit() throws TradingException {
      this.MAMethod = SQUtils.fixAllowedRange(this.MAMethod, 0, 3, 0);
      this.highestCalculator = new HighestCalculator(this.KPeriod);
      this.lowestCalculator = new LowestCalculator(this.KPeriod);
      this.fastKCalculator = new AverageCalculator(this.MAMethod, this.Slowing);
      this.slowDCalculator = new AverageCalculator(this.MAMethod, this.DPeriod);
   }

   protected void OnBarUpdate() throws TradingException {
      this.OnBarUpdateStandard();
   }

   private void OnBarUpdateStandard() throws TradingException {
      this.PriceField = SQUtils.fixAllowedRange(this.PriceField, 0, 1, 0);
      switch (this.PriceField) {
         case 0:
            this.highestCalculator.onBarUpdate(this.Input.High.get(0), this.getCurrentBar());
            this.lowestCalculator.onBarUpdate(this.Input.Low.get(0), this.getCurrentBar());
            break;
         case 1:
            this.highestCalculator.onBarUpdate(this.Input.Close.get(0), this.getCurrentBar());
            this.lowestCalculator.onBarUpdate(this.Input.Close.get(0), this.getCurrentBar());
      }

      double var1 = SQUtils.round(this.Input.Close.get(0) - this.lowestCalculator.getLowestValue(), 8);
      double var3 = SQUtils.round(this.highestCalculator.getHighestValue() - this.lowestCalculator.getLowestValue(), 8);
      this.lastK = this.curK;
      if (var3 < 1.0E-8 && var3 > -1.0E-8) {
         this.curK = this.CurrentBar == 0 ? 50.0 : this.lastK;
      } else {
         this.curK = Math.min(100.0, Math.max(0.0, 100.0 * var1 / var3));
      }

      this.fastKCalculator.onBarUpdate(this.curK, this.getCurrentBar());
      this.slowDCalculator.onBarUpdate(this.fastKCalculator.getValue(), this.getCurrentBar());
      this.FastK.set(0, this.fastKCalculator.getValue());
      this.SlowD.set(0, this.slowDCalculator.getValue());
   }
}
