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
package SQ.Blocks.Indicators.QQE;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;

import SQ.Calculators.AverageCalculator;
import SQ.Calculators.RSICalculator;
import SQ.Internal.IndicatorBlock;

@BuildingBlock(name="(QQE) Quantitative Qualitative Estimation", display="QQE(@Chart@#RSIPeriod#)[#Shift#]", returnType=ReturnTypes.Number)
@Indicator(oscillator=true, middleValue=50, min=0, max=100, step=1)
@ParameterSet(set="RSIPeriod=14,sF=5,wF=4.236")
public class QQE extends IndicatorBlock {
	
	@Parameter(defaultChartIndex=0)
	public ChartData Chart;
	
	@Parameter(category="Default", name="RSIPeriod", minValue=2, maxValue=10000, defaultValue="14", step=1, isPeriod=true)
	public int RSIPeriod;
	
	@Parameter(category="Default", name="sF", defaultValue="5", minValue=2, maxValue=650, step=1, builderMinValue=1, builderMaxValue=650, builderStep=1, isPeriod=true)
	public int sF;
	
	@Parameter(category="Default", name="wF", defaultValue="4.236", minValue=0.1, maxValue=100, builderMinValue=0.01, builderMaxValue=100, builderStep=0.025)
	public double wF; 
	
	@Output(name="QQE Value1", color=Colors.Green)
	public DataSeries Value1;
	
	@Output(name="QQE Value2", color=Colors.Red)
	public DataSeries Value2;
	
	private RSICalculator rsiCalculator;
	private AverageCalculator rsiEmaCalculator;
	private AverageCalculator atrRsiEmaCalculator;
	private AverageCalculator maAtrRsiEmaCalculator;

	private int WildersPeriod; 

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		WildersPeriod = RSIPeriod * 2 - 1;
		
		rsiCalculator = new RSICalculator(RSIPeriod);
		rsiEmaCalculator = new AverageCalculator(AverageCalculator.EMA, sF);
		atrRsiEmaCalculator = new AverageCalculator(AverageCalculator.EMA, WildersPeriod);
		maAtrRsiEmaCalculator = new AverageCalculator(AverageCalculator.EMA, WildersPeriod);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		double rsi0, rsi1, dar, tr, dv;
		
		rsiCalculator.onBarUpdate(Chart.Close.get(0), getCurrentBar());
		rsiEmaCalculator.onBarUpdate(rsiCalculator.getValue(), getCurrentBar());

		tr = Value2.get(1);
		rsi1 = Value1.get(1);
		rsi0 = rsiEmaCalculator.getValue();
		
		double atrRSI = Math.abs(rsi1 - rsi0);		
		atrRsiEmaCalculator.onBarUpdate(atrRSI, getCurrentBar());
		
		maAtrRsiEmaCalculator.onBarUpdate(atrRsiEmaCalculator.getValue(), getCurrentBar());

		//astra v1.1: Value 4.236 in v1.0 became default value for user adjustable variable "Filter".
		dar = maAtrRsiEmaCalculator.getValue() * wF; //Filter;
		dv = tr;
		
		if(rsi0 < tr){
			tr = rsi0 + dar;
			if(rsi1 < dv && tr > dv){
				tr = dv;
			}
		}
		else if(rsi0 > tr){
			tr = rsi0 - dar;
			if(rsi1 > dv && tr < dv){
				tr = dv;
			}
		}

		Value1.set(0, rsi0);
		Value2.set(0, tr);
	}
	
}
