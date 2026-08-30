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

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(LRSIR) Laguerre RSI is rising", display="Laguerre RSI(@Chart@#Gamma#)[#Shift#] is rising", returnType = ReturnTypes.Boolean)
@Help("Is triggered if Laguerre RSI is rising")
@OppositeBlock("LaguerreRSIFalling")
@ParameterSet(set="Gamma=0.1")
@ParameterSet(set="Gamma=0.2")
@ParameterSet(set="Gamma=0.3")
@ParameterSet(set="Gamma=0.4")
@ParameterSet(set="Gamma=0.5")
@ParameterSet(set="Gamma=0.6")
@ParameterSet(set="Gamma=0.7")
@ParameterSet(set="Gamma=0.8")
@ParameterSet(set="Gamma=0.9")
public class LaguerreRSIRising extends ConditionBlock {
	
	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="0.5", minValue=0, maxValue=0.95, step=0.01)
	public double Gamma;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		LaguerreRSI indicator = Strategy.Indicators.LaguerreRSI(Chart, Gamma);
		double curr = indicator.LRSI.getRounded(Shift);
		double prev = indicator.LRSI.getRounded(Shift+1);
		
		return (curr > prev);
	}

}