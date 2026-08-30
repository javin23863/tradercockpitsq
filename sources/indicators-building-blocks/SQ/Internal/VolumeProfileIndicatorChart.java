package SQ.Internal;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.tradinglib.ChartData;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import javax.imageio.ImageIO;

public abstract class VolumeProfileIndicatorChart extends AbstractChart {
   protected static final String DEFAULT_DARK_BG_COLOR = "#1a1a2e";
   protected static final String DEFAULT_DARK_PLOT_COLOR = "#16213e";
   protected static final String DEFAULT_LIGHT_BG_COLOR = "#e0e0e0";
   protected static final String DEFAULT_LIGHT_PLOT_COLOR = "#faf5eb";
   protected static final int DEFAULT_DARK_CANDLE_OPACITY = 100;
   protected static final int DEFAULT_LIGHT_CANDLE_OPACITY = 60;
   protected static final int DEFAULT_DARK_PROFILE_OPACITY = 50;
   protected static final int DEFAULT_LIGHT_PROFILE_OPACITY = 80;
   protected static final int DEFAULT_DELTA_FONT_SIZE = 8;
   protected static final int DEFAULT_CHART_HEIGHT = 800;
   protected static final int DEFAULT_SESSION_WIDTH = 400;
   protected double prevPOC = 0.0;
   protected double prevVAH = 0.0;
   protected double prevVAL = 0.0;
   protected double[] prevHVN = new double[5];
   protected double[] prevLVN = new double[5];
   protected double prevVPOC = 0.0;
   protected double prevVVAH = 0.0;
   protected double prevVVAL = 0.0;
   protected double prevBullPOC = 0.0;
   protected double prevBearPOC = 0.0;
   protected double[] bullVolumeBins;
   protected double[] bearVolumeBins;
   protected double prevDelta = 0.0;
   protected double prevDeltaPOC = 0.0;
   protected double prevDeltaVA = 0.0;
   protected double prevRange = 0.0;
   protected double prevPOCPctUp = 0.0;
   protected double prevPOCPctDown = 0.0;
   protected double prevPrevPOC = 0.0;
   protected double prevPrevVAMid = 0.0;
   protected int zzDirection = 0;
   protected double zzPivotHigh = 0.0;
   protected double zzPivotLow = Double.MAX_VALUE;
   protected long zzPivotHighTime = 0L;
   protected long zzPivotLowTime = 0L;
   protected long zzSessionStart = 0L;
   protected long zzLastPivotTime = 0L;
   protected boolean zzPivotConfirmed = false;
   protected double zzLastPivotPrice = 0.0;
   protected int zzLastPivotDir = 0;
   protected double[] volumeBins;
   protected double[] histPOC;
   protected double[] histVAH;
   protected double[] histVAL;
   protected double[][] histVolumeBins;
   protected double[][] histBullBins;
   protected double[][] histBearBins;
   protected double[][] histHVN;
   protected double[][] histLVN;
   protected double[] histPivotPrice;
   protected int[] histPivotDir;
   protected String[] histSessionLabel;

   @Override
   protected void ensureArrays() {
      int var1 = this.cfgBinSizeMode() == 2 ? 2000 : this.cfgProfileRows();
      if (this.volumeBins == null || this.volumeBins.length < var1) {
         this.volumeBins = new double[var1];
      }

      if (this.clusterBins == null || this.clusterBins.length < var1) {
         this.clusterBins = new double[var1];
      }

      if (this.bullVolumeBins == null || this.bullVolumeBins.length < var1) {
         this.bullVolumeBins = new double[var1];
      }

      if (this.bearVolumeBins == null || this.bearVolumeBins.length < var1) {
         this.bearVolumeBins = new double[var1];
      }
   }

   protected void ensureHistory() {
      byte var1 = 20;
      int var2 = this.cfgBinSizeMode() == 2 ? 2000 : this.cfgProfileRows();
      if (this.histSessionStart == null || this.histSessionStart.length < var1) {
         this.histSessionStart = new long[var1];
         this.histSessionLabel = new String[var1];
         this.histSessionEnd = new long[var1];
         this.histPOC = new double[var1];
         this.histVAH = new double[var1];
         this.histVAL = new double[var1];
         this.histIBH = new double[var1];
         this.histIBL = new double[var1];
         this.histSessionHigh = new double[var1];
         this.histSessionLow = new double[var1];
         this.histVolumeBins = new double[var1][var2];
         this.histBullBins = new double[var1][var2];
         this.histBearBins = new double[var1][var2];
         this.histHVN = new double[var1][5];
         this.histLVN = new double[var1][5];
         this.histNumBins = new int[var1];
         this.histTotalVolume = new double[var1];
         this.histBullVolume = new double[var1];
         this.histBearVolume = new double[var1];
         this.histPivotPrice = new double[var1];
         this.histPivotDir = new int[var1];
         this.historyCount = 0;
      }

      if (this.histBullBins == null || this.histBullBins.length < var1) {
         this.histBullBins = new double[var1][var2];
      }

      if (this.histBearBins == null || this.histBearBins.length < var1) {
         this.histBearBins = new double[var1][var2];
      }
   }

   protected void growHistory() {
   }

   protected void pushSessionHistory(double var1, double var3) {
      this.ensureHistory();
      int var5 = this.lastNumBins;
      int var6 = -1;

      for (int var7 = this.historyCount - 1; var7 >= Math.max(0, this.historyCount - 8); var7--) {
         if (this.histSessionStart[var7] == this.prevSessionStart) {
            var6 = var7;
            break;
         }
      }

      if (var6 >= 0) {
         int var11 = var6;
         this.histSessionEnd[var11] = this.prevSessionEnd;
         if (this.cfgCurrentSessionLabel() != null) {
            this.histSessionLabel[var11] = this.cfgCurrentSessionLabel();
         }

         this.histPOC[var11] = this.prevPOC;
         this.histVAH[var11] = this.prevVAH;
         this.histVAL[var11] = this.prevVAL;
         this.histIBH[var11] = this.prevIBH;
         this.histIBL[var11] = this.prevIBL;
         this.histSessionHigh[var11] = var1;
         this.histSessionLow[var11] = var3;
         this.histNumBins[var11] = var5;
         double[] var12 = this.cfgEnableVCP() && this.clusterBins != null && this.clusterBins.length >= var5 ? this.clusterBins : this.volumeBins;
         System.arraycopy(var12, 0, this.histVolumeBins[var11], 0, var5);
         System.arraycopy(this.bullVolumeBins, 0, this.histBullBins[var11], 0, var5);
         System.arraycopy(this.bearVolumeBins, 0, this.histBearBins[var11], 0, var5);
         System.arraycopy(this.prevHVN, 0, this.histHVN[var11], 0, 5);
         System.arraycopy(this.prevLVN, 0, this.histLVN[var11], 0, 5);
         this.histTotalVolume[var11] = this.prevTotalVolume;
         this.histBullVolume[var11] = this.prevTotalBullVolume;
         this.histBearVolume[var11] = this.prevTotalBearVolume;
         this.histPivotPrice[var11] = this.zzLastPivotPrice;
         this.histPivotDir[var11] = this.zzLastPivotDir;
      } else {
         if (this.historyCount >= this.histSessionStart.length) {
            int var9 = this.histSessionStart.length;
            System.arraycopy(this.histSessionStart, 1, this.histSessionStart, 0, var9 - 1);
            System.arraycopy(this.histSessionEnd, 1, this.histSessionEnd, 0, var9 - 1);
            System.arraycopy(this.histSessionLabel, 1, this.histSessionLabel, 0, var9 - 1);
            System.arraycopy(this.histPOC, 1, this.histPOC, 0, var9 - 1);
            System.arraycopy(this.histVAH, 1, this.histVAH, 0, var9 - 1);
            System.arraycopy(this.histVAL, 1, this.histVAL, 0, var9 - 1);
            System.arraycopy(this.histIBH, 1, this.histIBH, 0, var9 - 1);
            System.arraycopy(this.histIBL, 1, this.histIBL, 0, var9 - 1);
            System.arraycopy(this.histSessionHigh, 1, this.histSessionHigh, 0, var9 - 1);
            System.arraycopy(this.histSessionLow, 1, this.histSessionLow, 0, var9 - 1);
            System.arraycopy(this.histNumBins, 1, this.histNumBins, 0, var9 - 1);
            System.arraycopy(this.histTotalVolume, 1, this.histTotalVolume, 0, var9 - 1);
            System.arraycopy(this.histBullVolume, 1, this.histBullVolume, 0, var9 - 1);
            System.arraycopy(this.histBearVolume, 1, this.histBearVolume, 0, var9 - 1);
            System.arraycopy(this.histPivotPrice, 1, this.histPivotPrice, 0, var9 - 1);
            System.arraycopy(this.histPivotDir, 1, this.histPivotDir, 0, var9 - 1);
            System.arraycopy(this.histVolumeBins, 1, this.histVolumeBins, 0, var9 - 1);
            System.arraycopy(this.histBullBins, 1, this.histBullBins, 0, var9 - 1);
            System.arraycopy(this.histBearBins, 1, this.histBearBins, 0, var9 - 1);
            System.arraycopy(this.histHVN, 1, this.histHVN, 0, var9 - 1);
            System.arraycopy(this.histLVN, 1, this.histLVN, 0, var9 - 1);
            this.historyCount = var9 - 1;
         }

         int var10 = this.historyCount;
         this.histSessionStart[var10] = this.prevSessionStart;
         this.histSessionEnd[var10] = this.prevSessionEnd;
         this.histSessionLabel[var10] = this.cfgCurrentSessionLabel();
         this.histPOC[var10] = this.prevPOC;
         this.histVAH[var10] = this.prevVAH;
         this.histVAL[var10] = this.prevVAL;
         this.histIBH[var10] = this.prevIBH;
         this.histIBL[var10] = this.prevIBL;
         this.histSessionHigh[var10] = var1;
         this.histSessionLow[var10] = var3;
         this.histNumBins[var10] = var5;
         double[] var8 = this.cfgEnableVCP() && this.clusterBins != null && this.clusterBins.length >= var5 ? this.clusterBins : this.volumeBins;
         System.arraycopy(var8, 0, this.histVolumeBins[var10], 0, var5);
         System.arraycopy(this.bullVolumeBins, 0, this.histBullBins[var10], 0, var5);
         System.arraycopy(this.bearVolumeBins, 0, this.histBearBins[var10], 0, var5);
         System.arraycopy(this.prevHVN, 0, this.histHVN[var10], 0, 5);
         System.arraycopy(this.prevLVN, 0, this.histLVN[var10], 0, 5);
         this.histTotalVolume[var10] = this.prevTotalVolume;
         this.histBullVolume[var10] = this.prevTotalBullVolume;
         this.histBearVolume[var10] = this.prevTotalBearVolume;
         this.histPivotPrice[var10] = this.zzLastPivotPrice;
         this.histPivotDir[var10] = this.zzLastPivotDir;
         this.historyCount++;
      }
   }

   @Override
   protected void OnDeinit() throws TradingException {
      super.OnDeinit();
      this.volumeBins = null;
      this.clusterBins = null;
      this.bullVolumeBins = null;
      this.bearVolumeBins = null;
      this.histVolumeBins = null;
      this.histBullBins = null;
      this.histBearBins = null;
      this.histSessionStart = null;
      this.histSessionEnd = null;
      this.histSessionLabel = null;
      this.histPOC = null;
      this.histVAH = null;
      this.histVAL = null;
      this.histIBH = null;
      this.histIBL = null;
      this.histSessionHigh = null;
      this.histSessionLow = null;
      this.histHVN = null;
      this.histLVN = null;
      this.histNumBins = null;
      this.histTotalVolume = null;
      this.histBullVolume = null;
      this.histBearVolume = null;
      this.histPivotPrice = null;
      this.histPivotDir = null;
      this.prevHVN = null;
      this.prevLVN = null;
   }

   protected void calculateVolumeProfile() throws TradingException {
      if (this.cfgChart() != null) {
         double var1 = Double.MIN_VALUE;
         double var3 = Double.MAX_VALUE;
         int var5 = 0;
         double var6 = 0.0;
         long var8 = this.prevSessionStart + this.getIBPeriodMillis();
         double var10 = Double.MIN_VALUE;
         double var12 = Double.MAX_VALUE;
         int var14 = 0;

         try {
            while (true) {
               long var15 = this.cfgChart().Time(var14);
               if (var15 < this.prevSessionStart) {
                  break;
               }

               if (var15 >= this.prevSessionStart && var15 < this.prevSessionEnd) {
                  if (this.isSunday(var15)) {
                     var14++;
                     continue;
                  }

                  double var17 = this.cfgChart().High(var14);
                  double var19 = this.cfgChart().Low(var14);
                  var1 = Math.max(var1, var17);
                  var3 = Math.min(var3, var19);
                  var5++;
                  var6 += this.cfgChart().Volume(var14);
                  if (var15 < var8) {
                     var10 = Math.max(var10, var17);
                     var12 = Math.min(var12, var19);
                  }
               }

               var14++;
            }
         } catch (Exception var47) {
         }

         if (var5 != 0 && !(var1 <= var3)) {
            if (var10 > var12) {
               this.prevIBH = var10;
               this.prevIBL = var12;
            }

            double var49 = var1 - var3;
            double var50 = this.cfgChart().getInstrumentInfo().tickStep;
            double var20;
            int var52;
            if (this.cfgBinSizeMode() == 2) {
               var20 = this.cfgTicksPerBin() * var50;
               var52 = (int)Math.ceil(var49 / var20);
               var52 = Math.max(1, Math.min(var52, 2000));
            } else {
               var52 = this.cfgProfileRows();
               var20 = var49 / var52;
            }

            this.lastNumBins = var52;
            if (this.volumeBins == null || this.volumeBins.length < var52) {
               this.volumeBins = new double[var52];
            }

            for (int var22 = 0; var22 < var52; var22++) {
               this.volumeBins[var22] = 0.0;
            }

            if (this.bullVolumeBins == null || this.bullVolumeBins.length < var52) {
               this.bullVolumeBins = new double[var52];
            }

            if (this.bearVolumeBins == null || this.bearVolumeBins.length < var52) {
               this.bearVolumeBins = new double[var52];
            }

            for (int var53 = 0; var53 < var52; var53++) {
               this.bullVolumeBins[var53] = 0.0;
               this.bearVolumeBins[var53] = 0.0;
            }

            double var54 = 0.0;
            double var24 = 0.0;
            double var26 = 0.0;
            var14 = 0;

            try {
               while (true) {
                  long var28 = this.cfgChart().Time(var14);
                  if (var28 < this.prevSessionStart) {
                     break;
                  }

                  if (var28 >= this.prevSessionStart && var28 < this.prevSessionEnd) {
                     if (this.isSunday(var28)) {
                        var14++;
                        continue;
                     }

                     double var30 = this.cfgChart().Close(var14);
                     double var32 = this.cfgChart().Open(var14);
                     double var34 = this.cfgChart().High(var14);
                     double var36 = this.cfgChart().Low(var14);
                     double var38 = this.cfgChart().Volume(var14);
                     int var40 = (int)((var34 - var3) * 1000.0 / (var20 * 1000.0));
                     int var41 = (int)((var36 - var3) * 1000.0 / (var20 * 1000.0));
                     var40 = Math.max(0, Math.min(var52 - 1, var40));
                     var41 = Math.max(0, Math.min(var52 - 1, var41));
                     double var42 = var38 / (var40 - var41 + 1);
                     boolean var44 = var30 >= var32;

                     for (int var45 = var41; var45 <= var40; var45++) {
                        this.volumeBins[var45] = this.volumeBins[var45] + var42;
                        if (var44) {
                           this.bullVolumeBins[var45] = this.bullVolumeBins[var45] + var42;
                        } else {
                           this.bearVolumeBins[var45] = this.bearVolumeBins[var45] + var42;
                        }
                     }

                     var54 += var38;
                     if (var44) {
                        var24 += var38;
                     } else {
                        var26 += var38;
                     }
                  }

                  var14++;
               }
            } catch (Exception var46) {
            }

            int var55 = 0;
            double var29 = this.volumeBins[0];

            for (int var31 = 1; var31 < var52; var31++) {
               if (this.volumeBins[var31] > var29) {
                  var29 = this.volumeBins[var31];
                  var55 = var31;
               }
            }

            this.prevPOC = var3 + (var55 + 0.5) * var20;
            this.calculateValueArea(var55, var54, var20, var3, var52);
            this.findHVNs(var55, var20, var3, var52);
            if (this.cfgEnableLVN()) {
               this.findLVNs(var55, var20, var3, var52);
            }

            if (this.cfgEnableVCP()) {
               this.applyClusterEnhancement(var52, var20, var3, var54);
            } else {
               this.prevVPOC = this.prevPOC;
               this.prevVVAH = this.prevVAH;
               this.prevVVAL = this.prevVAL;
            }

            int var56 = 0;
            double var57 = this.bullVolumeBins[0];
            int var58 = 0;
            double var35 = this.bearVolumeBins[0];

            for (int var37 = 1; var37 < var52; var37++) {
               if (this.bullVolumeBins[var37] > var57) {
                  var57 = this.bullVolumeBins[var37];
                  var56 = var37;
               }

               if (this.bearVolumeBins[var37] > var35) {
                  var35 = this.bearVolumeBins[var37];
                  var58 = var37;
               }
            }

            this.prevBullPOC = var57 > 0.0 ? var3 + (var56 + 0.5) * var20 : 0.0;
            this.prevBearPOC = var35 > 0.0 ? var3 + (var58 + 0.5) * var20 : 0.0;
            this.prevTotalVolume = var54;
            this.prevTotalBullVolume = var24;
            this.prevTotalBearVolume = var26;
            this.prevDelta = var24 - var26;
            double var59 = (this.prevVAH + this.prevVAL) / 2.0;
            this.prevDeltaPOC = this.prevPrevPOC != 0.0 ? this.prevPOC - this.prevPrevPOC : 0.0;
            this.prevDeltaVA = this.prevPrevVAMid != 0.0 ? var59 - this.prevPrevVAMid : 0.0;
            this.prevRange = var1 - var3;
            if (this.prevRange > 0.0) {
               this.prevPOCPctUp = (var1 - this.prevPOC) / this.prevRange * 100.0;
               this.prevPOCPctDown = (this.prevPOC - var3) / this.prevRange * 100.0;
            } else {
               this.prevPOCPctUp = 0.0;
               this.prevPOCPctDown = 0.0;
            }

            this.prevPrevPOC = this.prevPOC;
            this.prevPrevVAMid = var59;
            if (this.cfgStoreChartData()) {
               this.pushSessionHistory(var1, var3);
            }
         }
      }
   }

   protected void calculateValueArea(int var1, double var2, double var4, double var6, int var8) {
      double var9 = var2 * (this.cfgValueAreaPct() / 100.0);
      double var11 = this.volumeBins[var1];
      int var13 = var1;
      int var14 = var1;

      while (var11 < var9) {
         boolean var15 = var13 + 1 < var8;
         boolean var16 = var14 - 1 >= 0;
         if (!var15 && !var16) {
            break;
         }

         double var17 = var15 ? this.volumeBins[var13 + 1] : -1.0;
         double var19 = var16 ? this.volumeBins[var14 - 1] : -1.0;
         if (var17 >= var19) {
            var11 += this.volumeBins[++var13];
         } else {
            var11 += this.volumeBins[--var14];
         }
      }

      this.prevVAL = var6 + var14 * var4;
      this.prevVAH = var6 + (var13 + 1) * var4;
   }

   protected void findHVNs(int var1, double var2, double var4, int var6) {
      for (int var7 = 0; var7 < 5; var7++) {
         this.prevHVN[var7] = 0.0;
      }

      double var30 = 0.0;

      for (int var9 = 0; var9 < var6; var9++) {
         if (this.volumeBins[var9] > var30) {
            var30 = this.volumeBins[var9];
         }
      }

      if (!(var30 <= 0.0)) {
         double var31 = var30 * this.cfgHvnThresholdPct() / 100.0;
         int var11 = Math.min(this.cfgHvnCount(), 5);
         int[] var12 = new int[var6];
         double[] var13 = new double[var6];
         int var14 = 0;

         for (int var15 = 0; var15 < var6; var15++) {
            if (var15 != var1 && !(this.volumeBins[var15] < var31)) {
               double var16 = var15 > 0 ? this.volumeBins[var15 - 1] : -1.0;
               double var18 = var15 < var6 - 1 ? this.volumeBins[var15 + 1] : -1.0;
               if (this.volumeBins[var15] > var16 && this.volumeBins[var15] > var18) {
                  var12[var14] = var15;
                  var13[var14] = this.volumeBins[var15];
                  var14++;
               }
            }
         }

         for (int var32 = 0; var32 < var14 - 1; var32++) {
            int var34 = var32;

            for (int var17 = var32 + 1; var17 < var14; var17++) {
               if (var13[var17] > var13[var34]) {
                  var34 = var17;
               }
            }

            if (var34 != var32) {
               double var36 = var13[var32];
               var13[var32] = var13[var34];
               var13[var34] = var36;
               int var19 = var12[var32];
               var12[var32] = var12[var34];
               var12[var34] = var19;
            }
         }

         int[] var33 = new int[var11];
         int var35 = 0;

         for (int var37 = 0; var37 < var14 && var35 < var11; var37++) {
            int var38 = var12[var37];
            double var39 = var13[var37];
            boolean var21 = true;
            int var22 = 0;

            while (true) {
               if (var22 < var35) {
                  int var23 = var33[var22];
                  int var24 = Math.min(var38, var23) + 1;
                  int var25 = Math.max(var38, var23);
                  double var26 = Double.MAX_VALUE;

                  for (int var28 = var24; var28 < var25; var28++) {
                     if (this.volumeBins[var28] < var26) {
                        var26 = this.volumeBins[var28];
                     }
                  }

                  double var40 = Math.min(var39, this.volumeBins[var23]);
                  if (!(var26 >= var40 * 0.5)) {
                     var22++;
                     continue;
                  }

                  var21 = false;
               }

               if (var21) {
                  var33[var35] = var38;
                  this.prevHVN[var35] = var4 + (var38 + 0.5) * var2;
                  var35++;
               }
               break;
            }
         }
      }
   }

   protected void findLVNs(int var1, double var2, double var4, int var6) {
      for (int var7 = 0; var7 < 5; var7++) {
         this.prevLVN[var7] = 0.0;
      }

      double var20 = 0.0;

      for (int var9 = 0; var9 < var6; var9++) {
         if (this.volumeBins[var9] > var20) {
            var20 = this.volumeBins[var9];
         }
      }

      if (!(var20 <= 0.0)) {
         double var21 = var20 * this.cfgLvnThresholdPct() / 100.0;
         int var11 = Math.min(this.cfgHvnCount(), 5);
         int[] var12 = new int[var6];
         double[] var13 = new double[var6];
         int var14 = 0;

         for (int var15 = 1; var15 < var6 - 1; var15++) {
            if (!(this.volumeBins[var15] > var21) && this.volumeBins[var15] < this.volumeBins[var15 - 1] && this.volumeBins[var15] < this.volumeBins[var15 + 1]
               )
             {
               var12[var14] = var15;
               var13[var14] = this.volumeBins[var15];
               var14++;
            }
         }

         for (int var22 = 0; var22 < var14 - 1; var22++) {
            int var16 = var22;

            for (int var17 = var22 + 1; var17 < var14; var17++) {
               if (var13[var17] < var13[var16]) {
                  var16 = var17;
               }
            }

            if (var16 != var22) {
               double var25 = var13[var22];
               var13[var22] = var13[var16];
               var13[var16] = var25;
               int var19 = var12[var22];
               var12[var22] = var12[var16];
               var12[var16] = var19;
            }
         }

         int var23 = Math.min(var11, var14);

         for (int var24 = 0; var24 < var23; var24++) {
            this.prevLVN[var24] = var4 + (var12[var24] + 0.5) * var2;
         }
      }
   }

   protected void exportVolumeProfilePNG_Dense(long var1, long var3, double var5, double var7, double var9, int var11) {
      String var12 = this.resolveExportFolder();
      new File(var12).mkdirs();
      String var13 = this.formatForFilename(var1);
      String var14 = this.formatForFilename(var3);
      String var15 = "";

      try {
         var15 = this.cfgChart().Symbol;
      } catch (Exception var59) {
      }

      String[] var16 = new String[]{
         "",
         "Previous Day",
         "Previous Week",
         "Previous Month",
         "Previous Year",
         "Actual Day",
         "Actual Week",
         "Actual Month",
         "Actual Year",
         "Previous Swing",
         "Actual Swing"
      };
      String var17 = this.cfgSessionLabel() != null
         ? this.cfgSessionLabel()
         : (this.cfgSessionType() >= 1 && this.cfgSessionType() < var16.length ? var16[this.cfgSessionType()] : "Session");
      String var18 = "";

      try {
         var18 = this.cfgChart().Timeframe;
      } catch (Exception var58) {
      }

      String var19 = this.cfgBinSizeMode() == 2 ? "Fixed Tick" : "Range";
      String var20 = (var18.isEmpty() ? "" : " | TF:" + var18) + " | Bins:" + var19 + (this.cfgEnableVCP() ? " | VolumeCluster" : "");
      String var21 = var15.isEmpty() ? "" : var15.replaceAll("[^A-Za-z0-9_-]", "") + "_";
      String var22 = var17.replaceAll("\\s+", "");
      String var23 = var12 + File.separator + "VolumeProfile_" + var21 + var22 + "_" + var13 + "_" + var14 + ".png";
      double var24 = 0.0;

      for (int var26 = 0; var26 < this.lastNumBins; var26++) {
         double var27 = this.cfgEnableVCP() && this.clusterBins != null && this.clusterBins.length >= this.lastNumBins
            ? this.clusterBins[var26]
            : this.volumeBins[var26];
         if (var27 > var24) {
            var24 = var27;
         }
      }

      if (!(var24 <= 0.0)) {
         int var62 = var7 > 0.0 ? (int)Math.floor((this.prevVAL - var5) / var7) : 0;
         int var64 = var7 > 0.0 ? (int)Math.floor((this.prevVAH - var5) / var7) : this.lastNumBins - 1;
         var62 = clampInt(var62, 0, this.lastNumBins - 1);
         var64 = clampInt(var64, 0, this.lastNumBins - 1);
         int var28 = Math.min(var62, var64);
         int var29 = Math.max(var62, var64);
         BufferedImage var30 = new BufferedImage(1600, 1000, 2);
         Graphics2D var31 = var30.createGraphics();

         try {
            var31.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            var31.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            var31.setColor(Color.WHITE);
            var31.fillRect(0, 0, 1600, 1000);
            var31.setColor(Color.BLACK);
            var31.setFont(new Font("SansSerif", 1, 18));
            SimpleDateFormat var32 = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.US);
            String var33 = "StrategyQuantX - Volume Profile"
               + (var15.isEmpty() ? "" : " - " + var15)
               + " | "
               + var17
               + var20
               + "  "
               + var32.format(new Date(var1))
               + " -> "
               + var32.format(new Date(var3));
            var31.drawString(var33, 220, 30);
            byte var34 = 30;
            int var35 = 1560 - var34;
            byte var36 = 6;
            var31.setColor(new Color(65, 129, 237));
            var31.fillRoundRect(var35, var36, var34, var34, 8, 8);
            var31.setColor(Color.WHITE);
            var31.setFont(new Font("SansSerif", 1, 20));
            FontMetrics var37 = var31.getFontMetrics();
            int var38 = var37.stringWidth("Q");
            var31.drawString("Q", var35 + (var34 - var38) / 2, var36 + var34 - 8);
            var31.setColor(new Color(65, 129, 237));
            var31.setFont(new Font("SansSerif", 1, 16));
            FontMetrics var39 = var31.getFontMetrics();
            String var40 = "strategyquant.com";
            int var41 = var39.stringWidth(var40);
            var31.drawString(var40, var35 - 8 - var41, var36 + var34 / 2 + 5);
            var31.setColor(Color.BLACK);
            var31.setFont(new Font("SansSerif", 0, 13));
            var31.drawString(
               String.format(Locale.US, "VOL: POC=%.6f  VAH=%.6f  VAL=%.6f  TotalVol=%.0f", this.prevPOC, this.prevVAH, this.prevVAL, var9), 220, 52
            );
            var31.setColor(new Color(220, 220, 220));
            var31.drawRect(220, 70, 1340, 840);
            double var42 = 840.0 / this.lastNumBins;
            int var44 = Math.max(1, (int)Math.floor(var42) - 1);
            var31.setFont(new Font("Monospaced", 0, 11));
            FontMetrics var45 = var31.getFontMetrics();
            int var46 = this.yLabelStepForDenseAxis();

            for (int var47 = 0; var47 < this.lastNumBins; var47++) {
               double var48 = this.cfgEnableVCP() && this.clusterBins != null && this.clusterBins.length >= this.lastNumBins
                  ? this.clusterBins[var47]
                  : this.volumeBins[var47];
               int var50 = (int)Math.round(var48 / var24 * 1340.0);
               int var51 = (int)Math.round(70.0 + (this.lastNumBins - 1 - var47) * var42);
               if (var47 >= var28 && var47 <= var29) {
                  var31.setColor(new Color(245, 250, 245));
                  var31.fillRect(220, var51, 1340, var44);
               }

               var31.setColor(var47 == var11 ? new Color(255, 215, 0) : new Color(120, 120, 120));
               var31.fillRect(220, var51, var50, var44);
               if (var47 % var46 == 0) {
                  String var52 = String.format(Locale.US, "%.6f", var5 + (var47 + 0.5) * var7);
                  var31.setColor(Color.BLACK);
                  var31.drawString(var52, 210 - var45.stringWidth(var52), var51 + var44 / 2 + var45.getAscent() / 2 - 2);
                  var31.setColor(new Color(180, 180, 180));
                  var31.drawLine(216, var51 + var44 / 2, 220, var51 + var44 / 2);
               }
            }

            this.drawLevelLine(var31, 70, 1340, 220, var42, var28, this.prevVAL, new Color(220, 20, 60), "VAL");
            this.drawLevelLine(var31, 70, 1340, 220, var42, var29, this.prevVAH, new Color(34, 139, 34), "VAH");
            this.drawLevelLine(var31, 70, 1340, 220, var42, var11, this.prevPOC, new Color(255, 165, 0), "POC");

            for (int var66 = 0; var66 < 5; var66++) {
               if (this.prevHVN[var66] > 0.0 && var7 > 0.0) {
                  int var68 = clampInt((int)((this.prevHVN[var66] - var5) / var7), 0, this.lastNumBins - 1);
                  this.drawLevelLine(var31, 70, 1340, 220, var42, var68, this.prevHVN[var66], new Color(128, 0, 128), "HVN" + (var66 + 1));
               }

               if (this.prevLVN[var66] > 0.0 && var7 > 0.0) {
                  int var69 = clampInt((int)((this.prevLVN[var66] - var5) / var7), 0, this.lastNumBins - 1);
                  this.drawLevelLine(var31, 70, 1340, 220, var42, var69, this.prevLVN[var66], new Color(0, 150, 136), "LVN" + (var66 + 1));
               }
            }

            short var67 = 940;
            var31.setColor(Color.BLACK);
            var31.drawLine(220, var67, 1560, var67);

            for (int var72 : new int[]{0, 25, 50, 75, 100}) {
               int var73 = 220 + (int)(var72 / 100.0 * 1340.0);
               var31.drawLine(var73, var67, var73, var67 + 6);
               var31.drawString(var72 + "%", var73 - 10, var67 + 22);
            }

            ImageIO.write(var30, "png", new File(var23));
         } catch (Exception var60) {
         } finally {
            var31.dispose();
         }
      }
   }

   protected void drawLevelLine(Graphics2D var1, int var2, int var3, int var4, double var5, int var7, double var8, Color var10, String var11) {
      int var12 = (int)Math.round(var2 + (this.lastNumBins - 1 - var7) * var5);
      var1.setColor(var10);
      var1.setStroke(new BasicStroke(2.0F));
      var1.drawLine(var4, var12, var4 + var3, var12);
      var1.drawString(var11 + " " + String.format(Locale.US, "%.6f", var8), var4 + var3 - 150, var12 - 5);
   }

   @Override
   protected void exportMultiSessionSVG() {
      if (this.historyCount != 0) {
         String var1 = this.resolveExportFolder();
         new File(var1).mkdirs();
         String var2 = "";

         try {
            var2 = this.cfgChart().Symbol;
         } catch (Exception var104) {
         }

         String[] var3 = new String[]{
            "",
            "Previous Day",
            "Previous Week",
            "Previous Month",
            "Previous Year",
            "Actual Day",
            "Actual Week",
            "Actual Month",
            "Actual Year",
            "Previous Swing",
            "Actual Swing"
         };
         String var4 = this.cfgSessionLabel() != null
            ? this.cfgSessionLabel()
            : (this.cfgSessionType() >= 1 && this.cfgSessionType() < var3.length ? var3[this.cfgSessionType()] : "Session");
         String var5 = "";

         try {
            var5 = this.cfgChart().Timeframe;
         } catch (Exception var103) {
         }

         String var6 = this.cfgBinSizeMode() == 2 ? "Fixed Tick(" + this.cfgTicksPerBin() + ")" : "Range(" + this.cfgProfileRows() + ")";
         String var7 = (var5.isEmpty() ? "" : " | TF:" + var5) + " | Bins:" + var6 + (this.cfgEnableVCP() ? " | VolumeCluster" : "");
         String var8 = var2.isEmpty() ? "" : var2.replaceAll("[^A-Za-z0-9_-]", "") + "_";
         String var9 = var4.replaceAll("\\s+", "");
         String var10 = this.getStrategy() != null ? this.getStrategy().getStrategyName() : "";
         var10 = var10 == null ? "" : var10.replaceAll("[^A-Za-z0-9_\\-]", "_");
         String var11 = var1 + File.separator + "VP_" + (var10.isEmpty() ? "" : var10 + "_") + var8 + var9 + "_" + this.fileRandomSuffix + ".svg";
         this.saveChartPath(var11);
         int var12 = this.historyCount;
         int var13 = 0;
         byte var14 = 20;
         if (var12 > var14) {
            var13 = var12 - var14;
            var12 = var14;
         }

         int var15 = var13 + var12 - 1;
         String var16 = this.formatForFilename(this.histSessionStart[var15]);
         double var17 = Double.MIN_VALUE;
         double var19 = Double.MAX_VALUE;

         for (int var21 = 0; var21 < var12; var21++) {
            var17 = Math.max(var17, this.histSessionHigh[var13 + var21]);
            var19 = Math.min(var19, this.histSessionLow[var13 + var21]);
         }

         if (!(var17 <= var19)) {
            double var116 = var17 - var19;
            var19 -= var116 * 0.02;
            var17 += var116 * 0.02;
            var116 = var17 - var19;
            byte var23 = 80;
            byte var24 = 70;
            byte var25 = 60;
            byte var26 = 95;
            int var27 = this.cfgShowVolumeSubchart() ? 120 : 0;
            int var28 = this.cfgShowVolumeSubchart() ? 10 : 0;
            int var29 = 800 - var28 - var27;
            int var30 = Math.max(800, var12 * 400);
            int var31 = var23 + var30 + var24;
            int var32 = var25 + var29 + var28 + var27 + var26;
            long var33 = this.histSessionStart[var13];
            long var35 = this.histSessionEnd[var13 + var12 - 1];
            long var37 = var35 - var33;
            if (var37 > 0L) {
               double[] var39 = new double[var12];
               double[] var40 = new double[var12];
               ArrayList var41 = new ArrayList();
               int var42 = 0;

               try {
                  while (true) {
                     long var43 = this.cfgChart().Time(var42);
                     if (var43 < var33) {
                        break;
                     }

                     if (var43 >= var33 && var43 < var35) {
                        var41.add(new long[]{var43});
                     }

                     var42++;
                  }
               } catch (Exception var112) {
               }

               Collections.reverse(var41);
               int var119 = var41.size();
               if (var119 > 0) {
                  double var44 = (double)var30 / var119;

                  for (int var46 = 0; var46 < var12; var46++) {
                     int var47 = -1;
                     int var48 = -1;

                     for (int var49 = 0; var49 < var119; var49++) {
                        long var50 = ((long[])var41.get(var49))[0];
                        if (var50 >= this.histSessionStart[var13 + var46] && var50 < this.histSessionEnd[var13 + var46]) {
                           if (var47 < 0) {
                              var47 = var49;
                           }

                           var48 = var49;
                        }
                     }

                     if (var47 >= 0) {
                        var39[var46] = var23 + var47 * var44;
                        var40[var46] = var23 + (var48 + 1) * var44;
                     } else {
                        var39[var46] = var23 + (double)(this.histSessionStart[var13 + var46] - var33) / var37 * var30;
                        var40[var46] = var23 + (double)(this.histSessionEnd[var13 + var46] - var33) / var37 * var30;
                     }
                  }
               } else {
                  for (int var120 = 0; var120 < var12; var120++) {
                     var39[var120] = var23 + (double)(this.histSessionStart[var13 + var120] - var33) / var37 * var30;
                     var40[var120] = var23 + (double)(this.histSessionEnd[var13 + var120] - var33) / var37 * var30;
                  }
               }

               double var118 = Math.max(0.1, Math.min(1.0, 1.0));
               double var121 = Math.max(0.1, Math.min(1.0, 0.5));

               try (BufferedWriter var122 = new BufferedWriter(new FileWriter(var11))) {
                  var122.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                  var122.write(
                     "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\""
                        + var31
                        + "\" height=\""
                        + var32
                        + "\" viewBox=\"0 0 "
                        + var31
                        + " "
                        + var32
                        + "\">"
                  );
                  var122.write("<rect id=\"bgRect\" width=\"" + var31 + "\" height=\"" + var32 + "\" fill=\"#1a1a2e\"/>\n");
                  String var123 = this.getStrategy() != null ? this.getStrategy().getStrategyName() : "";
                  String var124 = " (last " + var12 + " sessions)";
                  String var125 = "Volume Profile" + (var2.isEmpty() ? "" : " - " + var2) + " | " + var4 + var7 + var124;
                  String var126 = (var123.isEmpty() ? "" : svgEsc(var123) + " - ") + svgEsc(var125);
                  var122.write(
                     "<text id=\"chartTitle\" class=\"thTitle\" x=\""
                        + var23
                        + "\" y=\"35\" font-family=\"Sans-Serif\" font-size=\"16\" font-weight=\"700\" fill=\"#ffffff\">StrategyQuantX - "
                        + var126
                        + "</text>\n"
                  );
                  var122.write(
                     "<rect id=\"plotRect\" x=\""
                        + var23
                        + "\" y=\""
                        + var25
                        + "\" width=\""
                        + var30
                        + "\" height=\""
                        + var29
                        + "\" fill=\"#16213e\" rx=\"4\"/>\n"
                  );
                  byte var51 = 20;

                  for (int var52 = 0; var52 <= var51; var52++) {
                     double var53 = var19 + var116 * var52 / var51;
                     double var55 = var25 + var29 - (var53 - var19) / var116 * var29;
                     var122.write(
                        "<text class=\"thPrice\" x=\""
                           + (var23 + var30 + 8)
                           + "\" y=\""
                           + (var55 + 4.0)
                           + "\" font-family=\"Monospace\" font-size=\"9\" fill=\"#888\" text-anchor=\"start\">"
                           + String.format(Locale.US, "%.5f", var53)
                           + "</text>\n"
                     );
                     var122.write(
                        "<line class=\"thGrid\" x1=\""
                           + var23
                           + "\" y1=\""
                           + var55
                           + "\" x2=\""
                           + (var31 - var24)
                           + "\" y2=\""
                           + var55
                           + "\" stroke=\"#2a2a4a\" stroke-width=\"0.5\"/>\n"
                     );
                  }

                  for (int var127 = 0; var127 < var12; var127++) {
                     double var129 = var39[var127];
                     double var138 = var40[var127];
                     double var57 = Math.max(10.0, var138 - var129);
                     var122.write(
                        "<line class=\"thSesDiv\" x1=\""
                           + var129
                           + "\" y1=\""
                           + var25
                           + "\" x2=\""
                           + var129
                           + "\" y2=\""
                           + (var25 + var29)
                           + "\" stroke=\"#555\" stroke-width=\"0.8\" stroke-dasharray=\"4,3\"/>\n"
                     );
                     String var59 = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.US).format(new Date(this.histSessionStart[var13 + var127]));
                     double var60 = (var129 + var138) / 2.0;
                     var122.write(
                        "<text class=\"thDate\" x=\""
                           + var60
                           + "\" y=\""
                           + (var25 + var29 + var28 + var27 + 45)
                           + "\" font-family=\"Sans-Serif\" font-size=\"10\" fill=\"#aaa\" text-anchor=\"middle\">"
                           + svgEsc(var59)
                           + "</text>\n"
                     );
                     double var62 = var25 + var29 - (this.histSessionHigh[var13 + var127] - var19) / var116 * var29;
                     double var64 = var25 + var29 - (this.histSessionLow[var13 + var127] - var19) / var116 * var29;
                     int var66 = 4;
                     if (this.cfgShowPOCDelta()) {
                        var66++;
                     }

                     if (this.cfgShowVADelta()) {
                        var66++;
                     }

                     if (this.cfgShowProfileRange()) {
                        var66++;
                     }

                     if (this.cfgShowPOCPosition()) {
                        var66 += 2;
                     }

                     byte var67 = 11;
                     int var68 = var66 * var67;
                     double[] var69 = new double[var66];
                     if (var62 - var25 > var68 + 5) {
                        for (int var221 = 0; var221 < var66; var221++) {
                           var69[var221] = var62 - (var68 - var221 * var67) + 2.0;
                        }
                     } else {
                        for (int var70 = 0; var70 < var66; var70++) {
                           var69[var70] = var64 + 14.0 + var70 * var67;
                        }
                     }

                     int var222 = 0;
                     var122.write("<g class=\"sStats\" data-shy=\"" + String.format(Locale.US, "%.2f", var62) + "\">\n");
                     if (this.histSessionLabel != null && this.histSessionLabel[var13 + var127] != null) {
                        var122.write(
                           "<text class=\"thSesLbl\" x=\""
                              + var60
                              + "\" y=\""
                              + (var69[0] - var67)
                              + "\" font-family=\"Sans-Serif\" font-size=\"13\" font-weight=\"800\" fill=\"#ffcc00\" text-anchor=\"middle\">"
                              + svgEsc(this.histSessionLabel[var13 + var127])
                              + "</text>\n"
                        );
                     }

                     String var71 = String.format(Locale.US, "%.0f", this.histTotalVolume[var13 + var127]);
                     String var72 = String.format(Locale.US, "%.0f", this.histBullVolume[var13 + var127]);
                     String var73 = String.format(Locale.US, "%.0f", this.histBearVolume[var13 + var127]);
                     double var74 = this.histBullVolume[var13 + var127] - this.histBearVolume[var13 + var127];
                     String var76 = String.format(Locale.US, "%+.0f", var74);
                     String var77 = var74 >= 0.0 ? "#00bcd4" : "#f44336";
                     var122.write(
                        "<text class=\"thVol\" id=\"rpVol_"
                           + var127
                           + "\" x=\""
                           + var60
                           + "\" y=\""
                           + var69[var222++]
                           + "\" font-family=\"Sans-Serif\" font-size=\"12\" font-weight=\"700\" fill=\"#ccc\" text-anchor=\"middle\">Vol: "
                           + var71
                           + "</text>\n"
                     );
                     var122.write(
                        "<text id=\"rpBull_"
                           + var127
                           + "\" x=\""
                           + var60
                           + "\" y=\""
                           + var69[var222++]
                           + "\" font-family=\"Sans-Serif\" font-size=\"12\" font-weight=\"700\" fill=\"#4caf50\" text-anchor=\"middle\">Bull: "
                           + var72
                           + "</text>\n"
                     );
                     var122.write(
                        "<text id=\"rpBear_"
                           + var127
                           + "\" x=\""
                           + var60
                           + "\" y=\""
                           + var69[var222++]
                           + "\" font-family=\"Sans-Serif\" font-size=\"12\" font-weight=\"700\" fill=\"#f44336\" text-anchor=\"middle\">Bear: "
                           + var73
                           + "</text>\n"
                     );
                     var122.write(
                        "<text id=\"rpDelta_"
                           + var127
                           + "\" x=\""
                           + var60
                           + "\" y=\""
                           + var69[var222++]
                           + "\" font-family=\"Sans-Serif\" font-size=\"12\" font-weight=\"700\" fill=\""
                           + var77
                           + "\" text-anchor=\"middle\">Delta: "
                           + var76
                           + "</text>\n"
                     );
                     if (this.cfgShowPOCDelta()) {
                        String var78;
                        String var79;
                        if (var127 > 0) {
                           double var80 = this.histPOC[var13 + var127] - this.histPOC[var13 + var127 - 1];
                           var78 = String.format(Locale.US, "%+.5f", var80);
                           var79 = var80 >= 0.0 ? "#00bcd4" : "#f44336";
                        } else {
                           var78 = "N/A";
                           var79 = "#888";
                        }

                        var122.write(
                           "<text id=\"rpDPOC_"
                              + var127
                              + "\" x=\""
                              + var60
                              + "\" y=\""
                              + var69[var222++]
                              + "\" font-family=\"Sans-Serif\" font-size=\"10\" font-weight=\"700\" fill=\""
                              + var79
                              + "\" text-anchor=\"middle\">ΔPOC: "
                              + var78
                              + "</text>\n"
                        );
                     }

                     if (this.cfgShowVADelta()) {
                        String var269;
                        String var280;
                        if (var127 > 0) {
                           double var285 = (this.histVAH[var13 + var127] + this.histVAL[var13 + var127]) / 2.0;
                           double var82 = (this.histVAH[var13 + var127 - 1] + this.histVAL[var13 + var127 - 1]) / 2.0;
                           double var84 = var285 - var82;
                           var269 = String.format(Locale.US, "%+.5f", var84);
                           var280 = var84 >= 0.0 ? "#00bcd4" : "#f44336";
                        } else {
                           var269 = "N/A";
                           var280 = "#888";
                        }

                        var122.write(
                           "<text id=\"rpDVA_"
                              + var127
                              + "\" x=\""
                              + var60
                              + "\" y=\""
                              + var69[var222++]
                              + "\" font-family=\"Sans-Serif\" font-size=\"10\" font-weight=\"700\" fill=\""
                              + var280
                              + "\" text-anchor=\"middle\">ΔVA: "
                              + var269
                              + "</text>\n"
                        );
                     }

                     if (this.cfgShowProfileRange()) {
                        double var270 = this.histSessionHigh[var13 + var127] - this.histSessionLow[var13 + var127];
                        String var286 = String.format(Locale.US, "%.5f", var270);
                        var122.write(
                           "<text class=\"thStat\" id=\"rpRange_"
                              + var127
                              + "\" x=\""
                              + var60
                              + "\" y=\""
                              + var69[var222++]
                              + "\" font-family=\"Sans-Serif\" font-size=\"10\" font-weight=\"700\" fill=\"#ccc\" text-anchor=\"middle\">Range: "
                              + var286
                              + "</text>\n"
                        );
                     }

                     if (this.cfgShowPOCPosition()) {
                        double var271 = this.histSessionHigh[var13 + var127] - this.histSessionLow[var13 + var127];
                        if (var271 > 0.0) {
                           double var287 = (this.histSessionHigh[var13 + var127] - this.histPOC[var13 + var127]) / var271 * 100.0;
                           double var296 = (this.histPOC[var13 + var127] - this.histSessionLow[var13 + var127]) / var271 * 100.0;
                           var122.write(
                              "<text class=\"thStat\" id=\"rpPOCUp_"
                                 + var127
                                 + "\" x=\""
                                 + var60
                                 + "\" y=\""
                                 + var69[var222++]
                                 + "\" font-family=\"Sans-Serif\" font-size=\"10\" font-weight=\"700\" fill=\"#aaa\" text-anchor=\"middle\">POC↑: "
                                 + String.format(Locale.US, "%.1f%%", var287)
                                 + "</text>\n"
                           );
                           var122.write(
                              "<text class=\"thStat\" id=\"rpPOCDn_"
                                 + var127
                                 + "\" x=\""
                                 + var60
                                 + "\" y=\""
                                 + var69[var222++]
                                 + "\" font-family=\"Sans-Serif\" font-size=\"10\" font-weight=\"700\" fill=\"#aaa\" text-anchor=\"middle\">POC↓: "
                                 + String.format(Locale.US, "%.1f%%", var296)
                                 + "</text>\n"
                           );
                        } else {
                           var122.write(
                              "<text class=\"thStat\" id=\"rpPOCUp_"
                                 + var127
                                 + "\" x=\""
                                 + var60
                                 + "\" y=\""
                                 + var69[var222++]
                                 + "\" font-family=\"Sans-Serif\" font-size=\"10\" font-weight=\"700\" fill=\"#888\" text-anchor=\"middle\">POC↑: N/A</text>\n"
                           );
                           var122.write(
                              "<text class=\"thStat\" id=\"rpPOCDn_"
                                 + var127
                                 + "\" x=\""
                                 + var60
                                 + "\" y=\""
                                 + var69[var222++]
                                 + "\" font-family=\"Sans-Serif\" font-size=\"10\" font-weight=\"700\" fill=\"#888\" text-anchor=\"middle\">POC↓: N/A</text>\n"
                           );
                        }
                     }

                     var122.write("</g>\n");
                  }

                  boolean var128 = this.cfgSessionType() >= 5 && this.cfgSessionType() != 9;
                  var122.write(
                     "<defs><clipPath id=\"plotClip\"><rect x=\""
                        + var23
                        + "\" y=\""
                        + var25
                        + "\" width=\""
                        + var30
                        + "\" height=\""
                        + var29
                        + "\"/></clipPath></defs>\n"
                  );
                  var122.write("<g id=\"chartG\" clip-path=\"url(#plotClip)\">\n");
                  var122.write("<g id=\"candlesG\" opacity=\"" + String.format(Locale.US, "%.2f", var118) + "\">\n");
                  if (this.cfgShowCandlesticks()) {
                     this.drawSvgCandlesUnified(var122, var23, var30, var25, var29, var19, var116, var33, var35, var37, 1.0);
                  }

                  var122.write("</g>\n");
                  var122.write("<g id=\"profileG\" opacity=\"" + String.format(Locale.US, "%.2f", var121) + "\">\n");

                  for (int var130 = 0; var130 < var12; var130++) {
                     double var54 = var39[var130];
                     double var56 = var40[var130];
                     double var58 = Math.max(10.0, var56 - var54);
                     if (var128) {
                        var122.write("<g id=\"rpProfile_" + var130 + "\">\n");
                     }

                     int var163 = this.histNumBins[var13 + var130];
                     double var61 = this.histSessionHigh[var13 + var130] - this.histSessionLow[var13 + var130];
                     double var63 = var163 > 0 && var61 > 0.0 ? var61 / var163 : 1.0;
                     double[] var65 = this.histVolumeBins[var13 + var130];
                     double var194 = 0.0;
                     int var210 = 0;

                     for (int var214 = 0; var214 < var163; var214++) {
                        if (var65[var214] > var194) {
                           var194 = var65[var214];
                           var210 = var214;
                        }
                     }

                     if (var194 <= 0.0) {
                        if (var128) {
                           var122.write("</g>\n");
                        }
                     } else {
                        for (int var215 = 0; var215 < var163; var215++) {
                           if (!(var65[var215] <= 0.0)) {
                              double var231 = this.histSessionLow[var13 + var130] + var215 * var63;
                              double var241 = var231 + var63;
                              double var249 = var25 + var29 - (var241 - var19) / var116 * var29;
                              double var260 = var25 + var29 - (var231 - var19) / var116 * var29;
                              double var272 = Math.max(0.5, var260 - var249 - 0.3);
                              double var288 = var65[var215] / var194 * var58 * 0.8;
                              double var297 = this.histBullBins[var130][var215];
                              double var303 = this.histBearBins[var130][var215];
                              double var86 = var297 + var303;
                              if (var86 <= 0.0) {
                                 var86 = var65[var215];
                              }

                              double var88 = var288 * (var297 / var86);
                              double var90 = var288 - var88;
                              double var92 = var231 + var63 / 2.0;
                              boolean var94 = var215 == var210;
                              boolean var95 = var92 >= this.histVAL[var13 + var130] && var92 <= this.histVAH[var13 + var130];
                              if (var88 > 0.0) {
                                 String var96;
                                 if (var94) {
                                    var96 = "rgba(255,215,0,0.85)";
                                 } else if (var95) {
                                    var96 = "rgba(76,175,80,0.85)";
                                 } else {
                                    var96 = "rgba(76,175,80,0.55)";
                                 }

                                 var122.write(
                                    "<rect x=\""
                                       + var54
                                       + "\" y=\""
                                       + var249
                                       + "\" width=\""
                                       + var88
                                       + "\" height=\""
                                       + var272
                                       + "\" fill=\""
                                       + var96
                                       + "\"/>\n"
                                 );
                              }

                              if (var90 > 0.0) {
                                 String var306;
                                 if (var94) {
                                    var306 = "rgba(255,215,0,0.85)";
                                 } else if (var95) {
                                    var306 = "rgba(239,83,80,0.85)";
                                 } else {
                                    var306 = "rgba(239,83,80,0.55)";
                                 }

                                 var122.write(
                                    "<rect x=\""
                                       + (var54 + var88)
                                       + "\" y=\""
                                       + var249
                                       + "\" width=\""
                                       + var90
                                       + "\" height=\""
                                       + var272
                                       + "\" fill=\""
                                       + var306
                                       + "\"/>"
                                 );
                              }

                              if (var272 >= 2.0) {
                                 double var307 = var297 - var303;
                                 String var98 = String.format(Locale.US, "%+.0f", var307);
                                 String var99 = var307 >= 0.0 ? "#4caf50" : "#f44336";
                                 double var100 = var249 + var272 / 2.0 + 3.0;
                                 var122.write(
                                    "<text x=\""
                                       + (var54 - 3.0)
                                       + "\" y=\""
                                       + var100
                                       + "\" font-family=\"Monospace\" font-size=\"8\" font-weight=\"700\" fill=\""
                                       + var99
                                       + "\" text-anchor=\"end\">"
                                       + var98
                                       + "</text>\n"
                                 );
                              }
                           }
                        }

                        if (var128) {
                           var122.write("</g>\n");
                        }
                     }
                  }

                  var122.write("</g>\n");

                  for (int var131 = 0; var131 < var12; var131++) {
                     double var133 = var39[var131];
                     double var144 = var40[var131];
                     double var151 = Math.max(10.0, var144 - var133);
                     int var164 = (int)var151;
                     this.drawSvgLevel(var122, (int)var133, var164, var25, var29, var19, var116, this.histPOC[var13 + var131], "#ffa500", "POC");
                     this.drawSvgLevel(var122, (int)var133, var164, var25, var29, var19, var116, this.histVAH[var13 + var131], "#4caf50", "VAH");
                     this.drawSvgLevel(var122, (int)var133, var164, var25, var29, var19, var116, this.histVAL[var13 + var131], "#f44336", "VAL");
                     if (this.histIBH[var13 + var131] > 0.0 && this.histIBL[var13 + var131] > 0.0) {
                        double var169 = var25 + var29 - (this.histIBH[var13 + var131] - var19) / var116 * var29;
                        double var181 = var25 + var29 - (this.histIBL[var13 + var131] - var19) / var116 * var29;
                        double var189 = Math.min(var169, var181);
                        double var201 = Math.abs(var181 - var169);
                        var122.write(
                           "<rect x=\""
                              + (int)var133
                              + "\" y=\""
                              + var189
                              + "\" width=\""
                              + var164
                              + "\" height=\""
                              + var201
                              + "\" fill=\"#ff9800\" fill-opacity=\"0.13\" stroke=\"none\" vector-effect=\"non-scaling-stroke\"/>\n"
                        );
                     }

                     if (this.histIBH[var13 + var131] > 0.0) {
                        this.drawSvgLevel(var122, (int)var133, var164, var25, var29, var19, var116, this.histIBH[var13 + var131], "#00bcd4", "IBH");
                     }

                     if (this.histIBL[var13 + var131] > 0.0) {
                        this.drawSvgLevel(var122, (int)var133, var164, var25, var29, var19, var116, this.histIBL[var13 + var131], "#e040fb", "IBL");
                     }

                     for (int var170 = 0; var170 < 5; var170++) {
                        if (this.histHVN[var13 + var131][var170] > 0.0) {
                           this.drawSvgLevel(
                              var122, (int)var133, var164, var25, var29, var19, var116, this.histHVN[var13 + var131][var170], "#9c27b0", "HVN" + (var170 + 1)
                           );
                        }

                        if (this.histLVN[var13 + var131][var170] > 0.0) {
                           this.drawSvgLevel(
                              var122, (int)var133, var164, var25, var29, var19, var116, this.histLVN[var13 + var131][var170], "#009688", "LVN" + (var170 + 1)
                           );
                        }
                     }
                  }

                  boolean var132 = this.cfgSessionType() == 9 || this.cfgSessionType() == 10;
                  if (var132 && this.cfgShowZigZagLine() && var12 > 1) {
                     var122.write("<g id=\"zigzagG\" opacity=\"0.85\">\n");
                     StringBuilder var134 = new StringBuilder("<!-- ZZ pivots: ");

                     for (int var139 = 0; var139 < var12; var139++) {
                        var134.append(String.format(Locale.US, "[%d]=%.5f/%d ", var139, this.histPivotPrice[var13 + var139], this.histPivotDir[var13 + var139]));
                     }

                     var134.append(" -->\n");
                     var122.write(var134.toString());
                     StringBuilder var140 = new StringBuilder();

                     for (int var145 = 0; var145 < var12; var145++) {
                        double var148 = this.histPivotPrice[var13 + var145];
                        if (var148 <= 0.0) {
                           var148 = var145 % 2 == 0 ? this.histSessionHigh[var13 + var145] : this.histSessionLow[var13 + var145];
                        }

                        if (!(var148 <= 0.0)) {
                           double var156 = var40[var145];
                           double var171 = var25 + var29 - (var148 - var19) / var116 * var29;
                           if (var140.length() > 0) {
                              var140.append(" ");
                           }

                           var140.append(String.format(Locale.US, "%.1f,%.1f", var156, var171));
                        }
                     }

                     if (var140.length() > 0) {
                        var122.write(
                           "<polyline points=\""
                              + var140.toString()
                              + "\" fill=\"none\" stroke=\"#00e5ff\" stroke-width=\"1\" stroke-linejoin=\"round\" vector-effect=\"non-scaling-stroke\"/>\n"
                        );
                     }

                     var122.write("</g>\n");
                  }

                  var122.write("</g>\n");
                  if (this.cfgShowDeltaPerLevel()) {
                     var122.write("<g id=\"deltaG\" clip-path=\"url(#plotClip)\">\n");

                     for (int var135 = 0; var135 < var12; var135++) {
                        double var141 = var39[var135];
                        double var149 = var40[var135];
                        int var157 = this.histNumBins[var13 + var135];
                        double var165 = this.histSessionHigh[var13 + var135] - this.histSessionLow[var13 + var135];
                        double var177 = var157 > 0 && var165 > 0.0 ? var165 / var157 : 1.0;
                        double[] var186 = this.histVolumeBins[var13 + var135];
                        double var190 = 0.0;

                        for (int var202 = 0; var202 < var157; var202++) {
                           if (var186[var202] > var190) {
                              var190 = var186[var202];
                           }
                        }

                        if (!(var190 <= 0.0)) {
                           var122.write(
                              "<g class=\"dSes\" data-x1=\""
                                 + String.format(Locale.US, "%.1f", var141)
                                 + "\" data-x2=\""
                                 + String.format(Locale.US, "%.1f", var149)
                                 + "\">\n"
                           );

                           for (int var203 = 0; var203 < var157; var203++) {
                              if (!(var186[var203] <= 0.0)) {
                                 double var211 = this.histBullBins[var135][var203];
                                 double var232 = this.histBearBins[var135][var203];
                                 double var242 = var211 - var232;
                                 double var250 = this.histSessionLow[var13 + var135] + var203 * var177 + var177 / 2.0;
                                 double var261 = var165 > 0.0 ? var29 * var177 / var165 : 0.0;
                                 if (!(var261 < 2.0)) {
                                    String var273 = String.format(Locale.US, "%+.0f", var242);
                                    String var281 = var242 >= 0.0 ? "#4caf50" : "#f44336";
                                    double var289 = var25 + var29 - (var250 - var19) / var116 * var29;
                                    var122.write(
                                       "<text x=\""
                                          + (var141 - 3.0)
                                          + "\" y=\""
                                          + var289
                                          + "\" data-price=\""
                                          + String.format(Locale.US, "%.10f", var250)
                                          + "\" font-family=\"Monospace\" font-size=\"8\" font-weight=\"700\" fill=\""
                                          + var281
                                          + "\" text-anchor=\"end\">"
                                          + var273
                                          + "</text>\n"
                                    );
                                 }
                              }
                           }

                           var122.write("</g>\n");
                        }
                     }

                     var122.write("</g>\n");
                  }

                  var122.write("<g id=\"yAxisBg\"><rect x=\"0\" y=\"0\" width=\"" + var23 + "\" height=\"" + var32 + "\" fill=\"#1a1a2e\"/></g>\n");
                  var122.write("<g id=\"yAxisG\"></g>\n");
                  if (this.cfgShowVolumeSubchart()) {
                     int var136 = var25 + var29 + var28;
                     var122.write(
                        "<rect id=\"volSubBg\" x=\""
                           + var23
                           + "\" y=\""
                           + var136
                           + "\" width=\""
                           + var30
                           + "\" height=\""
                           + var27
                           + "\" fill=\"#16213e\" rx=\"4\" opacity=\"0.6\"/>"
                     );
                     var122.write(
                        "<line class=\"thVolSep\" x1=\""
                           + var23
                           + "\" y1=\""
                           + var136
                           + "\" x2=\""
                           + (var23 + var30)
                           + "\" y2=\""
                           + var136
                           + "\" stroke=\"#555\" stroke-width=\"0.8\"/>"
                     );
                     var122.write(
                        "<text class=\"thVolLbl\" x=\""
                           + (var23 + 5)
                           + "\" y=\""
                           + (var136 + 12)
                           + "\" font-family=\"Sans-Serif\" font-size=\"9\" fill=\"#888\" font-weight=\"700\">Volume</text>\n"
                     );
                     int var142 = 0;
                     double var146 = 0.0;
                     ArrayList var152 = new ArrayList();
                     ArrayList var158 = new ArrayList();
                     int var166 = 0;

                     try {
                        while (true) {
                           long var172 = this.cfgChart().Time(var166);
                           if (var172 < var33) {
                              break;
                           }

                           if (var172 >= var33 && var172 < var35) {
                              double var182 = this.cfgChart().Volume(var166);
                              var152.add(var182);
                              var158.add(this.cfgChart().Close(var166) >= this.cfgChart().Open(var166));
                              var142++;
                              if (var182 > var146) {
                                 var146 = var182;
                              }
                           }

                           var166++;
                        }
                     } catch (Exception var105) {
                     }

                     if (var142 > 0 && var146 > 0.0) {
                        double var173 = (double)var30 / var142;
                        double var183 = Math.max(1.0, var173 * 0.7);
                        int var191 = var27 - 16;

                        for (int var195 = 0; var195 < var142; var195++) {
                           double var204 = (Double)var152.get(var195);
                           boolean var216 = (Boolean)var158.get(var195);
                           double var233 = var204 / var146 * var191;
                           double var243 = var23 + (var142 - 1 - var195) * var173;
                           double var251 = var136 + var27 - var233;
                           String var262 = var216 ? "#26a69a" : "#ef5350";
                           var122.write(
                              "<rect x=\""
                                 + String.format(Locale.US, "%.1f", var243)
                                 + "\" y=\""
                                 + String.format(Locale.US, "%.1f", var251)
                                 + "\" width=\""
                                 + String.format(Locale.US, "%.1f", var183)
                                 + "\" height=\""
                                 + String.format(Locale.US, "%.1f", Math.max(0.5, var233))
                                 + "\" fill=\""
                                 + var262
                                 + "\" opacity=\"0.7\"/>"
                           );
                        }

                        int var196 = Math.min(this.cfgVolumeMALength(), var142);
                        if (var196 >= 2) {
                           StringBuilder var205 = new StringBuilder();
                           boolean var212 = true;

                           for (int var217 = 0; var217 < var142; var217++) {
                              int var234 = Math.max(0, var217 - var196 / 2);
                              int var238 = Math.min(var142, var234 + var196);
                              var234 = Math.max(0, var238 - var196);
                              double var244 = 0.0;

                              for (int var252 = var234; var252 < var238; var252++) {
                                 var244 += var152.get(var252);
                              }

                              double var253 = var244 / (var238 - var234);
                              double var263 = var253 / var146 * var191;
                              double var274 = var23 + (var142 - 1 - var217) * var173 + var183 / 2.0;
                              double var290 = var136 + var27 - var263;
                              if (var212) {
                                 var205.append(String.format(Locale.US, "M%.1f,%.1f", var274, var290));
                                 var212 = false;
                              } else {
                                 var205.append(String.format(Locale.US, " L%.1f,%.1f", var274, var290));
                              }
                           }

                           var122.write("<path d=\"" + var205.toString() + "\" fill=\"none\" stroke=\"#ffab40\" stroke-width=\"1.2\" opacity=\"0.9\"/>\n");
                        }
                     }

                     for (int var174 = 0; var174 < var12; var174++) {
                        double var178 = var39[var174];
                        var122.write(
                           "<line class=\"thVolSep\" x1=\""
                              + String.format(Locale.US, "%.1f", var178)
                              + "\" y1=\""
                              + var136
                              + "\" x2=\""
                              + String.format(Locale.US, "%.1f", var178)
                              + "\" y2=\""
                              + (var136 + var27)
                              + "\" stroke=\"#555\" stroke-width=\"0.8\" stroke-dasharray=\"4,3\"/>\n"
                        );
                     }
                  }

                  boolean var137 = this.cfgSessionType() == 2
                     || this.cfgSessionType() == 3
                     || this.cfgSessionType() == 4
                     || this.cfgSessionType() == 6
                     || this.cfgSessionType() == 7
                     || this.cfgSessionType() == 8
                     || this.cfgSessionType() == 9
                     || this.cfgSessionType() == 10;
                  String var143 = var137 ? "dd.MM" : "HH:mm";
                  this.drawSvgTimeAxis(var122, var23, var30, var25, var29 + var28 + var27, var33, var35, var143);
                  StringBuilder var147 = new StringBuilder();
                  var147.append("var barData=[");
                  boolean var150 = true;

                  for (int var153 = 0; var153 < var12; var153++) {
                     double var159 = var39[var153];
                     double var175 = var40[var153];
                     double var184 = Math.max(10.0, var175 - var159);
                     int var192 = 0;
                     int var197 = 0;

                     try {
                        while (true) {
                           long var206 = this.cfgChart().Time(var197);
                           if (var206 < this.histSessionStart[var13 + var153]) {
                              break;
                           }

                           if (var206 >= this.histSessionStart[var13 + var153] && var206 < this.histSessionEnd[var13 + var153]) {
                              var192++;
                           }

                           var197++;
                        }
                     } catch (Exception var106) {
                     }

                     if (var192 != 0) {
                        double var207 = var184 / var192;
                        var197 = 0;
                        int var218 = 0;

                        try {
                           while (true) {
                              long var236 = this.cfgChart().Time(var197);
                              if (var236 < this.histSessionStart[var13 + var153]) {
                                 break;
                              }

                              if (var236 >= this.histSessionStart[var13 + var153] && var236 < this.histSessionEnd[var13 + var153]) {
                                 double var245 = var159 + (var192 - 1 - var218) * var207 + var207 / 2.0;
                                 double var254 = this.cfgChart().Open(var197);
                                 double var264 = this.cfgChart().High(var197);
                                 double var275 = this.cfgChart().Low(var197);
                                 double var291 = this.cfgChart().Close(var197);
                                 double var298 = this.cfgChart().Volume(var197);
                                 if (!var150) {
                                    var147.append(",");
                                 }

                                 var147.append("{x:").append(String.format(Locale.US, "%.1f", var245));
                                 var147.append(",t:").append(var236);
                                 var147.append(",o:").append(String.format(Locale.US, "%.5f", var254));
                                 var147.append(",h:").append(String.format(Locale.US, "%.5f", var264));
                                 var147.append(",l:").append(String.format(Locale.US, "%.5f", var275));
                                 var147.append(",c:").append(String.format(Locale.US, "%.5f", var291));
                                 var147.append(",v:").append(String.format(Locale.US, "%.0f", var298));
                                 var147.append("}");
                                 var150 = false;
                                 var218++;
                              }

                              var197++;
                           }
                        } catch (Exception var107) {
                        }
                     }
                  }

                  var147.append("];\n");
                  if (var128) {
                     StringBuilder var154 = new StringBuilder();
                     var154.append(
                        "var rpMT="
                           + var25
                           + ",rpPH="
                           + var29
                           + ",rpGLow="
                           + String.format(Locale.US, "%.10f", var19)
                           + ",rpGRange="
                           + String.format(Locale.US, "%.10f", var116)
                           + ",rpN="
                           + var12
                           + ";\n"
                     );
                     var154.append(
                        "var rpIsCluster="
                           + (this.cfgEnableVCP() ? "true" : "false")
                           + ",rpClusterSigma="
                           + String.format(Locale.US, "%.2f", this.cfgClusterSpread())
                           + ",rpMaxCenters="
                           + this.cfgMaxClusterCenters()
                           + ";\n"
                     );
                     var154.append("var rpShowDelta=" + (this.cfgShowDeltaPerLevel() ? "true" : "false") + ",rpDeltaFs=8;\n");
                     var154.append("var rpSessions=[");

                     for (int var160 = 0; var160 < var12; var160++) {
                        double var167 = var39[var160];
                        double var179 = var40[var160];
                        double var187 = Math.max(10.0, var179 - var167);
                        int var199 = this.histNumBins[var13 + var160];
                        double var208 = this.histSessionLow[var13 + var160];
                        double var219 = this.histSessionHigh[var13 + var160] - var208;
                        double var239 = var199 > 0 && var219 > 0.0 ? var219 / var199 : 1.0;
                        int var247 = 0;
                        int var255 = 0;

                        try {
                           while (true) {
                              long var75 = this.cfgChart().Time(var255);
                              if (var75 < this.histSessionStart[var13 + var160]) {
                                 break;
                              }

                              if (var75 >= this.histSessionStart[var13 + var160] && var75 < this.histSessionEnd[var13 + var160]) {
                                 var247++;
                              }

                              var255++;
                           }
                        } catch (Exception var108) {
                        }

                        double[][] var258 = new double[var247][var199];
                        boolean[] var265 = new boolean[var247];
                        var255 = 0;
                        int var267 = 0;

                        try {
                           while (true) {
                              long var276 = this.cfgChart().Time(var255);
                              if (var276 < this.histSessionStart[var13 + var160]) {
                                 break;
                              }

                              if (var276 >= this.histSessionStart[var13 + var160] && var276 < this.histSessionEnd[var13 + var160]) {
                                 int var292 = var247 - 1 - var267;
                                 double var81 = this.cfgChart().Open(var255);
                                 double var83 = this.cfgChart().High(var255);
                                 double var85 = this.cfgChart().Low(var255);
                                 double var87 = this.cfgChart().Close(var255);
                                 double var89 = this.cfgChart().Volume(var255);
                                 var265[var292] = var87 >= var81;
                                 int var91 = Math.max(0, Math.min(var199 - 1, (int)((var83 - var208) / var239)));
                                 int var304 = Math.max(0, Math.min(var199 - 1, (int)((var85 - var208) / var239)));
                                 double var93 = var89 / Math.max(1, var91 - var304 + 1);

                                 for (int var305 = var304; var305 <= var91; var305++) {
                                    var258[var292][var305] = var93;
                                 }

                                 var267++;
                              }

                              var255++;
                           }
                        } catch (Exception var109) {
                        }

                        if (var160 > 0) {
                           var154.append(",");
                        }

                        var154.append(
                           "{nb:"
                              + var199
                              + ",low:"
                              + String.format(Locale.US, "%.10f", var208)
                              + ",bs:"
                              + String.format(Locale.US, "%.10f", var239)
                              + ",x:"
                              + String.format(Locale.US, "%.1f", var167)
                              + ",pw:"
                              + String.format(Locale.US, "%.1f", var187)
                              + ",n:"
                              + var247
                              + ",bars:["
                        );

                        for (int var277 = 0; var277 < var247; var277++) {
                           if (var277 > 0) {
                              var154.append(",");
                           }

                           var154.append("{b:" + (var265[var277] ? "1" : "0") + ",d:{");
                           boolean var282 = true;

                           for (int var293 = 0; var293 < var199; var293++) {
                              if (var258[var277][var293] > 0.0) {
                                 if (!var282) {
                                    var154.append(",");
                                 }

                                 var154.append(var293 + ":" + String.format(Locale.US, "%.2f", var258[var277][var293]));
                                 var282 = false;
                              }
                           }

                           var154.append("}}");
                        }

                        var154.append("]}");
                     }

                     var154.append("];\n");
                     var154.append("var rpCurSes=" + (var12 - 1) + ";\n");
                     var147.append(var154);
                     byte var161 = 26;
                     byte var168 = 3;
                     byte var176 = 15;
                     int var180 = var176 * (var161 + var168) + var168;
                     int var185 = var23 + var180 + 8;
                     int var188 = var25 + var29 + var28 + var27 + 60;
                     short var193 = 200;
                     byte var200 = 28;
                     byte var209 = 22;
                     byte var213 = 4;
                     int var220 = var185 + 120 + 50;
                     int var237 = 9 * (var200 + var213) + var193 + 320;
                     String var240 = "rx=\"4\" fill=\"#2a2a3e\" stroke=\"#555\" stroke-width=\"0.8\" cursor=\"pointer\"";
                     String var246 = "font-family=\"Sans-Serif\" font-size=\"12\" fill=\"#ccc\" text-anchor=\"middle\" pointer-events=\"none\" dominant-baseline=\"central\"";
                     String var248 = "font-family=\"Sans-Serif\" font-size=\"9\" fill=\"#ccc\" text-anchor=\"middle\" pointer-events=\"none\" dominant-baseline=\"central\"";
                     var122.write("<!-- Replay Toolbar -->\n");
                     var122.write("<g id=\"replayToolbar\">\n");
                     var122.write(
                        "<rect id=\"rpTbBg\" x=\""
                           + (var220 - 5)
                           + "\" y=\""
                           + (var188 - 3)
                           + "\" width=\""
                           + var237
                           + "\" height=\""
                           + (var209 + 6)
                           + "\" rx=\"6\" fill=\"rgba(20,20,35,0.85)\" stroke=\"#444\" stroke-width=\"0.5\"/>\n"
                     );
                     String[] var257 = new String[]{"rpSesFirst", "rpSesPrev10", "rpFirst", "rpPrev", "rpPlay", "rpNext", "rpLast", "rpSesNext10", "rpSesLast"};
                     String[] var259 = new String[]{"⏮", "⏪", "◀|", "◀", "▶", "▶", "|▶", "⏩", "⏭"};
                     String[] var266 = new String[]{
                        "First Session",
                        "-10 Sessions",
                        "Prev Session",
                        "Step Back",
                        "Play/Pause",
                        "Step Forward",
                        "Next Session",
                        "+10 Sessions",
                        "Last Session"
                     };
                     boolean[] var268 = new boolean[]{false, false, true, false, false, false, true, false, false};

                     for (int var278 = 0; var278 < 9; var278++) {
                        int var283 = var220 + var278 * (var200 + var213);
                        var122.write(
                           "<rect id=\""
                              + var257[var278]
                              + "\" x=\""
                              + var283
                              + "\" y=\""
                              + var188
                              + "\" width=\""
                              + var200
                              + "\" height=\""
                              + var209
                              + "\" "
                              + var240
                              + "><title>"
                              + var266[var278]
                              + "</title></rect>\n"
                        );
                        var122.write(
                           "<text x=\""
                              + (var283 + var200 / 2)
                              + "\" y=\""
                              + (var188 + var209 / 2)
                              + "\" "
                              + (var268[var278] ? var248 : var246)
                              + ">"
                              + var259[var278]
                              + "</text>\n"
                        );
                     }

                     var122.write(
                        "<text id=\"rpPauseIco\" x=\""
                           + (var220 + 4 * (var200 + var213) + var200 / 2)
                           + "\" y=\""
                           + (var188 + var209 / 2)
                           + "\" "
                           + var246
                           + " style=\"display:none\">⏸</text>\n"
                     );
                     int var279 = var220 + 9 * (var200 + var213) + 10;
                     int var284 = var188 + var209 / 2;
                     var122.write(
                        "<line x1=\""
                           + var279
                           + "\" y1=\""
                           + var284
                           + "\" x2=\""
                           + (var279 + var193)
                           + "\" y2=\""
                           + var284
                           + "\" stroke=\"#555\" stroke-width=\"2\" stroke-linecap=\"round\"/>\n"
                     );
                     var122.write(
                        "<line id=\"rpSliderFill\" x1=\""
                           + var279
                           + "\" y1=\""
                           + var284
                           + "\" x2=\""
                           + (var279 + var193)
                           + "\" y2=\""
                           + var284
                           + "\" stroke=\"#4fc3f7\" stroke-width=\"2\" stroke-linecap=\"round\"/>\n"
                     );
                     var122.write(
                        "<circle id=\"rpSliderKnob\" cx=\""
                           + (var279 + var193)
                           + "\" cy=\""
                           + var284
                           + "\" r=\"6\" fill=\"#4fc3f7\" stroke=\"#fff\" stroke-width=\"1\" cursor=\"pointer\"/>\n"
                     );
                     var122.write(
                        "<rect id=\"rpSliderHit\" x=\""
                           + (var279 - 5)
                           + "\" y=\""
                           + (var284 - 10)
                           + "\" width=\""
                           + (var193 + 10)
                           + "\" height=\"20\" fill=\"transparent\" cursor=\"pointer\"/>\n"
                     );
                     int var294 = var279 + var193 + 15;
                     var122.write(
                        "<text id=\"rpCounter\" x=\""
                           + var294
                           + "\" y=\""
                           + (var188 + var209 / 2)
                           + "\" font-family=\"Monospace\" font-size=\"11\" fill=\"#aaa\" dominant-baseline=\"central\">0 / 0</text>\n"
                     );
                     int var295 = var294 + 80;
                     var122.write(
                        "<text id=\"rpSesLabel\" x=\""
                           + var295
                           + "\" y=\""
                           + (var188 + var209 / 2)
                           + "\" font-family=\"Monospace\" font-size=\"11\" fill=\"#7ab5ff\" dominant-baseline=\"central\">Session "
                           + var12
                           + "/"
                           + var12
                           + "</text>\n"
                     );
                     int var299 = var295 + 110;
                     var122.write("<foreignObject x=\"" + var299 + "\" y=\"" + (var188 + 1) + "\" width=\"60\" height=\"" + (var209 - 2) + "\">\n");
                     var122.write("<body xmlns=\"http://www.w3.org/1999/xhtml\" style=\"margin:0;background:transparent\">\n");
                     var122.write(
                        "<input id=\"rpSesInput\" type=\"text\" value=\""
                           + var12
                           + "\" style=\"width:50px;height:"
                           + (var209 - 6)
                           + "px;background:#1a1a2e;color:#7ab5ff;border:1px solid #555;border-radius:3px;font-family:Monospace;font-size:11px;text-align:center;outline:none;padding:1px 2px\" title=\"Enter session number and press Enter\"/>\n"
                     );
                     var122.write("</body></foreignObject>\n");
                     var122.write("</g>\n");
                     var122.write(
                        "<rect id=\"rpSesHilight\" x=\"0\" y=\""
                           + var25
                           + "\" width=\"100\" height=\""
                           + var29
                           + "\" fill=\"none\" stroke=\"#4fc3f7\" stroke-width=\"2\" stroke-dasharray=\"6,3\" opacity=\"0.6\" style=\"display:none\"/>\n"
                     );
                     var147.append("var rpSlX=" + var279 + ",rpSlW=" + var193 + ",rpTbX=" + var220 + ";\n");
                     var147.append("var rpSesXArr=[");

                     for (int var300 = 0; var300 < var12; var300++) {
                        if (var300 > 0) {
                           var147.append(",");
                        }

                        var147.append(String.format(Locale.US, "%.1f", var39[var300]));
                     }

                     var147.append("];\n");
                     var147.append("var rpSesXEnd=[");

                     for (int var301 = 0; var301 < var12; var301++) {
                        if (var301 > 0) {
                           var147.append(",");
                        }

                        var147.append(String.format(Locale.US, "%.1f", var40[var301]));
                     }

                     var147.append("];\n");
                     var147.append("var rpOrigKpi=[");

                     for (int var302 = 0; var302 < var12; var302++) {
                        if (var302 > 0) {
                           var147.append(",");
                        }

                        var147.append(
                           "{v:"
                              + String.format(Locale.US, "%.0f", this.histTotalVolume[var13 + var302])
                              + ",b:"
                              + String.format(Locale.US, "%.0f", this.histBullVolume[var13 + var302])
                              + ",r:"
                              + String.format(Locale.US, "%.0f", this.histBearVolume[var13 + var302])
                              + ",d:"
                              + String.format(Locale.US, "%+.0f", this.histBullVolume[var13 + var302] - this.histBearVolume[var13 + var302])
                              + ",poc:"
                              + String.format(Locale.US, "%.6f", this.histPOC[var13 + var302])
                              + ",vah:"
                              + String.format(Locale.US, "%.6f", this.histVAH[var13 + var302])
                              + ",val:"
                              + String.format(Locale.US, "%.6f", this.histVAL[var13 + var302])
                              + ",sh:"
                              + String.format(Locale.US, "%.6f", this.histSessionHigh[var13 + var302])
                              + ",sl:"
                              + String.format(Locale.US, "%.6f", this.histSessionLow[var13 + var302])
                              + "}"
                        );
                     }

                     var147.append("];\n");
                  }

                  var122.write("<g id=\"crosshairG\" style=\"display:none;pointer-events:none\">\n");
                  var122.write(
                     "<line id=\"chV\" x1=\"0\" y1=\""
                        + var25
                        + "\" x2=\"0\" y2=\""
                        + (var25 + var29)
                        + "\" stroke=\"#999\" stroke-width=\"0.5\" stroke-dasharray=\"3,3\"/>\n"
                  );
                  var122.write(
                     "<line id=\"chH\" x1=\""
                        + var23
                        + "\" y1=\"0\" x2=\""
                        + (var23 + var30)
                        + "\" y2=\"0\" stroke=\"#999\" stroke-width=\"0.5\" stroke-dasharray=\"3,3\"/>\n"
                  );
                  var122.write("<g id=\"chPriceG\">");
                  var122.write(
                     "<rect id=\"chPriceBg\" x=\""
                        + (var23 + var30 + 2)
                        + "\" y=\"0\" width=\"60\" height=\"16\" rx=\"2\" fill=\"#333\" stroke=\"#666\" stroke-width=\"0.5\"/>"
                  );
                  var122.write(
                     "<text id=\"chPriceTxt\" x=\""
                        + (var23 + var30 + 32)
                        + "\" y=\"12\" font-family=\"Monospace\" font-size=\"9\" fill=\"#fff\" text-anchor=\"middle\"></text>"
                  );
                  var122.write("</g>\n");
                  var122.write("<g id=\"chTimeG\">");
                  var122.write(
                     "<rect id=\"chTimeBg\" x=\"0\" y=\""
                        + (var25 + var29 + 2)
                        + "\" width=\"70\" height=\"16\" rx=\"2\" fill=\"#333\" stroke=\"#666\" stroke-width=\"0.5\"/>"
                  );
                  var122.write(
                     "<text id=\"chTimeTxt\" x=\"0\" y=\""
                        + (var25 + var29 + 14)
                        + "\" font-family=\"Monospace\" font-size=\"9\" fill=\"#fff\" text-anchor=\"middle\"></text>"
                  );
                  var122.write("</g>\n");
                  var122.write("<g id=\"chTooltipG\">");
                  var122.write(
                     "<rect id=\"chTooltipBg\" x=\"0\" y=\"0\" width=\"165\" height=\"100\" rx=\"4\" fill=\"rgba(20,20,40,0.92)\" stroke=\"#555\" stroke-width=\"0.5\"/>"
                  );
                  var122.write("<text id=\"chTooltipTxt\" x=\"8\" y=\"14\" font-family=\"Monospace\" font-size=\"10\" fill=\"#ddd\">");
                  var122.write("<tspan id=\"chTD\" x=\"8\" dy=\"0\"></tspan>");
                  var122.write("<tspan id=\"chTO\" x=\"8\" dy=\"14\"></tspan>");
                  var122.write("<tspan id=\"chTH\" x=\"8\" dy=\"14\"></tspan>");
                  var122.write("<tspan id=\"chTL\" x=\"8\" dy=\"14\"></tspan>");
                  var122.write("<tspan id=\"chTC\" x=\"8\" dy=\"14\"></tspan>");
                  var122.write("<tspan id=\"chTV\" x=\"8\" dy=\"14\"></tspan>");
                  var122.write("</text></g>\n");
                  var122.write("</g>\n");
                  this.writeSvgDrawingTools(var122, var23, var25, var30, var29, var19, var116, var147.toString(), var28 + var27, var26);
                  var122.write("<script type=\"text/javascript\">\n");
                  var122.write("//<![CDATA[\n");
                  StringBuilder var155 = new StringBuilder("var _sesData=[");

                  for (int var162 = 0; var162 < var12; var162++) {
                     if (var162 > 0) {
                        var155.append(",");
                     }

                     var155.append(
                        String.format(
                           Locale.US,
                           "{x1:%.1f,x2:%.1f,h:%.6f,l:%.6f}",
                           var39[var162],
                           var40[var162],
                           this.histSessionHigh[var13 + var162],
                           this.histSessionLow[var13 + var162]
                        )
                     );
                  }

                  var155.append("];");
                  var122.write(var155.toString() + "\n");
                  var122.write(
                     "var _mt="
                        + var25
                        + ",_ph="
                        + var29
                        + ",_gL="
                        + String.format(Locale.US, "%.8f", var19)
                        + ",_gR="
                        + String.format(Locale.US, "%.8f", var116)
                        + ",_mL="
                        + var23
                        + ";\n"
                  );
                  var122.write("var _yFill='#ccc',_yStroke='#888',_yBg='#1a1a2e';\n");
                  var122.write("function _autoScale(){\n");
                  var122.write("  var svg=document.querySelector('svg');\n");
                  var122.write("  if(!svg)return;\n");
                  var122.write("  var cr=svg.getBoundingClientRect();\n");
                  var122.write("  var vb=svg.viewBox.baseVal;\n");
                  var122.write("  var de=document.documentElement,bd=document.body;\n");
                  var122.write("  var scrollPx=(de&&de.scrollLeft)||(bd&&bd.scrollLeft)||window.scrollX||window.pageXOffset||0;\n");
                  var122.write("  var pixToSvg=(cr.width>0)?vb.width/cr.width:1;\n");
                  var122.write("  var sl=scrollPx*pixToSvg;\n");
                  var122.write("  var vw=(window.innerWidth||document.documentElement.clientWidth)*pixToSvg;\n");
                  var122.write("  var vL=sl,vR=sl+vw;\n");
                  var122.write("  var visH=-1e30,visL=1e30,hasVis=false;\n");
                  var122.write("  for(var i=0;i<_sesData.length;i++){\n");
                  var122.write("    var s=_sesData[i];\n");
                  var122.write("    if(s.x2>=vL && s.x1<=vR){hasVis=true;visH=Math.max(visH,s.h);visL=Math.min(visL,s.l);}\n");
                  var122.write("  }\n");
                  var122.write("  if(!hasVis){visH=_gL+_gR;visL=_gL;}\n");
                  var122.write("  if(visH<=visL){visH=_gL+_gR;visL=_gL;}\n");
                  var122.write("  var pad=(visH-visL)*0.05;visH+=pad;visL-=pad;\n");
                  var122.write("  var minSpan=_gR*0.40;\n");
                  var122.write("  if((visH-visL)<minSpan){\n");
                  var122.write("    var mid=(visH+visL)/2;\n");
                  var122.write("    visL=mid-minSpan/2;visH=mid+minSpan/2;\n");
                  var122.write("  }\n");
                  var122.write("  var gH=_gL+_gR;\n");
                  var122.write("  if(visH>gH)visH=gH;\n");
                  var122.write("  if(visL<_gL)visL=_gL;\n");
                  var122.write("  if(visH<=visL){visH=gH;visL=_gL;}\n");
                  var122.write("  var yH=_mt+_ph-(visH-_gL)/_gR*_ph;\n");
                  var122.write("  var yL=_mt+_ph-(visL-_gL)/_gR*_ph;\n");
                  var122.write("  if(yL<=yH){yH=_mt;yL=_mt+_ph;}\n");
                  var122.write("  var sy=_ph/(yL-yH);\n");
                  var122.write("  if(!isFinite(sy)||sy<=0)sy=1;\n");
                  var122.write("  sy=Math.max(1,Math.min(3,sy));\n");
                  var122.write("  var ty=_mt-yH*sy;\n");
                  var122.write("  if(!isFinite(ty))ty=0;\n");
                  var122.write("  var cg=document.getElementById('chartG');\n");
                  var122.write("  if(cg)cg.setAttribute('transform','matrix(1,0,0,'+sy+',0,'+ty+')');\n");
                  var122.write("  var invSy=1/sy;\n");
                  var122.write("  var txts=(cg?cg.getElementsByTagName('text'):[]);\n");
                  var122.write("  for(var i=0;i<txts.length;i++){\n");
                  var122.write("    var t=txts[i];\n");
                  var122.write("    if(!t.dataset.oy)t.dataset.oy=t.getAttribute('y');\n");
                  var122.write("    var oy=parseFloat(t.dataset.oy)||0;\n");
                  var122.write("    var d=(oy+3)*(1-invSy);\n");
                  var122.write("    t.setAttribute('transform','translate(0,'+d+') scale(1,'+invSy+')');\n");
                  var122.write("  }\n");
                  var122.write("  var ya=document.getElementById('yAxisG');\n");
                  var122.write("  if(ya){ya.innerHTML='';var nT=10;\n");
                  var122.write("    for(var t=0;t<=nT;t++){\n");
                  var122.write("      var p=visL+(visH-visL)*t/nT;\n");
                  var122.write("      var yy=_mt+_ph-(_ph*t/nT);\n");
                  var122.write("      var ln=document.createElementNS('http://www.w3.org/2000/svg','line');\n");
                  var122.write("      ln.setAttribute('x1',_mL);ln.setAttribute('x2',_mL+6);\n");
                  var122.write("      ln.setAttribute('y1',yy);ln.setAttribute('y2',yy);\n");
                  var122.write("      ln.setAttribute('stroke',_yStroke);ya.appendChild(ln);\n");
                  var122.write("      var tx=document.createElementNS('http://www.w3.org/2000/svg','text');\n");
                  var122.write("      tx.setAttribute('x',_mL-3);tx.setAttribute('y',yy+3);\n");
                  var122.write("      tx.setAttribute('font-family','Monospace');tx.setAttribute('font-size','9');\n");
                  var122.write("      tx.setAttribute('fill',_yFill);tx.setAttribute('text-anchor','end');\n");
                  var122.write("      tx.textContent=p.toFixed(5);ya.appendChild(tx);\n");
                  var122.write("    }\n");
                  var122.write("  }\n");
                  var122.write("  var sgs=document.querySelectorAll('.sStats');\n");
                  var122.write("  for(var i=0;i<sgs.length;i++){\n");
                  var122.write("    var sg=sgs[i];\n");
                  var122.write("    var origShy=parseFloat(sg.dataset.shy)||0;\n");
                  var122.write("    var newShy=sy*origShy+ty;\n");
                  var122.write("    sg.setAttribute('transform','translate(0,'+(newShy-origShy)+')');\n");
                  var122.write("  }\n");
                  var122.write("  var dg=document.getElementById('deltaG');\n");
                  var122.write("  if(dg){\n");
                  var122.write("    var dGroups=dg.getElementsByClassName('dSes');\n");
                  var122.write("    for(var di=0;di<dGroups.length;di++){\n");
                  var122.write("      var dses=dGroups[di];\n");
                  var122.write("      var dx1=parseFloat(dses.dataset.x1),dx2=parseFloat(dses.dataset.x2);\n");
                  var122.write("      if(dx2>=vL&&dx1<=vR){\n");
                  var122.write("        dses.style.display='';\n");
                  var122.write("        var dtxts=dses.getElementsByTagName('text');\n");
                  var122.write("        for(var dj=0;dj<dtxts.length;dj++){\n");
                  var122.write("          var dt=dtxts[dj];\n");
                  var122.write("          var dp=parseFloat(dt.dataset.price);\n");
                  var122.write("          var ny=_mt+_ph-(dp-visL)/(visH-visL)*_ph;\n");
                  var122.write("          dt.setAttribute('y',ny);\n");
                  var122.write("        }\n");
                  var122.write("      }else{\n");
                  var122.write("        dses.style.display='none';\n");
                  var122.write("      }\n");
                  var122.write("    }\n");
                  var122.write("  }\n");
                  var122.write("}\n");
                  var122.write("// embedded Results panel uses container-driven sizing; autoscale disabled\n");
                  var122.write("function _stickyScroll(){\n");
                  var122.write("  var svg=document.querySelector('svg');\n");
                  var122.write("  if(!svg)return;\n");
                  var122.write("  var vb=svg.viewBox.baseVal;\n");
                  var122.write("  var cr=svg.getBoundingClientRect();\n");
                  var122.write("  var de=document.documentElement,bd=document.body;\n");
                  var122.write("  var scrollPx=(de&&de.scrollLeft)||(bd&&bd.scrollLeft)||window.scrollX||window.pageXOffset||0;\n");
                  var122.write("  var sx=scrollPx*(vb.width/cr.width);\n");
                  var122.write("  var tb=document.getElementById('toolbar');\n");
                  var122.write("  if(tb)tb.setAttribute('transform','translate('+sx+',0)');\n");
                  var122.write("  var rp=document.getElementById('replayToolbar');\n");
                  var122.write("  if(rp)rp.setAttribute('transform','translate('+sx+',0)');\n");
                  var122.write("  var ya=document.getElementById('yAxisG');\n");
                  var122.write("  if(ya)ya.setAttribute('transform','translate('+sx+',0)');\n");
                  var122.write("  var yb=document.getElementById('yAxisBg');\n");
                  var122.write("  if(yb)yb.setAttribute('transform','translate('+sx+',0)');\n");
                  var122.write("  var tt=document.getElementById('chartTitle');\n");
                  var122.write("  if(tt)tt.setAttribute('transform','translate('+sx+',0)');\n");
                  var122.write("  var ac=document.getElementById('annoGroup');\n");
                  var122.write("  if(ac)ac.setAttribute('transform','translate('+sx+',0)');\n");
                  var122.write("  var ch=document.getElementById('crosshairInfo');\n");
                  var122.write("  if(ch)ch.setAttribute('transform','translate('+sx+',0)');\n");
                  var122.write("}\n");
                  var122.write("window.addEventListener('scroll',_stickyScroll);\n");
                  var122.write("window.addEventListener('resize',_stickyScroll);\n");
                  var122.write("setTimeout(_stickyScroll,120);\n");
                  var122.write("//]]>\n");
                  var122.write("</script>\n");
                  var122.write("</svg>\n");
               } catch (Exception var111) {
               }
            }
         }
      }
   }

   protected void writeSvgDrawingTools(
      BufferedWriter var1, int var2, int var3, int var4, int var5, double var6, double var8, String var10, int var11, int var12
   ) throws IOException {
      byte var13 = 26;
      byte var14 = 26;
      byte var15 = 3;
      int var16 = var3 + var5 + var11 + 60;
      String[] var17 = new String[]{
         "btnTrend",
         "btnHoriz",
         "btnRect",
         "btnText",
         "btnDraw",
         "btnRuler",
         "btnColor",
         "btnTheme",
         "btnUndo",
         "btnClear",
         "btnCross",
         "btnZoomIn",
         "btnZoomOut",
         "btnZoomRst"
      };
      String[] var18 = new String[]{"╱", "─", "▭", "T", "✎", "\ud83d\udccf", "●", "☀", "↩", "\ud83d\uddd1", "┼", "+", "−", "↺"};
      String[] var19 = new String[]{
         "Trend Line",
         "Horizontal Level",
         "Rectangle",
         "Text",
         "Freehand Draw",
         "Ruler",
         "Color",
         "Light/Dark Mode",
         "Undo",
         "Clear All",
         "Crosshair",
         "Zoom In",
         "Zoom Out",
         "Reset Zoom"
      };
      int var20 = var17.length + 1;
      int var21 = var20 * (var13 + var15) + var15;
      int var22 = var2;
      var1.write("<g id=\"toolbar\" style=\"cursor:pointer\">\n");
      var1.write(
         "<rect id=\"tbBg\" x=\""
            + (var22 - 2)
            + "\" y=\""
            + (var16 - 2)
            + "\" width=\""
            + (var21 + 4)
            + "\" height=\""
            + (var14 + 4)
            + "\" rx=\"4\" fill=\"rgba(30,30,50,0.85)\" stroke=\"#555\" stroke-width=\"0.5\"/>\n"
      );

      for (int var23 = 0; var23 < var17.length; var23++) {
         int var24 = var22 + var15 + var23 * (var13 + var15);
         var1.write("<g id=\"g_" + var17[var23] + "\">\n");
         var1.write(
            "<rect class=\"tbBtn\" id=\""
               + var17[var23]
               + "\" x=\""
               + var24
               + "\" y=\""
               + var16
               + "\" width=\""
               + var13
               + "\" height=\""
               + var14
               + "\" rx=\"3\" fill=\"#2a2a4a\" stroke=\"#666\" stroke-width=\"0.8\"/>\n"
         );
         if (var17[var23].equals("btnColor")) {
            var1.write(
               "<circle id=\"colorDot\" cx=\""
                  + (var24 + var13 / 2)
                  + "\" cy=\""
                  + (var16 + var14 / 2)
                  + "\" r=\"7\" fill=\"#ffcc00\" stroke=\"#888\" stroke-width=\"0.5\" pointer-events=\"none\"/>\n"
            );
         } else if (var17[var23].equals("btnTheme")) {
            var1.write(
               "<text class=\"tbLbl\" id=\"themeIcon\" x=\""
                  + (var24 + var13 / 2)
                  + "\" y=\""
                  + (var16 + var14 / 2 + 5)
                  + "\" font-family=\"Sans-Serif\" font-size=\"13\" fill=\"#ccc\" text-anchor=\"middle\" pointer-events=\"none\">☀</text>\n"
            );
         } else {
            var1.write(
               "<text class=\"tbLbl\" x=\""
                  + (var24 + var13 / 2)
                  + "\" y=\""
                  + (var16 + var14 / 2 + 5)
                  + "\" font-family=\"Sans-Serif\" font-size=\"13\" fill=\"#ccc\" text-anchor=\"middle\" pointer-events=\"none\">"
                  + var18[var23]
                  + "</text>\n"
            );
         }

         var1.write("<title>" + var19[var23] + "</title>\n");
         var1.write("</g>\n");
      }

      int var31 = var22 + var15 + var17.length * (var13 + var15);
      var1.write("<g id=\"g_btnPrint\">\n");
      var1.write(
         "<rect class=\"tbBtn\" id=\"btnPrint\" x=\""
            + var31
            + "\" y=\""
            + var16
            + "\" width=\""
            + var13
            + "\" height=\""
            + var14
            + "\" rx=\"3\" fill=\"#2a2a4a\" stroke=\"#666\" stroke-width=\"0.8\"/>\n"
      );
      var1.write(
         "<text class=\"tbLbl\" x=\""
            + (var31 + var13 / 2)
            + "\" y=\""
            + (var16 + var14 / 2 + 5)
            + "\" font-family=\"Sans-Serif\" font-size=\"13\" fill=\"#ccc\" text-anchor=\"middle\" pointer-events=\"none\">\ud83d\udda8</text>\n"
      );
      var1.write("<title>Print</title>\n");
      var1.write("</g>\n");
      var1.write("</g>\n");
      int var32 = var22 + var21 + 8;
      var1.write("<g id=\"annoGroup\" style=\"cursor:pointer\">\n");
      var1.write("<foreignObject x=\"" + var32 + "\" y=\"" + (var16 + 2) + "\" width=\"120\" height=\"22\">\n");
      var1.write("<body xmlns=\"http://www.w3.org/1999/xhtml\" style=\"margin:0;background:transparent\">\n");
      var1.write(
         "<label style=\"display:flex;align-items:center;gap:3px;font-family:Sans-Serif;font-size:10px;color:#ccc;cursor:pointer;background:rgba(30,30,50,0.85);padding:2px 5px;border-radius:4px;border:0.5px solid #555\">\n"
      );
      var1.write("<input id=\"annoCheck\" type=\"checkbox\" checked=\"checked\" style=\"margin:0;cursor:pointer\"/>\n");
      var1.write("<span id=\"annoLabel\">Annotations</span></label>\n");
      var1.write("</body></foreignObject>\n");
      var1.write("</g>\n");
      String[] var25 = new String[]{"#ffcc00", "#00e5ff", "#ff5252", "#69f0ae", "#ff80ab", "#b388ff", "#ffffff", "#ffa726"};
      byte var26 = 18;
      int var27 = var22 + var15 + 6 * (var13 + var15);
      int var28 = var16 - var26 - 8;
      var1.write("<g id=\"colorPalette\" style=\"display:none;cursor:pointer\">\n");
      var1.write(
         "<rect x=\""
            + (var27 - 3)
            + "\" y=\""
            + (var28 - 3)
            + "\" width=\""
            + (var25.length * (var26 + 3) + 6)
            + "\" height=\""
            + (var26 + 6)
            + "\" rx=\"4\" fill=\"rgba(30,30,50,0.9)\" stroke=\"#555\" stroke-width=\"0.5\"/>\n"
      );

      for (int var29 = 0; var29 < var25.length; var29++) {
         int var30 = var27 + var29 * (var26 + 3);
         var1.write(
            "<rect class=\"swatch\" data-color=\""
               + var25[var29]
               + "\" x=\""
               + var30
               + "\" y=\""
               + var28
               + "\" width=\""
               + var26
               + "\" height=\""
               + var26
               + "\" rx=\"3\" fill=\""
               + var25[var29]
               + "\" stroke=\"#888\" stroke-width=\"0.5\"/>\n"
         );
      }

      var1.write("</g>\n");
      var1.write("<script type=\"text/ecmascript\"><![CDATA[\n");
      var1.write("(function(){\n");
      var1.write("var svg=document.documentElement,ns='http://www.w3.org/2000/svg';\n");
      var1.write("var MX=" + var2 + ",MT=" + var3 + ",PW=" + var4 + ",PH=" + var5 + ";\n");
      var1.write("var GL=" + String.format(Locale.US, "%.10f", var6) + ",GR=" + String.format(Locale.US, "%.10f", var8) + ";\n");
      var1.write("var mode='',pt1=null,drawn=[],preview=null;\n");
      var1.write("var activeColor='#ffcc00';\n");
      var1.write("var crosshairOn=false;\n");
      var1.write(var10);
      var1.write("var drawModes={'btnTrend':'trend','btnHoriz':'horiz','btnRect':'rect','btnText':'text','btnDraw':'draw','btnRuler':'ruler'};\n");
      var1.write("var toolBtnIds=['btnTrend','btnHoriz','btnRect','btnText','btnDraw','btnRuler'];\n");
      var1.write("function svgPt(e){\n");
      var1.write("  if(svg&&typeof svg.createSVGPoint==='function'){\n");
      var1.write("    var p=svg.createSVGPoint();p.x=e.clientX;p.y=e.clientY;\n");
      var1.write("    var m=svg.getScreenCTM();\n");
      var1.write("    if(m&&typeof m.inverse==='function')return p.matrixTransform(m.inverse());\n");
      var1.write("  }\n");
      var1.write("  var r=svg&&svg.getBoundingClientRect?svg.getBoundingClientRect():{left:0,top:0,width:1,height:1};\n");
      var1.write("  var vb=(svg&&svg.viewBox&&svg.viewBox.baseVal)?svg.viewBox.baseVal:{x:0,y:0,width:r.width||1,height:r.height||1};\n");
      var1.write("  var sx=(r.width>0)?vb.width/r.width:1,sy=(r.height>0)?vb.height/r.height:1;\n");
      var1.write("  return{x:vb.x+(e.clientX-r.left)*sx,y:vb.y+(e.clientY-r.top)*sy};}\n");
      var1.write("function clamp(p){return{x:Math.max(MX,Math.min(MX+PW,p.x)),y:Math.max(MT,Math.min(MT+PH,p.y))};}\n");
      var1.write("function yToPrice(y){return GL+GR*(1-(y-MT)/PH);}\n");
      var1.write("function fmtP(p){return p.toFixed(5);}\n");
      var1.write("var isDark=true;\n");
      var1.write("function btnFill(){return isDark?'#2a2a4a':'#e8e8f0';}\n");
      var1.write("function btnStroke(){return isDark?'#666':'#aaa';}\n");
      var1.write("function hilite(){toolBtnIds.forEach(function(id){var b=document.getElementById(id);");
      var1.write("b.setAttribute('fill',drawModes[id]===mode?'#4181ed':btnFill());");
      var1.write("b.setAttribute('stroke',drawModes[id]===mode?'#7ab5ff':btnStroke());});}\n");
      var1.write("function rmPreview(){if(preview){preview.parentNode.removeChild(preview);preview=null;}}\n");
      var1.write("function addPriceLabel(g,x,y,price,clr){\n");
      var1.write("var t=document.createElementNS(ns,'text');t.setAttribute('x',x+PW-3);t.setAttribute('y',y-3);\n");
      var1.write("t.setAttribute('font-family','Monospace');t.setAttribute('font-size','9');\n");
      var1.write("t.setAttribute('fill',clr);t.setAttribute('text-anchor','end');\n");
      var1.write("t.textContent=fmtP(price);g.appendChild(t);}\n");
      var1.write("var palette=document.getElementById('colorPalette');\n");
      var1.write("var colorDot=document.getElementById('colorDot');\n");
      var1.write("document.getElementById('g_btnColor').addEventListener('click',function(e){\n");
      var1.write("  e.stopPropagation();palette.style.display=palette.style.display==='none'?'block':'none';});\n");
      var1.write("var swatches=document.querySelectorAll('.swatch');\n");
      var1.write("for(var i=0;i<swatches.length;i++){(function(sw){\n");
      var1.write("  sw.addEventListener('click',function(e){e.stopPropagation();\n");
      var1.write("    activeColor=sw.getAttribute('data-color');colorDot.setAttribute('fill',activeColor);\n");
      var1.write("    palette.style.display='none';});\n");
      var1.write("})(swatches[i]);}\n");
      var1.write("document.getElementById('g_btnUndo').addEventListener('click',function(e){\n");
      var1.write("  e.stopPropagation();if(drawn.length){var el=drawn.pop();el.parentNode.removeChild(el);}});\n");
      var1.write("document.getElementById('g_btnClear').addEventListener('click',function(e){\n");
      var1.write("  e.stopPropagation();drawn.forEach(function(el){el.parentNode.removeChild(el);});drawn=[];});\n");
      var1.write("var dBg='#1a1a2e',dPl='#16213e',lBg='#e0e0e0',lPl='#faf5eb';\n");
      var1.write(
         "var dCandAlpha="
            + String.format(Locale.US, "%.2f", Math.max(0.1, Math.min(1.0, 1.0)))
            + ",lCandAlpha="
            + String.format(Locale.US, "%.2f", Math.max(0.1, Math.min(1.0, 0.6)))
            + ",dProfAlpha="
            + String.format(Locale.US, "%.2f", Math.max(0.1, Math.min(1.0, 0.5)))
            + ",lProfAlpha="
            + String.format(Locale.US, "%.2f", Math.max(0.1, Math.min(1.0, 0.8)))
            + ";\n"
      );
      var1.write("document.getElementById('g_btnTheme').addEventListener('click',function(e){\n");
      var1.write("  e.stopPropagation();isDark=!isDark;\n");
      var1.write("  document.getElementById('bgRect').setAttribute('fill',isDark?dBg:lBg);\n");
      var1.write("  document.getElementById('plotRect').setAttribute('fill',isDark?dPl:lPl);\n");
      var1.write("  var tt=document.querySelectorAll('.thTitle');for(var i=0;i<tt.length;i++)tt[i].setAttribute('fill',isDark?'#ffffff':dBg);\n");
      var1.write("  var gd=document.querySelectorAll('.thGrid');for(var i=0;i<gd.length;i++)gd[i].setAttribute('stroke',isDark?'#2a2a4a':'#ddd');\n");
      var1.write("  var pl=document.querySelectorAll('.thPrice');for(var i=0;i<pl.length;i++)pl[i].setAttribute('fill',isDark?'#888':'#555');\n");
      var1.write("  var dl=document.querySelectorAll('.thDate');for(var i=0;i<dl.length;i++)dl[i].setAttribute('fill',isDark?'#aaa':'#666');\n");
      var1.write("  var sd=document.querySelectorAll('.thSesDiv');for(var i=0;i<sd.length;i++)sd[i].setAttribute('stroke',isDark?'#555':'#bbb');\n");
      var1.write("  var vl=document.querySelectorAll('.thVol');for(var i=0;i<vl.length;i++)vl[i].setAttribute('fill',isDark?'#ccc':'#000');\n");
      var1.write(
         "  var sl=document.querySelectorAll('.thStat');for(var i=0;i<sl.length;i++){if(!sl[i].dataset.dk)sl[i].dataset.dk=sl[i].getAttribute('fill');sl[i].setAttribute('fill',isDark?sl[i].dataset.dk:'#000');}\n"
      );
      var1.write("  var br=document.querySelectorAll('.thBrand');for(var i=0;i<br.length;i++)br[i].setAttribute('fill',isDark?'#ffffff':dBg);\n");
      var1.write(
         "  var sesLbl=document.querySelectorAll('.thSesLbl');for(var i=0;i<sesLbl.length;i++)sesLbl[i].setAttribute('fill',isDark?'#ffcc00':'#000');\n"
      );
      var1.write("  document.getElementById('tbBg').setAttribute('fill',isDark?'rgba(30,30,50,0.85)':'rgba(240,240,245,0.9)');\n");
      var1.write("  document.getElementById('tbBg').setAttribute('stroke',isDark?'#555':'#aaa');\n");
      var1.write(
         "  var btns=document.querySelectorAll('.tbBtn');for(var i=0;i<btns.length;i++){btns[i].setAttribute('fill',btnFill());btns[i].setAttribute('stroke',btnStroke());}\n"
      );
      var1.write("  var bl=document.querySelectorAll('.tbLbl');for(var i=0;i<bl.length;i++)bl[i].setAttribute('fill',isDark?'#ccc':'#333');\n");
      var1.write(
         "  var al=document.getElementById('annoLabel');if(al){al.style.color=isDark?'#ccc':'#333';al.parentElement.style.background=isDark?'rgba(30,30,50,0.85)':'rgba(240,240,245,0.9)';al.parentElement.style.borderColor=isDark?'#555':'#aaa';}\n"
      );
      var1.write("  document.getElementById('themeIcon').textContent=isDark?'\\u2600':'\\u263E';\n");
      var1.write("  var cg=document.getElementById('candlesG');if(cg)cg.setAttribute('opacity',isDark?dCandAlpha:lCandAlpha);\n");
      var1.write("  var pg=document.getElementById('profileG');if(pg)pg.setAttribute('opacity',isDark?dProfAlpha:lProfAlpha);\n");
      var1.write("  _yFill=isDark?'#ccc':'#333';_yStroke=isDark?'#888':'#bbb';_yBg=isDark?dBg:lBg;\n");
      var1.write("  var yb=document.getElementById('yAxisBg');if(yb){var yr=yb.getElementsByTagName('rect');if(yr.length)yr[0].setAttribute('fill',_yBg);}\n");
      var1.write("  var vsb=document.getElementById('volSubBg');if(vsb)vsb.setAttribute('fill',isDark?dPl:lPl);\n");
      var1.write("  var vsp=document.querySelectorAll('.thVolSep');for(var i=0;i<vsp.length;i++)vsp[i].setAttribute('stroke',isDark?'#555':'#bbb');\n");
      var1.write("  var vlb=document.querySelectorAll('.thVolLbl');for(var i=0;i<vlb.length;i++)vlb[i].setAttribute('fill',isDark?'#888':'#555');\n");
      var1.write("  _autoScale();\n");
      var1.write("  hilite();\n");
      var1.write("});\n");
      var1.write("document.getElementById('g_btnPrint').addEventListener('click',function(e){\n");
      var1.write("  e.stopPropagation();\n");
      var1.write("  var tb=document.getElementById('toolbar');\n");
      var1.write("  var pp=document.getElementById('printPanel');\n");
      var1.write("  var incAnno=document.getElementById('annoCheck')?document.getElementById('annoCheck').checked:true;\n");
      var1.write("  tb.style.display='none';palette.style.display='none';pp.style.display='none';\n");
      var1.write("  if(!incAnno){drawn.forEach(function(el){el.style.display='none';});}\n");
      var1.write("  setTimeout(function(){window.print();setTimeout(function(){\n");
      var1.write("    tb.style.display='';pp.style.display='';\n");
      var1.write("    if(!incAnno){drawn.forEach(function(el){el.style.display='';});}\n");
      var1.write("  },300);},100);\n");
      var1.write("});\n");
      var1.write("toolBtnIds.forEach(function(id){\n");
      var1.write("document.getElementById('g_'+id).addEventListener('click',function(e){\n");
      var1.write("  e.stopPropagation();mode=(mode===drawModes[id])?'':drawModes[id];pt1=null;rmPreview();hilite();\n");
      var1.write("});});\n");
      var1.write("var drawPts=[],drawEl=null,isDrawing=false;\n");
      var1.write("svg.addEventListener('click',function(e){\n");
      var1.write("  if(!mode||mode==='draw')return;var raw=svgPt(e),p=clamp(raw);\n");
      var1.write("  if(p.x<MX||p.x>MX+PW||p.y<MT||p.y>MT+PH)return;\n");
      var1.write("  if(mode==='horiz'){var g=document.createElementNS(ns,'g');\n");
      var1.write("    var ln=document.createElementNS(ns,'line');\n");
      var1.write("    ln.setAttribute('x1',MX);ln.setAttribute('y1',p.y);ln.setAttribute('x2',MX+PW);ln.setAttribute('y2',p.y);\n");
      var1.write("    ln.setAttribute('stroke',activeColor);ln.setAttribute('stroke-width','1');ln.setAttribute('stroke-dasharray','6,3');\n");
      var1.write("    g.appendChild(ln);addPriceLabel(g,MX,p.y,yToPrice(p.y),activeColor);\n");
      var1.write("    svg.appendChild(g);drawn.push(g);return;}\n");
      var1.write("  if(mode==='trend'){if(!pt1){pt1=p;return;}rmPreview();\n");
      var1.write("    var ln=document.createElementNS(ns,'line');\n");
      var1.write("    ln.setAttribute('x1',pt1.x);ln.setAttribute('y1',pt1.y);ln.setAttribute('x2',p.x);ln.setAttribute('y2',p.y);\n");
      var1.write("    ln.setAttribute('stroke',activeColor);ln.setAttribute('stroke-width','1.5');\n");
      var1.write("    svg.appendChild(ln);drawn.push(ln);pt1=null;return;}\n");
      var1.write("  if(mode==='rect'){if(!pt1){pt1=p;return;}rmPreview();\n");
      var1.write("    var r=document.createElementNS(ns,'rect');\n");
      var1.write("    r.setAttribute('x',Math.min(pt1.x,p.x));r.setAttribute('y',Math.min(pt1.y,p.y));\n");
      var1.write("    r.setAttribute('width',Math.abs(p.x-pt1.x));r.setAttribute('height',Math.abs(p.y-pt1.y));\n");
      var1.write("    r.setAttribute('fill',activeColor);r.setAttribute('fill-opacity','0.12');\n");
      var1.write("    r.setAttribute('stroke',activeColor);r.setAttribute('stroke-width','1');r.setAttribute('stroke-dasharray','4,2');\n");
      var1.write("    svg.appendChild(r);drawn.push(r);pt1=null;return;}\n");
      var1.write("  if(mode==='text'){var txt=prompt('Enter text:');if(txt){\n");
      var1.write("    var t=document.createElementNS(ns,'text');t.setAttribute('x',p.x);t.setAttribute('y',p.y);\n");
      var1.write("    t.setAttribute('font-family','Sans-Serif');t.setAttribute('font-size','13');\n");
      var1.write("    t.setAttribute('fill',activeColor);t.setAttribute('font-weight','600');t.textContent=txt;\n");
      var1.write("    svg.appendChild(t);drawn.push(t);}return;}\n");
      var1.write("  if(mode==='ruler'){if(!pt1){pt1=p;return;}rmPreview();\n");
      var1.write("    var g=document.createElementNS(ns,'g');\n");
      var1.write("    var ln=document.createElementNS(ns,'line');\n");
      var1.write("    ln.setAttribute('x1',pt1.x);ln.setAttribute('y1',pt1.y);ln.setAttribute('x2',p.x);ln.setAttribute('y2',p.y);\n");
      var1.write("    ln.setAttribute('stroke',activeColor);ln.setAttribute('stroke-width','1');ln.setAttribute('stroke-dasharray','3,2');\n");
      var1.write("    g.appendChild(ln);\n");
      var1.write("    var dp=Math.abs(yToPrice(pt1.y)-yToPrice(p.y));\n");
      var1.write("    var dx=Math.abs(p.x-pt1.x),dy=Math.abs(p.y-pt1.y),dist=Math.sqrt(dx*dx+dy*dy);\n");
      var1.write("    var mx2=(pt1.x+p.x)/2,my2=(pt1.y+p.y)/2;\n");
      var1.write("    var t1=document.createElementNS(ns,'text');\n");
      var1.write("    t1.setAttribute('x',mx2);t1.setAttribute('y',my2-8);\n");
      var1.write("    t1.setAttribute('font-family','Sans-Serif');t1.setAttribute('font-size','11');\n");
      var1.write("    t1.setAttribute('fill',activeColor);t1.setAttribute('text-anchor','middle');t1.setAttribute('font-weight','700');\n");
      var1.write("    t1.textContent='\\u0394Price: '+fmtP(dp);g.appendChild(t1);\n");
      var1.write("    var t2=document.createElementNS(ns,'text');\n");
      var1.write("    t2.setAttribute('x',mx2);t2.setAttribute('y',my2+6);\n");
      var1.write("    t2.setAttribute('font-family','Sans-Serif');t2.setAttribute('font-size','10');\n");
      var1.write("    t2.setAttribute('fill',activeColor);t2.setAttribute('text-anchor','middle');\n");
      var1.write("    t2.textContent='\\u0394Px: '+Math.round(dist)+'px';g.appendChild(t2);\n");
      var1.write("    svg.appendChild(g);drawn.push(g);pt1=null;return;}\n");
      var1.write("});\n");
      var1.write("var svg=(typeof svg!=='undefined'&&svg)?svg:document.querySelector('svg');\n");
      var1.write("if(!svg)svg=document.documentElement.querySelector('svg');\n");
      var1.write("var vbBase=(svg&&svg.viewBox&&svg.viewBox.baseVal)?svg.viewBox.baseVal:null;\n");
      var1.write(
         "var origVB={x:0,y:0,w:(vbBase&&vbBase.width?vbBase.width:(parseFloat(svg&&svg.getAttribute('width'))||1000)),h:(vbBase&&vbBase.height?vbBase.height:(parseFloat(svg&&svg.getAttribute('height'))||600))};\n"
      );
      var1.write("var vb={x:0,y:0,w:origVB.w,h:origVB.h};\n");
      var1.write("function setVB(){svg.setAttribute('viewBox',vb.x+' '+vb.y+' '+vb.w+' '+vb.h);}\n");
      var1.write("function zoomAt(cx,cy,factor){\n");
      var1.write("  var nw=vb.w*factor,nh=vb.h*factor;\n");
      var1.write("  if(nw<50||nh<50||nw>origVB.w*3||nh>origVB.h*3)return;\n");
      var1.write("  vb.x=cx-(cx-vb.x)*factor;vb.y=cy-(cy-vb.y)*factor;\n");
      var1.write("  vb.w=nw;vb.h=nh;setVB();}\n");
      var1.write("svg.addEventListener('wheel',function(e){\n");
      var1.write("  e.preventDefault();var p=svgPt(e);\n");
      var1.write("  zoomAt(p.x,p.y,e.deltaY>0?1.12:0.89);\n");
      var1.write("},{passive:false});\n");
      var1.write("document.getElementById('g_btnZoomIn').addEventListener('click',function(e){\n");
      var1.write("  e.stopPropagation();zoomAt(vb.x+vb.w/2,vb.y+vb.h/2,0.75);});\n");
      var1.write("document.getElementById('g_btnZoomOut').addEventListener('click',function(e){\n");
      var1.write("  e.stopPropagation();zoomAt(vb.x+vb.w/2,vb.y+vb.h/2,1.33);});\n");
      var1.write("document.getElementById('g_btnZoomRst').addEventListener('click',function(e){\n");
      var1.write("  e.stopPropagation();vb.x=0;vb.y=0;vb.w=origVB.w;vb.h=origVB.h;setVB();});\n");
      var1.write("var isPan=false,panPt=null;\n");
      var1.write("svg.addEventListener('mousedown',function(e){\n");
      var1.write("  if(mode==='draw'){var raw=svgPt(e),p=clamp(raw);\n");
      var1.write("    if(p.x<MX||p.x>MX+PW||p.y<MT||p.y>MT+PH)return;\n");
      var1.write("    isDrawing=true;drawPts=[p.x+','+p.y];\n");
      var1.write("    drawEl=document.createElementNS(ns,'polyline');\n");
      var1.write("    drawEl.setAttribute('fill','none');drawEl.setAttribute('stroke',activeColor);\n");
      var1.write("    drawEl.setAttribute('stroke-width','2');drawEl.setAttribute('stroke-linecap','round');\n");
      var1.write("    drawEl.setAttribute('stroke-linejoin','round');\n");
      var1.write("    svg.appendChild(drawEl);return;}\n");
      var1.write("  if(!mode){isPan=true;panPt=svgPt(e);svg.style.cursor='grabbing';e.preventDefault();}\n");
      var1.write("});\n");
      var1.write("svg.addEventListener('mousemove',function(e){\n");
      var1.write("  if(isPan&&panPt){var p=svgPt(e);vb.x-=(p.x-panPt.x);vb.y-=(p.y-panPt.y);setVB();panPt=svgPt(e);return;}\n");
      var1.write("  if(mode==='draw'&&isDrawing){var raw=svgPt(e),p=clamp(raw);\n");
      var1.write("    drawPts.push(p.x+','+p.y);if(drawEl)drawEl.setAttribute('points',drawPts.join(' '));return;}\n");
      var1.write("  if(!mode&&!isPan){svg.style.cursor='grab';}\n");
      var1.write("  if(!mode||!pt1)return;var raw=svgPt(e),p=clamp(raw);rmPreview();\n");
      var1.write("  if(mode==='trend'||mode==='ruler'){preview=document.createElementNS(ns,'g');\n");
      var1.write("    var ln=document.createElementNS(ns,'line');\n");
      var1.write("    ln.setAttribute('x1',pt1.x);ln.setAttribute('y1',pt1.y);\n");
      var1.write("    ln.setAttribute('x2',p.x);ln.setAttribute('y2',p.y);\n");
      var1.write("    ln.setAttribute('stroke',activeColor);ln.setAttribute('stroke-width','1');\n");
      var1.write("    ln.setAttribute('stroke-dasharray','3,3');ln.setAttribute('opacity','0.6');\n");
      var1.write("    preview.appendChild(ln);\n");
      var1.write("    if(mode==='ruler'){\n");
      var1.write("      var dp=Math.abs(yToPrice(pt1.y)-yToPrice(p.y));\n");
      var1.write("      var dx=Math.abs(p.x-pt1.x),dy=Math.abs(p.y-pt1.y),dist=Math.sqrt(dx*dx+dy*dy);\n");
      var1.write("      var mx2=(pt1.x+p.x)/2,my2=(pt1.y+p.y)/2;\n");
      var1.write("      var t1=document.createElementNS(ns,'text');\n");
      var1.write("      t1.setAttribute('x',mx2);t1.setAttribute('y',my2-8);\n");
      var1.write("      t1.setAttribute('font-family','Sans-Serif');t1.setAttribute('font-size','11');\n");
      var1.write("      t1.setAttribute('fill',activeColor);t1.setAttribute('text-anchor','middle');t1.setAttribute('font-weight','700');\n");
      var1.write("      t1.textContent='\\u0394Price: '+fmtP(dp);preview.appendChild(t1);\n");
      var1.write("      var t2=document.createElementNS(ns,'text');\n");
      var1.write("      t2.setAttribute('x',mx2);t2.setAttribute('y',my2+6);\n");
      var1.write("      t2.setAttribute('font-family','Sans-Serif');t2.setAttribute('font-size','10');\n");
      var1.write("      t2.setAttribute('fill',activeColor);t2.setAttribute('text-anchor','middle');\n");
      var1.write("      t2.textContent='\\u0394Px: '+Math.round(dist)+'px';preview.appendChild(t2);\n");
      var1.write("    }\n");
      var1.write("    svg.appendChild(preview);}\n");
      var1.write("  if(mode==='rect'){preview=document.createElementNS(ns,'rect');\n");
      var1.write("    preview.setAttribute('x',Math.min(pt1.x,p.x));preview.setAttribute('y',Math.min(pt1.y,p.y));\n");
      var1.write("    preview.setAttribute('width',Math.abs(p.x-pt1.x));preview.setAttribute('height',Math.abs(p.y-pt1.y));\n");
      var1.write("    preview.setAttribute('fill',activeColor);preview.setAttribute('fill-opacity','0.08');\n");
      var1.write("    preview.setAttribute('stroke',activeColor);preview.setAttribute('stroke-width','0.8');\n");
      var1.write("    preview.setAttribute('stroke-dasharray','3,3');preview.setAttribute('opacity','0.5');\n");
      var1.write("    svg.appendChild(preview);}\n");
      var1.write("});\n");
      var1.write("svg.addEventListener('mouseup',function(e){\n");
      var1.write("  if(isPan){isPan=false;svg.style.cursor='grab';}\n");
      var1.write("  if(mode!=='draw'||!isDrawing)return;\n");
      var1.write("  isDrawing=false;if(drawEl&&drawPts.length>1){drawn.push(drawEl);}else if(drawEl){drawEl.parentNode.removeChild(drawEl);}\n");
      var1.write("  drawEl=null;drawPts=[];\n");
      var1.write("});\n");
      var1.write("var chG=document.getElementById('crosshairG');\n");
      var1.write("var chV=document.getElementById('chV'),chH=document.getElementById('chH');\n");
      var1.write("var chPriceBg=document.getElementById('chPriceBg'),chPriceTxt=document.getElementById('chPriceTxt');\n");
      var1.write("var chTimeBg=document.getElementById('chTimeBg'),chTimeTxt=document.getElementById('chTimeTxt');\n");
      var1.write("var chTooltipG=document.getElementById('chTooltipG'),chTooltipBg=document.getElementById('chTooltipBg');\n");
      var1.write("var chTD=document.getElementById('chTD'),chTO=document.getElementById('chTO');\n");
      var1.write("var chTH=document.getElementById('chTH'),chTL=document.getElementById('chTL');\n");
      var1.write("var chTC=document.getElementById('chTC'),chTV=document.getElementById('chTV');\n");
      var1.write("document.getElementById('g_btnCross').addEventListener('click',function(e){\n");
      var1.write("  e.stopPropagation();crosshairOn=!crosshairOn;\n");
      var1.write("  var btn=document.getElementById('btnCross');\n");
      var1.write("  btn.setAttribute('fill',crosshairOn?'#4181ed':btnFill());\n");
      var1.write("  btn.setAttribute('stroke',crosshairOn?'#7ab5ff':btnStroke());\n");
      var1.write("  if(!crosshairOn)chG.style.display='none';\n");
      var1.write("});\n");
      var1.write("function nearestBar(px){\n");
      var1.write("  if(!barData||barData.length===0)return null;\n");
      var1.write("  var best=null,bestD=1e9;\n");
      var1.write("  for(var i=0;i<barData.length;i++){var d=Math.abs(barData[i].x-px);if(d<bestD){bestD=d;best=barData[i];}}\n");
      var1.write("  return best;}\n");
      var1.write("function fmtDT(ms){var d=new Date(ms);\n");
      var1.write("  var dd=d.getUTCFullYear()+'-'+('0'+(d.getUTCMonth()+1)).slice(-2)+'-'+('0'+d.getUTCDate()).slice(-2);\n");
      var1.write("  var tt=('0'+d.getUTCHours()).slice(-2)+':'+('0'+d.getUTCMinutes()).slice(-2);\n");
      var1.write("  return{date:dd,time:tt};}\n");
      var1.write("svg.addEventListener('mousemove',function(e){\n");
      var1.write("  if(!crosshairOn)return;\n");
      var1.write("  var p=svgPt(e);\n");
      var1.write("  if(p.x<MX||p.x>MX+PW||p.y<MT||p.y>MT+PH){chG.style.display='none';return;}\n");
      var1.write("  chG.style.display='';\n");
      var1.write("  chV.setAttribute('x1',p.x);chV.setAttribute('x2',p.x);\n");
      var1.write("  chH.setAttribute('y1',p.y);chH.setAttribute('y2',p.y);\n");
      var1.write("  var price=GL+GR*(1-(p.y-MT)/PH);\n");
      var1.write("  chPriceTxt.textContent=fmtP(price);\n");
      var1.write("  chPriceBg.setAttribute('y',p.y-8);chPriceTxt.setAttribute('y',p.y+3);\n");
      var1.write("  chTimeBg.setAttribute('x',p.x-35);chTimeTxt.setAttribute('x',p.x);\n");
      var1.write("  var bar=nearestBar(p.x);\n");
      var1.write("  if(bar){\n");
      var1.write("    var dt=fmtDT(bar.t);\n");
      var1.write("    chTimeTxt.textContent=dt.time;\n");
      var1.write("    chTD.textContent=dt.date+' '+dt.time;\n");
      var1.write("    chTO.textContent='O: '+bar.o;\n");
      var1.write("    chTH.textContent='H: '+bar.h;\n");
      var1.write("    chTL.textContent='L: '+bar.l;\n");
      var1.write("    chTC.textContent='C: '+bar.c;\n");
      var1.write("    chTV.textContent='V: '+bar.v;\n");
      var1.write("    var tx=p.x+15,ty=p.y-50;\n");
      var1.write("    if(tx+170>MX+PW)tx=p.x-180;if(ty<MT)ty=MT+5;\n");
      var1.write("    chTooltipBg.setAttribute('x',tx);chTooltipBg.setAttribute('y',ty);\n");
      var1.write("    chTD.setAttribute('x',tx+8);chTO.setAttribute('x',tx+8);\n");
      var1.write("    chTH.setAttribute('x',tx+8);chTL.setAttribute('x',tx+8);\n");
      var1.write("    chTC.setAttribute('x',tx+8);chTV.setAttribute('x',tx+8);\n");
      var1.write("    chTD.setAttribute('y',ty+14);\n");
      var1.write("  }\n");
      var1.write("});\n");
      var1.write("svg.addEventListener('mouseleave',function(){if(crosshairOn)chG.style.display='none';});\n");
      var1.write("// Bar Replay Engine (multi-session)\n");
      var1.write("if(typeof rpSessions!=='undefined' && rpSessions.length>0){\n");
      var1.write("var rpStep=rpSessions[rpCurSes].n,rpPlaying=false,rpTimer=null;\n");
      var1.write("var rpKnob=document.getElementById('rpSliderKnob');\n");
      var1.write("var rpFill=document.getElementById('rpSliderFill');\n");
      var1.write("var rpCtr=document.getElementById('rpCounter');\n");
      var1.write("var rpSesLbl=document.getElementById('rpSesLabel');\n");
      var1.write("var rpPauseIco=document.getElementById('rpPauseIco');\n");
      var1.write("var rpTbG=document.getElementById('replayToolbar');\n");
      var1.write("var rpHilight=document.getElementById('rpSesHilight');\n");
      var1.write("var rpCurPocI=0,rpCurVaLo=0,rpCurVaHi=0;\n");
      var1.write("function rpShowAll(){\n");
      var1.write("  for(var s=0;s<rpN;s++){\n");
      var1.write("    var cg=document.getElementById('rpCandles_'+s);\n");
      var1.write("    if(cg){var cs=cg.querySelectorAll('[id^=rc_]');cs.forEach(function(c){c.style.display='';});}\n");
      var1.write("    var pg=document.getElementById('rpProfile_'+s);\n");
      var1.write("    if(pg)pg.style.display='';\n");
      var1.write("  }\n");
      var1.write("}\n");
      var1.write("function rpRender(){\n");
      var1.write("  var ses=rpSessions[rpCurSes];\n");
      var1.write("  var nb=ses.nb,bars=ses.bars,total=ses.n;\n");
      var1.write("  for(var s=0;s<rpN;s++){\n");
      var1.write("    var cg=document.getElementById('rpCandles_'+s);\n");
      var1.write("    if(!cg)continue;\n");
      var1.write("    if(s>rpCurSes){cg.style.display='none';continue;}\n");
      var1.write("    cg.style.display='';\n");
      var1.write("    var cs=cg.querySelectorAll('[id^=rc_]');\n");
      var1.write("    if(s===rpCurSes){\n");
      var1.write("      cs.forEach(function(c){var ci=parseInt(c.id.split('_')[1]);c.style.display=(ci<rpStep)?'':'none';});\n");
      var1.write("    } else {\n");
      var1.write("      cs.forEach(function(c){c.style.display='';});\n");
      var1.write("    }\n");
      var1.write("  }\n");
      var1.write("  for(var s=0;s<rpN;s++){\n");
      var1.write("    var pg=document.getElementById('rpProfile_'+s);\n");
      var1.write("    if(!pg)continue;\n");
      var1.write("    if(s>rpCurSes){pg.style.display='none';continue;}\n");
      var1.write("    if(s!==rpCurSes){pg.style.display='';continue;}\n");
      var1.write("    pg.style.display='';\n");
      var1.write("    while(pg.firstChild)pg.removeChild(pg.firstChild);\n");
      var1.write("    var bins=new Array(nb),bull=new Array(nb),bear=new Array(nb);\n");
      var1.write("    for(var j=0;j<nb;j++){bins[j]=0;bull[j]=0;bear[j]=0;}\n");
      var1.write("    for(var i=0;i<rpStep;i++){\n");
      var1.write("      var bar=bars[i];\n");
      var1.write("      for(var k in bar.d){\n");
      var1.write("        var idx=parseInt(k),vol=bar.d[k];\n");
      var1.write("        bins[idx]+=vol;if(bar.b)bull[idx]+=vol;else bear[idx]+=vol;\n");
      var1.write("      }\n");
      var1.write("    }\n");
      var1.write("    var db=bins;\n");
      var1.write("    if(rpIsCluster){\n");
      var1.write("      var avg=0;for(var j=0;j<nb;j++)avg+=bins[j];avg/=Math.max(1,nb);\n");
      var1.write("      var pks=[],pvs=[];\n");
      var1.write("      for(var j=0;j<nb;j++){\n");
      var1.write("        var lOk=(j===0||bins[j]>=bins[j-1]),rOk=(j===nb-1||bins[j]>=bins[j+1]);\n");
      var1.write("        if(lOk&&rOk&&bins[j]>avg){pks.push(j);pvs.push(bins[j]);}\n");
      var1.write("      }\n");
      var1.write("      var mc=Math.min(rpMaxCenters,pks.length);\n");
      var1.write(
         "      for(var a=0;a<mc;a++){var bi2=a;for(var b2=a+1;b2<pks.length;b2++){if(pvs[b2]>pvs[bi2])bi2=b2;}if(bi2!==a){var ti=pks[a];pks[a]=pks[bi2];pks[bi2]=ti;var tv2=pvs[a];pvs[a]=pvs[bi2];pvs[bi2]=tv2;}}\n"
      );
      var1.write("      if(mc>0){\n");
      var1.write("        db=new Array(nb);for(var j=0;j<nb;j++)db[j]=0;\n");
      var1.write("        for(var c=0;c<mc;c++){var ctr=pks[c],cv=pvs[c];\n");
      var1.write("          for(var j=0;j<nb;j++){var d2=(j-ctr)/rpClusterSigma;db[j]+=cv*Math.exp(-0.5*d2*d2);}\n");
      var1.write("        }\n");
      var1.write("      }\n");
      var1.write("    }\n");
      var1.write("    var maxV=0;rpCurPocI=0;\n");
      var1.write("    for(var j=0;j<nb;j++){if(db[j]>maxV){maxV=db[j];rpCurPocI=j;}}\n");
      var1.write("    if(maxV<=0){rpUpdateUI();return;}\n");
      var1.write("    var totalV=0;for(var j=0;j<nb;j++)totalV+=db[j];\n");
      var1.write("    var vaTarget=totalV*0.7,vaAcc=db[rpCurPocI];rpCurVaLo=rpCurPocI;rpCurVaHi=rpCurPocI;\n");
      var1.write("    while(vaAcc<vaTarget&&(rpCurVaLo>0||rpCurVaHi<nb-1)){\n");
      var1.write("      var up=(rpCurVaHi<nb-1)?db[rpCurVaHi+1]:-1,dn=(rpCurVaLo>0)?db[rpCurVaLo-1]:-1;\n");
      var1.write("      if(up>=dn&&up>=0){rpCurVaHi++;vaAcc+=db[rpCurVaHi];}else if(dn>=0){rpCurVaLo--;vaAcc+=db[rpCurVaLo];}else break;\n");
      var1.write("    }\n");
      var1.write("    for(var j=0;j<nb;j++){\n");
      var1.write("      if(db[j]<=0)continue;\n");
      var1.write("      var bLow=ses.low+j*ses.bs,bHi=bLow+ses.bs;\n");
      var1.write("      var y1=rpMT+rpPH-(bHi-rpGLow)/rpGRange*rpPH;\n");
      var1.write("      var y2=rpMT+rpPH-(bLow-rpGLow)/rpGRange*rpPH;\n");
      var1.write("      var bH=Math.max(0.5,y2-y1-0.3);\n");
      var1.write("      var bW=(db[j]/maxV)*ses.pw*0.8;\n");
      var1.write("      var bv=bull[j],sv=bear[j],tot=bv+sv;\n");
      var1.write("      if(tot<=0)tot=1;\n");
      var1.write("      var bullW=bW*(bv/tot),bearW=bW-bullW;\n");
      var1.write("      var isPOC=(j===rpCurPocI),isVA=(j>=rpCurVaLo&&j<=rpCurVaHi);\n");
      var1.write("      if(bullW>0){\n");
      var1.write("        var f=isPOC?'rgba(255,215,0,0.85)':isVA?'rgba(76,175,80,0.85)':'rgba(76,175,80,0.55)';\n");
      var1.write("        var r=document.createElementNS(ns,'rect');\n");
      var1.write("        r.setAttribute('x',ses.x);r.setAttribute('y',y1);\n");
      var1.write("        r.setAttribute('width',bullW);r.setAttribute('height',bH);\n");
      var1.write("        r.setAttribute('fill',f);pg.appendChild(r);\n");
      var1.write("      }\n");
      var1.write("      if(bearW>0){\n");
      var1.write("        var f2=isPOC?'rgba(255,215,0,0.85)':isVA?'rgba(239,83,80,0.85)':'rgba(239,83,80,0.55)';\n");
      var1.write("        var r2=document.createElementNS(ns,'rect');\n");
      var1.write("        r2.setAttribute('x',ses.x+bullW);r2.setAttribute('y',y1);\n");
      var1.write("        r2.setAttribute('width',bearW);r2.setAttribute('height',bH);\n");
      var1.write("        r2.setAttribute('fill',f2);pg.appendChild(r2);\n");
      var1.write("      }\n");
      var1.write("      if(rpShowDelta&&bH>=2){\n");
      var1.write("        var dv=bv-sv;\n");
      var1.write("        var dtxt=(dv>=0?'+':'')+Math.round(dv);\n");
      var1.write("        var dclr=dv>=0?'#4caf50':'#f44336';\n");
      var1.write("        var dt=document.createElementNS(ns,'text');\n");
      var1.write("        dt.setAttribute('x',ses.x-3);dt.setAttribute('y',y1+bH/2+3);\n");
      var1.write("        dt.setAttribute('font-family','Monospace');dt.setAttribute('font-size',rpDeltaFs);\n");
      var1.write("        dt.setAttribute('font-weight','700');dt.setAttribute('fill',dclr);\n");
      var1.write("        dt.setAttribute('text-anchor','end');\n");
      var1.write("        dt.textContent=dtxt;pg.appendChild(dt);\n");
      var1.write("      }\n");
      var1.write("    }\n");
      var1.write("  }\n");
      var1.write("  rpUpdateUI();\n");
      var1.write("}\n");
      var1.write("function rpUpdateUI(){\n");
      var1.write("  var ses=rpSessions[rpCurSes],total=ses.n;\n");
      var1.write("  var pct=total>0?rpStep/total:1;\n");
      var1.write("  rpKnob.setAttribute('cx',rpSlX+pct*rpSlW);\n");
      var1.write("  rpFill.setAttribute('x2',rpSlX+pct*rpSlW);\n");
      var1.write("  rpCtr.textContent=rpStep+' / '+total;\n");
      var1.write("  rpSesLbl.textContent='Session '+(rpCurSes+1)+'/'+rpN;\n");
      var1.write("  var rpSI=document.getElementById('rpSesInput');if(rpSI)rpSI.value=(rpCurSes+1);\n");
      var1.write("  rpHilight.setAttribute('x',rpSesXArr[rpCurSes]);\n");
      var1.write("  rpHilight.setAttribute('width',rpSesXEnd[rpCurSes]-rpSesXArr[rpCurSes]);\n");
      var1.write("  rpHilight.style.display='';\n");
      var1.write("  rpUpdateKpi();\n");
      var1.write("}\n");
      var1.write("function rpUpdateKpi(){try{\n");
      var1.write("  for(var q=0;q<rpN;q++){\n");
      var1.write("    var ve2=document.getElementById('rpVol_'+q);\n");
      var1.write("    if(!ve2)continue;\n");
      var1.write("    var ok=rpOrigKpi[q];\n");
      var1.write("    if(q>rpCurSes){\n");
      var1.write("      ve2.textContent='Vol: 0';\n");
      var1.write("      document.getElementById('rpBull_'+q).textContent='Bull: 0';\n");
      var1.write("      document.getElementById('rpBear_'+q).textContent='Bear: 0';\n");
      var1.write("      var dEl0=document.getElementById('rpDelta_'+q);\n");
      var1.write("      dEl0.textContent='Delta: 0';dEl0.setAttribute('fill','#888');\n");
      var1.write("      var dpEl0=document.getElementById('rpDPOC_'+q);if(dpEl0){dpEl0.textContent='\\u0394POC: ';dpEl0.setAttribute('fill','#888');}\n");
      var1.write("      var dvEl0=document.getElementById('rpDVA_'+q);if(dvEl0){dvEl0.textContent='\\u0394VA: ';dvEl0.setAttribute('fill','#888');}\n");
      var1.write("      var rgEl0=document.getElementById('rpRange_'+q);if(rgEl0)rgEl0.textContent='Range: ';\n");
      var1.write("      var puEl0=document.getElementById('rpPOCUp_'+q);if(puEl0)puEl0.textContent='POC\\u2191: ';\n");
      var1.write("      var pdEl0=document.getElementById('rpPOCDn_'+q);if(pdEl0)pdEl0.textContent='POC\\u2193: ';\n");
      var1.write("    } else if(q!==rpCurSes||rpStep>=rpSessions[q].n){\n");
      var1.write("      ve2.textContent='Vol: '+ok.v;\n");
      var1.write("      document.getElementById('rpBull_'+q).textContent='Bull: '+ok.b;\n");
      var1.write("      document.getElementById('rpBear_'+q).textContent='Bear: '+ok.r;\n");
      var1.write("      var dEl=document.getElementById('rpDelta_'+q);\n");
      var1.write("      dEl.textContent='Delta: '+(ok.d>=0?'+':'')+ok.d;\n");
      var1.write("      dEl.setAttribute('fill',ok.d>=0?'#00bcd4':'#f44336');\n");
      var1.write("      var dpEl=document.getElementById('rpDPOC_'+q);\n");
      var1.write(
         "      if(dpEl){var pd=q>0?(ok.poc-rpOrigKpi[q-1].poc):0;dpEl.textContent='\\u0394POC: '+(q>0?(pd>=0?'+':'')+pd.toFixed(5):'N/A');dpEl.setAttribute('fill',q>0?(pd>=0?'#00bcd4':'#f44336'):'#888');}\n"
      );
      var1.write("      var dvEl=document.getElementById('rpDVA_'+q);\n");
      var1.write(
         "      if(dvEl){var vm=(ok.vah+ok.val)/2;var vp=q>0?((rpOrigKpi[q-1].vah+rpOrigKpi[q-1].val)/2):vm;var vd2=vm-vp;dvEl.textContent='\\u0394VA: '+(q>0?(vd2>=0?'+':'')+vd2.toFixed(5):'N/A');dvEl.setAttribute('fill',q>0?(vd2>=0?'#00bcd4':'#f44336'):'#888');}\n"
      );
      var1.write("      var rgEl=document.getElementById('rpRange_'+q);\n");
      var1.write("      if(rgEl)rgEl.textContent='Range: '+(ok.sh-ok.sl).toFixed(5);\n");
      var1.write("      var puEl=document.getElementById('rpPOCUp_'+q),pdEl=document.getElementById('rpPOCDn_'+q);\n");
      var1.write(
         "      if(puEl){var rng=ok.sh-ok.sl;if(rng>0){puEl.textContent='POC\\u2191: '+((ok.sh-ok.poc)/rng*100).toFixed(1)+'%';pdEl.textContent='POC\\u2193: '+((ok.poc-ok.sl)/rng*100).toFixed(1)+'%';}}\n"
      );
      var1.write("    }\n");
      var1.write("  }\n");
      var1.write("  var s=rpCurSes,ses=rpSessions[s],ok2=rpOrigKpi[s];\n");
      var1.write("  if(rpStep<ses.n){\n");
      var1.write("    var tv=0,bv=0,rv=0;\n");
      var1.write("    for(var i=0;i<rpStep;i++){\n");
      var1.write("      var bar=ses.bars[i];\n");
      var1.write("      for(var k in bar.d){var vol=bar.d[k];tv+=vol;if(bar.b)bv+=vol;else rv+=vol;}\n");
      var1.write("    }\n");
      var1.write("    var dv=bv-rv;\n");
      var1.write("    document.getElementById('rpVol_'+s).textContent='Vol: '+Math.round(tv);\n");
      var1.write("    document.getElementById('rpBull_'+s).textContent='Bull: '+Math.round(bv);\n");
      var1.write("    document.getElementById('rpBear_'+s).textContent='Bear: '+Math.round(rv);\n");
      var1.write("    var dEl2=document.getElementById('rpDelta_'+s);\n");
      var1.write("    dEl2.textContent='Delta: '+(dv>=0?'+':'')+Math.round(dv);\n");
      var1.write("    dEl2.setAttribute('fill',dv>=0?'#00bcd4':'#f44336');\n");
      var1.write("    var curPoc=ses.low+rpCurPocI*ses.bs+ses.bs/2;\n");
      var1.write("    var curVaL=ses.low+rpCurVaLo*ses.bs;\n");
      var1.write("    var curVaH=ses.low+(rpCurVaHi+1)*ses.bs;\n");
      var1.write("    var rng2=ok2.sh-ok2.sl;\n");
      var1.write("    var dpEl2=document.getElementById('rpDPOC_'+s);\n");
      var1.write(
         "    if(dpEl2){var pd2=s>0?(curPoc-rpOrigKpi[s-1].poc):0;dpEl2.textContent='\\u0394POC: '+(s>0?(pd2>=0?'+':'')+pd2.toFixed(5):'N/A');dpEl2.setAttribute('fill',s>0?(pd2>=0?'#00bcd4':'#f44336'):'#888');}\n"
      );
      var1.write("    var dvEl2=document.getElementById('rpDVA_'+s);\n");
      var1.write(
         "    if(dvEl2){var vm2=(curVaH+curVaL)/2;var vp2=s>0?((rpOrigKpi[s-1].vah+rpOrigKpi[s-1].val)/2):vm2;var vd3=vm2-vp2;dvEl2.textContent='\\u0394VA: '+(s>0?(vd3>=0?'+':'')+vd3.toFixed(5):'N/A');dvEl2.setAttribute('fill',s>0?(vd3>=0?'#00bcd4':'#f44336'):'#888');}\n"
      );
      var1.write("    var rgEl2=document.getElementById('rpRange_'+s);\n");
      var1.write("    if(rgEl2)rgEl2.textContent='Range: '+rng2.toFixed(5);\n");
      var1.write("    var puEl2=document.getElementById('rpPOCUp_'+s),pdEl3=document.getElementById('rpPOCDn_'+s);\n");
      var1.write(
         "    if(puEl2&&rng2>0){puEl2.textContent='POC\\u2191: '+((ok2.sh-curPoc)/rng2*100).toFixed(1)+'%';pdEl3.textContent='POC\\u2193: '+((curPoc-ok2.sl)/rng2*100).toFixed(1)+'%';}\n"
      );
      var1.write("  }\n");
      var1.write("}catch(e){}}\n");
      var1.write("function rpSetStep(s){var t=rpSessions[rpCurSes].n;rpStep=Math.max(0,Math.min(t,s));rpRender();}\n");
      var1.write("function rpSwitchSession(dir){\n");
      var1.write("  rpGoToSession(rpCurSes+dir);\n");
      var1.write("}\n");
      var1.write("function rpGoToSession(n){\n");
      var1.write("  if(rpPlaying){rpPlaying=false;clearInterval(rpTimer);rpTimer=null;rpPauseIco.style.display='none';}\n");
      var1.write("  var ns=Math.max(0,Math.min(rpN-1,n));\n");
      var1.write("  if(ns===rpCurSes)return;\n");
      var1.write("  rpStep=rpSessions[rpCurSes].n;rpRender();\n");
      var1.write("  rpCurSes=ns;\n");
      var1.write("  rpStep=rpSessions[rpCurSes].n;\n");
      var1.write("  rpRender();\n");
      var1.write("}\n");
      var1.write("function rpJumpSession(delta){\n");
      var1.write("  rpGoToSession(rpCurSes+delta);\n");
      var1.write("}\n");
      var1.write("function rpTogglePlay(){\n");
      var1.write("  if(rpPlaying){rpPlaying=false;clearInterval(rpTimer);rpTimer=null;}\n");
      var1.write("  else{if(rpStep>=rpSessions[rpCurSes].n)rpStep=0;rpPlaying=true;rpTimer=setInterval(function(){\n");
      var1.write("    rpStep++;if(rpStep>=rpSessions[rpCurSes].n){\n");
      var1.write("      if(rpCurSes<rpN-1){rpStep=rpSessions[rpCurSes].n;rpRender();rpCurSes++;rpStep=0;rpRender();}\n");
      var1.write("      else{rpPlaying=false;clearInterval(rpTimer);rpTimer=null;rpPauseIco.style.display='none';rpRender();}\n");
      var1.write("      return;}\n");
      var1.write("    rpRender();\n");
      var1.write("  },300);}\n");
      var1.write("  rpPauseIco.style.display=rpPlaying?'':'none';\n");
      var1.write("}\n");
      var1.write("document.getElementById('rpSesFirst').addEventListener('click',function(){rpGoToSession(0);});\n");
      var1.write("document.getElementById('rpSesPrev10').addEventListener('click',function(){rpJumpSession(-10);});\n");
      var1.write("document.getElementById('rpFirst').addEventListener('click',function(){rpSwitchSession(-1);});\n");
      var1.write("document.getElementById('rpPrev').addEventListener('click',function(){rpSetStep(rpStep-1);});\n");
      var1.write("document.getElementById('rpPlay').addEventListener('click',function(){rpTogglePlay();});\n");
      var1.write("document.getElementById('rpNext').addEventListener('click',function(){rpSetStep(rpStep+1);});\n");
      var1.write("document.getElementById('rpLast').addEventListener('click',function(){rpSwitchSession(1);});\n");
      var1.write("document.getElementById('rpSesNext10').addEventListener('click',function(){rpJumpSession(10);});\n");
      var1.write("document.getElementById('rpSesLast').addEventListener('click',function(){rpGoToSession(rpN-1);});\n");
      var1.write("var rpSesInput=document.getElementById('rpSesInput');\n");
      var1.write("if(rpSesInput){\n");
      var1.write("  rpSesInput.addEventListener('mousedown',function(e){e.stopPropagation();});\n");
      var1.write("  rpSesInput.addEventListener('click',function(e){e.stopPropagation();this.focus();});\n");
      var1.write("  rpSesInput.addEventListener('keydown',function(e){\n");
      var1.write("    if(e.key==='Enter'){e.preventDefault();var v=parseInt(this.value,10);\n");
      var1.write("      if(!isNaN(v))rpGoToSession(v-1);}\n");
      var1.write("    e.stopPropagation();\n");
      var1.write("  });\n");
      var1.write("  rpSesInput.addEventListener('keyup',function(e){e.stopPropagation();});\n");
      var1.write("}\n");
      var1.write("var rpDragging=false;\n");
      var1.write("function rpSliderPos(e){\n");
      var1.write("  var pt=svgPt(e);\n");
      var1.write("  var pct=Math.max(0,Math.min(1,(pt.x-rpSlX)/rpSlW));\n");
      var1.write("  rpSetStep(Math.round(pct*rpSessions[rpCurSes].n));\n");
      var1.write("}\n");
      var1.write("document.getElementById('rpSliderHit').addEventListener('mousedown',function(e){rpDragging=true;rpSliderPos(e);});\n");
      var1.write("document.getElementById('rpSliderKnob').addEventListener('mousedown',function(e){rpDragging=true;});\n");
      var1.write("svg.addEventListener('mousemove',function(e){if(rpDragging)rpSliderPos(e);});\n");
      var1.write("svg.addEventListener('mouseup',function(){rpDragging=false;});\n");
      var1.write("document.addEventListener('keydown',function(e){\n");
      var1.write("  if(e.target&&e.target.tagName==='INPUT')return;\n");
      var1.write("  if(e.ctrlKey&&e.key==='Home'){e.preventDefault();rpGoToSession(0);return;}\n");
      var1.write("  if(e.ctrlKey&&e.key==='End'){e.preventDefault();rpGoToSession(rpN-1);return;}\n");
      var1.write("  if(e.key==='ArrowLeft')rpSetStep(rpStep-1);\n");
      var1.write("  else if(e.key==='ArrowRight')rpSetStep(rpStep+1);\n");
      var1.write("  else if(e.key===' '){e.preventDefault();rpTogglePlay();}\n");
      var1.write("  else if(e.key==='Home'){e.preventDefault();rpSetStep(0);}\n");
      var1.write("  else if(e.key==='End'){e.preventDefault();rpSetStep(rpSessions[rpCurSes].n);}\n");
      var1.write("  else if(e.key==='PageUp'){e.preventDefault();rpSwitchSession(-1);}\n");
      var1.write("  else if(e.key==='PageDown'){e.preventDefault();rpSwitchSession(1);}\n");
      var1.write("});\n");
      var1.write("rpUpdateUI();\n");
      var1.write("}\n");
      var1.write("hilite();})();\n");
      var1.write("]]></script>\n");
   }

   protected long getIBPeriodMillis() {
      if (this.cfgIBMinutes() > 0) {
         return this.cfgIBMinutes() * 60L * 1000L;
      }

      byte var1;
      if (this.cfgSessionType() == 1 || this.cfgSessionType() == 5) {
         var1 = 1;
      } else if (this.cfgSessionType() == 2 || this.cfgSessionType() == 6) {
         var1 = 2;
      } else if (this.cfgSessionType() == 3 || this.cfgSessionType() == 7) {
         var1 = 3;
      } else if (this.cfgSessionType() == 4 || this.cfgSessionType() == 8) {
         var1 = 4;
      } else if (this.cfgSessionType() != 9 && this.cfgSessionType() != 10) {
         var1 = 1;
      } else {
         var1 = 5;
      }

      switch (var1) {
         case 1:
            return 3600000L;
         case 2:
            return 43200000L;
         case 3:
            return 86400000L;
         case 4:
            return 518400000L;
         case 5:
            return 3600000L;
         default:
            return 3600000L;
      }
   }

   protected double computePivotThreshold() throws TradingException {
      double var1 = this.cfgChart().getInstrumentInfo().tickStep;
      switch (this.cfgPivotMethod()) {
         case 2:
            return this.cfgPivotTicks() * var1;
         case 3:
            return this.computeATR(this.cfgPivotATRPeriod()) * this.cfgPivotATRMultiple();
         default:
            double var3 = this.zzDirection == 1 ? this.zzPivotHigh : this.zzPivotLow;
            if (var3 <= 0.0) {
               var3 = this.cfgChart().Close(0);
            }

            return var3 * (this.cfgPivotPct() / 100.0);
      }
   }

   protected double computeATR(int var1) throws TradingException {
      int var2 = Math.min(var1, this.CurrentBar);
      if (var2 <= 0) {
         return this.cfgChart().High(0) - this.cfgChart().Low(0);
      }

      double var3 = 0.0;

      for (int var5 = 0; var5 < var2; var5++) {
         double var6 = this.cfgChart().High(var5);
         double var8 = this.cfgChart().Low(var5);
         double var10 = var5 + 1 <= this.CurrentBar ? this.cfgChart().Close(var5 + 1) : var6;
         double var12 = Math.max(var6 - var8, Math.max(Math.abs(var6 - var10), Math.abs(var8 - var10)));
         var3 += var12;
      }

      return var3 / var2;
   }

   protected boolean detectZigZagPivot() throws TradingException {
      long var1 = this.cfgChart().Time(0);
      double var3 = this.cfgChart().High(0);
      double var5 = this.cfgChart().Low(0);
      if (this.zzDirection == 0) {
         this.zzPivotHigh = var3;
         this.zzPivotLow = var5;
         this.zzPivotHighTime = var1;
         this.zzPivotLowTime = var1;
         this.zzDirection = 1;
         this.zzSessionStart = var1;
         return false;
      }

      boolean var7 = false;
      double var8 = this.computePivotThreshold();
      if (this.zzDirection == 1) {
         if (var3 > this.zzPivotHigh) {
            this.zzPivotHigh = var3;
            this.zzPivotHighTime = var1;
         }

         if (this.zzPivotHigh - var5 >= var8) {
            this.zzLastPivotPrice = this.zzPivotHigh;
            this.zzLastPivotDir = 1;
            this.zzLastPivotTime = this.zzPivotHighTime;
            this.zzDirection = -1;
            this.zzPivotLow = var5;
            this.zzPivotLowTime = var1;
            var7 = true;
         }
      } else {
         if (var5 < this.zzPivotLow) {
            this.zzPivotLow = var5;
            this.zzPivotLowTime = var1;
         }

         if (var3 - this.zzPivotLow >= var8) {
            this.zzLastPivotPrice = this.zzPivotLow;
            this.zzLastPivotDir = -1;
            this.zzLastPivotTime = this.zzPivotLowTime;
            this.zzDirection = 1;
            this.zzPivotHigh = var3;
            this.zzPivotHighTime = var1;
            var7 = true;
         }
      }

      return var7;
   }

   protected void applyClusterEnhancement(int var1, double var2, double var4, double var6) {
      double var8 = var6 / Math.max(1, var1);
      int[] var10 = new int[var1];
      double[] var11 = new double[var1];
      int var12 = 0;

      for (int var13 = 0; var13 < var1; var13++) {
         boolean var14 = var13 == 0 || this.volumeBins[var13] >= this.volumeBins[var13 - 1];
         boolean var15 = var13 == var1 - 1 || this.volumeBins[var13] >= this.volumeBins[var13 + 1];
         if (var14 && var15 && this.volumeBins[var13] > var8) {
            var10[var12] = var13;
            var11[var12] = this.volumeBins[var13];
            var12++;
         }
      }

      int var31 = Math.min(this.cfgMaxClusterCenters(), var12);
      if (var31 == 0) {
         this.prevVPOC = this.prevPOC;
         this.prevVVAH = this.prevVAH;
         this.prevVVAL = this.prevVAL;
      } else {
         for (int var32 = 0; var32 < var31; var32++) {
            int var34 = var32;

            for (int var16 = var32 + 1; var16 < var12; var16++) {
               if (var11[var16] > var11[var34]) {
                  var34 = var16;
               }
            }

            if (var34 != var32) {
               int var35 = var10[var32];
               var10[var32] = var10[var34];
               var10[var34] = var35;
               double var17 = var11[var32];
               var11[var32] = var11[var34];
               var11[var34] = var17;
            }
         }

         double var33 = this.cfgClusterSpread();
         if (this.clusterBins == null || this.clusterBins.length < var1) {
            this.clusterBins = new double[var1];
         }

         for (int var36 = 0; var36 < var1; var36++) {
            this.clusterBins[var36] = 0.0;
         }

         for (int var37 = 0; var37 < var31; var37++) {
            int var39 = var10[var37];
            double var18 = var11[var37];

            for (int var20 = 0; var20 < var1; var20++) {
               double var21 = (var20 - var39) / var33;
               double var23 = Math.exp(-0.5 * var21 * var21);
               this.clusterBins[var20] = this.clusterBins[var20] + var18 * var23;
            }
         }

         int var38 = 0;
         double var40 = this.clusterBins[0];

         for (int var19 = 1; var19 < var1; var19++) {
            if (this.clusterBins[var19] > var40) {
               var40 = this.clusterBins[var19];
               var38 = var19;
            }
         }

         this.prevVPOC = var4 + (var38 + 0.5) * var2;
         double var41 = 0.0;

         for (int var42 = 0; var42 < var1; var42++) {
            var41 += this.clusterBins[var42];
         }

         double var43 = var41 * (this.cfgValueAreaPct() / 100.0);
         double var44 = this.clusterBins[var38];
         int var25 = var38;
         int var26 = var38;

         while (var44 < var43 && (var25 > 0 || var26 < var1 - 1)) {
            double var27 = var25 > 0 ? this.clusterBins[var25 - 1] : 0.0;
            double var29 = var26 < var1 - 1 ? this.clusterBins[var26 + 1] : 0.0;
            if (var27 >= var29 && var25 > 0) {
               var44 += this.clusterBins[--var25];
            } else if (var26 < var1 - 1) {
               var44 += this.clusterBins[++var26];
            } else {
               var44 += this.clusterBins[--var25];
            }
         }

         this.prevVVAL = var4 + var25 * var2;
         this.prevVVAH = var4 + (var26 + 1) * var2;
      }
   }

   @Override
   protected abstract ChartData cfgChart();

   @Override
   protected abstract int cfgBinSizeMode();

   @Override
   protected abstract int cfgProfileRows();

   protected abstract int cfgTicksPerBin();

   protected abstract double cfgValueAreaPct();

   protected abstract int cfgHvnCount();

   protected abstract int cfgHvnThresholdPct();

   protected abstract int cfgLvnThresholdPct();

   protected abstract boolean cfgEnableLVN();

   protected abstract boolean cfgEnableVCP();

   protected abstract double cfgClusterSpread();

   protected abstract int cfgMaxClusterCenters();

   protected abstract int cfgIBMinutes();

   protected abstract int cfgSessionType();

   protected String cfgSessionLabel() {
      return null;
   }

   protected String cfgCurrentSessionLabel() {
      return null;
   }

   @Override
   protected abstract boolean cfgShowCandlesticks();

   protected abstract boolean cfgShowVolumeSubchart();

   protected abstract int cfgVolumeMALength();

   protected abstract boolean cfgShowPOCDelta();

   protected abstract boolean cfgShowVADelta();

   protected abstract boolean cfgShowProfileRange();

   protected abstract boolean cfgShowPOCPosition();

   protected abstract boolean cfgShowDeltaPerLevel();

   protected boolean cfgShowZigZagLine() {
      return false;
   }

   protected abstract int cfgPivotMethod();

   protected abstract double cfgPivotPct();

   protected abstract int cfgPivotTicks();

   protected abstract double cfgPivotATRMultiple();

   protected abstract int cfgPivotATRPeriod();
}
