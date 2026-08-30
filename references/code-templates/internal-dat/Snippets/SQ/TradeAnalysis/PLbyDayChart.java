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

public class PLbyDayChart extends TradeAnalysisChart {

	public PLbyDayChart() {
		this.name = L.tsq("P/L by day");
	}
	
	@Override
	public AbstractChart draw(OrdersList orders, byte plType, byte tradePeriod) {
		BarChart chart = new BarChart();
		chart.invertIfNegative(true);
		
		if(orders==null) {
			return chart;
		}
		
		double[] days = computeData(orders, plType, tradePeriod);
		
		for(int day=0; day<days.length; day++) {
			chart.addValue(L.tsq("P/L"), day+1, days[day]);
		}
				
		return chart;
	}

	private double[] computeData(OrdersList orders, byte plType, byte tradePeriod) {
		double[] days = new double[31];
		
		for(int day=0; day<days.length; day++) { 
			days[day]=0; 
		}
		
		int day;
		
		for(int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			
			day = SQTime.getDay(order.getTimeByPeriodType(tradePeriod)); //1-31

			days[day-1]+= order.getPLByType(plType);
		}
		
		return days;
	}
}