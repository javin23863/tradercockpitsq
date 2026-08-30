package SQ.Calculators;

import com.strategyquant.datalib.TradingException;

public class StdDevCalculator extends AbstractCalculator {
   private double currentValue;
   private double curTempValue;
   private double lastTempValue;

   public StdDevCalculator(int var1) throws TradingException {
      super(var1);
   }

   @Override
   protected void calculate(double var1, boolean var3) throws TradingException {
      if (this.CurrentBar == 0) {
         this.currentValue = 0.0;
         this.lastTempValue = var1;
      } else {
         if (!var3 && this.CurrentBar > 1) {
            this.lastTempValue = this.curTempValue;
         }

         this.curTempValue = var1 + this.lastTempValue - (this.CurrentBar >= this.Period ? this.oldBufferValue : 0.0);
         double var4 = this.curTempValue / Math.min(this.CurrentBar + 1, this.Period);
         double var6 = 0.0;

         for (int var8 = Math.min(this.CurrentBar, this.Period - 1); var8 >= 0; var8--) {
            double var9 = this.bufferGet(var8);
            var6 += (var9 - var4) * (var9 - var4);
         }

         this.currentValue = Math.sqrt(var6 / Math.min(this.CurrentBar + 1, this.Period));
      }
   }

   public double getValue() {
      return this.currentValue;
   }
}
