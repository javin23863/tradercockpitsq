package SQ.MonteCarlo.Manipulation;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.MonteCarloManipulation;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name = "Randomize trades order", display = "Randomize trades order, with method #Method#")
@Help("Randomizes order of trades using one of the methods. Please note that Resampling method is much more memory intensive than Exact method.")
public class RandomizeTradesOrder extends MonteCarloManipulation {
   public static final Logger Log = LoggerFactory.getLogger(RandomizeTradesOrder.class);
   @Parameter(name = "Method", defaultValue = "resampling")
   @Editor(type = 40, values = "Exact=exact, Resampling=resampling")
   public String Method;

   public void modifyTrades(IRandomGenerator var1, OrdersList var2) throws Exception {
      long var3 = -1L;
      long var5 = -1L;
      long var7 = 0L;
      long var9 = 0L;
      long var11 = -1L;

      for (int var13 = 0; var13 < var2.size(); var13++) {
         Order var14 = var2.get(var13);
         if (var3 == -1L || var3 > var14.OpenTime) {
            var3 = var14.OpenTime;
         }

         if (var5 == -1L || var5 < var14.CloseTime) {
            var5 = var14.CloseTime;
         }

         if (var11 != -1L) {
            var7 += Math.abs(var14.OpenTime - var11);
         }

         var11 = var14.CloseTime;
         var9 += var14.CloseTime - var14.OpenTime;
      }

      if (this.Method.toLowerCase().equals("exact")) {
         var2.shuffle();
      } else {
         int var19 = var2.size();
         OrdersList var20 = new OrdersList("RandomizeTradesOrder");

         for (int var15 = 0; var15 < var19; var15++) {
            Order var16 = var2.get(var1.nextInt(var19));
            if (!var20.contains(var16)) {
               var20.add(var16);
            } else {
               var20.add(new Order(var16));
            }
         }

         var2.clear();
         var2.addAll(var20);
         var20.clear();
      }

      var7 = (long)SQUtils.safeDivide(var7, var2.size());
      var9 = (long)SQUtils.safeDivide(var9, var2.size());
      this.fixOrderTimes(var2, var3, var5, var7, var9);
   }

   private void fixOrderTimes(OrdersList var1, long var2, long var4, long var6, long var8) {
      long var10 = var2;

      for (int var12 = 0; var12 < var1.size(); var12++) {
         Order var13 = var1.get(var12);
         var13.OpenTime = var10;
         var13.CloseTime = var13.OpenTime + var8;
         var10 += var6;
      }
   }
}
