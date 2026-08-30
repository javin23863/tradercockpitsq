package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.tradinglib.gp.AbstractFactory;
import org.jdom2.Element;

public class OptimizationIndexesFactory extends AbstractFactory<OptimizationIndexes> {
   private OptimizationParams parameters;

   public OptimizationIndexesFactory(OptimizationParams var1) {
      this.parameters = var1;
   }

   public OptimizationIndexes generateRandomCandidate(IRandomGenerator var1) throws Exception {
      int var2 = this.parameters.getUsedCount();
      short[] var3 = new short[var2];
      int var4 = 0;

      for (int var5 = 0; var5 < this.parameters.size(); var5++) {
         Parameter var6 = this.parameters.get(var5);
         if (var6.isUsed()) {
            var3[var4] = (short)var1.nextInt(this.parameters.get(var5).getCombinationsCount());
            var4++;
         }
      }

      return new OptimizationIndexes(var3);
   }

   @Override
   public AbstractFactory<OptimizationIndexes> clone() {
      return new OptimizationIndexesFactory(this.parameters);
   }

   @Override
   public void destroy() {
   }

   @Override
   public Element mutateATM(Element var1, IRandomGenerator var2) throws Exception {
      return null;
   }
}
