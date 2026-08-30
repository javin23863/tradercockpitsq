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
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.results.SpecialValues;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;

/**
 * @author Tomas Brynda
 */
public class MaxTSIntradayDrawdown extends DatabankColumn {

	private static final long DAY_MILLIS = 24 * 60 * 60 * 1000l;

	public MaxTSIntradayDrawdown() {
		super(L.tsq("Max TS Intraday Drawdown"), DatabankColumn.Decimal2PL, ValueTypes.Maximize, 0, -10000, 10000);

		setDependencies("MaxIntradayDrawdown");
	}

	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort, Result result, SettingsMap rgSpecialValues) throws Exception {
		if(ordersList.size() == 0) {
			return 0;
		}

		double maxTSIntraDD = Double.MAX_VALUE;
		double prevDD = 0;

		for(int i=0; i<ordersList.size(); i++) {
			Order o = ordersList.get(i);

			double temp = prevDD - o.MAE;
			if(temp < maxTSIntraDD) {
				maxTSIntraDD = temp;
			}

			prevDD = o.DD;
		}

		int maxIntradayDrawdown = stats.getInt("MaxIntradayDrawdown");

		//return maxTSIntraDD;
		return -Math.max(Math.abs(maxTSIntraDD), maxIntradayDrawdown);
	}	

}