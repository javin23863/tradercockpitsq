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

@BuildingBlock(name = "ShootingStar pattern", display = "ShootingStar pattern(@Chart@) before #Shift# bars", returnType = 3)
@Help("Is triggered when ShootingStar pattern is formed")
@OppositeBlock("Hammer")
@SortOrder(300)
public class ShootingStar extends ConditionBlock {
   @Parameter
   public ChartData Chart;
   @Parameter(defaultValue = "1")
   public int Shift;

   @Override
   public boolean OnBlockEvaluate() throws TradingException {
      double var1 = this.Chart.getInstrumentInfo().tickSize;
      int var3 = this.Chart.getInstrumentInfo().decimals;
      double var4 = this.Chart.High(this.Shift);
      double var6 = this.Chart.Low(this.Shift);
      double var8 = this.Chart.High(this.Shift + 1);
      double var10 = this.Chart.High(this.Shift + 2);
      double var12 = this.Chart.High(this.Shift + 3);
      double var14 = this.Chart.Open(this.Shift);
      double var16 = this.Chart.Close(this.Shift);
      double var18 = SQUtils.round(var4 - var6, var3);
      double var24 = 0.9;
      double var26 = 12.0;
      double var20;
      double var22;
      if (var14 > var16) {
         var22 = var14;
         var20 = var16;
      } else {
         var22 = var16;
         var20 = var14;
      }

      double var28 = SQUtils.round(var20 - var6, var3);
      double var30 = SQUtils.round(var4 - var22, var3);
      double var32 = SQUtils.round(Math.abs(var14 - var16), var3);
      double var34 = SQUtils.round(var32 * var24, var3);
      double var36 = SQUtils.round(var30 / 2.0, var3);
      double var38 = SQUtils.round(var30 / 3.0, var3);
      double var40 = SQUtils.round(var30 / 4.0, var3);
      double var42 = SQUtils.round(2.0 * var34, var3);
      double var44 = SQUtils.round(var26 * var1, var3);
      if (var4 >= var8 && var4 > var10 && var4 > var12) {
         if (var36 > var28 && var30 > var42 && var18 >= var44 && var14 != var16 && var38 <= var28 && var40 <= var28) {
            return true;
         }

         if (var38 > var28 && var30 > var42 && var18 >= var44 && var14 != var16 && var40 <= var28) {
            return true;
         }

         if (var40 > var28 && var30 > var42 && var18 >= var44 && var14 != var16) {
            return true;
         }
      }

      return false;
   }
}
