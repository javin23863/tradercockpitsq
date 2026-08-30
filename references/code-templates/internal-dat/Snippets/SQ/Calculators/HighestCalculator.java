package SQ.Calculators;

import com.strategyquant.datalib.TradingException;

public class HighestCalculator extends AbstractCalculator {

	private int highestIndex;
	private double highestValue = -Double.MAX_VALUE;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public HighestCalculator(int Period) throws TradingException {
		super(Period);
	}

	//------------------------------------------------------------------------
	
	protected void calculate(double newValue, boolean isSameBar) throws TradingException {
		int startIndex = CurrentBar < Period ? CurrentBar : (bufferLength() - 1);
		
		if(newValue > highestValue) {
			highestIndex = 0;
			highestValue = newValue;
		}
		else if(highestIndex < startIndex) {
			if(!isSameBar) {
				highestIndex++;
			}
			else if(highestIndex == 0) {			//latest value (which is the highest) was replaced by lower one
				findHighestInBuffer(startIndex);		
			}
		}
		else {
			findHighestInBuffer(startIndex);
		}
	}

	//------------------------------------------------------------------------
	
	private void findHighestInBuffer(int startIndex) {
		highestIndex = 0;
		highestValue = -Double.MAX_VALUE;
		
		for(int a=startIndex; a>=0; a--){
			double val = bufferGet(a);

			if(highestValue < val){
				highestValue = val;
				highestIndex = a;
			}
		}
	}
	
	//------------------------------------------------------------------------
	
	public double getHighestValue() {
		return highestValue;
	}
	
	//------------------------------------------------------------------------
	
	public int getHighestIndex() {
		return highestIndex;
	}

	
}
