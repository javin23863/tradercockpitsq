package com.strategyquant.tradinglib.project.bestresults;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import java.io.File;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BestResultOverview {
   public static final Logger Log = LoggerFactory.getLogger(BestResultOverview.class);
   private static String template = null;

   public String print(ResultsGroup var1, byte var2) {
      String var3 = null;

      try {
         var3 = this.loadTemplate();
         boolean var4 = var1.isStockpickerStrategy();
         if (var4) {
            template = template.replaceAll("StatsMoney.ReturnDDRatio", "StatsMoney.ReturnOpenDDRatio");
            template = template.replaceAll("StatsMoney.Drawdown", "StatsMoney.OpenDrawdown");
            template = template.replaceAll("StatsPct.DrawdownPct", "StatsPct.OpenDrawdownPct");
         }

         SQStats var5 = var1.portfolio().stats((byte)0, (byte)10, var2);
         Object var6 = null;

         for (DatabankColumn var8 : DatabankColumns.get().getAvailableClasses()) {
            try {
               double var9 = var8.getNumericValue(var5);
               var6 = var9 >= 0.0 ? "positiveNum" : "negativeNum";
               if (var8.getClass().getSimpleName().equals("Drawdown")
                  || var8.getClass().getSimpleName().equals("DrawdownPct")
                  || var8.getClass().getSimpleName().equals("OpenDrawdown")
                  || var8.getClass().getSimpleName().equals("OpenDrawdownPct")) {
                  var6 = "negativeNum";
               }

               var3 = var3.replace(
                  "{{StatsMoney." + var8.getClass().getSimpleName() + "}}", "<span class=\"" + var6 + "\">" + var8.printPlValue(var5, (byte)10) + "</span>"
               );
               var3 = var3.replace("{StatsMoney." + var8.getClass().getSimpleName() + "}", var8.printPlValue(var5, (byte)10));
            } catch (NullPointerException var11) {
            } catch (Exception var12) {
               Log.error("Exc. Column " + var8.getClass().getSimpleName(), var12);
            }
         }

         var3 = var3.replaceAll(Pattern.quote("${Label_1}"), L.t("TOTAL PROFIT", new Object[0]));
         var3 = var3.replaceAll(Pattern.quote("${Label_2}"), L.t("YEARLY AVG PROFIT", new Object[0]));
         var3 = var3.replaceAll(Pattern.quote("${Label_3}"), L.t("YEARLY AVG % RETURN", new Object[0]));
         var3 = var3.replaceAll(Pattern.quote("${Label_4}"), L.t("CAGR", new Object[0]));
         var3 = var3.replaceAll(Pattern.quote("${Label_5}"), L.t("# OF TRADES", new Object[0]));
         var3 = var3.replaceAll(Pattern.quote("${Label_6}"), L.t("SHARPE RATIO", new Object[0]));
         var3 = var3.replaceAll(Pattern.quote("${Label_7}"), L.t("PROFIT FACTOR", new Object[0]));
         var3 = var3.replaceAll(Pattern.quote("${Label_8}"), var4 ? L.t("RETURN / OPEN DD RATIO", new Object[0]) : L.t("RETURN / DD RATIO", new Object[0]));
         var3 = var3.replaceAll(Pattern.quote("${Label_9}"), L.t("WINNING PERCENTAGE", new Object[0]));
         var3 = var3.replaceAll(Pattern.quote("${Label_10}"), var4 ? L.t("OPEN DRAWDOWN", new Object[0]) : L.t("DRAWDOWN", new Object[0]));
         var3 = var3.replaceAll(Pattern.quote("${Label_11}"), var4 ? L.t("OPEN % DRAWDOWN", new Object[0]) : L.t("% DRAWDOWN", new Object[0]));
      } catch (Exception var13) {
      }

      return var3 != null ? var3 : "";
   }

   private String loadTemplate() throws Exception {
      if (template == null) {
         try {
            String var1 = MainApp.getDataPath() + "/user/settings/ResultsDashboard/template.html";
            template = SQUtils.fileToString(new File(var1));
            template = template.replaceAll("StatsPct.AnnualPctReturn", "StatsMoney.AvgPctProfitPerYear");
            template = template.replaceAll("StatsPct", "StatsMoney");
            template = template.replaceAll("StatsPips", "StatsMoney");
            return template;
         } catch (Exception var2) {
            Log.error("Exc. Cannot load template file.", var2);
            throw new Exception("Cannot load template file.");
         }
      } else {
         return template;
      }
   }
}
