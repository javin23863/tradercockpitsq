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
package SQ.Functions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

/**
 * The Class DailyEquityComputer.
 */
public class DailyEquityComputer {
	public static final Logger Log = LoggerFactory.getLogger(DailyEquityComputer.class);
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public static double[] computeDailyEquity(OrdersList orderList, byte resultPLType) throws Exception {
		
		if(orderList.isEmpty()) {
			return new double[0];
		}		
		
		Order order;
		
		// compute first and last order time (close time)
		long lastOrderTime = Long.MIN_VALUE;
		long firstOrderTime = Long.MAX_VALUE;
		
		for(int i=0; i<orderList.size(); i++) {
			Order o = orderList.get(i);
			
			if(!o.isFilledOrder()) {
				continue;
			}
			
			if(o.CloseTime > lastOrderTime) {
				lastOrderTime = o.CloseTime;
			}
			if(o.CloseTime < firstOrderTime) {
				firstOrderTime = o.CloseTime;
			}
		}
		
		int arraySize = 3 + ((int) ((lastOrderTime - firstOrderTime)/86400000));
		
		double equityPerDay[] = new double[arraySize];

		long arrayTime = SQTime.getDateInMs(firstOrderTime);
		double totalPL = 0;
		long orderTime;
		int equityIndex = 0;
		
		for(int i=0; i<orderList.size(); i++) {
			order = orderList.get(i);
			
			if(!order.isFilledOrder()) {
				continue;
			}
			
			totalPL += order.getPLByType(resultPLType);

			// get order time without hour, minute, second
			orderTime = SQTime.getDateInMs(order.CloseTime);
			
			if(orderTime == arrayTime) {
				// we are still at the same day
				equityPerDay[equityIndex] = totalPL;
				
			} else {
				// the order is in the future, add as many days as necessary to get to this date
				if(orderTime < arrayTime) {
					return new double[0];
				}
				
				double lastDayEquity = equityPerDay[equityIndex];

				while(arrayTime < orderTime) {
					equityIndex++;
					
					if(equityIndex >= equityPerDay.length) {
						Log.error("Unable to compute daily equity - index out of array {}, {},  {}, {}", equityIndex, equityPerDay.length, orderList.size(), i);
			
						return new double[0];
					}
					
					equityPerDay[equityIndex] = lastDayEquity;

					arrayTime = SQTime.addDays(arrayTime, 1);
				}
				
				// now we are again at the same day
				equityPerDay[equityIndex] = totalPL;
			}
		}
		
		if(equityIndex < equityPerDay.length) {
			double lastDayEquity = equityPerDay[equityIndex];
			
			for(int i=equityIndex+1; i<equityPerDay.length; i++) {
				equityPerDay[i] = lastDayEquity;
			}
		}
		
		return equityPerDay;
	}
}
