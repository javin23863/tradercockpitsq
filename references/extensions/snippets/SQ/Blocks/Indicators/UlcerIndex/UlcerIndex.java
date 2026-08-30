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
package SQ.Blocks.Indicators.UlcerIndex;

import com.strategyquant.datalib.*;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.*;
import SQ.Calculators.*;
import SQ.Internal.IndicatorBlock;

@BuildingBlock(name="(UI) Ulcer Index", display="Ulcer Index(@Chart@#Mode#,#Period#)[#Shift#]", returnType = ReturnTypes.Number)
@Help("Ulcer Index indicator")
@ParameterSet(set="Period=12")
@ParameterSet(set="Period=24")
@ParameterSet(set="Period=48")
@ParameterSet(set="Period=96")
@ParameterSet(set="Period=120")
@ParameterSet(set="Period=10")
@ParameterSet(set="Period=20")
@ParameterSet(set="Period=40")
@ParameterSet(set="Period=100")
@ParameterSet(set="Period=200")
public class UlcerIndex extends IndicatorBlock {

	@Parameter(defaultChartIndex=0)
	public ChartData Input;

	@Parameter(defaultValue="1")
	@Editor(type=Editors.Selection, values="UP UI=1,Down UI=2")
	public int Mode;

	@Parameter(defaultValue="24", isPeriod=true, minValue=2, maxValue=1000, step=1)
	public int Period;

	@Output
	public DataSeries Value;

	@Buffer
	public DataSeries ddBuffer;

	private HighestCalculator highestCalculator;
	private LowestCalculator lowestCalculator;


	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------


	protected void OnInit() throws TradingException {
		highestCalculator = new HighestCalculator(Period);
		lowestCalculator = new LowestCalculator(Period);
	}

	@Override
	protected void OnBarUpdate() throws TradingException {

		if (CurrentBar < Period+1) {
			// what to set when there are less bars than indicator period parameter
			Value.set(0,0);
			ddBuffer.set(0,0);
		}
		else{

			if(Mode == 1){ // UP UI=1

				highestCalculator.onBarUpdate(Input.Close.get(0), getCurrentBar());
				double hc = highestCalculator.getHighestValue();
				double dd =  100d*((Input.Close.get(0))-hc)/hc;
				ddBuffer.set(0,dd);
				double sum =0;

				for(int k = 0;k<Period;k++){
					sum = sum + Math.pow(ddBuffer.get(k),2);
				}

				double uI = Math.sqrt((sum/Period));

				Value.set(0,uI);
			}
			else if(Mode == 2){ // Down UI=2

				lowestCalculator.onBarUpdate(Input.Close.get(0), getCurrentBar());
				double lc = lowestCalculator.getLowestValue();
				double dd =  100d*((Input.Close.get(0))-lc)/lc;
				ddBuffer.set(0,dd);
				double sum =0;

				for(int k = 0;k<Period;k++){
					sum = sum + Math.pow(ddBuffer.get(k),2);
				}

				double uI = Math.sqrt((sum/Period));

				Value.set(0,uI);
			}
		}

	}
} 