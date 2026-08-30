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
package SQ.TradeAnalysis;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class PLLongChart extends TradeAnalysisChart {

	public PLLongChart() {
		this.name = L.tsq("Long Profit/Loss");
	}
	
	@Override
	public AbstractChart draw(OrdersList orders, byte plType, byte tradePeriod) {
		PieChart chart = new PieChart();
		
		if(orders==null) {
			return chart;
		}
		
		int[] trades = computeData(orders, plType);

		chart.addValue(L.tsq("Profit"), trades[0], ChartsConst.COLOR_GREEN);
		chart.addValue(L.tsq("Loss"), trades[1], ChartsConst.COLOR_RED);
		
		return chart;
	}

	private int[] computeData(OrdersList orders, byte plType) {
		int[] trades = new int[2]; //profit, loss
		
		for(int trade=0; trade<trades.length; trade++) { 
			trades[trade]=0; 
		}

		for(int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			
			if(order.isShort()) {
				continue;
			}
			
			if(order.getPLByType(plType)>=0) { //profit
				trades[0]+=1;  
			} else { //loss
				trades[1]+=1;
			}
		}
		
		return trades;
	}
}