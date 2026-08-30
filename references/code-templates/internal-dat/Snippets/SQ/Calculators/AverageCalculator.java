package SQ.Calculators;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;

public class AverageCalculator extends AbstractCalculator{
	
	public static final int SMA = 0;
	public static final int EMA = 1;
	public static final int SMMA = 2;
	public static final int LWMA = 3;

	private int Type = 0;
	
	private double lastValue;
	private double currentValue;
	private int NbBarLastReset = 0;
	private int AveragePrecision;
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	public AverageCalculator(int Type, int Period) throws TradingException {
		super(Period);
		
		this.Type = Type;
		if(Type < 0 || Type > 3) throw new TradingException("Invalid MA type used. Must be in range 0-3");
		this.AveragePrecision = 0;
	}
	
	//------------------------------------------------------------------------
	public AverageCalculator(int Type, int Period, int AveragePrecision) throws TradingException {
		super(Period);
		
		this.Type = Type;
		this.AveragePrecision = AveragePrecision;
		if (this.AveragePrecision < 0 ) this.AveragePrecision = 0;
		if(Type < 0 || Type > 3) throw new TradingException("Invalid MA type used. Must be in range 0-3");
	}

	//------------------------------------------------------------------------
	
	protected void calculate(double newValue, boolean isSameBar) throws TradingException {
		if(!isSameBar) {
			lastValue = currentValue;
		}
		
		switch(Type) {
			case SMA: calculateSMA(CurrentBar, newValue); break;
			case EMA: calculateEMA(CurrentBar, newValue); break;
			case SMMA: calculateSMMA(CurrentBar, newValue); break;
			case LWMA: calculateLWMA(CurrentBar, newValue); break;
		}
	}

	//------------------------------------------------------------------------
	
	private void calculateSMA(int CurrentBar, double newValue) {		
		if (CurrentBar == 0) {
			currentValue = newValue;
		} 
		else {
			if (AveragePrecision == 0 || (AveragePrecision > 0 && NbBarLastReset < (Period * this.AveragePrecision) / 100))
			{
				double sum = lastValue * Math.min(CurrentBar, Period);
				if (CurrentBar >= Period) {
					currentValue = (sum + newValue - oldBufferValue) / Math.min(CurrentBar, Period);
				} 
				else {
					currentValue = (sum + newValue) / (Math.min(CurrentBar, Period) + 1);
				}
				if (AveragePrecision > 0 ) NbBarLastReset ++;
			}
			else
			{
				currentValue = AveragebufferGet();
				NbBarLastReset = 0;			
			}
		}
	}

	//------------------------------------------------------------------------
	
	private void calculateEMA(int CurrentBar, double newValue) {
		if(CurrentBar == 0) {
			currentValue = newValue;
		} else {
			currentValue = newValue * (2.0 / (1 + Period)) + (1 - (2.0 / (1 + Period))) * lastValue;	
		}
	}

	//------------------------------------------------------------------------
	
	private void calculateSMMA(int CurrentBar, double newValue) {	
		if(CurrentBar == 1) {
			currentValue = newValue;
		}
		else {
			currentValue = (lastValue * (Period - 1) + newValue) / Period;
		}
	}

	//------------------------------------------------------------------------
	
	private void calculateLWMA(int CurrentBar, double newValue) {		
		int max = Math.min(Period, CurrentBar);
		int multiplier = Period;
		int divider = 0;
		double sum = 0;
		
		for(int a=0; a<max; a++){
			divider += multiplier;
			sum += bufferGet(a) * (multiplier--);
		}
		
		currentValue = sum / divider;
	}

	//------------------------------------------------------------------------
	
	public double getValue() {
		return currentValue;
	}
}
