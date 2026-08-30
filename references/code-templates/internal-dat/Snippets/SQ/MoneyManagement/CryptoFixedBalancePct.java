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

import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.simulator.Engines;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name="Crypto fixed % balance", display="Crypto fixed % balance: #Risk#%")
@Help("<b>Crypto fixed percentage of balance</b>")
@Description("Crypto #Risk#% of balance")
@SortOrder(200)
@ForEngine("MC")
public class CryptoFixedBalancePct extends MoneyManagementMethod {

	public static final Logger Log = LoggerFactory.getLogger("CryptoFixedBalancePct");

	@Parameter(name="Risk in %", defaultValue="5", minValue=0.1, maxValue=100000,step=0.1)
	@Help("How big percentage of your account will be risked in every trade?")
	public double Risk;

	@Parameter(defaultValue="1", minValue=0d, name="Size Decimals", maxValue=6d, step=1d, category="Default")
	@Help("Order size will be rounded to the selected number of decimal places.")
	public int Decimals;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public CryptoFixedBalancePct() {
		
	}

	//------------------------------------------------------------------------

	@Override
	public double computeTradeSize(StrategyBase strategy, String symbol, byte orderType, double price, double sl, double tickSize, double pointValue, double sizeStep) throws Exception {
		if(Risk < 0) {
			throw new Exception("Money management wasn't properly initialized. Call init() method before computing trade size!");
		}

		if(strategy.isEngineDefined() && strategy.getEngine() != Engines.MultiCharts){
			throw new Exception("Crypto fixed % balance money management method is available for Multicharts engine only");
		}

		double tradeSize = 0;
		
		//Maximum amount of money to risk 
		double riskPct = Risk * weight;
		if(riskPct > 100) riskPct = 100;
		
		double moneyToRisk = SQUtils.round2(strategy.getAccountBalance() * riskPct / 100);
		
		double openPrice = price > 0 ? price : (OrderTypes.isLongOrder(orderType) ? strategy.MarketData.Chart(symbol).Ask() : strategy.MarketData.Chart(symbol).Bid());
		
		// round computed trade size to give decimal points and cap it by maximum lots
		tradeSize = round(moneyToRisk / openPrice, sizeStep, Decimals);
		
		return tradeSize;
	}

}