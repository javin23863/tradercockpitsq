package com.strategyquant.plugin.CodeEditor.impl.ImportExport;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.snippets.compile.SQStructure;
import java.io.File;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.io.comparator.NameFileComparator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExtensionsFileMap {
   private static final Logger Log = LoggerFactory.getLogger(ExtensionsFileMap.class);
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
   String userExtedDirPath = new File(SQStructure.USER_EXTEND).getAbsolutePath();
   String userLibsDirPath = new File(SQStructure.USER_LIBS).getAbsolutePath();

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
         if (this.checkFile(var12, var2)) {
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
            var13.put("userdata", var14);
            if (var12.isDirectory()) {
               JSONArray var17 = this.prvParse(var12, var2, new JSONArray(), false, null);
               File[] var18 = var12.listFiles();
               if (var18 != null) {
                  if (var18.length == 0) {
                     String var19 = "folderClosed.gif";
                     var13.put("im0", var19);
                     var13.put("im1", var19);
                     var13.put("im2", var19);
                  }

                  var13.put("item", var17);
               }
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

   private boolean checkFile(File var1, String var2) {
      if (!var1.getAbsolutePath().startsWith(this.userExtedDirPath) && !var1.getAbsolutePath().startsWith(this.userLibsDirPath)) {
         return false;
      }

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
}
