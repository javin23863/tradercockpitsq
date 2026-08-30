package SQ.Utils;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.ChartData;

public class TimeDiffUtil {
	
	private static final long DAY_MILLIS = 24 * 60 * 60 * 1000;

	//------------------------------------------------------------------------
	/**
	 * Returns calendar days difference between current bar and a bar with applied shift
	 * @param chart
	 * @param shift
	 * @return
	 * @throws TradingException
	 */
	public static int getDiffInDays(ChartData chart, int shift, boolean isTSEngine) throws TradingException {
		if(shift == 0) return 0;
		
		long currentTime = chart.Time(0);
		int daysOffset = SQTime.getDiffInDays(currentTime, chart.Time(shift));
		
		if(daysOffset == 0) return 0;
		
		if(chart.TimeD(0) != 0) {
			long shiftedTime = chart.Time(shift);
			
			for(int i=0; i<=shift; i++) {
				long dailyTime = SQTime.correctDayStart(chart.TimeD(i));

				if(dailyTime <= shiftedTime) {
					return i;
				}
			}
			
			return 0;
		}
		else {
			int diffDays = 0;
			long dayStart = currentTime - (currentTime % DAY_MILLIS);
			
			for(int i=1; i<=shift; i++) {
				long time = chart.Time(i);
				
				if(time <= dayStart) {
					diffDays++;
					dayStart = time - (time % DAY_MILLIS);
				}
			}

			return diffDays;
		}
	}

	//------------------------------------------------------------------------
	/**
	 * Returns calendar weeks difference between current bar and a bar with applied shift
	 * @param chart
	 * @param shift
	 * @return
	 * @throws TradingException
	 */
	public static int getDiffInWeeks(ChartData chart, int shift) throws TradingException {
		if(shift == 0) return 0;
		
		long currentTime = chart.Time(0);
		long shiftedTime = chart.Time(shift);
		
		int daysOffset = SQTime.getDiffInDays(currentTime, shiftedTime);
		
		if(daysOffset == 0) return 0;
		
		int diffWeeks = daysOffset / 7;
		daysOffset = daysOffset % 7;
		
		if(daysOffset > 0) {
			int dow1 = SQTime.getDayOfWeekOriginal(currentTime);
			int dow2 = SQTime.getDayOfWeekOriginal(shiftedTime);
			
			if(dow1 < dow2) {
				diffWeeks++;
			}
		}
		
		return diffWeeks;
	}

	//------------------------------------------------------------------------
	/**
	 * Returns calendar months difference between current bar and a bar with applied shift
	 * @param chart
	 * @param shift
	 * @return
	 * @throws TradingException
	 */
	public static int getDiffInMonths(ChartData chart, int shift) throws TradingException {
		if(shift == 0) return 0;
		
		long currentTime = chart.Time(0);
		long shiftedTime = chart.Time(shift);
		
		int daysOffset = SQTime.getDiffInDays(currentTime, shiftedTime);
		
		if(daysOffset == 0) return 0;
		
		int year1 = SQTime.getYear(currentTime);
		int month1 = SQTime.getMonthOriginal(currentTime);
		int year2 = SQTime.getYear(shiftedTime);
		int month2 = SQTime.getMonthOriginal(shiftedTime);
		
		if(year1 == year2) {
			return month1 - month2;
		}
		
		//calculate months from the beginning of the year
		int diffMonths = month1;	
		year1--;
		
		//add full years
		diffMonths += (year1 - year2) * 12;
		
		//add months to year end of time2
		diffMonths += 12 - month2;
		
		return diffMonths;
	}
}
