package com.strategyquant.wizard.desktop.loader;

import com.strategyquant.wizard.desktop.utils.FileUtils;
import com.strategyquant.wizard.desktop.utils.JsonUtils;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class RecentFilesLoader {
   private static final String RECENT_FILE_NAME = "recentFiles.json";
   private static final String DEFAULT_RECENT = "{\"result\":\"success\",\"files\":[]}";
   private boolean netFailed;

   public String loadRecentFiles(String var1) {
      Object var2 = null;
      File var3 = new File(FileUtils.getAppFolder() + "//" + "recentFiles.json");
      if (!var3.exists()) {
         return "{\"result\":\"success\",\"files\":[]}";
      }

      try {
         String var4 = new String(Files.readAllBytes(var3.toPath()));
         return var4.replace("\\\\", "\\").replace("\\", "\\\\");
      } catch (IOException var5) {
         return "{\"result\":\"success\",\"files\":[]}";
      }
   }

   public String saveRecentFiles(String var1, String var2, String var3) {
      this.saveToCloud(var1, var3);

      try {
         File var4 = new File(FileUtils.getAppFolder() + "//" + "recentFiles.json");
         FileWriter var5 = new FileWriter(var4);
         var5.write(var2);
         var5.close();
         return "{\"result\":\"success\"}";
      } catch (IOException var6) {
         return JsonUtils.getErrorResonse(var6.getMessage());
      }
   }

   private String loadFromCloud(String var1) {
      this.netFailed = false;
      String var2 = "http://wizard.strategyquant.com/backend/getrecent?&s=" + var1;

      try {
         return JsonUtils.doGet(var2);
      } catch (IOException var4) {
         this.netFailed = true;
         return JsonUtils.getErrorResonse(var4.getMessage());
      }
   }

   private String saveToCloud(String var1, String var2) {
      this.netFailed = false;
      String var3 = "http://wizard.strategyquant.com/backend/saverecent?&s=" + var2;
      String var4 = var1;

      try {
         return JsonUtils.doPost(var3, var4);
      } catch (IOException var6) {
         this.netFailed = true;
         return JsonUtils.getErrorResonse(var6.getMessage());
      }
   }
}
