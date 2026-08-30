package com.strategyquant.tradinglib.setup;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.utils.IUniqueNameChecker;
import com.strategyquant.tradinglib.engine.TradingSetup;
import java.util.ArrayList;

public class Portfolio {
   private ArrayList<TradingSetup> tradingSetups = null;

   public Portfolio() {
   }

   public Portfolio(TradingSetup var1) throws Exception {
      this.add(var1);
   }

   public String add(TradingSetup var1) throws Exception {
      if (this.tradingSetups == null) {
         this.tradingSetups = new ArrayList<>();
      }

      String var2 = var1.getName();
      if (var2 == null) {
         var2 = var1.getStrategy().getStrategyName();
      }

      var2 = SQUtils.generateUniqueName(var2, new IUniqueNameChecker() {
         public boolean checkNameExist(String var1) {
            for (TradingSetup var3 : Portfolio.this.tradingSetups) {
               if (var3.getName().equals(var1)) {
                  return true;
               }
            }

            return false;
         }
      });
      var1.setName(var2);
      this.tradingSetups.add(var1);
      var1.getStrategy().setSetupName(var2);
      return var2;
   }

   public ArrayList<TradingSetup> getTradingSetups() {
      return this.tradingSetups;
   }

   public TradingSetup getFirstTradingSetup() {
      return this.tradingSetups != null && this.tradingSetups.size() != 0 ? this.tradingSetups.get(0) : null;
   }

   public void runChecks() throws TradingException {
      if (this.tradingSetups == null || this.tradingSetups.size() == 0) {
         throw new TradingException("Initialization: PortfolioSetup doesn't contain any trading setups!");
      }
   }

   public void initialize() {
   }

   public void deinitialize() throws Exception {
      for (TradingSetup var2 : this.tradingSetups) {
         var2.stop();
         var2.destroy();
      }
   }

   public void clear() {
      this.tradingSetups.clear();
   }

   public void remove(TradingSetup var1) {
      this.tradingSetups.remove(var1);
   }
}
