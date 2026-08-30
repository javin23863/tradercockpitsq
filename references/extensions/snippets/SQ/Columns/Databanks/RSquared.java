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

import com.strategyquant.tradinglib.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;

import SQ.Functions.DailyEquityComputer;

public class RSquared extends DatabankColumn {
	public static final Logger Log = LoggerFactory.getLogger("RSquared");
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public RSquared() {
		super(L.tsq("RSquared"), DatabankColumn.Decimal2, ValueTypes.Maximize, 0, 0, 1);

		setTooltip(L.tsq("RSquared - how straight is the equity curve"));
		
		// restrict stability computation only for money PL Type (we'll not compute separate stability for % or pips results)
		setPLTypeRestrictions(PlTypes.Money);
		
		// restrict computation only for Both direction, we'll not compute it for Long only or Short only results
		setDirectionRestrictions(Directions.Both); 		
	}

	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
		if(ordersList==null || ordersList.isEmpty()) {
			return 0d;
		}

		// we need to do:
			// array of trades should be computed as same as for equity chart in Time mode - for every day there should be a size of equity
            // array of line values should be the value of line (started at first trade, ended at last trade) computed for every day  

		double[] trades = DailyEquityComputer.computeDailyEquity(ordersList, PlTypes.Money);
		
		double[] line = getLineValues(trades);
		if(line == null || line.length == 0) {
			return 0;
		}
		
        // first pass: read in data, compute xbar and ybar
        double sumx = 0.0, sumy = 0.0, sumx2 = 0.0;
        for(int i=0; i<trades.length; i++) {
            sumx  += trades[i];
            sumx2 += trades[i] * trades[i];
            sumy  += line[i];
        }

        double xbar = sumx / trades.length;
        double ybar = sumy / trades.length;

        // second pass: compute summary statistics
        double xxbar = 0.0, yybar = 0.0, xybar = 0.0;
        for (int i = 0; i < trades.length; i++) {
            xxbar += (trades[i] - xbar) * (trades[i] - xbar);
            yybar += (line[i] - ybar) * (line[i] - ybar);
            xybar += (trades[i] - xbar) * (line[i] - ybar);
        }
        double beta1 = xybar / xxbar;
        double beta0 = ybar - beta1 * xbar;

        double ssr = 0.0;      // regression sum of squares
        for (int i = 0; i < trades.length; i++) {
            double fit = beta1*trades[i] + beta0;
            ssr += (fit - ybar) * (fit - ybar);
        }

        double R2 = ssr / yybar;
        if(R2 < 0) R2 = 0;
        if(R2 > 1) R2 = 1;

		stats.set("DataLength", line.length);

		return round2(R2);
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