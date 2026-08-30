package com.strategyquant.webguilib.init;

import com.strategyquant.lib.SQUtils;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WebInitializer {
   public static final Logger Log = LoggerFactory.getLogger(WebInitializer.class);
   private static FileOutputStream outstream;
   private static final String dependenciesMarker = "#sq-dependencies";
   private static ArrayList<String> moduleNames = new ArrayList<>();

   public static void createMergedJS(String var0, boolean var1) throws Exception {
      createMergedFile(var0, var1);
      moduleNames.clear();
   }

   public static void createMergedFile(String var0, boolean var1) throws Exception {
      File var2 = SQUtils.createFile(var0);
      outstream = new FileOutputStream(var2, !var1);
   }

   public static void appendJS(String var0) throws Exception {
      if (outstream != null) {
         fileToFile(new File(var0));
         stringToFile("\n");
         String var1 = SQUtils.fileToString(new File(var0));

         try {
            tryLoadModuleName(var1);
         } catch (Exception var3) {
            throw new Exception("Error while compiling JS files - File: " + var0 + ", Error: " + var3.getMessage(), var3);
         }
      } else {
         throw new Exception("Output file does not exist. You must call createMergedJS function first.");
      }
   }

   private static void tryLoadModuleName(String var0) throws Exception {
      if (var0.contains("angular.module")) {
         int var1 = var0.indexOf("angular.module");
         int var2 = var0.indexOf("(", var1);
         if (var2 < 0) {
            throw new Exception("Cannot load angular module name - bracket not found");
         }

         int var3 = var0.indexOf(")", var2 + 1);
         if (var3 < 0) {
            throw new Exception("Cannot load angular module name - end bracket not found");
         }

         String var4 = var0.substring(var2 + 1, var3);
         var4 = var4.replace(" ", "").replace("\t", "");
         String var5 = var4.substring(0, 1);
         int var6 = var4.indexOf(var5, 1);
         if (var6 < 0) {
            throw new Exception("Cannot load angular module name - End quote " + var5 + " not found");
         }

         String var7 = "'" + var4.substring(1, var6) + "'";
         if (var7.contains("app.") && !moduleNames.contains(var7)) {
            moduleNames.add(var7);
         }
      }
   }

   public static void appendFile(String var0) throws Exception {
      if (outstream != null) {
         fileToFile(new File(var0));
         stringToFile("\n");
      } else {
         throw new Exception("Output file does not exist. You must call createMergedFile function first.");
      }
   }

   public static void appendHTMLTemplateFile(String var0) throws Exception {
      if (outstream != null) {
         String var1 = "<script type=\"text/ng-template\" id=\"" + getFileURLPath(var0) + "\">\n";
         stringToFile(var1);
         fileToFile(new File(var0));
         stringToFile("\n</script>\n");
      } else {
         throw new Exception("Output file does not exist. You must call createMergedFile function first.");
      }
   }

   private static String getFileURLPath(String var0) {
      var0 = var0.replace("\\", "/");
      String var1 = "/internal/web/";
      String var2 = "/internal/plugins/";
      if (var0.contains(var1)) {
         int var5 = var0.indexOf(var1);
         return "../" + var0.substring(var5 + var1.length());
      } else if (var0.contains(var2)) {
         int var3 = var0.indexOf(var2);
         return "../../.." + var0.substring(var3);
      } else {
         Log.error("Incorrect path: " + var0);
         return null;
      }
   }

   public static void createAppJSFromTemplate(String var0, String var1) throws Exception {
      String var2 = SQUtils.fileToString(new File(var0));
      String var3 = getDependencies();
      var2 = var2.replace("#sq-dependencies", var3);
      PrintWriter var4 = new PrintWriter(new FileWriter(var1, false));

      try {
         var4.write(var2);
      } catch (Throwable var8) {
         try {
            var4.close();
         } catch (Throwable var7) {
            var8.addSuppressed(var7);
         }

         throw var8;
      }

      var4.close();
   }

   private static String getDependencies() {
      String var0 = "";

      for (String var2 : moduleNames) {
         var0 = var0 + "," + var2 + "\n";
      }

      return var0;
   }

   public static void finishFile() {
      try {
         outstream.close();
      } catch (Exception var1) {
         Log.error("Cannot close file outputstream.", var1);
      }
   }

   public static void fileToFile(File var0) throws Exception {
      FileInputStream var1 = new FileInputStream(var0);

      try {
         byte[] var2 = new byte[1024];

         int var3;
         while ((var3 = var1.read(var2)) > 0) {
            outstream.write(var2, 0, var3);
         }
      } finally {
         var1.close();
      }
   }

   public static void stringToFile(String var0) throws Exception {
      byte[] var1 = var0.getBytes();
      outstream.write(var1, 0, var1.length);
   }
}
