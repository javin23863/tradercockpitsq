package SQ.Internal;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.ChartData;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public abstract class TPOProfileIndicatorChart extends AbstractChart {
   private static final int MAX_SVG_BINS_PER_SESSION = 100;
   protected static final String DEFAULT_DARK_BG_COLOR = "#1a1a2e";
   protected static final String DEFAULT_DARK_PLOT_COLOR = "#16213e";
   protected static final String DEFAULT_LIGHT_BG_COLOR = "#e0e0e0";
   protected static final String DEFAULT_LIGHT_PLOT_COLOR = "#faf5eb";
   protected static final int DEFAULT_DARK_CANDLE_OPACITY = 100;
   protected static final int DEFAULT_LIGHT_CANDLE_OPACITY = 60;
   protected static final int DEFAULT_DARK_PROFILE_OPACITY = 50;
   protected static final int DEFAULT_LIGHT_PROFILE_OPACITY = 80;
   protected static final int DEFAULT_TPO_FONT_SIZE = 12;
   protected static final int DEFAULT_DELTA_FONT_SIZE = 11;
   protected static final int DEFAULT_CHART_HEIGHT = 900;
   protected static final int DEFAULT_SESSION_WIDTH = 1200;
   protected static final String[] TPO_LETTER_PALETTE = new String[]{
      "#e6194b",
      "#3cb44b",
      "#ffe119",
      "#4363d8",
      "#f58231",
      "#911eb4",
      "#42d4f4",
      "#f032e6",
      "#bfef45",
      "#fabed4",
      "#469990",
      "#dcbeff",
      "#9A6324",
      "#fffac8",
      "#800000",
      "#aaffc3",
      "#808000",
      "#ffd8b1",
      "#000075",
      "#a9a9a9",
      "#e6beff",
      "#1abc9c",
      "#2ecc71",
      "#3498db",
      "#9b59b6",
      "#34495e",
      "#f39c12",
      "#d35400",
      "#c0392b",
      "#7f8c8d",
      "#27ae60",
      "#2980b9",
      "#8e44ad",
      "#2c3e50",
      "#e74c3c",
      "#90b9e0",
      "#58d68d",
      "#f5b041",
      "#af7ac5",
      "#5dade2",
      "#48c9b0",
      "#eb984e",
      "#f1948a",
      "#85929e",
      "#73c6b6",
      "#82e0aa",
      "#f7dc6f",
      "#bb8fce",
      "#aed6f1",
      "#f0b27a",
      "#d7bde2",
      "#a3e4d7",
      "#fad7a0",
      "#d5f5e3",
      "#fadbd8",
      "#d6eaf8",
      "#e8daef",
      "#fdebd0",
      "#d4efdf",
      "#f2d7d5",
      "#d0ece7",
      "#f9e79f"
   };
   protected double prevTPO_POC = 0.0;
   protected double prevTPO_VAH = 0.0;
   protected double prevTPO_VAL = 0.0;
   protected double prevTPO_VPOC = 0.0;
   protected double prevTPO_VVAH = 0.0;
   protected double prevTPO_VVAL = 0.0;
   protected double prevTPO_BullPOC = 0.0;
   protected double prevTPO_BearPOC = 0.0;
   protected int[] bullTpoBins;
   protected int[] bearTpoBins;
   protected int[] tpoBins;
   protected long[] tpoMask;
   protected long[] bracketFirstTime = new long[62];
   protected long[] bracketLastTime = new long[62];
   protected double cumVolForBrackets = 0.0;
   protected int maxUsedBracket = -1;
   protected int tpoBracketCount = 0;
   protected int lastTpoPocIndex = -1;
   protected int lastSinglePrintCount = 0;
   protected int lastTpoHighBin = -1;
   protected int lastTpoLowBin = -1;
   protected boolean lastPoorHigh = false;
   protected boolean lastPoorLow = false;
   protected boolean lastExcessHigh = false;
   protected boolean lastExcessLow = false;
   protected double lastEffectiveBracketVolume = 0.0;
   protected long lastEffectiveBracketMillis = 0L;
   protected int lastBuyingTailLen = 0;
   protected int lastSellingTailLen = 0;
   protected int lastProfileShape = 5;
   protected double lastLedgeUpPrice = 0.0;
   protected double lastLedgeDownPrice = 0.0;
   protected double[] histTPO_POC;
   protected double[] histTPO_VAH;
   protected double[] histTPO_VAL;
   protected double[] histBinSize;
   protected int[][] histTpoBins;
   protected long[][] histTpoMask;
   protected int[] histTpoBracketCount;
   protected int[] histProfileShape;

   @Override
   protected void ensureArrays() {
      int var1 = this.cfgBinSizeMode() == 2 ? 2000 : this.cfgProfileRows();
      if (this.tpoBins == null || this.tpoBins.length < var1) {
         this.tpoBins = new int[var1];
      }

      if (this.tpoMask == null || this.tpoMask.length < var1) {
         this.tpoMask = new long[var1];
      }

      if (this.bracketFirstTime == null || this.bracketFirstTime.length != 62) {
         this.bracketFirstTime = new long[62];
      }

      if (this.bracketLastTime == null || this.bracketLastTime.length != 62) {
         this.bracketLastTime = new long[62];
      }

      if (this.clusterBins == null || this.clusterBins.length < var1) {
         this.clusterBins = new double[var1];
      }

      if (this.bullTpoBins == null || this.bullTpoBins.length < var1) {
         this.bullTpoBins = new int[var1];
      }

      if (this.bearTpoBins == null || this.bearTpoBins.length < var1) {
         this.bearTpoBins = new int[var1];
      }
   }

   protected void ensureHistory() {
      byte var1 = 20;
      int var2 = this.cfgBinSizeMode() == 2 ? 2000 : this.cfgProfileRows();
      if (this.histSessionStart == null || this.histSessionStart.length < var1) {
         this.histSessionStart = new long[var1];
         this.histSessionEnd = new long[var1];
         this.histTPO_POC = new double[var1];
         this.histBinSize = new double[var1];
         this.histTPO_VAH = new double[var1];
         this.histTPO_VAL = new double[var1];
         this.histIBH = new double[var1];
         this.histIBL = new double[var1];
         this.histSessionHigh = new double[var1];
         this.histSessionLow = new double[var1];
         this.histTpoBins = new int[var1][var2];
         this.histTpoMask = new long[var1][var2];
         this.histTpoBracketCount = new int[var1];
         this.histProfileShape = new int[var1];
         this.histNumBins = new int[var1];
         this.histTotalVolume = new double[var1];
         this.histBullVolume = new double[var1];
         this.histBearVolume = new double[var1];
         this.historyCount = 0;
      }
   }

   protected void growHistory() {
   }

   protected void pushSessionHistory(double var1, double var3, double var5) {
      this.ensureHistory();
      int var7 = this.lastNumBins;
      if (this.historyCount > 0 && this.histSessionStart[this.historyCount - 1] == this.prevSessionStart) {
         int var10 = this.historyCount - 1;
         this.histSessionEnd[var10] = this.prevSessionEnd;
         this.histTPO_POC[var10] = this.prevTPO_POC;
         this.histTPO_VAH[var10] = this.prevTPO_VAH;
         this.histTPO_VAL[var10] = this.prevTPO_VAL;
         this.histIBH[var10] = this.prevIBH;
         this.histIBL[var10] = this.prevIBL;
         this.histSessionHigh[var10] = var1;
         this.histSessionLow[var10] = var3;
         this.histNumBins[var10] = var7;
         this.histBinSize[var10] = var5;
         System.arraycopy(this.tpoBins, 0, this.histTpoBins[var10], 0, var7);
         System.arraycopy(this.tpoMask, 0, this.histTpoMask[var10], 0, var7);
         this.histTpoBracketCount[var10] = this.tpoBracketCount;
         this.histProfileShape[var10] = this.lastProfileShape;
         this.histTotalVolume[var10] = this.prevTotalVolume;
         this.histBullVolume[var10] = this.prevTotalBullVolume;
         this.histBearVolume[var10] = this.prevTotalBearVolume;
      } else {
         if (this.historyCount >= this.histSessionStart.length) {
            int var8 = this.histSessionStart.length;
            System.arraycopy(this.histSessionStart, 1, this.histSessionStart, 0, var8 - 1);
            System.arraycopy(this.histSessionEnd, 1, this.histSessionEnd, 0, var8 - 1);
            System.arraycopy(this.histTPO_POC, 1, this.histTPO_POC, 0, var8 - 1);
            System.arraycopy(this.histBinSize, 1, this.histBinSize, 0, var8 - 1);
            System.arraycopy(this.histTPO_VAH, 1, this.histTPO_VAH, 0, var8 - 1);
            System.arraycopy(this.histTPO_VAL, 1, this.histTPO_VAL, 0, var8 - 1);
            System.arraycopy(this.histIBH, 1, this.histIBH, 0, var8 - 1);
            System.arraycopy(this.histIBL, 1, this.histIBL, 0, var8 - 1);
            System.arraycopy(this.histSessionHigh, 1, this.histSessionHigh, 0, var8 - 1);
            System.arraycopy(this.histSessionLow, 1, this.histSessionLow, 0, var8 - 1);
            System.arraycopy(this.histNumBins, 1, this.histNumBins, 0, var8 - 1);
            System.arraycopy(this.histTotalVolume, 1, this.histTotalVolume, 0, var8 - 1);
            System.arraycopy(this.histBullVolume, 1, this.histBullVolume, 0, var8 - 1);
            System.arraycopy(this.histBearVolume, 1, this.histBearVolume, 0, var8 - 1);
            System.arraycopy(this.histTpoBracketCount, 1, this.histTpoBracketCount, 0, var8 - 1);
            System.arraycopy(this.histProfileShape, 1, this.histProfileShape, 0, var8 - 1);
            System.arraycopy(this.histTpoBins, 1, this.histTpoBins, 0, var8 - 1);
            System.arraycopy(this.histTpoMask, 1, this.histTpoMask, 0, var8 - 1);
            this.historyCount = var8 - 1;
         }

         int var9 = this.historyCount;
         this.histSessionStart[var9] = this.prevSessionStart;
         this.histSessionEnd[var9] = this.prevSessionEnd;
         this.histTPO_POC[var9] = this.prevTPO_POC;
         this.histTPO_VAH[var9] = this.prevTPO_VAH;
         this.histTPO_VAL[var9] = this.prevTPO_VAL;
         this.histIBH[var9] = this.prevIBH;
         this.histIBL[var9] = this.prevIBL;
         this.histSessionHigh[var9] = var1;
         this.histSessionLow[var9] = var3;
         this.histNumBins[var9] = var7;
         this.histBinSize[var9] = var5;
         System.arraycopy(this.tpoBins, 0, this.histTpoBins[var9], 0, var7);
         System.arraycopy(this.tpoMask, 0, this.histTpoMask[var9], 0, var7);
         this.histTpoBracketCount[var9] = this.tpoBracketCount;
         this.histProfileShape[var9] = this.lastProfileShape;
         this.histTotalVolume[var9] = this.prevTotalVolume;
         this.histBullVolume[var9] = this.prevTotalBullVolume;
         this.histBearVolume[var9] = this.prevTotalBearVolume;
         this.historyCount++;
      }
   }

   @Override
   protected void OnDeinit() throws TradingException {
      super.OnDeinit();
      this.tpoBins = null;
      this.tpoMask = null;
      this.bullTpoBins = null;
      this.bearTpoBins = null;
      this.clusterBins = null;
      this.histTpoBins = null;
      this.histTpoMask = null;
      this.histSessionStart = null;
      this.histSessionEnd = null;
      this.histTPO_POC = null;
      this.histTPO_VAH = null;
      this.histTPO_VAL = null;
      this.histBinSize = null;
      this.histIBH = null;
      this.histIBL = null;
      this.histSessionHigh = null;
      this.histSessionLow = null;
      this.histNumBins = null;
      this.histTotalVolume = null;
      this.histBullVolume = null;
      this.histBearVolume = null;
      this.histTpoBracketCount = null;
      this.histProfileShape = null;
   }

   protected void calculateTPOProfile() throws TradingException {
      this.debug("VP", "--- calculateTPOProfile START ---");
      this.debug("VP", "Looking for bars between:");
      this.debug("VP", "  prevSessionStart = " + SQTime.toFullDateTimeString(this.prevSessionStart));
      this.debug("VP", "  prevSessionEnd   = " + SQTime.toFullDateTimeString(this.prevSessionEnd));
      if (this.cfgChart() == null) {
         this.debug("VP", "ERROR: cfgChart() is NULL!");
      } else {
         double var1 = Double.MIN_VALUE;
         double var3 = Double.MAX_VALUE;
         int var5 = 0;
         double var6 = 0.0;
         int var8 = 0;
         long var9 = 0L;
         long var11 = 0L;
         long var13 = this.prevSessionStart + this.getIBPeriodMillis();
         double var15 = Double.MIN_VALUE;
         double var17 = Double.MAX_VALUE;
         int var19 = 0;

         try {
            while (true) {
               long var20 = this.cfgChart().Time(var19);
               var8++;
               if (var20 < this.prevSessionStart) {
                  this.debug("VP", "Pass1: Stopped at index " + var19 + ", barTime=" + SQTime.toFullDateTimeString(var20) + " < prevSessionStart");
                  break;
               }

               if (var20 >= this.prevSessionStart && var20 < this.prevSessionEnd) {
                  if (this.isSunday(var20)) {
                     var19++;
                     continue;
                  }

                  double var22 = this.cfgChart().High(var19);
                  double var24 = this.cfgChart().Low(var19);
                  var1 = Math.max(var1, var22);
                  var3 = Math.min(var3, var24);
                  if (var5 == 0) {
                     var9 = var20;
                  }

                  var11 = var20;
                  var5++;
                  var6 += this.cfgChart().Volume(var19);
                  if (var20 < var13) {
                     var15 = Math.max(var15, var22);
                     var17 = Math.min(var17, var24);
                  }
               }

               var19++;
            }
         } catch (Exception var71) {
            this.debug("VP", "Pass1: Exception at index " + var19 + " (end of data): " + var71.getMessage());
         }

         this.debug("VP", "=== PASS 1 SUMMARY ===");
         this.debug("VP", "  Total M1 bars scanned: " + var8);
         this.debug("VP", "  Bars in session: " + var5);
         this.debug("VP", "  First session bar: " + SQTime.toFullDateTimeString(var9));
         this.debug("VP", "  Last session bar: " + SQTime.toFullDateTimeString(var11));
         this.debug("VP", "  Session HIGH (from highs): " + var1);
         this.debug("VP", "  Session LOW (from lows): " + var3);
         if (var5 == 0) {
            this.debug("VP", "ERROR: No bars found in session! Check session boundaries and M1 data availability");
         } else if (var1 <= var3) {
            this.debug("VP", "ERROR: Invalid range - sessionHigh(" + var1 + ") <= sessionLow(" + var3 + ")");
         } else {
            if (var15 > var17) {
               this.prevIBH = var15;
               this.prevIBL = var17;
            }

            double var73 = var1 - var3;
            double var74 = this.cfgChart().getInstrumentInfo().tickStep;
            double var25;
            int var76;
            if (this.cfgBinSizeMode() == 2) {
               var25 = this.cfgTicksPerBin() * var74;
               var76 = (int)Math.ceil(var73 / var25);
               var76 = Math.max(1, Math.min(var76, 2000));
            } else {
               var76 = this.cfgProfileRows();
               var25 = var73 / var76;
            }

            this.lastNumBins = var76;
            if (this.tpoBins == null || this.tpoBins.length < var76) {
               this.tpoBins = new int[var76];
            }

            this.debug("VP", "=== BIN CALCULATION ===");
            this.debug("VP", "  Range = " + var73);
            this.debug("VP", "  numBins = " + var76);
            this.debug("VP", "  binSize = " + var25);
            this.debug("VP", "  Bin 0 covers: " + var3 + " to " + (var3 + var25));
            this.debug("VP", "  Bin " + (var76 - 1) + " covers: " + (var3 + (var76 - 1) * var25) + " to " + var1);

            for (int var27 = 0; var27 < var76; var27++) {
               this.tpoBins[var27] = 0;
            }

            if (this.bullTpoBins == null || this.bullTpoBins.length < var76) {
               this.bullTpoBins = new int[var76];
            }

            if (this.bearTpoBins == null || this.bearTpoBins.length < var76) {
               this.bearTpoBins = new int[var76];
            }

            for (int var77 = 0; var77 < var76; var77++) {
               this.bullTpoBins[var77] = 0;
               this.bearTpoBins[var77] = 0;
            }

            double var78 = 0.0;
            double var29 = 0.0;
            double var31 = 0.0;
            long var33 = this.getBracketMillis();
            long var36 = Math.max(0L, this.prevSessionEnd - this.prevSessionStart);
            int var35 = (int)Math.ceil((double)var36 / var33);
            var35 = clampInt(var35, 1, 62);
            this.lastEffectiveBracketVolume = 0.0;
            this.lastEffectiveBracketMillis = var33;
            this.tpoBracketCount = var35;
            if (this.tpoMask == null || this.tpoMask.length < var76) {
               this.tpoMask = new long[var76];
            }

            for (int var38 = 0; var38 < var76; var38++) {
               this.tpoMask[var38] = 0L;
            }

            this.cumVolForBrackets = 0.0;
            this.maxUsedBracket = -1;

            for (int var80 = 0; var80 < 62; var80++) {
               this.bracketFirstTime[var80] = 0L;
               this.bracketLastTime[var80] = 0L;
            }

            var19 = 0;
            int var81 = 0;
            int var39 = 0;
            long var40 = this.prevSessionEnd;

            try {
               while (true) {
                  long var42 = this.cfgChart().Time(var19);
                  if (var42 < this.prevSessionStart) {
                     break;
                  }

                  if (var42 >= this.prevSessionStart && var42 < this.prevSessionEnd) {
                     if (this.isSunday(var42)) {
                        var19++;
                        continue;
                     }

                     double var44 = this.cfgChart().Volume(var19);
                     var78 += var44;
                     boolean var46 = this.cfgChart().Close(var19) >= this.cfgChart().Open(var19);
                     if (var46) {
                        var29 += var44;
                     } else {
                        var31 += var44;
                     }

                     long var47 = Math.min(var40, this.prevSessionEnd);
                     int var49 = (int)((var42 - this.prevSessionStart) / var33);
                     int var50 = (int)((Math.max(var47 - 1L, var42) - this.prevSessionStart) / var33);
                     var49 = clampInt(var49, 0, 61);
                     var50 = clampInt(var50, 0, 61);
                     long var51 = 0L;

                     for (int var53 = var49; var53 <= var50; var53++) {
                        var51 |= 1L << var53;
                        if (this.bracketFirstTime[var53] == 0L) {
                           this.bracketFirstTime[var53] = var42;
                        }

                        this.bracketLastTime[var53] = var42;
                        if (var53 > this.maxUsedBracket) {
                           this.maxUsedBracket = var53;
                        }
                     }

                     double var54 = this.cfgChart().Low(var19);
                     double var56 = this.cfgChart().High(var19);
                     double var58 = this.cfgChart().Close(var19);
                     double var60 = this.cfgChart().Open(var19);
                     boolean var62 = var58 >= var60;
                     int var63 = (int)((var54 - var3) / var25);
                     int var64 = (int)((var56 - var3) / var25);
                     var63 = Math.max(0, Math.min(var76 - 1, var63));
                     var64 = Math.max(0, Math.min(var76 - 1, var64));
                     if (var64 < var63) {
                        int var65 = var64;
                        var64 = var63;
                        var63 = var65;
                     }

                     for (int var105 = var63; var105 <= var64; var105++) {
                        long var66 = this.tpoMask[var105];
                        long var68 = var66 | var51;
                        var39 += Long.bitCount(var68) - Long.bitCount(var66);
                        this.tpoMask[var105] = var68;
                        if (var62) {
                           this.bullTpoBins[var105]++;
                        } else {
                           this.bearTpoBins[var105]++;
                        }
                     }

                     if (var81 <= 5 || var81 % 500 == 0) {
                        this.debug(
                           "VP",
                           "Pass2 #"
                              + var81
                              + ": time="
                              + SQTime.toFullDateTimeString(var42)
                              + ", vol="
                              + var44
                              + ", brackets="
                              + var49
                              + "-"
                              + var50
                              + "/"
                              + var35
                        );
                     }

                     var81++;
                  }

                  var40 = var42;
                  var19++;
               }
            } catch (Exception var70) {
               this.debug("VP", "Pass2: Exception at index " + var19 + " (end of data)");
            }

            long var82 = 0L;
            int var83 = 0;
            int var45 = -1;
            int var84 = -1;

            for (int var85 = 0; var85 < var76; var85++) {
               int var48 = Long.bitCount(this.tpoMask[var85]);
               this.tpoBins[var85] = var48;
               var82 += var48;
               if (var48 > 0) {
                  if (var84 == -1) {
                     var84 = var85;
                  }

                  var45 = var85;
                  if (var48 == 1) {
                     var83++;
                  }
               }
            }

            this.lastSinglePrintCount = var83;
            this.lastTpoHighBin = var45;
            this.lastTpoLowBin = var84;
            this.lastExcessHigh = var45 >= 0 && this.tpoBins[var45] == 1;
            this.lastExcessLow = var84 >= 0 && this.tpoBins[var84] == 1;
            this.lastPoorHigh = var45 >= 0 && this.tpoBins[var45] >= 2;
            this.lastPoorLow = var84 >= 0 && this.tpoBins[var84] >= 2;
            int var86 = 0;
            if (var84 >= 0) {
               for (int var87 = var84; var87 <= var45 && this.tpoBins[var87] == 1; var87++) {
                  var86++;
               }
            }

            this.lastBuyingTailLen = var86;
            int var88 = 0;
            if (var45 >= 0) {
               for (int var90 = var45; var90 >= var84 && this.tpoBins[var90] == 1; var90--) {
                  var88++;
               }
            }

            this.lastSellingTailLen = var88;
            this.lastProfileShape = this.detectProfileShape(var84, var45);
            this.lastLedgeUpPrice = 0.0;
            this.lastLedgeDownPrice = 0.0;
            if (var84 >= 0 && var45 > var84) {
               int var91 = 0;
               int var94 = -1;
               int var96 = 0;
               int var52 = -1;
               int var99 = var84;

               for (int var101 = var84 + 1; var101 <= var45; var101++) {
                  if (this.tpoBins[var101] == this.tpoBins[var99] && this.tpoBins[var101] > 0) {
                     int var55 = var101 - var99 + 1;
                     int var102 = var84 + (var45 - var84) / 2;
                     if (var101 >= var102 && var55 > var91 && var55 >= 3) {
                        var91 = var55;
                        var94 = var101;
                     }

                     if (var99 <= var102 && var55 > var96 && var55 >= 3) {
                        var96 = var55;
                        var52 = var99;
                     }
                  } else {
                     var99 = var101;
                  }
               }

               if (var94 >= 0) {
                  this.lastLedgeUpPrice = var3 + (var94 + 0.5) * var25;
               }

               if (var52 >= 0) {
                  this.lastLedgeDownPrice = var3 + (var52 + 0.5) * var25;
               }
            }

            this.debug(
               "VP",
               "Pass2 complete: volumeAssignments="
                  + var81
                  + ", totalVolume="
                  + var78
                  + ", bracketCount="
                  + var35
                  + ", tpoTouches(unique)="
                  + var39
                  + ", totalTPO="
                  + var82
                  + ", singlePrintBins="
                  + var83
                  + ", hiBin="
                  + var45
                  + ", loBin="
                  + var84
            );
            this.calculateTPOLevels(var82, var25, var3, var76);
            this.prevTPO_VPOC = this.prevTPO_POC;
            this.prevTPO_VVAH = this.prevTPO_VAH;
            this.prevTPO_VVAL = this.prevTPO_VAL;
            int var92 = 0;
            int var95 = this.bullTpoBins[0];
            int var97 = 0;
            int var98 = this.bearTpoBins[0];

            for (int var100 = 1; var100 < var76; var100++) {
               if (this.bullTpoBins[var100] > var95) {
                  var95 = this.bullTpoBins[var100];
                  var92 = var100;
               }

               if (this.bearTpoBins[var100] > var98) {
                  var98 = this.bearTpoBins[var100];
                  var97 = var100;
               }
            }

            this.prevTPO_BullPOC = var95 > 0 ? var3 + (var92 + 0.5) * var25 : 0.0;
            this.prevTPO_BearPOC = var98 > 0 ? var3 + (var97 + 0.5) * var25 : 0.0;
            this.prevTotalVolume = var78;
            this.prevTotalBullVolume = var29;
            this.prevTotalBearVolume = var31;
            this.debug("VP", "--- calculateTPOProfile END ---");
            this.debug("VP", "FINAL RESULTS: TPO_POC=" + this.prevTPO_POC + ", TPO_VAH=" + this.prevTPO_VAH + ", TPO_VAL=" + this.prevTPO_VAL);
            if (this.cfgStoreChartData()) {
               this.pushSessionHistory(var1, var3, var25);
            }
         }
      }
   }

   protected void calculateTPOLevels(long var1, double var3, double var5, int var7) {
      this.debug("VP", "=== TPO CALCULATION ===");
      this.debug("VP", "  Total TPO: " + var1);
      if (var1 <= 0L) {
         this.prevTPO_POC = 0.0;
         this.prevTPO_VAH = 0.0;
         this.prevTPO_VAL = 0.0;
         this.debug("VP", "  TPO skipped: totalTPO <= 0");
      } else {
         int var8 = this.findMaxIndexInt(this.tpoBins, var7);
         this.lastTpoPocIndex = var8;
         this.prevTPO_POC = var5 + (var8 + 0.5) * var3;
         double[] var9 = this.calculateValueAreaInt(var8, var1, var3, var5, var7);
         this.prevTPO_VAL = var9[0];
         this.prevTPO_VAH = var9[1];
         this.debug("VP", "  TPO POC index: " + var8 + " price=" + this.prevTPO_POC);
         this.debug("VP", "  TPO VAH=" + this.prevTPO_VAH + ", TPO VAL=" + this.prevTPO_VAL);
      }
   }

   protected int findMaxIndexInt(int[] var1, int var2) {
      int var3 = 0;
      int var4 = var1[0];

      for (int var5 = 1; var5 < var2; var5++) {
         if (var1[var5] > var4) {
            var4 = var1[var5];
            var3 = var5;
         }
      }

      return var3;
   }

   protected double[] calculateValueAreaInt(int var1, long var2, double var4, double var6, int var8) {
      double var9 = var2 * (this.cfgValueAreaPct() / 100.0);
      long var11 = this.tpoBins[var1];
      int var13 = var1;
      int var14 = var1;
      int var15 = 0;

      while (var11 < var9) {
         boolean var16 = var13 + 1 < var8;
         boolean var17 = var14 - 1 >= 0;
         if (!var16 && !var17) {
            break;
         }

         int var18 = var16 ? this.tpoBins[var13 + 1] : -1;
         int var19 = var17 ? this.tpoBins[var14 - 1] : -1;
         var15++;
         if (var18 >= var19) {
            var11 += this.tpoBins[++var13];
         } else {
            var11 += this.tpoBins[--var14];
         }
      }

      double var20 = var6 + var14 * var4;
      double var21 = var6 + (var13 + 1) * var4;
      return new double[]{var20, var21};
   }

   protected int detectProfileShape(int var1, int var2) {
      if (var1 >= 0 && var2 >= 0 && var2 > var1) {
         int var3 = var2 - var1 + 1;
         if (var3 < 3) {
            return 5;
         }

         int var4 = this.lastTpoPocIndex;
         double var5 = (double)(var4 - var1) / (var3 - 1);
         int var7 = -1;
         int var8 = -1;
         boolean var9 = false;
         int var10 = var1 + var3 / 4;
         int var11 = var2 - var3 / 4;

         for (int var12 = var10; var12 <= var11; var12++) {
            if (this.tpoBins[var12] <= 1) {
               if (!var9) {
                  var7 = var12;
                  var9 = true;
               }

               var8 = var12;
            } else {
               if (var9 && var8 - var7 + 1 >= 2) {
                  int var13 = 0;
                  int var14 = 0;

                  for (int var15 = var1; var15 < var7; var15++) {
                     var13 += this.tpoBins[var15];
                  }

                  for (int var31 = var8 + 1; var31 <= var2; var31++) {
                     var14 += this.tpoBins[var31];
                  }

                  if (var13 >= 3 && var14 >= 3) {
                     return 4;
                  }
               }

               var9 = false;
            }
         }

         if (var9 && var8 - var7 + 1 >= 2) {
            int var18 = 0;
            int var22 = 0;

            for (int var28 = var1; var28 < var7; var28++) {
               var18 += this.tpoBins[var28];
            }

            for (int var29 = var8 + 1; var29 <= var2; var29++) {
               var22 += this.tpoBins[var29];
            }

            if (var18 >= 3 && var22 >= 3) {
               return 4;
            }
         }

         if (var5 > 0.66) {
            int var19 = var3 / 3;
            double var23 = 0.0;

            for (int var32 = var1; var32 < var1 + var19; var32++) {
               var23 += this.tpoBins[var32];
            }

            var23 /= Math.max(1, var19);
            double var33 = 0.0;

            for (int var17 = var2 - var19 + 1; var17 <= var2; var17++) {
               var33 += this.tpoBins[var17];
            }

            var33 /= Math.max(1, var19);
            if (var33 > var23 * 1.5) {
               return 1;
            }
         }

         if (var5 < 0.33) {
            int var20 = var3 / 3;
            double var25 = 0.0;

            for (int var35 = var2 - var20 + 1; var35 <= var2; var35++) {
               var25 += this.tpoBins[var35];
            }

            var25 /= Math.max(1, var20);
            double var36 = 0.0;

            for (int var41 = var1; var41 < var1 + var20; var41++) {
               var36 += this.tpoBins[var41];
            }

            var36 /= Math.max(1, var20);
            if (var36 > var25 * 1.5) {
               return 2;
            }
         }

         if (var5 >= 0.33 && var5 <= 0.66) {
            int var21 = var1 + var3 / 2;
            int var27 = 0;
            int var30 = 0;

            for (int var38 = var1; var38 < var21; var38++) {
               var27 += this.tpoBins[var38];
            }

            for (int var39 = var21; var39 <= var2; var39++) {
               var30 += this.tpoBins[var39];
            }

            double var40 = (double)Math.min(var27, var30) / Math.max(1, Math.max(var27, var30));
            if (var40 > 0.6) {
               return 3;
            }
         }

         return 5;
      } else {
         return 5;
      }
   }

   @Override
   protected void exportMultiSessionSVG() {
      if (this.historyCount != 0) {
         String var1 = this.resolveExportFolder();
         new File(var1).mkdirs();
         int var2 = this.historyCount - 1;
         String var3 = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date(this.histSessionStart[var2]));
         String var4 = "";

         try {
            var4 = this.cfgChart().Symbol;
         } catch (Exception var98) {
         }

         String[] var5 = new String[]{
            "", "Previous Day", "Previous Week", "Previous Month", "Previous Year", "Actual Day", "Actual Week", "Actual Month", "Actual Year"
         };
         String var6 = this.cfgSessionType() >= 1 && this.cfgSessionType() < var5.length ? var5[this.cfgSessionType()] : "Session";
         String var7 = var4.isEmpty() ? "" : var4.replaceAll("[^A-Za-z0-9_-]", "") + "_";
         String var8 = var6.replaceAll("\\s+", "");
         String var9 = "";

         try {
            var9 = this.cfgChart().Timeframe;
         } catch (Exception var97) {
         }

         String var10 = this.cfgBinSizeMode() == 2 ? "Fixed Tick" : "Range";
         double var11 = 0.0;
         if (this.cfgBinSizeMode() == 2) {
            double var13 = this.cfgChart().getInstrumentInfo().tickStep;
            var11 = this.cfgTicksPerBin() * var13;
         }

         String var105 = (var9.isEmpty() ? "" : " | TF:" + var9) + " | Bins:" + var10;
         String var14 = this.getStrategy() != null ? this.getStrategy().getStrategyName() : "";
         var14 = var14 == null ? "" : var14.replaceAll("[^A-Za-z0-9_\\-]", "_");
         String var15 = var1 + File.separator + "TPO_" + (var14.isEmpty() ? "" : var14 + "_") + var7 + var8 + "_" + this.fileRandomSuffix + ".svg";
         this.saveChartPath(var15);
         int var16 = this.historyCount;
         double var17 = Double.MIN_VALUE;
         double var19 = Double.MAX_VALUE;

         for (int var21 = 0; var21 < var16; var21++) {
            var17 = Math.max(var17, this.histSessionHigh[var21]);
            var19 = Math.min(var19, this.histSessionLow[var21]);
         }

         if (!(var17 <= var19)) {
            double var109 = var17 - var19;
            var19 -= var109 * 0.02;
            var17 += var109 * 0.02;
            var109 = var17 - var19;
            byte var23 = 80;
            byte var24 = 70;
            byte var25 = 60;
            byte var26 = 95;
            short var27 = 800;
            int var28 = Math.max(800, var16 * 300);
            boolean var29 = this.cfgSessionType() >= 5;
            int var30 = var23 + var28 + var24;
            int var31 = var25 + var27 + var26;
            long[] var32 = new long[var16];
            long var33 = 0L;

            for (int var35 = 0; var35 < var16; var35++) {
               var32[var35] = this.histSessionEnd[var35] - this.histSessionStart[var35];
               if (var32[var35] <= 0L) {
                  var32[var35] = 1L;
               }

               var33 += var32[var35];
            }

            if (var33 > 0L) {
               double[] var111 = new double[var16];
               double[] var36 = new double[var16];
               long var37 = 0L;

               for (int var39 = 0; var39 < var16; var39++) {
                  var111[var39] = var23 + (double)var37 / var33 * var28;
                  var37 += var32[var39];
                  var36[var39] = var23 + (double)var37 / var33 * var28;
               }

               double var112 = Math.max(0.1, Math.min(1.0, 1.0));
               double var41 = Math.max(0.1, Math.min(1.0, 0.5));
               String[] var43 = new String[]{"", "P-shape", "b-shape", "D-shape", "Double Dist.", "Other"};

               try (BufferedWriter var44 = new BufferedWriter(new FileWriter(var15))) {
                  var44.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
                  var44.write(
                     "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" width=\""
                        + var30
                        + "\" height=\""
                        + var31
                        + "\" viewBox=\"0 0 "
                        + var30
                        + " "
                        + var31
                        + "\">"
                  );
                  var44.write("<rect id=\"bgRect\" width=\"" + var30 + "\" height=\"" + var31 + "\" fill=\"#1a1a2e\"/>\n");
                  String var45 = "TPO Profile" + (var4.isEmpty() ? "" : " - " + var4) + " | " + var6 + var105 + " (" + var16 + " sessions)";
                  var44.write(
                     "<text id=\"chartTitle\" class=\"thTitle\" x=\""
                        + var23
                        + "\" y=\"35\" font-family=\"Sans-Serif\" font-size=\"16\" font-weight=\"700\" fill=\"#ffffff\">StrategyQuantX - "
                        + svgEsc(var45)
                        + "</text>\n"
                  );
                  var44.write(
                     "<rect id=\"plotRect\" x=\""
                        + var23
                        + "\" y=\""
                        + var25
                        + "\" width=\""
                        + var28
                        + "\" height=\""
                        + var27
                        + "\" fill=\"#16213e\" rx=\"4\"/>\n"
                  );
                  byte var46 = 20;

                  for (int var47 = 0; var47 <= var46; var47++) {
                     double var48 = var19 + var109 * var47 / var46;
                     double var50 = var25 + var27 - (var48 - var19) / var109 * var27;
                     var44.write(
                        "<text class=\"thPrice\" x=\""
                           + (var23 + var28 + 8)
                           + "\" y=\""
                           + (var50 + 4.0)
                           + "\" font-family=\"Monospace\" font-size=\"9\" fill=\"#888\" text-anchor=\"start\">"
                           + String.format(Locale.US, "%.5f", var48)
                           + "</text>\n"
                     );
                     var44.write(
                        "<line class=\"thGrid\" x1=\""
                           + var23
                           + "\" y1=\""
                           + var50
                           + "\" x2=\""
                           + (var30 - var24)
                           + "\" y2=\""
                           + var50
                           + "\" stroke=\"#2a2a4a\" stroke-width=\"0.5\"/>\n"
                     );
                  }

                  for (int var113 = 0; var113 < var16; var113++) {
                     double var121 = var111[var113];
                     double var132 = var36[var113];
                     double var52 = Math.max(10.0, var132 - var121);
                     var44.write(
                        "<line class=\"thSesDiv\" x1=\""
                           + var121
                           + "\" y1=\""
                           + var25
                           + "\" x2=\""
                           + var121
                           + "\" y2=\""
                           + (var25 + var27)
                           + "\" stroke=\"#555\" stroke-width=\"0.8\" stroke-dasharray=\"4,3\"/>\n"
                     );
                     String var54 = new SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.US).format(new Date(this.histSessionStart[var113]));
                     double var55 = (var121 + var132) / 2.0;
                     var44.write(
                        "<text class=\"thDate\" x=\""
                           + var55
                           + "\" y=\""
                           + (var25 + var27 + 55)
                           + "\" font-family=\"Sans-Serif\" font-size=\"10\" fill=\"#aaa\" text-anchor=\"middle\">"
                           + svgEsc(var54)
                           + "</text>\n"
                     );
                     double var57 = var25 + var27 - (this.histSessionHigh[var113] - var19) / var109 * var27;
                     double var59 = var25 + var27 - (this.histSessionLow[var113] - var19) / var109 * var27;
                     double var61;
                     double var63;
                     double var65;
                     if (var57 - var25 > 40.0) {
                        var61 = var57 - 26.0;
                        var63 = var57 - 15.0;
                        var65 = var57 - 4.0;
                     } else {
                        var61 = var59 + 14.0;
                        var63 = var59 + 25.0;
                        var65 = var59 + 36.0;
                     }

                     String var67 = String.format(Locale.US, "%.0f", this.histTotalVolume[var113]);
                     String var68 = String.format(Locale.US, "%.0f", this.histBullVolume[var113]);
                     String var69 = String.format(Locale.US, "%.0f", this.histBearVolume[var113]);
                     var44.write("<g class=\"sStats\" data-shy=\"" + String.format(Locale.US, "%.2f", var57) + "\">\n");
                     var44.write(
                        "<text class=\"thVol\" id=\"rpVol_"
                           + var113
                           + "\" x=\""
                           + var55
                           + "\" y=\""
                           + var61
                           + "\" font-family=\"Sans-Serif\" font-size=\"12\" font-weight=\"700\" fill=\"#ccc\" text-anchor=\"middle\">Vol: "
                           + var67
                           + "</text>\n"
                     );
                     var44.write(
                        "<text id=\"rpBull_"
                           + var113
                           + "\" x=\""
                           + var55
                           + "\" y=\""
                           + var63
                           + "\" font-family=\"Sans-Serif\" font-size=\"12\" font-weight=\"700\" fill=\"#4caf50\" text-anchor=\"middle\">Bull: "
                           + var68
                           + "</text>\n"
                     );
                     var44.write(
                        "<text id=\"rpBear_"
                           + var113
                           + "\" x=\""
                           + var55
                           + "\" y=\""
                           + var65
                           + "\" font-family=\"Sans-Serif\" font-size=\"12\" font-weight=\"700\" fill=\"#f44336\" text-anchor=\"middle\">Bear: "
                           + var69
                           + "</text>\n"
                     );
                     var44.write("</g>\n");
                     if (this.cfgShowShapeLabel() && this.histProfileShape[var113] >= 1 && this.histProfileShape[var113] <= 5) {
                        String var70 = var43[this.histProfileShape[var113]];

                        var44.write(
                           "<text x=\""
                              + var55
                              + "\" y=\""
                              + (var25 - 8)
                              + "\" font-family=\"Sans-Serif\" font-size=\"12\" font-weight=\"700\" fill=\""
                              + switch (this.histProfileShape[var113]) {
                                 case 1 -> "#4fc3f7";
                                 case 2 -> "#ff8a65";
                                 case 3 -> "#81c784";
                                 case 4 -> "#ce93d8";
                                 default -> "#bdbdbd";
                              }
                              + "\" text-anchor=\"middle\">"
                              + var70
                              + "</text>\n"
                        );
                     }
                  }

                  var44.write(
                     "<defs><clipPath id=\"plotClip\"><rect x=\""
                        + var23
                        + "\" y=\""
                        + var25
                        + "\" width=\""
                        + var28
                        + "\" height=\""
                        + var27
                        + "\"/></clipPath></defs>\n"
                  );
                  var44.write("<g id=\"chartG\" clip-path=\"url(#plotClip)\">\n");
                  var44.write("<g id=\"candlesG\" opacity=\"" + String.format(Locale.US, "%.2f", var112) + "\">\n");
                  if (this.cfgShowCandlesticks()) {
                     for (int var114 = 0; var114 < var16; var114++) {
                        int var122 = (int)var111[var114];
                        int var49 = (int)Math.max(10.0, var36[var114] - var111[var114]);
                        long var133 = this.histSessionEnd[var114] - this.histSessionStart[var114];
                        if (var133 <= 0L) {
                           var133 = 1L;
                        }

                        this.drawSvgCandlesUnified(
                           var44, var122, var49, var25, var27, var19, var109, this.histSessionStart[var114], this.histSessionEnd[var114], var133, 1.0
                        );
                     }
                  }

                  var44.write("</g>\n");
                  var44.write("<g id=\"profileG\" opacity=\"" + String.format(Locale.US, "%.2f", var41) + "\">\n");

                  for (int var115 = 0; var115 < var16; var115++) {
                     if (var29) {
                        var44.write("<g id=\"rpProfile_" + var115 + "\">\n");
                     }

                     double var123 = var111[var115];
                     double var134 = var36[var115];
                     double var143 = Math.max(10.0, var134 - var123);
                     double var157 = this.histTPO_VAL[var115];
                     double var56 = this.histTPO_VAH[var115];
                     if (var56 > var157) {
                        double var58 = var25 + var27 - (var56 - var19) / var109 * var27;
                        double var60 = var25 + var27 - (var157 - var19) / var109 * var27;
                        var44.write(
                           "<rect x=\""
                              + var123
                              + "\" y=\""
                              + var58
                              + "\" width=\""
                              + var143
                              + "\" height=\""
                              + Math.max(1.0, var60 - var58)
                              + "\" fill=\"rgba(70,130,180,0.30)\"/>\n"
                        );
                     }

                     if (this.cfgUseBlockMode()) {
                        int var179 = this.histTpoBracketCount[var115];
                        if (var179 <= 0) {
                           var179 = 1;
                        }

                        int var184 = this.histNumBins[var115];
                        double var188 = this.histSessionHigh[var115] - this.histSessionLow[var115];
                        double var62 = var11 > 0.0 ? var11 : (var184 > 0 && var188 > 0.0 ? var188 / var184 : 1.0);
                        int var64 = Math.max(1, (var184 + 100 - 1) / 100);
                        int var206 = (var184 + var64 - 1) / var64;
                        int var66 = 0;

                        for (int var217 = 0; var217 < var206; var217++) {
                           int var224 = var217 * var64;
                           int var228 = Math.min(var224 + var64, var184);
                           long var236 = 0L;

                           for (int var72 = var224; var72 < var228; var72++) {
                              var236 |= this.histTpoMask[var115][var72];
                           }

                           int var248 = Long.bitCount(var236);
                           if (var248 > var66) {
                              var66 = var248;
                           }
                        }

                        if (var66 <= 0) {
                           var66 = 1;
                        }

                        double var218 = Math.min(14.0, var143 / var66);
                        String[] var229 = new String[]{
                           "#e57373",
                           "#f06292",
                           "#ba68c8",
                           "#9575cd",
                           "#7986cb",
                           "#64b5f6",
                           "#4fc3f7",
                           "#4dd0e1",
                           "#4db6ac",
                           "#81c784",
                           "#aed581",
                           "#dce775",
                           "#fff176",
                           "#ffd54f",
                           "#ffb74d",
                           "#ff8a65"
                        };

                        for (int var237 = 0; var237 < var206; var237++) {
                           int var240 = var237 * var64;
                           int var249 = Math.min(var240 + var64, var184);
                           long var73 = 0L;

                           for (int var75 = var240; var75 < var249; var75++) {
                              var73 |= this.histTpoMask[var115][var75];
                           }

                           if (var73 != 0L) {
                              double var261 = this.histSessionLow[var115] + var240 * var62;
                              double var77 = this.histSessionLow[var115] + var249 * var62;
                              double var79 = var25 + var27 - (var77 - var19) / var109 * var27;
                              double var81 = var25 + var27 - (var261 - var19) / var109 * var27;
                              double var83 = Math.max(1.0, var81 - var79);
                              double var85 = Math.max(2.0, var218 * 0.85);
                              double var87 = Math.max(1.0, var83 * 0.9);
                              double var89 = var79 + (var83 - var87) / 2.0;
                              int var91 = 0;

                              for (int var92 = 0; var92 < var179 && var92 < 62; var92++) {
                                 if ((var73 & 1L << var92) != 0L) {
                                    String var93 = var229[var92 % var229.length];
                                    double var94 = var123 + var91 * var218;
                                    var44.write(
                                       "<rect class=\"tpoB\" data-bi=\""
                                          + var92 % var229.length
                                          + "\" x=\""
                                          + var94
                                          + "\" y=\""
                                          + var89
                                          + "\" width=\""
                                          + String.format(Locale.US, "%.1f", var85)
                                          + "\" height=\""
                                          + String.format(Locale.US, "%.1f", var87)
                                          + "\" fill=\""
                                          + var93
                                          + "\" rx=\"0.5\"/>"
                                    );
                                    var91++;
                                 }
                              }
                           }
                        }
                     }

                     if (var29) {
                        var44.write("</g>\n");
                     }
                  }

                  var44.write("</g>\n");

                  for (int var116 = 0; var116 < var16; var116++) {
                     double var124 = var111[var116];
                     double var135 = var36[var116];
                     double var144 = Math.max(10.0, var135 - var124);
                     int var158 = (int)var144;
                     double var161 = var25 + var27 - (this.histTPO_POC[var116] - var19) / var109 * var27;
                     if (var161 >= var25 && var161 <= var25 + var27) {
                        var44.write(
                           "<line x1=\""
                              + (int)var124
                              + "\" y1=\""
                              + var161
                              + "\" x2=\""
                              + ((int)var124 + var158)
                              + "\" y2=\""
                              + var161
                              + "\" stroke=\"#ffa500\" stroke-width=\"1\" stroke-dasharray=\"4,2\" opacity=\"0.8\" vector-effect=\"non-scaling-stroke\"/>\n"
                        );
                     }

                     var161 = var25 + var27 - (this.histTPO_VAH[var116] - var19) / var109 * var27;
                     if (var161 >= var25 && var161 <= var25 + var27) {
                        var44.write(
                           "<line x1=\""
                              + (int)var124
                              + "\" y1=\""
                              + var161
                              + "\" x2=\""
                              + ((int)var124 + var158)
                              + "\" y2=\""
                              + var161
                              + "\" stroke=\"#4caf50\" stroke-width=\"1\" stroke-dasharray=\"4,2\" opacity=\"0.8\" vector-effect=\"non-scaling-stroke\"/>\n"
                        );
                     }

                     var161 = var25 + var27 - (this.histTPO_VAL[var116] - var19) / var109 * var27;
                     if (var161 >= var25 && var161 <= var25 + var27) {
                        var44.write(
                           "<line x1=\""
                              + (int)var124
                              + "\" y1=\""
                              + var161
                              + "\" x2=\""
                              + ((int)var124 + var158)
                              + "\" y2=\""
                              + var161
                              + "\" stroke=\"#f44336\" stroke-width=\"1\" stroke-dasharray=\"4,2\" opacity=\"0.8\" vector-effect=\"non-scaling-stroke\"/>\n"
                        );
                     }

                     if (this.histIBH[var116] > 0.0 && this.histIBL[var116] > 0.0) {
                        var161 = var25 + var27 - (this.histIBH[var116] - var19) / var109 * var27;
                        double var173 = var25 + var27 - (this.histIBL[var116] - var19) / var109 * var27;
                        double var185 = Math.min(var161, var173);
                        double var192 = Math.abs(var173 - var161);
                        var44.write(
                           "<rect x=\""
                              + (int)var124
                              + "\" y=\""
                              + var185
                              + "\" width=\""
                              + var158
                              + "\" height=\""
                              + var192
                              + "\" fill=\"#ff9800\" fill-opacity=\"0.13\" stroke=\"none\" vector-effect=\"non-scaling-stroke\"/>\n"
                        );
                     }

                     if (this.histIBH[var116] > 0.0) {
                        var161 = var25 + var27 - (this.histIBH[var116] - var19) / var109 * var27;
                        if (var161 >= var25 && var161 <= var25 + var27) {
                           var44.write(
                              "<line x1=\""
                                 + (int)var124
                                 + "\" y1=\""
                                 + var161
                                 + "\" x2=\""
                                 + ((int)var124 + var158)
                                 + "\" y2=\""
                                 + var161
                                 + "\" stroke=\"#00bcd4\" stroke-width=\"1\" stroke-dasharray=\"4,2\" opacity=\"0.8\" vector-effect=\"non-scaling-stroke\"/>\n"
                           );
                        }
                     }

                     if (this.histIBL[var116] > 0.0) {
                        var161 = var25 + var27 - (this.histIBL[var116] - var19) / var109 * var27;
                        if (var161 >= var25 && var161 <= var25 + var27) {
                           var44.write(
                              "<line x1=\""
                                 + (int)var124
                                 + "\" y1=\""
                                 + var161
                                 + "\" x2=\""
                                 + ((int)var124 + var158)
                                 + "\" y2=\""
                                 + var161
                                 + "\" stroke=\"#e040fb\" stroke-width=\"1\" stroke-dasharray=\"4,2\" opacity=\"0.8\" vector-effect=\"non-scaling-stroke\"/>\n"
                           );
                        }
                     }
                  }

                  var44.write("</g>\n");
                  var44.write("<g id=\"tpoTextG\" clip-path=\"url(#plotClip)\">\n");
                  if (!this.cfgUseBlockMode()) {
                     String[] var117 = new String[]{
                        "#e57373",
                        "#f06292",
                        "#ba68c8",
                        "#9575cd",
                        "#7986cb",
                        "#64b5f6",
                        "#4fc3f7",
                        "#4dd0e1",
                        "#4db6ac",
                        "#81c784",
                        "#aed581",
                        "#dce775",
                        "#fff176",
                        "#ffd54f",
                        "#ffb74d",
                        "#ff8a65"
                     };

                     for (int var125 = 0; var125 < var16; var125++) {
                        double var128 = var111[var125];
                        double var51 = var36[var125];
                        double var53 = Math.max(10.0, var51 - var128);
                        int var167 = this.histTpoBracketCount[var125];
                        if (var167 <= 0) {
                           var167 = 1;
                        }

                        int var170 = this.histNumBins[var125];
                        double var174 = this.histSessionHigh[var125] - this.histSessionLow[var125];
                        double var186 = var11 > 0.0 ? var11 : (var170 > 0 && var174 > 0.0 ? var174 / var170 : 1.0);
                        int var193 = Math.max(1, (var170 + 100 - 1) / 100);
                        int var196 = (var170 + var193 - 1) / var193;
                        int var199 = 0;

                        for (int var202 = 0; var202 < var196; var202++) {
                           int var207 = var202 * var193;
                           int var212 = Math.min(var207 + var193, var170);
                           long var219 = 0L;

                           for (int var230 = var207; var230 < var212; var230++) {
                              var219 |= this.histTpoMask[var125][var230];
                           }

                           int var231 = Long.bitCount(var219);
                           if (var231 > var199) {
                              var199 = var231;
                           }
                        }

                        if (var199 <= 0) {
                           var199 = 1;
                        }

                        double var203 = Math.min(14.0, var53 / var199);

                        for (int var213 = 0; var213 < var196; var213++) {
                           int var220 = var213 * var193;
                           int var225 = Math.min(var220 + var193, var170);
                           long var232 = 0L;

                           for (int var241 = var220; var241 < var225; var241++) {
                              var232 |= this.histTpoMask[var125][var241];
                           }

                           if (var232 != 0L) {
                              double var242 = this.histSessionLow[var125] + var220 * var186;
                              double var253 = this.histSessionLow[var125] + var225 * var186;
                              double var262 = var25 + var27 - (var253 - var19) / var109 * var27;
                              double var266 = var25 + var27 - (var242 - var19) / var109 * var27;
                              double var268 = Math.max(1.0, var266 - var262);
                              double var269 = Math.min(12.0, Math.max(4.0, var268 * 0.85));
                              int var270 = 0;

                              for (int var84 = 0; var84 < var167 && var84 < 62; var84++) {
                                 if ((var232 & 1L << var84) != 0L) {
                                    char var272 = bracketChar(var84);
                                    String var86 = var117[var84 % var117.length];
                                    double var275 = var128 + var270 * var203;
                                    double var277 = var269 * 0.85;
                                    double var279 = var262 + var277;
                                    var44.write(
                                       "<text class=\"tpoL\" data-bi=\""
                                          + var84 % var117.length
                                          + "\" x=\""
                                          + var275
                                          + "\" y=\""
                                          + var279
                                          + "\" data-by=\""
                                          + String.format(Locale.US, "%.2f", var262)
                                          + "\" data-off=\""
                                          + String.format(Locale.US, "%.2f", var277)
                                          + "\" font-family=\"Monospace\" font-size=\""
                                          + var269
                                          + "\" fill=\""
                                          + var86
                                          + "\">"
                                          + var272
                                          + "</text>\n"
                                    );
                                    var270++;
                                 }
                              }
                           }
                        }
                     }
                  }

                  for (int var118 = 0; var118 < var16; var118++) {
                     double var126 = var111[var118];
                     double var136 = var36[var118];
                     int var145 = (int)Math.max(10.0, var136 - var126);
                     double var150 = var25 + var27 - (this.histTPO_POC[var118] - var19) / var109 * var27;
                     if (var150 >= var25 && var150 <= var25 + var27) {
                        var44.write(
                           "<text x=\""
                              + ((int)var126 + var145 - 3)
                              + "\" y=\""
                              + (var150 - 3.0)
                              + "\" data-by=\""
                              + String.format(Locale.US, "%.2f", var150)
                              + "\" data-off=\"-3\" font-family=\"Sans-Serif\" font-size=\"9\" fill=\"#ffa500\" text-anchor=\"end\" font-weight=\"700\">POC</text>\n"
                        );
                     }

                     var150 = var25 + var27 - (this.histTPO_VAH[var118] - var19) / var109 * var27;
                     if (var150 >= var25 && var150 <= var25 + var27) {
                        var44.write(
                           "<text x=\""
                              + ((int)var126 + var145 - 3)
                              + "\" y=\""
                              + (var150 - 3.0)
                              + "\" data-by=\""
                              + String.format(Locale.US, "%.2f", var150)
                              + "\" data-off=\"-3\" font-family=\"Sans-Serif\" font-size=\"9\" fill=\"#4caf50\" text-anchor=\"end\" font-weight=\"700\">VAH</text>\n"
                        );
                     }

                     var150 = var25 + var27 - (this.histTPO_VAL[var118] - var19) / var109 * var27;
                     if (var150 >= var25 && var150 <= var25 + var27) {
                        var44.write(
                           "<text x=\""
                              + ((int)var126 + var145 - 3)
                              + "\" y=\""
                              + (var150 - 3.0)
                              + "\" data-by=\""
                              + String.format(Locale.US, "%.2f", var150)
                              + "\" data-off=\"-3\" font-family=\"Sans-Serif\" font-size=\"9\" fill=\"#f44336\" text-anchor=\"end\" font-weight=\"700\">VAL</text>\n"
                        );
                     }

                     if (this.histIBH[var118] > 0.0) {
                        var150 = var25 + var27 - (this.histIBH[var118] - var19) / var109 * var27;
                        if (var150 >= var25 && var150 <= var25 + var27) {
                           var44.write(
                              "<text x=\""
                                 + ((int)var126 + var145 - 3)
                                 + "\" y=\""
                                 + (var150 - 3.0)
                                 + "\" data-by=\""
                                 + String.format(Locale.US, "%.2f", var150)
                                 + "\" data-off=\"-3\" font-family=\"Sans-Serif\" font-size=\"9\" fill=\"#00bcd4\" text-anchor=\"end\" font-weight=\"700\">IBH</text>\n"
                           );
                        }
                     }

                     if (this.histIBL[var118] > 0.0) {
                        var150 = var25 + var27 - (this.histIBL[var118] - var19) / var109 * var27;
                        if (var150 >= var25 && var150 <= var25 + var27) {
                           var44.write(
                              "<text x=\""
                                 + ((int)var126 + var145 - 3)
                                 + "\" y=\""
                                 + (var150 - 3.0)
                                 + "\" data-by=\""
                                 + String.format(Locale.US, "%.2f", var150)
                                 + "\" data-off=\"-3\" font-family=\"Sans-Serif\" font-size=\"9\" fill=\"#e040fb\" text-anchor=\"end\" font-weight=\"700\">IBL</text>\n"
                           );
                        }
                     }
                  }

                  var44.write("</g>\n");
                  var44.write("<g id=\"yAxisBg\"><rect x=\"0\" y=\"0\" width=\"" + var23 + "\" height=\"" + var31 + "\" fill=\"#1a1a2e\"/></g>\n");
                  var44.write("<g id=\"yAxisG\"></g>\n");

                  for (int var119 = 0; var119 < var16; var119++) {
                     this.drawSvgTimeAxis(
                        var44,
                        (int)var111[var119],
                        (int)Math.max(10.0, var36[var119] - var111[var119]),
                        var25,
                        var27,
                        this.histSessionStart[var119],
                        this.histSessionEnd[var119],
                        "HH:mm"
                     );
                  }

                  StringBuilder var120 = new StringBuilder();
                  var120.append("var barData=[");
                  boolean var127 = true;

                  for (int var129 = 0; var129 < var16; var129++) {
                     double var137 = var111[var129];
                     double var146 = var36[var129];
                     double var159 = Math.max(10.0, var146 - var137);
                     int var171 = 0;
                     int var175 = 0;

                     try {
                        while (true) {
                           long var180 = this.cfgChart().Time(var175);
                           if (var180 < this.histSessionStart[var129]) {
                              break;
                           }

                           if (var180 >= this.histSessionStart[var129] && var180 < this.histSessionEnd[var129]) {
                              var171++;
                           }

                           var175++;
                        }
                     } catch (Exception var99) {
                     }

                     if (var171 != 0) {
                        double var181 = var159 / var171;
                        var175 = 0;
                        int var189 = 0;

                        try {
                           while (true) {
                              long var194 = this.cfgChart().Time(var175);
                              if (var194 < this.histSessionStart[var129]) {
                                 break;
                              }

                              if (var194 >= this.histSessionStart[var129] && var194 < this.histSessionEnd[var129]) {
                                 double var200 = var137 + (var171 - 1 - var189) * var181 + var181 / 2.0;
                                 double var208 = this.cfgChart().Open(var175);
                                 double var221 = this.cfgChart().High(var175);
                                 double var233 = this.cfgChart().Low(var175);
                                 double var243 = this.cfgChart().Close(var175);
                                 double var254 = this.cfgChart().Volume(var175);
                                 if (!var127) {
                                    var120.append(",");
                                 }

                                 var120.append("{x:").append(String.format(Locale.US, "%.1f", var200));
                                 var120.append(",t:").append(var194);
                                 var120.append(",o:").append(String.format(Locale.US, "%.5f", var208));
                                 var120.append(",h:").append(String.format(Locale.US, "%.5f", var221));
                                 var120.append(",l:").append(String.format(Locale.US, "%.5f", var233));
                                 var120.append(",c:").append(String.format(Locale.US, "%.5f", var243));
                                 var120.append(",v:").append(String.format(Locale.US, "%.0f", var254));
                                 var120.append("}");
                                 var127 = false;
                                 var189++;
                              }

                              var175++;
                           }
                        } catch (Exception var100) {
                        }
                     }
                  }

                  var120.append("];\n");
                  if (var29) {
                     StringBuilder var130 = new StringBuilder();
                     var130.append(
                        "var rpMT="
                           + var25
                           + ",rpPH="
                           + var27
                           + ",rpGLow="
                           + String.format(Locale.US, "%.10f", var19)
                           + ",rpGRange="
                           + String.format(Locale.US, "%.10f", var109)
                           + ",rpN="
                           + var16
                           + ";\n"
                     );
                     var130.append("var rpSessions=[\n");

                     for (int var138 = 0; var138 < var16; var138++) {
                        double var141 = var111[var138];
                        double var155 = var36[var138];
                        double var168 = Math.max(10.0, var155 - var141);
                        int var177 = this.histNumBins[var138];
                        double var182 = this.histSessionLow[var138];
                        double var190 = this.histSessionHigh[var138] - this.histSessionLow[var138];
                        double var197 = var11 > 0.0 ? var11 : (var177 > 0 && var190 > 0.0 ? var190 / var177 : 1.0);
                        int var204 = 0;
                        int var209 = 0;

                        try {
                           while (true) {
                              long var214 = this.cfgChart().Time(var209);
                              if (var214 < this.histSessionStart[var138]) {
                                 break;
                              }

                              if (var214 >= this.histSessionStart[var138] && var214 < this.histSessionEnd[var138]) {
                                 var204++;
                              }

                              var209++;
                           }
                        } catch (Exception var101) {
                        }

                        double[][] var215 = new double[var204][var177];
                        boolean[] var222 = new boolean[var204];
                        int[] var226 = new int[var204];
                        int[][] var234 = new int[var204][];
                        var209 = 0;
                        int var238 = 0;

                        try {
                           while (true) {
                              long var244 = this.cfgChart().Time(var209);
                              if (var244 < this.histSessionStart[var138]) {
                                 break;
                              }

                              if (var244 >= this.histSessionStart[var138] && var244 < this.histSessionEnd[var138]) {
                                 int var255 = var204 - 1 - var238;
                                 double var74 = this.cfgChart().Open(var209);
                                 double var76 = this.cfgChart().High(var209);
                                 double var78 = this.cfgChart().Low(var209);
                                 double var80 = this.cfgChart().Close(var209);
                                 double var82 = this.cfgChart().Volume(var209);
                                 var222[var255] = var80 >= var74;
                                 int var271 = Math.max(0, Math.min(var177 - 1, (int)((var76 - var182) / var197)));
                                 int var273 = Math.max(0, Math.min(var177 - 1, (int)((var78 - var182) / var197)));
                                 double var274 = var82 / Math.max(1, var271 - var273 + 1);

                                 for (int var88 = var273; var88 <= var271; var88++) {
                                    var215[var255][var88] = var274;
                                 }

                                 long var276 = this.lastEffectiveBracketMillis > 0L ? this.lastEffectiveBracketMillis : 3600000L;
                                 int var90 = (int)((var244 - this.histSessionStart[var138]) / var276);
                                 var90 = Math.max(0, Math.min(61, var90));
                                 var226[var255] = var90;
                                 var234[var255] = new int[var271 - var273 + 1];

                                 for (int var280 = 0; var280 <= var271 - var273; var280++) {
                                    var234[var255][var280] = var273 + var280;
                                 }

                                 var238++;
                              }

                              var209++;
                           }
                        } catch (Exception var102) {
                        }

                        int var245 = this.histTpoBracketCount[var138];
                        if (var245 <= 0) {
                           var245 = 1;
                        }

                        int var250 = 0;

                        for (int var256 = 0; var256 < var177; var256++) {
                           int var259 = Long.bitCount(this.histTpoMask[var138][var256]);
                           if (var259 > var250) {
                              var250 = var259;
                           }
                        }

                        if (var250 <= 0) {
                           var250 = 1;
                        }

                        double var257 = Math.min(14.0, var168 / var250);
                        if (var138 > 0) {
                           var130.append(",");
                        }

                        var130.append(
                           "{nb:"
                              + var177
                              + ",low:"
                              + String.format(Locale.US, "%.10f", var182)
                              + ",bs:"
                              + String.format(Locale.US, "%.10f", var197)
                              + ",x:"
                              + String.format(Locale.US, "%.1f", var141)
                              + ",pw:"
                              + String.format(Locale.US, "%.1f", var168)
                              + ",bc:"
                              + var245
                              + ",lw:"
                              + String.format(Locale.US, "%.2f", var257)
                              + ",n:"
                              + var204
                              + ",bars:["
                        );

                        for (int var263 = 0; var263 < var204; var263++) {
                           if (var263 > 0) {
                              var130.append(",");
                           }

                           var130.append("{b:" + (var222[var263] ? "1" : "0") + ",bk:" + var226[var263] + ",d:{");
                           boolean var265 = true;

                           for (int var267 = 0; var267 < var177; var267++) {
                              if (var215[var263][var267] > 0.0) {
                                 if (!var265) {
                                    var130.append(",");
                                 }

                                 var130.append(var267 + ":" + String.format(Locale.US, "%.2f", var215[var263][var267]));
                                 var265 = false;
                              }
                           }

                           var130.append("}}");
                        }

                        var130.append("]}");
                     }

                     var130.append("];\n");
                     var130.append("var rpCurSes=" + (var16 - 1) + ";\n");
                     var120.append(var130);
                     StringBuilder var139 = new StringBuilder("var rpSesXArr=[");
                     StringBuilder var142 = new StringBuilder("var rpSesXEnd=[");

                     for (int var147 = 0; var147 < var16; var147++) {
                        if (var147 > 0) {
                           var139.append(",");
                           var142.append(",");
                        }

                        var139.append(String.format(Locale.US, "%.1f", var111[var147]));
                        var142.append(String.format(Locale.US, "%.1f", var36[var147]));
                     }

                     var139.append("];\n");
                     var142.append("];\n");
                     var120.append(var139).append(var142);
                     var120.append("var rpOrigKpi=[");

                     for (int var148 = 0; var148 < var16; var148++) {
                        if (var148 > 0) {
                           var120.append(",");
                        }

                        var120.append(
                           "{v:"
                              + String.format(Locale.US, "%.0f", this.histTotalVolume[var148])
                              + ",b:"
                              + String.format(Locale.US, "%.0f", this.histBullVolume[var148])
                              + ",r:"
                              + String.format(Locale.US, "%.0f", this.histBearVolume[var148])
                              + "}"
                        );
                     }

                     var120.append("];\n");
                     byte var149 = 26;
                     byte var156 = 3;
                     byte var160 = 15;
                     int var169 = var160 * (var149 + var156) + var156;
                     int var172 = var23 + var169 + 8;
                     int var178 = var25 + var27 + 60;
                     short var183 = 200;
                     byte var187 = 28;
                     byte var191 = 22;
                     byte var195 = 4;
                     int var198 = var172 + 120 + 50;
                     int var201 = 9 * (var187 + var195) + var183 + 320;
                     String var205 = "rx=\"4\" fill=\"#2a2a3e\" stroke=\"#555\" stroke-width=\"0.8\" cursor=\"pointer\"";
                     String var211 = "font-family=\"Sans-Serif\" font-size=\"12\" fill=\"#ccc\" text-anchor=\"middle\" pointer-events=\"none\" dominant-baseline=\"central\"";
                     String var216 = "font-family=\"Sans-Serif\" font-size=\"9\" fill=\"#ccc\" text-anchor=\"middle\" pointer-events=\"none\" dominant-baseline=\"central\"";
                     var44.write("<!-- Replay Toolbar -->\n");
                     var44.write("<g id=\"replayToolbar\">\n");
                     var44.write(
                        "<rect id=\"rpTbBg\" x=\""
                           + (var198 - 5)
                           + "\" y=\""
                           + (var178 - 3)
                           + "\" width=\""
                           + var201
                           + "\" height=\""
                           + (var191 + 6)
                           + "\" rx=\"6\" fill=\"rgba(20,20,40,0.85)\" stroke=\"#444\" stroke-width=\"0.8\"/>\n"
                     );
                     String[] var223 = new String[]{"rpSesFirst", "rpSesPrev10", "rpFirst", "rpPrev", "rpPlay", "rpNext", "rpLast", "rpSesNext10", "rpSesLast"};
                     String[] var227 = new String[]{"⏮", "⏪", "◀|", "◀", "▶", "▶", "|▶", "⏩", "⏭"};
                     String[] var235 = new String[]{
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
                     boolean[] var239 = new boolean[]{false, false, true, false, false, false, true, false, false};

                     for (int var246 = 0; var246 < 9; var246++) {
                        int var251 = var198 + var246 * (var187 + var195);
                        var44.write(
                           "<rect id=\""
                              + var223[var246]
                              + "\" x=\""
                              + var251
                              + "\" y=\""
                              + var178
                              + "\" width=\""
                              + var187
                              + "\" height=\""
                              + var191
                              + "\" "
                              + var205
                              + "><title>"
                              + var235[var246]
                              + "</title></rect>\n"
                        );
                        var44.write(
                           "<text x=\""
                              + (var251 + var187 / 2)
                              + "\" y=\""
                              + (var178 + var191 / 2)
                              + "\" "
                              + (var239[var246] ? var216 : var211)
                              + ">"
                              + var227[var246]
                              + "</text>\n"
                        );
                     }

                     var44.write(
                        "<text id=\"rpPauseIco\" x=\""
                           + (var198 + 4 * (var187 + var195) + var187 / 2)
                           + "\" y=\""
                           + (var178 + var191 / 2)
                           + "\" "
                           + var211
                           + " style=\"display:none\">⏸</text>\n"
                     );
                     int var247 = var198 + 9 * (var187 + var195) + 10;
                     int var252 = var178 + var191 / 2;
                     var120.append("var rpSlX=" + var247 + ",rpSlW=" + var183 + ";\n");
                     var44.write(
                        "<line x1=\""
                           + var247
                           + "\" y1=\""
                           + var252
                           + "\" x2=\""
                           + (var247 + var183)
                           + "\" y2=\""
                           + var252
                           + "\" stroke=\"#555\" stroke-width=\"2\" stroke-linecap=\"round\"/>\n"
                     );
                     var44.write(
                        "<line id=\"rpSliderFill\" x1=\""
                           + var247
                           + "\" y1=\""
                           + var252
                           + "\" x2=\""
                           + (var247 + var183)
                           + "\" y2=\""
                           + var252
                           + "\" stroke=\"#4fc3f7\" stroke-width=\"2\" stroke-linecap=\"round\"/>\n"
                     );
                     var44.write(
                        "<circle id=\"rpSliderKnob\" cx=\""
                           + (var247 + var183)
                           + "\" cy=\""
                           + var252
                           + "\" r=\"6\" fill=\"#4fc3f7\" stroke=\"#222\" stroke-width=\"1\" cursor=\"pointer\"/>\n"
                     );
                     var44.write(
                        "<rect id=\"rpSliderHit\" x=\""
                           + var247
                           + "\" y=\""
                           + (var252 - 8)
                           + "\" width=\""
                           + var183
                           + "\" height=\"16\" fill=\"transparent\" cursor=\"pointer\"/>\n"
                     );
                     int var258 = var247 + var183 + 10;
                     var44.write(
                        "<text id=\"rpCounter\" x=\""
                           + var258
                           + "\" y=\""
                           + (var178 + var191 / 2)
                           + "\" font-family=\"Monospace\" font-size=\"11\" fill=\"#ccc\" dominant-baseline=\"central\">0 / 0</text>\n"
                     );
                     int var260 = var258 + 75;
                     var44.write(
                        "<text id=\"rpSesLabel\" x=\""
                           + var260
                           + "\" y=\""
                           + (var178 + var191 / 2)
                           + "\" font-family=\"Monospace\" font-size=\"11\" fill=\"#7ab5ff\" dominant-baseline=\"central\">Session "
                           + var16
                           + "/"
                           + var16
                           + "</text>\n"
                     );
                     int var264 = var260 + 110;
                     var44.write("<foreignObject x=\"" + var264 + "\" y=\"" + (var178 + 1) + "\" width=\"60\" height=\"" + (var191 - 2) + "\">\n");
                     var44.write("<body xmlns=\"http://www.w3.org/1999/xhtml\" style=\"margin:0;background:transparent\">\n");
                     var44.write(
                        "<input id=\"rpSesInput\" type=\"text\" value=\""
                           + var16
                           + "\" style=\"width:50px;height:"
                           + (var191 - 6)
                           + "px;background:#1a1a2e;color:#7ab5ff;border:1px solid #555;border-radius:3px;font-family:Monospace;font-size:11px;text-align:center;outline:none;padding:1px 2px\" title=\"Enter session number and press Enter\"/>\n"
                     );
                     var44.write("</body></foreignObject>\n");
                     var44.write("</g>\n");
                     var44.write(
                        "<rect id=\"rpSesHilight\" x=\"0\" y=\""
                           + var25
                           + "\" width=\"0\" height=\""
                           + var27
                           + "\" fill=\"rgba(255,255,100,0.06)\" pointer-events=\"none\" style=\"display:none\"/>\n"
                     );
                  }

                  var44.write("<g id=\"crosshairG\" style=\"display:none;pointer-events:none\">\n");
                  var44.write(
                     "<line id=\"chV\" x1=\"0\" y1=\""
                        + var25
                        + "\" x2=\"0\" y2=\""
                        + (var25 + var27)
                        + "\" stroke=\"#999\" stroke-width=\"0.5\" stroke-dasharray=\"3,3\"/>\n"
                  );
                  var44.write(
                     "<line id=\"chH\" x1=\""
                        + var23
                        + "\" y1=\"0\" x2=\""
                        + (var23 + var28)
                        + "\" y2=\"0\" stroke=\"#999\" stroke-width=\"0.5\" stroke-dasharray=\"3,3\"/>\n"
                  );
                  var44.write("<g id=\"chPriceG\">");
                  var44.write(
                     "<rect id=\"chPriceBg\" x=\""
                        + (var23 + var28 + 2)
                        + "\" y=\"0\" width=\"60\" height=\"16\" rx=\"2\" fill=\"#333\" stroke=\"#666\" stroke-width=\"0.5\"/>"
                  );
                  var44.write(
                     "<text id=\"chPriceTxt\" x=\""
                        + (var23 + var28 + 32)
                        + "\" y=\"12\" font-family=\"Monospace\" font-size=\"9\" fill=\"#fff\" text-anchor=\"middle\"></text>"
                  );
                  var44.write("</g>\n");
                  var44.write("<g id=\"chTimeG\">");
                  var44.write(
                     "<rect id=\"chTimeBg\" x=\"0\" y=\""
                        + (var25 + var27 + 2)
                        + "\" width=\"70\" height=\"16\" rx=\"2\" fill=\"#333\" stroke=\"#666\" stroke-width=\"0.5\"/>"
                  );
                  var44.write(
                     "<text id=\"chTimeTxt\" x=\"0\" y=\""
                        + (var25 + var27 + 14)
                        + "\" font-family=\"Monospace\" font-size=\"9\" fill=\"#fff\" text-anchor=\"middle\"></text>"
                  );
                  var44.write("</g>\n");
                  var44.write("<g id=\"chTooltipG\">");
                  var44.write(
                     "<rect id=\"chTooltipBg\" x=\"0\" y=\"0\" width=\"165\" height=\"100\" rx=\"4\" fill=\"rgba(20,20,40,0.92)\" stroke=\"#555\" stroke-width=\"0.5\"/>"
                  );
                  var44.write("<text id=\"chTooltipTxt\" x=\"8\" y=\"14\" font-family=\"Monospace\" font-size=\"10\" fill=\"#ddd\">");
                  var44.write("<tspan id=\"chTD\" x=\"8\" dy=\"0\"></tspan>");
                  var44.write("<tspan id=\"chTO\" x=\"8\" dy=\"14\"></tspan>");
                  var44.write("<tspan id=\"chTH\" x=\"8\" dy=\"14\"></tspan>");
                  var44.write("<tspan id=\"chTL\" x=\"8\" dy=\"14\"></tspan>");
                  var44.write("<tspan id=\"chTC\" x=\"8\" dy=\"14\"></tspan>");
                  var44.write("<tspan id=\"chTV\" x=\"8\" dy=\"14\"></tspan>");
                  var44.write("</text></g>\n");
                  var44.write("</g>\n");
                  this.writeSvgDrawingTools(var44, var23, var25, var28, var27, var19, var109, var120.toString(), var26);
                  var44.write("<script type=\"text/javascript\">\n");
                  var44.write("//<![CDATA[\n");
                  StringBuilder var131 = new StringBuilder("var _sesData=[");

                  for (int var140 = 0; var140 < var16; var140++) {
                     if (var140 > 0) {
                        var131.append(",");
                     }

                     var131.append(
                        String.format(
                           Locale.US,
                           "{x1:%.1f,x2:%.1f,h:%.6f,l:%.6f}",
                           var111[var140],
                           var36[var140],
                           this.histSessionHigh[var140],
                           this.histSessionLow[var140]
                        )
                     );
                  }

                  var131.append("];");
                  var44.write(var131.toString() + "\n");
                  var44.write(
                     "var _mt="
                        + var25
                        + ",_ph="
                        + var27
                        + ",_gL="
                        + String.format(Locale.US, "%.8f", var19)
                        + ",_gR="
                        + String.format(Locale.US, "%.8f", var109)
                        + ",_mL="
                        + var23
                        + ";\n"
                  );
                  var44.write("var _yFill='#ccc',_yStroke='#888',_yBg='#1a1a2e';\n");
                  var44.write("function _autoScale(){\n");
                  var44.write("  var svg=document.querySelector('svg');\n");
                  var44.write("  if(!svg)return;\n");
                  var44.write("  var cr=svg.getBoundingClientRect();\n");
                  var44.write("  var vb=svg.viewBox.baseVal;\n");
                  var44.write("  var de=document.documentElement,bd=document.body;\n");
                  var44.write("  var scrollPx=(de&&de.scrollLeft)||(bd&&bd.scrollLeft)||window.scrollX||window.pageXOffset||0;\n");
                  var44.write("  var pixToSvg=(cr.width>0)?vb.width/cr.width:1;\n");
                  var44.write("  var sl=scrollPx*pixToSvg;\n");
                  var44.write("  var vw=(window.innerWidth||document.documentElement.clientWidth)*pixToSvg;\n");
                  var44.write("  var vL=sl,vR=sl+vw;\n");
                  var44.write("  var visH=-1e30,visL=1e30,hasVis=false;\n");
                  var44.write("  for(var i=0;i<_sesData.length;i++){\n");
                  var44.write("    var s=_sesData[i];\n");
                  var44.write("    if(s.x2>=vL && s.x1<=vR){hasVis=true;visH=Math.max(visH,s.h);visL=Math.min(visL,s.l);}\n");
                  var44.write("  }\n");
                  var44.write("  if(!hasVis){visH=_gL+_gR;visL=_gL;}\n");
                  var44.write("  if(visH<=visL){visH=_gL+_gR;visL=_gL;}\n");
                  var44.write("  var pad=(visH-visL)*0.05;visH+=pad;visL-=pad;\n");
                  var44.write("  var minSpan=_gR*0.40;\n");
                  var44.write("  if((visH-visL)<minSpan){\n");
                  var44.write("    var mid=(visH+visL)/2;\n");
                  var44.write("    visL=mid-minSpan/2;visH=mid+minSpan/2;\n");
                  var44.write("  }\n");
                  var44.write("  var gH=_gL+_gR;\n");
                  var44.write("  if(visH>gH)visH=gH;\n");
                  var44.write("  if(visL<_gL)visL=_gL;\n");
                  var44.write("  if(visH<=visL){visH=gH;visL=_gL;}\n");
                  var44.write("  var yH=_mt+_ph-(visH-_gL)/_gR*_ph;\n");
                  var44.write("  var yL=_mt+_ph-(visL-_gL)/_gR*_ph;\n");
                  var44.write("  if(yL<=yH){yH=_mt;yL=_mt+_ph;}\n");
                  var44.write("  var sy=_ph/(yL-yH);\n");
                  var44.write("  if(!isFinite(sy)||sy<=0)sy=1;\n");
                  var44.write("  sy=Math.max(1,Math.min(3,sy));\n");
                  var44.write("  var ty=_mt-yH*sy;\n");
                  var44.write("  if(!isFinite(ty))ty=0;\n");
                  var44.write("  var cg=document.getElementById('chartG');\n");
                  var44.write("  if(cg)cg.setAttribute('transform','matrix(1,0,0,'+sy+',0,'+ty+')');\n");
                  var44.write("  var tg=document.getElementById('tpoTextG');\n");
                  var44.write("  if(tg){var txts=tg.getElementsByTagName('text');\n");
                  var44.write("  for(var i=0;i<txts.length;i++){\n");
                  var44.write("    var t=txts[i];\n");
                  var44.write("    var by=parseFloat(t.dataset.by)||0;\n");
                  var44.write("    var off=parseFloat(t.dataset.off)||0;\n");
                  var44.write("    t.setAttribute('y',sy*by+ty+off);\n");
                  var44.write("  }}\n");
                  var44.write("  var ya=document.getElementById('yAxisG');\n");
                  var44.write("  if(ya){ya.innerHTML='';var nT=10;\n");
                  var44.write("    for(var t=0;t<=nT;t++){\n");
                  var44.write("      var p=visL+(visH-visL)*t/nT;\n");
                  var44.write("      var yy=_mt+_ph-(_ph*t/nT);\n");
                  var44.write("      var ln=document.createElementNS('http://www.w3.org/2000/svg','line');\n");
                  var44.write("      ln.setAttribute('x1',_mL);ln.setAttribute('x2',_mL+6);\n");
                  var44.write("      ln.setAttribute('y1',yy);ln.setAttribute('y2',yy);\n");
                  var44.write("      ln.setAttribute('stroke',_yStroke);ya.appendChild(ln);\n");
                  var44.write("      var tx=document.createElementNS('http://www.w3.org/2000/svg','text');\n");
                  var44.write("      tx.setAttribute('x',_mL-3);tx.setAttribute('y',yy+3);\n");
                  var44.write("      tx.setAttribute('font-family','Monospace');tx.setAttribute('font-size','9');\n");
                  var44.write("      tx.setAttribute('fill',_yFill);tx.setAttribute('text-anchor','end');\n");
                  var44.write("      tx.textContent=p.toFixed(5);ya.appendChild(tx);\n");
                  var44.write("    }\n");
                  var44.write("  }\n");
                  var44.write("  var sgs=document.querySelectorAll('.sStats');\n");
                  var44.write("  for(var i=0;i<sgs.length;i++){\n");
                  var44.write("    var sg=sgs[i];\n");
                  var44.write("    var origShy=parseFloat(sg.dataset.shy)||0;\n");
                  var44.write("    var newShy=sy*origShy+ty;\n");
                  var44.write("    sg.setAttribute('transform','translate(0,'+(newShy-origShy)+')');\n");
                  var44.write("  }\n");
                  var44.write("}\n");
                  var44.write("// embedded Results panel uses container-driven sizing; autoscale disabled\n");
                  var44.write("function _stickyScroll(){\n");
                  var44.write("  var svg=document.querySelector('svg');\n");
                  var44.write("  if(!svg)return;\n");
                  var44.write("  var vb=svg.viewBox.baseVal;\n");
                  var44.write("  var cr=svg.getBoundingClientRect();\n");
                  var44.write("  var de=document.documentElement,bd=document.body;\n");
                  var44.write("  var scrollPx=(de&&de.scrollLeft)||(bd&&bd.scrollLeft)||window.scrollX||window.pageXOffset||0;\n");
                  var44.write("  var sx=scrollPx*(vb.width/cr.width);\n");
                  var44.write("  var tb=document.getElementById('toolbar');\n");
                  var44.write("  if(tb)tb.setAttribute('transform','translate('+sx+',0)');\n");
                  var44.write("  var rp=document.getElementById('replayToolbar');\n");
                  var44.write("  if(rp)rp.setAttribute('transform','translate('+sx+',0)');\n");
                  var44.write("  var ya=document.getElementById('yAxisG');\n");
                  var44.write("  if(ya)ya.setAttribute('transform','translate('+sx+',0)');\n");
                  var44.write("  var yb=document.getElementById('yAxisBg');\n");
                  var44.write("  if(yb)yb.setAttribute('transform','translate('+sx+',0)');\n");
                  var44.write("  var tt=document.getElementById('chartTitle');\n");
                  var44.write("  if(tt)tt.setAttribute('transform','translate('+sx+',0)');\n");
                  var44.write("  var ac=document.getElementById('annoGroup');\n");
                  var44.write("  if(ac)ac.setAttribute('transform','translate('+sx+',0)');\n");
                  var44.write("  var ch=document.getElementById('crosshairInfo');\n");
                  var44.write("  if(ch)ch.setAttribute('transform','translate('+sx+',0)');\n");
                  var44.write("}\n");
                  var44.write("window.addEventListener('scroll',_stickyScroll);\n");
                  var44.write("window.addEventListener('resize',_stickyScroll);\n");
                  var44.write("setTimeout(_stickyScroll,120);\n");
                  var44.write("//]]>\n");
                  var44.write("</script>\n");
                  var44.write("</svg>\n");
               } catch (Exception var104) {
               }
            }
         }
      }
   }

   protected void writeSvgDrawingTools(BufferedWriter var1, int var2, int var3, int var4, int var5, double var6, double var8, String var10, int var11) throws IOException {
      byte var12 = 26;
      byte var13 = 26;
      byte var14 = 3;
      int var15 = var3 + var5 + 60;
      String[] var16 = new String[]{
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
      String[] var17 = new String[]{"╱", "─", "▭", "T", "✎", "\ud83d\udccf", "●", "☀", "↩", "\ud83d\uddd1", "┼", "+", "−", "↺"};
      String[] var18 = new String[]{
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
      int var19 = var16.length + 1;
      int var20 = var19 * (var12 + var14) + var14;
      int var21 = var2;
      var1.write("<g id=\"toolbar\" style=\"cursor:pointer\">\n");
      var1.write(
         "<rect id=\"tbBg\" x=\""
            + (var21 - 2)
            + "\" y=\""
            + (var15 - 2)
            + "\" width=\""
            + (var20 + 4)
            + "\" height=\""
            + (var13 + 4)
            + "\" rx=\"4\" fill=\"rgba(30,30,50,0.85)\" stroke=\"#555\" stroke-width=\"0.5\"/>\n"
      );

      for (int var22 = 0; var22 < var16.length; var22++) {
         int var23 = var21 + var14 + var22 * (var12 + var14);
         var1.write("<g id=\"g_" + var16[var22] + "\">\n");
         var1.write(
            "<rect class=\"tbBtn\" id=\""
               + var16[var22]
               + "\" x=\""
               + var23
               + "\" y=\""
               + var15
               + "\" width=\""
               + var12
               + "\" height=\""
               + var13
               + "\" rx=\"3\" fill=\"#2a2a4a\" stroke=\"#666\" stroke-width=\"0.8\"/>\n"
         );
         if (var16[var22].equals("btnColor")) {
            var1.write(
               "<circle id=\"colorDot\" cx=\""
                  + (var23 + var12 / 2)
                  + "\" cy=\""
                  + (var15 + var13 / 2)
                  + "\" r=\"7\" fill=\"#ffcc00\" stroke=\"#888\" stroke-width=\"0.5\" pointer-events=\"none\"/>\n"
            );
         } else if (var16[var22].equals("btnTheme")) {
            var1.write(
               "<text class=\"tbLbl\" id=\"themeIcon\" x=\""
                  + (var23 + var12 / 2)
                  + "\" y=\""
                  + (var15 + var13 / 2 + 5)
                  + "\" font-family=\"Sans-Serif\" font-size=\"13\" fill=\"#ccc\" text-anchor=\"middle\" pointer-events=\"none\">☀</text>\n"
            );
         } else {
            var1.write(
               "<text class=\"tbLbl\" x=\""
                  + (var23 + var12 / 2)
                  + "\" y=\""
                  + (var15 + var13 / 2 + 5)
                  + "\" font-family=\"Sans-Serif\" font-size=\"13\" fill=\"#ccc\" text-anchor=\"middle\" pointer-events=\"none\">"
                  + var17[var22]
                  + "</text>\n"
            );
         }

         var1.write("<title>" + var18[var22] + "</title>\n");
         var1.write("</g>\n");
      }

      int var30 = var21 + var14 + var16.length * (var12 + var14);
      var1.write("<g id=\"g_btnPrint\">\n");
      var1.write(
         "<rect class=\"tbBtn\" id=\"btnPrint\" x=\""
            + var30
            + "\" y=\""
            + var15
            + "\" width=\""
            + var12
            + "\" height=\""
            + var13
            + "\" rx=\"3\" fill=\"#2a2a4a\" stroke=\"#666\" stroke-width=\"0.8\"/>\n"
      );
      var1.write(
         "<text class=\"tbLbl\" x=\""
            + (var30 + var12 / 2)
            + "\" y=\""
            + (var15 + var13 / 2 + 5)
            + "\" font-family=\"Sans-Serif\" font-size=\"13\" fill=\"#ccc\" text-anchor=\"middle\" pointer-events=\"none\">\ud83d\udda8</text>\n"
      );
      var1.write("<title>Print</title>\n");
      var1.write("</g>\n");
      var1.write("</g>\n");
      int var31 = var21 + var20 + 8;
      var1.write("<g id=\"annoGroup\" style=\"cursor:pointer\">\n");
      var1.write("<foreignObject x=\"" + var31 + "\" y=\"" + (var15 + 2) + "\" width=\"120\" height=\"22\">\n");
      var1.write("<body xmlns=\"http://www.w3.org/1999/xhtml\" style=\"margin:0;background:transparent\">\n");
      var1.write(
         "<label style=\"display:flex;align-items:center;gap:3px;font-family:Sans-Serif;font-size:10px;color:#ccc;cursor:pointer;background:rgba(30,30,50,0.85);padding:2px 5px;border-radius:4px;border:0.5px solid #555\">\n"
      );
      var1.write("<input id=\"annoCheck\" type=\"checkbox\" checked=\"checked\" style=\"margin:0;cursor:pointer\"/>\n");
      var1.write("<span id=\"annoLabel\">Annotations</span></label>\n");
      var1.write("</body></foreignObject>\n");
      var1.write("</g>\n");
      String[] var24 = new String[]{"#ffcc00", "#00e5ff", "#ff5252", "#69f0ae", "#ff80ab", "#b388ff", "#ffffff", "#ffa726"};
      byte var25 = 18;
      int var26 = var21 + var14 + 6 * (var12 + var14);
      int var27 = var15 - var25 - 8;
      var1.write("<g id=\"colorPalette\" style=\"display:none;cursor:pointer\">\n");
      var1.write(
         "<rect x=\""
            + (var26 - 3)
            + "\" y=\""
            + (var27 - 3)
            + "\" width=\""
            + (var24.length * (var25 + 3) + 6)
            + "\" height=\""
            + (var25 + 6)
            + "\" rx=\"4\" fill=\"rgba(30,30,50,0.9)\" stroke=\"#555\" stroke-width=\"0.5\"/>\n"
      );

      for (int var28 = 0; var28 < var24.length; var28++) {
         int var29 = var26 + var28 * (var25 + 3);
         var1.write(
            "<rect class=\"swatch\" data-color=\""
               + var24[var28]
               + "\" x=\""
               + var29
               + "\" y=\""
               + var27
               + "\" width=\""
               + var25
               + "\" height=\""
               + var25
               + "\" rx=\"3\" fill=\""
               + var24[var28]
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
      var1.write(
         "var _tpoDk=['#e57373','#f06292','#ba68c8','#9575cd','#7986cb','#64b5f6','#4fc3f7','#4dd0e1','#4db6ac','#81c784','#aed581','#dce775','#fff176','#ffd54f','#ffb74d','#ff8a65'];\n"
      );
      var1.write(
         "var _tpoLt=['#c62828','#ad1457','#6a1b9a','#4527a0','#283593','#1565c0','#0277bd','#00838f','#00695c','#2e7d32','#558b2f','#9e9d24','#f9a825','#ff8f00','#ef6c00','#d84315'];\n"
      );
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
      var1.write("  var br=document.querySelectorAll('.thBrand');for(var i=0;i<br.length;i++)br[i].setAttribute('fill',isDark?'#ffffff':dBg);\n");
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
      var1.write("  _yFill=isDark?'#ccc':'#555';_yStroke=isDark?'#888':'#bbb';_yBg=isDark?dBg:lBg;\n");
      var1.write("  var yb=document.getElementById('yAxisBg');if(yb)yb.querySelector('rect').setAttribute('fill',_yBg);\n");
      var1.write("  _autoScale();\n");
      var1.write("  var _tc=isDark?_tpoDk:_tpoLt;\n");
      var1.write(
         "  var tl=document.querySelectorAll('.tpoL');for(var i=0;i<tl.length;i++){var bi=parseInt(tl[i].dataset.bi)||0;tl[i].setAttribute('fill',_tc[bi%_tc.length]);}\n"
      );
      var1.write(
         "  var tb=document.querySelectorAll('.tpoB');for(var i=0;i<tb.length;i++){var bi=parseInt(tb[i].dataset.bi)||0;tb[i].setAttribute('fill',_tc[bi%_tc.length]);}\n"
      );
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
      var1.write("// Bar Replay Engine (multi-session TPO)\n");
      var1.write("if(typeof rpSessions!=='undefined' && rpSessions.length>0){\n");
      var1.write("var rpStep=rpSessions[rpCurSes].n,rpPlaying=false,rpTimer=null;\n");
      var1.write("var rpKnob=document.getElementById('rpSliderKnob');\n");
      var1.write("var rpFill=document.getElementById('rpSliderFill');\n");
      var1.write("var rpCtr=document.getElementById('rpCounter');\n");
      var1.write("var rpSesLbl=document.getElementById('rpSesLabel');\n");
      var1.write("var rpPauseIco=document.getElementById('rpPauseIco');\n");
      var1.write("var rpTbG=document.getElementById('replayToolbar');\n");
      var1.write("var rpHilight=document.getElementById('rpSesHilight');\n");
      var1.write("var rpOrigProf=[];\n");
      var1.write("for(var s=0;s<rpN;s++){var pg=document.getElementById('rpProfile_'+s);rpOrigProf.push(pg?pg.innerHTML:'');}\n");
      var1.write("function rpShowAll(){\n");
      var1.write("  for(var s=0;s<rpN;s++){\n");
      var1.write("    var cg=document.getElementById('rpCandles_'+s);\n");
      var1.write("    if(cg){var cs=cg.querySelectorAll('[id^=rc_]');cs.forEach(function(c){c.style.display='';});}\n");
      var1.write("    var pg=document.getElementById('rpProfile_'+s);\n");
      var1.write("    if(pg){pg.style.display='';pg.innerHTML=rpOrigProf[s];}\n");
      var1.write("  }\n");
      var1.write("}\n");
      var1.write("function rpRender(){\n");
      var1.write("  var ses=rpSessions[rpCurSes];\n");
      var1.write("  var nb=ses.nb,bars=ses.bars,total=ses.n;\n");
      var1.write("  for(var s=0;s<rpN;s++){\n");
      var1.write("    var cg=document.getElementById('rpCandles_'+s);\n");
      var1.write("    if(!cg)continue;\n");
      var1.write("    if(s>rpCurSes){cg.style.display='none';continue;}\n");
      var1.write("    if(s===rpCurSes){\n");
      var1.write("      if(rpStep===0){cg.style.display='none';continue;}\n");
      var1.write("      cg.style.display='';\n");
      var1.write("      var cs=cg.querySelectorAll('[id^=rc_]');\n");
      var1.write("      cs.forEach(function(c){var ci=parseInt(c.id.split('_')[1]);c.style.display=(ci<rpStep)?'':'none';});\n");
      var1.write("    } else {\n");
      var1.write("      cg.style.display='';\n");
      var1.write("      var cs2=cg.querySelectorAll('[id^=rc_]');cs2.forEach(function(c){c.style.display='';});\n");
      var1.write("    }\n");
      var1.write("  }\n");
      var1.write("  var bCols=['#e57373','#f06292','#ba68c8','#9575cd','#7986cb','#64b5f6',\n");
      var1.write("    '#4fc3f7','#4dd0e1','#4db6ac','#81c784','#aed581','#dce775',\n");
      var1.write("    '#fff176','#ffd54f','#ffb74d','#ff8a65'];\n");
      var1.write("  var bChars='ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';\n");
      var1.write("  for(var s=0;s<rpN;s++){\n");
      var1.write("    var pg=document.getElementById('rpProfile_'+s);\n");
      var1.write("    if(!pg)continue;\n");
      var1.write("    if(s>rpCurSes){pg.style.display='none';continue;}\n");
      var1.write("    if(s!==rpCurSes){pg.style.display='';pg.innerHTML=rpOrigProf[s];continue;}\n");
      var1.write("    if(rpStep===0){pg.style.display='none';continue;}\n");
      var1.write("    pg.style.display='';\n");
      var1.write("    if(rpStep>=total){pg.innerHTML=rpOrigProf[s];continue;}\n");
      var1.write("    while(pg.firstChild)pg.removeChild(pg.firstChild);\n");
      var1.write("    var mask=new Array(nb);\n");
      var1.write("    for(var j=0;j<nb;j++)mask[j]={};\n");
      var1.write("    for(var i=0;i<rpStep;i++){\n");
      var1.write("      var bar=bars[i],bk=bar.bk;\n");
      var1.write("      for(var k in bar.d){mask[parseInt(k)][bk]=1;}\n");
      var1.write("    }\n");
      var1.write("    var lw=ses.lw||10;\n");
      var1.write("    for(var j=0;j<nb;j++){\n");
      var1.write("      var m=mask[j];if(Object.keys(m).length===0)continue;\n");
      var1.write("      var bLow=ses.low+j*ses.bs,bHi=bLow+ses.bs;\n");
      var1.write("      var y1=rpMT+rpPH-(bHi-rpGLow)/rpGRange*rpPH;\n");
      var1.write("      var y2=rpMT+rpPH-(bLow-rpGLow)/rpGRange*rpPH;\n");
      var1.write("      var rH=Math.max(1,y2-y1);\n");
      var1.write("      var fs=Math.max(10,Math.min(12,rH*0.9));\n");
      var1.write("      var col=0;\n");
      var1.write("      for(var b=0;b<62;b++){if(m[b]){\n");
      var1.write("        var ch=b<bChars.length?bChars[b]:'?';\n");
      var1.write("        var lclr=bCols[b%bCols.length];\n");
      var1.write("        var t=document.createElementNS(ns,'text');\n");
      var1.write("        t.setAttribute('x',(ses.x+col*lw).toFixed(1));\n");
      var1.write("        var ly=y1+fs*0.85;\n");
      var1.write("        t.setAttribute('y',ly.toFixed(1));\n");
      var1.write("        t.setAttribute('font-family','Monospace');\n");
      var1.write("        t.setAttribute('font-size',fs.toFixed(1));\n");
      var1.write("        t.setAttribute('fill',lclr);\n");
      var1.write("        t.textContent=ch;\n");
      var1.write("        pg.appendChild(t);col++;\n");
      var1.write("      }}\n");
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
      var1.write("    var ve=document.getElementById('rpVol_'+q);\n");
      var1.write("    if(!ve)continue;\n");
      var1.write("    var ok=rpOrigKpi[q];\n");
      var1.write("    if(q>rpCurSes){\n");
      var1.write("      ve.textContent='Vol: 0';\n");
      var1.write("      document.getElementById('rpBull_'+q).textContent='Bull: 0';\n");
      var1.write("      document.getElementById('rpBear_'+q).textContent='Bear: 0';\n");
      var1.write("    } else if(q!==rpCurSes||rpStep>=rpSessions[q].n){\n");
      var1.write("      ve.textContent='Vol: '+ok.v;\n");
      var1.write("      document.getElementById('rpBull_'+q).textContent='Bull: '+ok.b;\n");
      var1.write("      document.getElementById('rpBear_'+q).textContent='Bear: '+ok.r;\n");
      var1.write("    }\n");
      var1.write("  }\n");
      var1.write("  var s=rpCurSes,ses=rpSessions[s];\n");
      var1.write("  if(rpStep<ses.n){\n");
      var1.write("    var tv=0,bv=0,rv=0;\n");
      var1.write("    for(var i=0;i<rpStep;i++){\n");
      var1.write("      var bar=ses.bars[i];\n");
      var1.write("      for(var k in bar.d){var vol=bar.d[k];tv+=vol;if(bar.b)bv+=vol;else rv+=vol;}\n");
      var1.write("    }\n");
      var1.write("    document.getElementById('rpVol_'+s).textContent='Vol: '+Math.round(tv);\n");
      var1.write("    document.getElementById('rpBull_'+s).textContent='Bull: '+Math.round(bv);\n");
      var1.write("    document.getElementById('rpBear_'+s).textContent='Bear: '+Math.round(rv);\n");
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

   protected static String formatLegendTime(long var0, long var2, long var4) {
      boolean var6 = SQTime.getFullYear(var0) == SQTime.getFullYear(var2) && SQTime.getDayOfYear(var0) == SQTime.getDayOfYear(var2);
      int var7 = SQTime.getHour(var0);
      int var8 = SQTime.getMinute(var0);
      if (var6) {
         return String.format(Locale.US, "%02d:%02d", var7, var8);
      }

      int var9 = SQTime.getMonthOriginal(var0);
      int var10 = SQTime.getDay(var0);
      return String.format(Locale.US, "%02d-%02d %02d:%02d", var9, var10, var7, var8);
   }

   protected long sumTPOCounts() {
      long var1 = 0L;
      if (this.tpoBins == null) {
         return 0L;
      }

      for (int var3 = 0; var3 < this.tpoBins.length; var3++) {
         var1 += this.tpoBins[var3];
      }

      return var1;
   }

   protected static String buildLettersString(long var0, int var2) {
      if (var2 > 0 && var0 != 0L) {
         StringBuilder var3 = new StringBuilder();

         for (int var4 = 0; var4 < var2; var4++) {
            if ((var0 >>> var4 & 1L) != 0L) {
               var3.append(bracketChar(var4));
            }
         }

         return var3.toString();
      } else {
         return "";
      }
   }

   protected static char bracketChar(int var0) {
      if (var0 < 26) {
         return (char)(65 + var0);
      } else if (var0 < 52) {
         return (char)(97 + (var0 - 26));
      } else {
         return var0 < 62 ? (char)(48 + (var0 - 52)) : '?';
      }
   }

   protected static String bracketColor(int var0) {
      return var0 < 0 ? "#000000" : TPO_LETTER_PALETTE[var0 % TPO_LETTER_PALETTE.length];
   }

   protected long getBracketMillis() {
      int var1;
      if (this.cfgSessionType() == 1 || this.cfgSessionType() == 5) {
         var1 = this.cfgBracketMinDaily();
      } else if (this.cfgSessionType() == 2 || this.cfgSessionType() == 6) {
         var1 = this.cfgBracketMinWeekly();
      } else if (this.cfgSessionType() == 3 || this.cfgSessionType() == 7) {
         var1 = this.cfgBracketMinMonthly();
      } else if (this.cfgSessionType() != 4 && this.cfgSessionType() != 8) {
         var1 = this.cfgBracketMinDaily();
      } else {
         var1 = this.cfgBracketMinYearly();
      }

      return var1 * 60L * 1000L;
   }

   protected long getIBPeriodMillis() {
      byte var1;
      if (this.cfgSessionType() == 1 || this.cfgSessionType() == 5) {
         var1 = 1;
      } else if (this.cfgSessionType() == 2 || this.cfgSessionType() == 6) {
         var1 = 2;
      } else if (this.cfgSessionType() == 3 || this.cfgSessionType() == 7) {
         var1 = 3;
      } else if (this.cfgSessionType() != 4 && this.cfgSessionType() != 8) {
         var1 = 1;
      } else {
         var1 = 4;
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
         default:
            return 3600000L;
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

   protected abstract int cfgIBMinutes();

   protected abstract int cfgSessionType();

   @Override
   protected abstract boolean cfgShowCandlesticks();

   protected abstract boolean cfgShowVolumeSubchart();

   protected abstract int cfgVolumeMALength();

   protected abstract boolean cfgShowPOCDelta();

   protected abstract boolean cfgShowVADelta();

   protected abstract boolean cfgShowProfileRange();

   protected abstract boolean cfgShowPOCPosition();

   protected abstract int cfgBracketMinDaily();

   protected abstract int cfgBracketMinWeekly();

   protected abstract int cfgBracketMinMonthly();

   protected abstract int cfgBracketMinYearly();

   protected abstract boolean cfgShowShapeLabel();

   protected abstract boolean cfgUseBlockMode();
}
