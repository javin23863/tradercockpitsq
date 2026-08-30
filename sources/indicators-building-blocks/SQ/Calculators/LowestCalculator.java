package SQ.Calculators;

import com.strategyquant.datalib.TradingException;

public class LowestCalculator extends AbstractCalculator {
   private int lowestIndex;
   private double lowestValue = Double.MAX_VALUE;

   public LowestCalculator(int var1) throws TradingException {
      super(var1);
   }

   @Override
   protected void calculate(double var1, boolean var3) throws TradingException {
      int var4 = this.CurrentBar < this.Period ? this.CurrentBar : this.bufferLength() - 1;
      if (var1 < this.lowestValue) {
         this.lowestIndex = 0;
         this.lowestValue = var1;
      } else if (this.lowestIndex < var4) {
         if (!var3) {
            this.lowestIndex++;
         } else if (this.lowestIndex == 0) {
            this.findLowestInBuffer(var4);
         }
      } else {
         this.findLowestInBuffer(var4);
      }
   }

   private void findLowestInBuffer(int var1) {
      this.lowestIndex = 0;
      this.lowestValue = Double.MAX_VALUE;

      for (int var2 = var1; var2 >= 0; var2--) {
         double var3 = this.bufferGet(var2);
         if (this.lowestValue > var3) {
            this.lowestValue = var3;
            this.lowestIndex = var2;
         }
      }
   }

   public double getLowestValue() {
      return this.lowestValue;
   }

   public int getLowestIndex() {
      return this.lowestIndex;
   }
}
