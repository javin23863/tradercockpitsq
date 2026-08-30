package SQ.EngineCharts;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.EngineChart;
import com.strategyquant.tradinglib.GeneticInfo;
import com.strategyquant.tradinglib.gp.GPFitnessEvolutionData;
import org.json.JSONArray;
import org.json.JSONObject;

public class GeneticEvolutionInfo extends EngineChart {
   public GeneticEvolutionInfo() {
      super(L.tsq("Genetic Evolution info"), 50);
   }

   public JSONObject print() {
      JSONObject var1 = new JSONObject();
      GeneticInfo var2 = this.project.getGeneticInfo();
      JSONArray var3 = new JSONArray();
      if (!var2.isGeneticBuild()) {
         var3.put(new JSONObject().put("name", L.tsq("No data")).put("value", "No genetic evolution running"));
      } else {
         GPFitnessEvolutionData[] var4 = var2.getIslandsEvoData();
         if (var4 != null) {
            for (int var5 = 0; var5 < Math.min(7, var4.length); var5++) {
               if (var4[var5] != null) {
                  var3.put(
                     new JSONObject()
                        .put("name", String.format("Island #%d", var5 + 1))
                        .put("value", String.format("Generation: %d, Population: %d", var4[var5].generation, var4[var5].populationSize))
                  );
               }
            }
         }
      }

      var1.put("items", var3);
      var1.put("type", "rows");
      return var1;
   }

   public void addNextValue() {
   }
}
