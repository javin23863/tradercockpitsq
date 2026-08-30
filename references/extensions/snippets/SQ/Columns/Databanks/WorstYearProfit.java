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
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.ValueTypes;

import it.unimi.dsi.fastutil.ints.Int2DoubleMap;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

public class WorstYearProfit extends DatabankColumn {
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public WorstYearProfit() {
		super(L.tsq("Worst Year Profit"), DatabankColumn.Decimal2PL, ValueTypes.Minimize, 0, -10000, 10000);

		setWidth(100);
	}
	
	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
		Int2DoubleOpenHashMap map = new Int2DoubleOpenHashMap();
		map.clear();
		
		int year;

		for(int i = 0; i<ordersList.size(); i++) {
			Order order = ordersList.get(i);
			
			if(order.isBalanceOrder()) {
				// don't count balance orders (deposits, withdrawals) in
				continue;
			}
			
			double PL = getPLByStatsType(order, combination);
			if(PL == 0) {
				continue;
			}
			
			year = SQTime.getYear(order.CloseTime);
			if(!map.containsKey(year)) {
				map.put(year, 0);
			}
			
			map.put(year, map.get(year) + PL);
		}

		double worstYearProfit = 0;
		ObjectIterator<Int2DoubleMap.Entry> iter = map.int2DoubleEntrySet().fastIterator();
		double yearProfit;
		
		for(int i = 0; iter.hasNext(); i++) {
			yearProfit = iter.next().getDoubleValue();
			
			if(i==0) {
				worstYearProfit = yearProfit;
				continue;
			}
			
			if(worstYearProfit > yearProfit) {
				worstYearProfit = yearProfit;
			}
		};
		
		return round2(worstYearProfit);
	}	

}