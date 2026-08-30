package com.strategyquant.tradinglib.project.console;

import com.strategyquant.lib.SQTime;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CLILogger {
   private static final Logger Log = LoggerFactory.getLogger(CLILogger.class);
   public static final String Separator = "--------------------------------------------------";
   private PrintWriter writer;
   private static CLILogger instance;
   public static String lastCommandOutput;

   public static CLILogger getInstance() {
      if (instance == null) {
         instance = new CLILogger();
      }

      return instance;
   }

   public static void redirectOutputToFile(String var0) throws Exception {
      CLILogger var1 = getInstance();
      if (var0 == null) {
         closeFile();
      } else {
         try {
            File var2 = new File(var0);
            if (var2.getParentFile() != null && !var2.getParentFile().exists()) {
               var2.getParentFile().mkdirs();
            }

            var1.writer = new PrintWriter(new BufferedWriter(new FileWriter(var2, true)), true);
         } catch (Exception var3) {
            Log.error(String.format("Cannot redirect output to file. Reason: %s", var3.getMessage()));
            throw new Exception(String.format("Cannot redirect output to file. Reason: %s", var3.getMessage()));
         }
      }
   }

   public static void closeFile() {
      CLILogger var0 = getInstance();
      if (var0.writer != null) {
         try {
            var0.writer.flush();
            var0.writer.close();
         } catch (Exception var2) {
         }

         var0.writer = null;
      }
   }

   public static void log(String var0, String var1, int var2) {
      StringBuilder var3 = new StringBuilder();
      int var4 = var2 - var0.length() - var1.length();
      var3.append(var0);

      for (int var5 = 0; var5 < var4; var5++) {
         var3.append(" ");
      }

      var3.append(var1);
      log(var3.toString(), false);
   }

   public static void log(String var0, boolean var1) {
      if (!var0.equals("{}") && !var0.trim().isEmpty()) {
         var0 = var0.replaceAll(Pattern.quote("<br>"), "\n");
         CLILogger var2 = getInstance();
         if (var1) {
            var0 = SQTime.getLogTime(false, false) + " " + var0 + "\n" + "--------------------------------------------------";
         }

         lastCommandOutput = lastCommandOutput + var0 + "\n";
         if (var2.writer != null) {
            var2.writer.println(var0);
         } else {
            Log.info(var0);
         }
      }
   }

   public static void log(String var0) {
      log(var0, false);
   }
}
