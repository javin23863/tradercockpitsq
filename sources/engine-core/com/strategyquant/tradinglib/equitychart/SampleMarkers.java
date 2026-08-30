package com.strategyquant.tradinglib.equitychart;

import com.strategyquant.lib.TimePeriod;
import com.strategyquant.tradinglib.SampleTypes;
import java.util.List;

public class SampleMarkers {
   public void print(EquityChart var1, List<TimePeriod> var2, boolean var3) {
      for (TimePeriod var5 : var2) {
         if (var5.name.contains("OOS")) {
            AreaFill var6 = new AreaFill();
            if (var3) {
               var6.text = SampleTypes.typeToFullString(var5.type);
            }

            var6.color = "#89c284";
            var6.xFrom = var5.from;
            var6.xTo = var5.to;
            var1.addArea(var6);
         } else if (var5.name.contains("ISV")) {
            AreaFill var7 = new AreaFill();
            if (var3) {
               var7.text = SampleTypes.typeToFullString(var5.type);
            }

            var7.color = "#88a9bf";
            var7.xFrom = var5.from;
            var7.xTo = var5.to;
            var1.addArea(var7);
         }
      }
   }
}
