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
package SQ.Stats.Overview;

import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.jdom2.Element;

import com.strategyquant.lib.*;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.equitychart.EquityChartUtils;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;

import it.unimi.dsi.fastutil.longs.Long2FloatMap;
import it.unimi.dsi.fastutil.longs.Long2FloatRBTreeMap;

public class SQDefaultPct extends OverviewTemplate {
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public SQDefaultPct() {
		setName(L.tsq("SQ Default (% Monthly Performance)"));
		setScreenshotName("sqdefault_screenshot.jpg");
		setHtmlTemplateName("sqdefault_template.htm");
		
		try {
			loadTemplate();
		} catch (Exception e) {
			Log.error("Overview template couldn't be loaded. Exc. ", e);
		}
	}
	
	//------------------------------------------------------------------------

	@Override
	public String drawValues(ResultsGroup strategyResult, String resultKey, StatsTypeCombination combination) throws Exception {
		reset();
		
		addErrorsTable(strategyResult, resultKey);

		replaceValues(strategyResult, resultKey, combination);
		
		return print();
	}

	protected void addErrorsTable(ResultsGroup strategyResult, String resultKey) throws Exception {
		if(strategyResult == null) {
			return;
		}
		
		int problems = strategyResult.specialValues().getInt(SettingsKeys.StrategyProblems, 0);
		if(problems == 0) {
			return;
		}
		
		// add new style
		String style = "table.problems { width: 100%; background-color: #e0e2e3; color: #C00000; border: 1px solid #C00000;}\n"+
						"table.problems td { padding: 5px 5px 5px 30px; text-align: left; background-color: white; height: 15px; font-size: 11px;}\n"+
						"table.problems th { padding: 5px 5px 5px 10px; text-align: left; background-color: white; color: #606060; font-weight: normal; font-size: 11px;}\n"+
						"table.problems td.name {white-space: nowrap;}\n"+
						".problems h1 {color: #C00000; border-bottom: 0px;}";
		
		template = template.replace("</style>", style+"\n</style>");

		StringBuilder sProblems = new StringBuilder("<div class=\"problems\">");
		sProblems.append("<h1>"+L.t("Strategy Problems")+"</h1>");
		sProblems.append("<table class=\"problems\" cellspacing=\"1\" cellpadding=\"0\" border=\"0\">");
		sProblems.append("<tr><th>"+L.t("SQ identified some problems when testing the strategy - strategy is most probably not suitable for trading on real account !")+"<br/>"
				+ L.t("These problems affect either strategy logic or accuracy of backtesting.")+"</th></tr>");

		sProblems.append(printProblems(problems, strategyResult));
		sProblems.append("</table>");
		sProblems.append("</div>");
		sProblems.append("<br/><br/><br/>\n\n<div id=\"summaryBox\">");

		template = template.replace("<div id=\"summaryBox\">", sProblems.toString());
	}

	//------------------------------------------------------------------------

	private String printProblems(int problems, ResultsGroup strategyResult) {
		StringBuilder tableBody = new StringBuilder("");
		
		printProblem(BadStrategyException.ReasonNoTrades, problems, tableBody);
		printProblem(BadStrategyException.ReasonTooLittleTrades, problems, tableBody);
		printProblem(BadStrategyException.ReasonZeroPLTrades, problems, tableBody);
		printProblem(BadStrategyException.ReasonTooShortTrades, problems, tableBody);
		printProblem(BadStrategyException.ReasonZeroDurationTrades, problems, tableBody);
		printProblem(BadStrategyException.ReasonTooManyOpenTrades, problems, tableBody);
		printProblem(BadStrategyException.ReasonTooLongTrade, problems, tableBody);
		printProblem(BadStrategyException.ReasonNoFilledTrades, problems, tableBody);
		printProblem(BadStrategyException.ReasonStockpickerShiftChanged, problems, tableBody);
		
		return tableBody.toString();
	}

	//------------------------------------------------------------------------

	private void printProblem(int problemType, int problems, StringBuilder tableBody) {
		if((problems & problemType) != 0) {
			tableBody.append("<tr><td>");
			tableBody.append(BadStrategyException.getExplanation(problemType));
			tableBody.append("</td></tr>");
		}
	}
	
	//------------------------------------------------------------------------

	public void replaceValues(ResultsGroup strategyResult, String resultKey, StatsTypeCombination combination) throws Exception {
		Result result = null;
		SQStats stats = null;
		
		if(strategyResult!=null) {
	       	result = strategyResult.subResult(resultKey);
	   	   	
	       	if(result!=null) {
	   	   		try {
	   	   			stats = result.stats(combination);
	   	   			
	   	   		} catch(StatsDontExistException e) {
	   	   			if(!e.getMessage().contains(L.t("Result doesn't contain stats"))) {
	   	   				Log.debug(e.getMessage());
	   	   			}
	   	   		}
	   	   	}
		}

		//BIG
		String css_class_small = combination.getPLType() == PlTypes.Pips ? " small" : "";
		
       	replace("Big", 
       			L.t("TOTAL PROFIT"), 
       			stats==null ? NA   : d2WithPlType(stats.getDouble(StatsKey.NET_PROFIT), combination.getPLType()), 
       			stats==null ? null : stats.getDouble(StatsKey.NET_PROFIT)>0 ? "positiveNum"+css_class_small : "negativeNum"+css_class_small);	
       	
       	//SMALL1
       	if(combination.getPLType() == PlTypes.Money) {
       		
       		replace("Small1", 
       				L.t("PROFIT IN PIPS"), 
       				stats==null ? NA   : d2WithPlType(stats.getDouble(StatsKey.NET_PROFIT_PIPS), PlTypes.Pips), 
       				stats==null ? null : stats.getDouble(StatsKey.NET_PROFIT_PIPS)>0 ? "positiveNum" : "negativeNum");	
       	} else { 
       		SQStats statsMoney = null;
       		
       		if(result!=null) {
       			statsMoney = result.stats(combination.getDirection(), PlTypes.Money, combination.getSampleType());
       		}
       		
       		replace("Small1", 
       				L.t("PROFIT IN MONEY"), 
       				statsMoney==null ? NA   : d2WithPlType(statsMoney.getDouble(StatsKey.NET_PROFIT), PlTypes.Money), 
       				statsMoney==null ? null : statsMoney.getDouble(StatsKey.NET_PROFIT)>0 ? "positiveNum" : "negativeNum");	
       	}
       	
       	//SMALL2
   		replace("Small2", 
   				L.t("YEARLY AVG PROFIT"), 
   				stats==null ? NA   : d2WithPlType(stats.getDouble(StatsKey.AVG_PROFIT_BY_YEAR), combination.getPLType()), 
   				stats==null ? null : stats.getDouble(StatsKey.AVG_PROFIT_BY_YEAR)>0 ? "positiveNum" : "negativeNum");	
       	
   		//SMALL3
   		
   		replace("Small3", 
   				L.t("YEARLY AVG % RETURN"), 
   				stats==null ? NA   : d2WithPlType(stats.getDouble(StatsKey.AVG_PCT_PROFIT_BY_YEAR), PlTypes.Percent), 
   				stats==null ? null : stats.getDouble(StatsKey.AVG_PCT_PROFIT_BY_YEAR)>0 ? "positiveNum" : "negativeNum");	
       	
   		replace("Small4", 
   				L.t("CAGR"), 
   				stats==null ? NA   : d2WithPlType(stats.getDouble(StatsKey.CAGR), PlTypes.Percent), 
   				stats==null ? null : stats.getDouble(StatsKey.CAGR)>0 ? "positiveNum" : "negativeNum");	

   		replace("1_1", L.t("# OF TRADES"), stats==null ? NA : ""+stats.getInt(StatsKey.NUMBER_OF_TRADES));
       	replace("1_2", L.t("SHARPE RATIO"), stats==null ? NA : d2(stats.getDouble(StatsKey.SHARPE_RATIO)));
       	replace("1_3", L.t("PROFIT FACTOR"), stats==null ? NA : d2(stats.getDouble(StatsKey.PROFIT_FACTOR)));
       	replace("1_5", L.t("WINNING PERCENTAGE"), stats==null ? NA : d2(stats.getDouble(StatsKey.WINNING_PCT))+" %");
       	

		if(strategyResult.isStockpickerStrategy()) {
			replace("1_4", L.t("RETURN / OPEN DD RATIO"), stats==null ? NA : d2(stats.getDouble(StatsKey.RETURN_OPEN_DD_RATIO)));
			
	       	replace("2_1", L.t("OPEN DRAWDOWN"), stats==null ? NA : d2WithPlType(stats.getDouble("OpenDrawdown"), combination.getPLType()));
	       	replace("2_2", L.t("OPEN % DRAWDOWN"), stats==null ? NA : d2WithPlType(stats.getDouble("OpenDrawdownPct"), PlTypes.Percent));
		} else {
	       	replace("1_4", L.t("RETURN / DD RATIO"), stats==null ? NA : d2(stats.getDouble(StatsKey.RETURN_DD_RATIO)));
			
			String dd_key = combination.getPLType() == PlTypes.Percent ? StatsKey.PCT_DRAWDOWN : (combination.getPLType() == PlTypes.Pips ? StatsKey.PIPS_DRAWDOWN : StatsKey.DRAWDOWN);
	       	replace("2_1", L.t("DRAWDOWN"), stats==null ? NA : d2WithPlType(stats.getDouble(dd_key), combination.getPLType()));
	       	replace("2_2", L.t("% DRAWDOWN"), stats==null ? NA : d2WithPlType(stats.getDouble(StatsKey.PCT_DRAWDOWN), PlTypes.Percent));
		}

       	replace("2_3", L.t("DAILY AVG PROFIT"), stats==null ? NA : d2WithPlType(stats.getDouble(StatsKey.AVG_PROFIT_BY_DAY), combination.getPLType()));
       	replace("2_4", L.t("MONTHLY AVG PROFIT"), stats==null ? NA : d2WithPlType(stats.getDouble(StatsKey.AVG_PROFIT_BY_MONTH), combination.getPLType()));
       	replace("2_5", L.t("AVERAGE TRADE"), stats==null ? NA : d2WithPlType(stats.getDouble(StatsKey.AVG_TRADE), combination.getPLType()));
       	
       	replace("3_1", L.t("ANNUAL % / Max DD %"), stats==null ? NA : d2(stats.getDouble(StatsKey.AAR_DD_RATIO)));
    	replace("3_2", L.t("R EXPECTANCY"), stats==null ? NA : d2(stats.getDouble(StatsKey.R_EXPECTANCY)));
    	replace("3_3", L.t("R EXPECTANCY SCORE"), stats==null ? NA : d2(stats.getDouble(StatsKey.R_EXPECTANCY_SCORE)));
    	replace("3_4", L.t("STR QUALITY NUMBER"), stats==null ? NA : d2(stats.getDouble(StatsKey.SQN)));
    	replace("3_5", L.t("SQN SCORE"), stats==null ? NA : d2(stats.getDouble(StatsKey.SQN_SCORE)));
    	
    	//Strategy
    	replace("S1_1", L.t("Wins / Losses Ratio"), stats==null ? NA : d2(stats.getDouble(StatsKey.WIN_LOSS_RATIO)));
    	replace("S1_2", L.t("Payout Ratio (Avg Win/Loss)"), stats==null ? NA : d2(stats.getDouble(StatsKey.PAYOUT_RATIO)));
    	replace("S1_3", L.t("Average # of Bars in Trade"), stats==null ? NA : d2(stats.getDouble(StatsKey.AVG_BARS_TRADE)));
    	
    	replace("S2_1", L.t("AHPR"), stats==null ? NA : d2(stats.getDouble(StatsKey.AHPR)));
    	replace("S2_2", L.t("Z-Score"), stats==null ? NA : d2(stats.getDouble(StatsKey.Z_SCORE)));
    	replace("S2_3", L.t("Z-Probability"), stats==null ? NA : d2(stats.getDouble(StatsKey.Z_PROBABILITY))+" %");
    	
    	replace("S3_1", L.t("Expectancy"), stats==null ? NA : d2(stats.getDouble(StatsKey.EXPECTANCY)));
    	replace("S3_2", L.t("Deviation"), stats==null ? NA : d2WithPlType(stats.getDouble(StatsKey.STANDARD_DEV), combination.getPLType()));
    	replace("S3_3", L.t("Exposure"), stats==null ? NA : d2(stats.getDouble(StatsKey.EXPOSURE, 0))+" %");
    	
    	replace("S4_1", L.t("Stagnation in Days"), stats==null ? NA : ""+stats.getInt(StatsKey.STAGNATION_PERIOD));
    	replace("S4_2", L.t("Stagnation in %"), stats==null ? NA : d2WithPlType(stats.getDouble(StatsKey.STAGNATION_PERIOD_PCT), PlTypes.Percent));
    	replace("S4_3", "", "");

    	//Trades
    	replace("T1_1", "", "");
    	replace("T1_2", L.t("# of Wins"), stats==null ? NA : ""+stats.getInt(StatsKey.NUMBER_OF_PROFITS));
    	replace("T1_3", L.t("# of Losses"), stats==null ? NA : ""+stats.getInt(StatsKey.NUMBER_OF_LOSSES));
    	replace("T1_4", L.t("# of Cancelled/Expired"), stats==null ? NA : ""+stats.getInt(StatsKey.NUMBER_OF_CANCELED));
    	
    	replace("T2_1", L.t("Gross Profit"), stats==null ? NA : d2WithPlType(stats.getDouble(StatsKey.GROSS_PROFIT), combination.getPLType()));
    	replace("T2_2", L.t("Gross Loss"), stats==null ? NA : d2WithPlType(stats.getDouble(StatsKey.GROSS_LOSS), combination.getPLType()));
    	replace("T2_3", L.t("Average Win"), stats==null ? NA : d2WithPlType(stats.getDouble(StatsKey.AVG_WIN), combination.getPLType()));
    	replace("T2_4", L.t("Average Loss"), stats==null ? NA : d2WithPlType(stats.getDouble(StatsKey.AVG_LOSS), combination.getPLType()));
    	
    	replace("T3_1", L.t("Largest Win"), stats==null ? NA : d2WithPlType(stats.getDouble(StatsKey.MAX_PROFIT), combination.getPLType()));
    	replace("T3_2", L.t("Largest Loss"), stats==null ? NA : d2WithPlType(stats.getDouble(StatsKey.MAX_LOSS), combination.getPLType()));
    	replace("T3_3", L.t("Max Consec Wins"), stats==null ? NA : ""+stats.getInt(StatsKey.MAX_CONSEC_WINS));
    	replace("T3_4", L.t("Max Consec Losses"), stats==null ? NA : ""+stats.getInt(StatsKey.MAX_CONSEC_LOSS));
    	
    	replace("T4_1", L.t("Avg Consec Wins"), stats==null ? NA : d2(stats.getDouble(StatsKey.AVG_CONSEC_WIN)));
    	replace("T4_2", L.t("Avg Consec Loss"), stats==null ? NA : d2(stats.getDouble(StatsKey.AVG_CONSEC_LOSS)));
    	replace("T4_3", L.t("Avg # of Bars in Wins"), stats==null ? NA : d2(stats.getDouble(StatsKey.AVG_BARS_WIN)));
    	replace("T4_4", L.t("Avg # of Bars in Losses"), stats==null ? NA : d2(stats.getDouble(StatsKey.AVG_BARS_LOSS)));
    	
    	addMonthlyPerformanceTable(strategyResult, resultKey, combination);
	}
	
	//------------------------------------------------------------------------

	private void addMonthlyPerformanceTable(ResultsGroup strategyResult, String symbol, StatsTypeCombination combination) {
		
		String monhlyPerformanceContent;
		String tableBody;
		
		boolean displayStockpickerPerformance = false;
		
		try {
			displayStockpickerPerformance = strategyResult.isStockpickerStrategy() && strategyResult.mainResult().getMaxDailyDD() != null;
		} catch (Exception e) {}
		
		if(displayStockpickerPerformance) {
			tableBody = printStockpickerTableBody(strategyResult, symbol, combination);
			
			monhlyPerformanceContent = "<div class=\"performance\">"+
					"<h1>"+L.t("Monthly Performance % with Daily Equity")+"</h1>"+
				    "<table class=\"calendar\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\">"+
					"<tr class=\"months\">"+
					"<td>"+L.t("Year")+"</td><td>"+
					L.t("Jan")+"</td><td>"+
					L.t("Feb")+"</td><td>"+
					L.t("Mar")+"</td><td>"+
					L.t("Apr")+"</td><td>"+
					L.t("May")+"</td><td>"+
					L.t("Jun")+"</td><td>"+
					L.t("Jul")+"</td><td>"+
					L.t("Aug")+"</td><td>"+
					L.t("Sep")+"</td><td>"+
					L.t("Oct")+"</td><td>"+
					L.t("Nov")+"</td><td>"+
					L.t("Dec")+"</td><td>"+
					L.t("YTD")+"</td><td>"+
					L.t("Benchmark")+"</td><td>"+
					L.t("Comparison")+"</td><td>"+
					L.t("Max DD")+"</td><td>"+
					L.t("Benchmark Max DD")+"</td>"+
					"</tr>"+
					tableBody+
				    "</table>"+
				    "</div>";
		} else {
			tableBody = printTableBody(strategyResult, symbol, combination);
	
			monhlyPerformanceContent = "<div class=\"performance\">"+
			    "<h1>"+L.t("Monthly Performance")+" (%)</h1>"+
			    "<table class=\"calendar\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\">"+
				"<tr class=\"months\">"+
				"<td>"+L.t("Year")+"</td><td>"+L.t("Jan")+"</td><td>"+L.t("Feb")+"</td><td>"+L.t("Mar")+"</td><td>"+L.t("Apr")+"</td><td>"+L.t("May")+"</td><td>"+L.t("Jun")+"</td><td>"+L.t("Jul")+"</td><td>"+L.t("Aug")+"</td><td>"+L.t("Sep")+"</td><td>"+L.t("Oct")+"</td><td>"+L.t("Nov")+"</td><td>"+L.t("Dec")+"</td><td>"+L.t("YTD")+"</td>"+
				"</tr>"+
				tableBody+
			    "</table>"+
			    "</div>";
		}

		template = template.replace("<!-- end of data -->", monhlyPerformanceContent+"\n<!-- end of data -->");
	}

	//------------------------------------------------------------------------

	private final static double NotTraded = -1111d;
	
	private String printTableBody(ResultsGroup strategyResult, String resultKey, StatsTypeCombination combination) {
		StringBuilder tableBody = new StringBuilder("");
		
		if(strategyResult != null) {
			double initialCapital = 10000;
			
			try {
				Result result = strategyResult.subResult(ResultsGroup.Portfolio);		
				initialCapital = result.getSettings().getDouble("MoneyManagement.InitialCapital", 10000);
			} catch(Exception e) {
				Log.error("Failed to get MoneyManagement.InitialCapital value.", e);
			}
			
			try {
				TreeMap<Integer, Double[]> mpMap = null;
				OrdersList filteredOL = strategyResult.orders().filter(resultKey, combination.getDirection(), combination.getSampleType());
				filteredOL.sort(ComparatorByCloseTime);				
				
				mpMap = computeMonthlyPerformance(initialCapital, filteredOL, combination.getPLType());
				
				String trClass;
				int row=0;
				
				double plMoney, pctChange;
				double plMoneyLastMonth = initialCapital;
				double plMoneyLastYear = initialCapital;
								
		        for(Map.Entry<Integer, Double[]> entry : mpMap.entrySet()) {	        	
		        	String year = entry.getKey().toString();
		        	Double[] months = entry.getValue();
		        	   
		        	if(row%2==0) {
		        		trClass = "oddrow";
		        	} else {
		        		trClass = "evenrow";
		        	}
		        	
		        	StringBuilder rowStr = new StringBuilder("");
		        	
		        	rowStr.append("<tr class=\""+trClass+"\">");
		        	rowStr.append("<td class=\"bold\">"+year+"</td>");
		        	
		        	for(int m=0; m<months.length; m++) {
		        		plMoney = months[m]; //12months + year performance
		        		
		        		if(plMoney == NotTraded) {
		        			rowStr.append("<td>0</td>");
		        		} else {		        			
			        		//percentage change calc
			        		if(m < 12) {
			        			pctChange = plMoneyLastMonth == 0  ? 0 : ((plMoney - plMoneyLastMonth) / Math.abs(plMoneyLastMonth)) * 100;
			        		} else {
			        			pctChange = plMoneyLastYear == 0  ? 0 : ((plMoney - plMoneyLastYear) / Math.abs(plMoneyLastYear)) * 100;
			        		}
			        		
			        		if(plMoney<0) {
			        			//rowStr.append("<td class=\"negativeNum\">"+d2(plMoney)+"/"+d2(pctChange)+"</td>");
			        			rowStr.append("<td class=\"negativeNum\">"+d2(pctChange)+"</td>");
			        		} else {
			        			//rowStr.append("<td>"+d2(plMoney)+"/"+d2(pctChange)+"</td>");
			        			rowStr.append("<td>"+d2(pctChange)+"</td>");
			        		}

			        		if(m < 12) {
			        			plMoneyLastMonth = plMoney;
			        		} else {
			        			plMoneyLastYear = plMoney;
			        		}
		        		}
		        	}
		        	
		        	rowStr.append("</tr>");
		        	
		        	tableBody.insert(0, rowStr);
		        	row++;
		        }
		        
		        return tableBody.toString();
			} catch (Exception e){
				Log.error("Cannot get list of orders. ", e);
			}
		}
		
		return "<tr class=\"oddrow\">"
		+ "<td class=\"bold\">NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "</tr>";
	}

	//------------------------------------------------------------------------

	public static TreeMap<Integer, Double[]> computeMonthlyPerformance(double initialCapital, OrdersList orderList, byte plType) {
		TreeMap<Integer, Double[]> mpMap = new TreeMap<Integer, Double[]>(); //<year, values>
		
		int minYear = -1;
		int maxYear = -1;
		
		int year = -1;
		int month = -1;
		Double[] months = null;
		
		double equity = initialCapital;
				
		for(int i=0; i<orderList.size(); i++) {
			Order order = orderList.get(i);
				
			equity+=order.getPLByType(plType);
			
			year = SQTime.getYear(order.CloseTime)+1900;
			
			if(minYear==-1) minYear = year;
			if(maxYear==-1) maxYear = year;
			
			if(minYear > year) minYear = year;
			if(maxYear < year) maxYear = year;
			
			if(mpMap.containsKey(year)) {
				months = mpMap.get(year);
			} else {
				months = new Double[13]; //12months + year performance
				for(int j=0;j<13;j++) months[j] = NotTraded;
				mpMap.put(year, months);				
			}
			
			month = SQTime.getMonth(order.CloseTime); //0-11
			months[month] = equity;
			
			months[12] = equity;
		}
		
		//fill missing years
		if(minYear>0 && maxYear>0) {
			for(year=minYear; year<maxYear; year++) {
				if(!mpMap.containsKey(year)) {
					months = new Double[13];
					for(int j=0;j<13;j++) months[j]=NotTraded;
					mpMap.put(year, months);
				}
			}
		}
		
		return mpMap;
	}
	
	//------------------------------------------------------------------------

	private static int IndexYTD = 12;
	private static int IndexBenchmark = 13;
	private static int IndexComparison = 14;
	private static int IndexMaxDD = 15;
	private static int IndexBenchmarkMaxDD = 16;
	
	private String printStockpickerTableBody(ResultsGroup strategyResult, String resultKey, StatsTypeCombination combination) {		
		StringBuilder tableBody = new StringBuilder("");
		
		if(strategyResult != null) {
			try {
				TreeMap<Integer, Double[]> mpMap = null;
				mpMap = computeStockpickerMonthlyPerformance(strategyResult, resultKey, combination);
				
				String trClass;
				int row=0;
				
		        for(Map.Entry<Integer, Double[]> entry : mpMap.descendingMap().entrySet()) {	        	
		        	String year = entry.getKey().toString();
		        	Double[] months = entry.getValue();
		        	   
		        	if(row%2==0) {
		        		trClass = "oddrow";
		        	} else {
		        		trClass = "evenrow";
		        	}
		        	
		        	tableBody.append("<tr class=\""+trClass+"\">");
		        	tableBody.append("<td class=\"bold\">"+year+"</td>");
		        	
		        	for(int m=0; m<months.length; m++) {
						if (m == IndexYTD) {
							//YTD with bold
							if(months[m]<0) {
			        			tableBody.append("<td class=\"negativeNum\"><b>"+d2(months[m])+"</b></td>");
			        		} else {
			        			tableBody.append("<td><b>"+d2(months[m])+"</b></td>");
			        		}   
							
						} else if (m==IndexMaxDD || m==IndexBenchmarkMaxDD) {
							tableBody.append("<td>"+d2(months[m])+"</td>");
							
						} else { 
							if(months[m]<0) {
			        			tableBody.append("<td class=\"negativeNum\">"+d2(months[m])+"</td>");
			        		} else {
			        			tableBody.append("<td>"+d2(months[m])+"</td>");
			        		}  	
						}
						     		
		        	}
		        	
		        	tableBody.append("</tr>");
		        	row++;
		        }
		        
		        return tableBody.toString();
			} catch (Exception e){
				Log.error("Cannot get list of orders. ", e);
			}
		}
		
		return "<tr class=\"oddrow\">"
		+ "<td class=\"bold\">NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "<td>NA</td>"
		+ "</tr>";
	}

	//------------------------------------------------------------------------
	
	public static TreeMap<Integer, Double[]> computeStockpickerMonthlyPerformance(ResultsGroup strategyResult, String resultKey, StatsTypeCombination combination) throws Exception {
		Long2FloatRBTreeMap worstDailyEquity = strategyResult.mainResult().getWorstDailyEquity();
		Long2FloatRBTreeMap maxDailyDDPct = strategyResult.mainResult().getMaxDailyDDPct();

		Element elSettings = XMLUtil.stringToElement(strategyResult.getLastSettings());
		Element elRiskMoneyManagement = elSettings.getChild("RiskMoneyManagement");
		MoneyManagementMethod mmMethod = ProjectConfigHelper.getMoneyManagement(elRiskMoneyManagement.getChild("MoneyManagement"));
		double initialCapital = mmMethod.getInitialCapital();

		OrdersList orders = strategyResult.orders().filter(resultKey, combination.getDirection(), combination.getSampleType());
		long[] dateRange = EquityChartUtils.dateRange(orders, strategyResult.isStockpickerStrategy());
		long from = dateRange[0];
		long to = dateRange[1];
		
		// Calculate benchmark for SPY 
		Benchmark benchmark = new Benchmark();
		BenchmarkResult benchmarkResult = benchmark.calculate(strategyResult, from, to);
		TreeMap<Integer, Double[]> mpMap = new TreeMap<Integer, Double[]>(); // <year, values>

		int year = -1;
		int lastYear = -1;
		int month = -1;
		Double[] months = null;
		double monthlyPctPerformance, yearlyPctPerformance, benchmarkYearlyPctPerformance, comparison;
		long date;
		double equity, ddPct;

		double previousMonthEquity = initialCapital;
		double currentMonthEquity = initialCapital;
		double previousYearEquity = initialCapital;
		double currentYearEquity = initialCapital;
		
		double benchmarkEquity = initialCapital;
		double benchmarkPreviousYearEquity = initialCapital;
		double benchmarkCurrentYearEquity = initialCapital;
		double ddPctBenchmark;

		Iterator<Long2FloatMap.Entry> i = worstDailyEquity.long2FloatEntrySet().iterator();
		while (i.hasNext()) {
			Long2FloatMap.Entry item = i.next();
			date = item.getLongKey();
			equity = item.getFloatValue() + initialCapital;

			ddPct = maxDailyDDPct.containsKey(date) ? (double) maxDailyDDPct.get(date) : -1;

			if (benchmarkResult.equity.containsKey(date)) {
				benchmarkEquity = (double) benchmarkResult.equity.get(date) + initialCapital;
			}
			
			ddPctBenchmark = benchmarkResult.ddMapPct.containsKey(date) ? (double) benchmarkResult.ddMapPct.get(date) : -1;

			year = SQTime.getYear(date) + 1900;

			if (mpMap.containsKey(year)) {
				months = mpMap.get(year);
			} else {
				months = new Double[17]; // 12 months + year performance + benchmark + comparison + max DD + benchmark
				for (int j = 0; j < 17; j++)
					months[j] = 0.0;
				mpMap.put(year, months);
			}

			int currentMonth = SQTime.getMonth(date); // 0-11

			if (currentMonth != month) {
				if (month != -1) {
					monthlyPctPerformance = (currentMonthEquity - previousMonthEquity) / previousMonthEquity * 100;
					mpMap.get(lastYear)[month] = monthlyPctPerformance;
					
					yearlyPctPerformance = (currentYearEquity - previousYearEquity) / previousYearEquity * 100;
					mpMap.get(lastYear)[IndexYTD] = yearlyPctPerformance;

					benchmarkYearlyPctPerformance = (benchmarkCurrentYearEquity - benchmarkPreviousYearEquity) / benchmarkPreviousYearEquity * 100;
					mpMap.get(lastYear)[IndexBenchmark] = benchmarkYearlyPctPerformance;

					comparison = mpMap.get(lastYear)[IndexYTD] - mpMap.get(lastYear)[IndexBenchmark];
					mpMap.get(lastYear)[IndexComparison] = comparison;

					previousMonthEquity = currentMonthEquity;
				}
				
				if (year != lastYear) {
					previousYearEquity = currentYearEquity;
					benchmarkPreviousYearEquity = benchmarkCurrentYearEquity;
				}

				lastYear = year;
				month = currentMonth;
			}
			
			currentMonthEquity = equity;
			currentYearEquity = equity;
			benchmarkCurrentYearEquity = benchmarkEquity;

			if (ddPct != -1 && (mpMap.get(year)[IndexMaxDD] == 0 || mpMap.get(year)[IndexMaxDD] > ddPct)) {
				mpMap.get(year)[IndexMaxDD] = ddPct; // Update max DD for the year
			}

			if (ddPctBenchmark != -1 && (mpMap.get(year)[IndexBenchmarkMaxDD] == 0 || mpMap.get(year)[IndexBenchmarkMaxDD] > ddPctBenchmark)) {
				mpMap.get(year)[IndexBenchmarkMaxDD] = ddPctBenchmark; // Update max DD for the benchmark
			}
		}

		// Handle the last month
		if (month != -1) {
			monthlyPctPerformance = (currentMonthEquity - previousMonthEquity) / previousMonthEquity * 100;
			months[month] = monthlyPctPerformance;
			months[IndexYTD] += monthlyPctPerformance;

			yearlyPctPerformance = (currentYearEquity - previousYearEquity) / previousYearEquity * 100;
			mpMap.get(lastYear)[IndexYTD] = yearlyPctPerformance;

			benchmarkYearlyPctPerformance = (benchmarkCurrentYearEquity - benchmarkPreviousYearEquity) / benchmarkPreviousYearEquity * 100;
			mpMap.get(lastYear)[IndexBenchmark] = benchmarkYearlyPctPerformance;

			comparison = mpMap.get(lastYear)[IndexYTD] - mpMap.get(lastYear)[IndexBenchmark];
			mpMap.get(lastYear)[IndexComparison] = comparison;
		}

		return mpMap;
	}

}
