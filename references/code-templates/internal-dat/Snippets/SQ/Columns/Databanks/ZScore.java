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

public class ZScore extends DatabankColumn {

	public ZScore() {
		super(L.tsq("ZScore"), DatabankColumn.Decimal2, ValueTypes.Maximize, 0, -10, 10);	
	}
	
	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
		double zScore = computeZIndex(ordersList);		
		return round2(zScore);
	}	
	
	//------------------------------------------------------------------------

	private double computeZIndex(OrdersList ordersList) {
    	if(ordersList.size() <= 0) return 0;
    	
    	int W = 0;
    	int L = 0;
    	int WinLossSeriesCount = 0;
    	int ordersCount = 0;
    	
    	double Z;
    	double pl; 
    	double plPrev;
    	
    	for (int i=0; i<ordersList.size(); i++) {
    		Order o = ordersList.get(i);
    		
    		if(!o.isRealOrder() || !o.isFilledOrder()) continue;
    		
    		pl = ordersList.get(i).PL;
    		
    		if(ordersCount == 0) {
    			plPrev = 0;
    		} 
    		else {
    			plPrev = ordersList.get(ordersCount-1).PL;
    		}
    		
    		//counting the number of winning/losing series
    		
    		if (ordersCount == 0) {
    			WinLossSeriesCount = 1;
    		}
    		else {
    			if (sign(pl) * sign(plPrev) < 0) {
    				WinLossSeriesCount++;
    			} 
    		} 
    		
    		//counting the number of wins/losses
    		
    	    if (pl >= 0) W++;
    	    if (pl < 0) L++;
    	    
    	    ordersCount++;
    	}
    	
    	if (L > 0 && W > 0) {   
    		double x = 2f * W * L;
    		Z = (ordersCount * (WinLossSeriesCount - 0.5f) - x) / (Math.sqrt((x * (x - ordersCount)) / (ordersCount - 1f)));
    	} 
    	else {
    		if (L == 0) {
    			Z = 100000;
    		} 
    		else {
    			Z = -100000;
    		}
    	}

    	return(Z);   
	}	
	
	//------------------------------------------------------------------------

	private double sign(double val) {
		if (val<0) return -1;
		return 1;
	}
}