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
package SQ.Blocks.Order.Open;

import com.strategyquant.lib.*;

import java.util.ArrayList;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.simulator.Engines;

@BuildingBlock(name="(LMT) Enter at limit", display="EnterAtLimit", returnType=ReturnTypes.Order)
@Help("Opens limit order at given price")
@SortOrder(300)
public class EnterAtLimit extends EnterAtStop {
	
	// Block parameters
	// all parameters from EnterAtStop (parent class) are inherited too

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public void OnAction() throws TradingException {
		if((!AllowDuplicateTrades || !engineSupportsDuplicateTrades()) && checkLiveOrderExists(0, true) != null) {
			// market is not flat and duplicate trades are not allowed
			return;
		}

		if(Strategy != null && Strategy.Trader != null && !Strategy.Trader.IsMarketOpen()) {
			// market is not open (MT5 feature since Build 140)
			return;
		}

		ArrayList<ILiveOrder> pendingOrders = checkPendingOrdersExists(Direction);
		if(pendingOrders != null) {
			if(!ReplaceExisting && !Engines.isTradestationEngine(Strategy.getEngine())) {
				// always replace in Tradestation
				return;
			}
			
			// close existing order, it will be replaced with new one
			for(int i=0; i<pendingOrders.size(); i++) {
				pendingOrders.get(i).Close(OrderCloseTypes.Replaced);
			}
		}
		
		byte orderType = (Direction > 0 ? OrderTypes.BuyLimit : OrderTypes.SellLimit);

		// open trade
		double openPrice = Price.evaluateFormula(Strategy, Symbol, 0, Direction);
		if(openPrice == Order.NOT_DEFINED) {
			throw new TradingException("Open price not defined");
		}
		
		openPrice = SQUtils.fixPrice(Strategy.getInstrumentInfo().tickStep, openPrice);

		if(!checkOpenPriceWithinRange(openPrice)) {
			return;
		}
		
		double sl = computeSL(orderType, openPrice);
		double size = computeSize(orderType, openPrice, sl);

		ATM atm = Strategy.getATM();
		
		if(atm != null && atm.isApplicable(Strategy, size, sl, orderType)) {
			double pt = computePT(orderType, openPrice);

			openATMOrder(atm, openPrice, size, sl, pt, orderType, BarsValid);
		
		} else {
			openNormalOrder(openPrice, size, sl, orderType, BarsValid);
		}
	}
	
	

}
