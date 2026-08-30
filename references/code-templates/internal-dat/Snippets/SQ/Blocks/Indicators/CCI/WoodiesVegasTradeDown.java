package SQ.Blocks.Indicators.CCI;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(WVD) Woodies Vegas Trade Down", display="Woodies Vegas Trade Down(@Chart@#Period#,#Factor#)[#Shift#]", returnType = ReturnTypes.Boolean)
@Help("Is triggered if CCI falls back from 200 leves and make U. It is counter trend pattern, Factor = minimum distance between top and bottom hook on CCI chart")
@OppositeBlock("WoodiesVegasTradeUP")
@ParameterSet(set="Period=14,Factor=5")
@ParameterSet(set="Period=12,Factor=5")
@ParameterSet(set="Period=24,Factor=5")
@ParameterSet(set="Period=48,Factor=5")
@ParameterSet(set="Period=96,Factor=5")
@ParameterSet(set="Period=120,Factor=5")
public class WoodiesVegasTradeDown extends ConditionBlock {
	
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
		double cci6 = indicator.Value.getRounded(Shift + 6);
		double currCCI = indicator.Value.getRounded(Shift);

		boolean factorCondition = ((cci4-cci3)> Factor && (cci1-cci2)>Factor);
		boolean trendUP = cci1>0 && cci2>0 && cci3>0 && cci4>0 && cci5>0 && cci6>0;
		boolean basketDown = (cci1>cci2) && (cci1>cci3) && (cci4>cci2) && (cci4>cci3) ;
		boolean area200 = cci1 > 200 || cci2 > 200 || cci3 > 200 || cci4 > 200 || cci5 > 200;
		
		return area200 && basketDown && factorCondition && trendUP && currCCI < ((cci3+cci2)/2);
	}

}