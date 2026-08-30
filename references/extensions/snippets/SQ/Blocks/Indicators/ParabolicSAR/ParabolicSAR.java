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
package SQ.Blocks.Indicators.ParabolicSAR;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;
import com.strategyquant.tradinglib.simulator.Engines;

import SQ.Internal.IndicatorBlock;

@BuildingBlock(name="(PSAR) Parabolic SAR", display="ParabolicSAR(@Chart@#Step#, #Maximum#)[#Shift#]", returnType = ReturnTypes.Price)
@ParameterSet(set="Step=0.02,Maximum=0.2")
@ParameterSet(set="Step=0.02,Maximum=0.1")
@ParameterSet(set="Step=0.01,Maximum=0.2")
public class ParabolicSAR extends IndicatorBlock {

	@Parameter
	public ChartData Input;

	@Parameter(defaultValue="0.02", minValue=0.01, maxValue=0.6, step=0.01, builderMinValue=0.01, builderMaxValue=0.4, builderStep=0.001)
	public double Step;
	
	@Parameter(defaultValue="0.2", minValue=0.01, maxValue=1, step=0.1, builderMinValue=0.01, builderMaxValue=1, builderStep=0.01)
	public double Maximum;
	
	@Output
	public DataSeries Value;
	
	@Buffer
	public DataSeries AFBuffer;
	
	@Buffer
	public DataSeries EPBuffer;
	
	private int ExtLastRevPos;
	private boolean ExtDirectionLong;
	
	private double TradeLL = Double.MAX_VALUE, TradeHH = -Double.MAX_VALUE, Af, oParOp, oParCl, prevTradeHH, prevTradeLL;
	
	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		if(Engines.isTradestationEngine(Indicators.Engine)) {
			onBarUpdateTS();
		}
		else {
			onBarUpdateMT();
		}
	}

	//------------------------------------------------------------------------

	private void onBarUpdateTS() throws TradingException {
		if(getCurrentBar() == 0) {
			oParOp = Input.High(0);
			ExtDirectionLong = false;
			TradeHH = Input.High(0);
			TradeLL = Input.Low(0);
		}
		
		double curHigh = Input.High(0);
		double curLow = Input.Low(0);
		double prevHigh = Input.High(1);
		double prevLow = Input.Low(1);
		
		if(curHigh > TradeHH) TradeHH = curHigh;
		if(curLow < TradeLL) TradeLL = curLow;
		
        Value.set(0, oParOp);
        
        boolean transition = false;
        
        if(ExtDirectionLong){
            if(curLow <= oParOp) {
            	ExtDirectionLong = false;
            	transition = false;
              
            	oParCl = TradeHH;
            	TradeHH = curHigh;
            	TradeLL = curLow;
            	Af = Step;
            	oParOp = oParCl + Af * (TradeLL - oParCl);
            	
            	if(oParOp < curHigh) {
            		oParOp = curHigh;
            	}
            	if(oParOp < prevHigh) {
            		oParOp = prevHigh;
            	}
            }
            else {
            	oParCl = oParOp;
            	
            	if(TradeHH > prevTradeHH && Af < Maximum) {
            		Af = Math.min(Af + Step, Maximum);
            	}
            	
            	oParOp = oParCl + Af * (TradeHH - oParCl);
            	
            	if(oParOp > curLow) {
            		oParOp = curLow;
            	}
            	if(oParOp > prevLow) {
            		oParOp = prevLow;
            	}
            }
        }
        else {
        	if(curHigh >= oParOp) {
        		ExtDirectionLong = true;
            	transition = true;
                
            	oParCl = TradeLL;
            	TradeHH = curHigh;
            	TradeLL = curLow;
            	Af = Step ;
            	oParOp = oParCl + Af * (TradeHH - oParCl);
            	
            	if(oParOp > curLow) {
            		oParOp = curLow;
            	}
            	if(oParOp > prevLow) {
            		oParOp = prevLow;
            	}
        	}
        	else {
        		oParCl = oParOp;
            	
            	if(TradeLL < prevTradeLL && Af < Maximum) {
            		Af = Math.min(Af + Step, Maximum);
            	}
            	
            	oParOp = oParCl + Af * (TradeLL - oParCl);
            	
            	if(oParOp < curHigh) {
            		oParOp = curHigh;
            	}
            	if(oParOp < prevHigh) {
            		oParOp = prevHigh;
            	}
        	}
        }
        
        prevTradeHH = TradeHH;
        prevTradeLL = TradeLL;
        
        Value.set(0, oParOp);
	}

	//------------------------------------------------------------------------

	private void onBarUpdateMT() throws TradingException {
		if(CurrentBar < 3) {
			AFBuffer.set(0, 0);
			EPBuffer.set(0, 0);
			Value.set(0, 0);
			return;
		}
		
		double high1 = Input.High(1);
		double high2 = Input.High(2);
		double low1 = Input.Low(1);
		double low2 = Input.Low(2);
		
		double AFBuffer1 = AFBuffer.get(1);
		double AFBuffer2 = AFBuffer.get(2);
		double EPBuffer1 = EPBuffer.get(1);
		double EPBuffer2 = EPBuffer.get(2);
		
		double lastValue = Value.get(1);
		
		if(CurrentBar == 3){
			AFBuffer1 = Step;
			AFBuffer2 = Step;
			EPBuffer1 = low1;
			EPBuffer2 = low1;
			
			AFBuffer.set(2, AFBuffer2);
		    AFBuffer.set(1, AFBuffer1);
		    
		    ExtLastRevPos = 1;
		    ExtDirectionLong = false;

		    Value.set(2, high2);
		    Value.set(1, Math.max(high2, high1));
		    
		    EPBuffer.set(2, EPBuffer2);
		    EPBuffer.set(1, EPBuffer1);
		}
		
		if(ExtDirectionLong){
			if(lastValue > low1){
				EPBuffer1 = low1;
				AFBuffer1 = Step;
				lastValue = getPrevHighest(Input.High, CurrentBar - ExtLastRevPos + 1);
				
				ExtDirectionLong = false;
	            ExtLastRevPos = CurrentBar;
	            
				Value.set(1, lastValue);
	            EPBuffer.set(1, EPBuffer1);
	            AFBuffer.set(1, AFBuffer1);
			}
		}
		else {
			if(lastValue < high1){
				EPBuffer1 = high1;
				AFBuffer1 = Step;
				lastValue = getPrevLowest(Input.Low, CurrentBar - ExtLastRevPos + 1);
						
	            //--- switch to LONG
	            ExtDirectionLong = true;
	            ExtLastRevPos = CurrentBar;
	            
	            Value.set(1, lastValue);
	            EPBuffer.set(1, EPBuffer1);
	            AFBuffer.set(1, AFBuffer1);
	        }
		}
		
		//--- continue calculations
	    if(ExtDirectionLong){
	        //--- check for new High
	        if(high1 > EPBuffer2 && ExtLastRevPos != CurrentBar){
				EPBuffer1 = high1;
				AFBuffer1 = AFBuffer2 + Step;
				AFBuffer1 = AFBuffer1 > Maximum ? Maximum : AFBuffer1;
				
	            EPBuffer.set(1, EPBuffer1);
	            AFBuffer.set(1, AFBuffer1);
	        }
	        else {
	            //--- when we haven't reversed
	            if(ExtLastRevPos != CurrentBar){
					AFBuffer1 = AFBuffer2;
					EPBuffer1 = EPBuffer2;
	            	
	            	AFBuffer.set(1, AFBuffer1);
	            	EPBuffer.set(1, EPBuffer1);
	            }
	        }
	        
	        //--- calculate SAR for tomorrow
	        double sar = lastValue + AFBuffer1 * (EPBuffer1 - lastValue);
	        //--- check for SAR
	        sar = sar > low1 || sar > low2 ? Math.min(low1, low2) : sar;
	        
	        Value.set(0, sar);
	    }
	    else {
	    	//--- check for new Low
	    	if(low1 < EPBuffer2 && ExtLastRevPos != CurrentBar){
				EPBuffer1 = low1;
				AFBuffer1 = AFBuffer2 + Step;
				AFBuffer1 = AFBuffer1 > Maximum ? Maximum : AFBuffer1;
	    	
	    		EPBuffer.set(1, EPBuffer1);
	    		AFBuffer.set(1, AFBuffer1);
	    	}
	    	else {
	    		//--- when we haven't reversed
	    		if(ExtLastRevPos != CurrentBar){
					AFBuffer1 = AFBuffer2;
					EPBuffer1 = EPBuffer2;
	            	
	            	AFBuffer.set(1, AFBuffer1);
	            	EPBuffer.set(1, EPBuffer1);
	    		}
	    	}
	    	
	    	//--- calculate SAR for tomorrow
	    	double sar = lastValue + AFBuffer1 * (EPBuffer1 - lastValue);
	    	//--- check for SAR
	    	sar = sar < high1 || sar < high2 ? Math.max(high1, high2) : sar;
	    	
	    	Value.set(0, sar);
	    }
	}

	//------------------------------------------------------------------------

	private double getPrevHighest(DataSeries input, int period) {
		if(Input.Bars() < period + 1) return 0;
		
		try {
			double highestValue = -Double.MAX_VALUE;

			for(int a=1; a<=period; a++) {
				highestValue = Math.max(input.get(a), highestValue);
			}
			
			return highestValue;
		}
		catch (TradingException e) {
			return 0;
		}
	}

	//------------------------------------------------------------------------

	private double getPrevLowest(DataSeries input, int period) {
		if(Input.Bars() < period + 1) return 0;
		
		try {
			double lowestValue = Double.MAX_VALUE;

			for(int a=1; a<=period; a++) {
				lowestValue = Math.min(input.get(a), lowestValue);
			}
			
			return lowestValue;
		}
		catch (TradingException e) {
			return 0;
		}
	}

}
