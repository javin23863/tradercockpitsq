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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.ValueTypes;

import SQ.Functions.StatFunctions;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.Int2DoubleAVLTreeMap;

public class SharpeRatio extends DatabankColumn {
	public static final Logger Log = LoggerFactory.getLogger("SharpeRatio");
	
	private final long DAY_DURATION = 24 * 60* 60 * 1000;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public SharpeRatio() {
		super(L.tsq("Sharpe Ratio"), DatabankColumn.Decimal2, ValueTypes.Maximize, 0, -1, 1);
		
		this.setTooltip(L.tsq("Sharpe Ratio (annualized)"));

		// restrict SharpeRatio computation only for money PL Type (we'll not compute separate SharpeRatio for % or pips results, it is as same as for money)
		setPLTypeRestrictions(PlTypes.Money); 		

		// restrict SharpeRatio computation only for Both direction, we'll not compute Sharpe for Long only or Short only results
		setDirectionRestrictions(Directions.Both); 		
	}
	
	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
		
		DoubleArrayList dailyReturnsPct = computeDailyReturn(ordersList);
		
		double returnsMean = StatFunctions.computeAverage(dailyReturnsPct);
		double returnsStddev = StatFunctions.computeStdev(returnsMean, dailyReturnsPct);
		
		double sharpeRatio = Math.sqrt(252) * SQUtils.safeDivide(returnsMean, returnsStddev);

		return round2(sharpeRatio);
	}
	
	//------------------------------------------------------------------------

	private DoubleArrayList computeDailyReturn(OrdersList ordersList) {
		if(ordersList.size() == 0) {
			return null;
		}
		
		// we take 5% yearly profit as benchmark for computing Sharpe ratio
		double benchmark = 0.05/252;
		
		// compute first and last date
		long firstDate = Long.MAX_VALUE;
		long lastDate = -1;
		
		for(int i=0; i<ordersList.size(); i++) {
			Order o = ordersList.get(i);
			
			if(o.CloseTime < firstDate) {
				firstDate = o.CloseTime;
			}

			if(o.CloseTime > lastDate) {
				lastDate = o.CloseTime;
			}
		}

		// fix it so that it starts on Monday 
		firstDate = SQTime.correctDayStart(firstDate);
		int firstDateDow = SQTime.getDayOfWeek(firstDate);
		
		// create array of returns for every day that is not saturday/sunday
		DoubleArrayList returns = new DoubleArrayList(100);
		int index = 1;
		long startDate = firstDate;
		while(true) {
			int weekIndex = index+firstDateDow-1;
			if(weekIndex > 7) {
				int weeks = weekIndex / 7;
				weekIndex -= weeks*7;
			}
			if(weekIndex % 6 != 0 && weekIndex % 7 != 0) {
				// skip Saturdays and Sundays
				returns.add(-benchmark);
			}
			
			index++;
			
			startDate += DAY_DURATION;
			
			if(startDate > lastDate) {
				break;
			}
		}
		
		// now go through orders and add PctPL for every order
		for(int i=0; i<ordersList.size(); i++) {
			Order o = ordersList.get(i);
			
			int dow = SQTime.getDayOfWeek(o.CloseTime);
			if(dow == 6 || dow == 7) {
				// skip Saturdays and Sundays
				continue;
			}
			
			long closeTime = SQTime.correctDayStart(o.CloseTime);

			index = (int) ((closeTime - firstDate) / DAY_DURATION);
			
			// deduct weekends
			if(index > firstDateDow) {
				index -= 2;
			}
			int weeks = index / 7;
			index -= weeks*2;
			
			if(index < 0 || index >= returns.size()) {
				continue;
			}
			
			double currentDayPL = returns.getDouble(index);
			returns.set(index, currentDayPL+o.PctPL);
		}
		
		return returns;
	}

}