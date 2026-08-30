package SQ.Blocks.Indicators.ADX;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(ADX) Average Directional Movement Index", display = "ADX(@Chart@#Period#, #Line#)[#Shift#]")
@Indicator(min = 0.0, max = 100.0, step = 5.0)
@ParameterSets(
   {
         @ParameterSet(set = "Period=14,ComputeFrom=0"),
         @ParameterSet(set = "Period=20,ComputeFrom=0"),
         @ParameterSet(set = "Period=30,ComputeFrom=0"),
         @ParameterSet(set = "Period=40,ComputeFrom=0")
   }
)
public class ADX extends IndicatorBlock {
   @Parameter(category = "Default", name = "Input", defaultValue = "0")
   public ChartData Input;
   @Parameter(category = "Default", name = "Period", minValue = 2.0, maxValue = 10000.0, defaultValue = "14", step = 1.0)
   public int Period;
   @Output(name = "Main", color = "#008000")
   public DataSeries Main;
   @Output(name = "+DI", color = "#FF0000")
   public DataSeries DIPlus;
   @Output(name = "-DI", color = "#FF0000")
   public DataSeries DIMinus;
   @Buffer
   public DataSeries sumTr;
   @Buffer
   public DataSeries sumDmPlus;
   @Buffer
   public DataSeries sumDmMinus;

   protected void OnBarUpdate() throws TradingException {
      this.OnBarUpdateStandard();
   }

   private void OnBarUpdateStandard() throws TradingException {
      double var1 = this.Input.High.get(0);
      double var3 = this.Input.Low.get(0);
      double var5 = var1 - var3;
      if (this.getCurrentBar() == 0) {
         this.sumTr.set(0, var5);
         this.sumDmPlus.set(0, 0.0);
         this.sumDmMinus.set(0, 0.0);
         this.Main.set(0, 0.0);
      } else {
         double var7 = this.Input.High.get(1);
         double var9 = this.Input.Low.get(1);
         double var11 = this.Input.Close.get(1);
         double var13 = this.sumTr.get(1);
         double var15 = this.sumDmPlus.get(1);
         double var17 = this.sumDmMinus.get(1);
         double var19 = SQUtils.round(var1 - var7, 8);
         double var21 = SQUtils.round(var9 - var3, 8);
         double var23 = SQUtils.round(var1 - var11, 8);
         double var25 = SQUtils.round(var3 - var11, 8);
         double var27 = Math.max(Math.abs(var25), Math.max(var5, Math.abs(var23)));
         double var29 = var19 > var21 ? Math.max(var19, 0.0) : 0.0;
         double var31 = var21 > var19 ? Math.max(var21, 0.0) : 0.0;
         double var33 = 0.0;
         double var35 = 0.0;
         double var37 = 0.0;
         if (this.CurrentBar < this.Period) {
            var33 = SQUtils.round(var13 + var27, 8);
            var35 = var15 + var29;
            var37 = var17 + var31;
         } else {
            var33 = SQUtils.round(var13 - var13 / this.Period + var27, 8);
            var35 = var15 - var15 / this.Period + var29;
            var37 = var17 - var17 / this.Period + var31;
         }

         this.sumTr.set(0, var33);
         this.sumDmPlus.set(0, var35);
         this.sumDmMinus.set(0, var37);
         double var39 = 100.0 * (var33 == 0.0 ? 0.0 : var35 / var33);
         double var41 = 100.0 * (var33 == 0.0 ? 0.0 : var37 / var33);
         this.DIPlus.set(0, var39);
         this.DIMinus.set(0, var41);
         double var43 = Math.abs(var39 - var41);
         double var45 = SQUtils.round(var39 + var41, 8);
         this.Main.set(0, var45 == 0.0 ? 50.0 : ((this.Period - 1) * this.Main.get(1) + 100.0 * var43 / var45) / this.Period);
      }
   }

   private void OnBarUpdateMT(boolean var1) throws TradingException {
      if (this.getCurrentBar() == 0) {
         this.sumDmPlus.set(0, 0.0);
         this.sumDmMinus.set(0, 0.0);
      } else {
         double var10 = this.Input.Low.get(0);
         double var8 = this.Input.High.get(0);
         double var2 = var8 - this.Input.High.get(1);
         double var4 = this.Input.Low.get(1) - var10;
         var2 = SQUtils.round(var2, 8);
         var4 = SQUtils.round(var4, 8);
         if (var2 < 0.0) {
            var2 = 0.0;
         }

         if (var4 < 0.0) {
            var4 = 0.0;
         }

         if (var2 == var4) {
            var2 = 0.0;
            var4 = 0.0;
         } else if (var2 < var4) {
            var2 = 0.0;
         } else if (var4 < var2) {
            var4 = 0.0;
         }

         double var12 = Math.abs(var8 - var10);
         double var14 = Math.abs(var8 - this.Input.Close.get(1));
         double var16 = Math.abs(var10 - this.Input.Close.get(1));
         double var6 = Math.max(var12, var14);
         var6 = Math.max(var6, var16);
         if (var6 == 0.0) {
            this.sumDmPlus.set(0, 0.0);
            this.sumDmMinus.set(0, 0.0);
         } else {
            this.sumDmPlus.set(0, 100.0 * var2 / var6);
            this.sumDmMinus.set(0, 100.0 * var4 / var6);
         }

         double var18 = 2.0 / (this.Period + 1.0);
         this.DIPlus.set(0, this.sumDmPlus.get(0) * var18 + this.DIPlus.get(1) * (1.0 - var18));
         this.DIMinus.set(0, this.sumDmMinus.get(0) * var18 + this.DIMinus.get(1) * (1.0 - var18));
         double var20 = this.DIPlus.get(0) + this.DIMinus.get(0);
         if (var20 == 0.0) {
            this.sumTr.set(0, 0.0);
         } else {
            this.sumTr.set(0, 100.0 * (Math.abs(this.DIPlus.get(0) - this.DIMinus.get(0)) / var20));
         }

         this.Main.set(0, this.Indicators.EMA(this.sumTr, this.Period).Value.get(0));
      }
   }
}
