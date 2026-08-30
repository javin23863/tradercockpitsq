package SQ.Calculators;

import com.strategyquant.datalib.TradingException;

public class LowestCalculator extends AbstractCalculator {

	private int lowestIndex;
	private double lowestValue = Double.MAX_VALUE;    
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	public LowestCalculator(int Period) throws TradingException {
		super(Period);
	}

	//------------------------------------------------------------------------
	
	protected void calculate(double newValue, boolean isSameBar) throws TradingException {
		int startIndex = CurrentBar < Period ? CurrentBar : (bufferLength() - 1);
	
		if(newValue < lowestValue) {     
			lowestIndex = 0;
			lowestValue = newValue;
		}
		else if(lowestIndex < startIndex) {
			if(!isSameBar) {
				lowestIndex++;
			}
			else if(lowestIndex == 0) {			//latest value (which is the lowest) was replaced by higher one
				findLowestInBuffer(startIndex);		
			}
		}
		else {
			findLowestInBuffer(startIndex);
		}
	}

	//------------------------------------------------------------------------
	
	private void findLowestInBuffer(int startIndex) {
		lowestIndex = 0;
		lowestValue = Double.MAX_VALUE;
		
		for(int a=startIndex; a>=0; a--){
			double val = bufferGet(a);

			if(lowestValue > val){
				lowestValue = val;
				lowestIndex = a;
			}
		}
	}
	
	//------------------------------------------------------------------------
	
	public double getLowestValue() {
		return lowestValue;
	}
	
	//------------------------------------------------------------------------
	
	public int getLowestIndex() {
		return lowestIndex;
	}
	
}
