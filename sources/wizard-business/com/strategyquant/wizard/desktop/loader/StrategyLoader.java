package com.strategyquant.wizard.desktop.loader;

import com.strategyquant.lib.L;
import com.strategyquant.lib.utils.SQFileChooserFactory;
import com.strategyquant.wizard.desktop.settings.Settings;
import com.strategyquant.wizard.desktop.utils.JsonUtils;
import java.awt.Component;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

public class StrategyLoader {
   private static final String STRATEGY_IN_ZIP = "strategy_Portfolio.xml";
   private Component parent;

   public StrategyLoader(Component var1) {
      this.parent = var1;
   }

   public String loadFileWithName() {
      String var1 = Settings.get().get("strategyFolder");
      JFileChooser var2 = SQFileChooserFactory.createPersistingJFileChooser(var1 != null && !var1.isEmpty() ? var1 : null).getJFileChooser();
      var2.addChoosableFileFilter(new FileNameExtensionFilter("SQW files", "sqw"));
      var2.addChoosableFileFilter(new FileNameExtensionFilter("SQX files", "sqx"));
      var2.addChoosableFileFilter(new FileNameExtensionFilter("SQ4 files", "sq4"));
      var2.addChoosableFileFilter(new FileNameExtensionFilter("STR files", "str"));
      int var3 = var2.showOpenDialog(this.parent);
      if (var3 == 0) {
         File var4 = var2.getSelectedFile();
         Settings.get().set("strategyFolder", var4.getParent());
         return this.loadFile(var4.getAbsolutePath());
      } else {
         return null;
      }
   }

   public String loadFile(String var1) {
      File var2 = new File(var1);
      StringBuilder var3 = new StringBuilder();
      var3.append("{");
      if (!var2.exists()) {
         var3.append("\"result\":\"error\",\"errors\":{\"message\":\"File: " + var1.replace("\\", "\\\\") + " not found.\"}");
      } else {
         try {
            byte[] var4 = Files.readAllBytes(var2.toPath());
            String var5 = new String(var4);
            if (this.isZip(var5)) {
               var5 = this.getUnzippedStrategy(var4);
            }

            return var1 + "#;#" + var5;
         } catch (EOFException var6) {
            var3.append("\"result\":\"error\",\"errors\":{\"message\":\"Zip file is corrupted\"}");
         } catch (Throwable var7) {
            var7.printStackTrace();
            var3.append("\"result\":\"error\",\"errors\":{\"message\":\"" + var7.getMessage().replace("\\", "\\\\").replace("\n", "").replace("\r", "") + "\"}");
         }
      }

      var3.append("}");
      return var3.toString();
   }

   private String getUnzippedStrategy(byte[] var1) throws IOException {
      ZipInputStream var2 = new ZipInputStream(new ByteArrayInputStream(var1));

      String var7;
      label78: {
         try {
            ZipEntry var3 = null;

            while ((var3 = var2.getNextEntry()) != null) {
               if (var3.getName().equals("strategy_Portfolio.xml")) {
                  byte[] var4 = new byte[8192];
                  ByteArrayOutputStream var6 = new ByteArrayOutputStream();

                  int var5;
                  try {
                     while ((var5 = var2.read(var4)) != -1) {
                        var6.write(var4, 0, var5);
                     }
                  } finally {
                     var6.close();
                  }

                  var7 = new String(var6.toByteArray());
                  break label78;
               }
            }
         } catch (Throwable var13) {
            try {
               var2.close();
            } catch (Throwable var11) {
               var13.addSuppressed(var11);
            }

            throw var13;
         }

         var2.close();
         throw new RuntimeException(L.t("Strategy was not found in zip file", new Object[0]));
      }

      var2.close();
      return var7;
   }

   private boolean isZip(String var1) {
      return var1.startsWith("PK");
   }

   private boolean hasExtension(String var1) {
      int var2 = var1.lastIndexOf(".");
      if (var2 == -1) {
         return false;
      }

      int var3 = var1.lastIndexOf(File.separator);
      return var3 < var2;
   }

   public String saveFile(String var1, String var2, boolean var3) throws IOException {
      File var4 = var2 != null && !var2.isEmpty() ? new File(var2) : null;
      File var5 = var4;
      if (var3 || var5 == null) {
         String var6 = Settings.get().get("strategyFolder");
         JFileChooser var7 = SQFileChooserFactory.createPersistingJFileChooser(var6.isEmpty() ? null : var6).getJFileChooser();
         var7.addChoosableFileFilter(new FileNameExtensionFilter("SQW files", "sqw"));
         var7.addChoosableFileFilter(new FileNameExtensionFilter("SQX files", "sqx"));
         var7.addChoosableFileFilter(new FileNameExtensionFilter("SQ4 files", "sq4"));
         int var8 = var7.showSaveDialog(this.parent);
         if (var8 != 0) {
            return null;
         }

         var5 = var7.getSelectedFile();
         Settings.get().set("strategyFolder", var5.getParent());
         if (!this.hasExtension(var5.getAbsolutePath())) {
            var5 = new File(var5.getAbsoluteFile() + ".sqx");
         }
      }

      boolean var9 = var5.getAbsolutePath().endsWith("sqw");
      if (var9) {
         this.saveFile(var1, var5);
      } else if (var4 != null && var4.exists() && this.isZip(var4)) {
         this.saveToZip(var4, var5, var1);
      } else {
         this.saveToZip(var5, var1);
      }

      return var5.getAbsolutePath();
   }

   private void saveToZip(File var1, File var2, String var3) throws IOException {
      ByteArrayOutputStream var4 = new ByteArrayOutputStream();
      ZipFile var5 = new ZipFile(var1);

      try {
         ZipOutputStream var6 = new ZipOutputStream(var4);

         for (Enumeration var7 = var5.entries(); var7.hasMoreElements(); var6.closeEntry()) {
            ZipEntry var8 = (ZipEntry)var7.nextElement();
            if (!var8.getName().equalsIgnoreCase("strategy_Portfolio.xml")) {
               var6.putNextEntry(var8);
               InputStream var9 = var5.getInputStream(var8);
               byte[] var10 = new byte[1024];

               int var11;
               while ((var11 = var9.read(var10)) > 0) {
                  var6.write(var10, 0, var11);
               }
            } else {
               var6.putNextEntry(new ZipEntry("strategy_Portfolio.xml"));
               var6.write(var3.getBytes());
            }
         }

         var6.close();
      } catch (Throwable var13) {
         try {
            var5.close();
         } catch (Throwable var12) {
            var13.addSuppressed(var12);
         }

         throw var13;
      }

      var5.close();
      byte[] var14 = var4.toByteArray();
      Files.write(Paths.get(var2.getAbsolutePath()), var14);
   }

   private void saveToZip(File var1, String var2) throws IOException {
      ByteArrayOutputStream var3 = new ByteArrayOutputStream();
      ZipOutputStream var4 = new ZipOutputStream(var3);
      var4.putNextEntry(new ZipEntry("strategy_Portfolio.xml"));
      var4.write(var2.getBytes());
      var4.closeEntry();
      var4.close();
      byte[] var5 = var3.toByteArray();
      Files.write(Paths.get(var1.getAbsolutePath()), var5);
   }

   private boolean isZip(File var1) {
      try {
         byte[] var2 = Files.readAllBytes(var1.toPath());
         String var3 = new String(var2);
         return this.isZip(var3);
      } catch (Exception var4) {
         var4.printStackTrace();
         return false;
      }
   }

   private void saveFile(String var1, File var2) throws IOException {
      FileWriter var3 = null;
      var3 = new FileWriter(var2);
      var3.write(var1);
      var3.close();
   }

   public String loadCloudFileList(String var1, String var2) {
      return "{\"files\":[],\"result\":\"success\"}";
   }

   public String loadCloudFile(String var1, String var2) {
      String var3 = "http://wizard.strategyquant.com/backend/get?filename=" + var1 + "&s=" + var2;

      try {
         return JsonUtils.doGet(var3);
      } catch (IOException var5) {
         return JsonUtils.getErrorResonse(var5.getMessage());
      }
   }

   public String saveCloudFile(String var1, String var2, String var3) {
      String var4 = "http://wizard.strategyquant.com/backend/save?s=" + var3 + "&filename=" + var1;
      String var5 = "content=" + var2;

      try {
         return JsonUtils.doPost(var4, var5);
      } catch (IOException var7) {
         return JsonUtils.getErrorResonse(var7.getMessage());
      }
   }

   public String createCloudFolder(String var1, String var2) {
      String var3 = "http://wizard.strategyquant.com/backend/createdir?s=" + var2 + "&filename=" + var1;

      try {
         return JsonUtils.doGet(var3);
      } catch (IOException var5) {
         return JsonUtils.getErrorResonse(var5.getMessage());
      }
   }

   public void dispose() {
      this.parent = null;
   }
}
