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
package SQ.Blocks.Indicators.BarRange;

import SQ.Internal.IndicatorBlock;
import SQ.Internal.ValueBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="Highest in range", display="HighestInRange(@Chart@#TimeFrom#, #TimeTo#)[#Shift#]", returnType = ReturnTypes.Price)
@Help("returns highest high of candles in given time range")
@Indicator
@OppositeBlock("LowestInRange")
public class HighestInRange extends IndicatorBlock {

	@Parameter
	public ChartData Input;
	
	@Parameter(defaultValue="0", minValue=0, maxValue=2359, step=30)
	@Help("Time in format HHMM, for example 945 or 1430")
	public int TimeFrom;
	
	@Parameter(defaultValue="0", minValue=0, maxValue=2359, step=30)
	@Help("Time in format HHMM, for example 945 or 1430")
	public int TimeTo;
	
	@Output
	public DataSeries Value;
	
	private final int DayMillis = 24 * 60 * 60 * 1000;
	private final int HourMillis = 60 * 60 * 1000;
	private final int MinuteMillis = 60 * 1000;
	
	private long timeFrom = -1, timeTo = -1;
	private double highestValue = 0;
	private double lastValue, lastUsableValue;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		if(getCurrentBar() == 0) {
			long curDayStartTime = SQTime.correctDayStart(Input.Time(0));
			
			timeFrom = curDayStartTime + (TimeFrom / 100 * HourMillis) + ((TimeFrom % 100) * MinuteMillis);
			timeTo = curDayStartTime + (TimeTo / 100 * HourMillis) + ((TimeTo % 100) * MinuteMillis);
			
			if(timeTo < timeFrom) {
				timeTo += DayMillis;
			}
		}
		
		long currentTime = Input.Time(0);
		
		if(currentTime >= timeTo) {
	        //time range is over, set new indicator value and recalculate start/end times
			long curDayStartTime = SQTime.correctDayStart(currentTime);

			timeFrom = curDayStartTime + (TimeFrom / 100 * HourMillis) + ((TimeFrom % 100) * MinuteMillis);
			timeTo = curDayStartTime + (TimeTo / 100 * HourMillis) + ((TimeTo % 100) * MinuteMillis);

			lastValue = highestValue;
            highestValue = 0;
	         
	        if(timeTo <= currentTime){      
	            if(TimeTo < TimeFrom) {
	            	timeTo += DayMillis;
	            }
	            else {
	            	timeFrom += DayMillis;
		        	timeTo += DayMillis;
	            }
	        }
        	
        	if(timeFrom <= currentTime) {
        		highestValue = Input.High(0);
        	}
		}
		else if(currentTime >= timeFrom) {
			highestValue = Math.max(highestValue, Input.High(0));
		}
		else {
			highestValue = 0;
		}

		//avoid outputting zero values if there was a gap in data
	    if(lastValue > 0){
	        lastUsableValue = lastValue;
	        
	        Value.set(0, lastValue);
	    }
	    else {
	        Value.set(0, lastUsableValue);
	    }
		
	}
}
