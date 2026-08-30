package SQ.RiskManagement;

import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.RiskManagementMethod;
import com.strategyquant.tradinglib.Trader;

@ClassConfig(name = "Allow all trades", display = "Allow all trades")
@Help("Every trade will be allowed.")
public class AllowAllTrades extends RiskManagementMethod {
   public void verifyOrder(Trader var1, ILiveOrder var2) {
   }
}
