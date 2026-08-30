package com.strategyquant.plugin.Portfolio.impl.Composer;

import org.json.JSONArray;
import org.json.JSONObject;

public class PortfolioComposerChart {
   String labelTitle;
   String labelX;
   String labelY;
   String labelMinimumRiskPortfolio;
   String labelOptimalPortfolio;
   double[] x;
   double[] y;
   String[] weights;
   double bestX;
   double bestY;
   double MinimumRiskPortofolioX;
   double MinimumRiskPortofolioY;
   int decimals = 0;

   public PortfolioComposerChart(int var1) {
      this.x = new double[var1];
      this.y = new double[var1];
      this.weights = new String[var1];
   }

   public String toJsonString() {
      if (this.x == null) {
         return "Not initialized yet.";
      }

      JSONObject var1 = new JSONObject();
      var1.put("labelTitle", this.labelTitle);
      var1.put("labelX", this.labelX);
      var1.put("labelY", this.labelY);
      var1.put("labelMinimumRiskPortfolio", this.labelMinimumRiskPortfolio);
      var1.put("labelOptimalPortfolio", this.labelOptimalPortfolio);
      var1.put("x", new JSONArray(this.x));
      var1.put("y", new JSONArray(this.y));
      var1.put("weights", new JSONArray(this.weights));
      var1.put("bestX", this.bestX);
      var1.put("bestY", this.bestY);
      var1.put("MinRiskX", this.MinimumRiskPortofolioX);
      var1.put("MinRiskY", this.MinimumRiskPortofolioY);
      var1.put("decimals", this.decimals);
      return var1.toString();
   }
}
