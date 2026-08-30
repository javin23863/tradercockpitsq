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
package SQ.Blocks.CandlePatterns;

import SQ.Internal.ConditionBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="PiercingLine pattern", display="PiercingLine pattern(@Chart@) before #Shift# bars", returnType = ReturnTypes.Boolean)
@Help("Is triggered when PiercingLine pattern is formed")
@OppositeBlock("DarkCloud")
@SortOrder(700)
public class PiercingLine extends ConditionBlock {
	
	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="1")
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
    	
    	double O = Chart.Open(Shift);
    	double O1 = Chart.Open(Shift+1);
    	double C = Chart.Close(Shift);
    	double C1 = Chart.Close(Shift+1);

    	double H = Chart.High(Shift);
    	double L = Chart.Low(Shift);
    	
    	int digits = Chart.getInstrumentInfo().decimals;
    	double tickSize = Chart.getInstrumentInfo().tickSize;

    	double Piercing_Line_Ratio = 0.5;
    	double Piercing_Candle_Length = 10;
    	
    	double HL = SQUtils.round(H-L, digits);
    	double CO = SQUtils.round(C-O, digits);
    	double CO_HL = HL != 0 ? SQUtils.round(CO/HL, 6) : 0;
    	double O1C1_D2 = SQUtils.round((O1+C1)/2, digits);
    	double PCL_MTS = SQUtils.round(Piercing_Candle_Length*tickSize, digits);
    			
    	if(C1 < O1 && O1C1_D2 < C && O < C && C < O1 && CO_HL > Piercing_Line_Ratio && HL >= PCL_MTS) {
    		return true;
    	}
        
        return false;
	}

}
