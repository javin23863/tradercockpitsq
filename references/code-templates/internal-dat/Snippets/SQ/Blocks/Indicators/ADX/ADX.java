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
package SQ.Blocks.Indicators.ADX;

import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;

import org.jdom2.Element;

import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(ADX) Average Directional Movement Index", display="ADX(@Chart@#Period#, #Line#)[#Shift#]")
@Indicator(min=0, max=100, step=5)
@ParameterSet(set="Period=14,ComputeFrom=0")
@ParameterSet(set="Period=20,ComputeFrom=0")
@ParameterSet(set="Period=30,ComputeFrom=0")
@ParameterSet(set="Period=40,ComputeFrom=0")
public class ADX extends IndicatorBlock {
	
	@Parameter(category="Default", name="Input", defaultValue="0")
	public ChartData Input;
	
	@Parameter(category="Default", name="Period", minValue=2, maxValue=10000, defaultValue="14", step=1)
	public int Period;
	
	@Output(name="Main", color=Colors.Green)
	public DataSeries Main;
	
	@Output(name="+DI", color=Colors.Red)
	public DataSeries DIPlus;

	@Output(name="-DI", color=Colors.Red)
	public DataSeries DIMinus;
	
	@Buffer
	public DataSeries sumTr, sumDmPlus, sumDmMinus;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	@Override
	protected void OnBarUpdate() throws TradingException {
		OnBarUpdateStandard();
	}

	//------------------------------------------------------------------------

	/**
	 * Standard implementation of ADX
	 * @throws TradingException
	 */
	private void OnBarUpdateStandard() throws TradingException {
		double curHigh = Input.High.get(0);
		double curLow = Input.Low.get(0);
		double trueRange = curHigh - curLow;
		
		if(getCurrentBar() == 0){
			sumTr.set(0, trueRange);
			sumDmPlus.set(0, 0);
			sumDmMinus.set(0, 0);
			Main.set(0, 0);
		}
		else {
			double lastHigh = Input.High.get(1);
			double lastLow = Input.Low.get(1);
			double lastClose = Input.Close.get(1);
			double lastSumTr = sumTr.get(1);
			double lastSumDmPlus = sumDmPlus.get(1);
			double lastSumDmMinus = sumDmMinus.get(1);
			
			double deltaHH = SQUtils.round(curHigh - lastHigh, 8);
		    double deltaLL = SQUtils.round(lastLow - curLow, 8);
		    double deltaHC = SQUtils.round(curHigh - lastClose, 8);
		    double deltaLC = SQUtils.round(curLow - lastClose, 8);
			
			double tr = Math.max(Math.abs(deltaLC), Math.max(trueRange, Math.abs(deltaHC)));
			double dmPlus = deltaHH > deltaLL ? Math.max(deltaHH, 0) : 0;
			double dmMinus = deltaLL > deltaHH ? Math.max(deltaLL, 0) : 0;

			double curSumTr = 0;
			double curSumDmPlus = 0;
			double curSumDmMinus = 0;
			
			if (CurrentBar < Period){
				curSumTr = SQUtils.round(lastSumTr + tr, 8);
				curSumDmPlus = lastSumDmPlus + dmPlus;
				curSumDmMinus = lastSumDmMinus + dmMinus;
			}
			else {
				curSumTr = SQUtils.round(lastSumTr - lastSumTr / Period + tr, 8);
				curSumDmPlus = lastSumDmPlus - lastSumDmPlus / Period + dmPlus;
				curSumDmMinus = lastSumDmMinus - lastSumDmMinus / Period + dmMinus;
			}
			
			sumTr.set(0, curSumTr);
			sumDmPlus.set(0, curSumDmPlus);
			sumDmMinus.set(0, curSumDmMinus);
			
			double curDIPlus = 100 * (curSumTr == 0 ? 0 : curSumDmPlus / curSumTr);
			double curDIMinus = 100 * (curSumTr == 0 ? 0 : curSumDmMinus / curSumTr);

			DIPlus.set(0, curDIPlus);
			DIMinus.set(0, curDIMinus);
			
			double diff	= Math.abs(curDIPlus - curDIMinus);
			double sum = SQUtils.round(curDIPlus + curDIMinus, 8);

			Main.set(0, sum == 0 ? 50 : ((Period - 1) * Main.get(1) + 100 * diff / sum) / Period);
		}
	}

	//------------------------------------------------------------------------

	/**
	 * MetaTrader implementation of ADX
	 * @throws TradingException
	 */
	private void OnBarUpdateMT(boolean mt4) throws TradingException {
		double pdm, mdm, tr;
		double price_high, price_low;
		
		if(getCurrentBar() == 0){
			sumDmPlus.set(0, 0);
			sumDmMinus.set(0, 0);
			return;
		}
		
        price_low = Input.Low.get(0);
        price_high = Input.High.get(0);
        
        pdm = price_high - Input.High.get(1);
        mdm = Input.Low.get(1) - price_low;
        
       
        pdm = SQUtils.round(pdm, 8);
        mdm = SQUtils.round(mdm, 8);
        
        if(pdm < 0) pdm=0;  // +DM
        if(mdm < 0) mdm=0;  // -DM
        
        if(pdm == mdm) { pdm = 0; mdm = 0; }
        else if(pdm < mdm) pdm = 0;
        else if(mdm < pdm) mdm = 0;
  
        double num1 = Math.abs(price_high - price_low);				
       	double num2 = Math.abs(price_high - Input.Close.get(1));
        double num3 = Math.abs(price_low - Input.Close.get(1));
        
        tr = Math.max(num1, num2);
        tr = Math.max(tr, num3);
        
        if(tr == 0) { 
        	sumDmPlus.set(0, 0); 
        	sumDmMinus.set(0, 0); 
        }
        else { 
        	sumDmPlus.set(0, 100.0 * pdm / tr); 
        	sumDmMinus.set(0, 100.0 * mdm / tr); 
        }
        
        double pr = 2.0 / (Period + 1.0);
        
		//---- apply EMA to +DI
        //DIPlus.set(0, Indicators.EMA(sumDmPlus, Period).Value.get(0));
        DIPlus.set(0, sumDmPlus.get(0) * pr + DIPlus.get(1) * (1 - pr));
        
		//---- apply EMA to -DI
        //DIMinus.set(0, Indicators.EMA(sumDmMinus, Period).Value.get(0));
        DIMinus.set(0, sumDmMinus.get(0) * pr + DIMinus.get(1) * (1 - pr));
        
		//---- Directional Movement (DX)
		//double div = Math.abs(DIPlus.get(0) + DIMinus.get(0));
        double div = DIPlus.get(0) + DIMinus.get(0);
        
		if(div == 0.00) sumTr.set(0, 0);
		else sumTr.set(0, 100 * (Math.abs(DIPlus.get(0) - DIMinus.get(0)) / div));
		      
		//---- ADX is exponential moving average on DX
		Main.set(0, Indicators.EMA(sumTr, Period).Value.get(0));
	}
	
}
