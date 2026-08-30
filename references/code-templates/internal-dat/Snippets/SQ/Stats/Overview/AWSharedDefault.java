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

import java.util.Map;
import java.util.TreeMap;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class AWSharedDefault extends OverviewTemplate {
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------

	public AWSharedDefault() {
		setName(L.tsq("AW Shared Default"));
		setScreenshotName("sqdefault_screenshot.jpg");
		setHtmlTemplateName("awshareddefault_template.htm");
		
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
		sProblems.append("<h1>Strategy Problems</h1>");
		sProblems.append("<table class=\"problems\" cellspacing=\"1\" cellpadding=\"0\" border=\"0\">");
		sProblems.append("<tr><th>SQ identified some problems when testing the strategy - strategy is most probably not suitable for trading on real account !<br/>"
				+ "These problems affect either strategy logic or accuracy of backtesting.</th></tr>");

		sProblems.append(printProblems(problems));
		sProblems.append("</table>");
		sProblems.append("</div>");
		sProblems.append("<br/><br/><br/>\n\n<div id=\"summaryBox\">");

		template = template.replace("<div id=\"summaryBox\">", sProblems.toString());
	}

	//------------------------------------------------------------------------

	private String printProblems(int problems) {
		StringBuilder tableBody = new StringBuilder("");
		
		printProblem(BadStrategyException.ReasonNoTrades, problems, tableBody);
		printProblem(BadStrategyException.ReasonTooLittleTrades, problems, tableBody);
		printProblem(BadStrategyException.ReasonZeroPLTrades, problems, tableBody);
		printProblem(BadStrategyException.ReasonTooShortTrades, problems, tableBody);
		printProblem(BadStrategyException.ReasonZeroDurationTrades, problems, tableBody);
		printProblem(BadStrategyException.ReasonTooManyOpenTrades, problems, tableBody);
		printProblem(BadStrategyException.ReasonTooLongTrade, problems, tableBody);
		printProblem(BadStrategyException.ReasonNoFilledTrades, problems, tableBody);
		
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
	   	   			if(!e.getMessage().contains("Result doesn't contain stats")) {
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
       	replace("1_4", L.t("RETURN / DD RATIO"), stats==null ? NA : d2(stats.getDouble(StatsKey.RETURN_DD_RATIO)));
       	replace("1_5", L.t("WINNING PERCENTAGE"), stats==null ? NA : d2(stats.getDouble(StatsKey.WINNING_PCT))+" %");
       	
		String dd_key = combination.getPLType() == PlTypes.Percent ? StatsKey.PCT_DRAWDOWN : (combination.getPLType() == PlTypes.Pips ? StatsKey.PIPS_DRAWDOWN : StatsKey.DRAWDOWN);

       	replace("2_1", L.t("DRAWDOWN"), stats==null ? NA : d2WithPlType(stats.getDouble(dd_key), combination.getPLType()));
       	replace("2_2", L.t("% DRAWDOWN"), stats==null ? NA : d2WithPlType(stats.getDouble(StatsKey.PCT_DRAWDOWN), PlTypes.Percent));
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
		String tableBody = printTableBody(strategyResult, symbol, combination);

		String monhlyPerformanceContent = "<div class=\"performance\">"+
		    "<h1>Monthly Performance ("+PlTypes.print(combination.getPLType())+")</h1>"+
		    "<table class=\"calendar\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\">"+
			"<tr class=\"months\">"+
			"<td>Year</td><td>Jan</td><td>Feb</td><td>Mar</td><td>Apr</td><td>May</td><td>Jun</td><td>Jul</td><td>Aug</td><td>Sep</td><td>Oct</td><td>Nov</td><td>Dec</td><td>YTD</td>"+
			"</tr>"+
			tableBody+
		    "</table>"+
		    "</div>";

		template = template.replace("<!-- end of data -->", monhlyPerformanceContent+"\n<!-- end of data -->");
	}

	//------------------------------------------------------------------------

	private String printTableBody(ResultsGroup strategyResult, String resultKey, StatsTypeCombination combination) {
		StringBuilder tableBody = new StringBuilder("");
		
		if(strategyResult != null) {
			try {
				TreeMap<Integer, Double[]> mpMap = null;
				OrdersList filteredOL = strategyResult.orders().filter(resultKey, combination.getDirection(), combination.getSampleType());

				mpMap = computeMonthlyPerformance(filteredOL, combination.getPLType());
				
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
		        		if(months[m]<0) {
		        			tableBody.append("<td class=\"negativeNum\">"+d2(months[m])+"</td>");
		        		} else {
		        			tableBody.append("<td>"+d2(months[m])+"</td>");
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
		+ "</tr>";
	}

	//------------------------------------------------------------------------

	public static TreeMap<Integer, Double[]> computeMonthlyPerformance(OrdersList orderList, byte plType) {
		TreeMap<Integer, Double[]> mpMap = new TreeMap<Integer, Double[]>(); //<year, values>
		
		int minYear = -1;
		int maxYear = -1;
		
		int year = -1;
		int month = -1;
		Double[] months = null;
		
		for(int i=0; i<orderList.size(); i++) {
			Order order = orderList.get(i);
						
			year = SQTime.getYear(order.CloseTime)+1900;
			
			if(minYear==-1) minYear = year;
			if(maxYear==-1) maxYear = year;
			
			if(minYear > year) minYear = year;
			if(maxYear < year) maxYear = year;
			
			if(mpMap.containsKey(year)) {
				months = mpMap.get(year);
			} else {
				months = new Double[13]; //12months + year performance
				for(int j=0;j<13;j++) months[j]=0.0;
				mpMap.put(year, months);				
			}
			
			month = SQTime.getMonth(order.CloseTime); //0-11
			months[month]+=order.getPLByType(plType);
		}
		
		//compute year(s) performance
		for(Map.Entry<Integer, Double[]> entry : mpMap.entrySet()) {
			months = entry.getValue();
			
			double ytd = 0.0;
			for(int i=0;i<12;i++) ytd+=months[i];
			
			months[12]=ytd;
		}
		
		//fill missing years
		if(minYear>0 && maxYear>0) {
			for(year=minYear; year<maxYear; year++) {
				if(!mpMap.containsKey(year)) {
					months = new Double[13];
					for(int j=0;j<13;j++) months[j]=0.0;
					mpMap.put(year, months);
				}
			}
		}
		
		return mpMap;
	}
}
