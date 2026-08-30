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

public class DrawdownPct extends DatabankColumn {
    
	public DrawdownPct() {
		super(L.tsq("Max DD %"), DatabankColumn.Decimal2Pct, ValueTypes.Minimize, 0, 0, 100);

		setTooltip(L.tsq("Max Drawdown in %"));
	}

	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
		if(ordersList.size() == 0) {
			stats.set("AvgPctDrawdown", 0);
			return 0;
		}
		
		double initialCapital = settings.getDouble("MoneyManagement.InitialCapital", 10000); 		

		double peakEquity = initialCapital;
    	
    	double equityMoney = initialCapital;
    	
		for(int i=0; i<ordersList.size(); i++) {
			Order o = ordersList.get(i);
			
    		equityMoney += o.PL;

            o.DD = (float) specialSubtraction(peakEquity, equityMoney);
            
            // percentage DD is computed as % of current DD in money vs equity balance at the peak
           	o.PctDD = (float) getPercentageDD(o.DD, peakEquity); 
            
            if(equityMoney > peakEquity) {
            	peakEquity = equityMoney;
            }
    	}		

		double maxPctDD = 99999999;
		double sumPctDD = 0;
		
		// Note - drawdown in orders is negative ($ -1234), but drawdown returned 
		// in stats values should be positive ($ 1234)

		for(int i=0; i<ordersList.size(); i++) {
			Order o = ordersList.get(i);

			sumPctDD += o.PctDD;
			
			if(o.PctDD < maxPctDD) maxPctDD = o.PctDD;
		}
		
		// compute also average % drawdown
		stats.set("AvgPctDrawdown", round2(Math.abs(safeDivide(sumPctDD, ordersList.size()))));
		
		return round2(Math.abs(maxPctDD));
	}
	
	//------------------------------------------------------------------------
	
	private double specialSubtraction(double peak, double equity) {
		double dd = peak - equity;
			
		if(dd < 0) {
			// this means that current equity is bigger than last peak, 
			// so we are not in drawdown currently
			dd = 0;
		}
		
		// convert DD to negative number
		return -1 * dd;
	}  
	
	//------------------------------------------------------------------------

	private double getPercentageDD(double pl, double equity) {
		if(equity <= 0 || pl > equity) {
			return -1;
		}
	    	
		return pl / (equity / 100);
	}		
}