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
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.ValueTypes;

public class StandardDev extends DatabankColumn {

	public StandardDev() {
		super("StandardDev", DatabankColumn.Decimal2, ValueTypes.Minimize, 0, -1, 1);

		setTooltip(L.tsq("Standard Deviation of Trades"));
		setDependencies("AvgTrade");
	}
	
	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
		double avgTrade = stats.getDouble("AvgTrade");
		double standardDev = computeStdev(avgTrade, ordersList, combination);
		
		return round2(standardDev);
	}	
	
	//------------------------------------------------------------------------

	public  double computeStdev(double mean, OrdersList ordersList, StatsTypeCombination combination) {
    	if(ordersList.size() <= 0) return 0;
    	
    	double sum = 0, stdev, pl;
    	
    	int from = 0;
    	int to = ordersList.size();
    	
    	for (int i = from; i < to; i++) {
    		Order order = ordersList.get(i);
    		
    		pl = getPLByStatsType(order, combination);
    		sum += Math.pow((1f*pl - mean), 2f);
    	}
    	
    	stdev = (double) Math.sqrt(sum / ((double) (to - from)));
    	return (stdev);
	}		
}