package com.strategyquant.plugin.Databank.impl.FilterByCorrelation;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.TimePeriod;
import com.strategyquant.lib.TimePeriods;
import com.strategyquant.tradinglib.CorrelationLib;
import com.strategyquant.tradinglib.CorrelationType;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.correlation.CorrelationComputer;
import com.strategyquant.tradinglib.correlation.CorrelationPeriods;
import com.strategyquant.tradinglib.correlation.CorrelationTypes;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabankFilterByCorrelationServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(DatabankFilterByCorrelationServlet.class);

   protected String execute(String command, Map<String, String[]> args, String method) {
      String var4 = command;
      switch (command.hashCode()) {
         case -1274492040:
            if (var4.equals("filter")) {
               return this.onFilter(args);
            }
         default:
            return apiErrorJSON("Execution failed. Unknown command '" + command + "'.", null);
      }
   }

   private String onFilter(Map<String, String[]> args) {
      JSONObject response = new JSONObject();

      try {
         String projectName = this.tryGetParam(args, "projectName")[0];
         String databankName = this.tryGetParam(args, "databankName")[0];
         int period = Integer.parseInt(this.tryGetParam(args, "period")[0]);
         double maxCorrelation = Double.parseDouble(this.tryGetParam(args, "maxCorrelation")[0]);
         int removedCount = this.filterByCorrelation(projectName, databankName, period, maxCorrelation);
         response.put("success", "Strategies filtered, " + removedCount + " strategies removed.");
         return response.toString();
      } catch (Exception e) {
         Log.error("Error while filtering strategies by correlation", e);
         response.put("error", "Filtering failed - " + e.getMessage());
         return response.toString();
      }
   }

   private int filterByCorrelation(String projectName, String databankName, int period, double maxCorrelation) throws Exception {
      Log.info("Filtering by correlation (Project: " + projectName + ", databank: " + databankName + ")...");
      SQProject project = ProjectEngine.get(projectName);
      Databank databank = (Databank)project.getDatabanks().get(databankName);
      int removedCount = 0;
      ArrayList<ResultsGroup> rgSortedCandidates = new ArrayList<>(databank.getRecords());
      rgSortedCandidates.sort((o1, o2) -> Double.valueOf(o1.getFitness()).compareTo(o2.getFitness()));
      ArrayList<ResultsGroup> rgSortedCandidatesRanked = new ArrayList<>();

      for (ResultsGroup rgSC : rgSortedCandidates) {
         rgSortedCandidatesRanked.add(0, rgSC);
      }

      ArrayList<ResultsGroup> rgUncorrelated = new ArrayList<>();

      while (rgSortedCandidatesRanked.size() > 0) {
         ResultsGroup focusStrat = rgSortedCandidatesRanked.get(0).clone();
         String focusStratName = focusStrat.getName();
         Log.info("Focus strategy to compare: {}", focusStratName);
         rgUncorrelated.add(focusStrat.clone());
         rgSortedCandidatesRanked.remove(0);
         Iterator<ResultsGroup> it = rgSortedCandidatesRanked.iterator();

         while (it.hasNext()) {
            ResultsGroup subStrat = it.next();
            String subStratName = subStrat.getName();
            if (subStratName != focusStratName) {
               new CorrelationPeriods();
               CorrelationType corrType = (CorrelationType)CorrelationTypes.getInstance().findClassByName("ProfitLoss");
               CorrelationComputer correlationComputer = new CorrelationComputer();
               CorrelationPeriods correlationPeriods = this.precomputePeriodsAP(focusStrat, subStrat, period, corrType);
               double corrValue = SQUtils.round2(
                  correlationComputer.computeCorrelation(
                     false, focusStratName, subStratName, focusStrat.orders(), subStrat.orders(), correlationPeriods, corrType
                  )
               );
               Log.info(" - Correlation of '{}'' to focus strategy: {}", subStratName, corrValue);
               if (corrValue > maxCorrelation) {
                  it.remove();
                  removedCount++;
                  Log.info("   - Removing strategy " + subStratName + " (correlation " + corrValue + " > " + maxCorrelation + ")...");
                  databank.remove(subStratName, true, true, true, true, true, null);
               }
            } else {
               it.remove();
            }
         }
      }

      return removedCount;
   }

   private CorrelationPeriods precomputePeriodsAP(
      ResultsGroup paramResultsGroup1, ResultsGroup paramResultsGroup2, int paramInt, CorrelationType paramCorrelationType
   ) throws Exception {
      CorrelationPeriods correlationPeriods = new CorrelationPeriods();
      TimePeriod timePeriod = CorrelationLib.getPeriod(paramResultsGroup1.orders());
      long l1 = timePeriod.from;
      long l2 = timePeriod.to;
      timePeriod = CorrelationLib.getPeriod(paramResultsGroup2.orders());
      if (timePeriod.from < l1) {
         l1 = timePeriod.from;
      }

      if (timePeriod.to > l2) {
         l2 = timePeriod.to;
      }

      TimePeriods timePeriods1 = CorrelationLib.generatePeriods(paramInt, l1, l2);
      TimePeriods timePeriods2 = timePeriods1.clone();
      paramCorrelationType.computePeriods(paramResultsGroup1.orders(), paramInt, timePeriods1);
      correlationPeriods.put(paramResultsGroup1.getName(), timePeriods1);
      paramCorrelationType.computePeriods(paramResultsGroup2.orders(), paramInt, timePeriods2);
      correlationPeriods.put(paramResultsGroup2.getName(), timePeriods2);
      return correlationPeriods;
   }
}
