package SQ.Blocks.Indicators.SuperTrend;

import SQ.Blocks.Indicators.ATR.ATR;
import SQ.Blocks.Indicators.MTATR.MTATR;
import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;
import com.strategyquant.tradinglib.simulator.Engines;

@BuildingBlock(name = "(ST) SuperTrend", display = "SuperTrend(@Chart@#Mode#,#ATRPeriod#,#ATRMult#)[#Shift#]", returnType = 2)
@Help("SuperTrend help text")
@ParameterSets(
   {
         @ParameterSet(set = "ATRPeriod=12"),
         @ParameterSet(set = "ATRPeriod=24"),
         @ParameterSet(set = "ATRPeriod=48"),
         @ParameterSet(set = "ATRPeriod=120"),
         @ParameterSet(set = "ATRPeriod=480"),
         @ParameterSet(set = "ATRPeriod=10"),
         @ParameterSet(set = "ATRPeriod=20"),
         @ParameterSet(set = "ATRPeriod=40"),
         @ParameterSet(set = "ATRPeriod=100"),
         @ParameterSet(set = "ATRPeriod=500")
   }
)
public class SuperTrend extends IndicatorBlock {
   @Parameter
   public ChartData Input;
   @Parameter(defaultValue = "1")
   @Editor(type = 40, values = "Basic=1")
   public int Mode;
   @Parameter(defaultValue = "24", isPeriod = true, minValue = 2.0, maxValue = 240.0, step = 1.0)
   public int ATRPeriod;
   @Parameter(defaultValue = "3", isPeriod = true, minValue = 0.5, maxValue = 10.0, step = 0.1)
   public double ATRMult;
   @Output
   public DataSeries Value;

   protected void OnBarUpdate() throws TradingException {
      if (Engines.isTradestationEngine(this.Indicators.Engine)) {
         this.onBarUpdateTS();
      } else {
         this.onBarUpdateMT();
      }
   }

   protected void onBarUpdateMT() throws TradingException {
      MTATR var1 = this.Indicators.MTATR(this.Input, this.ATRPeriod);
      double var2 = var1.Value.getRounded(0);
      double var4 = (this.Input.High.get(0) + this.Input.Low.get(0)) / 2.0 + this.ATRMult * var2;
      double var6 = (this.Input.High.get(0) + this.Input.Low.get(0)) / 2.0 - this.ATRMult * var2;
      if (this.Input.Close.get(0) > this.Value.get(1) && this.Input.Close.get(1) <= this.Value.get(1)) {
         this.Value.set(0, var6);
      } else if (this.Input.Close.get(0) < this.Value.get(1) && this.Input.Close.get(1) >= this.Value.get(1)) {
         this.Value.set(0, var4);
      } else if (this.Value.get(1) < var6) {
         this.Value.set(0, var6);
      } else if (this.Value.get(1) > var4) {
         this.Value.set(0, var4);
      } else {
         this.Value.set(0, this.Value.get(1));
      }
   }

   protected void onBarUpdateTS() throws TradingException {
      ATR var1 = this.Indicators.ATR(this.Input, this.ATRPeriod);
      double var2 = var1.Value.getRounded(0);
      double var4 = (this.Input.High.get(0) + this.Input.Low.get(0)) / 2.0 + this.ATRMult * var2;
      double var6 = (this.Input.High.get(0) + this.Input.Low.get(0)) / 2.0 - this.ATRMult * var2;
      if (this.Input.Close.get(0) > this.Value.get(1) && this.Input.Close.get(1) <= this.Value.get(1)) {
         this.Value.set(0, var6);
      } else if (this.Input.Close.get(0) < this.Value.get(1) && this.Input.Close.get(1) >= this.Value.get(1)) {
         this.Value.set(0, var4);
      } else if (this.Value.get(1) < var6) {
         this.Value.set(0, var6);
      } else if (this.Value.get(1) > var4) {
         this.Value.set(0, var4);
      } else {
         this.Value.set(0, this.Value.get(1));
      }
   }
}
