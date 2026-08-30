/*
 * Copyright (c) 2021, StrategyQuant & clonex - All rights reserved.
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
package SQ.Blocks.Indicators.SRPercentRank;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import SQ.Blocks.Indicators.MTATR.MTATR;
import SQ.Internal.IndicatorBlock;
import java.util.*; 
import com.strategyquant.tradinglib.StrategyBase;

import SQ.Blocks.Functions.*;

/**.*
 * Indicator name as it will be displayed in UI, and its return type.
 * Possible return types:
 * ReturnTypes.Price - indicator is drawn on the price chart, like SMA, Bollinger Bands etc.
 * ReturnTypes.Number - indicator is drawn on separate chart, like CCI, RSI, MACD
 * ReturnTypes.PriceRange - indicator is price range, like ATR.
 */
@BuildingBlock(name="(SRPC) SRPercRank", display="SRPercRank(@Chart@#Mode#,#Length#,#ATRPeriod#)[#Shift#]",returnType = ReturnTypes.Number)
@Help("SRPercRank helps you to identify Support/Ressistance Levels. Indicator si also able to help you with breakout areas identifications. Check blog post: adress")
@Indicator(oscillator=true, middleValue=50, min=0, max=100, step=0.1)
@ParameterSet(set="Mode=1,Length=12,ATRPeriod=12")
@ParameterSet(set="Mode=1,Length=24,ATRPeriod=12")
@ParameterSet(set="Mode=1,Length=48,ATRPeriod=12")
@ParameterSet(set="Mode=1,Length=120,ATRPeriod=12")
@ParameterSet(set="Mode=1,Length=240,ATRPeriod=12")
@ParameterSet(set="Mode=1,Length=480,ATRPeriod=12")
@ParameterSet(set="Mode=2,Length=12,ATRPeriod=12")
@ParameterSet(set="Mode=2,Length=24,ATRPeriod=12")
@ParameterSet(set="Mode=2,Length=48,ATRPeriod=12")
@ParameterSet(set="Mode=2,Length=120,ATRPeriod=12")
@ParameterSet(set="Mode=2,Length=240,ATRPeriod=12")
@ParameterSet(set="Mode=2,Length=480,ATRPeriod=12")
public class SRPercentRank extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(defaultValue="2")
	@Editor(type=Editors.Selection, values="Basic Mode=1,ATR Mode=2")
	public int Mode;

	@Parameter(defaultValue="120", isPeriod=true, builderMinValue=2, builderMaxValue=500, minValue=2, maxValue=500, step=1)
	public int Length;

	@Parameter(defaultValue="12", isPeriod=true, minValue=5, maxValue=120, step=1)
	public int ATRPeriod;

	@Output
	public DataSeries Value;
	

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	@Override
	protected void OnBarUpdate() throws TradingException {

		if (CurrentBar < Length+1) {
			return;
		}

      	int count = 0;
		double percrank = 0;

		MTATR indicator = Indicators.MTATR(Chart, ATRPeriod); /// we are using MetaTrader implementation of ATR 
		double atrValue = indicator.Value.getRounded(Shift);
		
		for(int i =1; i<=Length;i++){

			if(Mode == 1){
				/// Basic
				if (Chart.Close.get(0) > Chart.Low.get(i) && Chart.Close.get(0) < Chart.High.get(i)){
					count++;
				}
			}
			else if(Mode == 2){
				/// +- ATR(ATRPeriod)
				if (Chart.Close.get(0)> (Chart.Low.get(i)-atrValue) && Chart.Close.get(0) < (Chart.High.get(i)+atrValue)){
					count++;
				}
			}
		}
		percrank = (double)count/Length*100;
		
		Value.set(0,percrank);
	}
}