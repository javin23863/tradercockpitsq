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


import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.ValueTypes;

import SQ.Functions.StatFunctions;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;

public class AHPR extends DatabankColumn {

	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public AHPR() {
		super(L.tsq("AHPR"), DatabankColumn.Decimal2, ValueTypes.Minimize, 0, 0, 40);
		
		setTooltip(L.tsq("Arithmetic Holding Period Return"));
	}

	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {
		TempData tempData = init(ordersList);
		
		for(int i=0; i<ordersList.size(); i++) {
			Order o = ordersList.get(i);
			
			int year = SQTime.getYear(o.CloseTime);

			if(tempData.yearlyReturn.containsKey(year)) {
				double yearPL = tempData.yearlyReturn.get(year);
				yearPL += o.PctPL;

				tempData.yearlyReturn.put(year, yearPL);
			}
		}
		
		DoubleArrayList arrYearlyReturn = new DoubleArrayList();
		IntSet keySet = tempData.yearlyReturn.keySet();
		IntIterator iterator = keySet.iterator();
		
		while(iterator.hasNext()) {
			int year = iterator.nextInt();
			
			arrYearlyReturn.add( (double) tempData.yearlyReturn.get(year) );
		}
		
		double AHPR = round2(StatFunctions.computeAverage(arrYearlyReturn));
		
		double SHPR = StatFunctions.computeStdev(AHPR, arrYearlyReturn);
		stats.set("SHPR", SHPR);
		
		return AHPR;
	}

	//------------------------------------------------------------------------

	class TempData {
		public Int2DoubleOpenHashMap yearlyReturn;
		public long firstOrderDay = -1;
		public long lastOrderDay = -1;
	}

	//------------------------------------------------------------------------

	private TempData init(OrdersList ordersList) {
		TempData tempData = new TempData();
		
		tempData.yearlyReturn = new Int2DoubleOpenHashMap();
		
		if(!ordersList.isEmpty()) {
			for(int i=0; i<ordersList.size(); i++) {
				Order o = ordersList.get(i);
				
				if(tempData.firstOrderDay == -1 || o.OpenTime < tempData.firstOrderDay) {
					tempData.firstOrderDay = o.OpenTime;
				}
				if(tempData.lastOrderDay == -1 || o.CloseTime > tempData.lastOrderDay) {
					tempData.lastOrderDay = o.CloseTime;
				}
			}
		}	
		
		if(tempData.firstOrderDay != -1 && tempData.lastOrderDay != -1) {
			int firstYear = SQTime.getYear(tempData.firstOrderDay);
			
			int lastYear = SQTime.getYear(tempData.lastOrderDay);

			for(int i=firstYear; i<=lastYear; i++) {
				tempData.yearlyReturn.put(i, 0d);
			}
		}
		
		return tempData;
	}		
}