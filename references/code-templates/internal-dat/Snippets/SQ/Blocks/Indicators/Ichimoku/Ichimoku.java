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
package SQ.Blocks.Indicators.Ichimoku;

import SQ.Calculators.HighestCalculator;
import SQ.Calculators.LowestCalculator;
import SQ.Internal.IndicatorBlock;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;

@BuildingBlock(display="Ichimoku(@Chart@#TenkanPeriod#,#KijunPeriod#,#SenkouPeriod#,#Line#)[#Shift#]", returnType = ReturnTypes.Price)
@ParameterSet(set="TenkanPeriod=9,KijunPeriod=26,SenkouPeriod=52")
public class Ichimoku extends IndicatorBlock {

	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="9", name="Tenkan", isPeriod=true)
	public int TenkanPeriod;
	
	@Parameter(defaultValue="26", name="Kijun", isPeriod=true)
	public int KijunPeriod;
	
	@Parameter(defaultValue="52", name="Senkou", isPeriod=true)
	public int SenkouPeriod;
	
	@Output(name="TenkanSen", color=Colors.Green)
	public DataSeries TenkanSen;
	
	@Output(name="KijunSen", color=Colors.Green)
	public DataSeries KijunSen;
	
	@Output(name="SenkouSpanA", color=Colors.Green)
	public DataSeries SenkouSpanA;
	
	@Output(name="SenkouSpanB", color=Colors.Green)
	public DataSeries SenkouSpanB;
	/*
	@Output(name="ChikouSpan", color=Colors.Green)
	public DataSeries ChikouSpan;*/
	
	@Buffer
	public DataSeries SumForSenkouA, SumForSenkouB;
	
	private HighestCalculator tenkanHighestCalculator;
	private LowestCalculator tenkanLowestCalculator;
	private HighestCalculator kijunHighestCalculator;
	private LowestCalculator kijunLowestCalculator;
	private HighestCalculator senkouHighestCalculator;
	private LowestCalculator senkouLowestCalculator;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnInit() throws TradingException {
		tenkanHighestCalculator = new HighestCalculator(TenkanPeriod);
		tenkanLowestCalculator = new LowestCalculator(TenkanPeriod);
		kijunHighestCalculator = new HighestCalculator(KijunPeriod);
		kijunLowestCalculator = new LowestCalculator(KijunPeriod);
		senkouHighestCalculator = new HighestCalculator(SenkouPeriod);
		senkouLowestCalculator = new LowestCalculator(SenkouPeriod);
	}
	
	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		tenkanHighestCalculator.onBarUpdate(Chart.High.get(0), getCurrentBar());
		tenkanLowestCalculator.onBarUpdate(Chart.Low.get(0), getCurrentBar());
		kijunHighestCalculator.onBarUpdate(Chart.High.get(0), getCurrentBar());
		kijunLowestCalculator.onBarUpdate(Chart.Low.get(0), getCurrentBar());
		senkouHighestCalculator.onBarUpdate(Chart.High.get(0), getCurrentBar());
		senkouLowestCalculator.onBarUpdate(Chart.Low.get(0), getCurrentBar());
		
		double ts, ks, sa, sb;		
		double median = Chart.Median.get(0);
		
		sa = median;
		sb = median;
		
		if(CurrentBar >= TenkanPeriod){
			ts = (tenkanHighestCalculator.getHighestValue() + tenkanLowestCalculator.getLowestValue()) / 2;
		}
		else {
			ts = median;
		}
		
		//ChikouSpan.set(0, 0);
		
		if(CurrentBar >= KijunPeriod ){
			ks = (kijunHighestCalculator.getHighestValue() + kijunLowestCalculator.getLowestValue()) / 2;
			//ChikouSpan.set(KijunPeriod, Chart.Close.get(0));
		}
		else {
			ks = median;
		}
			
		SumForSenkouA.set(0, (ts + ks) / 2);
		
		if(CurrentBar >= SenkouPeriod){
			SumForSenkouB.set(0, (senkouHighestCalculator.getHighestValue() + senkouLowestCalculator.getLowestValue()) / 2);
		}
		else {
			SumForSenkouB.set(0, median);
		}
		
		if(CurrentBar >= SenkouPeriod && CurrentBar >= KijunPeriod){
			sa = SumForSenkouA.get(KijunPeriod);
			sb = SumForSenkouB.get(KijunPeriod);
		}
		
        TenkanSen.set(0, ts);
        KijunSen.set(0, ks);
        SenkouSpanA.set(0, sa);
		SenkouSpanB.set(0, sb);
	}
	
}
