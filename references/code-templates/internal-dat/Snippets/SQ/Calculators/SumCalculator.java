package SQ.Calculators;

import com.strategyquant.datalib.TradingException;

public class SumCalculator extends AbstractCalculator{
	
	private double lastValue;
	private double currentValue;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	public SumCalculator(int Period) throws TradingException {
		super(Period);
	}

	//------------------------------------------------------------------------
	
	protected void calculate(double newValue, boolean isSameBar) throws TradingException {
		if(!isSameBar) {
			lastValue = currentValue;
		}
		
		currentValue = lastValue - oldBufferValue + newValue;
	}

	//------------------------------------------------------------------------
	
	public double getValue() {
		return currentValue;
	}
	
}
