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
import com.strategyquant.tradinglib.*;

@ClassConfig(name="Randomize starting bar", display="Randomize starting bar, with max change #MaxChange#")
public class RandomizeStartingBar extends MonteCarloRetest {
	public static final Logger Log = LoggerFactory.getLogger(RandomizeStartingBar.class);

	@Parameter(name="Max change", defaultValue="100", minValue=10, maxValue=5000, step=100)
	public int MaxChange;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public RandomizeStartingBar() {
		super(MonteCarloTestTypes.ModifySettings);
	}
	
	//------------------------------------------------------------------------
	public void modifySettings(IRandomGenerator rng, SettingsMap settings) throws Exception {
		int newVal = rng.nextInt(MaxChange);
		
		settings.set(SettingsKeys.StartingBar, newVal);
	}

	/**
	 * Gets the clone.
	 *
	 * @return the clone
	 * @throws Exception the exception
	 */
	@Override
	public RandomizeStartingBar getClone() throws Exception {
		RandomizeStartingBar mc = new RandomizeStartingBar();
		mc.MaxChange = this.MaxChange;
		mc.setParams(this.getParams());
		
		return mc;
	}
}