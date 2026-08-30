package SQ.CustomAnalysis;

import com.strategyquant.tradinglib.CustomAnalysisMethod;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OutlierTrade extends CustomAnalysisMethod {
   public static final Logger Log = LoggerFactory.getLogger(OutlierTrade.class);

   public OutlierTrade() {
      super("OutlierTrade", 20);
   }

   public boolean filterStrategy(String var1, String var2, String var3, ResultsGroup var4) throws Exception {
      String var5 = this.getInputArgs();
      var5.trim();

      try {
         String[] var6 = var5.split(",");
         double var7 = Double.parseDouble(var6[0].trim());
         int var9 = Integer.parseInt(var6[1].trim());
         String var10 = var4.getName();
         Result var11 = var4.mainResult();
         String var12 = var4.getMainResultKey();
         OrdersList var13 = var4.orders().filterWithClone(var12, (byte)0, (byte)10);
         if (var13.size() == 0) {
            return false;
         }

         double var14 = 0.0;
         double var16 = 0.0;
         double var18 = 0.0;
         int var20 = 0;

         for (int var21 = 0; var21 < var13.size(); var21++) {
            Order var22 = var13.get(var21);
            if (!var22.isBalanceOrder()) {
               double var23 = var22.PL;
               if (var9 == 0) {
                  if (var23 > var14) {
                     var18 = var16;
                     var16 = var14;
                     var14 = var23;
                  } else if (var23 > var16) {
                     var18 = var16;
                     var16 = var23;
                  } else if (var23 > var18) {
                     var18 = var23;
                  }
               } else if (var9 == 1) {
                  if (var23 > var14) {
                     var18 = var16;
                     var16 = var14;
                     var14 = var23;
                  } else if (var23 > var16 && var23 != var14) {
                     var18 = var16;
                     var16 = var23;
                  } else if (var23 > var18 && var23 != var14 && var23 != var16) {
                     var18 = var23;
                  }
               }

               var20++;
            }
         }

         if (var20 > 2 && var14 > var7 * (var16 + var18)) {
            Log.debug("Biggest Order PL: {} > {} + {}", new Object[]{var14, var16, var18});
            return false;
         } else {
            return true;
         }
      } catch (Exception var25) {
         Log.error("Error OutlierTrade", var25);
         return false;
      }
   }
}
