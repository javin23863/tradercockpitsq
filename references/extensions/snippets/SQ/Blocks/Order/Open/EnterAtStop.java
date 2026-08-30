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

import java.util.ArrayList;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ATM;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrderCloseTypes;
import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.Required;
import com.strategyquant.tradinglib.ReturnTypes;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.simulator.Engines;

import SQ.Functions.OrderFunctions;

@BuildingBlock(name="(STOP) Enter at stop", display="EnterAtStop", returnType=ReturnTypes.Order)
@Help("Opens stop order at given price")
@SortOrder(200)
public class EnterAtStop extends EnterAtMarket {

	// Block parameters
	// all parameters from EnterAtMarket (parent class) are inherited too

	// Basic parameters
	@Parameter(category="Basic")
	@Editor(type=Editors.Formula, formulaName="Price")
	@Required
	public IFormula Price;

	// Advanced parameters
	@Parameter(category="Advanced", defaultValue="0", minValue=0, maxValue=1000, step=1, builderMinValue=0, builderMaxValue=200, showIfDefault=false)
	@Help("Number of bars this pending order will be valid, after then it expires. 0 means it never expires.")
	@ForEngine("*,-TS,-MC")
	public int BarsValid;

	// Other parameters
	@Parameter(defaultValue="true", name="Replace Existing Order", category="Order identification", showIfDefault=false)
	@Help("If existing stop order with the same identification exist, should it be replaced?")
	public boolean ReplaceExisting;

	
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
/*
		String stime = SQTime.toDateMinuteString(Strategy.Time(0));
		if(stime.contains("2010.04.20 22:00")) {
			double LowM0 = Strategy.MarketData.Chart(0).LowM(0);

			//Log.info("{} - Close[1]: {}", stime, Strategy.MarketData.Chart(0).Close(1),	SQTime.toDateMinuteString(Strategy.MarketData.Chart(1).Time(0)));
			int a = 1;
			int b = a;
		}
*/

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
		
		byte orderType = (Direction > 0 ? OrderTypes.BuyStop : OrderTypes.SellStop);


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

	//------------------------------------------------------------------------

	protected ArrayList<ILiveOrder> checkPendingOrdersExists(int direction) {
		ArrayList<ILiveOrder> pendingOrdersList = null;
		boolean isNettingEngine = Strategy.getEngine() == Engines.MetaTrader5Netted || Strategy.getEngine() == Engines.Tradestation || Strategy.getEngine() == Engines.MultiCharts;
		
		for(int i=0; i<Strategy.Trader.getOpenOrdersCount(false); i++) {
			ILiveOrder order = Strategy.Trader.getOpenOrder(i, false);
			
			if(OrderFunctions.identify(order, Strategy, Symbol, direction, MagicNumber, Comment) && order.isPendingOrder() && (!isNettingEngine || !order.usesATM())) {
				if(pendingOrdersList == null) {
					pendingOrdersList = new ArrayList<ILiveOrder>();
				}
				
				pendingOrdersList.add(order);
			}
		}
		
		return pendingOrdersList;
	}	
}
