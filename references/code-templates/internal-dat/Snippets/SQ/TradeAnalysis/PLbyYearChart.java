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

import java.util.Map;
import java.util.TreeMap;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class PLbyYearChart extends TradeAnalysisChart {

	public PLbyYearChart() {
		this.name = L.tsq("P/L by year");
	}
	
	@Override
	public AbstractChart draw(OrdersList orders, byte plType, byte tradePeriod) {
		BarChart chart = new BarChart();
		chart.xAxisTitle = L.tsq("PL by Year");
		chart.invertIfNegative(true);
		
		if(orders==null) {
			return chart;
		}
				
	   	if(plType==PlTypes.Money) {
	   		chart.xAxisTitle = L.tsq("PL in money by Year");
	   	} else if(plType==PlTypes.Pips) {
	   		chart.xAxisTitle = L.tsq("PL in pips by Year");
	   	} else {
	   		chart.xAxisTitle = L.tsq("PL in % by Year");
	   	}
				
		TreeMap<Integer, Double> plByYearMap = computeData(orders, plType, tradePeriod);
		
		for(Map.Entry<Integer, Double> entry : plByYearMap.entrySet()) {
			chart.addValue(L.tsq("P/L"), entry.getKey(), entry.getValue());
		}

		return chart;
	}

	private TreeMap<Integer, Double> computeData(OrdersList orders, byte plType, byte tradePeriod) {
		TreeMap<Integer, Double> plByYearMap = new TreeMap<Integer, Double>();

		double pl;
		int year;
		
		for(int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			
			year = SQTime.getFullYear(order.getTimeByPeriodType(tradePeriod));
			
			pl = order.getPLByType(plType);
			
			if(plByYearMap.containsKey(year)) {					
				plByYearMap.put(year,  plByYearMap.get(year)+pl);
			} else {
				plByYearMap.put(year, pl);
			}
		}
		
		return plByYearMap;
	}
}