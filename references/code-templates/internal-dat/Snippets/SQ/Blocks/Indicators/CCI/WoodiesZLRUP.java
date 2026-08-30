package SQ.Blocks.Indicators.CCI;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(WZU) WoodiesZLRUP", display="WoodiesZLRUP(@Chart@#Period#,#Factor#)[#Shift#]", returnType = ReturnTypes.Boolean)
@Help("Is triggered if CCI makes hook in the direction of uptrend, Factor = minimum distance between top and bottom hook on CCI chart")
@OppositeBlock("WoodiesZLRDown")
@ParameterSet(set="Period=12,Factor =5")
@ParameterSet(set="Period=14,Factor =5")
@ParameterSet(set="Period=24,Factor =5")
@ParameterSet(set="Period=8,Factor =5")
public class WoodiesZLRUP extends ConditionBlock {
	
	@Parameter
	public DataSeries Input;
	
	@Parameter(defaultValue="14", minValue=2, maxValue=10000, step=1)
	public int Period;

	@Parameter(defaultValue="5", minValue=1, maxValue=50, step=1)
	public int Factor;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		
		CCI indicator = Strategy.Indicators.CCI(Input, Period);
			
		double cci1 = indicator.Value.getRounded(Shift + 1);
		double cci2 = indicator.Value.getRounded(Shift + 2);
		double cci3 = indicator.Value.getRounded(Shift + 3);
		double cci4 = indicator.Value.getRounded(Shift + 4);
		double cci5 = indicator.Value.getRounded(Shift + 5);
		double currCCI = indicator.Value.getRounded(Shift);

		boolean hookup = (currCCI>cci1)&&(cci2>cci1); 
		boolean factorCondition = ((currCCI-cci1)> Factor && (cci2-cci1)>Factor);
		boolean below100 = (currCCI < 100 && cci1 < 100  && cci2 < 100); 
		boolean aboveMinus100 = (currCCI > -100 && cci1 > -100  && cci2 > -100); 
	
		return (cci1>0 && cci2>0 && cci3>0 && cci4>0 && cci5>0 && currCCI>0) && hookup == true && factorCondition == true && below100 == true && aboveMinus100 == true;
	}

}