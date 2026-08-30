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

public class WinLossPLByMonthChart extends TradeAnalysisChart {

	public WinLossPLByMonthChart() {
		this.name = L.tsq("Wins/Losses Profit by month");
	}
	
	@Override
	public AbstractChart draw(OrdersList orders, byte plType, byte tradePeriod) {
		BarChart chart = new BarChart();
		chart.setCategoryColor("winners", ChartsConst.COLOR_GREEN);
		chart.setCategoryColor("losers", ChartsConst.COLOR_RED);
		
		if(orders==null) {
			return chart;
		}
		
		double[][] months = computeData(orders, plType, tradePeriod);
		
		for(int month=0; month<months.length; month++) {
			chart.addValue("winners", monthToString(month), months[month][0]);
			chart.addValue("losers", monthToString(month), months[month][1]);
		}

		return chart;
	}

	private double[][] computeData(OrdersList orders, byte plType, byte tradePeriod) {
		double[][] months = new double[12][2];
		
		for(int month=0; month<months.length; month++) { 
			months[month][0] = 0; //win
			months[month][1] = 0; //loss
		}
		
		int month;
		double pl;
		
		for(int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			
			month = SQTime.getMonth(order.getTimeByPeriodType(tradePeriod)); //0-11

			pl = order.getPLByType(plType);
			
			if(pl>0) { //win
				months[month][0]+=pl; 
			} else { //loss
				months[month][1]+=Math.abs(pl);
			}
		}
		
		return months;
	}
}