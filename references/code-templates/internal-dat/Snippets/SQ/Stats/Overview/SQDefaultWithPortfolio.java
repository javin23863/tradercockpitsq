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

import java.util.ArrayList;
import java.util.List;

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;

public class SQDefaultWithPortfolio extends SQDefault {
	
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	//------------------------------------------------------------------------
	
	public SQDefaultWithPortfolio() {
		setName(L.tsq("SQ with Portfolio"));
		setScreenshotName("sqdefault_screenshot_portfolio.jpg");
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
		
		replaceValues(strategyResult, resultKey, combination);
		
		addErrorsTable(strategyResult, resultKey);
		
		return print();
	}

	//------------------------------------------------------------------------

	public void replaceValues(ResultsGroup strategyResult, String resultKey, StatsTypeCombination combination) throws Exception {
		
		addPortfolioTable(strategyResult, resultKey, combination);
		
		super.replaceValues(strategyResult, resultKey, combination);
	}
		
	//------------------------------------------------------------------------

	private void addPortfolioTable(ResultsGroup strategyResult, String resultKey, StatsTypeCombination combination) throws Exception {
		if(strategyResult == null || !strategyResult.isPortfolio()) {
			return;
		}

		StringBuilder portfolioPerformance = new StringBuilder("<div class=\"performance\">");
		portfolioPerformance.append("<h1>"+L.t("Symbols/Strategies in Portfolio")+"</h1>");
		portfolioPerformance.append("<table class=\"portfolio\" cellspacing=\"1\" cellpadding=\"0\" border=\"0\">");
		portfolioPerformance.append("<tr>");
		portfolioPerformance.append("<th>"+L.t("Name")+"</th><th>"+L.t("Net Profit")+"</th><th>"+L.t("Drawdown ($)")+"</th><th>"+L.t("Drawdown (%)")+"</th><th>"+L.t("Trades")+"</th><th>"+L.t("Profit Factor")+"</th><th>"+L.t("Sharpe Ratio")+"</th><th>"+L.t("Return / DD Ratio")+"</th><th>"+L.t("Avg. Trade")+"</th><th>"+L.t("Stagnation (Days)")+"</th>");
		portfolioPerformance.append("</tr>");
		portfolioPerformance.append(printPortfolioListBody(strategyResult, resultKey, combination));
		portfolioPerformance.append("</table>");
		portfolioPerformance.append("</div>");
		portfolioPerformance.append("<br/><br/><br/>\n\n<div id=\"summaryBox\">");

		template = template.replace("<div id=\"summaryBox\">", portfolioPerformance.toString());
	}

	//------------------------------------------------------------------------

	private String printPortfolioListBody(ResultsGroup strategyResult, String resKey, StatsTypeCombination combination) throws Exception {
		StringBuilder tableBody = new StringBuilder("");
		List<String> resultKeys = new ArrayList<String>();
		
		for(String resultKey : strategyResult.getResultKeys()) {
			if(!resultKey.equals(ResultsGroup.Portfolio)) {
				resultKeys.add(resultKey);
			}
		}
		
		byte plType = combination.getPLType();
		byte sampleType = combination.getSampleType();
		
		for(String resultKey : resultKeys) {
			if(resultKey.startsWith(ResultsGroup.WFResult) || resultKey.startsWith(ICrossCheck.CROSSCHECK_PREFIX)) {
				continue;
			}
			
			Result result = strategyResult.subResult(resultKey);
			SQStats stats = result.stats(combination.getDirection(), plType, sampleType);
						
			tableBody.append("<tr>");
			tableBody.append("<td class=\"name\">");
			tableBody.append(limitStrategyName(resultKey));
			tableBody.append("</td><td class=\"name\">");
			tableBody.append(d2WithPlType(stats.getDouble("NetProfit"), plType));
			tableBody.append("</td><td>");
			tableBody.append(d2WithPlType(stats.getDouble("Drawdown"), plType));
			tableBody.append("</td><td>");
			tableBody.append(d2WithPlType(stats.getDouble("DrawdownPct"), PlTypes.Percent));
			tableBody.append("</td><td>");
			tableBody.append(stats.getInt("NumberOfTrades"));
			tableBody.append("</td><td>");
			tableBody.append(d2(stats.getDouble("ProfitFactor")));
			tableBody.append("</td><td>");
			tableBody.append(d2(stats.getDouble("SharpeRatio")));
			tableBody.append("</td><td>");
			tableBody.append(d2(stats.getDouble("ReturnDDRatio")));
			tableBody.append("</td><td class=\"name\">");
			tableBody.append(d2WithPlType(stats.getDouble("AvgTrade"), plType));
			tableBody.append("</td><td>");
			tableBody.append(stats.getInt("Stagnation"));
			tableBody.append("</td></tr>");
		}
		
		// add portfolio result to the end
		Result result = strategyResult.subResult(ResultsGroup.Portfolio);
		SQStats statsInMoney = result.stats(combination.getDirection(), plType, sampleType);

		tableBody.append("<tr>");
		tableBody.append("<td class=\"name\"><strong>"+L.t("Portfolio")+"</strong></td>");
		tableBody.append("<td class=\"name\">");
		tableBody.append(d2WithPlType(statsInMoney.getDouble("NetProfit"), plType));
		tableBody.append("</td><td>");
		tableBody.append(d2WithPlType(statsInMoney.getDouble("Drawdown"), plType));
		tableBody.append("</td><td>");
		tableBody.append(d2WithPlType(statsInMoney.getDouble("DrawdownPct"), PlTypes.Percent));
		tableBody.append("</td><td>");
		tableBody.append(statsInMoney.getInt("NumberOfTrades"));
		tableBody.append("</td><td>");
		tableBody.append(d2(statsInMoney.getDouble("ProfitFactor")));
		tableBody.append("</td><td>");
		tableBody.append(d2(statsInMoney.getDouble("SharpeRatio")));
		tableBody.append("</td><td>");
		tableBody.append(d2(statsInMoney.getDouble("ReturnDDRatio")));
		tableBody.append("</td><td class=\"name\">");
		tableBody.append(d2WithPlType(statsInMoney.getDouble("AvgTrade"), plType));
		tableBody.append("</td><td>");
		tableBody.append(statsInMoney.getInt("Stagnation"));
		tableBody.append("</td></tr>");
		
		return tableBody.toString();
	}

	//------------------------------------------------------------------------

	private String limitStrategyName(String resultKey) {
		if(resultKey.length() > 30) {
			return resultKey.substring(0, 30)+"...";
		}
		
		return resultKey;
	}

}
