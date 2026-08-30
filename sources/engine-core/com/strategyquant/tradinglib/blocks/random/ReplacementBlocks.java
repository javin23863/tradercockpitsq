package com.strategyquant.tradinglib.blocks.random;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.tradinglib.generator.GenerateException;
import java.util.ArrayList;

public class ReplacementBlocks extends ArrayList<ReplacementBlock> {
   private ArrayList<ReplacementBlock> blocksWithNoChildren = new ArrayList<>();
   private int cummulativeWeight = 0;
   private int cummulativeWeightBNC = 0;

   public boolean add(ReplacementBlock var1) {
      this.cummulativeWeight = this.cummulativeWeight + var1.weight;
      if (!var1.hasBlockChildren()) {
         this.cummulativeWeightBNC = this.cummulativeWeightBNC + var1.weight;
         this.blocksWithNoChildren.add(var1);
      }

      return super.add(var1);
   }

   public ReplacementBlock chooseRandomly(IRandomGenerator var1, int var2) throws GenerateException {
      if (this.size() == 1) {
         return this.get(0);
      } else {
         return var2 > 5 && this.blocksWithNoChildren.size() > 0
            ? this.choose(var1, this.cummulativeWeightBNC, this.blocksWithNoChildren)
            : this.choose(var1, this.cummulativeWeight, this);
      }
   }

   private ReplacementBlock choose(IRandomGenerator var1, int var2, ArrayList<ReplacementBlock> var3) throws GenerateException {
      int var4 = var1.nextInt(var2);
      int var5 = 0;

      for (ReplacementBlock var7 : var3) {
         var5 += var7.weight;
         if (var4 < var5) {
            return var7;
         }
      }

      throw new GenerateException("No block found, index: " + var4 + ", cummWeight: " + var2);
   }
}
