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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.UpdateEventTypes;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BlockDefinitionException;
import com.strategyquant.tradinglib.ClassConfig;
import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.ExitMethod;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.IActionEventListener;
import com.strategyquant.tradinglib.IBlock;
import com.strategyquant.tradinglib.IFormula;
import com.strategyquant.tradinglib.ILiveOrder;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrderCloseTypes;
import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.exit.TrailingStopHandler;
import com.strategyquant.tradinglib.simulator.Engines;

@ClassConfig(name="Trailing Stop")
@SortOrder(400)
@ForEngine("*,-SP,-SA")
public class TrailingStop extends ExitMethod {
	public static final Logger Log = LoggerFactory.getLogger("TrailingStop");
	
	@Parameter(name="Trailing Stop", category="Advanced", showIfDefault=false)
	@Editor(type=Editors.Formula, formulaName="RangeLevel")
	public IFormula TrailingStop;

	@Parameter(name="TS Activation Level", category="Advanced", showIfDefault=false)
	@Editor(type=Editors.Formula, formulaName="Range")
	public IFormula TrailingActivation;

	private double trailingSL = Order.NOT_DEFINED;
	private boolean debugTrailingStops = false;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public void setForOrder(ILiveOrder order, StrategyBase Strategy) throws TradingException {
		if(TrailingStop != null && !TrailingStop.isNoneValue()) {
			trailingSL = Order.NOT_DEFINED;
			
			order.registerEvent(UpdateEventTypes.BarOpen, new IActionEventListener() {
				@Override
				public void OnActionEvent(StrategyBase strategy) throws TradingException {
					if(order.isMarketOpen()) {
/*
						String time = SQTime.toDateMinuteString(Strategy.Time(0));
						if(time.contains("2016.03.02 00:00")) {
							int a = 1;
							int b = a;
						}
						Log.info("Moving Trailing SL to {} at {}", trailingSL, time);
 */
						trailingSL = TrailingStopHandler.checkTrailingStop(order, TrailingStop, TrailingActivation, Strategy, trailingSL, debugTrailingStops, (byte) -1);
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

	/** 
	 * used in Tradestation. SL, PT and other exits are not tied 
	 * with the exact orders but they are placed separately.
	 * @throws TradingException 
	 */
	@Override
	public boolean setExit(ILiveOrder order, StrategyBase strategy) throws TradingException {
		
		return TrailingStopHandler.setExit(order, strategy, trailingSL, debugTrailingStops);
	}	
	
	//------------------------------------------------------------------------

	@Override
	public IBlock clone(boolean includingParameters, StrategyBase strategy) throws BlockDefinitionException {
		TrailingStop clone = new TrailingStop();
		clone.TrailingStop = this.TrailingStop;
		clone.TrailingActivation = this.TrailingActivation;
		
		return clone;
	}
}