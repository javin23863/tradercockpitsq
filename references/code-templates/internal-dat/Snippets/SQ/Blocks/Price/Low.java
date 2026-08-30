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

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ReturnTypes;
import com.strategyquant.tradinglib.SortOrder;
import com.strategyquant.tradinglib.engine.stockpicker.constants.PickerTriggerTypes;

import SQ.Internal.ValueBlock;

@BuildingBlock(name="(L) Low", returnType=ReturnTypes.Price, display="Low[@Chart@#Shift#]")
@SortOrder(700)
@OppositeBlock("High")
public class Low extends ValueBlock {
	
	@Parameter
	public ChartData Chart;
	
	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public double OnBlockEvaluate(int relativeShift) throws TradingException {
		if(Strategy.isStockpicker()) {			
			if(Strategy.Stockpicker.strategyTriggeredAt() == PickerTriggerTypes.OnBarOpen && (relativeShift + Shift == 0)) {
				return Strategy.Stockpicker.data.Open(chartIndex, relativeShift + Shift);
			}
			
			return Strategy.Stockpicker.data.Low(chartIndex, relativeShift + Shift);
		}
		
		return Chart.Low(relativeShift + Shift);
	}

}