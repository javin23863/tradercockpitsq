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

import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ClassConfig(name="Randomize OHLC history data", display="Randomize OHLC history data, max price change #MaxChange# % of ATR(#ATRPeriod#) and probabilities (O,H,L,C): #ProbabilityOpen#, #ProbabilityHigh#, #ProbabilityLow#, #ProbabilityClose#")
@Help("<b>Note!</b>This option is supposed to work best with Selected Timeframe precision.")
public class RandomizeHistoryDataOHLC extends MonteCarloRetest {
	public static final Logger Log = LoggerFactory.getLogger(RandomizeHistoryDataOHLC.class);

	@Parameter(name="% Probability, Open", defaultValue="100", minValue=0, maxValue=100, step=1)
	@Help("Probability of changing the Open price")
	public int ProbabilityOpen;

	@Parameter(name="% Probability, High", defaultValue="100", minValue=0, maxValue=100, step=1)
	@Help("Probability of changing the High price")
	public int ProbabilityHigh;

	@Parameter(name="% Probability, Low", defaultValue="100", minValue=0, maxValue=100, step=1)
	@Help("Probability of changing the Low price")
	public int ProbabilityLow;

	@Parameter(name="% Probability, Close", defaultValue="100", minValue=0, maxValue=100, step=1)
	@Help("Probability of changing the Close price")
	public int ProbabilityClose;

	@Parameter(name="Max Change in % of ATR", defaultValue="10", minValue=1, maxValue=100, step=1)
	@Help("Maximum up change in % of ATR")
	public int MaxChange;

	@Parameter(name="ATR Period", defaultValue="14", minValue=3, maxValue=200, step=1)
	@Help("Maximum up change in % of ATR")
	public int ATRPeriod;

	@Parameter(name="Keep connected", defaultValue="true")
	@Help("Preserve original gaps between previous Close and new Open bars")
	public boolean KeepConnected;

	@Parameter(name="% Probability, Close", defaultValue="100", minValue=0, maxValue=100, step=1)
	@Help("Probability of changing the gap - used only if KeepConnected=true")
	public int ProbabilityGapChange;

	@Parameter(name="Max Change of gap in %", defaultValue="10", minValue=1, maxValue=100, step=1)
	@Help("Maximum change of gap in %. Used only if KeepConnected=true")
	public int MaxChangeOfGap;

	private double lastOriginalOpen = 0;
	private double lastOriginalClose = 0;
	private double lastClose = 0;
	private double lastGap = 0;
	private int CurrentBar = 0;
	private double atrValue = 0;

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public RandomizeHistoryDataOHLC() {
		super(MonteCarloTestTypes.ModifyData);
	}

	//------------------------------------------------------------------------

	@Override
	public void modifyOHLCData(IRandomGenerator rng, VersatileData data, double globalATR) {
		if(data.type == VersatileData.TICK_DATA) {
			// this method works only with OHLC data
			return;
		}

		// compute ATR & change for this bar
		double atr = computeATR(data);
		double change = atr * ((double) rng.nextInt(MaxChange))/ 100.0d;

		double plusMinus = 0;
		double gapChange = 0;

		// compute original gap size between previous close and this open
		lastOriginalOpen = data.open;
		if(lastOriginalClose != 0) {
			lastGap = lastOriginalOpen - lastOriginalClose;
		}
		lastOriginalClose = data.close;

		// change OHLC values
		// open
		if(shouldChange(rng, ProbabilityOpen)) {

			if(KeepConnected && lastClose != 0) {
				// special handling, we have to keep the original gap between previous close and this open
				if(shouldChange(rng, ProbabilityGapChange)) {

					plusMinus = (rng.nextInt(1) == 0 ? 1d : -1d);

					gapChange = plusMinus * (lastGap * ((double) rng.nextInt(MaxChangeOfGap)) / 100.0d);

					data.open = lastClose + lastGap + gapChange;

				} else {
					// don't change gap, useas same as original
					data.open = lastClose + lastGap;
				}

			} else {
				// open is changed without consideration of original previous candle or gap
				plusMinus = (rng.nextInt(1) == 0 ? 1d : -1d);

				gapChange = plusMinus * change;

				data.open = data.open + gapChange;
			}
		}

		// high
		if(shouldChange(rng, ProbabilityHigh)) {
			plusMinus = (rng.nextInt(1) == 0 ? 1d : -1d);

			data.high = data.high + gapChange + (plusMinus * change);
		}

		// low
		if(shouldChange(rng, ProbabilityLow)) {
			plusMinus = (rng.nextInt(1) == 0 ? 1d : -1d);

			data.low = data.low + gapChange + (plusMinus * change);
		}

		// close
		if(shouldChange(rng, ProbabilityClose)) {
			plusMinus = (rng.nextInt(1) == 0 ? 1d : -1d);

			data.close = data.close + gapChange + (plusMinus * change);
		}


		// fix H & L values - when values are changed randomly it could hppen that H and L values no longer match, fix it here.
		if(data.high < data.open) data.high = data.open;
		if(data.high < data.close) data.high = data.close;
		if(data.low > data.open) data.low = data.open;
		if(data.low > data.close) data.low = data.close;

		lastClose = data.close;
	}

	//------------------------------------------------------------------------
	private boolean shouldChange(IRandomGenerator rng, int probability) {
		if(probability<1) return false;

		double dblProbability = (double) probability / 100.0d;

		return rng.probability(dblProbability);
	}

	//------------------------------------------------------------------------
	private double computeATR(VersatileData data) {
		// standard dynamic ATR computation, as same as in ATR indicator snippet
		double curHigh = data.high;
		double curLow = data.low;
		double trueRange = curHigh - curLow;

		if (CurrentBar == 0){
			atrValue = trueRange;
		}
		else {
			double prevClose = lastClose;
			trueRange = Math.max(Math.abs(curLow - prevClose), Math.max(trueRange, Math.abs(curHigh - prevClose)));

			atrValue = ((Math.min(CurrentBar + 1, ATRPeriod) - 1 ) * atrValue + trueRange) / Math.min(CurrentBar + 1, ATRPeriod);
		}

		CurrentBar++;

		return atrValue;
	}

	//------------------------------------------------------------------------

	@Override
	public void initSettings(SettingsMap settings) {
		super.initSettings(settings);
	}

	//------------------------------------------------------------------------

	/**
	 * Clones this MC retest object
	 *
	 * @return the clone
	 * @throws Exception the exception
	 */
	@Override
	public RandomizeHistoryDataOHLC getClone() throws Exception {
		RandomizeHistoryDataOHLC mc = new RandomizeHistoryDataOHLC();
		mc.ProbabilityOpen = this.ProbabilityOpen;
		mc.ProbabilityHigh = this.ProbabilityHigh;
		mc.ProbabilityLow = this.ProbabilityLow;
		mc.ProbabilityClose = this.ProbabilityClose;
		mc.MaxChange = this.MaxChange;
		mc.ATRPeriod = this.ATRPeriod;
		mc.KeepConnected = this.KeepConnected;
		mc.ProbabilityGapChange = this.ProbabilityGapChange;
		mc.MaxChangeOfGap = this.MaxChangeOfGap;

		mc.setParams(this.getParams());

		return mc;
	}
}