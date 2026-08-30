package com.strategyquant.pluginlib;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class DirManager {
   private static String originPath;

   public static void usePathsRelativeTo(String var0) {
      originPath = var0;
      originPath = originPath.replace("\\", "/");

      while (originPath.endsWith("/")) {
         originPath = originPath.substring(0, originPath.length() - 1);
      }
   }

   public static void useAbsolutePaths() {
      originPath = null;
   }

   public static ArrayList<String> listAllSubdirectoryPaths(String var0) throws Exception {
      ArrayList var1 = new ArrayList();
      ArrayList var2 = getSubdirectoryNames(var0);
      var1.addAll(var2);
      ArrayList var3 = var2;

      while (!(var3 = getSubdirectoriesNames(var3)).isEmpty()) {
         var1.addAll(var3);
      }

      return var1;
   }

   public static ArrayList<String> getSubdirectoryNames(String var0) throws Exception {
      ArrayList var1 = new ArrayList();
      File var2 = new File(var0);
      if (var2.exists() && var2.isDirectory()) {
         File[] var3 = var2.listFiles();

         for (File var7 : var3) {
            if (var7.isDirectory()) {
               String var8 = var7.getAbsolutePath();
               if (!var8.contains(".svn")) {
                  var1.add(formatPath(var8));
               }
            }
         }

         Collections.sort(var1);
         return var1;
      } else {
         return var1;
      }
   }

   private static ArrayList<String> getSubdirectoriesNames(ArrayList<String> var0) throws Exception {
      ArrayList var1 = new ArrayList();

      for (String var3 : var0) {
         var1.addAll(getSubdirectoryNames(var3));
      }

      return var1;
   }

   public static ArrayList<String> listFileNamesInFolder(String var0) {
      ArrayList var1 = new ArrayList();
      File var2 = new File(var0);
      if (var2.exists()) {
         for (File var6 : var2.listFiles()) {
            if (!var6.isDirectory()) {
               String var7 = var6.getAbsolutePath();
               var1.add(formatPath(var7));
            }
         }
      }

      return var1;
   }

   private static String formatPath(String var0) {
      return var0.replace("\\", "/");
   }
}
