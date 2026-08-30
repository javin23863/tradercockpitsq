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
package SQ.Columns.Databanks;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SampleTypes;
import com.strategyquant.tradinglib.SettingsKeys;
import com.strategyquant.tradinglib.ValueTypes;
import com.strategyquant.tradinglib.WalkForwardPeriod;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.optimization.WalkForwardMatrixResult;
import com.strategyquant.tradinglib.results.SpecialValues;

/**
 * AvgParametersStability is computed in WalkForwardParametersStability class.
 * It measures parameters differences compared to optimization range.
 * 
 * @author Tomas Brynda
 *
 */
public class AvgParametersStability extends DatabankColumn {
	public static final Logger Log = LoggerFactory.getLogger("AvgParametersStability");
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public AvgParametersStability() {
		super(L.tsq("Avg. Parameters Stability"), DatabankColumn.Decimal2, ValueTypes.Maximize, 0, 0, 1);

		setTooltip(L.tsq("Avg. Parameters Stability - how stable are parameters in Walk Forward results (averaged for all WF optimiations in WF Matrix)"));
		
		// restrict stability computation only for money PL Type (we'll not compute separate stability for % or pips results)
		setPLTypeRestrictions(PlTypes.Money);
		
		// restrict computation only for Both direction, we'll not compute it for Long only or Short only results
		setDirectionRestrictions(Directions.Both); 		
	}

	//------------------------------------------------------------------------

	@Override
	public String getValue(ResultsGroup results, String resultKey, byte direction, byte plType, byte sampleType) throws Exception {
		SettingsMap map = results.specialValues();
		
		if(map.containsKey(SpecialValues.AvgParametersStability)) {	
			return "" + map.getDouble(SpecialValues.AvgParametersStability);
		}
/*		
		WalkForwardMatrixResult mwfResult = (WalkForwardMatrixResult) results.mainResult().get(SettingsKeys.WalkForwardResult);
		if(mwfResult != null) {
			ArrayList<WalkForwardResult> wfResults = mwfResult.getResultList();
			
			for(int i=0; i<wfResults.size(); i++) {
				WalkForwardResult wfResult = wfResults.get(i);
				
				String wfKey = wfResult.getResultKeyName();
				
				// get standard, stability, score stats WFstats
				double wfNetProfit = wfResult.stats.getDouble("NetProfit");
				double wfStabilityNetProfit = wfResult.statsStability.getDouble("NetProfit");
				double wfScoreNetProfit = wfResult.statsScore.getDouble("NetProfit");

				OrdersList wfRunOrders = results.orders().filter(wfKey, Directions.Both, SampleTypes.FullSample);
				
				ArrayList<WalkForwardPeriod> wfPeriods = wfResult.wfPeriods;
				for(int j=0; j<wfPeriods.size(); j++) {
					WalkForwardPeriod wfPeriod = wfPeriods.get(j);
					
					long from = wfPeriod.optimizeFrom;
					long to = wfPeriod.optimizeFrom;
					String testPrams = wfPeriod.testParameters;
					
					
				}
			}
		}
*/			
		
		return "N/A";
	}

}