package SQ.Blocks.Indicators.CCI;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(WTU) WoodiesTrendUP", display="WoodiesTrendUP(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Boolean)
@Help("Is triggered if WCCI is 6 consecutive bars abovoe 0")
@OppositeBlock("WoodiesTrendDown")
@ParameterSet(set="Period=14")
@ParameterSet(set="Period=6")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=48")
public class WoodiesTrendUP extends ConditionBlock {
	
	@Parameter
	public DataSeries Input;
	
	@Parameter(defaultValue="14", minValue=2, maxValue=10000, step=1)
	public int Period;

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

		
		return (cci1>0 && cci2>0 && cci3>0 && cci4>0 && cci5>0 && currCCI>0);
		
		
		
	}

				

}