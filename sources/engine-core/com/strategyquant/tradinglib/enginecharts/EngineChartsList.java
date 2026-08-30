package com.strategyquant.tradinglib.enginecharts;

import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.tradinglib.EngineChart;
import java.util.ArrayList;
import java.util.List;

public class EngineChartsList extends CustomClasses<EngineChart> {
   private static EngineChartsList instance;
   public ArrayList<EngineChart> globalCharts = new ArrayList<>();

   public static EngineChartsList getInstance() {
      if (instance == null) {
         instance = new EngineChartsList();
      }

      return instance;
   }

   private EngineChartsList() {
      this.setDirName("EngineCharts");
      this.setExpectedClassType(EngineChart.class);
      this.loadAvailableClasses();
      this.initGlobalCharts();
   }

   private void initGlobalCharts() {
      List var1 = this.getAvailableClasses();

      for (int var2 = 0; var2 < var1.size(); var2++) {
         EngineChart var3 = (EngineChart)var1.get(var2);
         if (var3.getType() == 10) {
            EngineChart var4 = (EngineChart)this.createNew(var3.getClassName());
            this.globalCharts.add(var4);
         }
      }
   }
}
