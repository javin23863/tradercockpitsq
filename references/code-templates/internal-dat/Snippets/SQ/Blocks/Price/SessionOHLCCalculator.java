package SQ.Blocks.Price;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.ChartData;

public class SessionOHLCCalculator {
	public static final Logger Log = LoggerFactory.getLogger(SessionOHLCCalculator.class);
	
	private static final long DAY_MILLIS = 24 * 60 * 60 * 1000;
	
	public static final byte OPEN = 1;
	public static final byte HIGH = 2;
	public static final byte LOW = 3;
	public static final byte CLOSE = 4;

	private byte type;
	private int startHours;
	private int startMinutes;
	private int endHours;
	private int endMinutes;
	private int daysShift;
	private ChartData Chart;

	int startHHMM = -1;
	int endHHMM = -1;
	
	private long sessionStartTime = -1;
	private long sessionEndTime = -1;
	
	private ArrayList<Double> sessionOpen = null;
	private ArrayList<Double> sessionHigh = null;
	private ArrayList<Double> sessionLow = null;
	private ArrayList<Double> sessionClose = null;
	
	private boolean waitingForSession = true;
	private int dataShift = 0;
	private int lastCalculatedBar = 0;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	public SessionOHLCCalculator() {
		Chart = null;
	}
	
	//------------------------------------------------------------------------

	public SessionOHLCCalculator(byte type, int startHours, int startMinutes, int endHours, int endMinutes, int daysShift, ChartData Chart) {
		this.type = type;
		this.startHours = startHours;
		this.startMinutes = startMinutes;
		this.endHours = endHours;
		this.endMinutes = endMinutes;
		this.daysShift = daysShift;
		this.Chart = Chart;
		
		this.startHHMM = startHours * 100 + startMinutes;
		this.endHHMM = endHours * 100 + endMinutes;
		
		sessionOpen = new ArrayList<>();
		sessionHigh = new ArrayList<>();
		sessionLow = new ArrayList<>();
		sessionClose = new ArrayList<>();
		
	}

	//------------------------------------------------------------------------

	public double get() throws TradingException {
		if(Chart == null) {
			return 0;
		}
		
		int totalBars = Chart.Time.size();
		
		if(totalBars > lastCalculatedBar){
			//Calculate historical values
			
			for(dataShift = totalBars - lastCalculatedBar; dataShift >= 0; dataShift--) {
				calculate();
			}
			
			lastCalculatedBar = totalBars;
		}

		if(daysShift >= sessionOpen.size()) {
			return 0;
		}
		else {
			if(sessionOpen.size() > daysShift + 1) {
				int index = sessionOpen.size() - 1;
				sessionOpen.remove(index);
				sessionHigh.remove(index);
				sessionLow.remove(index);
				sessionClose.remove(index);
			}
				
			switch(type) {
				case OPEN: return sessionOpen.get(daysShift);
				case HIGH: return sessionHigh.get(daysShift);
				case LOW: return sessionLow.get(daysShift);
				case CLOSE: return sessionClose.get(daysShift);
				default: return 0;
			}
		}
	}

	//------------------------------------------------------------------------
	
	private void calculate() throws TradingException {
		long currentTime = Chart.Time(dataShift);
		
		if(currentTime >= sessionEndTime) {
			if(!sessionClose.isEmpty()) {
				sessionClose.set(0, Chart.Close(dataShift + 1));
				
				long prevTime = Chart.Time(dataShift + 1);
				if(prevTime >= sessionStartTime && prevTime < sessionEndTime) {
					sessionHigh.set(0, Math.max(Chart.High(dataShift + 1), sessionHigh.get(0)));
					sessionLow.set(0, Math.min(Chart.Low(dataShift + 1), sessionLow.get(0)));
				}
			}
			
			calculateSessionTimes(currentTime);
			waitingForSession = true;
		}
		
		if(currentTime >= sessionStartTime) {
			if(waitingForSession) {
				waitingForSession = false;

				sessionOpen.add(0, Chart.Open(dataShift));
				sessionHigh.add(0, Chart.High(dataShift));
				sessionLow.add(0, Chart.Low(dataShift));
				sessionClose.add(0, Chart.Close(dataShift));
			}
			else {
				sessionHigh.set(0, Math.max(Chart.High(dataShift), sessionHigh.get(0)));
				sessionLow.set(0, Math.min(Chart.Low(dataShift), sessionLow.get(0)));
				sessionClose.set(0, Chart.Close(dataShift));
			}
		}

	}

	//------------------------------------------------------------------------
	
	private void calculateSessionTimes(long currentTime) {
		long dayStart = SQTime.correctDayStart(currentTime);
		
		long startTime = dayStart + (startHours * 60 + startMinutes) * 60000;
		long endTime = dayStart + (endHours * 60 + endMinutes) * 60000;
		
		if(startHHMM >= endHHMM) {
			endTime += DAY_MILLIS;
		}
		
		if(currentTime < (endTime - DAY_MILLIS)) {
			startTime -= DAY_MILLIS;
			endTime -= DAY_MILLIS;
		}
		else if(currentTime > endTime || endTime == sessionEndTime) {
			startTime += DAY_MILLIS;
			endTime += DAY_MILLIS;
		}
		
		sessionStartTime = startTime;
		sessionEndTime = endTime;
	}
	
}
