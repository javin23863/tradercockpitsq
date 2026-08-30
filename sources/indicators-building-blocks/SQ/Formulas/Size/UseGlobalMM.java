package SQ.Formulas.Size;

import SQ.Internal.MMFormulaBlock;
import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.Formula;
import com.strategyquant.tradinglib.MoneyManagementMethod;
import com.strategyquant.tradinglib.StrategyBase;

@Formula(order = 200, name = "Use global Money Management", formula = "Size")
public class UseGlobalMM extends MMFormulaBlock {
   private MoneyManagementMethod mmMethod = null;
   private boolean mmMethodInitialized = false;

   @Override
   public double computeSize(StrategyBase var1, String var2, byte var3, double var4, double var6) throws TradingException {
      if (!this.mmMethodInitialized) {
         this.mmMethodInitialized = true;
         SettingsMap var8 = var1.getSettings();
         if (var8.getBoolean("MoneyManagement.UseFromStrategy", false)) {
            this.mmMethod = var1.getGlobalMMMethod();
         }

         if (this.mmMethod == null) {
            this.mmMethod = (MoneyManagementMethod)var8.get("MoneyManagement.Method");
         }
      }

      if (this.mmMethod == null) {
         return 1.0;
      }

      try {
         InstrumentInfo var10 = var1.MarketData.getInstrumentInfo(var2);
         return this.mmMethod.computeTradeSize(var1, var2, var3, var4, var6, var10.tickSize, var10.pointValue, var10.orderSizeStep);
      } catch (Exception var9) {
         throw new TradingException(var9);
      }
   }
}
