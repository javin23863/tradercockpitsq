package SQ.TradingOptions;

import org.jdom2.Element;

import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;

public class UseInitialSLPT extends TradingOption {
	
	@Parameter(name="Use initial SL & PT", defaultValue="false", category="Trading options")
	@Help("Sets StopLoss and ProfitTarget immediately after the order gets filled. Otherwise SL/PT is set on next bar")
	@ForEngine("MC,TS")
	public boolean UseInitialSLPT;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBarUpdate(StrategyBase Strategy) throws Exception {
		return true;
	}
	
	//------------------------------------------------------------------------

	@Override
	public boolean isUsedInTrading() {
		return false;
	}

	//------------------------------------------------------------------------

	@Override
	public TradingOption getClone() {
		UseInitialSLPT option = new UseInitialSLPT();
		option.UseInitialSLPT = this.UseInitialSLPT;

		return option;
	}	
	
	//------------------------------------------------------------------------

	public void setFromParameterEl(Element paramElem) throws Exception {
		String key = paramElem.getAttributeValue("key");
		String value = paramElem.getTextTrim();
		
		if(value.isEmpty()) {
			value = paramElem.getAttributeValue("value");
		}
		
		if(key.equals("UseInitialSLPT")) {
			setParameterValue("UseInitialSLPT", value);	
		} 
	}		
	
}
