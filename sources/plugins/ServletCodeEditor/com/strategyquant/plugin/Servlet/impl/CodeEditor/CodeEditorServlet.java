package com.strategyquant.plugin.Servlet.impl.CodeEditor;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.plugins.compile.PluginCompiler;
import com.strategyquant.lib.snippets.CustomClassesReg;
import com.strategyquant.lib.snippets.ProtectedSnippets;
import com.strategyquant.lib.snippets.SnippetsCompiler;
import com.strategyquant.lib.snippets.SnippetsCompilerUtils;
import com.strategyquant.lib.snippets.compile.AutoImport;
import com.strategyquant.lib.snippets.compile.CompilationResult;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.lib.snippets.compile.inMemory.InMemoryJavaCompiler;
import com.strategyquant.lib.snippets.compile.indicators.IndicatorsBuilder;
import com.strategyquant.plugin.Servlet.impl.CodeEditor.searchInFiles.SearchInFiles;
import com.strategyquant.plugin.Servlet.impl.CodeEditor.searchInFiles.SearchInFilesResult;
import com.strategyquant.plugin.Servlet.impl.CodeEditor.templates.Template;
import com.strategyquant.plugin.Servlet.impl.CodeEditor.templates.Templates;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CodeEditorServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(CodeEditorServlet.class);
   private static CodeEditorInfoSender infoSender = null;
   private static FileMap fileMap = null;
   private static Templates templates = null;
   private PluginCompiler pc = new PluginCompiler();
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
   private static final String KEY_PLUGIN = "plugin";
   private static final String LAST_OPENED_SNIPPETS_FILE_PATH = SQPaths.settingsDirPath + "/lastOpenedSnippets.txt";

   private static synchronized CodeEditorInfoSender getInfoSenderInstance() {
      if (infoSender == null) {
         infoSender = new CodeEditorInfoSender();
      }

      return infoSender;
   }

   private static synchronized FileMap getFileMapInstance() {
      if (fileMap == null) {
         fileMap = new FileMap();
      }

      return fileMap;
   }

   private static synchronized Templates getTemplatesInstance() {
      if (templates == null) {
         templates = new Templates();
      }

      return templates;
   }

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "editorAutocomplete":
            return this.onEditorAutocomplete();
         case "listCodeTypes":
            return this.onListCodeTypes();
         case "list":
            return this.onList(var2);
         case "getContent":
            return this.onGetContent(var2);
         case "save":
            return this.onSave(var2);
         case "saveAs":
            return this.onSaveAs(var2);
         case "compile":
            return this.onCompile(var2);
         case "compileAll":
            return this.onCompileAll();
         case "compilePlugin":
            return this.onCompilePlugin(var2);
         case "stopCompilation":
            return this.onStopCompilation();
         case "listTemplates":
            return this.onListTemplates(var2);
         case "listIndicators":
            return this.onListIndicators();
         case "createNew":
            return this.onCreateNew(var2);
         case "createNewFile":
            return this.onCreateNewFile(var2);
         case "createNewTemplate":
            return this.onCreateNewTemplate(var2);
         case "createNewDir":
            return this.onCreateNewDir(var2);
         case "rename":
            return this.onRename(var2);
         case "delete":
            return this.onDelete(var2);
         case "reload":
            return this.onReload(var2);
         case "clone":
            return this.onClone(var2);
         case "findInFiles":
            return this.onFindInFiles(var2);
         case "getInfo":
            return this.onGetInfo();
         case "fixImports":
            return this.onFixImports(var2);
         case "saveLastOpenedFiles":
            return this.onSaveLastOpenedFiles(var2);
         case "loadLastOpenedFiles":
            return this.onLoadLastOpenedFiles();
         case "reloadApp":
            return this.onReloadApp();
         default:
            throw new CodeEditorServletException(String.format("Unknown command '%s'.", var1));
      }
   }

   private void validateFileName(String var1) throws CodeEditorServletException {
      if (!this.isNameValid(var1)) {
         throw new CodeEditorServletException(L.t("File name cannot contain spaces or special characters.", new Object[0]));
      }
   }

   private void prvCheckParamExists(Map<String, String[]> var1, String[] var2) throws CodeEditorServletException {
      try {
         this.checkParamExists(var1, var2);
      } catch (Exception var4) {
         throw new CodeEditorServletException(var4.getMessage());
      }
   }

   private static void prvStringToFile(String var0, String var1) throws CodeEditorServletException {
      try {
         SQUtils.stringToFile(new File(var0), var1);
      } catch (Exception var3) {
         throw new CodeEditorServletException(var3.getMessage());
      }
   }

   private static void prvStringToFile(File var0, String var1) throws CodeEditorServletException {
      try {
         SQUtils.stringToFile(var0, var1);
      } catch (Exception var3) {
         throw new CodeEditorServletException(var3.getMessage());
      }
   }

   private static String prvFileToString(File var0) throws CodeEditorServletException {
      try {
         return SQUtils.fileToString(var0);
      } catch (Exception var2) {
         throw new CodeEditorServletException(var2.getMessage());
      }
   }

   private String onFindInFiles(Map<String, String[]> var1) {
      String var2 = ((String[])var1.get("text"))[0];
      boolean var3 = Boolean.parseBoolean(((String[])var1.get("caseSensitive"))[0]);
      boolean var4 = Boolean.parseBoolean(((String[])var1.get("regex"))[0]);
      boolean var5 = Boolean.parseBoolean(((String[])var1.get("wholeWord"))[0]);
      JSONObject var6 = new JSONObject();
      SearchInFiles var7 = new SearchInFiles();
      SearchInFilesResult var8 = var7.search(SQStructure.getSnippetsSourceDirsAsMap(), var2, var3, var4, var5);
      var6.put("id", 0);
      var6.put("item", var8.getItems());
      var6.put("text", var2);
      var6.put("matches", var8.getTotalMatchesCount());
      var6.put("success", "Search done.");
      return var6.toString();
   }

   private String onReload(Map<String, String[]> var1) throws CodeEditorServletException {
      File var2 = this.getFile(var1);
      JSONObject var3 = new JSONObject();
      var3.put("file", var2.getAbsolutePath());
      var3.put("success", L.t("File refreshed.", new Object[0]));
      return var3.toString();
   }

   private String onDelete(Map<String, String[]> var1) throws Exception {
      String var2 = ((String[])var1.get("file"))[0];
      JSONObject var3 = new JSONObject();
      File var4 = new File(var2);
      if (ProtectedSnippets.getInstance().isProtected(var4)) {
         throw new CodeEditorServletException(L.t("Cannot delete a protected snippet.", new Object[0]));
      }

      if (!var4.exists()) {
         throw new CodeEditorServletException(L.t("File '%s'doesn't exist.", new Object[]{var2}));
      }

      ArrayList var5 = new ArrayList();
      this.deleteRecursive(var4, var5);
      JSONArray var6 = new JSONArray();

      for (String var8 : var5) {
         var6.put(var8);
      }

      var3.put("files", var6);
      var3.put("success", L.t("Files deleted.", new Object[0]));
      return var3.toString();
   }

   public boolean deleteRecursive(File var1, List<String> var2) throws Exception {
      if (!var1.exists()) {
         return true;
      }

      boolean var3 = true;
      if (var1.isDirectory()) {
         File[] var4 = var1.listFiles();
         if (var4 == null) {
            return true;
         }

         for (File var8 : var4) {
            var3 = var3 && this.deleteRecursive(var8, var2);
         }
      }

      String var10 = var1.getAbsolutePath();

      try {
         if (var3) {
            Files.delete(Paths.get(var10));
            var2.add(var1.getAbsolutePath());
            return true;
         } else {
            return false;
         }
      } catch (IOException var9) {
         throw new Exception(String.format("Error deleting file '%s'. Exc. %s", var10, var9.toString()));
      }
   }

   private String onRename(Map<String, String[]> var1) throws CodeEditorServletException {
      String var2 = ((String[])var1.get("name"))[0];
      String var3 = ((String[])var1.get("file"))[0];
      JSONObject var4 = new JSONObject();
      File var5 = new File(var3);
      String var6;
      if (var5.isDirectory()) {
         var6 = var5.getParentFile().getAbsolutePath() + '/' + var2;
      } else {
         String var7 = SQUtils.getExtension(var5.getName());
         var6 = var5.getParentFile().getAbsolutePath() + '/' + var2 + "." + var7;
      }

      File var8 = new File(var6);
      if (ProtectedSnippets.getInstance().isProtected(var5)) {
         throw new CodeEditorServletException(L.t("Cannot change a protected snippet.", new Object[0]));
      }

      if (!var5.renameTo(var8)) {
         throw new CodeEditorServletException(L.t("Cannot rename file.\nPlease check  if this file is open in another program.", new Object[0]));
      }

      var4.put("file", var8.getAbsolutePath());
      var4.put("success", L.t("File renamed.", new Object[0]));
      return var4.toString();
   }

   private String onClone(Map<String, String[]> var1) throws Exception {
      String var2 = ((String[])var1.get("name"))[0];
      String var3 = ((String[])var1.get("file"))[0];
      var2 = var2.substring(0, 1).toUpperCase() + var2.substring(1);
      this.validateFileName(var2);
      JSONObject var4 = new JSONObject();
      File var5 = new File(var3);
      if (var5.isDirectory()) {
         throw new CodeEditorServletException(L.t("Directory cannot be cloned.", new Object[0]));
      }

      String var6 = SQUtils.stripExtension(var5.getName());
      String var7 = SQUtils.getExtension(var5.getName());
      String var8 = var5.getParentFile().getAbsolutePath() + '/' + var2 + "." + var7;
      File var9 = new File(var8);
      String var10 = new File(SQStructure.getExtendBuiltinDirPath()).getAbsolutePath();
      String var11 = new File(SQStructure.getExtendUserDirPath()).getAbsolutePath();
      if (var9.getPath().startsWith(var10)) {
         var8 = var9.getPath().replace(var10, var11);
         var9 = new File(var8);
      }

      if (var9.exists()) {
         throw new Exception("File with this name already exists.");
      }

      String var12 = SQUtils.fileToString(var5);
      if (var7.equalsIgnoreCase("java")) {
         var12 = var12.replaceAll("(?<!double|int|long|String|char|boolean|float|byte|short) " + var6 + " ", " " + var2 + " ");
         var12 = var12.replaceAll(var6 + ".class", var2 + ".class");
         var12 = var12.replaceAll(" " + var6 + "\\(", " " + var2 + "(");
         var12 = var12.replaceAll("\"" + var6 + "\"", "\"" + var2 + "\"");
      }

      prvStringToFile(var9, var12);
      var4.put("file", var9.getAbsolutePath());
      var4.put("success", L.t("File cloned.", new Object[0]));
      return var4.toString();
   }

   private String onCreateNewDir(Map<String, String[]> var1) throws CodeEditorServletException {
      String var2 = ((String[])var1.get("name"))[0];
      String var3 = ((String[])var1.get("file"))[0];
      JSONObject var4 = new JSONObject();
      File var6 = new File(var3);
      String var5;
      if (var6.isDirectory()) {
         var5 = var6.getAbsolutePath() + '/' + var2;
      } else {
         var5 = var6.getParentFile().getAbsolutePath() + '/' + var2;
      }

      File var7 = new File(var5);
      if (!var7.mkdirs()) {
         throw new CodeEditorServletException(L.t("The directory could not be created.", new Object[0]));
      }

      var4.put("file", var7.getAbsolutePath());
      var4.put("success", "Directory created.");
      return var4.toString();
   }

   private String onCreateNew(Map<String, String[]> var1) throws CodeEditorServletException {
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("name"))[0];
      String var4 = ((String[])var1.get("type"))[0];
      String var5 = var1.containsKey("indicator") ? ((String[])var1.get("indicator"))[0] : null;
      String var6 = var1.containsKey("orderActionType") ? ((String[])var1.get("orderActionType"))[0] : null;
      String var7 = var1.containsKey("mcType") ? ((String[])var1.get("mcType"))[0] : null;

      try {
         var3 = var3.trim();
         if (var3.isEmpty()) {
            throw new CodeEditorServletException(L.t("File name cannot be blank.", new Object[0]));
         }

         var3 = var3.substring(0, 1).toUpperCase() + var3.substring(1);
         this.validateFileName(var3);
         if (var4.equals("MonteCarlo")) {
            if (var7.equals("Manipulation")) {
               var4 = "MonteCarloManipulation";
            } else if (var7.equals("Retest")) {
               var4 = "MonteCarloRetest";
            }
         }

         Template var9 = getTemplatesInstance().findTemplateByFileName(var4);
         String var10 = SQUtils.trimFilePath(var9.getTemplateFileDirectoryPath(), Templates.TEMPLATES_PATH);
         String var11 = SQStructure.getSnippetsUserDirPath() + var10;
         switch (var4) {
            case "BlocksIndicators":
               File var24 = new File(var11 + '/' + var3);
               if (!var24.exists() && !var24.mkdirs()) {
                  throw new CodeEditorServletException(L.t("Unable to create directory '%s'.", new Object[]{var24.getAbsolutePath()}));
               }

               var11 = var24.getAbsolutePath();
               break;
            case "BlocksSignal":
               if (var5 == null) {
                  throw new CodeEditorServletException("Indicator parameter is null");
               }

               boolean var23 = false;

               for (String var18 : SQStructure.getSnippetsSourceDirs()) {
                  File var19 = new File(new File(var18, var10), var5);
                  if (var19.exists()) {
                     var23 = true;
                     break;
                  }
               }

               if (!var23) {
                  throw new CodeEditorServletException(L.t("Cannnot create signal based on indicator '%s' because it doesn't exist.", new Object[]{var5}));
               }

               File var25 = new File(var11, var5);
               var11 = var25.getAbsolutePath();
               break;
            case "BlocksOrder":
               if (var6 == null) {
                  throw new CodeEditorServletException("OrderActionType parameter is null");
               }

               var5 = null;
               File var14 = new File(var11, var6);
               var11 = var14.getAbsolutePath();
         }

         String var8 = var9.createNewFile(var3, var11, var5);
         var2.put("file", var8);
         var2.put("success", "Snippet created.");
         return var2.toString();
      } catch (Exception var20) {
         Log.error("Error creating new snippet. Exc.", var20);
         throw new CodeEditorServletException(L.t("Cannot create snippet.", new Object[]{true}) + " " + var20.getMessage());
      }
   }

   private String onCreateNewFile(Map<String, String[]> var1) throws Exception {
      String var2 = ((String[])var1.get("name"))[0];
      String var3 = ((String[])var1.get("file"))[0];
      String var4 = ((String[])var1.get("template"))[0];
      String var5 = ((String[])var1.get("ext"))[0];
      boolean var6 = Boolean.parseBoolean(((String[])var1.get("createCodeFile"))[0]);
      var2 = var2.trim();
      if (var2.isEmpty()) {
         throw new CodeEditorServletException(L.t("File name cannot be blank.", new Object[0]));
      }

      var2 = var2.substring(0, 1).toUpperCase() + var2.substring(1);
      this.validateFileName(var2);
      JSONObject var7 = new JSONObject();
      File var9 = new File(var3);
      String var8;
      if (var9.isDirectory()) {
         String var10 = var9.getAbsolutePath().replace("\\", "/");
         if (var10.endsWith("SQ/Blocks/Indicators")) {
            var9 = new File(var9.getAbsolutePath() + '/' + var2);
            if (!var9.exists() && !var9.mkdirs()) {
               throw new CodeEditorServletException(L.t("Unable to create directory '%s'.", new Object[]{var9.getAbsolutePath()}));
            }

            var3 = var9.getAbsolutePath();
         }

         var8 = var9.getAbsolutePath() + '/' + var2 + "." + var5;
      } else {
         var8 = var9.getParentFile().getAbsolutePath() + '/' + var2 + "." + var5;
      }

      if (var6) {
         if (var4.equals("CodeBlocksFile")) {
            var8 = this.createCodeBlocksTemplate(var8);
         } else {
            File var14 = new File(var8);
            if (!var14.createNewFile()) {
               throw new CodeEditorServletException(L.t("The file could not be created.", new Object[0]));
            }

            var8 = var14.getAbsolutePath();
         }
      } else if (var4.equals("EmptyFile")) {
         var8 = this.createEmptyFileTemplate(var8);
      } else {
         Template var15 = getTemplatesInstance().findTemplateByPath(var4);
         var8 = var15.createNewFile(var2, var3, null);
      }

      var7.put("file", var8);
      var7.put("success", L.t("File created.", new Object[0]));
      return var7.toString();
   }

   private String createEmptyFileTemplate(String var1) throws Exception {
      File var2 = new File(var1);
      String var3 = SQUtils.fileToString(Templates.EMPTY_FILE_PATH);
      String var4 = SQUtils.trimFilePath(var2.getParentFile().getAbsolutePath(), SQStructure.SNIPPETS_DIR_PATH_WITHOUT_PACKAGE);
      var4 = var4.replace("/", ".").replace(".java", "");
      var3 = var3.replace("[package]", var4);
      String var5 = SQUtils.stripExtension(var2.getName());
      var3 = var3.replace("[class_name]", var5);
      prvStringToFile(var2, var3);
      return var2.getAbsolutePath();
   }

   private String createCodeBlocksTemplate(String var1) throws Exception {
      File var2 = new File(var1);
      String var3 = SQUtils.stripExtension(var2.getName());
      String var4;
      if (var1.contains(new File(SQStructure.getIndyCodeBuiltinDirPath() + "PseudoCode").getAbsolutePath())) {
         var4 = SQUtils.fileToString(Templates.PSEUDO_CODE_BLOCKS_TPL_PATH);
      } else {
         var4 = SQUtils.fileToString(Templates.CODE_BLOCKS_TPL_PATH);
      }

      var4 = var4.replace("SNIPPET_NAME", var3);
      prvStringToFile(var2, var4);
      return var2.getAbsolutePath();
   }

   private String onCreateNewTemplate(Map<String, String[]> var1) throws Exception {
      String var2 = ((String[])var1.get("file"))[0];

      try {
         JSONObject var3 = new JSONObject();
         StringBuilder var4 = new StringBuilder();
         String var5;
         if (var2.contains(",")) {
            String[] var6 = var2.split(",");

            for (String var10 : var6) {
               var4.append(this.createCodeBlocksTemplate(var10.trim())).append(",");
            }

            var4 = new StringBuilder(var4.substring(0, var4.length() - 1));
            var5 = L.t("Templates created.", new Object[0]);
         } else {
            var4 = new StringBuilder(this.createCodeBlocksTemplate(var2));
            var5 = L.t("Template created.", new Object[0]);
         }

         var3.put("file", var4.toString());
         var3.put("success", var5);
         return var3.toString();
      } catch (Exception var11) {
         Log.error("The template could not be created.", var11);
         throw new CodeEditorServletException("The template could not be created.");
      }
   }

   private boolean isNameValid(String var1) {
      Pattern var2 = Pattern.compile("[A-Z][a-zA-Z0-9]*");
      Matcher var3 = var2.matcher(var1);
      return var3.matches();
   }

   private String onListIndicators() {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();
      this.listIndicators(SQStructure.getSnippetsBuiltinDirPath(), var2);
      this.listIndicators(SQStructure.getSnippetsUserDirPath(), var2);
      var1.put("indicators", var2);
      var1.put("success", "Indicators listed.");
      return var1.toString();
   }

   private void listIndicators(String var1, JSONArray var2) {
      File var3 = new File(var1 + "/SQ/Blocks/Indicators");
      File[] var4 = var3.listFiles();
      if (var4 != null) {
         for (File var8 : var4) {
            if (var8.isDirectory()) {
               var2.put(var8.getName());
            }
         }
      }
   }

   private String onListTemplates(Map<String, String[]> var1) {
      String var2 = ((String[])var1.get("file"))[0];
      File var3 = new File(var2);
      if (!var3.isDirectory()) {
         var2 = var3.getParentFile().getAbsolutePath();
      }

      var2 = SQUtils.trimFilePath(var2, SQStructure.SNIPPETS_DIR_PATH_WITHOUT_PACKAGE);
      JSONObject var4 = new JSONObject();
      JSONArray var5 = new JSONArray();

      for (Template var7 : getTemplatesInstance().getAvailableTemplates()) {
         JSONObject var8 = new JSONObject();
         String var9 = SQUtils.trimFilePath(var7.getTemplateFileDirectoryPath(), Templates.TEMPLATES_PATH);
         if (var2.contains(var9)) {
            var8.put("file", var7.getTemplateFileAbsolutePath());
            var8.put("name", var7.getName());
            var8.put("description", var7.getDescription());
            var5.put(var8);
         }
      }

      var4.put("templates", var5);
      var4.put("success", "Templates listed.");
      return var4.toString();
   }

   private String onEditorAutocomplete() throws ClassNotFoundException, IOException {
      return new CodeAutoCompleteManager().getData();
   }

   private String onListCodeTypes() throws CodeEditorServletException {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();
      getFileMapInstance().searchForMissingTemplateFiles();

      for (String var6 : getFileMapInstance().getCodeBlocksTypes()) {
         var2.put(var6);
      }

      var1.put("items", var2);
      var1.put("success", "Code types listed.");
      return var1.toString();
   }

   private String onList(Map<String, String[]> var1) throws Exception {
      String var2 = ((String[])var1.get("filter"))[0];
      String[] var3 = this.getParam(var1, "dirs", "Builtin,User").split(",");
      JSONObject var4 = new JSONObject();
      getFileMapInstance().searchForMissingTemplateFiles();
      Map var5 = SQStructure.getExtendsSourceDirsAsMap(var3);
      int var6 = 0;
      var4.put("id", var6++);
      JSONArray var7 = getFileMapInstance().generateTree(var5, var2, var6);
      var4.put("item", var7);
      var4.put("success", "Sources listed.");
      return var4.toString();
   }

   private String onGetContent(Map<String, String[]> var1) throws Exception {
      File var2 = this.getFile(var1);
      String var3 = SQUtils.fileToString(var2);
      JSONObject var4 = new JSONObject();
      var4.put("name", var2.getName());
      var4.put("text", var3);
      var4.put("protected", ProtectedSnippets.getInstance().isProtected(var2));
      var4.put("success", "ok");
      return var4.toString();
   }

   private File getFile(Map<String, String[]> var1) throws CodeEditorServletException {
      this.prvCheckParamExists(var1, new String[]{"file"});
      String var2 = ((String[])var1.get("file"))[0];
      File var3 = new File(var2);
      if (!var3.exists()) {
         throw new CodeEditorServletException(String.format("File '%s' doesn't exist.", var2));
      } else {
         return var3;
      }
   }

   private String onSave(Map<String, String[]> var1) throws CodeEditorServletException {
      File var2 = this.getFile(var1);
      String var3 = ((String[])var1.get("data"))[0];
      JSONObject var4 = new JSONObject();
      prvStringToFile(var2, var3);
      var4.put("success", L.t("File saved.", new Object[0]));
      return var4.toString();
   }

   private String onSaveAs(Map<String, String[]> var1) throws CodeEditorServletException {
      this.prvCheckParamExists(var1, new String[]{"file"});
      String var2 = ((String[])var1.get("file"))[0];
      String var3 = ((String[])var1.get("data"))[0];
      JSONObject var4 = new JSONObject();
      prvStringToFile(var2, var3);
      var4.put("success", L.t("File saved.", new Object[0]));
      return var4.toString();
   }

   private String onCompile(Map<String, String[]> var1) throws CodeEditorServletException {
      File var2 = this.getFile(var1);
      if (ProtectedSnippets.getInstance().isProtected(var2)) {
         throw new CodeEditorServletException(L.t("Protected snippet doesn't need to be compiled.", new Object[0]));
      }

      String var3 = ((String[])var1.get("data"))[0];
      JSONObject var4 = new JSONObject();
      prvStringToFile(var2, var3);
      new Thread(
            () -> {
               ArrayList var2x = new ArrayList();
               var2x.add(var2);
               if (var3.contains("IndicatorBlock")) {
                  var2x.add(new File(SQStructure.SNIPPETS_DIR_PATH + "Internal/Indicators.java"));

                  try {
                     Log.info("Generating Indicators.java");
                     IndicatorsBuilder var3x = new IndicatorsBuilder();
                     var3x.run(var2);
                  } catch (Exception var6) {
                     Log.error("Generating Indicators.java failed. Exc.", var6);
                  }
               }

               CompilationResult var7;
               if (var2.getAbsolutePath().toLowerCase().endsWith(".tpl")) {
                  var7 = new CompilationResult();
                  var7.success = true;
                  var7.addCompilationMessage(
                     0,
                     "<html><font color=\"green\"><b>"
                        + L.t(
                           "Nothing to compile, template file changes are automatically applied after save. Please refresh the Source code to see the difference.",
                           new Object[0]
                        )
                        + "</b></font>"
                  );
               } else {
                  SnippetsCompiler var4x = SnippetsCompiler.getInstance();
                  var7 = var4x.run(var2x, true);
               }

               JSONObject var8 = new JSONObject();
               JSONObject var5 = var7.toJSON();
               var5.put("file", var2.getAbsolutePath());
               var8.put("compilationResult", var5);
               getInfoSenderInstance().sendData(var8);
            }
         )
         .start();
      var4.put("success", "Started.");
      return var4.toString();
   }

   private String onCompileAll() {
      JSONObject var1 = new JSONObject();
      new Thread(() -> {
         try {
            Log.info("Generating Indicators.java");
            IndicatorsBuilder var0 = new IndicatorsBuilder();
            var0.run();
         } catch (Exception var6) {
            Log.error("Generating Indicators.java failed. Exc.", var6);
         }

         SnippetsCompiler var7 = SnippetsCompiler.getInstance();
         CompilationResult var1x = var7.run(true);
         if (var1x.success) {
            File var2 = new File(MainApp.getDataPath() + "/internal/autocomplete", "cache.json");
            String var3 = var2.getAbsolutePath();

            try {
               Files.delete(Paths.get(var3));
            } catch (IOException var5) {
               Log.warn(String.format("Error deleting autocomplete file '%s'. Exc. %s", var3, var5.toString()));
            }
         }

         JSONObject var8 = new JSONObject();
         var8.put("compilationResult", var1x.toJSON());
         getInfoSenderInstance().sendData(var8);
      }).start();
      var1.put("success", "Started.");
      return var1.toString();
   }

   private String onStopCompilation() {
      JSONObject var1 = new JSONObject();
      var1.put("success", "Stopped.");
      return var1.toString();
   }

   private String onCompilePlugin(Map<String, String[]> var1) throws CodeEditorServletException {
      File var2 = this.getFile(var1);
      if (!var2.isDirectory()) {
         throw new CodeEditorServletException(L.t("File is not a folder.", new Object[0]));
      }

      JSONObject var3 = new JSONObject();
      new Thread(() -> {
         CompilationResult var2x = this.pc.compile(var2.getAbsolutePath());
         JSONObject var3x = new JSONObject();
         JSONObject var4 = var2x.toJSON();
         var4.put("plugin", true);
         var4.put("file", var2.getAbsolutePath());
         var3x.put("compilationResult", var4);
         getInfoSenderInstance().sendData(var3x);
      }).start();
      var3.put("success", "Started.");
      return var3.toString();
   }

   private String onFixImports(Map<String, String[]> var1) throws Exception {
      File var2 = this.getFile(var1);
      if (ProtectedSnippets.getInstance().isProtected(var2)) {
         throw new CodeEditorServletException(L.t("Protected snippet doesn't need to be fixed.", new Object[0]));
      }

      String var3 = ((String[])var1.get("data"))[0];
      JSONObject var4 = new JSONObject();
      prvStringToFile(var2, var3);
      if (this.fixImports(var2)) {
         var4.put("text", SQUtils.fileToString(var2));
         var4.put("success", L.t("New imports added.", new Object[0]));
      } else {
         var4.put("success", L.t("No new imports added.", new Object[0]));
      }

      return var4.toString();
   }

   private boolean fixImports(File var1) {
      ArrayList var2 = new ArrayList();
      var2.add(var1);
      List var3 = SnippetsCompilerUtils.loadDependencies();
      InMemoryJavaCompiler var4 = new InMemoryJavaCompiler();
      CompilationResult var5 = var4.compile(var2, var3);
      if (!var5.success) {
         AutoImport var6 = new AutoImport();
         return var6.fixImports(var5, var1);
      } else {
         return false;
      }
   }

   private String onGetInfo() {
      JSONObject var1 = new JSONObject();
      new Thread(() -> {
         try {
            CompilationResult var0 = SnippetsCompiler.getInstance().getLastCompilationResult();
            if (var0 == null || var0.success) {
               return;
            }

            DataToSend var1x = new DataToSend("codeEditor", new JSONObject().put("compilationResult", var0.toJSON()));
            SQWebSocketManager.addToDataQueue(var1x, new String[]{"SQEDITOR"});
         } catch (Exception var2) {
            Log.error("Error while checking snippets. Exc.", var2);
         }
      }).start();
      var1.put("success", "Notified");
      return var1.toString();
   }

   private String onSaveLastOpenedFiles(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();
      String var3 = ((String[])var1.get("files"))[0];

      try {
         BufferedWriter var4 = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(LAST_OPENED_SNIPPETS_FILE_PATH, false), StandardCharsets.UTF_8));

         try {
            String[] var5 = var3.split(",", -1);
            ArrayList var7 = new ArrayList();

            for (String var11 : var5) {
               String var6 = var11.trim();
               if (!var6.isEmpty() && !var7.contains(var6)) {
                  var4.write(var6 + "\n");
                  var7.add(var6);
               }
            }
         } catch (Throwable var13) {
            try {
               var4.close();
            } catch (Throwable var12) {
               var13.addSuppressed(var12);
            }

            throw var13;
         }

         var4.close();
      } catch (Exception var14) {
         Log.error("Exc.", var14);
      }

      var2.put("success", "Saved");
      return var2.toString();
   }

   private String onReloadApp() {
      JSONObject var1 = new JSONObject();
      Log.info("Reloading registered custom classes...");
      CustomClassesReg.reloadAll();
      var1.put("success", L.t("App reloaded.", new Object[0]));
      return var1.toString();
   }

   private String onLoadLastOpenedFiles() {
      JSONObject var1 = new JSONObject();
      JSONArray var2 = new JSONArray();
      ArrayList var3 = new ArrayList();
      File var4 = new File(LAST_OPENED_SNIPPETS_FILE_PATH);
      if (var4.exists()) {
         try {
            FileInputStream var5 = new FileInputStream(var4);

            try {
               InputStreamReader var6 = new InputStreamReader(var5, StandardCharsets.UTF_8);

               try {
                  BufferedReader var7 = new BufferedReader(var6);

                  String var8;
                  try {
                     while ((var8 = var7.readLine()) != null) {
                        try {
                           var4 = new File(var8);
                           if (var4.exists() && !var3.contains(var4.getAbsolutePath())) {
                              String var9 = prvFileToString(var4);
                              var2.put(
                                 new JSONObject()
                                    .put("name", var4.getName())
                                    .put("file", var4.getAbsolutePath())
                                    .put("protected", ProtectedSnippets.getInstance().isProtected(var4))
                                    .put("text", var9)
                              );
                              var3.add(var4.getAbsolutePath());
                           }
                        } catch (CodeEditorServletException var13) {
                        }
                     }
                  } catch (Throwable var14) {
                     try {
                        var7.close();
                     } catch (Throwable var12) {
                        var14.addSuppressed(var12);
                     }

                     throw var14;
                  }

                  var7.close();
               } catch (Throwable var15) {
                  try {
                     var6.close();
                  } catch (Throwable var11) {
                     var15.addSuppressed(var11);
                  }

                  throw var15;
               }

               var6.close();
            } catch (Throwable var16) {
               try {
                  var5.close();
               } catch (Throwable var10) {
                  var16.addSuppressed(var10);
               }

               throw var16;
            }

            var5.close();
         } catch (IOException var17) {
            Log.warn("Error reading last opened snippets file. Exc.", var17);
         }
      }

      var1.put("files", var2);
      var1.put("success", "Loaded");
      return var1.toString();
   }
}
