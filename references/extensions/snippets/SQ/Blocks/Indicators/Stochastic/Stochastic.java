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
package SQ.Blocks.Indicators.Stochastic;

import SQ.Calculators.AverageCalculator;
import SQ.Calculators.HighestCalculator;
import SQ.Calculators.LowestCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;

@BuildingBlock(name="(STOCH) Stochastic", display="Stoch(@Chart@#KPeriod#, #DPeriod#, #Slowing#).#Line#[#Shift#]", returnType = ReturnTypes.Number)
@Indicator(oscillator=true, middleValue=50, min=0, max=100, step=1)
@ParameterSet(set="KPeriod=5,DPeriod=3,Slowing=3,MAMethod=0,PriceField=0")
@ParameterSet(set="KPeriod=14,DPeriod=3,Slowing=3,MAMethod=0,PriceField=0")
@ParameterSet(set="KPeriod=21,DPeriod=7,Slowing=7,MAMethod=0,PriceField=0")
public class Stochastic extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Input;
	
	@Parameter(name="%K Period", defaultValue="9", minValue=2, maxValue=10000, step=1, isPeriod=true)
	public int KPeriod;

	@Parameter(name="%D Period", defaultValue="3", minValue=2, maxValue=10000, step=1, isPeriod=true)
	public int DPeriod;

	@Parameter(defaultValue="3", minValue=2, maxValue=10000, step=1, isPeriod=true)
	public int Slowing;

	@Parameter(name="MA Method", defaultValue="0")
	@Editor(type=Editors.Selection, values="Simple=0,Exponential=1,Smoothed=2,Linear weighted=3")
	public int MAMethod;
    
	@Parameter(defaultValue="0")
	@Editor(type=Editors.Selection, values="Low/High=0,Close/Close=1")
	public int PriceField;
	
	@Output(name = "Fast %K", color = Colors.Green)
	public DataSeries FastK;
	
	@Output(name = "Slow %D", color = Colors.Red)
	public DataSeries SlowD;
	
	private HighestCalculator highestCalculator;
	private LowestCalculator lowestCalculator;
	private AverageCalculator fastKCalculator;
	private AverageCalculator slowDCalculator;
	
	private double curK, lastK;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		MAMethod = SQUtils.fixAllowedRange(MAMethod, 0, 3, 0);
		
		highestCalculator = new HighestCalculator(KPeriod);
		lowestCalculator = new LowestCalculator(KPeriod);
		fastKCalculator = new AverageCalculator(MAMethod, Slowing);
		slowDCalculator = new AverageCalculator(MAMethod, DPeriod);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		OnBarUpdateStandard();
	}

	//------------------------------------------------------------------------

	/**
	 * Standard implementation of Stochastic
	 * @throws TradingException
	 */
	private void OnBarUpdateStandard() throws TradingException {
		PriceField = SQUtils.fixAllowedRange(PriceField, 0, 1, 0);
		 
		switch(PriceField){
			case 0:
				highestCalculator.onBarUpdate(Input.High.get(0), getCurrentBar());
				lowestCalculator.onBarUpdate(Input.Low.get(0), getCurrentBar());
				break;
			case 1:
				highestCalculator.onBarUpdate(Input.Close.get(0), getCurrentBar());
				lowestCalculator.onBarUpdate(Input.Close.get(0), getCurrentBar());
				break;
		}
		
		double nom = SQUtils.round(Input.Close.get(0) - lowestCalculator.getLowestValue(), 8);
		double den = SQUtils.round(highestCalculator.getHighestValue() - lowestCalculator.getLowestValue(), 8);
		
		lastK = curK;
		
		if(den < 0.00000001 && den > -0.00000001){
			curK = CurrentBar == 0 ? 50 : lastK;
		} else {
			curK = Math.min(100, Math.max(0, 100 * nom / den));
		}
		
		fastKCalculator.onBarUpdate(curK, getCurrentBar());
		slowDCalculator.onBarUpdate(fastKCalculator.getValue(), getCurrentBar());

		FastK.set(0, fastKCalculator.getValue());
		SlowD.set(0, slowDCalculator.getValue());
	}

}
