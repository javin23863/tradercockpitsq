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

@BuildingBlock(name="Hammer pattern", display="Hammer pattern(@Chart@) before #Shift# bars", returnType = ReturnTypes.Boolean)
@Help("Is triggered when Hammer pattern is formed")
@OppositeBlock("ShootingStar")
@SortOrder(200)
public class Hammer extends ConditionBlock {
	
	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="1")
	public int Shift;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	public boolean OnBlockEvaluate() throws TradingException {
    	double tickSize = Chart.getInstrumentInfo().tickSize;
    	int digits = Chart.getInstrumentInfo().decimals;
    	
    	double H = Chart.High(Shift);
    	double L = Chart.Low(Shift);
    	double L1 = Chart.Low(Shift+1);
    	double L2 = Chart.Low(Shift+2);
    	double L3 = Chart.Low(Shift+3);
    	
    	double O = Chart.Open(Shift);
    	double C = Chart.Close(Shift);
    	double CL = SQUtils.round(H-L, digits);
    	
    	double BodyLow, BodyHigh;
    	double Candle_WickBody_Percent = 0.9;
    	double CandleLength = 12;
    	
    	if (O > C) {
            BodyHigh = O;
            BodyLow = C;  
        } 
    	else {
            BodyHigh = C;
            BodyLow = O; 
        }
    	
    	double LW = SQUtils.round(BodyLow - L, digits);
	    double UW = SQUtils.round(H - BodyHigh, digits);
	    double BLa = SQUtils.round(Math.abs(O - C), digits);
	    double BL90 = SQUtils.round(BLa * Candle_WickBody_Percent, digits);
	   
	    double LW_D2 = SQUtils.round(LW / 2, digits);
	    double LW_D3 = SQUtils.round(LW / 3, digits);
	    double LW_D4 = SQUtils.round(LW / 4, digits);
	    double BL90_M2 = SQUtils.round(2 * BL90, digits);
	    double CL_MPV = SQUtils.round(CandleLength * tickSize, digits);
        
        if(L <= L1 && L < L2 && L < L3)  {
    		if(LW_D2 > UW && LW > BL90_M2 && CL >= CL_MPV && O != C && LW_D3 <= UW && LW_D4 <= UW)  {
	    	  	return true;
	      	}
	      	if(LW_D3 > UW && LW > BL90_M2 && CL >= CL_MPV && O != C && LW_D4 <= UW)  {
	      		return true;
	      	}
	      	if(LW_D4 > UW && LW > BL90_M2 && CL >= CL_MPV && O != C)  {
	    	  	return true;
	      	}
	    }
        
        return false;
	}

}
