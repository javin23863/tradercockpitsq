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

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

import SQ.Functions.StatFunctions;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.ValueTypes;

public class SQN extends DatabankColumn {
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	public SQN() {
		super(L.tsq("SQN"), DatabankColumn.Decimal2, ValueTypes.Maximize, 0, 0, 20);
		
		setTooltip(L.tsq("Strategy Quality Number"));
		
		setDependencies("AvgLoss", "NumberOfTrades", "AvgTradesPerMonth", "Expectancy", "StandardDev");
		
		// restrict SQN computation only for money PL Type (we'll not compute separate SQN for % or pips results)
		setPLTypeRestrictions(PlTypes.Money); 
		
		// restrict computation only for Both direction, we'll not compute it for Long only or Short only results
		setDirectionRestrictions(Directions.Both); 		
	}

	//------------------------------------------------------------------------
	
	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {

		DoubleArrayList rResult = new DoubleArrayList(ordersList.size()+10);
		rResult.clear();
		
		double avgLossForExpectancy = stats.getDouble("AvgLoss");
        if(avgLossForExpectancy == 0) {
        	avgLossForExpectancy = 1;
        }
        
        avgLossForExpectancy = Math.abs(avgLossForExpectancy);
		
		for(int i=0; i<ordersList.size(); i++) {
			Order o = ordersList.get(i);
			
			rResult.add(o.PL / avgLossForExpectancy);
		}
		
		int numberOfTrades = stats.getInt("NumberOfTrades");
		double avgTradesPerMonth = stats.getDouble("AvgTradesPerMonth");
		
		double sqn = 0;
		double sqn_score = 0;
		
		if(numberOfTrades > 0) {
			
			if(numberOfTrades <= 100) {
				sqn = computeOriginalSQN(rResult);
				
			} else {
				sqn = computeOriginalSQNAbove100Trades(stats, rResult);
			}
			
        	sqn_score = computeSQNScore(rResult) * (avgTradesPerMonth * 12f) / 100f;
        }		
		
		stats.set("SQNScore", round2(sqn_score));
		
	    return round2(sqn);	
	}

	//------------------------------------------------------------------------

	private double computeOriginalSQN(DoubleArrayList rResults) {
		if(rResults.size() < 3) return 0;
		
		double meanPct = StatFunctions.computeAverage(rResults);
        double stddevPct = correctStdDev(StatFunctions.computeStdev(meanPct, rResults));		
		
        if(stddevPct == 0) return 0;
        
		return (double) ((meanPct / stddevPct) * Math.sqrt(rResults.size()));
	}
	
	//------------------------------------------------------------------------

	private double computeOriginalSQNAbove100Trades(SQStats stats, DoubleArrayList rResults) {
		
		double RExpectancy = stats.getDouble("Expectancy");
		double StdDev = stats.getDouble("StandardDev");
		
		double ratio = SQUtils.safeDivide(RExpectancy, StdDev);
		
		return ratio * 10d;
	}
	
	//------------------------------------------------------------------------

	private double computeSQNScore(DoubleArrayList orders) {
		double sqn_avg = 0;
		int indexFrom = 0;
		int chunks = 100;
		int iterations = 0;
		
		while(true) {
			int size = indexFrom + chunks;
			if(size > orders.size()) {
				size = orders.size();
			}
			
			double sqn = computeSQNOn100Orders(orders, indexFrom, size);
			double coef = 1f;
			
			if((size - indexFrom) < chunks) {
				coef = ((double) (size - indexFrom)) / (double) chunks;
			}
			
			sqn_avg += sqn * coef;
			iterations++;
			
			indexFrom += chunks;
			if(indexFrom > orders.size() || size == orders.size()) {
				break;
			}
		}
		
		if(iterations == 0) return 0;
		
		sqn_avg /= (double) iterations;
		
		return sqn_avg;
	}	

	//------------------------------------------------------------------------

	private double computeSQNOn100Orders(DoubleArrayList orders, int indexFrom, int size) {
        double meanPct = StatFunctions.computeAverage(orders, indexFrom, size);
        double stddevPct = correctStdDev(StatFunctions.computeStdev(meanPct, orders, indexFrom, size));		
		
        if(meanPct == 0 || stddevPct == 0) return 0;
        
		return (double) ((meanPct / stddevPct) * Math.sqrt((double) (size - indexFrom)));
	}			

	//------------------------------------------------------------------------

	private double correctStdDev(double value) {
		return Math.abs(value) < 0.000001 ? 0 : value;
	}
}