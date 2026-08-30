/**
 * 
 */
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
package SQ.Blocks.Indicators.HeikenAshi;

import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;

@IgnoreInBuilder
@BuildingBlock(name="(HA) Heiken Ashi", returnType=ReturnTypes.Price, display="Heiken Ashi(@Chart@)[#Shift#]")
public class HeikenAshi extends IndicatorBlock {

	@Parameter
	public ChartData Chart;
	
	@Output(name="HAOpen", color=Colors.Green)
	public DataSeries HAOpen;
	
	@Output(name="HAClose", color=Colors.Green)
	public DataSeries HAClose;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		if (getCurrentBar() == 0) {
            HAOpen.set(0, Chart.Open(0));
            HAClose.set(0, Chart.Close(0));
            return;
        }

        HAClose.set(0, (Chart.Open(0) + Chart.High(0) + Chart.Low(0) + Chart.Close(0)) / 4.0);
        HAOpen.set(0, (HAOpen.get(1) + HAClose.get(1)) / 2);
	}
	
}
