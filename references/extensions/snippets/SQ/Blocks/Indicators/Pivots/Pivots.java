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
package SQ.Blocks.Indicators.Pivots;

import SQ.Internal.IndicatorBlock;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

@BuildingBlock(name="(Pivots) Pivots", display="Pivots(@Chart@#StartHour#:#StartMinute#).#Line#[#Shift#]", returnType = ReturnTypes.Price)
@Help("Pivots")
@ParameterSet(set="StartHour=0,StartMinute=0")
public class Pivots extends IndicatorBlock {

	@Parameter
	public ChartData Chart;
	
	@Parameter(defaultValue="0", minValue=0, maxValue=23, step=1)
	public int StartHour;
	
	@Parameter(defaultValue="0", minValue=0, maxValue=59, step=1)
	public int StartMinute;
	
	@Output(name = "PP", color = Colors.Red)
	public DataSeries PP;
	
	@Output(name = "R1", color = Colors.Red)
	public DataSeries R1;
	
	@Output(name = "R2", color = Colors.Red)
	public DataSeries R2;
	
	@Output(name = "R3", color = Colors.Red)
	public DataSeries R3;
	
	@Output(name = "S1", color = Colors.Red)
	public DataSeries S1;
	
	@Output(name = "S2", color = Colors.Red)
	public DataSeries S2;
	
	@Output(name = "S3", color = Colors.Red)
	public DataSeries S3;
	
	private int Period;
	private int StartMinutesIntoDay;
	private int CloseMinutesIntoDay;
	
	private int PrevClosingBar = 1;
	private long PrevClosingTime = 0;
	
	private double PrevHigh = 0;
	private double PrevLow = 0;
	private double PrevClose = 0;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	@Override
	protected void OnBarUpdate() throws TradingException {
		if(getCurrentBar() == 0) {
			if(StartHour < 0 || StartHour > 23) {
				StartHour = 0;
			}
			if(StartMinute < 0 || StartMinute > 59) {
				StartMinute = 0;
			}
			
			Period = (int)(TimeframeManager.getMillis(Chart.Timeframe) / 60000);
			
			if(Period == 0) {
				// this can happen for Intraday chart
				return;
			}

			StartMinutesIntoDay = correctStartMinutes((StartHour * 60) + StartMinute); // 8' o'clock x 60 = 480
			CloseMinutesIntoDay = StartMinutesIntoDay - Period; 
			CloseMinutesIntoDay = CloseMinutesIntoDay < 0 ? CloseMinutesIntoDay + 1440 : CloseMinutesIntoDay;
			return;
		}

		if(Period == 0) {
			// this can happen for Intraday chart
			return;
		}
		
		PrevClosingBar = findLastTimeMatch(CloseMinutesIntoDay, 1, PrevClosingBar, true);
		
		if(PrevClosingTime != Chart.Time(PrevClosingBar)) {
	    	PrevClosingTime = Chart.Time(PrevClosingBar);
	    	
		    int PrevOpeningBar = findLastTimeMatch(StartMinutesIntoDay, PrevClosingBar + 1, 1000000, false);
	    	
		    PrevHigh = Chart.High(PrevClosingBar);
		    PrevLow = Chart.Low(PrevClosingBar);
		    PrevClose = Chart.Close(PrevClosingBar);
		      
		    for(int SearchHighLow = PrevClosingBar; SearchHighLow < (PrevOpeningBar + 1); SearchHighLow++){
		        if(Chart.High(SearchHighLow) > PrevHigh) PrevHigh = Chart.High(SearchHighLow);
		        if(Chart.Low(SearchHighLow) < PrevLow) PrevLow = Chart.Low(SearchHighLow);
		    }
	    }
	    
	    double pp = (PrevHigh + PrevLow + PrevClose) / 3;
	    
        PP.set(0, pp);
		R1.set(0, (2 * pp) - PrevLow);
		R2.set(0, pp + (PrevHigh - PrevLow));
		R3.set(0, pp + 2 * (PrevHigh - PrevLow));
		S1.set(0, (2 * pp) - PrevHigh);
		S2.set(0, pp - (PrevHigh - PrevLow));
		S3.set(0, pp - 2 * (PrevHigh - PrevLow));
	}


	//------------------------------------------------------------------------
	
	private int findLastTimeMatch(int TimeToLookFor, int StartingBar, int prevBarFound, boolean isClosingBar) throws TradingException{
		int HowManyBarsBack = Math.min(CurrentBar - 1, 1440 / Period * 3);
		
		if(checkBarIsWhatWeLookFor(TimeToLookFor, StartingBar, isClosingBar)) {
			return StartingBar;
		}
		else if(prevBarFound < HowManyBarsBack && checkBarIsWhatWeLookFor(TimeToLookFor, prevBarFound, isClosingBar)) {
			return prevBarFound;
		}
		else if(prevBarFound < HowManyBarsBack && checkBarIsWhatWeLookFor(TimeToLookFor, prevBarFound + 1, isClosingBar)) {
			return prevBarFound + 1;
		}
		else {
			for(int a=StartingBar + 1; a<HowManyBarsBack; a++) {
				if(checkBarIsWhatWeLookFor(TimeToLookFor, a, isClosingBar)) {
					return a;
				}
			}
			
			return HowManyBarsBack + 1;
		}
	}

	//------------------------------------------------------------------------
	
	private boolean checkBarIsWhatWeLookFor(int TimeToLookFor, int index, boolean isClosingBar) throws TradingException {
		int PreviousBarsTime = (SQTime.getHour(Chart.Time(index - 1)) * 60) + SQTime.getMinute(Chart.Time(index - 1));
        int CurrentBarsTime = (SQTime.getHour(Chart.Time(index)) * 60) + SQTime.getMinute(Chart.Time(index));
		int NextBarsTime = (SQTime.getHour(Chart.Time(index + 1)) * 60) + SQTime.getMinute(Chart.Time(index + 1));
        
        if(CurrentBarsTime == TimeToLookFor) return true;  
        
        int PreviousBarDay = SQTime.getDayOfYear(Chart.Time(index - 1));
        int CurrentBarDay = SQTime.getDayOfYear(Chart.Time(index));
        int NextBarDay = SQTime.getDayOfYear(Chart.Time(index + 1));
        
        if(NextBarDay != CurrentBarDay) {
        	NextBarsTime = NextBarsTime - 1440;
        }
        
        if(PreviousBarDay != CurrentBarDay) {
        	if(PreviousBarsTime > TimeToLookFor && CurrentBarsTime > TimeToLookFor) {		//Handling Pivots time outside trading session
        		return true;
        	}
        	PreviousBarsTime = PreviousBarsTime + 1440;
        }
        
        if(PreviousBarsTime > TimeToLookFor && NextBarsTime < TimeToLookFor) {
        	return isClosingBar ? (CurrentBarsTime < TimeToLookFor) : true;
        }
        
        return false;
	}

	//------------------------------------------------------------------------
	
	int correctStartMinutes(int minutes){
	   int temp = minutes;
	   while(temp % Period != 0){
	      temp++;
	   }
	   return temp >= 1440 ? temp - 1440 : temp;
	}  
}
