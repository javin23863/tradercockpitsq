package com.strategyquant.tradinglib.gp;

import com.strategyquant.lib.IRandomGenerator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EvolutionPipeline<T extends IGPNode> implements IEvolutionaryOperator<T>, Serializable {
   private final List<IEvolutionaryOperator<T>> pipeline;

   public EvolutionPipeline(List<IEvolutionaryOperator<T>> var1) {
      if (var1.isEmpty()) {
         throw new IllegalArgumentException("Pipeline must contain at least one operator.");
      }

      this.pipeline = new ArrayList<>(var1);
   }

   @Override
   public List<T> apply(List<T> var1, IRandomGenerator var2, AbstractFactory<T> var3, int var4, int var5) throws Exception {
      List var6 = var1;

      for (IEvolutionaryOperator var8 : this.pipeline) {
         var6 = var8.apply(var6, var2, var3, var4, var5);
      }

      int var11 = var6.size();

      for (int var12 = 0; var12 < var6.size(); var12++) {
         IGPNode var9 = (IGPNode)var6.get(var12);
         GPIDs var10 = var9.getGPIDs();
         if (var10.nodeIndex < 0) {
            var10.nodeIndex = var11++;
         }
      }

      return var6;
   }
}
