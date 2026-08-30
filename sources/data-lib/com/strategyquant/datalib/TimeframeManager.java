package com.strategyquant.datalib;

import com.strategyquant.datalib.timeframe.TimeframesComparator;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.constants.SQPaths;
import java.io.File;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeframeManager {
   public static final Logger Log = LoggerFactory.getLogger(TimeframeManager.class);
   public static final String TF_UNKNOWN = "unknown";
   public static final String TF_INTRADAY = "Intraday";
   public static final int TF_INT_UNKNOWN = -2;
   public static final String TF_TICK = "TICK";
   public static final String TF_M1 = "M1";
   public static final String TF_M3 = "M3";
   public static final String TF_M5 = "M5";
   public static final String TF_M15 = "M15";
   public static final String TF_M30 = "M30";
   public static final String TF_H1 = "H1";
   public static final String TF_H2 = "H2";
   public static final String TF_H3 = "H3";
   public static final String TF_H4 = "H4";
   public static final String TF_H6 = "H6";
   public static final String TF_H8 = "H8";
   public static final String TF_H12 = "H12";
   public static final String TF_D1 = "D1";
   public static final String TF_WEEKLY = "Weekly";
   public static final String TF_MONTHLY = "Monthly";
   private static final long MINUTE_MILLIS = 60000L;
   private static final long HOUR_MILLIS = 3600000L;
   private static final long DAY_MILLIS = 86400000L;
   private static final long WEEK_MILLIS = 604800000L;
   private static final long MONTH_MILLIS = 2419200000L;
   public static final int PRECISION_SELECTED_TF = 1;
   public static final int PRECISION_BASE_TF = 2;
   public static String filePath = SQPaths.settingsDirPath + "/customTimeframes.txt";
   private static final String delimiter = "\r\n";
   private ArrayList<String> defaultTimeframes = new ArrayList<>();
   private ArrayList<String> customTimeframes = new ArrayList<>();
   private static TimeframeManager instance;

   public static synchronized TimeframeManager getInstance() {
      if (instance == null) {
         instance = new TimeframeManager();
      }

      return instance;
   }

   private TimeframeManager() {
      this.defaultTimeframes.clear();
      this.defaultTimeframes.add("TICK");
      this.defaultTimeframes.add("M1");
      this.defaultTimeframes.add("M5");
      this.defaultTimeframes.add("M15");
      this.defaultTimeframes.add("M30");
      this.defaultTimeframes.add("H1");
      this.defaultTimeframes.add("H4");
      this.defaultTimeframes.add("D1");
      this.defaultTimeframes.add("Weekly");
      this.defaultTimeframes.add("Monthly");
      File var1 = new File(filePath);
      if (var1.exists()) {
         try {
            SQUtils.fileToArrayList(this.customTimeframes, filePath, "\r\n");

            while (this.customTimeframes.contains("")) {
               this.customTimeframes.remove("");
            }
         } catch (Exception var3) {
            Log.error("Cannot load custom timeframes file. ", var3);
         }
      } else {
         Log.warn("Custom timeframes file doesn't exist.");
      }
   }

   private boolean isValid(String var1) {
      if (var1 == null || var1.isEmpty()) {
         return false;
      }

      if (var1.equals("Monthly")) {
         return true;
      }

      if (var1.equals("Weekly")) {
         return true;
      }

      if (!var1.startsWith("S") && !var1.startsWith("M") && !var1.startsWith("H") && !var1.startsWith("D")) {
         return false;
      }

      try {
         Integer.parseInt(var1.substring(1));
         return true;
      } catch (Exception var3) {
         return false;
      }
   }

   public static ArrayList<String> getTimeframes() {
      return getInstance()._getTimeframes();
   }

   public static ArrayList<String> getPredefinedTimeframes() {
      ArrayList var0 = new ArrayList();
      var0.add("TICK");
      var0.add("M1");
      var0.add("M3");
      var0.add("M5");
      var0.add("M15");
      var0.add("M30");
      var0.add("H1");
      var0.add("H2");
      var0.add("H3");
      var0.add("H4");
      var0.add("H6");
      var0.add("H8");
      var0.add("H12");
      var0.add("D1");
      var0.add("Weekly");
      var0.add("Monthly");
      return var0;
   }

   public static void addTimeframe(String var0) throws Exception {
      getInstance()._addTimeframe(var0);
   }

   public static String translateSQ3TFToString(int var0) {
      switch (var0) {
         case -10:
            return "Intraday";
         case -2:
            return "unknown";
         case 0:
            return "TICK";
         case 1:
            return "M1";
         case 5:
            return "M5";
         case 14:
            return "M14";
         case 15:
            return "M15";
         case 30:
            return "M30";
         case 53:
            return "M53";
         case 60:
            return "H1";
         case 120:
            return "H2";
         case 240:
            return "H4";
         case 1440:
            return "D1";
         case 10080:
            return "Weekly";
         case 43200:
            return "Monthly";
         default:
            return "unknown";
      }
   }

   public static int translateToMTConstant(String var0) {
      switch (var0) {
         case "TICK":
            return 0;
         case "M1":
            return 1;
         case "M5":
            return 5;
         case "M14":
            return 14;
         case "M15":
            return 15;
         case "M30":
            return 30;
         case "M53":
            return 53;
         case "H1":
            return 60;
         case "H2":
            return 120;
         case "H4":
            return 240;
         case "D1":
            return 1440;
         case "Weekly":
            return 10080;
         case "Monthly":
            return 43200;
         case "Intraday":
            return -10;
         default:
            return -2;
      }
   }

   public static String M(int var0) {
      return "M" + Integer.toString(var0);
   }

   public static long getMillis(String var0) throws TradingException {
      if (var0.equals("TICK") || var0.equals("Intraday")) {
         return 0L;
      }

      if (var0.equals("Weekly")) {
         return 604800000L;
      }

      if (var0.equals("Monthly")) {
         return 2419200000L;
      }

      String var1 = var0.substring(0, 1);
      int var2 = 0;

      try {
         var2 = Integer.parseInt(var0.substring(1));
      } catch (Exception var5) {
         throw new TradingException("Invalid timeframe format. Cannot parse time units count.");
      }

      switch (var1) {
         case "S":
            return var2 * 1000;
         case "M":
            return var2 * 60000L;
         case "H":
            return var2 * 3600000L;
         case "D":
            return var2 * 86400000L;
         default:
            throw new TradingException("Invalid timeframe format. Prefix '" + var1 + "' not recognized.");
      }
   }

   public static boolean isTimeFrameLessThen(String var0, String var1) throws TradingException {
      long var2 = getMillis(var0);
      long var4 = getMillis(var1);
      return var2 < var4;
   }

   public static long getTFHash(String var0) throws TradingException {
      if (var0.equals("TICK")) {
         return 0L;
      }

      if (var0.equals("Intraday")) {
         return -10L;
      }

      if (var0.equals("Weekly")) {
         return 604800700L;
      }

      if (var0.equals("Monthly")) {
         return 2419200800L;
      }

      String var1 = var0.substring(0, 1);
      int var2 = 0;

      try {
         var2 = Integer.parseInt(var0.substring(1));
      } catch (Exception var5) {
         throw new TradingException("Invalid timeframe format. Cannot parse time units count.");
      }

      switch (var1) {
         case "S":
            return var2 * 1000 + 300;
         case "M":
            return var2 * 60000L + 400L;
         case "H":
            return var2 * 3600000L + 500L;
         case "D":
            return var2 * 86400000L + 600L;
         default:
            throw new TradingException("Invalid timeframe format. Prefix '" + var1 + "' not recognized.");
      }
   }

   public static String getTFName(long var0) {
      if (var0 < 0L) {
         return "Intraday";
      }

      if (var0 == 0L) {
         return "TICK";
      }

      String var2 = "";
      long var3 = 1L;
      if (var0 < 60000L) {
         var2 = "S";
         var3 = 1000L;
      } else if (var0 < 3600000L) {
         var2 = "M";
         var3 = 60000L;
      } else if (var0 < 86400000L) {
         if (var0 % 3600000L != 0L && var0 % 60000L == 0L) {
            var2 = "M";
            var3 = 60000L;
         } else {
            var2 = "H";
            var3 = 3600000L;
         }
      } else {
         if (var0 >= 604800000L) {
            if (var0 < 2419200000L) {
               return "Weekly";
            }

            return "Monthly";
         }

         var2 = "D";
         var3 = 86400000L;
      }

      return var2 + var0 / var3;
   }

   public static boolean TFExists(String var0) {
      return getInstance().defaultTimeframes.contains(var0) || getInstance().customTimeframes.contains(var0);
   }

   private ArrayList<String> _getTimeframes() {
      ArrayList var1 = new ArrayList<>(this.defaultTimeframes);
      var1.addAll(this.customTimeframes);
      var1.sort(new TimeframesComparator());
      return var1;
   }

   private void _addTimeframe(String var1) throws Exception {
      if (this.customTimeframes.contains(var1) || this.defaultTimeframes.contains(var1)) {
         throw new Exception("Timeframe '" + var1 + "' already exists.");
      }

      if (this.isValid(var1)) {
         this.customTimeframes.add(var1);
         SQUtils.arrayListToFile(this.customTimeframes, filePath, "\r\n");
      } else {
         throw new Exception("Timeframe '" + var1 + "' has invalid format.");
      }
   }
}
