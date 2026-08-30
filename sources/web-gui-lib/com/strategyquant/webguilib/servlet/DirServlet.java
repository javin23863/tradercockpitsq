package com.strategyquant.webguilib.servlet;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(DirServlet.class);
   private HashMap<Integer, String> filePaths = new HashMap<>();
   private HashMap<Integer, PathInfo> fileInfos = new HashMap<>();
   private int id = 1;
   private boolean showFiles = false;
   private String extensions = null;

   @Override
   protected String execute(String var1, Map<String, String[]> var2, String var3) {
      switch (var1) {
         case "getFileId":
            return this.getFileId(var2);
         case "getFilePath":
            return this.getFilePath(var2);
         case "getFilePaths":
            return this.getFilePaths(var2);
         case "setLastFolderUsed":
            return this.onSetLastFolderUsed(var2);
         case "getLastFolderUsed":
            return this.onGetLastFolderUsed(var2);
         case "checkFolderExists":
            return this.onCheckFolderExists(var2);
         case "settings":
            return this.onSettings(var2);
         default:
            String[] var6 = var1.split("/");
            if (var6 != null && var6.length > 0) {
               switch (var6[var6.length - 1]) {
                  case "listdir":
                     return this.onListDir(var6, var2);
                  default:
                     return apiErrorJSON(L.t("Execution failed. Unknown command '%s'.", new Object[]{var1}), null);
               }
            } else {
               return apiErrorJSON(L.t("Execution failed. Incorrect url arguments '%s'.", new Object[]{var1}), null);
            }
      }
   }

   private String onListDir(String[] var1, Map<String, String[]> var2) {
      JSONObject var3 = new JSONObject();
      JSONArray var4 = new JSONArray();
      boolean var5 = true;
      if (var2.containsKey("id")) {
         int var6 = Integer.parseInt(((String[])var2.get("id"))[0]);
         if (!this.fileInfos.containsKey(var6) || var5) {
            this.loadFileInfo(var6);
         }

         PathInfo var7 = this.fileInfos.get(var6);
         if (var7.getSubFilesIds() != null) {
            for (int var8 = 0; var8 < var7.subFilesIds.size(); var8++) {
               JSONObject var9 = new JSONObject();
               String var10 = var7.subFilesNames.get(var8);
               var9.put("id", var7.subFilesIds.get(var8));
               var9.put("text", var10);
               if (var7.getSubDirs().get(var8)) {
                  var9.put("child", "1");
                  if (var7.projectSubDirs.get(var8)) {
                     var9.put("im0", "project.gif");
                     var9.put("im1", "project.gif");
                     var9.put("im2", "project.gif");
                  } else {
                     var9.put("im0", "folderClosed.gif");
                  }

                  var4.put(var9);
               } else if (this.showFiles && fileShouldBeShown(this.extensions, var10)) {
                  var9.put("child", "0");
                  var9.put("im0", "leaf.gif");
                  var4.put(var9);
               }
            }

            var3.put("id", var6);
            var3.put("item", var4);
         } else {
            Log.debug("empty folder - " + var7);
         }
      } else {
         Log.error("Cannot list the dir contents. Id not set");
      }

      var3.put("success", L.t("Dirs listed.", new Object[0]));
      Log.debug(var3.toString());
      return var3.toString();
   }

   public static boolean fileShouldBeShown(String var0, String var1) {
      if (var0 == null) {
         return true;
      }

      String[] var2 = var0.split("\\,");

      for (int var3 = 0; var3 < var2.length; var3++) {
         if (SQUtils.getExtension(var1).equals(var2[var3])) {
            return true;
         }
      }

      return false;
   }

   private void loadFileInfo(int var1) {
      if (var1 == 0) {
         File[] var2 = File.listRoots();
         PathInfo var3 = this.getFileInfo(var2);
         this.fileInfos.put(var1, var3);
      } else {
         String var5 = this.filePaths.get(var1);
         File[] var6 = this.listDir(var5);
         PathInfo var4 = this.getFileInfo(var6);
         var4.setName(new File(var5).getName());
         this.fileInfos.put(var1, var4);
      }
   }

   private PathInfo getFileInfo(File[] var1) {
      PathInfo var2 = new PathInfo();
      if (var1 != null) {
         ArrayList var3 = new ArrayList();
         ArrayList var4 = new ArrayList();
         ArrayList var5 = new ArrayList();
         ArrayList var6 = new ArrayList();

         for (File var10 : var1) {
            this.filePaths.put(this.id, var10.getAbsolutePath());
            var3.add(this.id);
            var5.add(var10.isDirectory());
            var4.add(this.isProjectFolder(var10));
            var6.add(var10.getName().isEmpty() ? var10.getPath() : var10.getName());
            this.id++;
         }

         var2.setDirectory(true);
         var2.setSubDirs(var5);
         var2.setProjectSubDirs(var4);
         var2.setSubFilesIds(var3);
         var2.setSubFilesNames(var6);
      } else {
         var2.setDirectory(false);
      }

      return var2;
   }

   private boolean isProjectFolder(File var1) {
      if (var1.isDirectory()) {
         File[] var2 = this.listDir(var1.getAbsolutePath());
         if (var2 != null) {
            for (File var6 : var2) {
               if (var6.getName().equals("config.xml")) {
                  return true;
               }
            }
         }
      }

      return false;
   }

   private File[] listDir(String var1) {
      File var2 = new File(var1);
      return var2.listFiles();
   }

   private String getFileId(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      int var3 = -1;

      try {
         String var4 = ((String[])var1.get("filePath"))[0].toString();

         for (Integer var6 : this.filePaths.keySet()) {
            if (this.filePaths.get(var6).equals(var4)) {
               var3 = var6;
               break;
            }
         }

         var2.put("fileId", var3);
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Cannot get file id", new Object[0]), var7);
      }

      var2.put("success", var3 == -1 ? "Id not found." : "Id found.");
      return var2.toString();
   }

   private String getFilePath(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         int var3 = Integer.parseInt(((String[])var1.get("id"))[0].toString());
         var2.put("filePath", this.filePaths.get(var3));
      } catch (Exception var4) {
         return apiErrorJSON(L.t("Cannot get file path", new Object[0]), var4);
      }

      var2.put("success", L.t("Path found.", new Object[0]));
      return var2.toString();
   }

   private String getFilePaths(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String[] var3 = (String[])var1.get("ids[]");
         JSONArray var4 = new JSONArray();

         for (int var5 = 0; var5 < var3.length; var5++) {
            String var6 = this.filePaths.get(Integer.parseInt(var3[var5]));
            var4.put(var6.replace("\\", "/"));
         }

         var2.put("filePaths", var4);
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Cannot get file paths", new Object[0]), var7);
      }

      var2.put("success", L.t("Paths found.", new Object[0]));
      return var2.toString();
   }

   private String onSetLastFolderUsed(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = ((String[])var1.get("path"))[0].toString();
         File var4 = new File(var3);
         if (!var4.isDirectory() && var4.getParent() != null) {
            var3 = var4.getParent();
         }

         try {
            String var5 = ((String[])var1.get("actionType"))[0];
            MainApp.settings().set(var5, var3);
            MainApp.settings().save();
         } catch (Exception var6) {
         }

         MainApp.settings().set("lastFolderUsed", var3);
         var2.put("path", var3);
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Cannot save last folder path.", new Object[0]), var7);
      }

      var2.put("success", L.t("Last folder used saved.", new Object[0]));
      return var2.toString();
   }

   private String onGetLastFolderUsed(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = null;

      try {
         try {
            String var4 = ((String[])var1.get("actionType"))[0];
            if (MainApp.settings().containsKey(var4)) {
               var3 = MainApp.settings().get(var4);
            }
         } catch (Exception var5) {
         }

         if (var3 == null) {
            var3 = MainApp.settings().get("lastFolderUsed");
         }

         var2.put("path", var3);
      } catch (Exception var6) {
         return apiErrorJSON(L.t("Cannot get last folder path.", new Object[0]), var6);
      }

      var2.put("success", L.t("Last folder used loaded.", new Object[0]));
      return var2.toString();
   }

   private String onCheckFolderExists(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = ((String[])var1.get("path"))[0].toString();
         File var4 = new File(var3);
         if (var4.exists()) {
            var2.put("exists", true);
         } else {
            var2.put("exists", false);
         }
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot get folder path", new Object[0]), var5);
      }

      var2.put("success", L.t("Folder checked.", new Object[0]));
      return var2.toString();
   }

   private String onSettings(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = null;

      try {
         var3 = this.tryGetParam(var1, "extension")[0];
         var3 = var3.isEmpty() ? null : var3;
      } catch (Exception var6) {
      }

      this.extensions = var3;

      try {
         this.showFiles = Boolean.parseBoolean(((String[])var1.get("showFiles"))[0].toString());
      } catch (Exception var5) {
         return apiErrorJSON(L.t("DirServlet settings not saved", new Object[0]), var5);
      }

      var2.put("success", L.t("Settings saved.", new Object[0]));
      return var2.toString();
   }
}
