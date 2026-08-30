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
package SQ.Trading.Commissions;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@ClassConfig(name="Percentage based", display="#CommissionPct# % of equity")
@Help("<b>Percentage based commissions</b><br/>Used mainly for stocks, it computes commission as % of actual price of purchased asset.")
public class PercentageBased extends CommissionsMethod {

	@Parameter(defaultValue="0", minValue=-100d, name="Commission", maxValue=100d, step=1d, category="Default", decimals=4)
	@Help("Commission in % of price per full lot (5 means 5%)")
	public double CommissionPct;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public double computeCommissionsOnOpen(ILiveOrder order, double tickSize, double pointValue) throws Exception {
		double pctCommission = CommissionPct / 100d;
		
		double purchasedEquity = order.getSize() * order.getOpenPrice() * pointValue;
		
		return pctCommission * purchasedEquity;
	}

	//------------------------------------------------------------------------

	@Override
	public double computeCommissionsOnClose(ILiveOrder order, double tickSize, double pointValue) throws Exception {
		return 0;
	}
}
