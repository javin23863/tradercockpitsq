package com.strategyquant.strategyquant;

import com.strategyquant.lib.app.MainApp;

public class SQInitializer {
   public SQInitializer() {
      MainApp.setMultiInstance(true);
      MainApp.addInstance(new SQApp(new MainWindow()));
      MainApp.settings().load();
      MainApp.start();
   }
}
