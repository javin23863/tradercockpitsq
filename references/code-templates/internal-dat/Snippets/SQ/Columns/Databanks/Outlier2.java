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

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class Outlier2 extends DatabankColumn {
    
	public Outlier2() {
		super("Outlier1", 
          DatabankColumn.Decimal2, // value display format
          ValueTypes.Maximize, // whether value should be maximized / minimized / approximated to a value   
          0, // target value if approximation was chosen  
          0, // average minimum of this value
          100); // average maximum of this value
		
    setWidth(80); // defaultcolumn width in pixels
    
		setTooltip("Outlier filtering Trade with Same Profit Loss");  
	}
	
	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
    
      double firstPL = 0;
	  double secondPL = 0;
	  double thirdPL = 0;
	  int count = 0;
      double results = 0;
	  for(int i2 = 0; i2 < ordersList.size(); i2++){
        Order order = ordersList.get(i2);
		if(order.isBalanceOrder()) {continue; }
			
        double PL = getPLByStatsType(order, combination);

        if(PL > firstPL){
          thirdPL = secondPL;
          secondPL = firstPL;
          firstPL = PL;
        } else if(PL > secondPL && PL != firstPL) {  
          thirdPL = secondPL;
          secondPL = PL;
        } 
        else if(PL > thirdPL && PL != firstPL && PL != secondPL) { 
          thirdPL = PL;
        }
			
		count++;
       }

		if(count > 2 && (secondPL + thirdPL) != 0){
			results = firstPL / (secondPL + thirdPL);
        }
    return round2(results);
	}	
}