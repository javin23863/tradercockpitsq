package SQ.Calculators;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.debug.Debugger;

public abstract class AbstractCalculator extends Debugger {
	public static final Logger Log = LoggerFactory.getLogger("AbstractCalculator");

	protected int Period;

	private double[] buffer;
	private int bufferIndexOffset = 0;

	protected double oldBufferValue = 0;
	protected int CurrentBar = 0;

	private int bufferLastBar = 0;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public AbstractCalculator(int Period) throws TradingException {
		if(Period <= 0) {
			Log.debug("Period is 0, used value 2");
			Period = 2;
		}

		this.Period = Period;
	}

	//------------------------------------------------------------------------

	public void onBarUpdate(double newValue, int CurrentBar) throws TradingException {
		if(buffer == null){
			buffer = new double[Period];
			bufferIndexOffset = 0;
			bufferLastBar = CurrentBar;
		}

		this.CurrentBar = CurrentBar;
		boolean isSameBar = bufferLastBar == CurrentBar;

		if(isSameBar) {
			// we are on the same bar - just rewrite last buffer value
			bufferSet(0, newValue);
		}
		else {
			// we are on a new bar - shift buffer
			shiftBuffer(newValue);
			bufferLastBar = CurrentBar;
		}

		calculate(newValue, isSameBar);
	}

	//------------------------------------------------------------------------

	protected abstract void calculate(double newValue, boolean isSameBar) throws TradingException;

	//------------------------------------------------------------------------

	/**
	 * Shifts buffer values to the right and adds new value to index 0
	 * @param newValue
	 */
	protected void shiftBuffer(double newValue){
		oldBufferValue = bufferGet(bufferLength() - 1);

		increaseBufferIndexOffset();

		bufferSet(0, newValue);
	}

	protected void increaseBufferIndexOffset() {
		bufferIndexOffset++;
		if(bufferIndexOffset > buffer.length-1) {
			bufferIndexOffset = 0;
		}

	}

	//------------------------------------------------------------------------

	public double bufferGet(int index) {
		int realIndex = computeRealIndex(index);

		return buffer[realIndex];
	}

	//------------------------------------------------------------------------

	private void bufferSet(int index, double value) {
		int realIndex = computeRealIndex(index);

		buffer[realIndex] = value;
	}
	
	//------------------------------------------------------------------------

	public double AveragebufferGet() {
		
		Double AverageBuffer = 0.0;
		for (int idx = 0; idx <= Math.min(CurrentBar, Period - 1); idx++) {
			AverageBuffer += buffer[idx];
		}

		return AverageBuffer / Math.min(CurrentBar + 1, Period);
	}

	//------------------------------------------------------------------------

	protected int computeRealIndex(int index) {
		int realIndex = bufferIndexOffset - index;

		if(realIndex < 0) {
			realIndex += buffer.length;
		}

		return realIndex;
	}

	//------------------------------------------------------------------------

	protected int bufferLength() {
		return buffer.length;
	}
}
