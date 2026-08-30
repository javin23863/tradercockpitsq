package com.strategyquant.tradinglib.gp;

import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.lib.IRandomGenerator;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GPIslandJob<T extends IGPNode> extends GridJob<Integer> {
   private static final long serialVersionUID = -2179402133615108244L;
   private static final Logger Log = LoggerFactory.getLogger("GPIslandJob");
   private GPSettings<T> gpSettings;
   private int islandIndex;
   private GPGenerationalEngine<T> engine;
   private String gpGroupID;
   private IRandomGenerator rng;
   private ArrayList<T> initialPopulation;
   private GridJob parentGridJob;
   private String source;

   public GPIslandJob(String var1, Map<String, Serializable> var2, GridJob var3) {
      super(var1, 0, var2);
      long var4 = (Long)var2.get("IslandRandomSeed");
      GPSettings var6 = (GPSettings)var2.get("GPSettings");
      this.initialPopulation = (ArrayList<T>)var2.get("GPInitialPopulation");
      this.rng = var6.rng.clone(var4);
      this.gpSettings = var6.clone(this.rng);
      this.gpGroupID = (String)var2.get("GPGroupID");
      this.islandIndex = (Integer)var2.get("IslandIndex");
      this.source = (String)var2.get("GPSource");
      this.parentGridJob = var3;
   }

   public static String createJobID(int var0, GridJob var1, String var2) {
      if (var1 != null) {
         String var3 = var1.getJobId();
         return String.format("%s-%s_%d", var3, var2, var0);
      } else {
         return String.format("%s_%d", var2, var0);
      }
   }

   public Integer call() throws Exception {
      Log.debug("Island Job started");

      try {
         this.engine = new GPGenerationalEngine<>(
            this, this.gpSettings, this.gpGroupID, this.getJobId(), this.source, this.islandIndex, this.initialPopulation, this.parentGridJob
         );
         this.engine.evolve();
      } finally {
         Log.debug("Island Job finished");
         if (this.gpSettings.factory != null) {
            this.gpSettings.factory.destroy();
         }
      }

      return null;
   }

   public void messageReceived(GridMessage var1) {
      if (var1.getMessageID() == 2) {
         if (this.engine != null) {
            this.engine.pause();
         }
      } else if (var1.getMessageID() == 3) {
         if (this.engine != null) {
            this.engine.restart();
         }
      } else if (var1.getMessageID() == 4) {
         if (this.engine != null) {
            this.engine.stop();
         }
      } else if (var1.getMessageID() == 5 && var1.getCustomID().equals("GBIslandExchange")) {
         List var2 = (List)var1.getData();
         this.engine.addToInbox(var2);
      }
   }

   public void destroy() {
      Log.debug(String.format("Job %s destroyed", this.getJobId()));
      if (this.engine != null) {
         this.engine.stop();
         this.engine.destroy();
      }
   }
}
