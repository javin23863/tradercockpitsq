package com.strategyquant.plugin.Portfolio.impl.Composer;

import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.TaskStoppedException;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import com.strategyquant.tradinglib.results.SpecialValues;
import com.strategyquant.tradinglib.simplegrid.SimpleGridEngine;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PortfolioComposerSimulator {
   private static final Logger Log = LoggerFactory.getLogger(PortfolioComposerSimulator.class);
   private SQProject project;
   private PortfolioComposerSettings settings;
   private PortfolioComposerResultsSender sender;
   private boolean autocomputation;
   private int numberOfSimulations;
   private double jobCounter = 0.0;
   private int lastIndex;
   private StopPauseEngine stopPauseEngine;
   public ArrayList<Double[]> weightCombinations;

   public void start(SQProject var1, PortfolioComposerSettings var2, PortfolioComposerResultsSender var3, StopPauseEngine var4, boolean var5) {
      this.project = var1;
      this.settings = var2;
      this.sender = var3;
      this.stopPauseEngine = var4;
      this.autocomputation = var5;
      this._start();
   }

   private void _start() {
      try {
         this.stopPauseEngine.start();
         ResultsGroup var1 = null;
         if (this.autocomputation) {
            this.updateUIProgress(1, L.t("Preparing simulations, please wait...", new Object[0]));
            this.weightCombinations = this.calculateWeightCombinations();
            this.numberOfSimulations = this.weightCombinations.size();
            ArrayList var2 = this.runOnGrid(false);
            var1 = this.selectOptimalPortfolio(var2);
            var2.clear();
         } else {
            this.updateUIProgress(50, L.t("Calculating portfolio, please wait...", new Object[0]));
            String var10 = "Portfolio-" + System.currentTimeMillis();
            PortfolioComposer var3 = new PortfolioComposer();
            var1 = var3.recalculate(var10, this.settings, this.settings.weights.toArray(new Double[this.settings.weights.size()]));
         }

         this.addBestPortfolioToDatabank(var1);
         this.updateUI(var1);
      } catch (Exception var7) {
         Log.error("Error while computing portfolio.", var7);
         this.updateUI("Error while computing portfolio. " + var7.getMessage());
      } finally {
         this.stopPauseEngine.finish();
      }
   }

   private ArrayList<Double[]> calculateWeightCombinations() {
      ArrayList var1 = new ArrayList();
      SecureRandom var2 = new SecureRandom();
      byte var3 = 20;
      byte[] var4 = var2.generateSeed(var3);
      var2.setSeed(var4);
      byte var5 = 1;
      int var6 = 100 / var5;
      byte var7 = 1;
      int var8 = 0;
      if (var7 * this.settings.weights.size() < 80) {
         var8 = var7 * this.settings.weights.size();
      } else {
         var7 = 0;
      }

      for (int var9 = 0; var9 < this.settings.maxSimulations; var9++) {
         Double[] var10 = new Double[this.settings.weights.size()];
         int var11 = var6 - var8 / var5;

         for (int var12 = 0; var12 < var10.length; var12++) {
            var10[var12] = (double)var7;
         }

         for (int var17 = 0; var17 < this.settings.weights.size() - 1; var17++) {
            int var13 = Math.max(0, var11);
            int var14 = var2.nextInt(var13 + 1);
            Double[] var15 = var10;
            int var16 = var17;
            var15[var16] = var15[var16] + var14 * var5;
            var11 -= var14;
         }

         Double[] var18 = var10;
         int var19 = this.settings.weights.size() - 1;
         var18[var19] = var18[var19] + var11 * var5;
         var1.add(var10);
      }

      return var1;
   }

   private ArrayList<ResultsGroup> runOnGrid(boolean var1) throws Exception {
      final ArrayList var2 = new ArrayList();
      this.lastIndex = 0;
      this.jobCounter = 0.0;
      SimpleGridEngine var3 = new SimpleGridEngine<PortfolioComposerJob, ResultsGroup>(this.stopPauseEngine, null, var1) {
         protected ArrayList<PortfolioComposerJob> createJobsBatch(int var1, GridClient var2x) throws Exception {
            return PortfolioComposerSimulator.this.createBatch(var1, var2x, PortfolioComposerSimulator.this.numberOfSimulations);
         }

         protected void processResult(ResultsGroup var1, JobDetails var2x) throws Exception {
            PortfolioComposerSimulator.this.processJobResult(var1, var2x, var2);
         }

         protected void onError(String var1, Exception var2x) {
            Log.error("PortfolioSimulation Error {}, Exception ", var1, var2x);
         }
      };
      var3.start();
      var3.unregisterListeners();
      return var2;
   }

   private ResultsGroup selectOptimalPortfolio(ArrayList<ResultsGroup> var1) {
      Log.info(String.format("Select the best portfolio from %d.", var1.size()));
      this.numberOfSimulations = 0;

      for (int var2 = 0; var2 < var1.size(); var2++) {
         double var3 = Double.valueOf(((ResultsGroup)var1.get(var2)).specialValues().getString(SpecialValues.PortfolioComposerStdDeviation, null)) * 100.0;
         if (var3 != 0.0) {
            this.numberOfSimulations++;
         }
      }

      PortfolioComposerChart var27 = new PortfolioComposerChart(this.numberOfSimulations);
      var27.labelMinimumRiskPortfolio = "Minimum Risk Portfolio";
      var27.labelOptimalPortfolio = "Optimal Portfolio";
      int var28 = -1;
      int var4 = -1;
      var27.decimals = this.settings.getDecimalsByFitness();
      double var5 = Double.MAX_VALUE;
      double var7 = 0.0;
      int var9 = 0;

      for (int var10 = 0; var10 < var1.size(); var10++) {
         double var11 = Double.valueOf(((ResultsGroup)var1.get(var10)).specialValues().getString(SpecialValues.PortfolioComposerStdDeviation, null)) * 100.0;
         if (var11 != 0.0) {
            var7 += var11;
            double var13 = Double.valueOf(
                  ((ResultsGroup)var1.get(var10)).specialValues().getString(SpecialValues.PortfolioComposerDailyExpectedReturnPct, null)
               )
               * 100.0;
            var27.weights[var9] = "";
            String var15 = ((ResultsGroup)var1.get(var10)).specialValues().getString(SpecialValues.PortfolioComposerWeights, null);
            var15 = var15.replaceAll("[\\[\\]]", "");
            String[] var16 = var15.split(",");

            for (int var17 = 0; var17 < var16.length; var17++) {
               double var18 = Double.parseDouble(var16[var17].trim());
               if (var17 < this.weightCombinations.get(var10).length - 1) {
                  var27.weights[var9] = var27.weights[var9] + SQUtils.d2String(var18, 0) + ",";
               } else {
                  var27.weights[var9] = var27.weights[var9] + SQUtils.d2String(var18, 0);
               }
            }

            if (var11 < var5) {
               var5 = var11;
               var4 = var9;
            }

            var9++;
         }
      }

      if (var9 > 0) {
         var7 /= var9;
      } else {
         var7 = 100.0;
      }

      if (this.settings.PortfolioSelectionType == PortfolioComposerSettings.PortfolioSelectionType.SharpeRatio) {
         var27.labelTitle = "Minimum Variance Frontier";
         var27.labelX = "Standard Deviation of Daily Returns";
         var27.labelY = "Expected Daily Returns";
         double var35 = -999999.0;
         var9 = 0;

         for (int var12 = 0; var12 < var1.size(); var12++) {
            double var45 = Double.valueOf(((ResultsGroup)var1.get(var12)).specialValues().getString(SpecialValues.PortfolioComposerSharpeRatio, null));
            if (var45 != -1.0) {
               double var51 = Double.valueOf(((ResultsGroup)var1.get(var12)).specialValues().getString(SpecialValues.PortfolioComposerStdDeviation, null))
                  * 100.0;
               double var60 = Double.valueOf(
                     ((ResultsGroup)var1.get(var12)).specialValues().getString(SpecialValues.PortfolioComposerDailyExpectedReturnPct, null)
                  )
                  * 100.0;
               BigDecimal var19 = new BigDecimal(var51).setScale(4, RoundingMode.HALF_UP);
               BigDecimal var20 = new BigDecimal(var60).setScale(4, RoundingMode.HALF_UP);
               var27.x[var9] = var19.doubleValue();
               var27.y[var9] = var20.doubleValue();
               if (var45 > var35 && var51 <= var7) {
                  var35 = var45;
                  var28 = var12;
                  var27.bestX = var19.doubleValue();
                  var27.bestY = var20.doubleValue();
               }

               if (var12 == var4) {
                  var27.MinimumRiskPortofolioX = var19.doubleValue();
                  var27.MinimumRiskPortofolioY = var20.doubleValue();
               }

               var9++;
            }
         }
      }

      if (this.settings.PortfolioSelectionType == PortfolioComposerSettings.PortfolioSelectionType.ReturnDrawdownRatio) {
         var27.labelTitle = "Return vs Drawdown";
         var27.labelX = "Net Profit $";
         var27.labelY = "Drawdown $";
         double var36 = -9999999.0;
         var9 = 0;

         for (int var41 = 0; var41 < var1.size(); var41++) {
            double var46 = Double.valueOf(((ResultsGroup)var1.get(var41)).specialValues().getString(SpecialValues.PortfolioComposerSharpeRatio, null));
            if (var46 != -1.0) {
               try {
                  String var56 = "Portfolio";
                  SQStats var52 = ((ResultsGroup)var1.get(var41)).subResult(var56).statsOrNull((byte)0, (byte)10, (byte)127);
                  double var61 = var52.getDouble("NetProfit");
                  double var69 = Math.abs(var52.getDouble("Drawdown"));
                  int var21 = var52.getInt("NumberOfTrades");
                  var61 = Math.round(var61 * 100.0) / 100.0;
                  var69 = Math.round(var69 * 100.0) / 100.0;
                  var27.x[var9] = var61;
                  var27.y[var9] = var69;
                  if (var21 < 10) {
                     continue;
                  }

                  if (var69 == 0.0) {
                     if (var61 == 0.0) {
                        continue;
                     }

                     if (var36 < 10.0) {
                        var36 = 10.0;
                     }
                  }

                  if (SQUtils.safeDivide(var61, var69) > var36) {
                     var36 = SQUtils.round2(SQUtils.safeDivide(var61, var69));
                     var28 = var41;
                     var27.bestX = var61;
                     var27.bestY = var69;
                  }

                  if (var41 == var4) {
                     var27.MinimumRiskPortofolioX = var61;
                     var27.MinimumRiskPortofolioY = var69;
                  }
               } catch (Exception var26) {
                  Log.error("Exc.", var26);
               }

               var9++;
            }
         }
      }

      if (this.settings.PortfolioSelectionType == PortfolioComposerSettings.PortfolioSelectionType.CAGRMaxDrawdownRatio) {
         var27.labelTitle = "Compound Annual Growth Rate vs Drawdown";
         var27.labelX = "CAGR %";
         var27.labelY = "Max Drawdown %";
         double var37 = -9999999.0;
         var9 = 0;

         for (int var42 = 0; var42 < var1.size(); var42++) {
            double var47 = Double.valueOf(((ResultsGroup)var1.get(var42)).specialValues().getString(SpecialValues.PortfolioComposerSharpeRatio, null));
            if (var47 != -1.0) {
               try {
                  String var57 = "Portfolio";
                  SQStats var53 = ((ResultsGroup)var1.get(var42)).subResult(var57).statsOrNull((byte)0, (byte)10, (byte)127);
                  double var63 = var53.getDouble("CAGR");
                  double var71 = var53.getDouble("DrawdownPct");
                  int var77 = var53.getInt("NumberOfTrades");
                  var63 = Math.round(var63 * 100.0) / 100.0;
                  var71 = Math.round(var71 * 100.0) / 100.0;
                  var27.x[var9] = var63;
                  var27.y[var9] = var71;
                  if (var77 < 10) {
                     continue;
                  }

                  if (var71 == 0.0) {
                     if (var63 == 0.0) {
                        continue;
                     }

                     if (var37 < 10.0) {
                        var37 = 10.0;
                     }
                  }

                  if (SQUtils.safeDivide(var63, var71) > var37) {
                     var37 = SQUtils.round2(SQUtils.safeDivide(var63, var71));
                     var28 = var42;
                     var27.bestX = var63;
                     var27.bestY = var71;
                  }

                  if (var42 == var4) {
                     var27.MinimumRiskPortofolioX = var63;
                     var27.MinimumRiskPortofolioY = var71;
                  }
               } catch (Exception var25) {
                  Log.error("Exc.", var25);
               }

               var9++;
            }
         }
      }

      if (this.settings.PortfolioSelectionType == PortfolioComposerSettings.PortfolioSelectionType.CAGRMeanDrawdownRatio) {
         var27.labelTitle = "Compound Annual Growth Rate vs Average Drawdown";
         var27.labelX = "CAGR %";
         var27.labelY = "Avg Drawdown %";
         double var38 = -9999999.0;
         var9 = 0;

         for (int var43 = 0; var43 < var1.size(); var43++) {
            double var48 = Double.valueOf(((ResultsGroup)var1.get(var43)).specialValues().getString(SpecialValues.PortfolioComposerSharpeRatio, null));
            if (var48 != -1.0) {
               try {
                  String var58 = "Portfolio";
                  SQStats var54 = ((ResultsGroup)var1.get(var43)).subResult(var58).statsOrNull((byte)0, (byte)10, (byte)127);
                  double var65 = var54.getDouble("CAGR");
                  double var73 = Math.abs(var54.getDouble("AvgPctDrawdown"));
                  int var78 = var54.getInt("NumberOfTrades");
                  var65 = Math.round(var65 * 100.0) / 100.0;
                  var73 = Math.round(var73 * 100.0) / 100.0;
                  var27.x[var9] = var65;
                  var27.y[var9] = var73;
                  if (var78 < 10) {
                     continue;
                  }

                  if (var73 == 0.0) {
                     if (var65 == 0.0) {
                        continue;
                     }

                     if (var38 < 10.0) {
                        var38 = 10.0;
                     }
                  }

                  if (SQUtils.safeDivide(var65, var73) > var38) {
                     var38 = SQUtils.round2(SQUtils.safeDivide(var65, var73));
                     var28 = var43;
                     var27.bestX = var65;
                     var27.bestY = var73;
                  }

                  if (var43 == var4) {
                     var27.MinimumRiskPortofolioX = var65;
                     var27.MinimumRiskPortofolioY = var73;
                  }
               } catch (Exception var24) {
                  Log.error("Exc.", var24);
               }

               var9++;
            }
         }
      }

      if (this.settings.PortfolioSelectionType == PortfolioComposerSettings.PortfolioSelectionType.NetProfit) {
         var27.labelTitle = "Return vs Drawdown";
         var27.labelX = "Net Profit $";
         var27.labelY = "Drawdown $";
         double var39 = -9999999.0;
         var9 = 0;

         for (int var44 = 0; var44 < var1.size(); var44++) {
            double var49 = Double.valueOf(((ResultsGroup)var1.get(var44)).specialValues().getString(SpecialValues.PortfolioComposerSharpeRatio, null));
            if (var49 != -1.0) {
               try {
                  String var59 = "Portfolio";
                  SQStats var55 = ((ResultsGroup)var1.get(var44)).subResult(var59).statsOrNull((byte)0, (byte)10, (byte)127);
                  double var67 = var55.getDouble("NetProfit");
                  double var75 = Math.abs(var55.getDouble("Drawdown"));
                  var67 = Math.round(var67 * 100.0) / 100.0;
                  var75 = Math.round(var75 * 100.0) / 100.0;
                  var27.x[var9] = var67;
                  var27.y[var9] = var75;
                  if (var67 > var39) {
                     var39 = var67;
                     var28 = var44;
                     var27.bestX = var67;
                     var27.bestY = var75;
                  }

                  if (var44 == var4) {
                     var27.MinimumRiskPortofolioX = var67;
                     var27.MinimumRiskPortofolioY = var75;
                  }
               } catch (Exception var23) {
                  Log.error("Exc.", var23);
               }

               var9++;
            }
         }
      }

      if (var28 == -1) {
         var28 = 0;
         var27.bestX = 0.0;
         var27.bestY = 0.0;
      }

      ResultsGroup var40 = (ResultsGroup)var1.get(var28);
      var40.specialValues().set(SpecialValues.PortfolioComposerChart, var27.toJsonString());
      return var40;
   }

   protected void processJobResult(ResultsGroup var1, JobDetails var2, ArrayList<ResultsGroup> var3) {
      String var4 = var2.getJobID();
      if (var2.isSuccess()) {
         var3.add(var1);
      } else {
         var2.getException();
         Log.error(String.format("Simulation %s failed, error: %s", var4, var2.getException()));
      }

      this.jobCounter++;
      int var5 = Double.valueOf(this.jobCounter / this.numberOfSimulations * 100.0).intValue();
      this.updateUIProgress(var5, L.t("Finished simulation %d/%d", new Object[]{(int)this.jobCounter, this.numberOfSimulations}));
   }

   protected ArrayList<PortfolioComposerJob> createBatch(int var1, GridClient var2, long var3) throws Exception {
      ArrayList var5 = new ArrayList();
      int var6 = 0;

      for (int var7 = this.lastIndex; var7 < this.lastIndex + var1 && var7 < var3; var7++) {
         if (this.stopPauseEngine.isStopped()) {
            throw new TaskStoppedException();
         }

         String var8 = String.format("Portfolio-%d", var7);
         PortfolioComposerJob var9 = new PortfolioComposerJob("portfolio-composer-simulation-" + var7, this.getJobParams(var7, var8));
         var5.add(var9);
         var6++;
      }

      this.lastIndex += var6;
      return var5.size() == 0 ? null : var5;
   }

   private Map<String, Serializable> getJobParams(int var1, String var2) {
      HashMap var3 = new HashMap();
      var3.put("StrategyName", var2);
      var3.put("OptimizationSettings", this.settings);
      var3.put("Weights", (Serializable)this.weightCombinations.get(var1));
      return var3;
   }

   private void addBestPortfolioToDatabank(ResultsGroup var1) throws Exception {
      try {
         Databank var2 = (Databank)this.project.getDatabanks().get("Portfolios");
         if (var2 == null) {
            this.project.getDatabanks().add("Portfolios");
         }

         var2 = (Databank)this.project.getDatabanks().get("Portfolios");
         var2.clearRecords(true, true, "PortfolioComposer output");
         var2.add(var1, false);
      } catch (Exception var3) {
         throw new Exception("Failed to add the best portfolio to the output databank.", var3);
      }
   }

   private void updateUIProgress(int var1, String var2) {
      this.sender.progress(var1, var2);
   }

   private void updateUI(String var1) {
      this.updateUI(null, var1);
   }

   private void updateUI(ResultsGroup var1) {
      this.updateUI(var1, null);
   }

   private void updateUI(ResultsGroup var1, String var2) {
      JSONObject var3 = new JSONObject();
      if (var1 != null) {
         var3.put("strategyName", var1.getName());
         var3.put("weights", var1.specialValues().getString(SpecialValues.PortfolioComposerWeights, null));
      }

      if (var2 != null) {
         var3.put("error", var2);
      }

      var3.put("progress", 100);
      this.sender.sendData(var3);
   }
}
