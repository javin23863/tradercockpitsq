package SQ.Blocks.Indicators.SuperTrend;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

import SQ.Internal.IndicatorBlock;
import SQ.Blocks.Indicators.MTATR.MTATR;
import SQ.Blocks.Indicators.ATR.ATR;
import com.strategyquant.tradinglib.simulator.Engines;

@BuildingBlock(name="(ST) SuperTrend", display="SuperTrend(@Chart@#Mode#,#ATRPeriod#,#ATRMult#)[#Shift#]", returnType = ReturnTypes.Price)
@Help("SuperTrend help text")
@ParameterSet(set="ATRPeriod=12")
@ParameterSet(set="ATRPeriod=24")
@ParameterSet(set="ATRPeriod=48")
@ParameterSet(set="ATRPeriod=120")
@ParameterSet(set="ATRPeriod=480")
@ParameterSet(set="ATRPeriod=10")
@ParameterSet(set="ATRPeriod=20")
@ParameterSet(set="ATRPeriod=40")
@ParameterSet(set="ATRPeriod=100")
@ParameterSet(set="ATRPeriod=500")
public class SuperTrend extends IndicatorBlock {

	@Parameter
	public ChartData Input;
	
	@Parameter(defaultValue="1")
	@Editor(type=Editors.Selection, values="Basic=1")
	public int Mode;

	@Parameter(defaultValue="24", isPeriod=true, minValue=2, maxValue=240, step=1)
	public int ATRPeriod;

	@Parameter(defaultValue="3", isPeriod=true, minValue=0.5, maxValue=10, step=0.1)
	public double ATRMult;


	@Output
	public DataSeries Value;


	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		if(Engines.isTradestationEngine(Indicators.Engine)) {
			onBarUpdateTS();
		}
		else {
			
			onBarUpdateMT();
		}
	}

	protected void onBarUpdateMT() throws TradingException {
		// MT4/5 ATR
		MTATR indicator = Indicators.MTATR(Input, ATRPeriod);
  	  	double atrValue = indicator.Value.getRounded(0);

		double dUpperLevel = (Input.High.get(0)+Input.Low.get(0))/2 + ATRMult*atrValue;
		double dLowerLevel = (Input.High.get(0)+Input.Low.get(0))/2 - ATRMult*atrValue;
		
		if(Input.Close.get(0) > Value.get(1) && Input.Close.get(1) <=Value.get(1)){

			Value.set(0, dLowerLevel);
		}
		else if(Input.Close.get(0) < Value.get(1) && Input.Close.get(1) >=Value.get(1)){

			Value.set(0, dUpperLevel);
		}
		else if(Value.get(1)<dLowerLevel){

			Value.set(0, dLowerLevel);
		}
		else if(Value.get(1)>dUpperLevel){
			Value.set(0, dUpperLevel);
		}
		else Value.set(0, Value.get(1));
	  
  }	

	protected void onBarUpdateTS() throws TradingException {

		//// TS ATR
		ATR indicator = Indicators.ATR(Input, ATRPeriod);
  	  	double atrValue = indicator.Value.getRounded(0);

		double dUpperLevel = (Input.High.get(0)+Input.Low.get(0))/2 + ATRMult*atrValue;
		double dLowerLevel = (Input.High.get(0)+Input.Low.get(0))/2 - ATRMult*atrValue;
		
		if(Input.Close.get(0) > Value.get(1) && Input.Close.get(1) <=Value.get(1)){

			Value.set(0, dLowerLevel);
		}
		else if(Input.Close.get(0) < Value.get(1) && Input.Close.get(1) >=Value.get(1)){

			Value.set(0, dUpperLevel);
		}
		else if(Value.get(1)<dLowerLevel){

			Value.set(0, dLowerLevel);
		}
		else if(Value.get(1)>dUpperLevel){
			Value.set(0, dUpperLevel);
		}
		else Value.set(0, Value.get(1));
	  
  	}	
}



