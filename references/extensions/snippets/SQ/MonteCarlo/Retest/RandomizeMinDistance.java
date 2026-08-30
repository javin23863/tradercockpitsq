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

@ClassConfig(name="Randomize min distance", display="Randomize min distance from price from #Min# to #Max#")
public class RandomizeMinDistance extends MonteCarloRetest {
	public static final Logger Log = LoggerFactory.getLogger(RandomizeMinDistance.class);
	
	@Parameter(name="Min", defaultValue="0", minValue=0, maxValue=100, step=0.1)
	public double Min;
	
	@Parameter(name="Max", defaultValue="10", minValue=0, maxValue=100, step=0.1)
	public double Max;
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public RandomizeMinDistance() {
		super(MonteCarloTestTypes.ModifySettings);
	}
	
	//------------------------------------------------------------------------

	public void modifySettings(IRandomGenerator rng, SettingsMap settings) throws Exception {
		if(Min < 0) Min = 0;
		
		int steps = (int) ((Max-Min)/0.1);
		
		double value = Min + rng.nextInt(steps) * 0.1;
			
		settings.set(SettingsKeys.MinimumDistance, value);
	}	
	
	/**
	 * Gets the clone.
	 *
	 * @return the clone
	 * @throws Exception the exception
	 */
	@Override
	public RandomizeMinDistance getClone() throws Exception {
		RandomizeMinDistance mc = new RandomizeMinDistance();
		mc.Min = this.Min;
		mc.Max = this.Max;
		mc.setParams(this.getParams());
		
		return mc;
	}
}