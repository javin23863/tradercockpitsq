package com.strategyquant.tradinglib.robustnesstests;

import com.strategyquant.tradinglib.SQStats;
import java.io.IOException;
import java.util.jar.JarOutputStream;
import org.jdom2.Element;

public class RobustnessResults {
   private RobustnessOrdersValues ordersValues;
   private SQStats stats;

   public RobustnessResults() {
   }

   public RobustnessResults(Element var1) {
      this.stats = new SQStats();
      this.stats.setFromXML(var1);
   }

   public RobustnessOrdersValues getOrdersValues() {
      return this.ordersValues;
   }

   public void setOrdersValues(RobustnessOrdersValues var1) {
      this.ordersValues = var1;
   }

   public void setOriginalStats(SQStats var1) {
      this.stats = var1;
   }

   public SQStats getOriginalStats() {
      return this.stats;
   }

   public Element getXml(String var1) {
      Element var2 = new Element(var1);
      var2.addContent(this.stats.getXML());
      return var2;
   }

   public void saveOrderValuesToFile(JarOutputStream var1) throws IOException {
      this.ordersValues.saveOrderValuesToFile(var1);
   }

   public RobustnessResults getClone() throws Exception {
      RobustnessResults var1 = new RobustnessResults();
      if (this.ordersValues != null) {
         var1.ordersValues = this.ordersValues.clone();
      }

      if (this.stats != null) {
         var1.stats = this.stats.getClone();
      }

      return var1;
   }
}
