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
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;

import java.util.ArrayList;
import java.util.HashMap;

public class MaxNewHighDuration extends DatabankColumn {

	public MaxNewHighDuration() {
		super(L.tsq("Max Drawdown Duration"), DatabankColumn.Integer, ValueTypes.Minimize, 0, 0, 10000);

		setTooltip(L.tsq("Max Drawdown in Days"));

		// this means that value depends on number of trading days
		// and has to be normalized by days when comparing with another
		// result with different number of trading days
		setDependentOnTradingPeriod(true);
	}

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
		if(ordersList.size() == 0) return 0;

		Order veryFirstOrder = ordersList.get(0);
		Order veryLastOrder = ordersList.get(ordersList.size()-1);
		Order bestMaxDrawdownDurationFirstOrder = null;
		Order bestMaxDrawdownDurationLastOrder = null;

		long bestMaxDrawdownDurationPeriod = 0;
		long bestMaxDrawdownDurationArrayGap = 0;
		long maxDrawdownDurationPeriod = 0;

		double accountBalance = 0;

		OutOfSample oosPeriods = (OutOfSample) settings.get("ChartOOS");

		for(int i=0; i<ordersList.size(); i++){
			// The data points is less than the tracked drawdown length
			if(ordersList.size() - i < bestMaxDrawdownDurationArrayGap) break;

			Order startOrder = ordersList.get(i);
			accountBalance += getPLByStatsType(startOrder, combination);

			double accountBalance2 = accountBalance;
			Order endOrder = null;
			int endOrderIndex = -1;
			for(int j=i+1; j<ordersList.size(); j++){
				Order checkingEndOrder = ordersList.get(j);
				accountBalance2 += getPLByStatsType(checkingEndOrder, combination);

				if(accountBalance2 <= accountBalance){
					endOrder = null;
					endOrderIndex = -1;
				} else if(accountBalance2 > accountBalance && endOrder == null){
					endOrder = checkingEndOrder;
					endOrderIndex = j;
				}
			}

			if(endOrder == null) {
				endOrder = veryLastOrder;
				endOrderIndex = ordersList.size() - 1;
			}

			maxDrawdownDurationPeriod = endOrder.CloseTime - startOrder.CloseTime;
			if(maxDrawdownDurationPeriod > bestMaxDrawdownDurationPeriod) {
				bestMaxDrawdownDurationFirstOrder = startOrder;
				bestMaxDrawdownDurationLastOrder = endOrder;
				bestMaxDrawdownDurationPeriod = maxDrawdownDurationPeriod;
				bestMaxDrawdownDurationArrayGap = endOrderIndex - i;
			}
		}

		bestMaxDrawdownDurationFirstOrder = bestMaxDrawdownDurationFirstOrder == null ? veryFirstOrder : bestMaxDrawdownDurationFirstOrder;
		bestMaxDrawdownDurationLastOrder = bestMaxDrawdownDurationLastOrder == null ? veryLastOrder : bestMaxDrawdownDurationLastOrder;

		int totalDays = SQTime.getDaysBetween(veryFirstOrder.OpenTime, veryLastOrder.CloseTime);

		if(bestMaxDrawdownDurationFirstOrder == null || bestMaxDrawdownDurationLastOrder == null){
			return 0;
		}

		// compute maxDrawdownDuration
		long maxDrawdownDurationFrom=bestMaxDrawdownDurationFirstOrder.CloseTime;
		long maxDrawdownDurationTo=correctMaxDrawdownDurationEndTime(maxDrawdownDurationFrom, bestMaxDrawdownDurationLastOrder.CloseTime, oosPeriods, combination.getSampleType());

		maxDrawdownDurationPeriod = SQTime.getDaysBetween(maxDrawdownDurationFrom, maxDrawdownDurationTo);
		stats.set(StatsKey.MAX_NEW_HIGH_DURATION_FROM, maxDrawdownDurationFrom);
		stats.set(StatsKey.MAX_NEW_HIGH_DURATION_TO, maxDrawdownDurationTo);

		double maxDrawdownDurationPeriodPct = SQUtils.safeDivide(maxDrawdownDurationPeriod, totalDays) * 100d;
		stats.set(StatsKey.MAX_NEW_HIGH_DURATION_PCT, round2(maxDrawdownDurationPeriodPct));

		return (int) maxDrawdownDurationPeriod;
	}

	//------------------------------------------------------------------------

	private long correctMaxDrawdownDurationEndTime(long startTime, long endTime, OutOfSample oosPeriods, byte sampleType) {
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