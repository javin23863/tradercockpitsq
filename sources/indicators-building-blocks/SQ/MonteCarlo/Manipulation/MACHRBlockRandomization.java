package SQ.MonteCarlo.Manipulation;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MonteCarloManipulation;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Parameter;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "MACHR Block Randomization", display = "Randomize blocks of trades, block size #BlockSize#")
@Help(
   "Randomizes blocks of trades using bootstrap sampling to simulate robustness against market regime changes. Divides trades into blocks and then samples blocks with replacement, preserving the internal structure within each block."
)
public class MACHRBlockRandomization extends MonteCarloManipulation {
   public static final Logger Log = LoggerFactory.getLogger(MACHRBlockRandomization.class);
   @Parameter(name = "BlockSize", defaultValue = "5", minValue = 2.0, maxValue = 50.0, step = 1.0)
   @Help("Number of trades per block. Larger blocks preserve more of the original market regime structure.")
   public int BlockSize;
   @Parameter(name = "PreserveTimestamps", defaultValue = "true")
   @Help("If true, adjusts timestamps to maintain chronological order after block randomization.")
   public boolean PreserveTimestamps;

   public void modifyTrades(IRandomGenerator var1, OrdersList var2) throws Exception {
      if (var2.size() < this.BlockSize) {
         Log.warn("Not enough trades ({}) for block size ({}). Skipping manipulation.", var2.size(), this.BlockSize);
      } else {
         List var3 = this.createTradeBlocks(var2);
         if (var3.size() < 2) {
            Log.warn("Not enough blocks ({}) created. Need at least 2 blocks for meaningful randomization.", var3.size());
         } else {
            List var4 = this.randomizeBlocks(var1, var3);
            this.reconstructOrdersList(var2, var4);
            if (this.PreserveTimestamps) {
               this.fixOrderTimestamps(var2);
            }

            Log.debug("MACHR Block Randomization completed: {} trades in {} blocks", var2.size(), var3.size());
         }
      }
   }

   private List<List<Order>> createTradeBlocks(OrdersList var1) {
      ArrayList var2 = new ArrayList();
      int var3 = 0;

      while (var3 < var1.size()) {
         ArrayList var4 = new ArrayList();

         for (int var5 = var3; var5 < Math.min(var3 + this.BlockSize, var1.size()); var5++) {
            var4.add(var1.get(var5));
         }

         var2.add(var4);
         var3 += this.BlockSize;
      }

      return var2;
   }

   private List<List<Order>> randomizeBlocks(IRandomGenerator var1, List<List<Order>> var2) {
      ArrayList var3 = new ArrayList();

      for (int var4 = 0; var4 < var2.size(); var4++) {
         int var5 = var1.nextInt(var2.size());
         List var6 = (List)var2.get(var5);
         ArrayList var7 = new ArrayList();

         for (Order var9 : var6) {
            var7.add(new Order(var9));
         }

         var3.add(var7);
      }

      return var3;
   }

   private void reconstructOrdersList(OrdersList var1, List<List<Order>> var2) {
      var1.clear();

      for (List var4 : var2) {
         for (Order var6 : var4) {
            var1.add(var6);
         }
      }
   }

   private void fixOrderTimestamps(OrdersList var1) {
      if (var1.size() != 0) {
         long var2 = 0L;
         long var4 = 0L;
         int var6 = 0;

         for (int var7 = 0; var7 < var1.size(); var7++) {
            Order var8 = var1.get(var7);
            var4 += var8.CloseTime - var8.OpenTime;
            if (var7 > 0) {
               Order var9 = var1.get(var7 - 1);
               var2 += Math.abs(var8.OpenTime - var9.CloseTime);
               var6++;
            }
         }

         long var15 = var4 / var1.size();
         long var16 = var6 > 0 ? var2 / var6 : var15;
         long var11 = var1.get(0).OpenTime;

         for (int var13 = 0; var13 < var1.size(); var13++) {
            Order var14 = var1.get(var13);
            var14.OpenTime = var11;
            var14.CloseTime = var11 + (var14.CloseTime - var14.OpenTime);
            var11 = var14.CloseTime + var16;
         }
      }
   }
}
