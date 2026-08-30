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

public class PLGrowthByDurationChart extends TradeAnalysisChart {
	
	private TimeSeriesScatterChart chart = null;
	private TimeSeries winSeries;
	private TimeSeries lossSeries;
	
	public PLGrowthByDurationChart() {
		this.name = L.tsq("P/L Growth by duration");
		
		this.chart = new TimeSeriesScatterChart();
		this.chart.xAxisTicksDurationInMS = true;
		
		this.winSeries = new TimeSeries("losers");
		this.winSeries.color = ChartsConst.COLOR_GREEN;
		this.chart.addSeries(this.winSeries);

		this.lossSeries = new TimeSeries("winners");
		this.lossSeries.color = ChartsConst.COLOR_RED;
		this.chart.addSeries(this.lossSeries);
	}
	
	@Override
	public AbstractChart draw(OrdersList orders, byte plType, byte tradePeriod) {
		this.lossSeries.clear();
		this.winSeries.clear();
		
		if(orders==null) {
			return this.chart;
		}
		
		ArrayList<double[]> trades = computeData(orders, plType);
		
		for(int trade=0; trade<trades.size(); trade++) {
			double[] values = trades.get(trade);
			
			if(values[1]<0) {
				this.lossSeries.addValue((long)values[0], values[1]);
			} else {
				this.winSeries.addValue((long)values[0], values[1]);
			}
		}

		return chart;
	}

	private ArrayList<double[]> computeData(OrdersList orders, byte plType) {
		ArrayList<double[]> trades = new ArrayList<double[]>();
		
		new Order();
		for(int i = 0; i < orders.size(); i++) {
			Order order = orders.get(i);
			
			double[] values = new double[2];					
			values[0] = order.Duration;
			values[1] = order.getPLByType(plType);
			
			trades.add(values);
		}
		
		return trades;
	}
}