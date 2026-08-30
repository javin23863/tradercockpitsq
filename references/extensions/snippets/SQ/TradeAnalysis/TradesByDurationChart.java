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

import java.util.ArrayList;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class TradesByDurationChart extends TradeAnalysisChart {
	private ArrayList<TimeDuration> timeDurationList;
	
	public TradesByDurationChart() {
		this.name = L.tsq("Trades by duration");
	}
	
	@Override
	public AbstractChart draw(OrdersList orders, byte plType, byte tradePeriod) {
		BarChart chart = new BarChart();
		
		if(orders==null) {
			return chart;
		}
		
		int[] trades = computeData(orders, plType);
		
		for(int trade=0; trade<trades.length; trade++) {
			chart.addValue(L.tsq("Trades"), timeDurationList.get(trade).toString(), trades[trade]);
		}
		/*
		chart.setDomainAxisLabelPositions(SQChart.CategoryLabelPositions_UP_90);
		chart.setDomainAxisLabelFont(new Font("Arial Unicode MS", Font.PLAIN, 10));
		chart.setIntegerRangeAxis(true);
		*/
		return chart;
	}

	private int[] computeData(OrdersList orders, byte plType) {
		timeDurationList = calculateTimeDurationScale(orders);	

		int[] trades = new int[timeDurationList.size()];
		
		for(int trade=0; trade<trades.length; trade++) { 
			trades[trade]=0; 
		}
		
		for(int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			
			for(int index=0; index<timeDurationList.size(); index++) {
				if(order.Duration<timeDurationList.get(index).seconds) {
					trades[index]+=1;  
					
					break;
				}
			}
		}
		
		return trades;
	}
}