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
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsKey;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.ValueTypes;

public class AvgConsecLosses extends DatabankColumn {
    
	public AvgConsecLosses() {
		super(L.tsq("Avg Consec. Losses"), DatabankColumn.Decimal2, ValueTypes.Minimize, 0, 0, 20);
		
		setTooltip(L.tsq("Average Consecutive Losses"));
	}

	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {

	    int totalConsecLosses = 0;
	    int consecLossesPeriods = 0;
	    int consecLosses = 0;
	    int lastOrderWas = 0;
		
	    for(int i = 0; i<ordersList.size(); i++) {
	    	Order order = ordersList.get(i);

	    	if(order.PL < 0) {
	            consecLosses++;
	            lastOrderWas = -1; // loss

	    	} else {
	            if(lastOrderWas < 0) {
	                // last order was not win (it was loss), so reset the counter
	                // and set consecutive losses data
	                lastOrderWas = 1; // win
	                totalConsecLosses += consecLosses;
	                consecLossesPeriods++;
	                consecLosses = 0;
	            }
	            lastOrderWas = 1; // win
			} 		
	    }
	    
	    // count also the very last stats        
        if (consecLosses > 0) {
            // set consecutive losses data
            totalConsecLosses += consecLosses;
            consecLossesPeriods++;
        }

	    return round2(safeDivide(totalConsecLosses, consecLossesPeriods));
	}

}