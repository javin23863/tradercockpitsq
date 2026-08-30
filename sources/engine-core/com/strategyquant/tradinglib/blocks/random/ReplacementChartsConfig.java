package com.strategyquant.tradinglib.blocks.random;

import com.strategyquant.tradinglib.generator.GenerateException;
import java.util.ArrayList;
import org.jdom2.Element;

public class ReplacementChartsConfig extends ArrayList<ReplacementChartConfig> {
   public ReplacementChartsConfig(Element var1, double var2) throws GenerateException {
      int var4 = 0;

      for (Element var6 : var1.getChildren()) {
         this.add(new ReplacementChartConfig(var4, var6, var2));
         var4++;
      }
   }
}
