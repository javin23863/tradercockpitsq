package SQ.Calculators;

import com.strategyquant.datalib.TradingException;

public class StdDevCalculator extends AbstractCalculator {

	private double currentValue;

	private double curTempValue;
	private double lastTempValue;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	public StdDevCalculator(int Period) throws TradingException {
		super(Period);
	}

	//------------------------------------------------------------------------
	
	protected void calculate(double newValue, boolean isSameBar) throws TradingException {
		if(CurrentBar == 0) {
			currentValue = 0;
			lastTempValue = newValue;
        }
        else {
        	if(!isSameBar && CurrentBar > 1) {
	    		lastTempValue = curTempValue;
	    	}
        	curTempValue = newValue + lastTempValue - (CurrentBar >= Period ? oldBufferValue : 0);

            double avg = curTempValue / Math.min(CurrentBar + 1, Period);
            double sum = 0;
            
            for (int barsBack = Math.min(CurrentBar, Period - 1); barsBack >= 0; barsBack--){
            	double bufferBarsBack = bufferGet(barsBack);
                sum += (bufferBarsBack - avg) * (bufferBarsBack - avg);
            }

            currentValue = Math.sqrt(sum / Math.min(CurrentBar + 1, Period));
        }
	}
	
	//------------------------------------------------------------------------
	
	public double getValue() {
		return currentValue;
	}
	
}
