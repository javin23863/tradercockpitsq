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
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.ValueTypes;

/**
 * @author Tomas Brynda
 */
public class TSIndex extends DatabankColumn {
	public static final Logger Log = LoggerFactory.getLogger("TSIndex");
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public TSIndex() {
		super(L.tsq("TS Index"), DatabankColumn.Decimal2, ValueTypes.Maximize, 0, 0, 1);

		setTooltip(L.tsq("TS Index - Tradestation index"));
		
		// restrict stability computation only for money PL Type (we'll not compute separate stability for % or pips results)
		setPLTypeRestrictions(PlTypes.Money);	
		
		setDependencies("NetProfit", "NumberOfProfits", "MaxIntradayDrawdown");
	}

	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
		if(ordersList==null || ordersList.isEmpty()) {
			return 0d;
		}
	
		double netProfit = stats.getDouble("NetProfit");
		double numberOfProfits = stats.getDouble("NumberOfProfits");
		double maxIntradayDrawdown = stats.getDouble("MaxIntradayDrawdown");
		
		return round2(netProfit * numberOfProfits / Math.abs(maxIntradayDrawdown));
	}	

}