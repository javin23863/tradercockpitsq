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
import com.strategyquant.tradinglib.simulator.Engines;

@ClassConfig(name="Simple Martingale MM", display="Simple Martingale MM")
@Help("<b>Simple Martingale MM</b><br/><span style=\"color: red\">Experimental feature</span><br/>Characteristics:<br/><ul><li>Optional separate logic for Buy and Sell</li><li>Multiplies the last trade size by multiplier after loss</li><li>Resets size to a starting size after profit</li><li>When computed size is bigger than reset value, it uses Starting lots again</li></ul>")
@SortOrder(900)
@Description("Simple Martingale MM")
@ForEngine("*,-SP,-SA")
public class SimpleMartingaleMM extends MoneyManagementMethod {
	
	public static final Logger Log = LoggerFactory.getLogger("SimpleMartingaleMM");
	
	@Parameter(name="Starting lots", defaultValue="0.1", minValue=0.01)
	@Help("Starting size.")
	public double LotsStart;
	
	@Parameter(name="Lots multiplier", defaultValue="2.0", minValue=0.01)
	@Help("Multiplier to multiply the previous order size by.")
	public double LotsMultiplier;
	
	@Parameter(name="Maximum Lots (reset)", defaultValue="1.0", minValue=0.01)
	@Help("Resets to Starting Lots if computed lot size is bigger than this.")
	public double LotsReset;
	
	@Parameter(defaultValue="1", minValue=0d, name="Size Decimals", maxValue=6d, step=1d, category="Default")
	@Help("Order size will be rounded to the selected number of decimal places. Use 2 for microlots, 1 for mini lots and 0 for stocks and futures.")
	public int Decimals;
	
	@Parameter(name="Separate MM by direction?", defaultValue="true")
	@Help("If set to true, it will use Martingale independently for buy and sell orders.")
	public boolean SeparateByDirection;	

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	public SimpleMartingaleMM() {
	}

	//------------------------------------------------------------------------

	@Override
	public double computeTradeSize(StrategyBase strategy, String symbol, byte orderType, double price, double sl, double tickSize, double pointValue, double sizeStep) throws Exception {		
		if(LotsStart < 0) {
			throw new Exception("Money management wasn't properly initialized. Call init() method before computing trade size!");
		}
		
		int engineUsed = strategy.getEngine();
		if(strategy.isEngineDefined() && (engineUsed == Engines.MultiCharts || engineUsed == Engines.Tradestation)) {
			return round(LotsStart, sizeStep, Decimals);
		}
		
		int direction = 0;
		if(SeparateByDirection) {
			direction = (OrderTypes.isLongOrder(orderType) ?  1 : -1);
		}
		
		Order lastClosedOrder = getLastClosedOrder(direction, strategy);
		if(lastClosedOrder == null) {
			// if there is no last order or last order ended up in profit reset the size
			return round(LotsStart, sizeStep, Decimals);
		}

		double PL = getPL(lastClosedOrder);
		if(PL > 0) {
			// it was profit, reset
			return round(LotsStart, sizeStep, Decimals);
		}

		double lastOrderSize = SQUtils.round(lastClosedOrder.Size, Decimals);
		
		double newSize = lastOrderSize * LotsMultiplier * weight;
		
		if(newSize > LotsReset) {
			// we reached maximum allowed size, reset it back to the start one
			return round(LotsStart, sizeStep, Decimals);
		}
		
		return round(newSize, sizeStep, Decimals);
	}
	
	//------------------------------------------------------------------------

	private double getPL(Order order) {
		if(order.isLong()) {
			return order.ClosePrice - order.OpenPrice;			
		}
		
		return order.OpenPrice - order.ClosePrice;
	}

	//------------------------------------------------------------------------

	protected Order getLastClosedOrder(int direction, StrategyBase Strategy) {
		String strategyName = Strategy.getStrategyName();
		
		if (Strategy.Trader != null)
		{
			for(int i=Strategy.Trader.getHistoryOrdersCount()- 1; i >= 0; i--) {
				Order order = Strategy.Trader.getHistoryOrder(i);
				
				if(!order.StrategyName.equals(strategyName)) {
					continue;
				}
				
				if(!order.Symbol.equals(Strategy.MarketData.Chart(0).Symbol)) {
					continue;
				} 
				
				if(direction != 0 && order.getDirection() != direction) {
					continue;
				}
				
				if(order.OpenPrice == order.ClosePrice) {
					// no profit or loss, ignore such order
					continue;
				}
	
				return order;
			}
		}
		
		return null;
	}

}