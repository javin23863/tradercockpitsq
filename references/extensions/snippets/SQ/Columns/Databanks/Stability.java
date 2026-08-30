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
import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.SampleTypes;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.ValueTypes;
import com.strategyquant.tradinglib.correlation.CorrelationComputer;

import SQ.Functions.DailyEquityComputer;

/**
 * Stability is computed as R-Squared^2 between equity in time, and line created from first to last trade, multiplied by NetProfit to meausre steepness of the equity.
 * 
 * @author Mark Fric
 *
 */
public class Stability extends DatabankColumn {
	public static final Logger Log = LoggerFactory.getLogger("Stability");
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public Stability() {
		super(L.tsq("Stability"), DatabankColumn.Decimal2, ValueTypes.Maximize, 0, 0, 1);

		setTooltip(L.tsq("Stability - how straight and steep is the equity curve"));
		
		// restrict stability computation only for money PL Type (we'll not compute separate stability for % or pips results)
		setPLTypeRestrictions(PlTypes.Money);
		
		// restrict computation only for Both direction, we'll not compute it for Long only or Short only results
		setDirectionRestrictions(Directions.Both); 		
		
		setDependencies("NetProfit");
	}

	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
		if(ordersList==null || ordersList.isEmpty()) {
			return 0d;
		}
		
		double[] trades = DailyEquityComputer.computeDailyEquity(ordersList, PlTypes.Money);
		double[] line = getLineValues(trades);

		//printValues(trades, line);
		
		if(line == null || line.length == 0) {
			return 0;
		}
		
		double similarity =  CorrelationComputer.calculateSimilarity(trades, line);

		double stability = Math.pow(similarity, 2);

		double netProfit  = stats.getDouble("NetProfit");
		
		if(netProfit < 0) {
			stability *= -1;
		}
		
		return round2(stability);
	}	

	//------------------------------------------------------------------------

	private double[] getLineValues(double[] trades) {
		if(trades == null || trades.length == 0) {
			return new double[0];
		}
		
		double[] line = new double[trades.length];
		
		double y1 = trades[0];
		double y2 = trades[trades.length-1];

		for(int i=0; i<trades.length; i++) {
			line[i] = getLineValue(i, 0, y1, trades.length, y2);
		}
				
		return line;
	}
	
	//------------------------------------------------------------------------

	private static double getLineValue(double x, double x1, double y1, double x2, double y2) {
		return y1 + ((y2 - y1) / (x2 - x1))*(x - x1);
	}

}