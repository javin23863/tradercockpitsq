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

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;

import SQ.Internal.ConditionBlock;
/*
 * Ichmoku signals as seen in:
 * https://www.ichimokutrader.com/signals.html
 */
@BuildingBlock(name="(Ichimoku) Senkou Span cross bearish", display="Ichimoku(@Chart@#TenkanPeriod#,#KijunPeriod#,#SenkouPeriod#)[#Shift#] Senkou Span cross bearish", returnType = ReturnTypes.Boolean)
@OppositeBlock("IchimokuSenkouSpanCrossBullish")
@ParameterSet(set="TenkanPeriod=9,KijunPeriod=26,SenkouPeriod=52")
@ParameterSet(set="TenkanPeriod=9,KijunPeriod=26,SenkouPeriod=52,SignalStrength=2")
public class IchimokuSenkouSpanCrossBearish extends ConditionBlock {
	
	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="9", name="Tenkan", isPeriod=true)
	public int TenkanPeriod;
	
	@Parameter(defaultValue="26", name="Kijun", isPeriod=true)
	public int KijunPeriod;
	
	@Parameter(defaultValue="52", name="Senkou", isPeriod=true)
	public int SenkouPeriod;
	
	@Parameter(name="Signal strength (at least)", defaultValue="1")
	@Editor(type=Editors.Selection, values="Weak=0,Neutral=1,Strong=2")
	public int SignalStrength;

	@Parameter
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
		Ichimoku ichimoku = Strategy.Indicators.Ichimoku(Chart, TenkanPeriod, KijunPeriod, SenkouPeriod);

		double senkouSpanA1 = ichimoku.SenkouSpanA.getRounded(Shift+1);
		double senkouSpanA0 = ichimoku.SenkouSpanA.getRounded(Shift);
		double senkouSpanB1 = ichimoku.SenkouSpanB.getRounded(Shift+1);
		double senkouSpanB0 = ichimoku.SenkouSpanB.getRounded(Shift);
		
		double kumoTop = Math.max(senkouSpanA0,  senkouSpanB0); 
		double kumoBottom = Math.min(senkouSpanA0,  senkouSpanB0); 

		boolean signal = (senkouSpanA1 > senkouSpanB1) && (senkouSpanA0 < senkouSpanB0);
		
		SignalStrength = SQUtils.fixAllowedRange(SignalStrength, 0, 2, 1);
		
		if(SignalStrength == 2) {
			// for strong signal the cross should happen below kumo cloud
			signal = signal && (Chart.Close(Shift) < kumoBottom);
			
		} else if(SignalStrength == 1) {
			// for neutral signal the cross should happen in kumo cloud
			signal = signal && (Chart.Close(Shift) < kumoTop);
			
		} else if(SignalStrength == 0) {
			// do nothing, if there is cross signal is always at least weak

		}
		
		return signal;
	}

}
