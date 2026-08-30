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

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@ClassConfig(name="Move SL to BE")
@SortOrder(300)
@ForEngine("*,-SP,-SA")
public class MoveSL2BE extends ExitMethod {
	
	@Parameter(defaultValue="SQ.Formulas.RangeLevel.None", name="Move SL to BE", category="Advanced", showIfDefault=false)
	@Help("Move StopLoss to Break Even")
	@Editor(type=Editors.Formula, formulaName="RangeLevel")
	public IFormula MoveSL2BE;
	
	@Parameter(name="SL to BE - Add pips", category="Advanced", showIfDefault=false)
	@Editor(type=Editors.Formula, formulaName="Range")
	public IFormula SL2BEAddPips;

	private double move2BESL = Order.NOT_DEFINED;
	private boolean debugTrailingStops = false;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public void setForOrder(ILiveOrder order, StrategyBase Strategy) throws TradingException {
		if(MoveSL2BE != null && !MoveSL2BE.isNoneValue()) {
			move2BESL = Order.NOT_DEFINED;
			
			order.registerEvent(UpdateEventTypes.BarOpen, new IActionEventListener() {
				@Override
				public void OnActionEvent(StrategyBase strategy) throws TradingException {
					if(order.isMarketOpen()) {
						checkSL2BE(order, MoveSL2BE, SL2BEAddPips, Strategy);
					}
				}
			});			
		}
		
	}

	//------------------------------------------------------------------------

	@Override
	public double computeValue(byte orderType, StrategyBase strategy, String symbol, double price) throws TradingException {
		throw new TradingException("This method shouldn't be called!");
	}	

	//------------------------------------------------------------------------

	protected void checkSL2BE(ILiveOrder order, IFormula fMoveSL2BE, IFormula fSL2BEAddPips, StrategyBase Strategy) throws TradingException {
		//move2BESL = 0; // don't reset it here, we need to remember the value for Tradestation
		
		if(order.isClosedOrder()) return; // it will delete this listener
		if(!order.isMarketOrder()) return;

		int digits = Strategy.MarketData.getInstrumentInfo(order.getSymbol()).decimals;
		
		double price;
		if(order.getDirection() == Directions.Long) {
			price = Strategy.MarketData.Chart(order.getSymbol()).Bid();
		} else {
			price = Strategy.MarketData.Chart(order.getSymbol()).Ask();
		}
		
		// Move Stop Loss to Break Even
		double orderBELevel = fMoveSL2BE.evaluateFormula(Strategy, order.getSymbol(), price, order.getDirection());
		if(orderBELevel == Order.NOT_DEFINED) return;

		orderBELevel = SQUtils.fixPrice(order.getInstrumentInfo().tickStep, orderBELevel, digits);

		double beAddPips = 0;
		if(fSL2BEAddPips != null) {
			beAddPips = fSL2BEAddPips.evaluateFormula(Strategy, order.getSymbol(), 0, 1);
			beAddPips = beAddPips == Order.NOT_DEFINED ? 0 : beAddPips;
		}
		
		double SL = order.getSL() == Order.NOT_DEFINED ? Order.NOT_DEFINED : SQUtils.round(order.getSL(), digits);
		double openPrice = order.isNettingMode() ? order.getLastOpenPrice() : order.getOpenPrice();
		
		double newSL;
		
		double minPriceDistance = SQUtils.round(Strategy.getMinDistance() * order.getInstrumentInfo().tickSize, digits);
		
		if(order.getDirection() == Directions.Long) {
			newSL = SQUtils.round(openPrice + beAddPips, digits);
			
			if (openPrice <= orderBELevel  && price >= newSL && (SL == Order.NOT_DEFINED || SL < newSL) && Math.abs(price - newSL) >= minPriceDistance) {
				newSL = SQUtils.fixPrice(order.getInstrumentInfo().tickStep, newSL, digits);
				order.setSL(OrderCloseTypes.MoveSL2BE, newSL).Send();
				if(debugTrailingStops) Log.info(SQTime.toDateMinuteString(Strategy.Time(0))+" - Order " + order.getOrderId() + " - Moving Long SL2BE to: {}", newSL);
				move2BESL = newSL;
			}

		} else { // it is short order
			newSL = SQUtils.round(openPrice - beAddPips, digits);
			
			if (openPrice >= orderBELevel  && price <= newSL && (SL == Order.NOT_DEFINED || SL > newSL) && Math.abs(price - newSL) >= minPriceDistance) {
				newSL = SQUtils.fixPrice(order.getInstrumentInfo().tickStep, newSL, digits);

				order.setSL(OrderCloseTypes.MoveSL2BE, newSL).Send();
				if(debugTrailingStops) Log.info("{}, Order " + order.getOrderId() + " - Moving Short SL2BE to: {}", SQTime.toDateMinuteString(Strategy.Time(0)), newSL);
				
				move2BESL = newSL;
			}
		}
	}
	
	//------------------------------------------------------------------------

	/** 
	 * used in Tradestation. SL, PT and other exits are not tied 
	 * with the exact orders but they are placed separately.
	 * @throws TradingException 
	 */
	@Override
	public boolean setExit(ILiveOrder order, StrategyBase strategy) throws TradingException {
		if(move2BESL == Order.NOT_DEFINED || order.usesATM()) {
			return false;
		}
		
		int direction = order.isLong() ? -1 : 1;

		byte orderType = (direction > 0 ? OrderTypes.BuyToCoverStop : OrderTypes.SellToCoverStop);
if(debugTrailingStops) Log.info("{} - Creating order for move2BESL at: {}", SQTime.toDateMinuteString(Strategy.Time(0)), move2BESL);
		
		ILiveOrder slOrder = Strategy.Trader.Open(orderType, order.getSymbol(), move2BESL)
				.setMagicNumber(order.getMagicNumber())
				.setSLType(OrderCloseTypes.MoveSL2BE)
				.Send();
		
		return true;
	}		

	//------------------------------------------------------------------------

	@Override
	public IBlock clone(boolean includingParameters, StrategyBase strategy) throws BlockDefinitionException {
		MoveSL2BE clone = new MoveSL2BE();
		clone.MoveSL2BE = this.MoveSL2BE;
		clone.SL2BEAddPips = this.SL2BEAddPips;
		return clone;
	}
}