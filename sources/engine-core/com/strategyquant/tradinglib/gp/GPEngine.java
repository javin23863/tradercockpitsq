package com.strategyquant.tradinglib.gp;

import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.client.IGridMessageListener;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.L;
import com.strategyquant.lib.utils.SQUUID;
import com.strategyquant.tradinglib.project.IProgressStatusListener;
import com.strategyquant.tradinglib.project.ProgressEngine;
import com.strategyquant.tradinglib.project.StopPauseEngine;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GPEngine<T extends IGPNode> implements IProgressStatusListener, IGridMessageListener {
   private static final Logger Log = LoggerFactory.getLogger("GPIslandsEngine");
   private GPSettings<T> gpSettings;
   private String gpGroupID;
   private GridClient gridClient;
   private IGPEvolutionMessagesListener<T> evolutionMessagesListener;
   private ArrayList<String> islandJobIds = new ArrayList<>();
   private StopPauseEngine stopPauseEngine;
   private int runningStatus = 0;
   private IGPFinishedListener gpFinishedListener;
   private ArrayList<T> initialPopulation = null;
   private GridJob parentGridJob;
   private GPIslandJob<T> singleThreadedIsland;
   private IStopConditionsChecker stopConditionsChecker;
   private String source;

   public GPEngine(GPSettings<T> var1, StopPauseEngine var2, ArrayList<T> var3, GridJob var4, String var5) {
      this(var1, var2, var3, var4, var5, null);
   }

   public GPEngine(GPSettings<T> var1, StopPauseEngine var2, ArrayList<T> var3, GridJob var4, String var5, IStopConditionsChecker var6) {
      this.gpSettings = var1;
      this.stopPauseEngine = var2;
      this.initialPopulation = var3;
      this.parentGridJob = var4;
      this.stopConditionsChecker = var6;
      this.source = var5;
      this.gpGroupID = SQUUID.randomUUID().substring(0, 6);
      this.checkSettings();
      this.gridClient = SQGrid.getGridClient();
      this.gridClient.registerMessageListener(this.gpGroupID, this);
      var2.registerStatusListener(this);
   }

   @Override
   public void onStatusChanged(int var1) {
      if (var1 != 4 && var1 != 3 && var1 != 50) {
         if (var1 == 2) {
            if (this.runningStatus == 1) {
               this.runningStatus = var1;
               if (this.singleThreadedIsland != null) {
                  this.singleThreadedIsland.messageReceived(new GridMessage(2));
               } else {
                  this.gridClient.pause(this.gpGroupID);
               }
            }
         } else if (var1 == 1 && this.runningStatus == 2) {
            this.runningStatus = var1;
            if (this.singleThreadedIsland != null) {
               this.singleThreadedIsland.messageReceived(new GridMessage(3));
            } else {
               this.gridClient.restart(this.gpGroupID);
            }
         }
      } else {
         if (this.singleThreadedIsland != null) {
            this.singleThreadedIsland.messageReceived(new GridMessage(4));
         } else {
            this.gridClient.stop(this.gpGroupID);
         }

         this.stopPauseEngine.unregisterStatusListener(this);
      }
   }

   public void start() throws Exception {
      if (this.runningStatus == 0) {
         this.runningStatus = 1;
         if (this.gpSettings.singleThreaded && this.gpSettings.numberOfIslands != 1) {
            throw new Exception("Single threaded mode cannot be used if number of islands is bigger than 1!");
         }

         if (this.stopPauseEngine instanceof ProgressEngine) {
         }

         ArrayList var1 = new ArrayList();

         for (int var2 = 0; var2 < this.gpSettings.numberOfIslands; var2++) {
            Map var3 = this.getIslandParams(var2);
            String var4 = GPIslandJob.createJobID(var2, this.parentGridJob, this.source);
            this.islandJobIds.add(var4);
            GPIslandJob var5 = new GPIslandJob(var4, var3, this.parentGridJob);
            var1.add(var5);
         }

         Log.debug(L.t("Created %d island jobs", new Object[]{var1.size()}));
         if (this.gpSettings.singleThreaded) {
            if (var1.size() != 1) {
               throw new Exception("Single threaded mode - incorrect number of island jobs: " + var1.size());
            }

            this.singleThreadedIsland = (GPIslandJob<T>)var1.get(0);
            this.singleThreadedIsland.call();
            this.runningStatus = 3;
            if (this.gpFinishedListener != null) {
               this.gpFinishedListener.finished(1);
            }

            this.gridClient.stop(this.gpGroupID);
            this.gridClient.removeMessageListener(this.gpGroupID);
         } else {
            this.gridClient.executeOnGrid(this.gpGroupID, var1);
         }
      }
   }

   protected void processMessage(GridMessage var1) {
      JobDetails var2 = var1.getJobDetails();
      if (var2 != null
         && var2.getException() != null
         && var2.getException().contains("OutOfMemoryError")
         && this.gpFinishedListener != null
         && this.gridClient.countAllJobs(this.gpGroupID) > 0L) {
         this.gpFinishedListener.finished(3);
      } else {
         if (var1.getMessageID() == 1) {
            if (this.gridClient.countAllJobs(this.gpGroupID) == 1L) {
               Log.debug("GP Engine finished");
               if (var2 != null && var2.getException() != null && this.stopPauseEngine instanceof ProgressEngine) {
                  ((ProgressEngine)this.stopPauseEngine).printToLog(L.t("Genetic engine finished with exception %s", new Object[]{var2.getException()}));
               }

               this.runningStatus = 3;
               if (this.gpFinishedListener != null) {
                  this.gpFinishedListener.finished(1);
               }

               this.gridClient.stop(this.gpGroupID);
               this.gridClient.removeMessageListener(this.gpGroupID);
            }
         } else if (var1.getMessageID() == 5 && var1.getCustomID().equals("GPEvaluatedCandidate")) {
            if (this.evolutionMessagesListener != null) {
               IGPNode var5 = (IGPNode)var1.getData();
               this.evolutionMessagesListener.newCandidate((T)var5);
            }
         } else if (var1.getMessageID() == 5 && var1.getCustomID().equals("GPEvolutionStatus")) {
            if (this.evolutionMessagesListener != null) {
               GPEvolutionMessage var4 = (GPEvolutionMessage)var1.getData();
               this.evolutionMessagesListener.evolutionMessage(var4);
            }
         } else if (var1.getMessageID() == 5 && var1.getCustomID().equals("GPException")) {
            if (this.evolutionMessagesListener != null) {
               this.evolutionMessagesListener.evolutionException(var1.getData());
            }
         } else if (var1.getMessageID() == 5 && var1.getCustomID().equals("GPLastPopulation") && this.evolutionMessagesListener != null) {
            GPEvolutionPopulationMessage var3 = (GPEvolutionPopulationMessage)var1.getData();
            this.evolutionMessagesListener.lastPopulation(var3);
         }
      }
   }

   private Map<String, Serializable> getIslandParams(int var1) {
      HashMap var2 = new HashMap();
      var2.put("GPSettings", this.gpSettings);
      var2.put("GPGroupID", this.gpGroupID);
      var2.put("IslandIndex", var1);
      var2.put("GPSource", this.source);
      if (this.initialPopulation != null) {
         var2.put("GPInitialPopulation", (ArrayList)this.initialPopulation.clone());
      } else {
         var2.put("GPInitialPopulation", null);
      }

      var2.put("IslandRandomSeed", var1 + this.gpSettings.rng.nextLong());
      return var2;
   }

   private void checkSettings() {
      if (this.gpSettings.numberOfIslands == 0) {
         this.gpSettings.numberOfIslands = 1;
      }
   }

   public void setEvolutionMessagesListener(IGPEvolutionMessagesListener<T> var1) {
      this.evolutionMessagesListener = var1;
   }

   public void broadcastMessage(String var1, Serializable var2) throws Exception {
      if (Log.isDebugEnabled()) {
         Log.debug("Broadcasting message {}, data {}", var1, var2);
      }

      for (int var3 = 0; var3 < this.islandJobIds.size(); var3++) {
         String var4 = GPGenerationalEngine.getEvaluationGroupID(this.gpGroupID, this.islandJobIds.get(var3));
         GridMessage var5 = new GridMessage(5, var1, var2);
         this.gridClient.sendMessage(var4, null, var5);
      }
   }

   public void broadcastNewThresholdFitness(double var1) throws Exception {
      this.broadcastMessage("GPThresholdFitness", var1);
   }

   public void setFinishListener(IGPFinishedListener var1) {
      this.gpFinishedListener = var1;
   }

   public boolean isFinished() {
      return this.runningStatus == 3;
   }

   public void destroy() {
      this.unregisterListeners();
      this.gpFinishedListener = null;
      this.evolutionMessagesListener = null;
   }

   public void unregisterListeners() {
      if (this.gridClient != null) {
         this.gridClient.removeMessageListener(this.gpGroupID);
      }

      if (this.stopPauseEngine != null) {
         this.stopPauseEngine.unregisterStatusListener(this);
      }
   }

   public void messageReceived(GridMessage var1) {
      this.processMessage(var1);
   }
}
