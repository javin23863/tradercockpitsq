package com.strategyquant.plugin.Servlet.impl.AlgoWizard;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import java.io.File;
import java.util.ArrayList;
import org.jdom2.Element;
import org.json.JSONArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlgoWizardRecentFiles {
   private static final Logger Log = LoggerFactory.getLogger(AlgoWizardRecentFiles.class);
   public static final String configFilePath = SQPaths.settingsDirPath + "/AlgoWizardRecentFiles.xml";
   private static final int maxRecentFiles = 0;
   private static AlgoWizardRecentFiles instance = null;
   private ArrayList<String> recentFiles = new ArrayList<>();

   private AlgoWizardRecentFiles() {
      instance = this;

      try {
         this.load();
      } catch (Exception var2) {
         Log.error("Cannot load AlgoWizard recent files", var2);
      }
   }

   private void load() throws Exception {
      this.recentFiles.clear();
      File var1 = new File(configFilePath);
      if (!var1.exists()) {
         Log.info("AlgoWizardRecentFiles.xml doesn't exist yet");
      } else {
         Element var2 = XMLUtil.fileToXmlElement(var1);
         int var3 = 0;

         for (Element var5 : var2.getChildren("File")) {
            this.recentFiles.add(var5.getText());
            var3++;
         }
      }
   }

   public static ArrayList<String> list() {
      return get().recentFiles;
   }

   public static JSONArray listJSON() {
      JSONArray var0 = new JSONArray();

      for (String var2 : get().recentFiles) {
         var0.put(var2);
      }

      return var0;
   }

   public static void save(String var0) {
      get()._save(var0);
   }

   private static AlgoWizardRecentFiles get() {
      if (instance == null) {
         instance = new AlgoWizardRecentFiles();
      }

      return instance;
   }

   public void _save(String var1) {
      if (this.recentFiles.contains(var1)) {
         this.recentFiles.remove(var1);
      }

      this.recentFiles.add(0, var1);
      Element var2 = new Element("RecentFiles");

      for (String var4 : this.recentFiles) {
         Element var5 = new Element("File");
         var5.setText(var4);
         var2.addContent(var5);
      }

      SQUtils.stringToFile(new File(configFilePath), XMLUtil.elementToString(var2));
   }
}
