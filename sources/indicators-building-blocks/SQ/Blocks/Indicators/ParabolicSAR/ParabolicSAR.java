package SQ.Blocks.Indicators.ParabolicSAR;

import SQ.Internal.IndicatorBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;
import com.strategyquant.tradinglib.simulator.Engines;

@BuildingBlock(name = "(PSAR) Parabolic SAR", display = "ParabolicSAR(@Chart@#Step#, #Maximum#)[#Shift#]", returnType = 2)
@ParameterSets({@ParameterSet(set = "Step=0.02,Maximum=0.2"), @ParameterSet(set = "Step=0.02,Maximum=0.1"), @ParameterSet(set = "Step=0.01,Maximum=0.2")})
public class ParabolicSAR extends IndicatorBlock {
   @Parameter
   public ChartData Input;
   @Parameter(defaultValue = "0.02", minValue = 0.01, maxValue = 0.6, step = 0.01, builderMinValue = 0.01, builderMaxValue = 0.4, builderStep = 0.001)
   public double Step;
   @Parameter(defaultValue = "0.2", minValue = 0.01, maxValue = 1.0, step = 0.1, builderMinValue = 0.01, builderMaxValue = 1.0, builderStep = 0.01)
   public double Maximum;
   @Output
   public DataSeries Value;
   @Buffer
   public DataSeries AFBuffer;
   @Buffer
   public DataSeries EPBuffer;
   private int ExtLastRevPos;
   private boolean ExtDirectionLong;
   private double TradeLL = Double.MAX_VALUE;
   private double TradeHH = -Double.MAX_VALUE;
   private double Af;
   private double oParOp;
   private double oParCl;
   private double prevTradeHH;
   private double prevTradeLL;

   protected void OnBarUpdate() throws TradingException {
      if (Engines.isTradestationEngine(this.Indicators.Engine)) {
         this.onBarUpdateTS();
      } else {
         this.onBarUpdateMT();
      }
   }

   private void onBarUpdateTS() throws TradingException {
      if (this.getCurrentBar() == 0) {
         this.oParOp = this.Input.High(0);
         this.ExtDirectionLong = false;
         this.TradeHH = this.Input.High(0);
         this.TradeLL = this.Input.Low(0);
      }

      double var1 = this.Input.High(0);
      double var3 = this.Input.Low(0);
      double var5 = this.Input.High(1);
      double var7 = this.Input.Low(1);
      if (var1 > this.TradeHH) {
         this.TradeHH = var1;
      }

      if (var3 < this.TradeLL) {
         this.TradeLL = var3;
      }

      this.Value.set(0, this.oParOp);
      boolean var9 = false;
      if (this.ExtDirectionLong) {
         if (var3 <= this.oParOp) {
            this.ExtDirectionLong = false;
            var9 = false;
            this.oParCl = this.TradeHH;
            this.TradeHH = var1;
            this.TradeLL = var3;
            this.Af = this.Step;
            this.oParOp = this.oParCl + this.Af * (this.TradeLL - this.oParCl);
            if (this.oParOp < var1) {
               this.oParOp = var1;
            }

            if (this.oParOp < var5) {
               this.oParOp = var5;
            }
         } else {
            this.oParCl = this.oParOp;
            if (this.TradeHH > this.prevTradeHH && this.Af < this.Maximum) {
               this.Af = Math.min(this.Af + this.Step, this.Maximum);
            }

            this.oParOp = this.oParCl + this.Af * (this.TradeHH - this.oParCl);
            if (this.oParOp > var3) {
               this.oParOp = var3;
            }

            if (this.oParOp > var7) {
               this.oParOp = var7;
            }
         }
      } else if (var1 >= this.oParOp) {
         this.ExtDirectionLong = true;
         var9 = true;
         this.oParCl = this.TradeLL;
         this.TradeHH = var1;
         this.TradeLL = var3;
         this.Af = this.Step;
         this.oParOp = this.oParCl + this.Af * (this.TradeHH - this.oParCl);
         if (this.oParOp > var3) {
            this.oParOp = var3;
         }

         if (this.oParOp > var7) {
            this.oParOp = var7;
         }
      } else {
         this.oParCl = this.oParOp;
         if (this.TradeLL < this.prevTradeLL && this.Af < this.Maximum) {
            this.Af = Math.min(this.Af + this.Step, this.Maximum);
         }

         this.oParOp = this.oParCl + this.Af * (this.TradeLL - this.oParCl);
         if (this.oParOp < var1) {
            this.oParOp = var1;
         }

         if (this.oParOp < var5) {
            this.oParOp = var5;
         }
      }

      this.prevTradeHH = this.TradeHH;
      this.prevTradeLL = this.TradeLL;
      this.Value.set(0, this.oParOp);
   }

   private void onBarUpdateMT() throws TradingException {
      if (this.CurrentBar < 3) {
         this.AFBuffer.set(0, 0.0);
         this.EPBuffer.set(0, 0.0);
         this.Value.set(0, 0.0);
      } else {
         double var1 = this.Input.High(1);
         double var3 = this.Input.High(2);
         double var5 = this.Input.Low(1);
         double var7 = this.Input.Low(2);
         double var9 = this.AFBuffer.get(1);
         double var11 = this.AFBuffer.get(2);
         double var13 = this.EPBuffer.get(1);
         double var15 = this.EPBuffer.get(2);
         double var17 = this.Value.get(1);
         if (this.CurrentBar == 3) {
            var9 = this.Step;
            var11 = this.Step;
            var13 = var5;
            var15 = var5;
            this.AFBuffer.set(2, var11);
            this.AFBuffer.set(1, var9);
            this.ExtLastRevPos = 1;
            this.ExtDirectionLong = false;
            this.Value.set(2, var3);
            this.Value.set(1, Math.max(var3, var1));
            this.EPBuffer.set(2, var15);
            this.EPBuffer.set(1, var13);
         }

         if (this.ExtDirectionLong) {
            if (var17 > var5) {
               var13 = var5;
               var9 = this.Step;
               var17 = this.getPrevHighest(this.Input.High, this.CurrentBar - this.ExtLastRevPos + 1);
               this.ExtDirectionLong = false;
               this.ExtLastRevPos = this.CurrentBar;
               this.Value.set(1, var17);
               this.EPBuffer.set(1, var13);
               this.AFBuffer.set(1, var9);
            }
         } else if (var17 < var1) {
            var13 = var1;
            var9 = this.Step;
            var17 = this.getPrevLowest(this.Input.Low, this.CurrentBar - this.ExtLastRevPos + 1);
            this.ExtDirectionLong = true;
            this.ExtLastRevPos = this.CurrentBar;
            this.Value.set(1, var17);
            this.EPBuffer.set(1, var13);
            this.AFBuffer.set(1, var9);
         }

         if (this.ExtDirectionLong) {
            if (var1 > var15 && this.ExtLastRevPos != this.CurrentBar) {
               var13 = var1;
               double var21 = var11 + this.Step;
               var9 = var21 > this.Maximum ? this.Maximum : var21;
               this.EPBuffer.set(1, var13);
               this.AFBuffer.set(1, var9);
            } else if (this.ExtLastRevPos != this.CurrentBar) {
               var9 = var11;
               var13 = var15;
               this.AFBuffer.set(1, var9);
               this.EPBuffer.set(1, var13);
            }

            double var19 = var17 + var9 * (var13 - var17);
            var19 = !(var19 > var5) && !(var19 > var7) ? var19 : Math.min(var5, var7);
            this.Value.set(0, var19);
         } else {
            if (var5 < var15 && this.ExtLastRevPos != this.CurrentBar) {
               var13 = var5;
               double var22 = var11 + this.Step;
               var9 = var22 > this.Maximum ? this.Maximum : var22;
               this.EPBuffer.set(1, var13);
               this.AFBuffer.set(1, var9);
            } else if (this.ExtLastRevPos != this.CurrentBar) {
               var9 = var11;
               var13 = var15;
               this.AFBuffer.set(1, var9);
               this.EPBuffer.set(1, var13);
            }

            double var24 = var17 + var9 * (var13 - var17);
            var24 = !(var24 < var1) && !(var24 < var3) ? var24 : Math.max(var1, var3);
            this.Value.set(0, var24);
         }
      }
   }

   private double getPrevHighest(DataSeries var1, int var2) {
      if (this.Input.Bars() < var2 + 1) {
         return 0.0;
      }

      try {
         double var3 = -Double.MAX_VALUE;

         for (int var5 = 1; var5 <= var2; var5++) {
            var3 = Math.max(var1.get(var5), var3);
         }

         return var3;
      } catch (TradingException var6) {
         return 0.0;
      }
   }

   private double getPrevLowest(DataSeries var1, int var2) {
      if (this.Input.Bars() < var2 + 1) {
         return 0.0;
      }

      try {
         double var3 = Double.MAX_VALUE;

         for (int var5 = 1; var5 <= var2; var5++) {
            var3 = Math.min(var1.get(var5), var3);
         }

         return var3;
      } catch (TradingException var6) {
         return 0.0;
      }
   }
}
