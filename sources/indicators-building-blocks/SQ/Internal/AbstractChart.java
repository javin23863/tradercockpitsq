package SQ.Internal;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.results.SpecialValues;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractChart extends IndicatorBlock {
   private static final Logger Log = LoggerFactory.getLogger(AbstractChart.class);
   protected static final int DEFAULT_MAX_SESSIONS_FOR_SVG = 20;
   protected long currentSessionStart = 0L;
   protected long currentSessionEnd = 0L;
   protected long prevSessionStart = 0L;
   protected long prevSessionEnd = 0L;
   protected double prevIBH = 0.0;
   protected double prevIBL = 0.0;
   protected double prevTotalVolume = 0.0;
   protected double prevTotalBullVolume = 0.0;
   protected double prevTotalBearVolume = 0.0;
   protected boolean debugEnabled = false;
   protected int lastNumBins = 0;
   protected static final int MAX_BINS = 2000;
   protected double[] clusterBins;
   protected String fileRandomSuffix;
   private boolean storeChartData = false;
   private boolean storeChartDataLoaded = false;
   protected int historyCount = 0;
   protected long[] histSessionStart;
   protected long[] histSessionEnd;
   protected double[] histIBH;
   protected double[] histIBL;
   protected double[] histSessionHigh;
   protected double[] histSessionLow;
   protected int[] histNumBins;
   protected double[] histTotalVolume;
   protected double[] histBullVolume;
   protected double[] histBearVolume;
   protected int M1chartNumber = -1;

   protected abstract void ensureArrays();

   protected abstract void exportMultiSessionSVG();

   protected abstract ChartData cfgChart();

   protected abstract int cfgProfileRows();

   protected abstract int cfgBinSizeMode();

   protected boolean cfgStoreChartData() {
      if (!this.storeChartDataLoaded) {
         try {
            StrategyBase var1 = this.getStrategy();
            if (var1 != null) {
               SettingsMap var2 = var1.getSettings();
               if (var2 != null) {
                  this.storeChartData = var2.getBoolean("StoreChartData", false);
               }
            }
         } catch (Exception var3) {
            Log.error("Failed to read StoreChartData from strategy settings", var3);
         }

         this.storeChartDataLoaded = true;
      }

      return this.storeChartData;
   }

   protected abstract boolean cfgShowCandlesticks();

   protected void OnInit() throws TradingException {
      this.ensureArrays();
      this.fileRandomSuffix = UUID.randomUUID().toString().substring(0, 8);
   }

   protected void OnDeinit() throws TradingException {
      if (this.cfgStoreChartData() && this.historyCount > 0) {
         this.exportMultiSessionSVG();
      }
   }

   protected String resolveExportFolder() {
      String var1 = MainApp.getDataPath() + "internal/tmp/profilechart";
      String var2 = Paths.get(var1).normalize().toString();
      var2 = var2.replace("\\", File.separator).replace("/", File.separator);

      try {
         File var3 = new File(var2);
         if (!var3.exists()) {
            var3.mkdirs();
         }
      } catch (Exception var4) {
         var2 = ".";
      }

      return var2;
   }

   public void triggerExport() {
      if (this.historyCount > 0) {
         this.exportMultiSessionSVG();
      }
   }

   protected static String joinPath(String var0, String var1) {
      return var0.endsWith(File.separator) ? var0 + var1 : var0 + File.separator + var1;
   }

   protected void saveChartPath(String var1) {
      StrategyBase var2 = this.getStrategy();
      if (var2 != null) {
         SettingsMap var3 = var2.getSpecialValues();
         if (var3 != null) {
            String var4 = var3.getString(SpecialValues.ProfileChartPaths, "");
            String var5 = var4.isEmpty() ? var1 : var4 + "," + var1;
            var3.setString(SpecialValues.ProfileChartPaths, var5);
         }
      }
   }

   protected void drawSvgLevel(BufferedWriter var1, int var2, int var3, int var4, int var5, double var6, double var8, double var10, String var12, String var13) throws IOException {
      double var14 = var4 + var5 - (var10 - var6) / var8 * var5;
      if (!(var14 < var4) && !(var14 > var4 + var5)) {
         var1.write(
            "<line x1=\""
               + var2
               + "\" y1=\""
               + var14
               + "\" x2=\""
               + (var2 + var3)
               + "\" y2=\""
               + var14
               + "\" stroke=\""
               + var12
               + "\" stroke-width=\"1\" stroke-dasharray=\"4,2\" opacity=\"0.8\" vector-effect=\"non-scaling-stroke\"/>\n"
         );
         var1.write(
            "<text x=\""
               + (var2 + var3 - 3)
               + "\" y=\""
               + (var14 - 3.0)
               + "\" font-family=\"Sans-Serif\" font-size=\"9\" fill=\""
               + var12
               + "\" text-anchor=\"end\" font-weight=\"700\">"
               + var13
               + "</text>\n"
         );
      }
   }

   protected void drawSvgCandlesUnified(
      BufferedWriter var1, int var2, int var3, int var4, int var5, double var6, double var8, long var10, long var12, long var14, double var16
   ) throws IOException {
      ChartData var18 = this.cfgChart();
      if (var18 != null && var14 > 0L) {
         int var19 = 0;
         int var20 = 0;

         try {
            while (true) {
               long var21 = var18.Time(var20);
               if (var21 < var10) {
                  break;
               }

               if (var21 >= var10 && var21 < var12) {
                  var19++;
               }

               var20++;
            }
         } catch (Exception var56) {
         }

         if (var19 != 0) {
            double var58 = Math.max(1.0, (double)var3 / var19 * 0.7);
            double var23 = (double)var3 / var19;
            var20 = 0;
            int var25 = 0;

            try {
               while (true) {
                  long var26 = var18.Time(var20);
                  if (var26 < var10) {
                     break;
                  }

                  if (var26 >= var10 && var26 < var12) {
                     double var28 = var18.Open(var20);
                     double var30 = var18.High(var20);
                     double var32 = var18.Low(var20);
                     double var34 = var18.Close(var20);
                     double var36 = var2 + (var19 - 1 - var25) * var23;
                     double var38 = var4 + var5 - (var30 - var6) / var8 * var5;
                     double var40 = var4 + var5 - (var32 - var6) / var8 * var5;
                     double var42 = var4 + var5 - (var28 - var6) / var8 * var5;
                     double var44 = var4 + var5 - (var34 - var6) / var8 * var5;
                     boolean var46 = var34 >= var28;
                     String var47 = var46 ? "#26a69a" : "#ef5350";
                     String var48 = String.format(Locale.US, "%.2f", var16);
                     double var49 = var36 + var58 / 2.0;
                     var1.write(
                        "<line x1=\""
                           + var49
                           + "\" y1=\""
                           + var38
                           + "\" x2=\""
                           + var49
                           + "\" y2=\""
                           + var40
                           + "\" stroke=\""
                           + var47
                           + "\" stroke-width=\"0.8\" stroke-opacity=\""
                           + var48
                           + "\"/>\n"
                     );
                     double var51 = Math.min(var42, var44);
                     double var53 = Math.max(0.5, Math.abs(var44 - var42));
                     var1.write(
                        "<rect x=\""
                           + var36
                           + "\" y=\""
                           + var51
                           + "\" width=\""
                           + var58
                           + "\" height=\""
                           + var53
                           + "\" fill=\""
                           + var47
                           + "\" fill-opacity=\""
                           + var48
                           + "\"/>\n"
                     );
                     var25++;
                  }

                  var20++;
               }
            } catch (Exception var55) {
            }
         }
      }
   }

   protected void drawSvgCandlesWithIds(
      BufferedWriter var1, int var2, int var3, int var4, int var5, double var6, double var8, long var10, long var12, long var14, double var16
   ) throws IOException {
      ChartData var18 = this.cfgChart();
      if (var18 != null && var14 > 0L) {
         int var19 = 0;
         int var20 = 0;

         try {
            while (true) {
               long var21 = var18.Time(var20);
               if (var21 < var10) {
                  break;
               }

               if (var21 >= var10 && var21 < var12) {
                  var19++;
               }

               var20++;
            }
         } catch (Exception var57) {
         }

         if (var19 != 0) {
            double var59 = Math.max(1.0, (double)var3 / var19 * 0.7);
            double var23 = (double)var3 / var19;
            var20 = 0;
            int var25 = 0;

            try {
               while (true) {
                  long var26 = var18.Time(var20);
                  if (var26 < var10) {
                     break;
                  }

                  if (var26 >= var10 && var26 < var12) {
                     double var28 = var18.Open(var20);
                     double var30 = var18.High(var20);
                     double var32 = var18.Low(var20);
                     double var34 = var18.Close(var20);
                     double var36 = var2 + (var19 - 1 - var25) * var23;
                     double var38 = var4 + var5 - (var30 - var6) / var8 * var5;
                     double var40 = var4 + var5 - (var32 - var6) / var8 * var5;
                     double var42 = var4 + var5 - (var28 - var6) / var8 * var5;
                     double var44 = var4 + var5 - (var34 - var6) / var8 * var5;
                     boolean var46 = var34 >= var28;
                     String var47 = var46 ? "#26a69a" : "#ef5350";
                     String var48 = String.format(Locale.US, "%.2f", var16);
                     double var49 = var36 + var59 / 2.0;
                     int var51 = var19 - 1 - var25;
                     var1.write("<g id=\"rc_" + var51 + "\">");
                     var1.write(
                        "<line x1=\""
                           + var49
                           + "\" y1=\""
                           + var38
                           + "\" x2=\""
                           + var49
                           + "\" y2=\""
                           + var40
                           + "\" stroke=\""
                           + var47
                           + "\" stroke-width=\"0.8\" stroke-opacity=\""
                           + var48
                           + "\"/>"
                     );
                     double var52 = Math.min(var42, var44);
                     double var54 = Math.max(0.5, Math.abs(var44 - var42));
                     var1.write(
                        "<rect x=\""
                           + var36
                           + "\" y=\""
                           + var52
                           + "\" width=\""
                           + var59
                           + "\" height=\""
                           + var54
                           + "\" fill=\""
                           + var47
                           + "\" fill-opacity=\""
                           + var48
                           + "\"/>"
                     );
                     var1.write("</g>\n");
                     var25++;
                  }

                  var20++;
               }
            } catch (Exception var56) {
            }
         }
      }
   }

   protected void drawSvgTimeAxis(BufferedWriter var1, int var2, int var3, int var4, int var5, long var6, long var8, String var10) throws IOException {
      ChartData var11 = this.cfgChart();
      if (var11 != null) {
         int var12 = 0;
         int var13 = 0;

         try {
            while (true) {
               long var14 = var11.Time(var13);
               if (var14 < var6) {
                  break;
               }

               if (var14 >= var6 && var14 < var8) {
                  var12++;
               }

               var13++;
            }
         } catch (Exception var28) {
         }

         if (var12 != 0) {
            long[] var30 = new long[var12];
            var13 = 0;
            int var15 = 0;

            try {
               while (true) {
                  long var16 = var11.Time(var13);
                  if (var16 < var6) {
                     break;
                  }

                  if (var16 >= var6 && var16 < var8) {
                     var30[var15++] = var16;
                  }

                  var13++;
               }
            } catch (Exception var27) {
            }

            double var31 = (double)var3 / var12;
            int var18 = Math.max(1, (int)Math.ceil(var12 * 14.0 / var3));
            SimpleDateFormat var19 = new SimpleDateFormat(var10, Locale.US);

            for (int var20 = 0; var20 < var12; var20 += var18) {
               long var21 = var30[var12 - 1 - var20];
               double var23 = var2 + var20 * var31 + var31 / 2.0;
               String var25 = String.format(Locale.US, "%.1f", var23);
               var1.write(
                  "<line x1=\""
                     + var25
                     + "\" y1=\""
                     + (var4 + var5)
                     + "\" x2=\""
                     + var25
                     + "\" y2=\""
                     + (var4 + var5 + 4)
                     + "\" stroke=\"#555\" stroke-width=\"0.5\"/>\n"
               );
               int var26 = var4 + var5 + 6;
               var1.write(
                  "<text x=\""
                     + var25
                     + "\" y=\""
                     + var26
                     + "\" transform=\"rotate(-45,"
                     + var25
                     + ","
                     + var26
                     + ")\" font-family=\"Monospace\" font-size=\"9\" fill=\"#888\" text-anchor=\"end\">"
                     + var19.format(new Date(var21))
                     + "</text>\n"
               );
            }
         }
      }
   }

   protected static int clampInt(int var0, int var1, int var2) {
      return Math.max(var1, Math.min(var2, var0));
   }

   protected int yLabelStepForDenseAxis() {
      int var1 = this.lastNumBins > 0 ? this.lastNumBins : this.cfgProfileRows();
      return var1 <= 150 ? 1 : (int)Math.ceil(var1 / 150.0);
   }

   protected String formatForFilename(long var1) {
      SimpleDateFormat var3 = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US);
      return var3.format(new Date(var1));
   }

   protected static String svgEsc(String var0) {
      return var0 == null ? "" : var0.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&apos;");
   }

   protected boolean isSunday(long var1) {
      return SQTime.getDayOfWeek(var1) == 7;
   }

   public void debug(String var1, String var2) {
      if (this.debugEnabled) {
         super.debug(var1, var2);
      }
   }
}
