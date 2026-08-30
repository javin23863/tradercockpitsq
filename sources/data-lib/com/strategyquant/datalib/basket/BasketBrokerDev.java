package com.strategyquant.datalib.basket;

import com.strategyquant.lib.app.MainApp;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasketBrokerDev {
   private static final Logger Log = LoggerFactory.getLogger(BasketBrokerDev.class);
   private boolean dev;
   private static BasketBrokerDev instance = new BasketBrokerDev();

   public static BasketBrokerDev getInstance() {
      return instance;
   }

   private BasketBrokerDev() {
      File var1 = new File(MainApp.getDataPath() + "/datadev.txt");
      Log.debug("Checking file in location: {}", var1.getAbsolutePath());
      this.dev = var1.exists();
      Log.debug("Dev mode: {}", this.dev);
   }

   public boolean isDev() {
      return this.dev;
   }
}
