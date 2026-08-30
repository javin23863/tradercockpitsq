/*
 * Copyright (c) 2017-2018, StrategyQuant - All rights reserved.
 *
 * Code in this file was made in a good faith that it is correct and does what it should.
 * If you found a bug in this code OR you have an improvement suggestion OR you want to include
 * your own code snippet into our standard library please contact us at:
 * https://roadmap.strategyquant.com
 *
 * This code can be used only within StrategyQuant products.
 * Every owner of valid (free, trial or commercial) license of any StrategyQuant product
 * is allowed to freely use, copy, modify or make derivative work of this code without limitations,
 * to be used in all StrategyQuant products and share his/her modifications or derivative work
 * with the StrategyQuant community.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR
 * PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES
 * OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsKey;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.ValueTypes;

public class TotalTradingDays extends DatabankColumn {
    
	private static final long MILISECONDS_IN_DAY = 24l * 60l * 60l * 1000l;
	
	public TotalTradingDays() {
		super(L.tsq("Total Trading Days"), DatabankColumn.Integer, ValueTypes.Minimize, 0, 10, 1000);
		
		// restrict TotalTradingDays computation only for money PL Type (we'll not compute separate stability for % or pips results, it is the same)
		setPLTypeRestrictions(PlTypes.Money); 

		// restrict computation only for Both direction, we'll not compute it for Long only or Short only results
		//setDirectionRestrictions(Directions.Both); 		
	}
	
	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
		
		if(settings.containsKey(StatsKey.TOTAL_TRADING_DAYS) && settings.containsKey(StatsKey.TOTAL_TRADING_MONTHS) && settings.containsKey(StatsKey.TOTAL_TRADING_YEARS) && settings.containsKey(StatsKey.PROFITABLE_MONTHS)) {
			// special handling for Monte Carlo manipulation stats - here the open and close times for orders are artifical, so we use values from original strategy
			stats.set("TotalTradingMonths", settings.getDouble(StatsKey.TOTAL_TRADING_MONTHS));
			stats.set("TotalTradingYears", settings.getDouble(StatsKey.TOTAL_TRADING_YEARS));
			stats.set("ProfitableMonths", settings.getInt(StatsKey.PROFITABLE_MONTHS));
			return settings.getDouble(StatsKey.TOTAL_TRADING_DAYS);
		}
		
		TempData tempData = init(ordersList);
		
		if(ordersList.isEmpty() || tempData.badDatesRecognized) {
			stats.set("TotalTradingMonths", 1);
			stats.set("TotalTradingYears", 1);
			stats.set("ProfitableMonths", 1);
			return 1;
		}
		
		for(int i = 0; i<ordersList.size(); i++) {
			Order order = ordersList.get(i);
			
			// the algorithm: 
			// go from open date to close date and set all days, months, years 
			// during which this order was open as used
			long openDay = order.OpenTime; 
			long closeDay = order.CloseTime;
			
			int dayIndexStart = getDaysBetween(tempData.startingDay, openDay);
			int monthIndexStart = dayIndexStart / 30;
			int yearIndexStart = dayIndexStart / 365;

			int dayIndexEnd = getDaysBetween(tempData.startingDay, closeDay);
			int monthIndexEnd = dayIndexEnd / 30;
			int yearIndexEnd = dayIndexEnd / 365;

			if(dayIndexStart >= tempData.daysInTradeArray.length) dayIndexStart = tempData.daysInTradeArray.length-1;
			if(dayIndexEnd >= tempData.daysInTradeArray.length) dayIndexEnd = tempData.daysInTradeArray.length-1;

			if(monthIndexStart >= tempData.monthsInTradeArray.length) monthIndexStart = tempData.monthsInTradeArray.length-1;
			if(monthIndexEnd >= tempData.monthsInTradeArray.length) monthIndexEnd = tempData.monthsInTradeArray.length-1;

			if(yearIndexStart >= tempData.yearsInTradeArray.length) yearIndexStart = tempData.yearsInTradeArray.length-1;
			if(yearIndexEnd >= tempData.yearsInTradeArray.length) yearIndexEnd = tempData.yearsInTradeArray.length-1;

			for(int j = dayIndexStart; j <= dayIndexEnd; j++) { 
				tempData.daysInTradeArray[j] = true;			
			}
			
			for(int j = monthIndexStart; j <= monthIndexEnd; j++) {
				tempData.monthsInTradeArray[j] = true;
			}
			
			for(int j = yearIndexStart; j <= yearIndexEnd; j++) {
				tempData.yearsInTradeArray[j] = true;
			}			
		}
		
		stats.set("TotalTradingMonths", getCount(tempData.monthsInTradeArray));
		stats.set("TotalTradingYears", getCount(tempData.yearsInTradeArray));
		stats.set("ProfitableMonths", tempData.profitMonths);
		
		return getCount(tempData.daysInTradeArray);
	}	
	
	//------------------------------------------------------------------------

	class TempData {
		public boolean[] daysInTradeArray;
		public boolean[] monthsInTradeArray;
		public boolean[] yearsInTradeArray;
		public int profitMonths;
		
		public long startingDay;
		public boolean badDatesRecognized = false;
	}
	
	//------------------------------------------------------------------------
	
	public TempData init(OrdersList ordersList) throws Exception {
		TempData tempData = new TempData();
		
		// create boolean array for every day, month and year when there could be trade
		if(!ordersList.isEmpty()) {
			long minOrderOpen = -1;
			long maxOrderClose = -1;
			
			int lastMonth = 0;
			int month = 0;
			double monthPL = 0;
			
			for(int i=0; i<ordersList.size(); i++) {
				Order order = ordersList.get(i);
				
				month = SQTime.getYear(order.CloseTime) * 100000 + SQTime.getMonth(order.CloseTime);
				if(lastMonth != 0 && lastMonth != month) {
					if(monthPL>0) tempData.profitMonths++;
					
					monthPL = 0;
				}
				
				monthPL+=order.PL;
				lastMonth = month;
				
				if(maxOrderClose == -1 || order.CloseTime > maxOrderClose) {
					maxOrderClose = order.CloseTime;
				}
				if(minOrderOpen == -1 || order.OpenTime < minOrderOpen) {
					minOrderOpen = order.OpenTime;
				}
			}
			
			if(monthPL>0) {
				tempData.profitMonths++;
			}

			int totalDays = (int) SQTime.getDaysBetween(minOrderOpen, maxOrderClose)+1;
	    	int totalMonths = SQTime.getMonthsBetween(minOrderOpen, maxOrderClose)+1;
	    	int totalYears = SQTime.getYearsBetween(minOrderOpen, maxOrderClose)+1;
			
	    	if(totalDays > 40000) {
	    		// there cannot be over 40k days, if there is then most probably order dates were in a bad format
	    		tempData.badDatesRecognized = true;
	    		return tempData;
	    	}

	    	tempData.daysInTradeArray = new boolean[totalDays];
	    	tempData.monthsInTradeArray = new boolean[totalMonths];
	    	tempData.yearsInTradeArray = new boolean[totalYears];
			
			for(int i=0; i<tempData.daysInTradeArray.length; i++) tempData.daysInTradeArray[i] = false;
			for(int i=0; i<tempData.monthsInTradeArray.length; i++) tempData.monthsInTradeArray[i] = false;
			for(int i=0; i<tempData.yearsInTradeArray.length; i++) tempData.yearsInTradeArray[i] = false;
			
			tempData.startingDay = minOrderOpen;
		}		
		
		return tempData;
	}

	//------------------------------------------------------------------------

	/**
	 * return count of array element that are true
	 * @param array
	 * @return
	 */
	private int getCount(boolean[] array) {
		int count = 0;
		
		for(int i=0; i<array.length; i++) {
			if(array[i]) count++;
		}
		
		return count;
	}	
	
	//------------------------------------------------------------------------

	private int getDaysBetween(long day1, long day2) {
		return (int) (Math.abs(day2 - day1) / (MILISECONDS_IN_DAY));
	}	
}