package com.strategyquant.plugin.Servlet.impl.CodeEditor;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.ProtectedSnippets;
import com.strategyquant.lib.snippets.compile.SQStructure;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.comparator.NameFileComparator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileMap {
   private static final Logger Log = LoggerFactory.getLogger(FileMap.class);
   private static final String KEY_MISSING = "missing";
   private static final String KEY_CONTENT = "content";
   private static final String KEY_INFO = "info";
   private static final String KEY_NAME = "name";
   private static final String KEY_FILE = "file";
   private static final String KEY_TYPE = "type";
   private static final String KEY_TYPES = "types";
   private static final String KEY_ID = "id";
   private static final String KEY_TEXT = "text";
   private static final String KEY_USERDATA = "userdata";
   private static final String KEY_ITEM = "item";
   private int index;
   private final Lock lock = new ReentrantLock();
   private final File fileInternalDir;
   private final List<File> missingTemplateFiles = new ArrayList<>();
   private final List<File> fileCodeBlockDirs = new ArrayList<>();
   private final List<File> fileSnippetsBlocksDirs = new ArrayList<>();
   private static final Pattern patternForEngine = Pattern.compile("^@ForEngine\\(value=\"([^\"]+)\"\\)", 8);

   public FileMap() {
      this.fileInternalDir = new File(SQStructure.getSnippetsBuiltinInternalDirPath());

      for (String var4 : SQStructure.getSnippetsSourceDirsWithSuffix("SQ/Blocks")) {
         this.fileSnippetsBlocksDirs.add(new File(var4));
      }
   }

   public String[] getCodeBlocksTypes() {
      this.lock.lock();

      try {
         HashSet var1 = new HashSet();

         for (File var3 : this.fileCodeBlockDirs) {
            File var4 = var3.getParentFile();
            if (var4 != null) {
               var1.add(var4.getName());
            }
         }

         return var1.stream().sorted(String.CASE_INSENSITIVE_ORDER).toArray(String[]::new);
      } finally {
         this.lock.unlock();
      }
   }

   public void searchForMissingTemplateFiles() {
      this.lock.lock();

      try {
         this.missingTemplateFiles.clear();
         this.fileCodeBlockDirs.clear();
         String[] var1 = SQStructure.getIndyCodeDirs();
         String[] var2 = SQStructure.getSnippetsSourceDirsWithSuffix("SQ/Blocks");

         for (int var3 = 0; var3 < var1.length; var3++) {
            ArrayList var4 = new ArrayList();
            String var5 = var1[var3];
            File var6 = new File(var5);
            File[] var7 = var6.listFiles();
            if (var7 != null) {
               for (File var11 : var7) {
                  if (var11.isDirectory() && this.dirContains(var11, "blocks") && (var3 == 0 && this.dirContains(var11, "Main.tpl") || var3 > 0)) {
                     var4.add(new File(var11, "blocks"));
                  }
               }
            }

            this.searchForMissingTemplateFilesRecursively(new File(var2[var3]), var4);
            this.fileCodeBlockDirs.addAll(var4);
         }

         this.fileCodeBlockDirs.sort(NameFileComparator.NAME_INSENSITIVE_COMPARATOR);
      } catch (Exception var15) {
         Log.error("Exc.", var15);
      } finally {
         this.lock.unlock();
      }
   }

   private void searchForMissingTemplateFilesRecursively(File var1, List<File> var2) throws Exception {
      if (var1.isDirectory()) {
         File[] var3 = var1.listFiles();
         if (var3 != null) {
            Arrays.sort(var3, NameFileComparator.NAME_INSENSITIVE_COMPARATOR);

            for (File var7 : var3) {
               this.searchForMissingTemplateFilesRecursively(var7, var2);
            }
         }
      } else {
         String var18 = SQUtils.fileToString(var1);
         if (!var18.contains("@BuildingBlock")) {
            return;
         }

         String var19 = SQUtils.stripExtension(var1.getName());
         boolean var20 = false;

         for (File var22 : var2) {
            if (!this.dirContains(var22, var19 + ".tpl")) {
               if (var18.contains("@ForEngine")) {
                  String var8 = var22.getParentFile().getName();
                  String[] var9 = new String[0];
                  Matcher var10 = patternForEngine.matcher(var18);
                  if (var10.find()) {
                     var9 = var10.group(1).split(",");
                  }

                  boolean var11 = false;

                  for (String var15 : var9) {
                     var15 = var15.trim();
                     switch (var15) {
                        case "-TS":
                        case "-MC":
                           if (var8.equals("EasyLanguage")) {
                              var11 = true;
                              return;
                           }
                           break;
                        case "-MT4":
                           if (var8.equals("MetaTrader4")) {
                              var11 = true;
                              return;
                           }
                           break;
                        case "-MT5":
                        case "-MT5H":
                        case "-MT5N":
                           if (var8.equals("MetaTrader5")) {
                              var11 = true;
                              return;
                           }
                     }

                     if (!var11) {
                        var20 = true;
                     }
                  }
               } else {
                  var20 = true;
               }
            }
         }

         if (var20) {
            this.missingTemplateFiles.add(var1);
         }
      }
   }

   private boolean dirContains(File var1, String var2) {
      return new File(var1, var2).exists();
   }

   public JSONArray generateTree(Map<String, String> var1, String var2, int var3) {
      this.lock.lock();

      try {
         this.index = var3;
         JSONArray var4 = new JSONArray();

         for (Entry var6 : var1.entrySet()) {
            this.parseFolder((String)var6.getKey(), var2, var4, (String)var6.getValue());
         }

         return var4;
      } finally {
         this.lock.unlock();
      }
   }

   private void parseFolder(String var1, String var2, JSONArray var3, String var4) {
      File var5 = new File(var1);
      if (var5.exists()) {
         this.prvParse(var5, var2.trim().toLowerCase(), var3, true, var4);
      }
   }

   private JSONArray prvParse(File var1, String var2, JSONArray var3, boolean var4, String var5) {
      File[] var6 = var1.listFiles();
      if (var6 == null) {
         return var3;
      }

      Arrays.sort(var6, NameFileComparator.NAME_INSENSITIVE_COMPARATOR);
      JSONObject var7 = null;
      JSONArray var8 = null;

      for (File var12 : var6) {
         if (!var12.getAbsolutePath().equals(this.fileInternalDir.getAbsolutePath()) && this.checkFile(var12, var2)) {
            if (var4 && var7 == null) {
               var7 = new JSONObject();
               var7.put("id", this.index++);
               var7.put("text", var5);
               var8 = new JSONArray();
               var7.put("item", var8);
               var3.put(var7);
            }

            JSONObject var13 = new JSONObject();
            var13.put("id", this.index++);
            var13.put("text", var12.getName());
            JSONArray var14 = new JSONArray();
            JSONObject var15 = new JSONObject();
            var15.put("name", "file");
            var15.put("content", var12.getAbsolutePath());
            var14.put(var15);
            JSONObject var16 = new JSONObject();
            var16.put("name", "fileType");
            var16.put("content", var12.isDirectory() ? "dir" : "file");
            var14.put(var16);
            if (this.isSnippetsBlockSourceFile(var12)) {
               JSONObject var17 = this.checkTemplates(var12);
               if (var17.getBoolean("missing")) {
                  var13.put("text", var12.getName() + "<span class=\"missing-file\"></span>");
               }

               JSONObject var18 = new JSONObject();
               var18.put("name", "templates");
               var18.put("content", var17);
               var14.put(var18);
            }

            var13.put("userdata", var14);
            if (var12.isDirectory()) {
               JSONArray var20 = this.prvParse(var12, var2, new JSONArray(), false, null);
               File[] var23 = var12.listFiles();
               if (var23 != null) {
                  if (var23.length == 0) {
                     String var19 = "folderClosed.gif";
                     var13.put("im0", var19);
                     var13.put("im1", var19);
                     var13.put("im2", var19);
                  }

                  var13.put("item", var20);
               }
            } else if (this.isProtectedFile(var12)) {
               String var21 = "ico-ce-lock.gif";
               var13.put("im0", var21);
               var13.put("im1", var21);
               var13.put("im2", var21);
               JSONObject var24 = new JSONObject();
               var24.put("name", "protected");
               var24.put("content", true);
               var14.put(var24);
            } else if (this.isJavaFile(var12)) {
               String var22 = "nav-ico-uncomp.png";
               var13.put("im0", var22);
               var13.put("im1", var22);
               var13.put("im2", var22);
            }

            if (var8 == null) {
               var3.put(var13);
            } else {
               var8.put(var13);
            }
         }
      }

      return var3;
   }

   private boolean isProtectedFile(File var1) {
      return ProtectedSnippets.getInstance().isProtected(var1);
   }

   private boolean isJavaFile(File var1) {
      return var1.getAbsolutePath().toLowerCase().endsWith(".java");
   }

   private boolean checkFile(File var1, String var2) {
      if (var2.equals("")) {
         return true;
      }

      if (var1.isDirectory()) {
         return this.containsFileByFilter(var1, var2);
      }

      String var3 = SQUtils.stripExtension(var1.getName().toLowerCase());
      return var3.contains(var2);
   }

   private boolean containsFileByFilter(File var1, String var2) {
      File[] var3 = var1.listFiles();
      if (var3 == null) {
         return false;
      }

      for (File var7 : var3) {
         if (var7.isDirectory()) {
            if (this.containsFileByFilter(var7, var2)) {
               return true;
            }
         } else {
            String var8 = SQUtils.stripExtension(var7.getName().toLowerCase());
            if (var8.contains(var2)) {
               return true;
            }
         }
      }

      return false;
   }

   private JSONObject checkTemplates(File var1) {
      JSONObject var2 = new JSONObject();
      JSONArray var3 = new JSONArray();
      String var4 = SQUtils.stripExtension(var1.getName());
      boolean var5 = this.isMissingTemplate(var1);
      if (var1.isDirectory()) {
         if (var5) {
            var3.put(new JSONObject().put("item", "Category contains building block snippets with missing templates."));
         }
      } else {
         JSONArray var6 = new JSONArray();
         ArrayList var7 = new ArrayList();
         String var8 = new File(SQStructure.getExtendUserDirPath()).getAbsolutePath();
         String var9 = new File(SQStructure.getExtendBuiltinDirPath()).getAbsolutePath();

         for (File var11 : this.fileCodeBlockDirs) {
            if (var1.getPath().startsWith(var8) && var11.getPath().startsWith(var8) || var1.getPath().startsWith(var9) && var11.getPath().startsWith(var9)) {
               boolean var12 = this.dirContains(var11, var4 + ".tpl");
               String var13 = new File(var11.getAbsolutePath(), var4 + ".tpl").getAbsolutePath();
               JSONObject var14 = new JSONObject();
               var14.put("type", var11.getParentFile().getName());
               var14.put("file", var13);
               var14.put("missing", !var12);
               if (!MainApp.isRelease() || !var1.getPath().startsWith(var9) || var12) {
                  if (!var12) {
                     String var15 = var11.getParentFile().getName();
                     var3.put(new JSONObject().put("item", "<b>" + var4 + "</b> Missing code for this block in <b>" + var15 + "</b>"));
                     var7.add(var13);
                  }

                  var6.put(var14);
               }
            }
         }

         if (!var7.isEmpty()) {
            JSONObject var16 = new JSONObject();
            var16.put("type", "addAllMissingTemplates");
            var7.sort(Comparator.comparing(String::toString));
            var16.put("file", String.join(",", var7));
            var16.put("missing", true);
            var6.put(var16);
         }

         var2.put("types", var6);
      }

      var2.put("missing", var5);
      var2.put("info", var3);
      return var2;
   }

   private boolean isMissingTemplate(File var1) {
      for (File var3 : this.missingTemplateFiles) {
         if (var3.getAbsolutePath().startsWith(var1.getAbsolutePath())) {
            return true;
         }
      }

      return false;
   }

   private boolean isSnippetsBlockSourceFile(File var1) {
      for (File var3 : this.fileSnippetsBlocksDirs) {
         if (var1.getAbsolutePath().startsWith(var3.getAbsolutePath())) {
            return true;
         }
      }

      return false;
   }
}
