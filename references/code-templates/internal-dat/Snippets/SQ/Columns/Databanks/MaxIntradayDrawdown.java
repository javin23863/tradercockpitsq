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
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.ValueTypes;
import com.strategyquant.tradinglib.results.SpecialValues;

import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;

/**
 * @author Tomas Brynda
 */
public class MaxIntradayDrawdown extends DatabankColumn {
	
	private static final long DAY_MILLIS = 24 * 60 * 60 * 1000l;
    
	public MaxIntradayDrawdown() {
		super(L.tsq("Max Intraday Drawdown"), DatabankColumn.Decimal2PL, ValueTypes.Minimize, 0, -10000, 10000);
	}

	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort, Result result, SettingsMap rgSpecialValues) throws Exception {
		double maxDrawdown = 0;

        if (result != null) {
            Long2FloatRBTreeMap worstDailyEquityMap = result.getWorstDailyEquity();

            if (worstDailyEquityMap != null && !worstDailyEquityMap.isEmpty()) {

                Long2FloatRBTreeMap dailyPLMap = new Long2FloatRBTreeMap();
                long dateFrom = 0;
                long dateTo = 0;

                for (int i = 0; i < ordersList.size(); i++) {
                    Order o = ordersList.get(i);
                    long date = o.CloseTime - (o.CloseTime % DAY_MILLIS);

                    dateTo = Math.max(dateTo, date);
                    dateFrom = (dateFrom == 0) ? date : Math.min(dateFrom, date);

                    dailyPLMap.addTo(date, o.PL);
                }

                double equity = 0;

                for (long curDate = dateFrom; curDate <= dateTo; curDate += DAY_MILLIS) {
                    if (worstDailyEquityMap.containsKey(curDate)) {
                        float worstEquity = worstDailyEquityMap.get(curDate);
                        if (worstEquity != Float.MAX_VALUE) {
                            double intradayDD = equity - worstEquity;
                            maxDrawdown = Math.max(maxDrawdown, intradayDD);
                        }
                    }

                    if (dailyPLMap.containsKey(curDate)) {
                        equity += dailyPLMap.get(curDate);
                    }
                }
            }
        }

        return maxDrawdown;
	}	

}