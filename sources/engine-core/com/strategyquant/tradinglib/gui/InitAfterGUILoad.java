package com.strategyquant.tradinglib.gui;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InitAfterGUILoad {
   public static final Logger Log = LoggerFactory.getLogger(InitAfterGUILoad.class);
   public static List<IInitAfterGUILoad> listeners = new ArrayList<>();

   public static void register(IInitAfterGUILoad var0) {
      listeners.add(var0);
   }

   public static void notifyAllListeners() {
      for (int var0 = 0; var0 < listeners.size(); var0++) {
         try {
            listeners.get(var0).callAfterGUILoaded();
         } catch (Exception var2) {
            Log.error("Exc.", var2);
         }
      }
   }
}
