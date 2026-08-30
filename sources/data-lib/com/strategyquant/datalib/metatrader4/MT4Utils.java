package com.strategyquant.datalib.metatrader4;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MT4Utils {
   private static final Logger Log = LoggerFactory.getLogger("MT4Utils");
   private static final Set<String> IGNORE_FOLDER_SET = new HashSet<>();

   public static final MT4Utils.MetaTraderLocation getDataFolder(String var0) {
      var0 = var0.toLowerCase().trim();
      new File(var0);
      File var2 = getDataFolderFile();
      File[] var3 = var2.listFiles();
      if (var3 == null) {
         return null;
      } else {
         String var4 = getTerminalHash(var0);
         String var5 = var2.getAbsolutePath() + "\\" + var4;
         if (new File(var5).exists()) {
            String[] var6 = getServerNames(var5);
            return new MT4Utils.MetaTraderLocation(var5, var6);
         } else {
            return null;
         }
      }
   }

   public static String getTerminalHash(String var0) {
      try {
         var0 = var0.toUpperCase().replace("/", "\\");
         if (var0.endsWith("\\")) {
            var0 = var0.substring(0, var0.length() - 1);
         }

         return Hex.encodeHexString(MessageDigest.getInstance("MD5").digest(var0.toUpperCase().getBytes("UTF-16LE"))).toUpperCase();
      } catch (Exception var2) {
         Log.error("Cannot get terminal folder hash", var2);
         return null;
      }
   }

   public static File getDataFolderFile() {
      String var0 = System.getProperty("user.home");
      String var1 = var0 + "\\AppData\\Roaming\\MetaQuotes\\Terminal";
      return new File(var1);
   }

   public static String[] getServerNames(String var0) {
      File[] var1 = new File(var0, "history").listFiles();
      LinkedList var2 = new LinkedList();
      if (var1 != null) {
         for (File var6 : var1) {
            if (!var6.isFile() && !IGNORE_FOLDER_SET.contains(var6.getName())) {
               String[] var7 = var6.list(new FilenameFilter() {
                  @Override
                  public boolean accept(File var1, String var2x) {
                     return var2x.endsWith("hst");
                  }
               });
               if (var7 != null && var7.length > 0) {
                  var2.add(var6.getName());
               }
            }
         }
      }

      return var2.toArray(new String[0]);
   }

   private static String readOriginFile(File var0) {
      File var1 = new File(var0, "origin.txt");
      if (!var1.exists()) {
         return null;
      }

      try {
         String var2 = new String(Files.readAllBytes(var1.toPath()), "UTF-16LE");
         if (var2.length() > 0) {
            var2 = var2.substring(1);
         }

         return var2;
      } catch (IOException var3) {
         return null;
      }
   }

   public static void main(String[] var0) {
      MT4Utils.MetaTraderLocation var1 = getDataFolder("C:\\Program Files (x86)\\MetaTrader 4");
      System.out.println(var1.getDataFolder() + " " + var1.getServerNames());
   }

   static {
      IGNORE_FOLDER_SET.add("default");
      IGNORE_FOLDER_SET.add("downloads");
      IGNORE_FOLDER_SET.add("deleted");
   }

   public static class MetaTraderLocation {
      private String dataFolder;
      private String[] serverNames;

      public MetaTraderLocation(String var1, String[] var2) {
         this.dataFolder = var1;
         this.serverNames = var2;
      }

      public String getDataFolder() {
         return this.dataFolder;
      }

      public String[] getServerNames() {
         return this.serverNames;
      }
   }
}
