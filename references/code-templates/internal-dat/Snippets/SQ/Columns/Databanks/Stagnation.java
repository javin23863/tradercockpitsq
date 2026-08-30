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
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.SampleTypes;
import com.strategyquant.tradinglib.StatsKey;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.ValueTypes;
import com.strategyquant.tradinglib.strategy.OutOfSample;

import java.util.HashMap;

public class Stagnation extends DatabankColumn {

	public Stagnation() {
		super(L.tsq("Stagnation"), DatabankColumn.Integer, ValueTypes.Minimize, 0, 0, 10000);

		setTooltip(L.tsq("Stagnation in Days"));

		// this means that value depends on number of trading days 
		// and has to be normalized by days when comparing with another
		// result with different number of trading days
		setDependentOnTradingPeriod(true);
	}

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {

		Order veryFirstOrder = null;
		Order veryLastOrder = null;
		Order bestStagnationFirstOrder = null;
		Order bestStagnationLastOrder = null;

		long bestStagnationPeriod = 0;
		long stagnationPeriod = 0;

		double accountBalance = 0;
		double latestProfitHigh = 0;
		Order peakOrder = ordersList.size() > 0 ? ordersList.get(0) : null;

		boolean stagnationActive = false;

		OutOfSample oosPeriods = (OutOfSample) settings.get("ChartOOS");

		// go through orders
		for(int i=0; i<ordersList.size(); i++) {
			Order order = ordersList.get(i);

			double pl = getPLByStatsType(order, combination);

			accountBalance += pl;

			long endTime = stagnationActive && peakOrder != null ? correctStagnationEndTime(peakOrder.CloseTime, order.CloseTime, oosPeriods, combination.getSampleType()) : order.CloseTime;

			if(accountBalance > latestProfitHigh || endTime != order.CloseTime) {
				if(stagnationActive && peakOrder != null) {
					// there was drawdown and now we are going up
					stagnationPeriod = endTime - peakOrder.CloseTime;

					if(stagnationPeriod > bestStagnationPeriod) {
						bestStagnationFirstOrder = peakOrder;
						bestStagnationLastOrder = order;
						bestStagnationPeriod = stagnationPeriod;
					}
				}

				peakOrder = order;
				latestProfitHigh = accountBalance;
				stagnationActive = false;
			}
			else {
				stagnationActive = true;
			}

			veryLastOrder = order;
		}

		int totalDays;

		if(ordersList.isEmpty()) {
			totalDays = 0;
		} else {
			totalDays = SQTime.getDaysBetween(ordersList.get(0).OpenTime, veryLastOrder.CloseTime);
		}

		// compute max new high duration
		long stagnationFrom=0;
		long stagnationTo=0;

		if(stagnationActive && veryLastOrder != null && peakOrder != null) {
			long startTime = peakOrder.CloseTime;
			long endTime = correctStagnationEndTime(startTime, veryLastOrder.CloseTime, oosPeriods, combination.getSampleType());

			stagnationPeriod = endTime - startTime;

			if(stagnationPeriod > bestStagnationPeriod) {
				bestStagnationFirstOrder = peakOrder;
				bestStagnationLastOrder = veryLastOrder;
				bestStagnationPeriod = stagnationPeriod;
			}
		}

		if(bestStagnationFirstOrder == null) {
			bestStagnationFirstOrder = veryFirstOrder;
		}
		if(bestStagnationLastOrder == null) {
			bestStagnationLastOrder = veryLastOrder;
		}

		if(bestStagnationFirstOrder != null) {
			stagnationFrom = bestStagnationFirstOrder.CloseTime;
		}

		if(bestStagnationLastOrder != null) {
			stagnationTo = correctStagnationEndTime(stagnationFrom, bestStagnationLastOrder.CloseTime, oosPeriods, combination.getSampleType());
		} else {
			stagnationTo = 0;
		}

		if(stagnationFrom != 0 && stagnationTo != 0) {
			stagnationPeriod = SQTime.getDaysBetween(stagnationFrom, stagnationTo);
		} else {
			stagnationPeriod = 0;
		}

		stats.set(StatsKey.STAGNATION_FROM, stagnationFrom);
		stats.set(StatsKey.STAGNATION_TO, stagnationTo);

		double stagnationPeriodPct = SQUtils.safeDivide(stagnationPeriod, totalDays) * 100d;
		stats.set(StatsKey.STAGNATION_PERIOD_PCT, round2(stagnationPeriodPct));

		return (int) stagnationPeriod;
	}

	//------------------------------------------------------------------------

	private long correctStagnationEndTime(long startTime, long endTime, OutOfSample oosPeriods, byte sampleType) {
		if(oosPeriods == null) return endTime;

		int count = oosPeriods.getRangesCount();

		for(int a=0; a<count; a++) {
			long dateFrom = oosPeriods.getDateFrom(a);
			long dateTo = oosPeriods.getDateTo(a);

			if(sampleType == SampleTypes.InSample && startTime < dateTo) {
				if(endTime > dateFrom) {
					endTime = dateFrom;
				}
				break;
			}
			else if(sampleType == SampleTypes.OutOfSample && startTime >= dateFrom && startTime <= dateTo) {
				if(endTime > dateTo) {
					endTime = dateTo;
				}
				break;
			}

			if(sampleType == SampleTypes.OutOfSample && startTime < dateFrom && a > 0) {
				endTime = oosPeriods.getDateTo(a-1);
				break;
			}
		}
		return endTime;
	}

}