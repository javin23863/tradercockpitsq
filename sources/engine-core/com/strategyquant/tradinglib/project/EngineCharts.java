package com.strategyquant.tradinglib.project;

import com.strategyquant.tradinglib.EngineChart;
import com.strategyquant.tradinglib.enginecharts.EngineChartsList;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class EngineCharts {
   private ArrayList<EngineChart> list = new ArrayList<>();
   private ArrayList<EngineChart> global = EngineChartsList.getInstance().globalCharts;
   public FitnessEvolution fitnessEvolution = null;
   private SQProject project;

   public EngineCharts(SQProject var1) {
      this.project = var1;
      this.init(var1);
   }

   private void init(SQProject var1) {
      this.fitnessEvolution = new FitnessEvolution(var1);
      List var2 = EngineChartsList.getInstance().getAvailableClasses();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         EngineChart var4 = (EngineChart)var2.get(var3);
         if (var4.getType() == 50) {
            EngineChart var5 = (EngineChart)EngineChartsList.getInstance().createNew(var4.getClassName());
            var5.setProject(var1);
            this.list.add(var5);
         }
      }
   }

   public JSONObject print() {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();
      this.printCharts(this.list, var2);
      this.printCharts(this.global, var2);
      var1.put("charts", var2);
      if (this.project.getName().equals("Builder")) {
         var1.put("fitnessEvolution", this.fitnessEvolution.print());
      }

      return var1;
   }

   private void printCharts(ArrayList<EngineChart> var1, JSONArray var2) {
      for (int var3 = 0; var3 < var1.size(); var3++) {
         EngineChart var4 = (EngineChart)var1.get(var3);
         JSONObject var5 = new JSONObject();
         JSONObject var6 = var4.print();
         if (var6 != null) {
            var5.put("type", var4.getClassName());
            var5.put("data", var6);
            var2.put(var5);
         }
      }
   }

   public void generate() {
      for (int var1 = 0; var1 < this.list.size(); var1++) {
         EngineChart var2 = this.list.get(var1);
         var2.addNextValue();
      }

      this.fitnessEvolution.addNextValue();
   }
}
