package SQ.Calculators;

import com.strategyquant.datalib.TradingException;

public class RSICalculator extends AbstractCalculator {

	private double currentValue;

	private double curAvgUp, lastAvgUp;
	private double curAvgDown, lastAvgDown;

	private AverageCalculator downAverageCalculator;
	private AverageCalculator upAverageCalculator;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	public RSICalculator(int Period) throws TradingException {
		super(Period);
		
		downAverageCalculator = new AverageCalculator(AverageCalculator.SMA, Period);
		upAverageCalculator = new AverageCalculator(AverageCalculator.SMA, Period);
	}

	//------------------------------------------------------------------------
	
	protected void calculate(double newValue, boolean isSameBar) throws TradingException {
		if(CurrentBar == 0) {
			downAverageCalculator.onBarUpdate(0, CurrentBar);
			upAverageCalculator.onBarUpdate(0, CurrentBar);
			return;
		}

		double buffer1 = bufferGet(1);
		double down = Math.max(buffer1 - newValue, 0);
		double up = Math.max(newValue - buffer1, 0);
		
		if(!isSameBar) {
			lastAvgDown = curAvgDown;
			lastAvgUp = curAvgUp;
		}
		
		if(CurrentBar < Period){
			downAverageCalculator.onBarUpdate(down, CurrentBar);
			upAverageCalculator.onBarUpdate(up, CurrentBar);
			return;
		}
		else if(CurrentBar == Period){
			downAverageCalculator.onBarUpdate(Math.max(buffer1- newValue, 0), CurrentBar);
			upAverageCalculator.onBarUpdate(Math.max(newValue - buffer1, 0), CurrentBar);
			
			// First averages 
			curAvgDown = downAverageCalculator.getValue();
			curAvgUp = upAverageCalculator.getValue();
		}  
		else {
			// Rest of averages are smoothed
			curAvgDown = (lastAvgDown * (Period - 1) + down) / Period;
			curAvgUp = (lastAvgUp * (Period - 1) + up) / Period;
		}

		currentValue = 0;
		
		if(curAvgDown != 0){
			currentValue = 100.0d - 100.0d / (1.0d + curAvgUp / curAvgDown);
		}
		else if(curAvgUp != 0){
			currentValue = 100.0;
		}
		else {
			currentValue = 50.0;
        }
	}
	
	//------------------------------------------------------------------------
	
	public double getValue() {
		return currentValue;
	}
	
}
