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
package SQ.ExitMethods;

import com.strategyquant.datalib.*;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.simulator.Engines;

@ClassConfig(name="Stop Loss")
@SortOrder(100)
public class StopLoss extends ExitMethod {

	@Parameter(category="Basic", showIfDefault=false)
	@Editor(type=Editors.Formula, formulaName="SLPT")
	@ExitType(ExitTypes.StopLoss)
	public IFormula StopLoss;

	private boolean debugTrailingStops = false;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public void setForOrder(ILiveOrder order, StrategyBase strategy) throws TradingException {
		if(order.isFilled()) {
			setOrderSLValue(order, strategy);
		}
		else {
			boolean isTSMCEngine = strategy.getEngine() == Engines.Tradestation || strategy.getEngine() == Engines.MultiCharts;
			
			if(isTSMCEngine && isInitialSLPTApplied(strategy)) {
				if(order.isMarketOrder()) {
					setInitialOrderSLValue(order, strategy);
				}
				else {
					setInitialExit(order, strategy);
				}
			}
		}
	}

	//------------------------------------------------------------------------ 
	// Metatrader

	private void setOrderSLValue(ILiveOrder order, StrategyBase strategy) throws TradingException {

		//int digits = Strategy.MarketData.getInstrumentInfo(order.getSymbol()).decimals;
		int direction = order.isLong() ? 1 : -1;

		double openPrice = order.isNettingMode() ? order.getLastOpenPrice(): order.getOpenPrice();

		double sl = StopLoss.evaluateFormula(strategy, order.getSymbol(), openPrice, -direction);
		if(sl == Order.NOT_DEFINED) return;

		sl = SQUtils.fixPrice(strategy.getInstrumentInfo().tickStep, sl);
		//sl = SQUtils.round(sl, digits);

		if(sl == openPrice) {
			// SL is same as open price, that cannot be set. Close whole order
			order.Close(OrderCloseTypes.Deleted);
			return;
		}

		sl = correctSLPT(order, sl, true);

		order.setSL(sl).Send();

		if(!order.isSuccessful()) {
			// we were not successful in setting StopLoss, close whole order
			order.Close(OrderCloseTypes.Deleted);
		}
	}

	//------------------------------------------------------------------------
	// TradeStation Multicharts

	private void setInitialOrderSLValue(ILiveOrder order, StrategyBase strategy) throws TradingException {

		//int digits = Strategy.MarketData.getInstrumentInfo(order.getSymbol()).decimals;
		int direction = order.isLong() ? 1 : -1;

		double openPrice = order.isNettingMode() ? order.getLastOpenPrice(): order.getOpenPrice();

		double sl = StopLoss.evaluateFormula(strategy, order.getSymbol(), openPrice, -direction);
		if(sl == Order.NOT_DEFINED) return;

		sl = SQUtils.fixPrice(strategy.getInstrumentInfo().tickStep, sl);
		//sl = SQUtils.round(sl, digits);

		if(sl == openPrice) {
			// SL is same as open price, that cannot be set. Close whole order
			order.Close(OrderCloseTypes.Deleted);
			return;
		}

		sl = correctSLPT(order, sl, true);

		order.setSL(sl).Send();

		if(!order.isSuccessful()) {
			// we were not successful in setting StopLoss, close whole order
			order.Close(OrderCloseTypes.Deleted);
		}
	}

	//------------------------------------------------------------------------

	@Override
	public double computeValue(byte orderType, StrategyBase strategy, String symbol, double price) throws TradingException {
		int direction = OrderTypes.isLongOrder(orderType) ? 1 : -1;

		double sl = StopLoss.evaluateFormula(strategy, symbol, price, -direction);  
		if(sl == Order.NOT_DEFINED) return Order.NOT_DEFINED;

		sl = SQUtils.fixPrice(strategy.getInstrumentInfo().tickStep, sl);

		double tickSize = strategy.getInstrumentInfo().tickSize;

		return correctSLPT(price, sl, direction, tickSize, true);		//used to calculate SL for money management methods
	}

	//------------------------------------------------------------------------

	/**
	 * used in Tradestation. SL, PT and other exits are not tied 
	 * with the exact orders but they are placed separately.
	 * @throws TradingException
	 */
	@Override
	public boolean setExit(ILiveOrder order, StrategyBase strategy) throws TradingException {
		int direction = order.isLong() ? 1 : -1;

		if(order.getOpenPrice() == 0) {
			return false;
		}

		double sl = StopLoss.evaluateFormula(strategy, order.getSymbol(), order.getOpenPrice(), -direction);
		if(sl == Order.NOT_DEFINED) {
			return false;
		}

		sl = SQUtils.fixPrice(Strategy.getInstrumentInfo().tickStep, sl);

		if(debugTrailingStops) Log.info(SQTime.toDateMinuteString(Strategy.Time(0))+" - Setting SL to: {}, corrected: {}", sl, correctSLPT(order, sl, true));

		sl = correctSLPT(order, sl, true);

		// set it also here. It is not used in trading engine, but this way Trailing stop knows the actual SL
		order.setSL(sl).Send();

		if(!shouldApplySLPTToOrder(order, strategy)) {
			return true;
		}

		byte orderType = (direction < 0 ? OrderTypes.BuyToCoverStop : OrderTypes.SellToCoverStop);

		ILiveOrder slOrder = Strategy.Trader.Open(orderType, order.getSymbol(), sl)
				.setComment("ExitSL")
				.setMagicNumber(order.getMagicNumber())
				.Send();

		return true;
	}

	public boolean setInitialExit(ILiveOrder order, StrategyBase strategy) throws TradingException {
		int direction = order.isLong() ? 1 : -1;

//		if(order.getOpenPrice() == 0) {
//			return false;
//		}

		double sl = StopLoss.evaluateFormula(strategy, order.getSymbol(), 0, 1);
		if(sl == Order.NOT_DEFINED || sl <= 0) {
			return false;
		}

		sl = SQUtils.fixPrice(Strategy.getInstrumentInfo().tickStep, sl);

		if(debugTrailingStops) Log.info(SQTime.toDateMinuteString(Strategy.Time(0))+" - Setting SL to: {}, corrected: {}", sl, correctSLPT(order, sl, true));

		sl = correctSLPT(order, sl, true);

		order.setSL(sl).Send();

		if(!shouldApplySLPTToOrder(order, strategy)) {
			return true;
		}

		byte orderType = (direction < 0 ? OrderTypes.BuyToCoverStop : OrderTypes.SellToCoverStop);

		ILiveOrder slOrder = Strategy.Trader.Open(orderType, order.getSymbol(), sl)
				.setComment("ExitSL")
				.setMagicNumber(order.getMagicNumber())
				.Send();

		return true;
	}

	//------------------------------------------------------------------------

	@Override
	public IBlock clone(boolean includingParameters, StrategyBase strategy) throws BlockDefinitionException {
		return this; //no need to clone
	}

}