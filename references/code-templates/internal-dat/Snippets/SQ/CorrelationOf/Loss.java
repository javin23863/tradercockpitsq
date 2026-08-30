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
package SQ.CorrelationOf;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.TimePeriods;
import com.strategyquant.tradinglib.CorrelationLib;
import com.strategyquant.tradinglib.CorrelationType;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;

public class Loss extends CorrelationType {

	public Loss() {
		name = L.tsq("Loss");
		dataType = DATA_TYPE_PL;
	}

	@Override
	public void computePeriods(OrdersList orders, int period, TimePeriods periods) throws Exception {
		long ms;
			
		for(int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			
			if(isCanceledOrder(order)) {
				continue;
			}
			
			ms = CorrelationLib.getCorrectPeriod(order.CloseTime, period);

			if(!periods.containsKey(ms)) {
				throw new Exception("Period '"+SQTime.toDateMinuteString(ms)+"' not found!");
			}
			
			periods.get(ms).value += order.PL;
		}
	}
	
	@Override
	public boolean shouldSkipPeriod(double value1, double value2) {
		// Skip periods where both strategies had no losses
		return value1 >= 0 && value2 >= 0;
	}
}