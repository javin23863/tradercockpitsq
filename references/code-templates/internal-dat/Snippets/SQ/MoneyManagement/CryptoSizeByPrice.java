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

@ClassConfig(name="Crypto size by price", display="Crypto size by price")
@Help("<b>Size computed as Balance / Asset Size.</b> Position sizing method specifically for crypto trading - you have to specify the exact decimal numbers for rounding.")
@Description("Crypto size by price, max #MaxSize# lots")
@SortOrder(500)
@ForEngine("*,-SP,-SA")
public class CryptoSizeByPrice extends MoneyManagementMethod {
	
	public static final Logger Log = LoggerFactory.getLogger("StocksSizeByPrice");
	
	@Parameter(name="Use account balance", defaultValue="true")
	@Help("If set to true, it will use current account balance. Otherwise it will use initial capital.")
	public boolean UseAccountBalance;
	
	@Parameter(name="Maximum size", defaultValue="100", minValue=0.01, maxValue=1000000000,step=0.1)
	@Help("The biggest size allowed")
	public double MaxSize;
	
	@Parameter(defaultValue="4", minValue=0d, name="Size Decimals", maxValue=12d, step=1d, category="Default")
	@Help("Order size will be rounded to the selected number of decimal places. Use 2 for microlots, 1 for mini lots and 0 for stocks and futures.")
	public int Decimals;	

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	public CryptoSizeByPrice() {
	}

	//------------------------------------------------------------------------

	@Override
	public double computeTradeSize(StrategyBase strategy, String symbol, byte orderType, double price, double sl, double tickSize, double pointValue, double sizeStep) throws Exception {
		if(MaxSize < 0) {
			throw new Exception("Money management wasn't properly initialized. Call init() method before computing trade size!");
		}
		
		double openPrice = price > 0 ? price : (OrderTypes.isLongOrder(orderType) ? strategy.MarketData.Chart(symbol).Ask() : strategy.MarketData.Chart(symbol).Bid());
		
		double tradeSize = 0;
		
		if(UseAccountBalance) {
			tradeSize = (strategy.getAccountBalance() / openPrice) * weight;
		} else {
			tradeSize = (strategy.getInitialBalance() / openPrice) * weight;
		}

		// round computed trade size to give decimal points and cap it by maximum lots
		tradeSize = round(tradeSize, sizeStep, Decimals);
		if(tradeSize <= 0) {
			tradeSize = 1;
		}
		
		if(tradeSize > MaxSize) {
			tradeSize = MaxSize;
		}
		
		if(Log.isDebugEnabled()) Log.debug("Final trade size : "+tradeSize);

		return tradeSize;
	}

}