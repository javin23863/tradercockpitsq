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
package SQ.Blocks.Indicators.TrueRange;

import SQ.Internal.IndicatorBlock;
import SQ.Internal.ValueBlock;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(TR) TrueRange", returnType=ReturnTypes.PriceRange, display="TrueRange[@Chart@#Shift#]")
@Indicator(min=0, max=5000, step=0.001)
public class TrueRange extends IndicatorBlock {

	@Parameter
	public ChartData Chart;

	@Output(name = "TrueRange", color = Colors.Red)
	public DataSeries Value;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		double close1 = Chart.Close(1);
		double high = Chart.High(0);
		double low = Chart.Low(0);
		double TrueHigh, TrueLow;

		if(close1 > high) {
			TrueHigh = close1;
		} else {
			TrueHigh = high;
		}

		if(close1 < low) {
			TrueLow = close1;
		} else {
			TrueLow = low;
		}

		double TrueRange = TrueHigh - TrueLow ;

		Value.set(0, TrueRange);
	}
}