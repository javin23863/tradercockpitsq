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

import com.strategyquant.lib.*;
import com.strategyquant.datalib.*;
import com.strategyquant.tradinglib.*;

public class TSOverview extends OverviewTemplate {
	
	public TSOverview() {
		setName(L.tsq("TS Overview"));
		setScreenshotName("ts_screenshot.jpg");
		setHtmlTemplateName("ts_template.htm");
		
		try {
			loadTemplate();
		} catch (Exception e) {
			Log.error("Overview template couldn't be loaded. Exc. ", e);
		}
	}
	
	@Override
	public String drawValues(ResultsGroup strategyResult, String resultKey, StatsTypeCombination combination) throws Exception {
		reset();
		
		replaceValues(strategyResult, resultKey, combination);
		
		return print();
	}

	public void replaceValues(ResultsGroup strategyResult, String resultKey, StatsTypeCombination combination) throws Exception {
		SQStats statsAll = null;
		SQStats statsLong = null;
		SQStats statsShort = null;
		
		Result result = null;
		
		if(strategyResult!=null) {
	       	result = strategyResult.subResult(resultKey);
	   	   	
	       	if(result!=null) {
	       		statsAll = result.stats(Directions.Both, combination.getPLType(), SampleTypes.FullSample);
	       		statsLong = result.stats(Directions.Long, combination.getPLType(), SampleTypes.FullSample);
	       		statsShort = result.stats(Directions.Short, combination.getPLType(), SampleTypes.FullSample);
	   	   	}
		}
		
		replaceLabel("1", L.t("Total Net Profit"));
		replaceValue("1_All", statsAll==null ? NA : d2WithPlType(statsAll.getDouble(StatsKey.NET_PROFIT), combination.getPLType()), statsAll==null ? null : statsAll.getDouble(StatsKey.NET_PROFIT)>0 ? "positiveNum" : "negativeNum");
		replaceValue("1_Long", statsLong==null ? NA : d2WithPlType(statsLong.getDouble(StatsKey.NET_PROFIT), combination.getPLType()), statsLong==null ? null : statsLong.getDouble(StatsKey.NET_PROFIT)>0 ? "positiveNum" : "negativeNum");
		replaceValue("1_Short", statsShort==null ? NA : d2WithPlType(statsShort.getDouble(StatsKey.NET_PROFIT), combination.getPLType()), statsShort==null ? null : statsShort.getDouble(StatsKey.NET_PROFIT)>0 ? "positiveNum" : "negativeNum");
		
		replaceLabel("2", L.t("Gross Profit"));
		replaceValue("2_All", statsAll==null ? NA : d2WithPlType(statsAll.getDouble(StatsKey.GROSS_PROFIT), combination.getPLType()), statsAll==null ? null : statsAll.getDouble(StatsKey.GROSS_PROFIT)>0 ? "positiveNum" : "negativeNum");
		replaceValue("2_Long", statsLong==null ? NA : d2WithPlType(statsLong.getDouble(StatsKey.GROSS_PROFIT), combination.getPLType()), statsLong==null ? null : statsLong.getDouble(StatsKey.GROSS_PROFIT)>0 ? "positiveNum" : "negativeNum");
		replaceValue("2_Short", statsShort==null ? NA : d2WithPlType(statsShort.getDouble(StatsKey.GROSS_PROFIT), combination.getPLType()), statsShort==null ? null : statsShort.getDouble(StatsKey.GROSS_PROFIT)>0 ? "positiveNum" : "negativeNum");
		
		replaceLabel("3", L.t("Gross Loss"));
		replaceValue("3_All", statsAll==null ? NA : d2WithPlType(statsAll.getDouble(StatsKey.GROSS_LOSS), combination.getPLType()), statsAll==null ? null : statsAll.getDouble(StatsKey.GROSS_LOSS)>0 ? "positiveNum" : "negativeNum");
		replaceValue("3_Long", statsLong==null ? NA : d2WithPlType(statsLong.getDouble(StatsKey.GROSS_LOSS), combination.getPLType()), statsLong==null ? null : statsLong.getDouble(StatsKey.GROSS_LOSS)>0 ? "positiveNum" : "negativeNum");
		replaceValue("3_Short", statsShort==null ? NA : d2WithPlType(statsShort.getDouble(StatsKey.GROSS_LOSS), combination.getPLType()), statsShort==null ? null : statsShort.getDouble(StatsKey.GROSS_LOSS)>0 ? "positiveNum" : "negativeNum");
		
		replaceLabel("4", L.t("Profit Factor"));
		replaceValue("4_All", statsAll==null ? NA  : d2(statsAll.getDouble(StatsKey.PROFIT_FACTOR)));
		replaceValue("4_Long", statsLong==null ? NA  : d2(statsLong.getDouble(StatsKey.PROFIT_FACTOR)));
		replaceValue("4_Short", statsShort==null ? NA  : d2(statsShort.getDouble(StatsKey.PROFIT_FACTOR)));
		
		replaceLabel("5", L.t("Total Number of trades"));
		replaceValue("5_All", statsAll==null ? NA  : ""+statsAll.getInt(StatsKey.NUMBER_OF_TRADES));
		replaceValue("5_Long", statsLong==null ? NA  : ""+statsLong.getInt(StatsKey.NUMBER_OF_TRADES));
		replaceValue("5_Short", statsShort==null ? NA  : ""+statsShort.getInt(StatsKey.NUMBER_OF_TRADES));

		replaceLabel("6", L.t("Percent Profitable"));
		replaceValue("6_All", statsAll==null ? NA  : d2WithPlType(statsAll.getDouble(StatsKey.WINNING_PCT), PlTypes.Percent));
		replaceValue("6_Long", statsLong==null ? NA  : d2WithPlType(statsLong.getDouble(StatsKey.WINNING_PCT), PlTypes.Percent));
		replaceValue("6_Short", statsShort==null ? NA  : d2WithPlType(statsShort.getDouble(StatsKey.WINNING_PCT), PlTypes.Percent));
		
		replaceLabel("7", L.t("Winning Trades"));
		replaceValue("7_All", statsAll==null ? NA  : ""+statsAll.getInt(StatsKey.NUMBER_OF_PROFITS));
		replaceValue("7_Long", statsLong==null ? NA  : ""+statsLong.getInt(StatsKey.NUMBER_OF_PROFITS));
		replaceValue("7_Short", statsShort==null ? NA  : ""+statsShort.getInt(StatsKey.NUMBER_OF_PROFITS));
		
		replaceLabel("8", L.t("Losing Trades"));
		replaceValue("8_All", statsAll==null ? NA  : ""+statsAll.getInt(StatsKey.NUMBER_OF_LOSSES));
		replaceValue("8_Long", statsLong==null ? NA  : ""+statsLong.getInt(StatsKey.NUMBER_OF_LOSSES));
		replaceValue("8_Short", statsShort==null ? NA  : ""+statsShort.getInt(StatsKey.NUMBER_OF_LOSSES));
		
		replaceLabel("9", L.t("Avg. Trade Net Profit"));
		replaceValue("9_All", statsAll==null ? NA  : d2WithPlType(statsAll.getDouble(StatsKey.AVG_TRADE), combination.getPLType()), statsAll==null ? null : statsAll.getDouble(StatsKey.AVG_TRADE)>0 ? "positiveNum" : "negativeNum");
		replaceValue("9_Long", statsLong==null ? NA  : d2WithPlType(statsLong.getDouble(StatsKey.AVG_TRADE), combination.getPLType()), statsLong==null ? null : statsLong.getDouble(StatsKey.AVG_TRADE)>0 ? "positiveNum" : "negativeNum");
		replaceValue("9_Short", statsShort==null ? NA  : d2WithPlType(statsShort.getDouble(StatsKey.AVG_TRADE), combination.getPLType()), statsShort==null ? null : statsShort.getDouble(StatsKey.AVG_TRADE)>0 ? "positiveNum" : "negativeNum");

		replaceLabel("10", L.t("Avg. Winning Trade"));
		replaceValue("10_All", statsAll==null ? NA  : d2WithPlType(statsAll.getDouble(StatsKey.AVG_WIN), combination.getPLType()));
		replaceValue("10_Long", statsLong==null ? NA  : d2WithPlType(statsLong.getDouble(StatsKey.AVG_WIN), combination.getPLType()));
		replaceValue("10_Short", statsShort==null ? NA  : d2WithPlType(statsShort.getDouble(StatsKey.AVG_WIN), combination.getPLType()));
		
		replaceLabel("11", L.t("Payout Ratio (Avg Win/Loss)"));
		replaceValue("11_All", statsAll==null ? NA  : d2(statsAll.getDouble(StatsKey.PAYOUT_RATIO)));
		replaceValue("11_Long", statsLong==null ? NA  : d2(statsLong.getDouble(StatsKey.PAYOUT_RATIO)));
		replaceValue("11_Short", statsShort==null ? NA  : d2(statsShort.getDouble(StatsKey.PAYOUT_RATIO)));
		
		replaceLabel("12", L.t("Avg. Losing Trade"));
		replaceValue("12_All", statsAll==null ? NA  : d2WithPlType(statsAll.getDouble(StatsKey.AVG_LOSS), combination.getPLType()), statsAll==null ? null : statsAll.getDouble(StatsKey.AVG_LOSS)>0 ? "positiveNum" : "negativeNum");
		replaceValue("12_Long", statsLong==null ? NA  : d2WithPlType(statsLong.getDouble(StatsKey.AVG_LOSS), combination.getPLType()), statsLong==null ? null : statsLong.getDouble(StatsKey.AVG_LOSS)>0 ? "positiveNum" : "negativeNum");
		replaceValue("12_Short", statsShort==null ? NA  : d2WithPlType(statsShort.getDouble(StatsKey.AVG_LOSS), combination.getPLType()), statsShort==null ? null : statsShort.getDouble(StatsKey.AVG_LOSS)>0 ? "positiveNum" : "negativeNum");
				
		replaceLabel("13", L.t("Avg. Profit by Day"));
		replaceValue("13_All", statsAll==null ? NA  : d2WithPlType(statsAll.getDouble(StatsKey.AVG_PROFIT_BY_DAY), combination.getPLType()), statsAll==null ? null : statsAll.getDouble(StatsKey.AVG_PROFIT_BY_DAY)>0 ? "positiveNum" : "negativeNum");
		replaceValue("13_Long", statsLong==null ? NA  : d2WithPlType(statsLong.getDouble(StatsKey.AVG_PROFIT_BY_DAY), combination.getPLType()), statsLong==null ? null : statsLong.getDouble(StatsKey.AVG_PROFIT_BY_DAY)>0 ? "positiveNum" : "negativeNum");
		replaceValue("13_Short", statsShort==null ? NA  : d2WithPlType(statsShort.getDouble(StatsKey.AVG_PROFIT_BY_DAY), combination.getPLType()), statsShort==null ? null : statsShort.getDouble(StatsKey.AVG_PROFIT_BY_DAY)>0 ? "positiveNum" : "negativeNum");
		
		replaceLabel("14", L.t("Avg. Profit by Month"));
		replaceValue("14_All", statsAll==null ? NA  : d2WithPlType(statsAll.getDouble(StatsKey.AVG_PROFIT_BY_MONTH), combination.getPLType()), statsAll==null ? null : statsAll.getDouble(StatsKey.AVG_PROFIT_BY_MONTH)>0 ? "positiveNum" : "negativeNum");
		replaceValue("14_Long", statsLong==null ? NA  : d2WithPlType(statsLong.getDouble(StatsKey.AVG_PROFIT_BY_MONTH), combination.getPLType()), statsLong==null ? null : statsLong.getDouble(StatsKey.AVG_PROFIT_BY_MONTH)>0 ? "positiveNum" : "negativeNum");
		replaceValue("14_Short", statsShort==null ? NA  : d2WithPlType(statsShort.getDouble(StatsKey.AVG_PROFIT_BY_MONTH), combination.getPLType()), statsShort==null ? null : statsShort.getDouble(StatsKey.AVG_PROFIT_BY_MONTH)>0 ? "positiveNum" : "negativeNum");
		
		replaceLabel("15", L.t("Avg. Profit by Year"));
		replaceValue("15_All", statsAll==null ? NA  : d2WithPlType(statsAll.getDouble(StatsKey.AVG_PROFIT_BY_YEAR), combination.getPLType()), statsAll==null ? null : statsAll.getDouble(StatsKey.AVG_PROFIT_BY_YEAR)>0 ? "positiveNum" : "negativeNum");
		replaceValue("15_Long", statsLong==null ? NA  : d2WithPlType(statsLong.getDouble(StatsKey.AVG_PROFIT_BY_YEAR), combination.getPLType()), statsLong==null ? null : statsLong.getDouble(StatsKey.AVG_PROFIT_BY_YEAR)>0 ? "positiveNum" : "negativeNum");
		replaceValue("15_Short", statsShort==null ? NA  : d2WithPlType(statsShort.getDouble(StatsKey.AVG_PROFIT_BY_YEAR), combination.getPLType()), statsShort==null ? null : statsShort.getDouble(StatsKey.AVG_PROFIT_BY_YEAR)>0 ? "positiveNum" : "negativeNum");
		
		replaceLabel("16", L.t("Largest Winning Trade"));
		replaceValue("16_All", statsAll==null ? NA  : d2WithPlType(statsAll.getDouble(StatsKey.MAX_PROFIT), combination.getPLType()), statsAll==null ? null : statsAll.getDouble(StatsKey.MAX_PROFIT)>0 ? "positiveNum" : "negativeNum");
		replaceValue("16_Long", statsLong==null ? NA  : d2WithPlType(statsLong.getDouble(StatsKey.MAX_PROFIT), combination.getPLType()), statsLong==null ? null : statsLong.getDouble(StatsKey.MAX_PROFIT)>0 ? "positiveNum" : "negativeNum");
		replaceValue("16_Short", statsShort==null ? NA  : d2WithPlType(statsShort.getDouble(StatsKey.MAX_PROFIT), combination.getPLType()), statsShort==null ? null : statsShort.getDouble(StatsKey.MAX_PROFIT)>0 ? "positiveNum" : "negativeNum");
	
		replaceLabel("17", L.t("Largest Losing Trade"));
		replaceValue("17_All", statsAll==null ? NA  : d2WithPlType(statsAll.getDouble(StatsKey.MAX_LOSS), combination.getPLType()), statsAll==null ? null : statsAll.getDouble(StatsKey.MAX_LOSS)>0 ? "positiveNum" : "negativeNum");
		replaceValue("17_Long", statsLong==null ? NA  : d2WithPlType(statsLong.getDouble(StatsKey.MAX_LOSS), combination.getPLType()), statsLong==null ? null : statsLong.getDouble(StatsKey.MAX_LOSS)>0 ? "positiveNum" : "negativeNum");
		replaceValue("17_Short", statsShort==null ? NA  : d2WithPlType(statsShort.getDouble(StatsKey.MAX_LOSS), combination.getPLType()), statsShort==null ? null : statsShort.getDouble(StatsKey.MAX_LOSS)>0 ? "positiveNum" : "negativeNum");
		
		replaceLabel("18", L.t("Avg Consecutive Winning Trades"));
		replaceValue("18_All", statsAll==null ? NA  : d2(statsAll.getDouble(StatsKey.AVG_CONSEC_WIN)));
		replaceValue("18_Long", statsLong==null ? NA  : d2(statsLong.getDouble(StatsKey.AVG_CONSEC_WIN)));
		replaceValue("18_Short", statsShort==null ? NA  : d2(statsShort.getDouble(StatsKey.AVG_CONSEC_WIN)));
		
		replaceLabel("19", L.t("Avg Consecutive Losing Trades"));
		replaceValue("19_All", statsAll==null ? NA  : d2(statsAll.getDouble(StatsKey.AVG_CONSEC_LOSS)));
		replaceValue("19_Long", statsLong==null ? NA  : d2(statsLong.getDouble(StatsKey.AVG_CONSEC_LOSS)));
		replaceValue("19_Short", statsShort==null ? NA  : d2(statsShort.getDouble(StatsKey.AVG_CONSEC_LOSS)));
		
		replaceLabel("20", L.t("Max. Consecutive Winning Trades"));
		replaceValue("20_All", statsAll==null ? NA  : ""+statsAll.getInt(StatsKey.MAX_CONSEC_WINS));
		replaceValue("20_Long", statsLong==null ? NA  : ""+statsLong.getInt(StatsKey.MAX_CONSEC_WINS));
		replaceValue("20_Short", statsShort==null ? NA  : ""+statsShort.getInt(StatsKey.MAX_CONSEC_WINS));
		
		replaceLabel("21", L.t("Max. Consecutive Losing Trades"));
		replaceValue("21_All", statsAll==null ? NA  : ""+statsAll.getInt(StatsKey.MAX_CONSEC_LOSS));
		replaceValue("21_Long", statsLong==null ? NA  : ""+statsLong.getInt(StatsKey.MAX_CONSEC_LOSS));
		replaceValue("21_Short", statsShort==null ? NA  : ""+statsShort.getInt(StatsKey.MAX_CONSEC_LOSS));

		replaceLabel("22", L.t("Avg. Bars in Total Trades"));
		replaceValue("22_All", statsAll==null ? NA  : d2(statsAll.getDouble(StatsKey.AVG_BARS_TRADE)));
		replaceValue("22_Long", statsLong==null ? NA  : d2(statsLong.getDouble(StatsKey.AVG_BARS_TRADE)));
		replaceValue("22_Short", statsShort==null ? NA  : d2(statsShort.getDouble(StatsKey.AVG_BARS_TRADE)));
		
		replaceLabel("23", L.t("Avg. Bars in Winning Trades"));
		replaceValue("23_All", statsAll==null ? NA  : d2(statsAll.getDouble(StatsKey.AVG_BARS_WIN)));
		replaceValue("23_Long", statsLong==null ? NA  : d2(statsLong.getDouble(StatsKey.AVG_BARS_WIN)));
		replaceValue("23_Short", statsShort==null ? NA  : d2(statsShort.getDouble(StatsKey.AVG_BARS_WIN)));
		
		replaceLabel("24", L.t("Avg. Bars in Losing Trades"));
		replaceValue("24_All", statsAll==null ? NA  : d2(statsAll.getDouble(StatsKey.AVG_BARS_LOSS)));
		replaceValue("24_Long", statsLong==null ? NA  : d2(statsLong.getDouble(StatsKey.AVG_BARS_LOSS)));
		replaceValue("24_Short", statsShort==null ? NA  : d2(statsShort.getDouble(StatsKey.AVG_BARS_LOSS)));
		
		String dd_key = combination.getPLType() == PlTypes.Percent ? StatsKey.PCT_DRAWDOWN : (combination.getPLType() == PlTypes.Pips ? StatsKey.PIPS_DRAWDOWN : StatsKey.DRAWDOWN);
		
		replaceLabel("25", L.t("Drawdown"));
		replaceValue("25_All", statsAll==null ? NA  : d2WithPlType(statsAll.getDouble(dd_key), combination.getPLType()), statsAll==null ? null : statsAll.getDouble(dd_key)>0 ? "positiveNum" : "negativeNum");
		replaceValue("25_Long", statsLong==null ? NA  : d2WithPlType(statsLong.getDouble(dd_key), combination.getPLType()), statsLong==null ? null : statsLong.getDouble(dd_key)>0 ? "positiveNum" : "negativeNum");
		replaceValue("25_Short", statsShort==null ? NA  : d2WithPlType(statsShort.getDouble(dd_key), combination.getPLType()), statsShort==null ? null : statsShort.getDouble(dd_key)>0 ? "positiveNum" : "negativeNum");
		
		replaceLabel("26", L.t("% Drawdown"));
		replaceValue("26_All", statsAll==null ? NA  : d2WithPlType(statsAll.getDouble(StatsKey.PCT_DRAWDOWN), PlTypes.Percent), statsAll==null ? null : statsAll.getDouble(StatsKey.PCT_DRAWDOWN)>0 ? "positiveNum" : "negativeNum");
		replaceValue("26_Long", statsLong==null ? NA  : d2WithPlType(statsLong.getDouble(StatsKey.PCT_DRAWDOWN), PlTypes.Percent), statsLong==null ? null : statsLong.getDouble(StatsKey.PCT_DRAWDOWN)>0 ? "positiveNum" : "negativeNum");
		replaceValue("26_Short", statsShort==null ? NA  : d2WithPlType(statsShort.getDouble(StatsKey.PCT_DRAWDOWN), PlTypes.Percent), statsShort==null ? null : statsShort.getDouble(StatsKey.PCT_DRAWDOWN)>0 ? "positiveNum" : "negativeNum");

	
		replaceLabel("25", L.t("Drawdown"));
		replaceValue("25_All", statsAll==null ? NA  : d2WithPlType(statsAll.getDouble(dd_key), combination.getPLType()), statsAll==null ? null : statsAll.getDouble(dd_key)>0 ? "positiveNum" : "negativeNum");
		replaceValue("25_Long", statsLong==null ? NA  : d2WithPlType(statsLong.getDouble(dd_key), combination.getPLType()), statsLong==null ? null : statsLong.getDouble(dd_key)>0 ? "positiveNum" : "negativeNum");
		replaceValue("25_Short", statsShort==null ? NA  : d2WithPlType(statsShort.getDouble(dd_key), combination.getPLType()), statsShort==null ? null : statsShort.getDouble(dd_key)>0 ? "positiveNum" : "negativeNum");
		
		replaceLabel("26", L.t("% Drawdown"));
		replaceValue("26_All", statsAll==null ? NA  : d2WithPlType(statsAll.getDouble(StatsKey.PCT_DRAWDOWN), PlTypes.Percent), statsAll==null ? null : statsAll.getDouble(StatsKey.PCT_DRAWDOWN)>0 ? "positiveNum" : "negativeNum");
		replaceValue("26_Long", statsLong==null ? NA  : d2WithPlType(statsLong.getDouble(StatsKey.PCT_DRAWDOWN), PlTypes.Percent), statsLong==null ? null : statsLong.getDouble(StatsKey.PCT_DRAWDOWN)>0 ? "positiveNum" : "negativeNum");
		replaceValue("26_Short", statsShort==null ? NA  : d2WithPlType(statsShort.getDouble(StatsKey.PCT_DRAWDOWN), PlTypes.Percent), statsShort==null ? null : statsShort.getDouble(StatsKey.PCT_DRAWDOWN)>0 ? "positiveNum" : "negativeNum");

		replaceLabel("27", L.t("Total Slippage ($)"));
		replaceValue("27_All", statsAll==null ? NA  : d2WithPlType(statsAll.getDouble(StatsKey.SLIPPAGE_IN_MONEY), PlTypes.Money), statsAll==null ? null : statsAll.getDouble(StatsKey.SLIPPAGE_IN_MONEY)>0 ? "positiveNum" : "negativeNum");
		replaceValue("27_Long", statsLong==null ? NA  : d2WithPlType(statsLong.getDouble(StatsKey.SLIPPAGE_IN_MONEY), PlTypes.Money), statsLong==null ? null : statsLong.getDouble(StatsKey.SLIPPAGE_IN_MONEY)>0 ? "positiveNum" : "negativeNum");
		replaceValue("27_Short", statsShort==null ? NA  : d2WithPlType(statsShort.getDouble(StatsKey.SLIPPAGE_IN_MONEY), PlTypes.Money), statsShort==null ? null : statsShort.getDouble(StatsKey.SLIPPAGE_IN_MONEY)>0 ? "positiveNum" : "negativeNum");
		
		replaceLabel("28", L.t("Total Commission ($)"));
		replaceValue("28_All", statsAll==null ? NA  : d2WithPlType(statsAll.getDouble(StatsKey.COMMISSION), PlTypes.Money), statsAll==null ? null : statsAll.getDouble(StatsKey.COMMISSION)>0 ? "positiveNum" : "negativeNum");
		replaceValue("28_Long", statsLong==null ? NA  : d2WithPlType(statsLong.getDouble(StatsKey.COMMISSION), PlTypes.Money), statsLong==null ? null : statsLong.getDouble(StatsKey.COMMISSION)>0 ? "positiveNum" : "negativeNum");
		replaceValue("28_Short", statsShort==null ? NA  : d2WithPlType(statsShort.getDouble(StatsKey.COMMISSION), PlTypes.Money), statsShort==null ? null : statsShort.getDouble(StatsKey.COMMISSION)>0 ? "positiveNum" : "negativeNum");
	}

}
