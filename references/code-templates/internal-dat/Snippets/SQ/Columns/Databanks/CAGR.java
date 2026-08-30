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

public class CAGR extends DatabankColumn {
    
	public CAGR() {
		super(L.tsq("CAGR"), DatabankColumn.Decimal2Pct, ValueTypes.Maximize, 0, 0, 50);
		
		setDependencies("TotalDataYears");
		setTooltip(L.tsq("Compound Annual Growth Rate"));
	}
	
	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
        
		double sumTradesInMoney = 0;

		for(int i = 0; i<ordersList.size(); i++) {
			Order order = ordersList.get(i);
			
			if(order.isBalanceOrder()) {
				// don't count balance orders (deposits, withdrawals) in
				continue;
			}
			
			sumTradesInMoney += order.PL;
		}
		
		
		int totalYearsOfTrading = stats.getInt("TotalDataYears");
		
		double initialCapital = settings.getDouble("MoneyManagement.InitialCapital");
		
		if(totalYearsOfTrading <= 0 || initialCapital <= 0) {
			return 0;
		}
		
		// CAGR formula:
		// http://www.investopedia.com/ask/answers/071014/what-formula-calculating-compound-annual-growth-rate-cagr-excel.asp
		double temp1 = (initialCapital+sumTradesInMoney) / initialCapital;
		double temp2 = safeDivide(1, totalYearsOfTrading);
		
		double CAGR = (Math.pow(temp1, temp2) - 1) * 100;
		
		return round2(CAGR);		
	}
	
}