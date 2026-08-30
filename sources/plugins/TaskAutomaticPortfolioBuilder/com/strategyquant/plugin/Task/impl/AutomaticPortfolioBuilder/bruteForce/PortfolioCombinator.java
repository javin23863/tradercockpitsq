package com.strategyquant.plugin.Task.impl.AutomaticPortfolioBuilder.bruteForce;

import com.strategyquant.tradinglib.ResultsGroup;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioCombinator {
   public static final Logger Log = LoggerFactory.getLogger("PortfolioCombinator");
   PortfolioMasterComputer computer;
   List<ResultsGroup> results;

   public PortfolioCombinator(PortfolioMasterComputer var1, List<ResultsGroup> var2) {
      this.computer = var1;
      this.results = var2;
   }

   public void combination(int var1) throws Exception {
      int var2 = this.results.size();
      if (var1 > var2) {
         throw new Exception("Invalid input, K > N");
      }

      int[] var3 = new int[var1];
      int var4 = 0;
      int var5 = 0;

      while (var4 >= 0) {
         if (var5 <= var2 + (var4 - var1)) {
            var3[var4] = var5;
            if (var4 == var1 - 1) {
               if (this.computer.isStopped()) {
                  return;
               }

               this.computer.computePortfolio(this.cloneCombination(var3));
               var5++;
            } else {
               var5 = var3[var4] + 1;
               var4++;
            }
         } else if (--var4 > 0) {
            var5 = var3[var4] + 1;
         } else {
            var5 = var3[0] + 1;
         }
      }
   }

   private int[] cloneCombination(int[] var1) {
      int[] var2 = new int[var1.length];

      for (int var3 = 0; var3 < var1.length; var3++) {
         var2[var3] = var1[var3];
      }

      return var2;
   }
}
