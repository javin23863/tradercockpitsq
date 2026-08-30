package com.strategyquant.webguilib.init;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.lib.utils.DirectoryHash;
import com.strategyquant.pluginlib.DirManager;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.plugindef.app.IAppPlugin;
import com.strategyquant.webguilib.CSSFilesComparator;
import com.strategyquant.webguilib.JSFilesComparator;
import com.strategyquant.webguilib.LibFilesComparator;
import com.strategyquant.webguilib.WebServer;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutoCompiler {
   private static final Logger Log = LoggerFactory.getLogger(AutoCompiler.class);
   private static final String[] EXCLUDED_PATHS = new String[]{"AppSQXHome/offline"};

   public static void compileBatchFiles(WebServer var0) throws Exception {
      DirManager.usePathsRelativeTo(MainApp.getDataPath());
      HashMap var1 = new HashMap();
      String var2 = var0.getWebPath() + "/libs";
      String var3 = var0.getWebPath() + "/common";
      compileBatch1(var0, var1, var2, var3);
      compileBatch2(var1, var2, var3);
      compileBatch3(var1, var2, var3);
   }

   public static void compileLayoutFiles(WebServer var0, String var1) throws Exception {
      String var2 = var0.getWebPath() + "/app/layout";
      String var3 = var0.getWebPath() + "/" + var1 + "/layout";
      String var4 = var0.getWebPath() + "/" + var1 + "/build";
      String var5 = var4 + "/layout.js";
      HashMap var6 = new HashMap();
      loadAllFiles(var3, var6);
      loadAllFiles(var2, var6);
      loadAllFiles(var0.getWebPath() + "/app/login", var6);

      for (String var8 : var6.keySet()) {
         ArrayList var9 = (ArrayList)var6.get(var8);
         if (var8.equals("js")) {
            Log.debug("Creating file: " + var5);
            WebInitializer.createMergedFile(var5, true);
            var9.sort(new JSFilesComparator());

            for (String var11 : var9) {
               WebInitializer.appendFile(var11);
            }

            WebInitializer.finishFile();
         }
      }
   }

   public static boolean checkFilesChanged(WebServer var0) {
      String var1 = SQStructure.INTERNAL_DIR_PATH + "web/";
      int var2 = DirectoryHash.calculate(new File(var0.getPluginsPath()));
      int var3 = DirectoryHash.calculate(new File(SQPaths.userDirPath + "/extend/Plugins"));
      int var4 = DirectoryHash.calculate(new File(var1 + "app"));
      int var5 = var2 + var3 + var4;
      ArrayList var6 = new ArrayList();

      for (IAppPlugin var8 : SQPluginManager.getPlugins(IAppPlugin.class)) {
         var6.add(var1 + var8.getName());
      }

      Collections.sort(var6);

      for (String var15 : var6) {
         File var9 = new File(var15);
         if (var9.exists() && var9.isDirectory()) {
            var5 += DirectoryHash.calculate(var9);
         }
      }

      String var14 = var0.getWebPath() + "/" + MainApp.getProduct() + "/filesMD5.txt";
      File var16 = new File(var14);
      if (var16.exists()) {
         int var17;
         try {
            var17 = Integer.parseInt(SQUtils.fileToString(var16));
         } catch (Exception var12) {
            Log.error("Cannot read MD5 file!", var12);
            var17 = 0;
         }

         if (var17 == var5) {
            return false;
         }
      }

      try {
         SQUtils.stringToFile(var16, Integer.toString(var5));
      } catch (Exception var11) {
         Log.error("Cannot write MD5 file!", var11);
      }

      return true;
   }

   private static void compileBatch1(WebServer var0, HashMap<String, ArrayList<String>> var1, String var2, String var3) throws Exception {
      String var4 = var3 + "/Batch1/libs_temp.js";
      String var5 = var3 + "/Batch1/styles.css";
      compileBatchLibs(var1, var2 + "/Batch1", var4, var5);
      compileSQFiles(var0, var1, var2, var3, var4, var5);
   }

   private static void compileBatch2(HashMap<String, ArrayList<String>> var0, String var1, String var2) throws Exception {
      String var3 = var2 + "/Batch2/libs.js";
      String var4 = var2 + "/Batch2/styles.css";
      compileBatchLibs(var0, var1 + "/Batch2", var3, var4);
   }

   private static void compileBatch3(HashMap<String, ArrayList<String>> var0, String var1, String var2) throws Exception {
      String var3 = var2 + "/Batch3/libs.js";
      String var4 = var2 + "/Batch3/styles.css";
      compileBatchLibs(var0, var1 + "/Batch3", var3, var4);
   }

   private static void compileSQFiles(WebServer var0, HashMap<String, ArrayList<String>> var1, String var2, String var3, String var4, String var5) throws Exception {
      String var6 = var0.getWebPath() + "/app/directives";
      String var7 = var0.getPluginsPath();
      String var8 = SQPaths.userDirPath + "/extend/Plugins";
      String var9 = var0.getWebPath() + "/app/sq-tools";
      String var10 = var3 + "/Batch1";
      String var11 = var10 + "/libs.js";
      String var12 = var10 + "/plugins.js";
      String var13 = var10 + "/pluginStyles.css";
      String var14 = var3 + "/templates.html";
      String var15 = var0.getWebPath() + "/app/app_tpl.js";
      String var16 = var0.getWebPath() + "/common/sqapp.js";
      LanguageInitializer.clearLanguages();
      var1.clear();
      loadAllFiles(var9, var1);
      loadAllFiles(var6, var1);
      loadAllFiles(var7, var1);
      loadAllFiles(var8, var1);
      loadAllFiles(var0.getWebPath() + "/app/layout/views", var1);
      loadAllFiles(var0.getWebPath() + "/app/login/views", var1);
      Iterator var17 = var1.keySet().iterator();

      while (true) {
         label72:
         while (true) {
            if (!var17.hasNext()) {
               WebInitializer.createAppJSFromTemplate(var15, var16);
               File var23 = new File(var4);
               File var24 = new File(var16);
               File var25 = new File(var12);
               WebInitializer.createMergedFile(var11, true);
               WebInitializer.fileToFile(var23);
               WebInitializer.fileToFile(var24);
               WebInitializer.fileToFile(var25);
               WebInitializer.finishFile();
               Files.delete(var24.toPath());
               Files.delete(var25.toPath());
               Files.delete(var23.toPath());
               File var30 = new File(var13);
               WebInitializer.createMergedFile(var5, false);
               WebInitializer.fileToFile(var30);
               WebInitializer.finishFile();
               Files.delete(var30.toPath());
               return;
            }

            String var18 = (String)var17.next();
            ArrayList var19 = (ArrayList)var1.get(var18);
            if (var18.equals("js")) {
               WebInitializer.createMergedJS(var12, true);
               var19.sort(new JSFilesComparator());
               Iterator var29 = var19.iterator();

               while (true) {
                  if (!var29.hasNext()) {
                     break label72;
                  }

                  String var34 = (String)var29.next();
                  WebInitializer.appendJS(var34);
               }
            }

            if (var18.equals("css")) {
               WebInitializer.createMergedFile(var13, true);
               Iterator var28 = var19.iterator();

               while (true) {
                  if (!var28.hasNext()) {
                     break label72;
                  }

                  String var33 = (String)var28.next();
                  if (!var33.endsWith("skin.css")) {
                     WebInitializer.appendFile(var33);
                  }
               }
            }

            if (var18.equals("lng")) {
               Iterator var27 = var19.iterator();

               while (true) {
                  if (!var27.hasNext()) {
                     break label72;
                  }

                  String var32 = (String)var27.next();
                  LanguageInitializer.append(var32);
               }
            }

            if (var18.equals("html")) {
               WebInitializer.createMergedFile(var14, true);

               for (String var21 : var19) {
                  WebInitializer.appendHTMLTemplateFile(var21);
               }

               Iterator var26 = SQPluginManager.getAllAvailablePlugins(IAppPlugin.class).iterator();

               while (true) {
                  if (!var26.hasNext()) {
                     break label72;
                  }

                  IAppPlugin var31 = (IAppPlugin)var26.next();
                  if (!var31.getAppCode().equals("SQWIZARD")) {
                     String var22 = var0.getWebPath() + "/" + var31.getAppCode() + "/layout/views/layout.html";
                     if (new File(var22).exists()) {
                        WebInitializer.appendHTMLTemplateFile(var22);
                     }
                  }
               }
            }
         }

         WebInitializer.finishFile();
      }
   }

   private static void compileBatchLibs(HashMap<String, ArrayList<String>> var0, String var1, String var2, String var3) throws Exception {
      var0.clear();
      loadAllFiles(var1, var0);
      Iterator var4 = var0.keySet().iterator();

      while (true) {
         ArrayList var6;
         while (true) {
            if (!var4.hasNext()) {
               return;
            }

            String var5 = (String)var4.next();
            var6 = (ArrayList)var0.get(var5);
            if (var5.equals("js")) {
               WebInitializer.createMergedFile(var2, true);
               var6.sort(new LibFilesComparator());
               break;
            }

            if (var5.equals("css")) {
               WebInitializer.createMergedFile(var3, true);
               var6.sort(new CSSFilesComparator());
               break;
            }
         }

         for (String var8 : var6) {
            WebInitializer.appendFile(var8);
         }

         WebInitializer.finishFile();
      }
   }

   private static void loadAllFiles(String var0, HashMap<String, ArrayList<String>> var1) throws Exception {
      ArrayList var2 = DirManager.listAllSubdirectoryPaths(var0);
      var2.add(var0);

      for (String var4 : var2) {
         ArrayList var5 = DirManager.listFileNamesInFolder(var4);
         Collections.sort(var5);

         for (String var7 : var5) {
            if (!isExcludedPath(normalizePath(var7))) {
               String var8 = SQUtils.getExtension(var7);
               if (!var1.containsKey(var8)) {
                  var1.put(var8, new ArrayList());
               }

               ((ArrayList)var1.get(var8)).add(var7);
            }
         }
      }
   }

   private static boolean isExcludedPath(String var0) {
      for (String var4 : EXCLUDED_PATHS) {
         String var5 = normalizePath(var4);
         if (var0.equals(var5) || var0.endsWith("/" + var5) || var0.contains("/" + var5 + "/")) {
            return true;
         }
      }

      return false;
   }

   private static String normalizePath(String var0) {
      return var0.replace('\\', '/');
   }
}
