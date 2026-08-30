package com.strategyquant.wizard.desktop.settings;

import com.strategyquant.wizard.desktop.utils.FileUtils;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Settings {
   public static final String STRATEGY_FOLDER = "strategyFolder";
   public static final String INDICATOR_FOLDER = "indicatorFolder";
   public static final String RESULT_FOLDER = "resultFolder";
   private static final Logger logger = Logger.getLogger("");
   private static Settings instance = new Settings();
   private Properties properties = new Properties();

   public static Settings get() {
      return instance;
   }

   public void initialize() {
      File var1 = new File(FileUtils.getAppFolder(), "config.properties");

      try {
         FileReader var2 = new FileReader(var1);
         this.properties.load(var2);
         var2.close();
      } catch (Exception var3) {
         logger.log(Level.SEVERE, "Error while loading configuration");
      }
   }

   public String get(String var1) {
      return this.properties.getProperty(var1);
   }

   public void set(String var1, String var2) {
      this.properties.setProperty(var1, var2);
      this.save();
   }

   private void save() {
      File var1 = new File(FileUtils.getAppFolder(), "config.properties");

      try {
         FileWriter var2 = new FileWriter(var1);
         this.properties.store(var2, "");
         var2.close();
      } catch (Exception var3) {
         logger.log(Level.SEVERE, "Error while loading configuration");
      }
   }
}
