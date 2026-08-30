/*
 * Copyright (c) 2017, StrategyQuant - All rights reserved.
 *
 * Code in this file was made in a good faith that it is correct and does what it should.
 * If you found a bug in this code OR you have an improvement suggestion OR you want to include
 * your own code snippet into our standard library please create an issue of request at:
 * http://roadmap.strategyquant.com/
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
package SQ.Blocks.Order.Open;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(MKT) Enter/reverse at market", display="Enter/ReverseAtMarket", returnType=ReturnTypes.Order)
@Help("Opens order at current market price<br/> If there already is existing order to an opposite direction, it closes it first.")
@SortOrder(110)
@ForEngine("*,-SP,-SA")
public class EnterReverseAtMarket extends EnterAtMarket {
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public void OnAction() throws TradingException {
		byte orderType = (Direction > 0 ? OrderTypes.Buy : OrderTypes.Sell);
		   
		ATM atm = Strategy.getATM();
		boolean atmUsed = atm != null && atm.isApplicable(Strategy, 0, 0, orderType);
		
		if(atmUsed) {
			ILiveOrder oppositeOrder = checkLiveOrderExists((Direction > 0 ? -1 : 1), false);
			while(oppositeOrder != null) {
				// there is an opposite order, close it first
				oppositeOrder.Close(OrderCloseTypes.Replaced);
				oppositeOrder = checkLiveOrderExists((Direction > 0 ? -1 : 1), false);
			}
		}
		else {
			ILiveOrder oppositeOrder = checkLiveOrderExists((Direction > 0 ? -1 : 1), false);
			if(oppositeOrder != null) {
				// there is an opposite order, close it first
				oppositeOrder.Close(OrderCloseTypes.Replaced);
			}
		}

		if((!AllowDuplicateTrades || !engineSupportsDuplicateTrades()) && checkLiveOrderExists(Direction, false) != null) {
			// market is not flat and duplicate trades are not allowed
			return;
		}
		
		// open trade

		double sl = computeSL(orderType, (Direction > 0 ? Strategy.MarketData.Chart(Symbol).Ask() : Strategy.MarketData.Chart(Symbol).Bid()));
		double size = computeSize(orderType, 0, sl);

		if(atmUsed) {
			double pt = computePT(orderType, (Direction > 0 ? Strategy.MarketData.Chart(Symbol).Ask() : Strategy.MarketData.Chart(Symbol).Bid()));
			openATMOrder(atm, -1, size, sl, pt, orderType, 0);
		} 
		else {
			openNormalOrder(-1, size, sl, orderType, 0);
		}
	}
	
}