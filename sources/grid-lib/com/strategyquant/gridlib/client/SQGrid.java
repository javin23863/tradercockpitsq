package com.strategyquant.gridlib.client;

import com.strategyquant.gridlib.config.Config;
import java.io.IOException;
import javax.jms.JMSException;

public class SQGrid {
   private static SQGrid instance;
   private GridClient gridClient;

   public static void init(Config var0) throws Exception {
      init(new GridClient(var0));
   }

   public static void init(GridClient var0) throws Exception {
      if (instance != null) {
         throw new Exception("SQGrid.init() called more than once!");
      }

      instance = new SQGrid(var0);
   }

   private SQGrid(GridClient var1) throws JMSException, IOException {
      this.gridClient = var1;
   }

   public static GridClient getGridClient() {
      if (instance == null) {
         throw new RuntimeException("You have to call SQGrid.init() on program startup!");
      } else {
         return instance.gridClient;
      }
   }

   public static boolean started() {
      return instance != null;
   }

   public static void close() throws Exception {
      instance.gridClient.close();
      instance = null;
   }
}
