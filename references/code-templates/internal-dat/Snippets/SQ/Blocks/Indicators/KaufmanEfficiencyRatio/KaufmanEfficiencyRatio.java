package SQ.Blocks.Indicators.KaufmanEfficiencyRatio;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import SQ.Calculators.*;

import SQ.Internal.IndicatorBlock;

@BuildingBlock(name="(KER) Kaufman Efficiency Ratio", display="Kaufman Efficiency Ratio(@Chart@#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Kaufman Efficiency Ratio")
@Indicator(oscillator=true, middleValue=0.5, min=0, max=1, step=0.01)
@ParameterSet(set="Period=10")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=48")
@ParameterSet(set="Period=120")

public class KaufmanEfficiencyRatio extends IndicatorBlock {


	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(defaultValue="10", isPeriod=true, minValue=2, maxValue=1000, step=1)
	public int Period;

	@Output(name = "KFE", color = Colors.Blue)
	public DataSeries Value;

	private SumCalculator volsumcalculator;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------


	protected void OnInit() throws TradingException {
		volsumcalculator = new SumCalculator(Period);
	}

	@Override
	protected void OnBarUpdate() throws TradingException {
		if(CurrentBar<Period){
			Value.set(0, 0); 
		
		} else{

			double change = Math.abs(Chart.Close.get(0)-Chart.Close.get(Period)); /// Direction = ABS (Close - Close[n])
			volsumcalculator.onBarUpdate(Math.abs(Chart.Close.get(0)-Chart.Close.get(1)), getCurrentBar()); //// Volatility = (ABS(Close - Close[1]))
			
			double calc = volsumcalculator.getValue(); 
			double ker = change/calc; 
			
			Value.set(0, ker); 
		}
	}
}