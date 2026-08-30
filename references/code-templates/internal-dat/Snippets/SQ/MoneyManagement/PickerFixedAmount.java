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

@ClassConfig(name="Fixed amount (Stockpicker)", display="Fixed amount: #RiskedMoney# $")
@Help("<b>Fixed - the method allocates fixed amount of capital and split into maximum positions allowed.</b>")
@SortOrder(400)
@Description("Fixed amount, #RiskedMoney# $")
@ForEngine("SP,SA")
public class PickerFixedAmount extends MoneyManagementMethod {

	@Parameter(defaultValue="10000", minValue=1d, name="RiskedMoney", maxValue=1000000d, step=100d, category="Default")
	@Help("How much in $ (money) will be risked on this strategy?")
	public double RiskedMoney;

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
		if(RiskedMoney < 0) {
			throw new Exception("Money management wasn't properly initialized. Call init() method before computing trade size!");
		}
		
		if(price <= 0) {
			throw new Exception(String.format("PickerFixedAmount money management - invalid %s symbol price. It must be > 0, got %s.", symbol, SQUtils.d2(price)));
		}

		double tradeSize = (RiskedMoney * weight) / price;
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
