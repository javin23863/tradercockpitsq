package SQ.Stats.Overview;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Result;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;
import java.util.ArrayList;

public class SQDefaultWithPortfolio extends SQDefault {
   public SQDefaultWithPortfolio() {
      this.setName(L.tsq("SQ with Portfolio"));
      this.setScreenshotName("sqdefault_screenshot_portfolio.jpg");
      this.setHtmlTemplateName("sqdefault_template.htm");

      try {
         this.loadTemplate();
      } catch (Exception var2) {
         Log.error("Overview template couldn't be loaded. Exc. ", var2);
      }
   }

   @Override
   public String drawValues(ResultsGroup var1, String var2, StatsTypeCombination var3) throws Exception {
      this.reset();
      this.replaceValues(var1, var2, var3);
      this.addErrorsTable(var1, var2);
      return this.print();
   }

   @Override
   public void replaceValues(ResultsGroup var1, String var2, StatsTypeCombination var3) throws Exception {
      this.addPortfolioTable(var1, var2, var3);
      super.replaceValues(var1, var2, var3);
   }

   private void addPortfolioTable(ResultsGroup var1, String var2, StatsTypeCombination var3) throws Exception {
      if (var1 != null && var1.isPortfolio()) {
         StringBuilder var4 = new StringBuilder("<div class=\"performance\">");
         var4.append("<h1>" + L.t("Symbols/Strategies in Portfolio", new Object[0]) + "</h1>");
         var4.append("<table class=\"portfolio\" cellspacing=\"1\" cellpadding=\"0\" border=\"0\">");
         var4.append("<tr>");
         var4.append(
            "<th>"
               + L.t("Name", new Object[0])
               + "</th><th>"
               + L.t("Net Profit", new Object[0])
               + "</th><th>"
               + L.t("Drawdown ($)", new Object[0])
               + "</th><th>"
               + L.t("Drawdown (%)", new Object[0])
               + "</th><th>"
               + L.t("Trades", new Object[0])
               + "</th><th>"
               + L.t("Profit Factor", new Object[0])
               + "</th><th>"
               + L.t("Sharpe Ratio", new Object[0])
               + "</th><th>"
               + L.t("Return / DD Ratio", new Object[0])
               + "</th><th>"
               + L.t("Avg. Trade", new Object[0])
               + "</th><th>"
               + L.t("Stagnation (Days)", new Object[0])
               + "</th>"
         );
         var4.append("</tr>");
         var4.append(this.printPortfolioListBody(var1, var2, var3));
         var4.append("</table>");
         var4.append("</div>");
         var4.append("<br/><br/><br/>\n\n<div id=\"summaryBox\">");
         this.template = this.template.replace("<div id=\"summaryBox\">", var4.toString());
      }
   }

   private String printPortfolioListBody(ResultsGroup var1, String var2, StatsTypeCombination var3) throws Exception {
      StringBuilder var4 = new StringBuilder("");
      ArrayList var5 = new ArrayList();

      for (String var7 : var1.getResultKeys()) {
         if (!var7.equals("Portfolio")) {
            var5.add(var7);
         }
      }

      byte var12 = var3.getPLType();
      byte var13 = var3.getSampleType();

      for (String var9 : var5) {
         if (!var9.startsWith("WF:") && !var9.startsWith("CrossCheck_")) {
            Result var10 = var1.subResult(var9);
            SQStats var11 = var10.stats(var3.getDirection(), var12, var13);
            var4.append("<tr>");
            var4.append("<td class=\"name\">");
            var4.append(this.limitStrategyName(var9));
            var4.append("</td><td class=\"name\">");
            var4.append(this.d2WithPlType(var11.getDouble("NetProfit"), var12));
            var4.append("</td><td>");
            var4.append(this.d2WithPlType(var11.getDouble("Drawdown"), var12));
            var4.append("</td><td>");
            var4.append(this.d2WithPlType(var11.getDouble("DrawdownPct"), (byte)20));
            var4.append("</td><td>");
            var4.append(var11.getInt("NumberOfTrades"));
            var4.append("</td><td>");
            var4.append(this.d2(var11.getDouble("ProfitFactor")));
            var4.append("</td><td>");
            var4.append(this.d2(var11.getDouble("SharpeRatio")));
            var4.append("</td><td>");
            var4.append(this.d2(var11.getDouble("ReturnDDRatio")));
            var4.append("</td><td class=\"name\">");
            var4.append(this.d2WithPlType(var11.getDouble("AvgTrade"), var12));
            var4.append("</td><td>");
            var4.append(var11.getInt("Stagnation"));
            var4.append("</td></tr>");
         }
      }

      Result var14 = var1.subResult("Portfolio");
      SQStats var15 = var14.stats(var3.getDirection(), var12, var13);
      var4.append("<tr>");
      var4.append("<td class=\"name\"><strong>" + L.t("Portfolio", new Object[0]) + "</strong></td>");
      var4.append("<td class=\"name\">");
      var4.append(this.d2WithPlType(var15.getDouble("NetProfit"), var12));
      var4.append("</td><td>");
      var4.append(this.d2WithPlType(var15.getDouble("Drawdown"), var12));
      var4.append("</td><td>");
      var4.append(this.d2WithPlType(var15.getDouble("DrawdownPct"), (byte)20));
      var4.append("</td><td>");
      var4.append(var15.getInt("NumberOfTrades"));
      var4.append("</td><td>");
      var4.append(this.d2(var15.getDouble("ProfitFactor")));
      var4.append("</td><td>");
      var4.append(this.d2(var15.getDouble("SharpeRatio")));
      var4.append("</td><td>");
      var4.append(this.d2(var15.getDouble("ReturnDDRatio")));
      var4.append("</td><td class=\"name\">");
      var4.append(this.d2WithPlType(var15.getDouble("AvgTrade"), var12));
      var4.append("</td><td>");
      var4.append(var15.getInt("Stagnation"));
      var4.append("</td></tr>");
      return var4.toString();
   }

   private String limitStrategyName(String var1) {
      return var1.length() > 30 ? var1.substring(0, 30) + "..." : var1;
   }
}
