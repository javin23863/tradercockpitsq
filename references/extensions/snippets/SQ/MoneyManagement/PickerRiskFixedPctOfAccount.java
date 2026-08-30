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

@ClassConfig(name="Risk fixed % of account (Stockpicker)", display="Risk fixed % of account: #Risk#%")
@Help("<b>Risk % - the method allocates defined percent amount of the account capital to a selected strategy.<br>The amount will be split into maximum positions allowed.")
@Description("Risk #Risk#% of account")
@SortOrder(300)
@ForEngine("SP,SA")
public class PickerRiskFixedPctOfAccount extends MoneyManagementMethod {
	
	public static final Logger Log = LoggerFactory.getLogger("PickerRiskFixedPctOfAccount");
	
	@Parameter(name="Risk in %", category="Risk", defaultValue="10", minValue=0.1, maxValue=100,step=0.1)
	@Help("How big percentage of your account will be risked on this strategy?")
	public double Risk;

	@Parameter(name="Allow fractional shares", defaultValue="false")
	@Help("Allow fractional shares - if true it will allow trading fractions of stocks. Not avaiable for all brokers and all stocks - check documentation for more info.")
	public boolean AllowFractionalShares;
	
	@Parameter(defaultValue="4", minValue=0d, name="Fractional decimal numbers", maxValue=12d, step=1d, category="Default")
	@Help("Fractional decimal numbers.")
	public int FractionalDecimalNumbers;	
	
	
	@Parameter(name="Fractional step", defaultValue="0.01", minValue=0, maxValue=1,step=0.01)
	@Help("Fractional step")
	public double FractionalStep;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public double computeTradeSize(StrategyBase strategy, String symbol, byte orderType, double price, double sl, double tickSize, double pointValue, double sizeStep) throws Exception {
		if(Risk < 0) {
			throw new Exception("Money management wasn't properly initialized. Call init() method before computing trade size!");
		}
		
		if(price <= 0) {
			throw new Exception(String.format("PickerRiskFixedPctOfAccount money management - invalid %s symbol price. It must be > 0, got %s.", symbol, SQUtils.d2(price)));
		}
	
		double riskPct = Risk * weight;
		if(riskPct > 100) riskPct = 100;
		
		double maxRisk = SQUtils.round7(strategy.getAccountEquity() * (riskPct / 100));
		
		double tradeSize = maxRisk / price ;
		if(maxPos > 0) {
			tradeSize = tradeSize / maxPos;
		}
				
		if(AllowFractionalShares) {			 
			 tradeSize = ((Math.round(tradeSize / FractionalStep)) * FractionalStep);
			 
			 tradeSize = SQUtils.round(tradeSize, FractionalDecimalNumbers);

		} else {
			if(tradeSize < 1) {
				throw new TradeSizeSmallerThanOneException(String.format("Calculated trade size %s < 1", SQUtils.d2(tradeSize)));
			}
			
			tradeSize = SQUtils.roundDown(tradeSize, 0);
		}
		
		return tradeSize;
	}
}