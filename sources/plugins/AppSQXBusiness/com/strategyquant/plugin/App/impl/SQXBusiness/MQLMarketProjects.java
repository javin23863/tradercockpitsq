package com.strategyquant.plugin.App.impl.SQXBusiness;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.lib.sqxbusiness.MQLMarketConst;
import java.io.File;
import java.util.LinkedHashMap;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MQLMarketProjects {
   public static final Logger Log = LoggerFactory.getLogger(MQLMarketProjects.class);
   private static final String sampleProjectPath = SQStructure.INTERNAL_DIR_PATH + "web/SQXBUSINESS/Sample Project";
   private static MQLMarketProjects instance;
   private LinkedHashMap<String, Element> projects;

   private MQLMarketProjects() {
      this.loadProjects();
   }

   private static MQLMarketProjects get() {
      if (instance == null) {
         instance = new MQLMarketProjects();
      }

      return instance;
   }

   public static LinkedHashMap<String, Element> list() {
      return get().projects;
   }

   public static void add(String var0, Element var1) throws Exception {
      File var2 = new File(MQLMarketConst.getProjectDirPath(var0));
      if (var2.exists()) {
         throw new Exception(L.t("Cannot create project with name '%s' - Folder already exists", new Object[]{var0}));
      }

      var2.mkdirs();
      SQUtils.stringToFile(var2 + "/" + "config.xml", XMLUtil.elementToString(var1));
      get().projects.put(var0, var1);
   }

   public static void update(String var0, Element var1, String var2) throws Exception {
      File var3 = new File(MQLMarketConst.getProjectDirPath(var0));
      if (var2 != null) {
         if (var3.exists()) {
            throw new Exception(L.t("Cannot rename project - Folder '%s' already exists", new Object[]{var0}));
         }

         File var4 = new File(MQLMarketConst.getProjectDirPath(var2));
         if (!var4.exists()) {
            throw new Exception(L.t("Cannot rename project - Folder '%s' doesn't exist", new Object[]{var2}));
         }

         var4.renameTo(var3);
      }

      SQUtils.stringToFile(var3 + "/" + "config.xml", XMLUtil.elementToString(var1));
      get().projects.put(var0, var1);
   }

   public static void remove(String var0) {
      SQUtils.deleteDirectory(MQLMarketConst.getProjectDirPath(var0));
      get().projects.remove(var0);
   }

   private void loadProjects() {
      try {
         this.projects = new LinkedHashMap<>();
         File var1 = new File(sampleProjectPath);
         if (var1.exists()) {
            try {
               String var2 = var1.getName();
               File var3 = new File(var1.getAbsolutePath() + "/" + "config.xml");
               Element var4 = XMLUtil.fileToXmlElement(var3);
               var4.setAttribute("sample", "true");
               this.projects.put(var2, var4);
            } catch (Exception var10) {
               Log.error("Cannot load SQX Business sample project", var10);
            }
         }

         File var12 = new File(MQLMarketConst.projectsPath);
         if (var12.exists()) {
            File[] var13 = var12.listFiles();

            for (int var14 = 0; var14 < var13.length; var14++) {
               File var5 = var13[var14];
               if (var5.isDirectory()) {
                  File var6 = new File(var5.getAbsolutePath() + "/" + "config.xml");
                  if (var6.exists()) {
                     try {
                        String var7 = var5.getName();
                        Element var8 = XMLUtil.fileToXmlElement(var6);
                        this.projects.put(var7, var8);
                     } catch (Exception var9) {
                        Log.error("Cannot load SQX Business project '" + var5.getName() + "'", var9);
                     }
                  }
               }
            }
         }
      } catch (Exception var11) {
         Log.error("Loading SQX Business projects failed", var11);
      }
   }
}
