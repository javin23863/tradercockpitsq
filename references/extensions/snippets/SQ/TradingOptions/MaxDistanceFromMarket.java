package SQ.TradingOptions;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Activator;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;

public class MaxDistanceFromMarket extends TradingOption {
	
	@Parameter(name="Max distance from market", defaultValue="false", category="Trading options")
	@Help("Limits pending orders maximal distance from current market price")
	@ForEngine("*,-SP,-SA")
	public boolean MaxDistanceFromMarket;
	
	@Parameter(name="Max distance percent", defaultValue="6", minValue=0, maxValue=1000, step=0.1, category="Trading options")
	@Help("Orders with price exceeding this level will be canceled")
	@Editor(type=Editors.Spinner)	
	@Activator(param="MaxDistanceFromMarket")
	@ForEngine("*,-SP,-SA")
	public double MaxDistancePct;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean isUsedInTrading() {
		return false;
	}	
	
	//------------------------------------------------------------------------

	@Override
	public boolean OnBarUpdate(StrategyBase Strategy) throws TradingException {
		return true;
	}
	
	//------------------------------------------------------------------------

	@Override
	public TradingOption getClone() {
		MaxDistanceFromMarket option = new MaxDistanceFromMarket();
		option.MaxDistanceFromMarket = this.MaxDistanceFromMarket;
		option.MaxDistancePct = this.MaxDistancePct;
		
		return option;
	}
	
}
