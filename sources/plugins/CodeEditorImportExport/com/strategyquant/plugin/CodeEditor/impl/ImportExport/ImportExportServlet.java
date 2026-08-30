package com.strategyquant.plugin.CodeEditor.impl.ImportExport;

import com.strategyquant.lib.L;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ImportExportServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(ImportExportServlet.class);
   private static ExtensionsFileMap fileMap = null;
   private static final String KEY_SUCCESS = "success";
   private static final String KEY_FILE = "file";
   private static final String KEY_FILES = "files";
   private static final String KEY_COMPILATION_RESULT = "compilationResult";
   private static final String KEY_INDICATORS = "indicators";
   private static final String KEY_TEMPLATE = "template";
   private static final String KEY_TEMPLATES = "templates";
   private static final String KEY_ID = "id";
   private static final String KEY_TEXT = "text";
   private static final String KEY_MATCHES = "matches";
   private static final String KEY_ITEM = "item";
   private static final String KEY_ITEMS = "items";
   private static final String KEY_NAME = "name";
   private static final String KEY_PROTECTED = "protected";
   private static final String KEY_TYPE = "type";
   private static final String KEY_DESCRIPTION = "description";
   private static final String KEY_DATA = "data";

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "import":
            return this.onImport(var2);
         case "export":
            return this.onExport(var2);
         case "overwrite":
            return this.onOverwrite(var2);
         case "overwriteAlways":
            return this.onOverwriteAlways(var2);
         case "skip":
            return this.onSkip(var2);
         case "skipAlways":
            return this.onSkipAlways(var2);
         case "cancel":
            return this.onCancel(var2);
         case "list":
            return this.onList(var2);
         default:
            return apiErrorJSON(L.t("Execution failed. Unknown command '%s'.", new Object[]{var1}), null);
      }
   }

   private String onList(Map<String, String[]> var1) throws Exception {
      String var2 = ((String[])var1.get("filter"))[0];
      JSONObject var3 = new JSONObject();
      LinkedHashMap var4 = new LinkedHashMap();
      var4.put(MainApp.getDataPath() + "user", "user");
      int var5 = 0;
      var3.put("id", var5++);
      JSONArray var6 = getFileMapInstance().generateTree(var4, var2, var5);
      var3.put("item", var6);
      var3.put("success", "Sources listed.");
      return var3.toString();
   }

   private static synchronized ExtensionsFileMap getFileMapInstance() {
      if (fileMap == null) {
         fileMap = new ExtensionsFileMap();
      }

      return fileMap;
   }

   private String onImport(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "files")[0];
         String[] var4 = var3.split(",");
         ArrayList var5 = ExtensionManager.install(var4);
         JSONArray var6 = new JSONArray();

         for (int var7 = 0; var7 < var5.size(); var7++) {
            String var8 = (String)var5.get(var7);
            var6.put(var8);
         }

         JSONArray var11 = new JSONArray();

         for (Entry var9 : ExtensionManager.notImportedMap.entrySet()) {
            var11.put(new JSONObject().put("name", var9.getKey()).put("reason", var9.getValue()));
         }

         var2.put("newFiles", var6);
         var2.put("notImportedFiles", var11);
      } catch (Exception var10) {
         return apiErrorJSON(L.t("Import failed.", new Object[0]), var10);
      }

      var2.put("success", L.t("Extensions imported.", new Object[0]));
      return var2.toString();
   }

   private String onExport(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "files")[0];
         String var4 = this.tryGetParam(var1, "targetPath")[0];
         if (var3.trim().isEmpty()) {
            throw new Exception(L.t("There are no files to export.", new Object[0]));
         }

         String[] var5 = var3.split(",");
         ExtensionManager.export(var5, var4);
      } catch (Exception var6) {
         return apiErrorJSON(L.t("Export failed.", new Object[0]), var6);
      }

      var2.put("success", L.t("Extensions exported.", new Object[0]));
      return var2.toString();
   }

   private String onOverwrite(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      ExtensionManager.overwrite();
      var2.put("success", L.t("Import overwritten.", new Object[0]));
      return var2.toString();
   }

   private String onOverwriteAlways(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      ExtensionManager.overwriteAlways();
      var2.put("success", L.t("Import will always overwrite.", new Object[0]));
      return var2.toString();
   }

   private String onSkip(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      ExtensionManager.skip();
      var2.put("success", L.t("Import skipped.", new Object[0]));
      return var2.toString();
   }

   private String onSkipAlways(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      ExtensionManager.skipAlways();
      var2.put("success", L.t("Import will always skip.", new Object[0]));
      return var2.toString();
   }

   private String onCancel(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      ExtensionManager.cancelImport();
      var2.put("success", L.t("Import canceled.", new Object[0]));
      return var2.toString();
   }
}
