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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@ClassConfig(name="Risk fixed % of account", display="Risk fixed % of account: #Risk#%")
@Help("<b>Risk fixed percentage of account size</b><br/>Strategy will risk a given % of equity for every trade.<br/><br/>This is a simple but very effective money management that will allow the strategy to increase the number of lots as your account grows.<br/><br/>It is recommended to risk around 2-5% of the account per trade.")
@Description("Risk #Risk#% of account")
@SortOrder(300)
@ForEngine("*,-SP,-SA")
public class RiskFixedPctOfAccount extends MoneyManagementMethod {
	
	public static final Logger Log = LoggerFactory.getLogger("RiskFixedPctOfAccount");
	
	@Parameter(name="Risk in %", category="Risk", defaultValue="5", minValue=0.1, maxValue=100000,step=0.1)
	@Help("How big percentage of your account will be risked in every trade?")
	public double Risk;
	
	@Parameter(defaultValue="1", minValue=0d, name="Size Decimals", maxValue=6d, step=1d, category="Default")
	@Help("Order size will be rounded to the selected number of decimal places. Use 2 for microlots, 1 for mini lots and 0 for stocks and futures.")
	public int Decimals;
	
	@Parameter(name="StopLoss in pips", category="Risk", defaultValue="100", minValue=0, maxValue=1000000,step=10)
	@Help("If order doesn't have SL then this predefined StopLoss will be used to compute the correct trade size according to money management")
	public int StopLossInPips;

	@Parameter(name="Size if no MM", category="Lots", defaultValue="1", minValue=0, maxValue=1000000000,step=0.1)
	@Help("How many lots should be traded if we disable or cannot use Money Management - for example when computed trade size is 0")
	public double LotsIfNoMM;

	@Parameter(name="Maximum lots", category="Lots", defaultValue="5", minValue=0.01, maxValue=1000000000,step=0.1)
	@Help("The biggest lot size allowed")
	public double MaxLots;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public double computeTradeSize(StrategyBase strategy, String symbol, byte orderType, double price, double sl, double tickSize, double pointValue, double sizeStep) throws Exception {
		if(Risk < 0) {
			throw new Exception("Money management wasn't properly initialized. Call init() method before computing trade size!");
		}
		
		double tradeSize = 0;
		double orderSL;
		
		if(sl > 0) {
			// SL for order is known
			// order.StopLoss is a price level, we have to get the difference between SL level and open price to get SLin pips/ticks
			double openPrice = price > 0 ? price : (OrderTypes.isLongOrder(orderType) ? strategy.MarketData.Chart(symbol).Ask() : strategy.MarketData.Chart(symbol).Bid());
			
			orderSL = Math.abs(openPrice - sl) / tickSize; 

		} else {
			// SL for order is not known
			orderSL = StopLossInPips;
		}
		
		// we have to compute size of next trade so that it risks max X % of current equity
		double slInMoney = SQUtils.round7(orderSL * tickSize * pointValue);

		//Log.info("maxSL * tickSize * pointValue = "+orderSL+" * "+tickSize+" * "+pointValue+" = "+slInMoney);

		double riskPct = Risk * weight;
		if(riskPct > 100) riskPct = 100;
		
		double maxRisk = SQUtils.round7(strategy.getAccountEquity() * (riskPct / 100));
		
		//Log.info("equity * (mmRisk / 100) = "+Trader.getAccountEquity()+" * "+Risk+" = "+maxRisk);

		tradeSize = SQUtils.round7(maxRisk / slInMoney);
		
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