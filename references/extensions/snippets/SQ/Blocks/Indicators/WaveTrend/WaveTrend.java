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
package SQ.Blocks.Indicators.WaveTrend;

import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;

@BuildingBlock(name="(WAVT) WaveTrend", display="WaveTrend(@Chart@#ChannelLength#,#AverageLength#).#Line#[#Shift#]")
@Indicator(oscillator=true, middleValue=0, min=-80, max=80, step=0.1)
@ForEngine("MT4,MT5,TS,MC")
@ParameterSet(set="ChannelLength=10,AverageLength=21")
@ParameterSet(set="ChannelLength=9,AverageLength=12")
@ParameterSet(set="ChannelLength=14,AverageLength=21")
@ParameterSet(set="ChannelLength=10,AverageLength=21,ComputedFrom=0")
@ParameterSet(set="ChannelLength=9,AverageLength=12,ComputedFrom=0")
@ParameterSet(set="ChannelLength=14,AverageLength=21,ComputedFrom=0")
public class WaveTrend extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;
	
	@Parameter(category="Default", name="ChannelLength", minValue=2, maxValue=200, defaultValue="10", step=1, isPeriod=true)
	public int ChannelLength;
	
	@Parameter(category="Default", name="AverageLength", minValue=2, maxValue=200, defaultValue="21", step=1, isPeriod=true)
	public int AverageLength;

	@Output(name="WT1", color=Colors.Green)
	public DataSeries WT1;
	
	@Output(name="WT2", color=Colors.Red)
	public DataSeries WT2;
	
	@Buffer
	public DataSeries avgPrice, esa, d, ci;

	// EMA multipliers
	private double esaMultiplier;
	private double dMultiplier;
	private double tciMultiplier;
	private double wt2Multiplier;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		// Calculate EMA multipliers
		esaMultiplier = 2.0 / (ChannelLength + 1.0);
		dMultiplier = 2.0 / (ChannelLength + 1.0);
		tciMultiplier = 2.0 / (AverageLength + 1.0);
		wt2Multiplier = 2.0 / (4 + 1.0); // SMA(4) approximated with EMA
	}

	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		// Calculate average price (HLC3)
		double currentAvgPrice = (Chart.High.get(0) + Chart.Low.get(0) + Chart.Close.get(0)) / 3.0;
		avgPrice.set(0, currentAvgPrice);
		
		if (getCurrentBar() == 0) {
			// Initialize first values
			esa.set(0, currentAvgPrice);
			d.set(0, 0);
			ci.set(0, 0);
			WT1.set(0, 0);
			WT2.set(0, 0);
		} else {
			// Calculate ESA (Exponential Smoothed Average)
			double currentEsa = esaMultiplier * currentAvgPrice + (1 - esaMultiplier) * esa.get(1);
			esa.set(0, currentEsa);
			
			// Calculate D (smoothed absolute deviation)
			double absoluteDiff = Math.abs(currentAvgPrice - currentEsa);
			double currentD = dMultiplier * absoluteDiff + (1 - dMultiplier) * d.get(1);
			d.set(0, currentD);
			
			// Calculate CI (similar to CCI)
			if (currentD != 0) {
				double currentCi = (currentAvgPrice - currentEsa) / (0.015 * currentD);
				ci.set(0, currentCi);
				
				// Calculate TCI (smoothed CI) - this becomes WT1
				double currentWT1 = tciMultiplier * currentCi + (1 - tciMultiplier) * WT1.get(1);
				WT1.set(0, currentWT1);
				
				// Calculate WT2 (smoothed WT1)
				double currentWT2 = wt2Multiplier * currentWT1 + (1 - wt2Multiplier) * WT2.get(1);
				WT2.set(0, currentWT2);
			} else {
				ci.set(0, 0);
				WT1.set(0, WT1.get(1));
				WT2.set(0, WT2.get(1));
			}
		}
	}
}