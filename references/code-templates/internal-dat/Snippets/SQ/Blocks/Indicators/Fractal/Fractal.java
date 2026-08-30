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
package SQ.Blocks.Indicators.Fractal;

import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ReturnTypes;

@BuildingBlock(name="Fractal", display="Fractal(@Chart@#Fractal#)[#Shift#]", returnType = ReturnTypes.Price)
public class Fractal extends IndicatorBlock {
	
	@Parameter(defaultChartIndex=0)
	public ChartData Chart;

	@Parameter(name="Fractal", defaultValue="3")
	@Editor(type=Editors.Selection, values="3-bar=3,5-bar=5")
	public int Fractal;
	
	@Output(name="Up", color=Colors.Green, showZeroValues = false, chartType = "point")
	public DataSeries Up;
	
	@Output(name="Down", color=Colors.Red, showZeroValues = false, chartType = "point")
	public DataSeries Down;
	
	double dCurrent;
	int FractalUsed;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
	    if((Fractal - 1) / 2 <= 0){
	    	FractalUsed = 3;
	    }
	    else {
	    	FractalUsed = Fractal;
	    }
	}

	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		if (CurrentBar < FractalUsed-1) {
			Up.set(0, 0);
			Down.set(0, 0);
			return;
		}
		
		int eachSideLength = (FractalUsed - 1) / 2;
		int middleBar = eachSideLength + 1;
		
		double dCurrentHigh = Chart.High.get(middleBar); 
	    double dCurrentLow = Chart.Low.get(middleBar);
	    boolean bFoundHigh = true;
	    boolean bFoundLow = true;
	      
	    for(int a=FractalUsed; a > 0; a--){
	         if(a == middleBar) continue;
	         
	      //----Fractals up
	         if(Chart.High.get(a) >= dCurrentHigh) bFoundHigh = false;
	      //----Fractals down
	         if(Chart.Low.get(a) <= dCurrentLow) bFoundLow = false;
	    }

		Up.set(0, bFoundHigh ? dCurrentHigh : 0);
		Down.set(0, bFoundLow ? dCurrentLow : 0);
	}
}
