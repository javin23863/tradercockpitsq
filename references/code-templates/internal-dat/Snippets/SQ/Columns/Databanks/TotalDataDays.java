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

import com.strategyquant.datalib.ChartDef;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.ChartSetup;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.PlTypes;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.SampleTypes;
import com.strategyquant.tradinglib.SettingsKeys;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.ValueTypes;
import com.strategyquant.tradinglib.strategy.OutOfSample;

public class TotalDataDays extends DatabankColumn {
    
	public TotalDataDays() {
		super(L.tsq("Total Data Days"), DatabankColumn.Integer, ValueTypes.Minimize, 0, 10, 1000);
		
		setPLTypeRestrictions(PlTypes.Money); 

		// restrict computation only for Both direction, we'll not compute it for Long only or Short only results
		setDirectionRestrictions(Directions.Both); 		
	}
	
	//------------------------------------------------------------------------

	@Override
	public double compute(SQStats stats, StatsTypeCombination combination, OrdersList ordersList, SettingsMap settings, SQStats statsLong, SQStats statsShort) throws Exception {

		int days = 0;
		int months = 0;
		int years = 0;
		
		ChartSetup chartSetup = (ChartSetup) settings.get(SettingsKeys.BacktestChart);
		if(chartSetup != null) {
			ChartDef mainChart = chartSetup.getMainChart();
			if(mainChart != null) {
				long dateFrom = mainChart.getHistoryFrom();
				long dateTo = mainChart.getHistoryTo();

				OutOfSample oos = (OutOfSample) settings.get(SettingsKeys.OutOfSample);
				days = getDaysBySampleType(oos, combination.getSampleType(), dateFrom, dateTo);
				months = (int) (days / 30.41);
				years = days / 365;

				if(oos != null && combination.getSampleType() != SampleTypes.FullSample) {
				} else {
					// compute from full sample 
					days = SQTime.getDaysBetween(dateFrom, dateTo);
					months = (int) (days / 30.41);
					years = days / 365;
				}
				
				if(days > 0) {
					if(months == 0) months = 1;
					if(years == 0) years = 1;
				}
				
			}
		} else {
			// check if the portfolio values are filled		long dataFirstDate = Long.MAX_VALUE;
			long dateFrom = settings.getLong(SettingsKeys.PortfolioDataStart, Long.MAX_VALUE);
			long dateTo = settings.getLong(SettingsKeys.PortfolioDataEnd, Long.MIN_VALUE);

			if(dateFrom != Long.MAX_VALUE && dateTo != Long.MIN_VALUE) {
				days = SQTime.getDaysBetween(dateFrom, dateTo);
				months = SQTime.getMonthsBetween(dateFrom, dateTo);
				years = SQTime.getYearsBetween(dateFrom, dateTo);
			
				if(days > 0) {
					if(months == 0) months = 1;
					if(years == 0) years = 1;
				}
			}
		}
		
		stats.set("TotalDataMonths", months);
		stats.set("TotalDataYears", years);
		
		return days;
	}

	//------------------------------------------------------------------------
	
	private int getDaysBySampleType(OutOfSample oos, byte sampleType, long dateFrom, long dateTo) {
		if(oos == null || sampleType == SampleTypes.FullSample) {
			return SQTime.getDaysBetween(dateFrom, dateTo);
		}
		
		int days = 0;
		
		for(int i=0; i<oos.getRangesCount(); i++) {
			byte oosSampleType = oos.getSampleType(i);
			
			if(sampleType == SampleTypes.InSampleValidation) {
				if(oosSampleType < SampleTypes.InSampleValidation || oosSampleType > SampleTypes.InSampleValidation10) 
				{
					continue;
				}

			} else if(sampleType == SampleTypes.OutOfSample) {
				if(oosSampleType < 20 || oosSampleType > 30) continue;
				
			} else if(sampleType == SampleTypes.InSample) {
				if(SampleTypes.isISV(oosSampleType) || oosSampleType == SampleTypes.InSampleTraining || oosSampleType == SampleTypes.InSampleValidation || oosSampleType == SampleTypes.InSample) {
					continue;
				}

			} else if(sampleType == SampleTypes.InSampleTraining) {
				// special handling for IST - count all non-IST days
				if(SampleTypes.isISV(oosSampleType) || SampleTypes.isOOS(oosSampleType)) {
					long tempDateFrom = oos.getDateFrom(i);
					long tempDateTo = oos.getDateTo(i);
					
					days += SQTime.getDaysBetween(tempDateFrom, tempDateTo);
					continue;
				}

			} else {
				if(sampleType != oosSampleType) continue;	
			}

			// special handling for IS - count all non-IS days 
			
			long tempDateFrom = oos.getDateFrom(i);
			long tempDateTo = oos.getDateTo(i);
			
			days += SQTime.getDaysBetween(tempDateFrom, tempDateTo);
		}

		if(sampleType == SampleTypes.InSample || sampleType == SampleTypes.InSampleTraining) {
			int totalDays = SQTime.getDaysBetween(dateFrom, dateTo);
			
			return totalDays - days;
			
		} else {
			return days;
		}
	}	

}