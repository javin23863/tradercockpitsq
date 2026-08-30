package SQ.Blocks.Indicators.CCI;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(ZBD) CCI Zero Line Break Down", display="CCI(@Chart@#Period#)[#Shift#] Zero Line Break Down", returnType = ReturnTypes.Boolean)
@Help("Is triggered if CCI cross zero line down")
@OppositeBlock("WoodiesCCIZeroLineBreakUP")
@ParameterSet(set="Period=6")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=48")
public class WoodiesCCIZeroLineBreakDown extends ConditionBlock {
	
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
		double currCCI = indicator.Value.getRounded(Shift);

		
		return (cci1>0 && currCCI<0);
	}

}