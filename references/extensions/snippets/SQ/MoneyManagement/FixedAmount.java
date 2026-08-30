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
package SQ.MoneyManagement;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@ClassConfig(name="Fixed amount", display="Fixed amount: #RiskedMoney# $")
@Help("<b>Strategy will risk given amount of money for every trade</b><br/>This is basic money management without compounding. It can be used to test real performance of strategies whose Stop Loss is based on ATR.")
@SortOrder(400)
@Description("Fixed amount, #RiskedMoney# $")
@ForEngine("*,-SP,-SA")
public class FixedAmount extends MoneyManagementMethod {

	@Parameter(defaultValue="500", minValue=1d, name="RiskedMoney", maxValue=1000000d, step=100d, category="Default")
	@Help("Risk in $")
	public double RiskedMoney;
	
	@Parameter(defaultValue="1", minValue=0d, name="Size Decimals", maxValue=6d, step=1d, category="Default")
	@Help("Order size will be rounded to the selected number of decimal places. Use 2 for microlots, 1 for mini lots and 0 for stocks and futures.")
	public int Decimals;
	
	@Parameter(name="Size if no MM", category="Lots", defaultValue="0.1", minValue=0, maxValue=1000000000,step=0.1)
	@Help("How many lots should be traded if we disable or cannot use Money Management - for example when computed trade size is 0")
	public double LotsIfNoMM;

	@Parameter(name="Maximum lots", category="Lots", defaultValue="0.5", minValue=0.01, maxValue=1000000000,step=0.1)
	@Help("The biggest lot size allowed")
	public double MaxLots;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public double computeTradeSize(StrategyBase strategy, String symbol, byte orderType, double price, double sl, double tickSize, double pointValue, double sizeStep) throws Exception {
		if(RiskedMoney < 0) {
			throw new Exception("Money management wasn't properly initialized. Call init() method before computing trade size!");
		}
		
		double openPrice = price > 0 ? price : (OrderTypes.isLongOrder(orderType) ? strategy.MarketData.Chart(symbol).Ask() : strategy.MarketData.Chart(symbol).Bid());
		
		double tradeSize = 0;
		double orderSL;
		
		if(sl > 0) {
			// SL for order is known
			// order.StopLoss is a price level, we have to get the difference between SL level and open price to get SLin pips/ticks
			orderSL = Math.abs(openPrice - sl) / tickSize; 

		} else {
			// SL for order is not known
			return Math.min(LotsIfNoMM, MaxLots);
		}
		
		// we have to compute size of next trade so that it risks max X % of current equity
		double slInMoney = orderSL * tickSize * pointValue;

		//Log.info("maxSL * tickSize * pointValue = "+orderSL+" * "+tickSize+" * "+pointValue+" = "+slInMoney);

		double ratio = (RiskedMoney * weight) / slInMoney;
		
		tradeSize = SQUtils.round7(ratio);
		
		//Log.info("maxRisk / slInMoney = "+maxRisk+" / "+slInMoney+" = "+tradeSize);

		// round computed trade size to give decimal points and cap it by maximum lots
		tradeSize = round(tradeSize, sizeStep, Decimals);
		if(tradeSize <= 0) {
			tradeSize = LotsIfNoMM;
		}
		
		if(tradeSize > MaxLots) {
			tradeSize = MaxLots;
		}
		
		return tradeSize;
	}
}
