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

@ClassConfig(name="Randomly skip trades", display="Randomly skip trades, with probability #Probability# %")
@Help("Randomly skip trades")
public class RandomlySkipTrades extends MonteCarloManipulation {
	public static final Logger Log = LoggerFactory.getLogger(RandomlySkipTrades.class);
	
	@Parameter(name="Probability", defaultValue="10", minValue=1, maxValue=100, step=1)
	public int Probability;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public void modifyTrades(IRandomGenerator rng, OrdersList originalOrders) throws Exception {
		
		double dblProbability = ((double) Probability/ 100.0d);
				
		int tradesToRemove = (int) Math.round(originalOrders.size() * dblProbability);
		
		for(int i=0; i<tradesToRemove; i++) {
			
			int size = originalOrders.size();
			if(size == 0) {
				continue;
			}
			
			int tradeToRemove = rng.nextInt(size);
			
            // skip this trade - this means we have to remove it from the list of trades
			originalOrders.remove(tradeToRemove);
		}
	}	
}