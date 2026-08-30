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
package SQ.MonteCarlo.Manipulation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@ClassConfig(name="Randomize trades order", display="Randomize trades order, with method #Method#")
@Help("Randomizes order of trades using one of the methods. Please note that Resampling method is much more memory intensive than Exact method.")
public class RandomizeTradesOrder extends MonteCarloManipulation {
	public static final Logger Log = LoggerFactory.getLogger(RandomizeTradesOrder.class);
	
	@Parameter(name="Method", defaultValue="resampling")
	@Editor(type=Editors.Selection, values="Exact=exact, Resampling=resampling")
	public String Method;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	public void modifyTrades(IRandomGenerator rng, OrdersList originalOrders) throws Exception {

		// first compute first and last order date, and average distance of orders
		long firstOrderOpen = -1;
		long lastOrderClose = -1;
		long avgTimeBetweenOrders = 0;
		long avgOrderDuration = 0;
		long previousCloseTime = -1;
		
		for(int i=0; i<originalOrders.size(); i++) {
			Order o = originalOrders.get(i);

			if(firstOrderOpen == -1 || firstOrderOpen > o.OpenTime) {
				firstOrderOpen = o.OpenTime;
			}
			if(lastOrderClose == -1 || lastOrderClose < o.CloseTime) {
				lastOrderClose = o.CloseTime;
			}
			if(previousCloseTime != -1) {
				avgTimeBetweenOrders += Math.abs(o.OpenTime - previousCloseTime);
			}
			previousCloseTime = o.CloseTime;
			
			avgOrderDuration += o.CloseTime - o.OpenTime;
		}
		
		// now perform randomization
		if(Method.toLowerCase().equals("exact")) { //Exact           
			// only shuffles the order of trades 
			originalOrders.shuffle();

		} else { //Resampling
			int sizeOfOrders = originalOrders.size();

			// copy randomly picked trades to temporary array                        
			OrdersList randomizedOrdersList = new OrdersList("RandomizeTradesOrder");
			
			for(int k=0; k<sizeOfOrders; k++) {
				Order o = originalOrders.get(rng.nextInt(sizeOfOrders));

				if(!randomizedOrdersList.contains(o)) {
					randomizedOrdersList.add(o);
				} else {
					randomizedOrdersList.add(new Order(o));
				}
			}

			// clear original orders array
			originalOrders.clear();

			// add resampled orders to original array
			originalOrders.addAll(randomizedOrdersList);
			
			randomizedOrdersList.clear();
		}
		
		// order stats and equity chart are computed using their open and close time. 
		// By just changing the order of trades we wouldn't achieve anything, we have to update also order times so that they follow in the right order
		avgTimeBetweenOrders = (long) SQUtils.safeDivide(avgTimeBetweenOrders, originalOrders.size());
		avgOrderDuration = (long) SQUtils.safeDivide(avgOrderDuration, originalOrders.size());
		
		fixOrderTimes(originalOrders, firstOrderOpen, lastOrderClose, avgTimeBetweenOrders, avgOrderDuration);
	}

	//------------------------------------------------------------------------

	private void fixOrderTimes(OrdersList originalOrders, long firstOrderOpen, long lastOrderClose, long avgTimeBetweenOrders, long avgOrderDuration) {

		// it doesn't matter here what dates are set, only that they are going one after another
		long startTime = firstOrderOpen;
		
		for(int i=0; i<originalOrders.size(); i++) {
			Order o = originalOrders.get(i);
			
			// we have to keep the same trade duration so that constants like TotalTradingDays and CAGR are computed correctly
			o.OpenTime = startTime;
			o.CloseTime = o.OpenTime + avgOrderDuration;
			
			startTime += avgTimeBetweenOrders;
		}
	}
}