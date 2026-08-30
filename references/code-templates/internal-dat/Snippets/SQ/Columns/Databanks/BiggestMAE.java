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
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.*;

public class BiggestMAE extends DatabankColumn {
    
	public BiggestMAE() {
		super(L.tsq("Biggest MAE"), DatabankColumn.Decimal2PL, ValueTypes.Maximize, 0, 35000, 0);

		setWidth(70);
		
		setTooltip(L.tsq("Biggest MAE - is the worst Maximum Adverse Excursion of all trades"));
	}
	
	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
		double worstMAE = -Double.MAX_VALUE;
		
		for(int i = 0; i<ordersList.size(); i++) {
			Order order = ordersList.get(i);

			if(order.isPendingOrder()) {
				continue;
			}
			
			double mae = getMAEByStatsType(order, combination);
		    if(mae > worstMAE) {
				worstMAE = mae;
		    }
		}
		
		return -1*worstMAE;
	}

	//------------------------------------------------------------------------

	private double getMAEByStatsType(Order order, StatsTypeCombination combination) {
		if(combination.getPLType() == PlTypes.Money) {
			return order.MAE;
		} else if(combination.getPLType() == PlTypes.Percent) {
			return SQUtils.safeDivide(order.MAE, order.AccountBalance - order.PL) * 100.0;
		} else if(combination.getPLType() == PlTypes.Pips) {
			return order.PipsMAE;
		}

		return 0;
	}

}