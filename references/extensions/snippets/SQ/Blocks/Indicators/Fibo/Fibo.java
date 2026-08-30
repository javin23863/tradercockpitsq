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
package SQ.Blocks.Indicators.Fibo;

import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.NoShift;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;


@BuildingBlock(name="(FIB) Fibo", display="Fibo(@Chart@)", returnType = ReturnTypes.Price)
@NoShift
@ParameterSet(set="FiboRange=1,FiboLevel=23.6")
@ParameterSet(set="FiboRange=1,FiboLevel=-23.6")
@ParameterSet(set="FiboRange=1,FiboLevel=38.2")
@ParameterSet(set="FiboRange=1,FiboLevel=-38.2")
@ParameterSet(set="FiboRange=1,FiboLevel=61.8")
@ParameterSet(set="FiboRange=1,FiboLevel=-61.8")
@ParameterSet(set="FiboRange=2,FiboLevel=23.6")
@ParameterSet(set="FiboRange=2,FiboLevel=-23.6")
@ParameterSet(set="FiboRange=2,FiboLevel=38.2")
@ParameterSet(set="FiboRange=2,FiboLevel=-38.2")
@ParameterSet(set="FiboRange=2,FiboLevel=61.8")
@ParameterSet(set="FiboRange=2,FiboLevel=-61.8")
@ParameterSet(set="FiboRange=5,FiboLevel=23.6")
@ParameterSet(set="FiboRange=5,FiboLevel=-23.6")
@ParameterSet(set="FiboRange=5,FiboLevel=38.2")
@ParameterSet(set="FiboRange=5,FiboLevel=-38.2")
@ParameterSet(set="FiboRange=5,FiboLevel=61.8")
@ParameterSet(set="FiboRange=5,FiboLevel=-61.8")
@ParameterSet(set="FiboRange=6,FiboLevel=23.6")
@ParameterSet(set="FiboRange=6,FiboLevel=-23.6")
@ParameterSet(set="FiboRange=6,FiboLevel=38.2")
@ParameterSet(set="FiboRange=6,FiboLevel=-38.2")
@ParameterSet(set="FiboRange=6,FiboLevel=61.8")
@ParameterSet(set="FiboRange=6,FiboLevel=-61.8")
public class Fibo extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;
	
	@Parameter(defaultValue="1", name = "FiboRange")
	@Help("Choose range of Fibo indicator")
	@Editor(type=Editors.Selection, values="High-Low previous day=1,High-low previous week=2,High-low previous month=3,Open-Close previous day=5,Open-Close previous week=6,Open-Close previous month=7")
	public int FiboRange;
	
	@Parameter(name = "FiboLevel", defaultValue="76.4")
	@Help("Only for some of Fibo ranges - select value of X here")
	@Editor(type=Editors.Selection, values="-423.6=-423.6,-261.8=-261.8,-161.8=-161.8,-127.2=-127.2,-100.0=-100.0,-78.6=-78.6,-61.8=-61.8,-50.0=-50.0,-38.2=-38.2,-23.6=-23.6,0.0=0.0,23.6=23.6,38.2=38.2,50.0=50.0,61.8=61.8,78.6=78.6,100.0=100.0,127.2=127.2,161.8=161.8,261.8=261.8,423.6=423.6")
	public double FiboLevel;

	@Output(name="Value", color=Colors.Green)
	public DataSeries Value;	
	
	private long tfEndTime = 0;
	private int barsUsed = -1;
	private double prevTFOpen, prevTFHigh, prevTFLow, prevTFClose;
	private double fiboLevel;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		FiboRange = SQUtils.fixAllowedRange(FiboRange, 1, 7, 1);
		
		if(isNewTFStart()){
			double upperValue = 0, lowerValue = 0;
			
			switch(FiboRange){
				case 0:
				case 1:
				case 2:
				case 3:
					upperValue = prevTFHigh;
					lowerValue = prevTFLow;
					break;
				case 5:
				case 6:
				case 7:
					upperValue = Math.max(prevTFOpen, prevTFClose);
					lowerValue = Math.min(prevTFOpen, prevTFClose);
					break;
			}
			
			double fiboPct = FiboLevel;
			double percentStep = (upperValue - lowerValue) / 100;
			double delta = fiboPct * percentStep;

			boolean bullish = prevTFClose > prevTFOpen;
			
			fiboLevel = bullish ? (upperValue - delta) : (lowerValue + delta);
					
			prevTFOpen = Chart.Open(0);
			prevTFHigh = Chart.High(0);
			prevTFLow = Chart.Low(0);
			prevTFClose = Chart.Close(0);
			
			barsUsed = 1;
		}
		else {
			prevTFHigh = Math.max(prevTFHigh, Chart.High(0));
			prevTFLow = Math.min(prevTFLow, Chart.Low(0));
			prevTFClose = Chart.Close(0);
			
			barsUsed++;
		}

		Value.set(0, fiboLevel);
	}
	
	//------------------------------------------------------------------------
	
	private boolean isNewTFStart() throws TradingException {
		long curTime = Chart.Time(0);
		
		switch(FiboRange){
			case 9:
			case 10:
				if(barsUsed == -1){
					return true;
				}
				else return false;
			case 0:
			case 1:
			case 2:
			case 3:
			case 4:
			case 5:
			case 6:
			case 7:
			case 8:
			default: 
				if(tfEndTime == 0 || tfEndTime <= curTime){
					setEndTime();
					return true;
				}
				else return false;
		}
	}
	
	//------------------------------------------------------------------------

	private void setEndTime() throws TradingException{
		long curDayStart = SQTime.setTime(Chart.Time(0), 0, 0, 0, 0);
		
		switch(FiboRange){
			case 0:
			case 1:
			case 5:
				tfEndTime = SQTime.addDays(curDayStart, 1);
				break;
			case 2:
			case 6:
				tfEndTime = SQTime.setDayOfWeek(curDayStart, SQTime.MONDAY);
				tfEndTime = SQTime.addDays(tfEndTime, 7);
				break;
			case 3:
			case 7:
				tfEndTime = SQTime.setDayOfMonth(curDayStart, 1);
				tfEndTime = SQTime.addMonths(tfEndTime, 1);
				break;
			case 4:
				break;
		}
	}
	
}