package SQ.Columns.Databanks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.DatabankColumn;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.StatsTypeCombination;

public class ZProbability extends DatabankColumn {
   public ZProbability() {
      super(L.tsq("ZProbability"), "Decimal2", (byte)1, 0.0, -10.0, 10.0);
      this.setDependencies(new String[]{"ZScore"});
   }

   public double compute(SQStats var1, StatsTypeCombination var2, OrdersList var3, SettingsMap var4, SQStats var5, SQStats var6) throws Exception {
      double var7 = var1.getDouble("ZScore");
      double var9;
      if (var7 <= -10.0) {
         var9 = 99.9;
      } else {
         var9 = 100.0 * this.calculateProbabilityFromZ(var7);
      }

      return this.round2(var9);
   }

   private double calculateProbabilityFromZ(double var1) throws Exception {
      return Math.abs(var1) > 6.0 ? 0.0 : 1.0 - this.poz(var1);
   }

   private double poz(double var1) {
      double var5;
      if (var1 == 0.0) {
         var5 = 0.0;
      } else {
         double var3 = 0.5 * Math.abs(var1);
         if (var3 > 3.0) {
            var5 = 1.0;
         } else if (var3 < 1.0) {
            double var7 = var3 * var3;
            var5 = (
                  (
                           (
                                    (
                                             (
                                                      (((1.24818987E-4 * var7 - 0.001075204047) * var7 + 0.005198775019) * var7 - 0.019198292004) * var7
                                                         + 0.059054035642
                                                   )
                                                   * var7
                                                - 0.151968751364
                                          )
                                          * var7
                                       + 0.319152932694
                                 )
                                 * var7
                              - 0.5319230073
                        )
                        * var7
                     + 0.797884560593
               )
               * var3
               * 2.0;
         } else {
            var3 -= 2.0;
            var5 = (
                     (
                              (
                                       (
                                                (
                                                         (
                                                                  (
                                                                           (
                                                                                    (
                                                                                             (
                                                                                                      (
                                                                                                               (
                                                                                                                        (-4.5255659E-5 * var3 + 1.5252929E-4)
                                                                                                                              * var3
                                                                                                                           - 1.9538132E-5
                                                                                                                     )
                                                                                                                     * var3
                                                                                                                  - 6.76904986E-4
                                                                                                            )
                                                                                                            * var3
                                                                                                         + 0.001390604284
                                                                                                   )
                                                                                                   * var3
                                                                                                - 7.9462082E-4
                                                                                          )
                                                                                          * var3
                                                                                       - 0.002034254874
                                                                                 )
                                                                                 * var3
                                                                              + 0.006549791214
                                                                        )
                                                                        * var3
                                                                     - 0.010557625006
                                                               )
                                                               * var3
                                                            + 0.011630447319
                                                      )
                                                      * var3
                                                   - 0.009279453341
                                             )
                                             * var3
                                          + 0.005353579108
                                    )
                                    * var3
                                 - 0.002141268741
                           )
                           * var3
                        + 5.35310849E-4
                  )
                  * var3
               + 0.999936657524;
         }
      }

      return var1 > 0.0 ? (var5 + 1.0) * 0.5 : (1.0 - var5) * 0.5;
   }
}
