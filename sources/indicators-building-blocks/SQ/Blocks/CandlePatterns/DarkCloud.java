package SQ.Blocks.CandlePatterns;

import SQ.Internal.ConditionBlock;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;

@BuildingBlock(name = "DarkCloud pattern", display = "DarkCloud pattern(@Chart@) before #Shift# bars", returnType = 3)
@Help("Is triggered when DarkCloud pattern is formed")
@OppositeBlock("PiercingLine")
@SortOrder(600)
public class DarkCloud extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "1")
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      double var1 = this.Chart.Open(this.Shift);
      double var3 = this.Chart.Open(this.Shift + 1);
      double var5 = this.Chart.Close(this.Shift);
      double var7 = this.Chart.Close(this.Shift + 1);
      double var9 = this.Chart.High(this.Shift);
      double var11 = this.Chart.Low(this.Shift);
      int var13 = this.Chart.getInstrumentInfo().decimals;
      double var14 = this.Chart.getInstrumentInfo().tickSize;
      double var16 = 0.5;
      double var18 = 10.0;
      double var20 = SQUtils.round(var9 - var11, var13);
      double var22 = SQUtils.round(var1 - var5, var13);
      double var24 = var20 != 0.0 ? SQUtils.round(var22 / var20, 6) : 0.0;
      double var26 = SQUtils.round((var3 + var7) / 2.0, var13);
      double var28 = SQUtils.round(var18 * var14, var13);
      return var7 > var3 && var26 > var5 && var1 > var5 && var5 > var3 && var24 > var16 && var20 >= var28;
   }
}
