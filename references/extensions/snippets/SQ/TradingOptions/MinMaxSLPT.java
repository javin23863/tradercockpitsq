package SQ.TradingOptions;

import org.jdom2.Element;

import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.TradingOption;

public class MinMaxSLPT extends TradingOption {
	
	@Parameter(name="Minimum SL", defaultValue="0", minValue=0, maxValue=1000000, category="Trading options")
	@Help("Minimum SL in pips, it will cut the SL value so that it is not smaller than this setting. If 0, no limit is used.")
	@ForEngine("*,-SP,-SA")
	public int MinimumSL;

	@Parameter(name="Maximum SL", defaultValue="0", minValue=0, maxValue=1000000, category="Trading options")
	@Help("Maximum SL in pips, it will cut the SL value so that it is not bigger than this setting. If 0, no limit is used.")
	@ForEngine("*,-SP,-SA")
	public int MaximumSL;
	
	@Parameter(name="Minimum PT", defaultValue="0", minValue=0, maxValue=1000000, category="Trading options")
	@Help("Minimum PT in pips, it will cut the PT value so that it is not smaller than this setting. If 0, no limit is used.")
	@ForEngine("*,-SP,-SA")
	public int MinimumPT;

	@Parameter(name="Maximum PT", defaultValue="0", minValue=0, maxValue=1000000, category="Trading options")
	@Help("Maximum PT in pips, it will cut the PT value so that it is not bigger than this setting. If 0, no limit is used.")
	@ForEngine("*,-SP,-SA")
	public int MaximumPT;

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
		MinMaxSLPT option = new MinMaxSLPT();
		option.MinimumSL = this.MinimumSL;
		option.MaximumSL = this.MaximumSL;
		option.MinimumPT = this.MinimumPT;
		option.MaximumPT = this.MaximumPT;

		return option;
	}	
	
	//------------------------------------------------------------------------

	public void setFromParameterEl(Element paramElem) throws Exception {
		String key = paramElem.getAttributeValue("key");
		String value = paramElem.getTextTrim();
		
		if(value.isEmpty()) {
			value = paramElem.getAttributeValue("value");
		}
		
		if(key.equals("MinimumSLPT")) {
			setParameterValue("MinimumSL", value);	
			setParameterValue("MinimumPT", value);	
		} 
		else if(key.equals("MaximumSLPT")) {
			setParameterValue("MaximumSL", value);	
			setParameterValue("MaximumPT", value);	
		} 
		else {
			setParameterValue(key, value);	
		}		
	}		
	
}
