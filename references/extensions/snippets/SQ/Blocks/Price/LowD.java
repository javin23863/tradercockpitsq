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
package SQ.Blocks.Price;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.engine.stockpicker.constants.PickerTriggerTypes;
import com.strategyquant.tradinglib.simulator.Engines;
import SQ.Internal.ValueBlock;
import SQ.Utils.TimeDiffUtil;
import com.strategyquant.lib.*;
import java.util.Calendar;
import java.util.Date;
import com.strategyquant.tradinglib.*;
import com.strategyquant.lib.SQTime; 

@BuildingBlock(name="(DL) Daily Low", returnType=ReturnTypes.Price, display="LowD[@Chart@#Shift#]")
@SortOrder(700)
@OppositeBlock("HighD")
public class LowD extends ValueBlock {
	
	@Parameter
	public ChartData Chart;
	
	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	protected void OnInit() {
		barsShiftedSeries = new DataSeries();
		if (this.isTradestationEngine()) {
			SettingsMap settings = Strategy.getSettings();
			if(settings.containsKey(SettingsKeys.ReservedBars)) {
				reservedBars = settings.getInt(SettingsKeys.ReservedBars, 0);
			}
		}
	}
	@Override
	protected void OnDeinit() {
	   if (barsShiftedSeries != null) {
		   barsShiftedSeries.destroy();
	    }
	}
	
	@Override
	public double OnBlockEvaluate(int relativeShift) throws TradingException {
		if(Strategy.isStockpicker()) {			
			if(Strategy.Stockpicker.strategyTriggeredAt() == PickerTriggerTypes.OnBarOpen && (relativeShift + Shift == 0)) {
				return Strategy.Stockpicker.data.OpenD(chartIndex, relativeShift + Shift);
			}
			return Strategy.Stockpicker.data.LowD(chartIndex, relativeShift + Shift);
		}

		//In EasyLanguage, OHLC are shifted by bars
		if(Engines.isTradestationEngine(Strategy.getEngine())) {
			int daysOffset = TimeDiffUtil.getDiffInDays(Chart, relativeShift, true);
		
			if (Shift == 0 && daysOffset == 0 && relativeShift != 0) {
				int barIndex  = relativeShift;	 
				if (barIndex  < 0 || barIndex >= Chart.Low.size()) return 0;
				
				if (barsShiftedSeries.size() < Chart.Low.size() - reservedBars) {
					for (int i = 0; i < (Chart.Low.size() - reservedBars - barsShiftedSeries.size()); i++) {
						barsShiftedSeries.add(0);
		        	}
				}
				if (barsShiftedSeries.get(relativeShift) == 0) {
					int maxIndex = Chart.Low.size() - 1;
					double lowD = calculateLowDUpToBar(barIndex, maxIndex);
					barsShiftedSeries.set(relativeShift, lowD);
		            return lowD;
				}
				return barsShiftedSeries.get(relativeShift);
			}else {
				return Chart.LowD(Shift + daysOffset);
			}
		}
		else {
			return Chart.LowD(Shift + relativeShift);
		}
	}

    private double calculateLowDUpToBar(int barIndex, int maxIndex) throws TradingException {
        double dayLow = Double.MAX_VALUE;
        for (int i = barIndex; i <= maxIndex; i++) {
        	if (SQTime.isSameDay(Chart.Time.get(i), Chart.Time.get(barIndex))) {
                dayLow = Math.min(dayLow, Chart.Low.get(i));
            } else {
            	break;
            }
        }
        return dayLow;
    }
}