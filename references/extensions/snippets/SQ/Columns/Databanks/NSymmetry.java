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
import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.ValueTypes;

public class NSymmetry extends DatabankColumn {
    
	public NSymmetry() {
		super(L.tsq("NSymmetry"), DatabankColumn.Decimal2Pct, ValueTypes.Maximize, 0, 0, 100);

		setTooltip(L.tsq("How symmetrical is profit between Long and Short side? The difference of NSymmetry from Symmetry is that if one side is in profit and other in loss it returns -1 instead of 0."));
		setDependencies("NetProfit");
		
		// restrict computation only for Both direction, we'll not compute it for Long only or Short only results
		setDirectionRestrictions(Directions.Both); 			
	}

	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
		if(combination.getDirection() == Directions.Both && statsLong != null && statsShort != null) {
			double longPL = statsLong.getDouble("NetProfit");
			double shortPL = statsShort.getDouble("NetProfit");

			double symmetry = computeSymmetry(longPL, shortPL);

			return round2(symmetry);
		} else {
			return 0d;
		}
	}
	
	//------------------------------------------------------------------------

	private double computeSymmetry(double longPL, double shortPL) {
		if(longPL == 0 || shortPL == 0) {
			// there is no symmetry if any of them is zero
			return 0;
		}
    	
		if(longPL == shortPL) {
    		return 1f;
    	}
			
    	double maxProfit = 0;
    	double smallerProfit = 0;

		if((longPL < 0 && shortPL < 0) || (longPL > 0 && shortPL > 0)) {
			// both profits are either positive or negative
			maxProfit = Math.max(Math.abs(longPL), Math.abs(shortPL));
			smallerProfit = Math.min(Math.abs(longPL), Math.abs(shortPL));

			// compute how much % is small profit from max profit
	    	return smallerProfit / (maxProfit/100f);
		} 
		
		// now one of the PLs is positive, another negative. Return negative symmetry
		return -1;
	}	
	
}