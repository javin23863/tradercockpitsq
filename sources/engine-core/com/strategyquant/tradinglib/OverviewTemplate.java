package com.strategyquant.tradinglib;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.debug.Debugger;
import com.strategyquant.tradinglib.results.stats.comparator.OrderComparatorByCloseTime;
import java.io.File;
import java.text.DecimalFormat;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import org.apache.commons.lang3.text.StrSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class OverviewTemplate extends Debugger {
   public static final Logger Log = LoggerFactory.getLogger(OverviewTemplate.class);
   private static DecimalFormat d2Format = new DecimalFormat("#.##");
   private String name = "?";
   private String screenshotName = null;
   protected String htmlTemplateName = null;
   private Map<String, String> valuesMap = new HashMap<>();
   protected static final Comparator<Order> ComparatorByCloseTime = new OrderComparatorByCloseTime();
   protected static String NA = "N/A";
   protected String template;
   protected String templateOrig;

   public String getName() {
      return this.name;
   }

   public void setName(String var1) {
      this.name = var1;
   }

   public String getScreenshotName() {
      return this.screenshotName;
   }

   public void setScreenshotName(String var1) {
      this.screenshotName = var1;
   }

   public String getHtmlTemplateName() {
      return this.htmlTemplateName;
   }

   public void setHtmlTemplateName(String var1) {
      this.htmlTemplateName = var1;
   }

   @Override
   public String toString() {
      return this.name;
   }

   public abstract String drawValues(ResultsGroup var1, String var2, StatsTypeCombination var3) throws Exception;

   public void loadTemplate() throws Exception {
      try {
         String var1 = MainApp.getDataPath() + "internal/plugins/ResultsOverview/templates/" + this.htmlTemplateName;
         this.templateOrig = SQUtils.fileToString(new File(var1));
      } catch (Exception var2) {
         Log.error("Exc.", var2);
         throw new Exception("Unable to read template file.");
      }
   }

   public void reset() {
      this.valuesMap.clear();
      this.template = this.templateOrig;
   }

   public String print() {
      this.template = this.template.replaceAll(Pattern.quote("${Label_Stats}"), L.t("Stats", new Object[0]));
      this.template = this.template.replaceAll(Pattern.quote("${Label_Strategy}"), L.t("Strategy", new Object[0]));
      this.template = this.template.replaceAll(Pattern.quote("${Label_Trades}"), L.t("Trades", new Object[0]));
      this.template = this.template.replaceAll(Pattern.quote("${Label_Performance_summary}"), L.t("Performance summary", new Object[0]));
      this.template = this.template.replaceAll(Pattern.quote("${Label_All_trades}"), L.t("All trades", new Object[0]));
      this.template = this.template.replaceAll(Pattern.quote("${Label_Long_trades}"), L.t("Long trades", new Object[0]));
      this.template = this.template.replaceAll(Pattern.quote("${Label_Short_trades}"), L.t("Short trades", new Object[0]));
      return StrSubstitutor.replace(this.template, this.valuesMap);
   }

   public String d2WithPlType(Double var1, byte var2) {
      String var3 = this.d2(var1);
      switch (var2) {
         case 10:
            return "$ " + var3;
         case 20:
            return var3 + " %";
         case 30:
            return var3 + " ticks";
         default:
            return var2 == 0 ? var3 : var3 + " " + var2;
      }
   }

   public String d2(Double var1) {
      return d2Format.format(var1);
   }

   protected void replace(String var1, String var2, String var3) {
      this.replace(var1, var2, var3, null);
   }

   protected void replace(String var1, String var2, String var3, String var4) {
      if (var4 != null) {
         this.valuesMap.put("Class_" + var1, var4);
      }

      this.valuesMap.put("Label_" + var1, var2);
      this.valuesMap.put("Value_" + var1, var3);
   }

   protected void replaceLabel(String var1, String var2) {
      this.valuesMap.put("Label_" + var1, L.tsq(var2));
   }

   protected void replaceValue(String var1, String var2) {
      this.replaceValue(var1, var2, null);
   }

   protected void replaceValue(String var1, String var2, String var3) {
      if (var3 != null) {
         this.valuesMap.put("Class_" + var1, var3);
      }

      this.valuesMap.put("Value_" + var1, var2);
   }
}
