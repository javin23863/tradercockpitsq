package com.strategyquant.tradinglib.project;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.file.SQFileManager;
import com.strategyquant.tradinglib.plugindef.app.IAppPlugin;
import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import org.jdom2.Element;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectEngine {
   private static final Logger Log = LoggerFactory.getLogger(ProjectEngine.class);
   private static ProjectEngine instance = null;
   private HashMap<String, SQProject> projects = new HashMap<>();
   private static ReentrantLock lock = new ReentrantLock();
   public static boolean loading = true;

   public static List<SQProject> list() {
      return new ArrayList<>(getInstance().projects.values());
   }

   public static List<SQProject> listCustomProjects() {
      ArrayList var0 = new ArrayList();
      ArrayList var1 = new ArrayList<>(getInstance().projects.values());

      for (int var2 = 0; var2 < var1.size(); var2++) {
         SQProject var3 = (SQProject)var1.get(var2);
         if (!var3.isAppProject()) {
            var0.add(var3);
         }
      }

      return var0;
   }

   public static synchronized ProjectEngine getInstance() {
      if (instance == null) {
         instance = new ProjectEngine();
         initTasks();
         loading = false;
      }

      return instance;
   }

   private ProjectEngine() {
      this.loadAvailableProjects();
   }

   private void loadAvailableProjects() {
      File var1 = new File(SQPaths.projectsDirPath);
      if (!var1.exists()) {
         var1.mkdirs();
      } else {
         for (File var5 : var1.listFiles()) {
            if (var5.isDirectory()) {
               this.loadProject(var5.getName());
            }
         }
      }

      this.checkDefaultProjects();
   }

   private static void initTasks() {
      for (String var1 : instance.projects.keySet()) {
         try {
            instance.projects.get(var1).initTasksFromConfig();
         } catch (Exception var3) {
            Log.error("Initialization of project '" + var1 + "' failed", var3);
         }
      }
   }

   private void checkDefaultProjects() {
      for (IAppPlugin var2 : SQPluginManager.getPlugins(IAppPlugin.class)) {
         String var3 = var2.getProject();
         if (var3 != null && !this.projects.containsKey(var3)) {
            this.loadProject(var3);
         }
      }
   }

   private void loadProject(String var1) {
      Log.debug("Loading project '" + var1 + "'.");

      try {
         SQProject var2 = new SQProject(var1);
         this.projects.put(var1, var2);
      } catch (Exception var3) {
         Log.error("Cannot load project " + var1 + ". Exc.", var3);
      }
   }

   public static SQProject get(String var0) throws Exception {
      SQProject var1 = getInstance().projects.get(var0);
      if (var1 == null) {
         throw new Exception("Project '" + var0 + "' does not exist.");
      } else {
         return var1;
      }
   }

   public static SQProject add(String var0) throws Exception {
      var0 = generateProjectName(var0);
      SQProject var1 = new SQProject(var0);
      getInstance().projects.put(var0, var1);
      var1.initTasksFromConfig();
      return var1;
   }

   public static SQProject add(String var0, File var1, Element var2, HashMap<String, Element> var3) throws Exception {
      var0 = generateProjectName(var0);
      var2.setAttribute("name", var0);
      File var4 = new File(SQPaths.projectsDirPath + "/" + var0 + "/" + "project.cfx");
      SQUtils.ensureDirExists(var4.getParent());
      if (var3 != null) {
         SQFileManager.createProjectFile(var4, var2, var3);
      } else {
         Files.copy(var1.toPath(), var4.toPath());
         SQFileManager.updateProjectConfig(var4, var2);
      }

      SQProject var5 = new SQProject(var0);
      getInstance().projects.put(var0, var5);
      var5.initTasksFromConfig();
      return var5;
   }

   public static void remove(String var0) throws Exception {
      SQProject var1 = get(var0);
      if (var1.isAppProject()) {
         throw new Exception(String.format("Predefined project %s cannot be removed.", var0));
      }

      var1.getDatabanks().clear(false, String.format("Remove project %s", var0));
      getInstance().projects.remove(var0);
      SQUtils.deleteRecursive(new File(var1.getProjectFolder()));
   }

   public static void rename(String var0, String var1) throws Exception {
      if (!var0.equals(var1)) {
         if (projectExists(var1)) {
            throw new Exception(L.t("Project with the same name already exists.", new Object[0]));
         }

         SQProject var2 = get(var0);
         var2.setProjectName(var1);
         getInstance().projects.remove(var0);
         getInstance().projects.put(var1, var2);
      }
   }

   public static boolean projectExists(String var0) {
      return getInstance().projects.containsKey(var0);
   }

   public static String generateProjectName(String var0) {
      String var1 = var0;
      if (!projectExists(var1)) {
         return var1;
      }

      for (int var2 = 2; var2 < 1000; var2++) {
         var1 = var0 + "(" + var2 + ")";
         if (!projectExists(var1)) {
            break;
         }
      }

      return var1;
   }

   public static boolean isDefaulProject(String var0) {
      return var0 != null && (var0.equals("Builder") || var0.equals("Optimizer") || var0.equals("Retester") || var0.equals("PortfolioMaster"));
   }

   public static void loadStrategies() {
      Log.info("Loading strategies from files ...");

      for (SQProject var1 : list()) {
         try {
            var1.getDatabanks().loadStrategies();
         } catch (Exception var3) {
            Log.error("Project '" + var1.getName() + "', loading strategies failed - " + var3.getMessage(), var3);
         }
      }
   }

   public static void stopAll() {
      for (SQProject var1 : list()) {
         try {
            Log.debug("Stop running project '" + var1.getName() + "'.");
            var1.stop();
         } catch (Exception var3) {
            Log.error("Project '" + var1.getName() + "' - The project couldn't be stopped.");
         }
      }
   }

   public static boolean isInitialized() {
      return instance != null;
   }

   public static void projectFinished(String var0) {
      try {
         JSONObject var1 = new JSONObject();
         var1.put("productCode", SQWebSocketManager.getProductCode(var0));
         if (!isDefaulProject(var0)) {
            ArrayList var2 = new ArrayList<>(getInstance().projects.values());

            for (int var3 = 0; var3 < var2.size(); var3++) {
               SQProject var4 = (SQProject)var2.get(var3);
               if (!isDefaulProject(var4.getName()) && var4.isRunning()) {
                  return;
               }
            }
         }

         var1.put("finished", true);
         DataToSend var6 = new DataToSend("appStatusChanged", var1);
         SQWebSocketManager.addToDataQueue(var6, "SQUANT");
      } catch (Exception var5) {
         Log.error("Exc.", var5);
      }
   }
}
