package SQ.Formulas.Size;

import SQ.Internal.MMFormulaBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@Formula(order=200, name="Use global Money Management", formula="Size")
public class UseGlobalMM extends MMFormulaBlock {

	private MoneyManagementMethod mmMethod = null;
	private boolean mmMethodInitialized = false;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public double computeSize(StrategyBase strategy, String symbol, byte orderType, double price, double sl) throws TradingException {
		
		if(!mmMethodInitialized) {
			mmMethodInitialized = true;
			
			SettingsMap settings = strategy.getSettings();
			if(settings.getBoolean(SettingsKeys.UseGlobalMMFromStrategy, false) == true) {
				// use global MM from strategy if exists
				mmMethod = strategy.getGlobalMMMethod();
			}
			
			if(mmMethod == null) {
				mmMethod = (MoneyManagementMethod)  settings.get(SettingsKeys.MoneyManagement);
			}
		}
		
		if(mmMethod == null) {
			return 1;	

		} else {

			try {
				InstrumentInfo ii = strategy.MarketData.getInstrumentInfo(symbol);
				return mmMethod.computeTradeSize(strategy, symbol, orderType, price, sl, ii.tickSize, ii.pointValue, ii.orderSizeStep);

			} catch (Exception e) {
				throw new TradingException(e);
			}
		}
	}
}
