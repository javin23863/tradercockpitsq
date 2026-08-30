package com.strategyquant.tradinglib.file;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.project.DefaultProjectConfig;
import com.strategyquant.tradinglib.project.ProjectResources;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.tradinglib.project.StrategySaver;
import com.strategyquant.tradinglib.taskImpl.ISQTask;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.jdom2.Content;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQFileManager {
   private static final Logger Log = LoggerFactory.getLogger(SQFileManager.class);
   public static final String CFX_SUFFIX = "cfx";
   public static final String SQB_SUFFIX = "sqb";
   public static final String PLA_SUFFIX = "pla";
   private static final String CONFIG_FILE = "config.xml";
   public static final String TASK_XML_FILE_ATTR = "taskXMLFile";
   private static Map<String, String> ZipFileSystemConfig = null;

   public static File saveFile(String var0, String var1) throws Exception {
      return saveFile(var0, var1, null);
   }

   public static File createV136ProjectFile(SQProject var0) throws Exception {
      return createV136ProjectFile(var0.getProjectConfigFile(), var0.getConfig());
   }

   public static File createV136ProjectFile(File var0, Element var1) throws Exception {
      File var2 = tryCreateProjectBackupFile(var0);
      File var3 = ensureDirExists(var0.getAbsolutePath());
      HashMap var4 = convertOldProjectConfig(var1);

      try {
         ZipOutputStream var5 = new ZipOutputStream(new FileOutputStream(var3), StandardCharsets.UTF_8);

         try {
            for (String var7 : var4.keySet()) {
               try {
                  Element var8 = (Element)var4.get(var7);
                  ZipEntry var9 = new ZipEntry(var7);
                  var5.putNextEntry(var9);
                  byte[] var10 = XMLUtil.elementToString(var8, true).getBytes(StandardCharsets.UTF_8);
                  var5.write(var10, 0, var10.length);
                  var5.closeEntry();
               } catch (Exception var13) {
                  throw new Exception(L.t("Couldn't save task file '%s'.", new Object[]{var7}), var13);
               }
            }

            try {
               ZipEntry var16 = new ZipEntry("config.xml");
               var5.putNextEntry(var16);
               byte[] var17 = tryCorrectVersion(XMLUtil.elementToString(var1, true)).getBytes(StandardCharsets.UTF_8);
               var5.write(var17, 0, var17.length);
               var5.closeEntry();
            } catch (Exception var12) {
               throw new Exception(L.t("Couldn't save config.xml file.", new Object[0]), var12);
            }
         } catch (Throwable var14) {
            try {
               var5.close();
            } catch (Throwable var11) {
               var14.addSuppressed(var11);
            }

            throw var14;
         }

         var5.close();
      } catch (Exception var15) {
         throw new Exception(L.t("Failed to save project file - %s", new Object[]{var15.getMessage()}), var15);
      }

      if (var2 != null) {
         Files.delete(var2.toPath());
      }

      return var3;
   }

   public static HashMap<String, Element> convertOldProjectConfig(Element var0) throws Exception {
      Element var1 = XMLUtil.getChildElem(var0, "Tasks");
      List var2 = var1.getChildren("Task");
      HashMap var3 = new HashMap();

      for (int var4 = 0; var4 < var2.size(); var4++) {
         Element var5 = (Element)var2.get(var4);

         try {
            Element var6 = XMLUtil.getChildElem(var5, "Settings");
            String var7 = XMLUtil.getStringAttr(var5, "type", "Unknown");
            String var8 = var7 + "-Task" + (var4 + 1) + ".xml";
            var5.setAttribute("taskXMLFile", var8);
            var5.removeContent();
            var3.put(var8, var6.clone());
         } catch (Exception var9) {
            throw new Exception(
               L.t("Unable to convert old task '%s' to new project format", new Object[]{var5 != null ? var5.getAttributeValue("name") : "null"}), var9
            );
         }
      }

      var0.setAttribute("version", MainApp.getAppVersion());
      return var3;
   }

   public static File createV136ProjectFile(File var0, DefaultProjectConfig var1) throws Exception {
      File var2 = tryCreateProjectBackupFile(var0);
      File var3 = ensureDirExists(var0.getAbsolutePath());

      try {
         ZipOutputStream var4 = new ZipOutputStream(new FileOutputStream(var3), StandardCharsets.UTF_8);

         try {
            if (var1.taskConfig != null) {
               String var5 = var1.taskXMLFileName;

               try {
                  ZipEntry var6 = new ZipEntry(var1.taskXMLFileName);
                  var4.putNextEntry(var6);
                  byte[] var7 = XMLUtil.elementToString(var1.taskConfig, true).getBytes(StandardCharsets.UTF_8);
                  var4.write(var7, 0, var7.length);
                  var4.closeEntry();
               } catch (Exception var10) {
                  throw new Exception(L.t("Couldn't save task file '%s'.", new Object[]{var5}), var10);
               }
            }

            try {
               ZipEntry var13 = new ZipEntry("config.xml");
               var4.putNextEntry(var13);
               byte[] var14 = tryCorrectVersion(XMLUtil.elementToString(var1.projectConfig, true)).getBytes(StandardCharsets.UTF_8);
               var4.write(var14, 0, var14.length);
               var4.closeEntry();
            } catch (Exception var9) {
               throw new Exception(L.t("Couldn't save config.xml file.", new Object[0]), var9);
            }
         } catch (Throwable var11) {
            try {
               var4.close();
            } catch (Throwable var8) {
               var11.addSuppressed(var8);
            }

            throw var11;
         }

         var4.close();
      } catch (Exception var12) {
         throw new Exception(L.t("Failed to save project file - %s", new Object[]{var12.getMessage()}), var12);
      }

      if (var2 != null) {
         Files.delete(var2.toPath());
      }

      return var3;
   }

   private static File tryCreateProjectBackupFile(File var0) throws Exception {
      if (!var0.exists()) {
         return null;
      }

      File var1 = new File(SQUtils.stripExtension(var0.getAbsolutePath()) + "_backup." + SQUtils.getExtension(var0.getAbsolutePath()));
      Files.copy(var0.toPath(), var1.toPath(), StandardCopyOption.REPLACE_EXISTING);
      return var1;
   }

   public static Element loadProjectConfig(File var0) throws Exception {
      String var1 = getZipContent(var0.getAbsolutePath(), "config.xml");
      return XMLUtil.stringToElement(var1);
   }

   public static void updateProjectConfig(SQProject var0) throws Exception {
      updateProjectConfig(var0.getProjectConfigFile(), var0.getConfig());
   }

   public static void updateProjectConfig(File var0, Element var1) throws Exception {
      updateZipContent(var0, "config.xml", XMLUtil.elementToString(var1, true));
   }

   public static HashMap<String, Element> loadTaskConfigs(File var0) throws Exception {
      HashMap var1 = new HashMap();
      if (var0 == null) {
         throw new Exception(L.t("Project file is null", new Object[0]));
      }

      if (!var0.exists()) {
         throw new Exception(L.t("Project file '%s' doesn't exist", new Object[]{var0.getAbsolutePath()}));
      }

      try {
         ZipFile var2 = new ZipFile(var0);

         HashMap var11;
         try {
            Enumeration var3 = var2.entries();

            while (var3.hasMoreElements()) {
               ZipEntry var4 = (ZipEntry)var3.nextElement();
               String var5 = var4.getName();

               try {
                  if (!var5.equals("config.xml")) {
                     String var6 = SQUtils.inputStreamToString(var2.getInputStream(var4));
                     if (var6.trim().isEmpty()) {
                        throw new Exception(L.t("Empty file.", new Object[0]));
                     }

                     var1.put(var5, XMLUtil.stringToElement(var6));
                  }
               } catch (Exception var8) {
                  throw new Exception(L.t("Couldn't load task file '%s'", new Object[]{var5}), var8);
               }
            }

            var11 = var1;
         } catch (Throwable var9) {
            try {
               var2.close();
            } catch (Throwable var7) {
               var9.addSuppressed(var7);
            }

            throw var9;
         }

         var2.close();
         return var11;
      } catch (Exception var10) {
         throw new Exception(L.t("Failed to load task configs - %s", new Object[]{var10.getMessage()}), var10);
      }
   }

   public static Element loadTaskConfig(File var0, String var1) throws Exception {
      try {
         String var2 = getZipContent(var0.getAbsolutePath(), var1);
         return XMLUtil.stringToElement(var2);
      } catch (Exception var3) {
         throw new Exception(L.t("Error while loading task xml %s", new Object[]{var1}));
      }
   }

   public static void updateTaskConfig(File var0, String var1, Element var2) throws Exception {
      updateZipContent(var0, var1, XMLUtil.elementToString(var2, true));
   }

   public static void updateTaskConfig(FileSystem var0, String var1, String var2, Element var3) throws Exception {
      updateZipContent(var0, var1, var2, XMLUtil.elementToString(var3, true));
   }

   public static void updateWholeProjectConfig(SQProject var0) throws Exception {
      File var1 = var0.getProjectConfigFile();
      File var2 = tryCreateProjectBackupFile(var1);
      saveProjectFile(var0, var0.getProjectConfigFile());
      Files.delete(var2.toPath());
   }

   public static void saveProjectFile(SQProject var0, File var1) throws Exception {
      ensureDirExists(var1.getAbsolutePath());
      ProjectResources.addResources(var0);

      try {
         ZipOutputStream var2 = new ZipOutputStream(new FileOutputStream(var1), StandardCharsets.UTF_8);

         try {
            byte[] var3 = null;
            Element var4 = var0.getConfig();
            Element var5 = var4.getChild("Tasks");
            List var6 = var5.getChildren("Task");
            ArrayList var7 = var0.getTasks();
            if (var6.size() != var7.size()) {
               throw new Exception("Project integrity failed.");
            }

            try {
               var2.putNextEntry(new ZipEntry("config.xml"));
               var3 = XMLUtil.elementToString(var4, true).getBytes(StandardCharsets.UTF_8);
               var2.write(var3, 0, var3.length);
            } catch (Exception var14) {
               throw new Exception("Couldn't save config.xml file.", var14);
            }

            for (int var8 = 0; var8 < var7.size(); var8++) {
               ISQTask var9 = (ISQTask)var7.get(var8);
               String var10 = var9.getTaskXMLFileName();

               try {
                  var2.putNextEntry(new ZipEntry(var10));
                  var3 = XMLUtil.elementToString(var9.getConfig(), true).getBytes(StandardCharsets.UTF_8);
                  var2.write(var3, 0, var3.length);
               } catch (Exception var13) {
                  throw new Exception(String.format("Couldn't save task file '%s'.", var10), var13);
               }
            }
         } catch (Throwable var15) {
            try {
               var2.close();
            } catch (Throwable var12) {
               var15.addSuppressed(var12);
            }

            throw var15;
         }

         var2.close();
      } catch (Exception var16) {
         throw new Exception(L.t("Failed to save project file - %s", new Object[]{var16.getMessage()}), var16);
      }
   }

   public static void createProjectFile(File var0, Element var1, HashMap<String, Element> var2) throws Exception {
      ensureDirExists(var0.getAbsolutePath());

      try {
         ZipOutputStream var3 = new ZipOutputStream(new FileOutputStream(var0), StandardCharsets.UTF_8);

         try {
            byte[] var4 = null;

            try {
               var3.putNextEntry(new ZipEntry("config.xml"));
               var4 = XMLUtil.elementToString(var1, true).getBytes(StandardCharsets.UTF_8);
               var3.write(var4, 0, var4.length);
            } catch (Exception var11) {
               throw new Exception(L.t("Couldn't save config.xml file.", new Object[0]), var11);
            }

            for (String var6 : var2.keySet()) {
               Element var7 = (Element)var2.get(var6);

               try {
                  var3.putNextEntry(new ZipEntry(var6));
                  var4 = XMLUtil.elementToString(var7, true).getBytes(StandardCharsets.UTF_8);
                  var3.write(var4, 0, var4.length);
               } catch (Exception var10) {
                  throw new Exception(L.t("Couldn't save task file %s.", new Object[]{var6}), var10);
               }
            }
         } catch (Throwable var12) {
            try {
               var3.close();
            } catch (Throwable var9) {
               var12.addSuppressed(var9);
            }

            throw var12;
         }

         var3.close();
      } catch (Exception var13) {
         throw new Exception(L.t("Failed to save project file - %s", new Object[]{var13.getMessage()}), var13);
      }
   }

   private static synchronized void updateZipContent(File var0, String var1, String var2) throws Exception {
      if (ZipFileSystemConfig == null) {
         ZipFileSystemConfig = new HashMap<>();
         ZipFileSystemConfig.put("create", "true");
         ZipFileSystemConfig.put("compressionMethod", "DEFLATED");
      }

      String var3 = ("jar:file:/" + var0.getAbsolutePath())
         .replace("\\", "/")
         .replace("%", "%25")
         .replace("//", "/")
         .replace(" ", "%20")
         .replace("[", "%5B")
         .replace("]", "%5D");
      URI var4 = URI.create(var3);

      try {
         FileSystem var5 = FileSystems.newFileSystem(var4, ZipFileSystemConfig);

         try {
            Path var6 = var5.getPath("/" + var1);
            Files.writeString(var6, var2);
         } catch (Throwable var9) {
            if (var5 != null) {
               try {
                  var5.close();
               } catch (Throwable var8) {
                  var9.addSuppressed(var8);
               }
            }

            throw var9;
         }

         if (var5 != null) {
            var5.close();
         }
      } catch (Exception var10) {
         throw new Exception(L.t("Failed to update zip content - '%s'", new Object[]{var3}), var10);
      }
   }

   public static synchronized void updateZipContent(FileSystem var0, String var1, String var2, String var3) throws Exception {
      try {
         Path var4 = var0.getPath("/" + var2);
         Files.writeString(var4, var3);
      } catch (Exception var5) {
         throw new Exception(L.t("Failed to update zip content - '%s'", new Object[]{var1}), var5);
      }
   }

   public static FileSystem createFileSystem(File var0) throws IOException {
      if (ZipFileSystemConfig == null) {
         ZipFileSystemConfig = new HashMap<>();
         ZipFileSystemConfig.put("create", "true");
         ZipFileSystemConfig.put("compressionMethod", "DEFLATED");
      }

      String var1 = ("jar:file:/" + var0.getAbsolutePath())
         .replace("\\", "/")
         .replace("%", "%25")
         .replace("//", "/")
         .replace(" ", "%20")
         .replace("[", "%5B")
         .replace("]", "%5D");
      URI var2 = URI.create(var1);
      return FileSystems.newFileSystem(var2, ZipFileSystemConfig);
   }

   public static File saveFile(String var0, String var1, Element var2) throws Exception {
      if (var0.endsWith("cfx")) {
         return createZipFile(var0, var1);
      }

      if (var0.endsWith("sqb")) {
         if (var2 == null) {
            var2 = XMLUtil.stringToElement(var1);
         }

         Element var4 = XMLUtil.findFirst(var2, "Blocks");
         if (var4 == null) {
            throw new Exception(L.t("Blocks element not found in XML config", new Object[0]));
         } else {
            return createZipFile(var0, var1);
         }
      } else {
         File var3 = SQUtils.createFile(var0);
         if (var0.endsWith("pla")) {
            var1 = StrategySaver.getPLAContent(SQUtils.stripExtension(var3.getName()), var1);
         }

         SQUtils.stringToFile(var3, var1);
         return var3;
      }
   }

   public static String loadFile(String var0) throws Exception {
      if (var0.endsWith("cfx")) {
         return getZipContent(var0, "config.xml");
      }

      if (var0.endsWith("sqb")) {
         Element var1 = XMLUtil.stringToElement(getZipContent(var0, null));
         Element var2 = XMLUtil.findFirst(var1, "Blocks");
         if (var2 == null) {
            throw new Exception(L.t("Blocks element not found in XML config", new Object[0]));
         } else {
            return XMLUtil.elementToString(var2);
         }
      } else {
         return SQUtils.fileToString(var0);
      }
   }

   public static void xmlToCfx(File var0, boolean var1) throws Exception {
      String var2 = SQUtils.fileToString(var0);
      String var3 = var0.getAbsolutePath().substring(0, var0.getAbsolutePath().length() - 3) + "cfx";
      saveFile(var3, var2);
      if (var1) {
         var0.delete();
      }
   }

   private static String getZipContent(String var0, String var1) throws Exception {
      ZipInputStream var2 = null;

      try {
         var2 = new ZipInputStream(new FileInputStream(var0), StandardCharsets.UTF_8);
         if (var1 != null) {
            while (var2.available() > 0) {
               ZipEntry var3 = var2.getNextEntry();
               if (var3.getName().equals(var1)) {
                  break;
               }
            }
         } else {
            var2.getNextEntry();
         }

         String var8 = SQUtils.inputStreamToString(var2);
         return checkXml(var8);
      } finally {
         if (var2 != null) {
            var2.close();
         }
      }
   }

   private static File createZipFile(String var0, String var1) throws Exception {
      File var2 = ensureDirExists(var0);
      var1 = tryCorrectVersion(var1);
      ZipOutputStream var3 = null;

      try {
         var3 = new ZipOutputStream(new FileOutputStream(var2), StandardCharsets.UTF_8);
         ZipEntry var4 = new ZipEntry("config.xml");
         var3.putNextEntry(var4);
         byte[] var5 = var1.getBytes(StandardCharsets.UTF_8);
         var3.write(var5, 0, var5.length);
         var3.closeEntry();
         return var2;
      } finally {
         if (var3 != null) {
            var3.close();
         }
      }
   }

   private static File ensureDirExists(String var0) {
      File var1 = new File(var0);
      if (!var1.exists() && var1.getParentFile() != null) {
         var1.getParentFile().mkdirs();
      }

      return var1;
   }

   public static String tryCorrectVersion(String var0) {
      int var1 = var0.indexOf(">");
      if (var1 < 0) {
         return var0;
      }

      String var2 = " version=\"" + MainApp.getAppVersion() + "\"";
      String var3 = var0.substring(0, var1);
      int var4 = var3.indexOf(" version");
      if (var4 < 0) {
         var3 = var3 + var2;
      } else {
         var3 = var3.replaceAll("\\s+(?:version)\\s*=\\s*\"[^\"]*\"", var2);
      }

      return var3 + var0.substring(var1);
   }

   private static String checkXml(String var0) {
      try {
         Element var1 = XMLUtil.stringToXmlElement(var0);
         if (checkXmlElement(var1)) {
            var0 = XMLUtil.elementToString(var1);
         }
      } catch (Exception var2) {
         Log.error("Exc.", var2);
      }

      return var0;
   }

   private static boolean checkXmlElement(Element var0) {
      boolean var1 = false;
      if (var0.getAttributeValue("version") == null) {
         if (var0.getName().equals("Project")) {
            List var2 = var0.getChild("Tasks").getChildren("Task");

            for (int var3 = 0; var3 < var2.size(); var3++) {
               if (checkTask((Element)var2.get(var3))) {
                  var1 = true;
               }
            }
         } else if (var0.getName().equals("Task")) {
            var1 = checkTask(var0);
         }
      }

      return var1;
   }

   private static boolean checkTask(Element var0) {
      boolean var1 = false;
      Element var2 = var0.getChild("Settings");

      try {
         if (checkConditions(var2.getChild("Rankings").getChild("Conditions"))) {
            var1 = true;
         }
      } catch (Exception var6) {
      }

      try {
         List var3 = var2.getChild("CrossChecks").getChildren();

         for (int var4 = 0; var4 < var3.size(); var4++) {
            Element var5 = ((Element)var3.get(var4)).getChild("AcceptanceSettings").getChild("Conditions");
            if (checkConditions(var5)) {
               var1 = true;
            }
         }
      } catch (Exception var7) {
      }

      return var1;
   }

   private static boolean checkConditions(Element var0) {
      boolean var1 = false;
      List var2 = var0.getChildren("Condition");

      for (int var3 = 0; var3 < var2.size(); var3++) {
         try {
            if (checkConditionColumnValue(((Element)var2.get(var3)).getChild("Left-Side").getChild("Column-Value"))) {
               var1 = true;
            }
         } catch (Exception var6) {
         }

         try {
            if (checkConditionColumnValue(((Element)var2.get(var3)).getChild("Right-Side").getChild("Column-Value"))) {
               var1 = true;
            }
         } catch (Exception var5) {
         }
      }

      return var1;
   }

   private static boolean checkConditionColumnValue(Element var0) {
      boolean var1 = false;
      if (var0.getAttributeValue("resultType").equals("RetestWithHigherPrecision")
         || var0.getAttributeValue("resultType").equals("RetestOnAdditionalMarkets")
         || var0.getAttributeValue("resultType").equals("WhatIf")) {
         var0.setAttribute("direction", "0");
         var0.setAttribute("sampleType", "127");
         var0.setAttribute("plType", "10");
         var1 = true;
      }

      return var1;
   }

   public static void updateTaskConfigurations(SQProject var0, HashMap<String, Element> var1) throws Exception {
      String var2 = var0.getProjectConfigFile().getAbsolutePath();

      try {
         FileSystem var3 = createFileSystem(new File(var2));

         try {
            for (String var5 : var1.keySet()) {
               Element var6 = (Element)var1.get(var5);
               ISQTask var7 = var0.getTaskByXMLFile(var5);
               Element var8 = var7.getConfig();
               var8.removeContent();

               for (Content var10 : var6.getContent()) {
                  var8.addContent(var10.clone());
               }

               updateTaskConfig(var3, var2, var5, var6);
            }
         } finally {
            var3.close();
         }
      } catch (Exception var15) {
         throw new Exception("Unable to update task configurations in the project file '" + var2 + "' - " + var15.getMessage(), var15);
      }
   }
}
