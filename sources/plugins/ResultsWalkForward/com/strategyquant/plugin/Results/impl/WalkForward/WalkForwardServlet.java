package com.strategyquant.plugin.Results.impl.WalkForward;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.plugin.Results.impl.WalkForward.views.WFViews;
import com.strategyquant.tradinglib.CustomCellFormat;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.WalkForwardColumn;
import com.strategyquant.tradinglib.WalkForwardPeriod;
import com.strategyquant.tradinglib.WalkForwardResult;
import com.strategyquant.tradinglib.conditions.Condition;
import com.strategyquant.tradinglib.conditions.IConditionValue;
import com.strategyquant.tradinglib.crosscheck.CrossCheckDataNotExistException;
import com.strategyquant.tradinglib.databank.DatabankColumns;
import com.strategyquant.tradinglib.databank.DatabankTableColumnEntry;
import com.strategyquant.tradinglib.databank.DatabankTableView;
import com.strategyquant.tradinglib.databank.RecordNotFoundException;
import com.strategyquant.tradinglib.optimization.WalkForwardColumns;
import com.strategyquant.tradinglib.optimization.WalkForwardMatrixResult;
import com.strategyquant.tradinglib.optimization.WalkForwardParametersStability;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.results.IResultsGroupProvider;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.util.ArrayList;
import java.util.Map;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WalkForwardServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(WalkForwardServlet.class);
   private static final String LOCK_WFSERVLET = "WalkForwardServlet";
   private static IResultsGroupProvider rgProvider;
   private WFViews views;

   public WalkForwardServlet(IResultsGroupProvider var1) {
      rgProvider = var1;
      this.views = new WFViews(MainApp.getDataPath() + "/user/settings/views/walkForwardResults");
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      if (var1.startsWith("views")) {
         return this.views.execute(var1, var2, var3);
      }

      switch (var1) {
         case "getConditions":
            return this.onGetConditions(var2);
         case "print":
            return this.onPrint(var2);
         case "printTable":
            return this.onPrintTable(var2);
         case "export":
            return this.onExport(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onExport(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"path", "useComma"});
      String var2 = ((String[])var1.get("project"))[0].toString();
      String var3 = ((String[])var1.get("databank"))[0].toString();
      String var4 = ((String[])var1.get("strategy"))[0].toString();
      String var5 = ((String[])var1.get("resultkey"))[0].toString();
      String var6 = ((String[])var1.get("view"))[0].toString();
      String var7 = ((String[])var1.get("path"))[0];
      boolean var8 = Boolean.parseBoolean(((String[])var1.get("useComma"))[0]);
      new JSONObject();
      Databank var10 = (Databank)ProjectEngine.get(var2).getDatabanks().get(var3);
      ResultsGroup var11 = var10.getLocked(var4, "WalkForwardServlet");

      try {
         DatabankTableView var12 = this.views.getViewByName(var6);
         WalkForwardMatrixResult var13 = (WalkForwardMatrixResult)var11.mainResult().get("WalkForwardResult");
         if (var13 == null) {
            throw new Exception("WF results not found.");
         }

         WalkForwardResult var14 = var13.getWFResult(var5, false);
         WFParamsExport var15 = new WFParamsExport();
         if (var7.endsWith("xlsx")) {
            var15.toXlsx(var7, var8, var14, var12);
         } else {
            var15.toCsv(var7, var8, var14, var12);
         }
      } catch (Exception var19) {
         return apiErrorJSON(L.t("WF params export failed", new Object[0]), var19);
      } finally {
         var11.releaseLock("WalkForwardServlet");
      }

      JSONObject var21 = new JSONObject();
      var21.put("success", "WF params exported.");
      return var21.toString();
   }

   private synchronized String onGetConditions(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      String var3 = "<Conditions/>";
      ResultsGroup var4 = null;

      try {
         var4 = rgProvider.get(var1, "WalkForwardServlet");
         WalkForwardMatrixResult var5 = (WalkForwardMatrixResult)var4.mainResult().get("WalkForwardResult");
         if (var5 == null) {
            throw new Exception("WalkForwardResult not found.");
         }

         if (var5.conditionsElem == null) {
            var5.conditionsElem = (Element)var4.specialValues().get("WalkForwardConditions");
         }

         if (var5.conditionsElem == null) {
            throw new Exception("Conditions not found.");
         }

         var3 = XMLUtil.elementToString(var5.conditionsElem);
      } catch (RecordNotFoundException var11) {
         Log.info("onGetConditions: " + var11.getMessage());
         return apiErrorJSONNoLog(null, var11);
      } catch (Exception var12) {
         Log.error("Error while loading WF Conditions. Exc.", var12);
      } finally {
         if (var4 != null) {
            var4.releaseLock("WalkForwardServlet");
         }
      }

      var2.put("conditions", var3);
      var2.put("success", "ok");
      return var2.toString();
   }

   private synchronized String onPrint(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"project", "databank", "strategy", "resultkey", "conditions", "thresholdPct"});
      String var2 = ((String[])var1.get("resultkey"))[0].toString();
      String var3 = ((String[])var1.get("conditions"))[0].toString();
      int var4 = Integer.parseInt(((String[])var1.get("thresholdPct"))[0]);
      int var5 = Integer.parseInt(((String[])var1.get("robRows"))[0]);
      int var6 = Integer.parseInt(((String[])var1.get("robColumns"))[0]);
      int var7 = Integer.parseInt(((String[])var1.get("robComb"))[0]);
      String var8 = ((String[])var1.get("type"))[0].toString();
      Element var9 = null;
      if (var1.containsKey("column")) {
         try {
            var9 = XMLUtil.stringToXmlElement(((String[])var1.get("column"))[0].toString());
         } catch (Exception var21) {
            Log.error("Exc.", var21);
         }
      }

      JSONObject var10 = new JSONObject();
      ResultsGroup var11 = null;

      try {
         var11 = rgProvider.get(var1, "WalkForwardServlet");
         WalkForwardMatrixResult var12 = (WalkForwardMatrixResult)var11.mainResult().get("WalkForwardResult");
         if (var12 != null) {
            WalkForwardResult var13 = var12.getWFResult(var2, false);
            Element var14 = this.getConditionsEl(var12, var3, var4, var5, var6, var7);
            ArrayList var15 = ProjectConfigHelper.getConditions(var14);
            if (this.paramsChanged(var12, var15, var4, var5, var6, var7)) {
               var12.computeRobustnessResults(var11, var14, var15, var4, var5, var6, var7);
            }

            var10.put("chart", this.printChart(var11, var12, var9, var8));
            var10.put("conditions", this.printConditionsTable(var11, var13, var14, var15));
            var10.put("result", this.printRobustnessResult(var12, var2, var5 * var6));
         }
      } catch (RecordNotFoundException var22) {
         Log.info("onPrint: " + var22.getMessage());
         return apiErrorJSONNoLog(null, var22);
      } catch (Exception var23) {
         return apiErrorJSON(L.t("Getting walk forward results failed", new Object[0]), var23);
      } finally {
         if (var11 != null) {
            var11.releaseLock("WalkForwardServlet");
         }
      }

      var10.put("success", "ok");
      return var10.toString();
   }

   private Element getConditionsEl(WalkForwardMatrixResult var1, String var2, int var3, int var4, int var5, int var6) {
      Element var7 = null;

      try {
         if (!var2.equals("replace-default-conditions") && !var2.equals("add-default-conditions")) {
            var7 = XMLUtil.stringToElement(var2);
            var7.setAttribute("thresholdPct", var3 + "");
            var7.setAttribute("robCombRows", var4 + "");
            var7.setAttribute("robCombCols", var5 + "");
            var7.setAttribute("robMinComb", var6 + "");
         } else {
            if (!var2.equals("replace-default-conditions") && var1.conditionsElem != null) {
               var7 = var1.conditionsElem.clone();
               Element var8 = XMLUtil.fileToXmlElement(new File(SQPaths.wfDefaultConditionsPath));

               for (Element var10 : var8.getChildren("Condition")) {
                  var7.addContent(var10.clone());
               }
            } else {
               var7 = XMLUtil.fileToXmlElement(new File(SQPaths.wfDefaultConditionsPath));
            }

            var7.setAttribute("thresholdPct", "80");
            var7.setAttribute("robCombRows", "3");
            var7.setAttribute("robCombCols", "3");
            var7.setAttribute("robMinComb", "7");
         }
      } catch (Exception var11) {
         Log.error("Cannot parse conditions. Exc.", var11);
      }

      return var7;
   }

   private boolean paramsChanged(WalkForwardMatrixResult var1, ArrayList<Condition> var2, int var3, int var4, int var5, int var6) {
      if (var1.conditions != null
         && var1.conditions.size() == var2.size()
         && var1.thresholdPct == var3
         && var1.rows == var4
         && var1.cols == var5
         && var1.comb == var6) {
         for (int var7 = 0; var7 < var2.size(); var7++) {
            Condition var8 = (Condition)var1.conditions.get(var7);
            Condition var9 = (Condition)var2.get(var7);
            if (!var8.toString().equals(var9.toString()) || var8.isUsed() != var9.isUsed()) {
               return true;
            }
         }

         return false;
      } else {
         return true;
      }
   }

   private JSONObject printRobustnessResult(WalkForwardMatrixResult var1, String var2, int var3) throws Exception {
      WalkForwardResult var4 = null;
      boolean var5;
      boolean var6;
      if (var1.getResultList().size() > 1) {
         var4 = var1.getWFResult(var2, false);
         var5 = var1.passed;
         var6 = var4.passed;
      } else {
         var4 = (WalkForwardResult)var1.getResultList().get(0);
         var5 = var4.passed;
         var6 = var4.passed;
      }

      JSONObject var7 = new JSONObject();
      var7.put("combination", var4.toString(var1.periodType));
      var7.put("matrixResult", var1.getResultList().size() > 1);
      var7.put("passed", var5);
      var7.put("runPassed", var6);
      var7.put("condPassed", var4.passedConditions);
      var7.put("condTotal", var4.testedConditions);
      var7.put("combPassed", var1.combinationsPassed);
      var7.put("combTotal", var1.combinationsTotal);
      var7.put("condPassedInBestGroup", var1.condPassedInBestGroup);
      var7.put("groupSize", var3);
      var7.put("bestGroupArroundCell", var1.bestGroupArroundCell);
      var7.put("recommendedCombination", var1.recommendedCombination);
      var7.put("scorePct", var4.scorePerc);
      return var7;
   }

   private String printConditionsTable(ResultsGroup var1, WalkForwardResult var2, Element var3, ArrayList<Condition> var4) throws Exception {
      for (int var5 = 0; var5 < var4.size(); var5++) {
         Condition var6 = (Condition)var4.get(var5);

         try {
            boolean var7 = var6.isMet(new Object[]{var1, var2});
            Element var8 = (Element)var3.getChildren().get(var5);
            var8.setAttribute("passed", var7 + "");
            Element var9 = var8.getChild("Left-Side");

            try {
               ((Element)var9.getChildren().get(0)).setAttribute("value", var6.getLeftStatsValue() + "");
            } catch (Exception var13) {
            }

            Element var10 = var8.getChild("Right-Side");

            try {
               ((Element)var10.getChildren().get(0)).setAttribute("value", var6.getRightStatsValue() + "");
            } catch (Exception var12) {
            }
         } catch (Exception var14) {
            Log.error("Error while checking condition", var14);
         }
      }

      return XMLUtil.elementToString(var3);
   }

   private JSONObject printChart(ResultsGroup var1, WalkForwardMatrixResult var2, Element var3, String var4) throws Exception {
      if (var2 != null && var2.getResultList().size() != 1 && var3 != null) {
         JSONObject var5 = new JSONObject();
         double var6 = -1.0;
         double var8 = -1.0;
         JSONArray var10 = new JSONArray();
         IConditionValue var11 = ProjectConfigHelper.getConditionValue(var3);
         if (var4.equals("score")) {
            JSONArray var12 = new JSONArray();

            for (int var13 = var2.start1; var13 <= var2.stop1; var13 += var2.increment1) {
               var12.put(var13);
            }

            var5.put("x", var12);
            JSONArray var24 = new JSONArray();

            for (int var14 = var2.start2; var14 <= var2.stop2; var14 += var2.increment2) {
               var24.put(var14);
            }

            var5.put("y", var24);

            for (int var27 = var2.start2; var27 <= var2.stop2; var27 += var2.increment2) {
               JSONArray var15 = new JSONArray();

               for (int var16 = var2.start1; var16 <= var2.stop1; var16 += var2.increment1) {
                  WalkForwardResult var17 = var2.getWFResult(var16, var27);
                  boolean var18 = var17.toString(var2.periodType).equals(var2.bestGroupArroundWFCell.toString(var2.periodType)) && var2.passed;
                  var15.put(new JSONArray().put(var17.passed).put(var17.scorePerc).put(var18));
               }

               var10.put(var15);
            }
         } else {
            for (int var22 = var2.start2; var22 <= var2.stop2; var22 += var2.increment2) {
               for (int var25 = var2.start1; var25 <= var2.stop1; var25 += var2.increment1) {
                  WalkForwardResult var28 = var2.getWFResult(var25, var22);
                  double var30 = 0.0;

                  try {
                     var30 = var11.getValue(new Object[]{var1, var28});
                  } catch (CrossCheckDataNotExistException var20) {
                  } catch (Exception var21) {
                     Log.error("Exc.", var21);
                  }

                  if (var6 == -1.0 && var8 == -1.0) {
                     var6 = var30;
                     var8 = var30;
                  }

                  if (var30 < var6) {
                     var6 = var30;
                  }

                  if (var30 > var8) {
                     var8 = var30;
                  }

                  var10.put(new JSONObject().put("x", var25).put("y", var22).put("z", var30).put("style", var30));
               }
            }
         }

         var5.put("values", var10);
         byte var23 = 1;
         String var26 = L.t("Value", new Object[0]);

         try {
            String var29 = var3.getAttributeValue("class");
            if (DatabankColumns.get().checkClassExists(var29)) {
               DatabankColumn var31 = (DatabankColumn)DatabankColumns.get().findClassByName(var29);
               var23 = var31.getValueType();
               var26 = var11.getTitle();
            } else if (WalkForwardColumns.get().checkClassExists(var29)) {
               WalkForwardColumn var32 = (WalkForwardColumn)WalkForwardColumns.get().findClassByName(var29);
               var23 = var32.getValueType();
               var26 = var11.getTitle();
            }

            if (var29.equals("WFScore")) {
               var5.put("zMin", 0);
               var5.put("zMax", 100);
            }
         } catch (Exception var19) {
            Log.error("Exc.", var19);
         }

         if (!var5.has("zMin")) {
            var5.put("zMin", var6);
            var5.put("zMax", var8);
         }

         var5.put("reverseColorScale", var23 == 2);
         var5.put("reverseColorScale", var23 == 2);
         var5.put("periodInPercent", var2.periodType == 10);
         var5.put("xlabel", var2.periodType == 10 ? L.t("OOS %", new Object[0]) : L.t("IS days", new Object[0]));
         var5.put("ylabel", var2.periodType == 10 ? L.t("Runs", new Object[0]) : L.t("OOS days", new Object[0]));
         var5.put("zlabel", var26);
         return var5;
      } else {
         return null;
      }
   }

   private synchronized String onPrintTable(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"project", "databank", "strategy", "resultkey"});
      String var2 = ((String[])var1.get("resultkey"))[0].toString();
      String var3 = ((String[])var1.get("view"))[0].toString();
      JSONObject var4 = new JSONObject();
      ResultsGroup var5 = rgProvider.get(var1, "WalkForwardServlet");

      try {
         DatabankTableView var6 = this.views.getViewByName(var3);
         MainApp.settings().set("wfResultsView", var3);
         var4.put("rows", new JSONArray());
         var4.put("success", "ok");
         WalkForwardMatrixResult var7 = (WalkForwardMatrixResult)var5.mainResult().get("WalkForwardResult");
         if (var7 != null) {
            WalkForwardResult var8 = var7.getWFResult(var2, false);
            var4.put("rows", this.printTable(var8, var6));
            var4.put("combination", var8.toString(var7.periodType));
            var4.put("matrixResult", var7.getResultList().size() > 1);

            try {
               double var9 = (Double)var5.specialValues().get(WalkForwardParametersStability.getSpecialValuesKey(var2));
               var4.put("parametersStability", var9);
            } catch (Exception var14) {
               Log.error("Cannot get parameters stability for result '" + var2 + "'", var14);
               var4.put("parametersStability", "N/A");
            }
         }

         return var4.toString();
      } finally {
         var5.releaseLock("WalkForwardServlet");
      }
   }

   private JSONArray printTable(WalkForwardResult var1, DatabankTableView var2) {
      JSONArray var3 = new JSONArray();
      ArrayList var4 = var1.wfPeriods;

      for (int var5 = 0; var5 < var4.size(); var5++) {
         WalkForwardPeriod var6 = (WalkForwardPeriod)var4.get(var5);
         JSONObject var7 = new JSONObject();
         JSONArray var8 = new JSONArray();
         var8.put(SQTime.toDateString(var6.optimizeFrom) + " - " + SQTime.toDateString(var6.optimizeTo));
         if (var5 == var4.size() - 1) {
            var8.put(SQTime.toDateString(var6.runFrom) + " - " + SQTime.toDateString(var6.runTo) + " (future)");
         } else {
            var8.put(SQTime.toDateString(var6.runFrom) + " - " + SQTime.toDateString(var6.runTo));
         }

         var8.put(SQTime.getDaysBetween(var6.optimizeFrom, var6.optimizeTo));
         var8.put(SQTime.getDaysBetween(var6.runFrom, var6.runTo));
         ArrayList var9 = var2.columns;

         for (int var10 = 0; var10 < var9.size(); var10++) {
            DatabankTableColumnEntry var11 = (DatabankTableColumnEntry)var9.get(var10);
            DatabankColumn var12 = var11.tableColumn;

            try {
               var8.put(var11.sampleType == 10 ? var12.getNumericValue(var6.optimizationStatData) : var12.getNumericValue(var6.runStatData));
            } catch (Exception var14) {
               Log.error("WF column value couldn't be printed.", var14);
               var8.put("N/A");
            }
         }

         var8.put(var6.testParameters);
         var7.put("id", "r" + var5);
         if (var5 == var4.size() - 1) {
            var7.put("class", "wf-italic");
         }

         var7.put("data", var8);
         var3.put(var7);
      }

      return var3;
   }

   public String printHtmlFormatedValue(SQStats var1, DatabankColumn var2, String var3) throws Exception {
      double var4 = var2.getNumericValue(var1);
      CustomCellFormat var6 = var2.getCustomFormat((byte)0, (byte)10, (byte)127, Double.toString(var4), null);
      var6.customStyle = var3;
      return var6.getHtmlContent(var4);
   }
}
