package com.strategyquant.plugin.Servlet.impl.CodeEditor;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.classInfo.ClassHelper;
import com.strategyquant.lib.classInfo.ClazzInfo;
import com.strategyquant.lib.classInfo.CodeInfo;
import com.strategyquant.lib.utils.JsonCreator;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeAutoCompleteManager {
   private static final Logger Log = LoggerFactory.getLogger(CodeAutoCompleteManager.class);

   public String getData() throws IOException, ClassNotFoundException {
      String var1 = this.getSqAutoCompleteData();
      String var2 = this.getCustomJarsCompleteData();
      String var3 = this.getJavaDocs();
      JsonCreator var4 = new JsonCreator();
      var4.beginObject();
      var4.put("success", "Code types listed.", true);
      var4.putBeginObject("data");
      var4.putRaw("autocompleteData", var1, true);
      var4.putRaw("customJarsAutoCompleteData", var2, true);
      var4.putRaw("javaDocs", var3, false);
      var4.endObject(false);
      var4.endObject(false);
      String var5 = var4.toJson();
      Log.debug(var3);
      return var5;
   }

   private File[] getCustomJarFiles() {
      File var1 = new File(MainApp.getDataPath(), "user" + File.separator + "libs");
      return var1.listFiles((var0, var1x) -> {
         File var2 = new File(var0, var1x);
         return var2.isFile() && var1x.endsWith(".jar");
      });
   }

   private File getCustomJarCacheFile(File var1) {
      return new File(MainApp.getDataPath() + "/internal/autocomplete", "cache_custom_" + var1.getName().replace(".jar", ".json"));
   }

   private String getCachedCustomJarData(File var1) {
      String var2 = null;
      if (var1.exists()) {
         try {
            byte[] var3 = Files.readAllBytes(var1.toPath());
            var2 = new String(var3, StandardCharsets.UTF_8);
         } catch (Exception var4) {
            Log.error("Error while loading cache.json file", var4);
         }
      }

      return var2;
   }

   private CodeInfo performLoadCustomJarInfo(File var1) throws IOException {
      CodeInfo var2 = new CodeInfo();
      var2.setImports(new HashSet());
      var2.setClasses(new LinkedList());
      JarFile var3 = new JarFile(var1);
      Enumeration var4 = var3.entries();
      URL[] var5 = new URL[]{new URL("jar:file:" + var1.getAbsolutePath() + "!/")};
      URLClassLoader var6 = URLClassLoader.newInstance(var5);

      while (var4.hasMoreElements()) {
         JarEntry var7 = (JarEntry)var4.nextElement();
         if (!var7.isDirectory() && var7.getName().endsWith(".class")) {
            Log.debug(String.format("Analysing class: %s", var7.getName()));
            String var8 = var7.getName().substring(0, var7.getName().length() - 6);
            var8 = var8.replace('/', '.');

            try {
               Class var9 = var6.loadClass(var8);
               ClazzInfo var10 = ClassHelper.getClazzInfo(var9);
               if (var10 != null) {
                  var2.getClasses().add(var10);
                  var2.getImports().add(var9.getPackage().getName());
               }
            } catch (Throwable var11) {
               Log.error("Error while analyzing class", var11);
            }
         }
      }

      return var2;
   }

   private String getCustomJarsCompleteData() throws IOException {
      File[] var1 = this.getCustomJarFiles();
      JsonCreator var2 = new JsonCreator();
      var2.beginObject();
      if (var1 != null) {
         for (int var3 = 0; var3 < var1.length; var3++) {
            File var4 = var1[var3];
            Log.debug(String.format("Analysing jar: %s", var4.getName()));
            File var5 = this.getCustomJarCacheFile(var4);
            String var6 = this.getCachedCustomJarData(var5);
            if (var6 == null) {
               var6 = this.performLoadCustomJarInfo(var4).toJSON();
               Files.write(var5.toPath(), var6.getBytes());
            }

            var2.putRaw(var4.getName(), var6, var3 < var1.length - 1);
         }
      }

      var2.endObject(false);
      return var2.toJson();
   }

   private String getSqAutoCompleteData() throws ClassNotFoundException, IOException {
      File var1 = new File(MainApp.getDataPath() + "/internal/autocomplete", "cache_v2.json");
      String var2 = null;
      if (var1.exists()) {
         try {
            byte[] var3 = Files.readAllBytes(var1.toPath());
            var2 = new String(var3, StandardCharsets.UTF_8);
         } catch (Exception var5) {
            Log.error("Error while loading cache.json file", var5);
         }
      }

      if (var2 == null) {
         File var6 = new File(MainApp.getDataPath() + "/internal/autocomplete", "packages.txt");
         List var4 = Files.readAllLines(var6.toPath());
         var4.removeIf(var0 -> var0.trim().isEmpty());
         var2 = ClassHelper.getClassesJson(var4.toArray(new String[0]));
         Files.write(var1.toPath(), var2.getBytes());
      }

      return var2;
   }

   private String getJavaDocs() {
      File var1 = new File(MainApp.getDataPath() + "/internal/autocomplete", "docs.json");
      String var2 = null;
      if (var1.exists()) {
         try {
            byte[] var3 = Files.readAllBytes(var1.toPath());
            var2 = new String(var3, StandardCharsets.UTF_8).replaceAll("\\s+", " ").replaceAll("\\{@code\\s(.*?)\\}", "**$1**").replace("\\n ", "\\n");
         } catch (Exception var4) {
            Log.error("Error while loading docs.json file", var4);
         }
      }

      if (var2 == null) {
         var2 = "{}";
      }

      return var2;
   }
}
