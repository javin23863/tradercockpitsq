package SQ.CustomAnalysis;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.TimePeriod;
import com.strategyquant.lib.TimePeriods;
import com.strategyquant.tradinglib.CorrelationLib;
import com.strategyquant.tradinglib.CorrelationType;
import com.strategyquant.tradinglib.CustomAnalysisMethod;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.correlation.CorrelationComputer;
import com.strategyquant.tradinglib.correlation.CorrelationPeriods;
import com.strategyquant.tradinglib.correlation.CorrelationTypes;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import java.util.ArrayList;

public class CorrelationFilter extends CustomAnalysisMethod {
   public CorrelationFilter() {
      super("CorrelationFilter", 10);
   }

   public boolean filterStrategy(String var1, String var2, String var3, ResultsGroup var4) throws Exception {
      return true;
   }

   public ArrayList<ResultsGroup> processDatabank(String var1, String var2, String var3, ArrayList<ResultsGroup> var4) throws Exception {
      byte var5 = 10;
      double var6 = Double.parseDouble(this.getInputArgs());
      SQProject var8 = ProjectEngine.get(var1);
      Databank var9 = (Databank)var8.getDatabanks().get(var3);
      ArrayList var10 = new ArrayList(var9.getRecords());
      ArrayList var11 = new ArrayList();

      for (int var12 = 0; var12 < var10.size(); var12++) {
         var11.add(((ResultsGroup)var10.get(var12)).clone());
      }

      var11.sort((var0, var1x) -> -Double.valueOf(var0.getFitness()).compareTo(var1x.getFitness()));

      for (int var23 = 0; var23 < var11.size(); var23++) {
         ResultsGroup var13 = ((ResultsGroup)var11.get(var23)).clone();
         String var14 = var13.getName();

         for (int var15 = var23 + 1; var15 < var11.size(); var15++) {
            ResultsGroup var16 = ((ResultsGroup)var11.get(var15)).clone();
            String var17 = var16.getName();
            CorrelationType var18 = (CorrelationType)CorrelationTypes.getInstance().findClassByName("ProfitLoss");
            CorrelationComputer var19 = new CorrelationComputer();
            CorrelationPeriods var20 = this.precomputePeriodsAP(var13, var16, var5, var18);
            double var21 = SQUtils.round2(var19.computeCorrelation(false, var14, var17, var13.orders(), var16.orders(), var20, var18));
            if (var21 > var6) {
               var11.remove(var15);
               var15--;
            }
         }
      }

      return var11;
   }

   private CorrelationPeriods precomputePeriodsAP(ResultsGroup var1, ResultsGroup var2, int var3, CorrelationType var4) throws Exception {
      CorrelationPeriods var5 = new CorrelationPeriods();
      TimePeriod var6 = CorrelationLib.getPeriod(var1.orders());
      long var7 = var6.from;
      long var9 = var6.to;
      var6 = CorrelationLib.getPeriod(var2.orders());
      if (var6.from < var7) {
         var7 = var6.from;
      }

      if (var6.to > var9) {
         var9 = var6.to;
      }

      TimePeriods var11 = CorrelationLib.generatePeriods(var3, var7, var9);
      TimePeriods var12 = var11.clone();
      var4.computePeriods(var1.orders(), var3, var11);
      var5.put(var1.getName(), var11);
      var4.computePeriods(var2.orders(), var3, var12);
      var5.put(var2.getName(), var12);
      return var5;
   }
}
