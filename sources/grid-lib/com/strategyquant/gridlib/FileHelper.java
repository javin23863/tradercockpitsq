package com.strategyquant.gridlib;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileHelper {
   private static final Logger LOGGER = LoggerFactory.getLogger(FileHelper.class);
   private static final int BUFFER_SIZE = 10240;

   private static void generateFileList(String var0, File var1, List<String> var2) {
      if (var1.isFile()) {
         var2.add(generateZipEntry(var0, var1.toString()));
      }

      if (var1.isDirectory()) {
         String[] var3 = var1.list();

         for (String var7 : var3) {
            generateFileList(var0, new File(var1, var7), var2);
         }
      }
   }

   private static String generateZipEntry(String var0, String var1) {
      return var1.substring(var0.length() + 1, var1.length());
   }

   public static String countHash(String var0) throws IOException {
      StringBuilder var1 = new StringBuilder();
      List var2 = getPaths(var0);
      int var3 = var0.length();

      for (String var5 : var2) {
         String var6 = var5.substring(var3 + 1);
         if (var1.length() > 0) {
            var1.append("\n");
         }

         var1.append(countHash(new File(var5)) + " " + var6);
      }

      return var1.toString();
   }

   private static List<String> getPaths(String var0) {
      LinkedList var1 = new LinkedList();
      fillPaths(var1, var0);
      return var1;
   }

   private static void fillPaths(List<String> var0, String var1) {
      File var2 = new File(var1);
      File[] var3 = var2.listFiles();
      if (var3 != null) {
         for (File var7 : var3) {
            if (var7.isDirectory()) {
               fillPaths(var0, var7.getAbsolutePath());
            } else {
               var0.add(var7.getAbsolutePath());
            }
         }
      }
   }

   public static String countHash(File var0) throws IOException {
      FileInputStream var1 = new FileInputStream(var0);
      String var2 = DigestUtils.md5Hex(var1);
      var1.close();
      return var2;
   }

   public static File zip(String var0) throws IOException {
      LinkedList var1 = new LinkedList();
      generateFileList(var0, new File(var0), var1);
      return zip(var0, var1);
   }

   public static File zip(String var0, List<String> var1) throws IOException {
      LOGGER.debug("Zipping files in folder:" + var0);
      byte[] var2 = new byte[10240];
      File var3 = File.createTempFile("strategyquant_", ".zip");
      FileOutputStream var4 = new FileOutputStream(var3);
      ZipOutputStream var5 = new ZipOutputStream(var4);

      try {
         for (String var7 : var1) {
            var7 = var7.trim();
            LOGGER.debug("Adding file: " + var7 + "...");
            ZipEntry var8 = new ZipEntry(var7);
            var5.putNextEntry(var8);
            FileInputStream var9 = new FileInputStream(new File(var0, var7));

            int var10;
            try {
               while ((var10 = var9.read(var2)) > 0) {
                  var5.write(var2, 0, var10);
               }
            } catch (Throwable var14) {
               try {
                  var9.close();
               } catch (Throwable var13) {
                  var14.addSuppressed(var13);
               }

               throw var14;
            }

            var9.close();
         }

         var5.closeEntry();
         LOGGER.debug("Files successfully compressed");
      } catch (Throwable var15) {
         try {
            var5.close();
         } catch (Throwable var12) {
            var15.addSuppressed(var12);
         }

         throw var15;
      }

      var5.close();
      return var3;
   }

   public static void unzip(String var0, File var1) {
      byte[] var2 = new byte[10240];

      try {
         File var3 = new File(var0);
         if (!var3.exists()) {
            var3.mkdir();
         }

         ZipInputStream var4 = new ZipInputStream(new FileInputStream(var1));

         try {
            for (ZipEntry var5 = var4.getNextEntry(); var5 != null; var5 = var4.getNextEntry()) {
               String var6 = var5.getName();
               File var7 = new File(var0, var6);
               LOGGER.debug("file unzip : " + var7.getAbsoluteFile());
               new File(var7.getParent()).mkdirs();
               FileOutputStream var8 = new FileOutputStream(var7);

               int var9;
               try {
                  while ((var9 = var4.read(var2)) > 0) {
                     var8.write(var2, 0, var9);
                  }
               } catch (Throwable var13) {
                  try {
                     var8.close();
                  } catch (Throwable var12) {
                     var13.addSuppressed(var12);
                  }

                  throw var13;
               }

               var8.close();
            }

            var4.closeEntry();
         } catch (Throwable var14) {
            try {
               var4.close();
            } catch (Throwable var11) {
               var14.addSuppressed(var11);
            }

            throw var14;
         }

         var4.close();
         LOGGER.debug("UnZip done");
      } catch (IOException var15) {
         var15.printStackTrace();
      }
   }
}
