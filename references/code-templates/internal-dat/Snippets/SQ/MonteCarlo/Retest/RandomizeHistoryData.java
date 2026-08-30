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
package SQ.MonteCarlo.Retest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.datalib.consts.Precisions;
import com.strategyquant.tradinglib.*;

@ClassConfig(name="Randomize history data (by tick)", display="Randomize history data (by tick), with probability #ProbabilityUp# % up / #ProbabilityDown# % down and max price change of ATR #MaxChangeUp# % up / #MaxChangeDown# % down")
@Help("<b>Note!</b>This option is supposed to work best with Selected Timeframe precision.")
public class RandomizeHistoryData extends MonteCarloRetest {
	public static final Logger Log = LoggerFactory.getLogger(RandomizeHistoryData.class);
	
	@Parameter(name="Probability up", defaultValue="20", minValue=1, maxValue=100, step=1)
	@Help("Probability of changing the price up")
	public int ProbabilityUp;

	@Parameter(name="Max up change", defaultValue="10", minValue=1, maxValue=100, step=1)
	@Help("Maximum up change in % of ATR(14)")
	public int MaxChangeUp;

	@Parameter(name="Probability down", defaultValue="20", minValue=1, maxValue=100, step=1)
	@Help("Probability of changing the price down")
	public int ProbabilityDown;
	
	@Parameter(name="Max change down", defaultValue="10", minValue=1, maxValue=100, step=1)
	@Help("Maximum down change in % of ATR(14)")
	public int MaxChangeDown;
	
	@Parameter(name="Keep connected", defaultValue="true")
	@Help("Preserve original gaps between previous Close and new Open bars")
	public boolean KeepConnected;

	private int relativeMaxChangeUp = -1;
	private int relativeMaxChangeDown = -1;
	
	private TickEvent lastTick = new TickEvent();
	private double lastOrigAsk;
	private double lastOrigBid;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public RandomizeHistoryData() {
		super(MonteCarloTestTypes.ModifyData);
	}
	
	//------------------------------------------------------------------------

	@Override
	public void modifyData(IRandomGenerator rng, TickEvent tickEvent, double globalATR) {
		double dblProbability;
		int relativeMaxChange, maxChange;
		
		boolean up = rng.nextInt(2) == 0;
		
		double askOrig = tickEvent.getAsk();
		double bidOrig = tickEvent.getBid();
		
		if(up) {
			dblProbability = ((double) ProbabilityUp / 100.0d);
			relativeMaxChange = relativeMaxChangeUp;
			maxChange = MaxChangeUp;
		} else {
			dblProbability = ((double) ProbabilityDown / 100.0d);
			relativeMaxChange = relativeMaxChangeDown;
			maxChange = MaxChangeDown;
		}
		
		if(rng.probability(dblProbability)) {
			// we should change this price
			double ask = tickEvent.getAsk();
			double bid = tickEvent.getBid();
			double spread = ask - bid;
									
			if(KeepConnected && lastTick.getTime()>0 && lastTick.getTime() != tickEvent.getTime()) {
				tickEvent.setAsk(lastTick.getAsk() + (ask - lastOrigAsk));
				tickEvent.setBid(lastTick.getBid() + (bid - lastOrigBid));
				
			} else {
				int change;
				
				if(relativeMaxChange <= 0) {
					change = rng.nextInt(maxChange);
				} else {
					change = rng.nextInt(relativeMaxChange);
				}
				
				double dblChange = ((double) change)/ 100.0d;

				double priceChange = 2 * globalATR * dblChange;
				
				bid = (up ? bid + priceChange : bid - priceChange);
				ask = bid + spread;
				
				tickEvent.setAsk(ask);
				tickEvent.setBid(bid);	
			}
		}
		
		lastTick.set(tickEvent.getTime(), 0, 0, tickEvent.getBid(), tickEvent.getAsk(), 0, 0, true, true);
		lastOrigAsk = askOrig;
		lastOrigBid = bidOrig;
	}

	//------------------------------------------------------------------------
	
	@Override
	public void initSettings(SettingsMap settings) {
		super.initSettings(settings);
		
		// compute relative Max Change according to chart TF and precision
		ChartSetup chartSetup = (ChartSetup) settings.get(SettingsKeys.BacktestChart);
		if(chartSetup != null) {
			int testPrecision = chartSetup.getTestPrecision();
			
			if(testPrecision == Precisions.SelectedTF) {
				// do nothing for selected TF precision
				relativeMaxChangeUp = MaxChangeUp;
				relativeMaxChangeDown = MaxChangeDown;
				return;
			}
			
			// it is higher precision, set MaxChange to 1/3 of the value
			// Because the change is applied to every minute bar / tick we have to lower the MaxChange 
			// so that the resulting full bar in target timeframe (for example H1) isn't changed too much
			relativeMaxChangeUp = MaxChangeUp / 3;
			if(relativeMaxChangeUp <= 0) {
				relativeMaxChangeUp = 1;
			}
			
			relativeMaxChangeDown = MaxChangeDown / 3;
			if(relativeMaxChangeDown <= 0) {
				relativeMaxChangeDown = 1;
			}
			
		}
	}	

	//------------------------------------------------------------------------
	
	/**
	 * Clones this MC retest object
	 *
	 * @return the clone
	 * @throws Exception the exception
	 */
	@Override
	public RandomizeHistoryData getClone() throws Exception {
		RandomizeHistoryData mc = new RandomizeHistoryData();
		mc.ProbabilityUp = this.ProbabilityUp;
		mc.ProbabilityDown = this.ProbabilityDown;
		mc.MaxChangeUp = this.MaxChangeUp;
		mc.MaxChangeDown = this.MaxChangeDown;
		mc.KeepConnected = this.KeepConnected;
		mc.setParams(this.getParams());
		
		return mc;
	}
}