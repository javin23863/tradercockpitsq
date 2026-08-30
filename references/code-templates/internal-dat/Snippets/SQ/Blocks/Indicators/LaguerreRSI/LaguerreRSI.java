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
package SQ.Blocks.Indicators.LaguerreRSI;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import SQ.Internal.IndicatorBlock;

@BuildingBlock(name="(LRSI) Laguerre RSI", display="Laguerre RSI(@Chart@#Gamma#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("LaguerreRSI help text")
@Indicator(oscillator=true, middleValue=0.50, min=0, max=1, step=0.01)
@ParameterSet(set="Gamma=0.1")
@ParameterSet(set="Gamma=0.2")
@ParameterSet(set="Gamma=0.3")
@ParameterSet(set="Gamma=0.4")
@ParameterSet(set="Gamma=0.5")
@ParameterSet(set="Gamma=0.6")
@ParameterSet(set="Gamma=0.7")
@ParameterSet(set="Gamma=0.8")
@ParameterSet(set="Gamma=0.9")
public class LaguerreRSI extends IndicatorBlock {

	@Parameter
	public ChartData Chart;

	@Parameter(defaultValue="0.5", isPeriod=true, minValue=0.0, maxValue=1, step=0.01)
	public double Gamma;

	@Output
	public DataSeries LRSI;
	
	@Buffer
	public DataSeries L0;
	
	@Buffer
	public DataSeries L1;
	
	@Buffer
	public DataSeries L2;
	
	@Buffer
	public DataSeries L3;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {

		if (CurrentBar < 20) {

			LRSI.set(0,0);
			L0.set(0,0);
			L1.set(0,0);
			L2.set(0,0);
			L3.set(0,0);

		} 
	
		else {

			L0.set(0,(1-Gamma)*Chart.Close.get(0) + Gamma*L0.get(1));
			L1.set(0, - Gamma*L0.get(0) + L0.get(1)+ Gamma*L1.get(1));
			L2.set(0, - Gamma*L1.get(0) + L1.get(1)+ Gamma*L2.get(1));			
			L3.set(0, - Gamma*L2.get(0) + L2.get(1)+ Gamma*L3.get(1));

			double CU = 0;
			double CD = 0;

			if(L0.get(0)>=L1.get(0)){
				CU = L0.get(0)-L1.get(0);
			}
			else CD = L1.get(0) - L0.get(0);
			
			if(L1.get(0)>=L2.get(0)){
				CU = CU+ L1.get(0)-L2.get(0);
			}
			else CD = CD + L2.get(0) - L1.get(0);
			
			if(L2.get(0) >= L3.get(0)){
				CU = CU+ L2.get(0)-L3.get(0);
			}
			else CD = CD + L3.get(0) - L2.get(0);

			if(CU+CD !=0){
				
				LRSI.set(0,CU/(CU+CD));

			}
			else LRSI.set(0,0);
		}
	}
}