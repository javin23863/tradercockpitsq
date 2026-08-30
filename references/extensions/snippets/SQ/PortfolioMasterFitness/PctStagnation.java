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

package SQ.PortfolioMasterFitness;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StatsKey;
import com.strategyquant.tradinglib.ValueTypes;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterFitness;

public class PctStagnation extends PortfolioMasterFitness {

	public PctStagnation() {
		super(L.tsq("% Stagnation"), ValueTypes.Minimize, 0, 0, 100);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	public double compute(ResultsGroup result, byte sampleType) throws Exception {
		return result.portfolio().stats(Directions.Both, PlTypes.Money, sampleType).getDouble(StatsKey.STAGNATION_PERIOD_PCT);
	}
	
	//------------------------------------------------------------------------

	@Override
	public String print(double value) throws Exception {		
		return PlTypes.printPL(value, PlTypes.Percent);
	}
}
