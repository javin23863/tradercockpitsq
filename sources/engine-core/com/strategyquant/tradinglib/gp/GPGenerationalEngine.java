package com.strategyquant.tradinglib.gp;

import com.strategyquant.gridlib.client.GridClient;
import com.strategyquant.gridlib.client.GridJob;
import com.strategyquant.gridlib.client.GridMessage;
import com.strategyquant.gridlib.client.IGridMessageListener;
import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.gridlib.client.SQGrid;
import com.strategyquant.lib.SQUtils;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GPGenerationalEngine<T extends IGPNode> {
   private static final Logger Log = LoggerFactory.getLogger("GPGenerationalEngine");
   private static final int StatusRunning = 1;
   private static final int StatusPaused = 2;
   private static final int StatusStopped = 3;
   private GPSettings<T> gpSettings;
   private int islandIndex;
   private int currentGeneration;
   private GridClient gridClient;
   private String evaluationGroupID;
   private int runningStatus = 0;
   private ArrayList<T> initialPopulation = null;
   private ArrayList<T> population = new ArrayList<>();
   private ArrayList<T> candidatesInbox = new ArrayList<>();
   private Int2IntOpenHashMap similarStrategiesMap = new Int2IntOpenHashMap();
   private String gpGroupID;
   private GPFitnessComparator<T> gpComparator;
   private double thresholdFitness = 0.0;
   private int initialPopulationSize = 0;
   private Int2ObjectOpenHashMap<T> computedFitnessMap = new Int2ObjectOpenHashMap();
   private GPIslandJob<T> gpIslandJob;
   private GridJob parentGridJob;
   private String islandJobID;
   private int lastCheckedGeneration;
   private int restartReason;
   private int finishedJobs;
   private IntOpenHashSet uniqueTopStrategies = new IntOpenHashSet();
   private GPFitnessEvolution<T> gpFitnessEvolution;
   private String source;

   public GPGenerationalEngine(GPIslandJob<T> var1, GPSettings<T> var2, String var3, String var4, String var5, int var6, ArrayList<T> var7, GridJob var8) {
      this.gpIslandJob = var1;
      this.gpSettings = var2;
      this.islandIndex = var6;
      this.gpGroupID = var3;
      this.islandJobID = var4;
      this.parentGridJob = var8;
      this.source = var5;
      this.initialPopulation = var7;
      if (var7 != null) {
         this.initialPopulationSize = var7.size();
      }

      this.gpFitnessEvolution = new GPFitnessEvolution<>(var6, var2);
      this.evaluationGroupID = getEvaluationGroupID(var3, var4);
      this.gpComparator = new GPFitnessComparator<>(var2.naturalFitness, (byte)11);
      this.gridClient = SQGrid.getGridClient();
      this.gridClient.registerMessageListener(this.evaluationGroupID, new IGridMessageListener() {
         public void messageReceived(GridMessage var1) {
            GPGenerationalEngine.this.processMessage(var1);
         }
      });
   }

   public static String getEvaluationGroupID(String var0, String var1) {
      return String.format("%s_%s", var0, var1);
   }

   public void evolve() throws Exception {
      try {
         this.runningStatus = 1;

         do {
            this.gpEvolution();
            if (this.checkStopped()) {
               break;
            }

            if (this.restartReason == 70) {
               String var1 = String.format("-- Evolution on island #%d restarted because of stagnation", this.islandIndex + 1);
               Log.debug(var1);
               this.sendEvolutionStatusMessage(70, var1);
            } else {
               if (!this.gpSettings.restartOnFinish) {
                  break;
               }

               String var9 = String.format("-- Evolution on island #%d ended, restarted again", this.islandIndex + 1);
               this.sendEvolutionStatusMessage(60, var9);
            }
         } while (this.gpSettings.restartOnFinish || this.gpSettings.restartOnStagnation);
      } catch (Exception var7) {
         if (!(var7 instanceof InterruptedException)) {
            Log.error("Error", var7);
            GridMessage var2 = new GridMessage(5, "GPException", SQUtils.getStackTrace(var7));
            this.gridClient.sendMessage(this.gpGroupID, null, var2);
         }
      } finally {
         this.gridClient.stop(this.evaluationGroupID);
         this.gridClient.removeMessageListener(this.evaluationGroupID);
         GridMessage var4 = new GridMessage(1);
         this.gridClient.sendMessage(this.gpGroupID, null, var4);
      }
   }

   private void gpEvolution() throws Exception {
      this.gpFitnessEvolution.reset();
      this.clearFromPreviousEvolution();
      this.generateInitialPopulation();
      this.sortEvaluatedPopulation();
      this.decimateInitialPopulation();
      this.addExistingInitialPopulation();
      this.sendLastPopulation();

      while (true) {
         this.gpFitnessEvolution.computeFitnessData(this.population, this.currentGeneration);
         this.sortEvaluatedPopulation();
         this.currentGeneration++;
         if (this.currentGeneration > this.gpSettings.maxGenerations) {
            break;
         }

         this.checkPaused();
         if (this.checkStopped()) {
            break;
         }

         this.sendEvolutionStatusMessage(30, String.format("-- Evolving generation %d population for island #%d", this.currentGeneration, this.islandIndex + 1));
         this.nextEvolutionStep();
         if (this.gpFitnessEvolution.isStagnating()) {
            this.restartReason = 70;
            break;
         }

         this.sendLastPopulation();
      }

      this.clearFromPreviousEvolution();
      this.sendEvolutionStatusMessage(40, String.format("-- Evolution on island #%d finished", this.islandIndex + 1));
   }

   private void clearFromPreviousEvolution() {
      this.currentGeneration = 0;
      this.restartReason = 0;
      if (this.computedFitnessMap != null) {
         this.computedFitnessMap.clear();
      }

      this.clearPopulation();
      if (this.candidatesInbox != null) {
         this.candidatesInbox.clear();
      }
   }

   private void clearPopulation() {
      if (this.population != null) {
         for (int var1 = 0; var1 < this.population.size(); var1++) {
            IGPNode var2 = this.population.get(var1);
            var2.destroy();
         }

         this.population.clear();
      }
   }

   private void nextEvolutionStep() throws Exception {
      this.receiveImmigrants();
      ArrayList var1 = new ArrayList(this.gpSettings.elitismSize);
      Iterator var2 = this.population.iterator();

      while (var1.size() < this.gpSettings.elitismSize && !this.checkStopped()) {
         if (var2.hasNext()) {
            var1.add((IGPNode)var2.next());
         }
      }

      ArrayList var3 = new ArrayList(this.population.size());
      var3.addAll(
         this.gpSettings
            .selectionStrategy
            .select(this.population, this.gpSettings.naturalFitness, this.population.size() - this.gpSettings.elitismSize, this.gpSettings.rng, var1)
      );
      ArrayList var4 = new ArrayList(var3);
      List var5 = this.gpSettings.evolutionPipeline.apply(var3, this.gpSettings.rng, this.gpSettings.factory, this.islandIndex, this.currentGeneration);
      var5.addAll(var1);
      this.moveNewPopulationToGlobalPopulation(var5);
      this.destroyUnusedCandidates(this.population, var4);
      var5.clear();
      this.evaluatePopulation(3);
      this.sortEvaluatedPopulation();
      this.processFreshBloodSettings();
      this.migrateIndividuals();
   }

   private void destroyUnusedCandidates(List<T> var1, ArrayList<T> var2) {
      for (int var3 = 0; var3 < var2.size(); var3++) {
         IGPNode var4 = (IGPNode)var2.get(var3);
         String var5 = var4.getGPIDs().toShortString();
         if (!this.existsInNewPopulation((T)var4, var1)) {
            var4.destroy();
         }
      }
   }

   private void processFreshBloodSettings() throws Exception {
      if (this.gpSettings.freshBloodReplaceSimilar) {
         this.removeTooSimilarStrategies();
      }

      if (this.gpSettings.freshBloodReplaceWeakest) {
         this.removeWeakestStrategies();
      }

      if (this.population.size() < this.gpSettings.populationSize) {
         this.generateAdditionalCandidates();
      }
   }

   private void removeTooSimilarStrategies() throws Exception {
      byte var1 = 2;
      this.similarStrategiesMap.clear();
      Iterator var2 = this.population.iterator();

      while (var2.hasNext()) {
         IGPNode var3 = (IGPNode)var2.next();
         if (var3.getFitness((byte)11) == 0.0) {
            var3.destroy();
            var2.remove();
         } else {
            int var4 = var3.getFingerprint();
            if (!this.similarStrategiesMap.containsKey(var4)) {
               this.similarStrategiesMap.put(var4, 1);
            } else {
               int var5 = this.similarStrategiesMap.get(var4);
               if (var5 < var1) {
                  this.similarStrategiesMap.put(var4, var5 + 1);
               } else {
                  var3.destroy();
                  var2.remove();
               }
            }
         }
      }
   }

   private void removeWeakestStrategies() throws Exception {
      if (this.currentGeneration != 0 && this.currentGeneration % this.gpSettings.replaceWeakestGenerations == 0) {
         double var1 = Math.min(this.gpSettings.replaceWeakestPct, 50) / 100.0;
         int var3 = 0;
         if (var1 > 0.0) {
            var3 = (int)(var1 * this.gpSettings.populationSize);
         }

         if (var3 == 0) {
            var3 = 1;
         }

         int var4 = this.gpSettings.populationSize - this.population.size();
         int var5 = var3 - var4;
         if (var5 > 0) {
            for (int var6 = 0; var6 < var5; var6++) {
               if (this.population.size() > 0) {
                  int var7 = this.population.size() - 1;
                  IGPNode var8 = this.population.get(var7);
                  var8.destroy();
                  this.population.remove(var7);
               }
            }
         }
      }
   }

   private void generateAdditionalCandidates() throws Exception {
      this.sendEvolutionStatusMessage(80, String.format("-- Generating additional population for island #%d", this.islandIndex + 1));
      GPIDs var1 = new GPIDs();
      var1.islandIndex = this.islandIndex;
      var1.generationIndex = 0;
      var1.generationType = "Initial";
      int var2 = this.gpSettings.populationSize;
      int var3 = this.population.size();
      ArrayList var4 = new ArrayList();

      while (true) {
         Log.debug("Generating additional - current population: {}, required: {}", this.population.size(), var2);
         if (this.population.size() >= var2) {
            this.gpSettings.factory.generateRandomCandidate(this.gpSettings.rng);
            break;
         }

         this.checkPaused();
         if (this.checkStopped()) {
            break;
         }

         int var5 = var2 - this.population.size();
         if (var5 > this.gridClient.getUsedComputedThreads() * 2) {
            var5 = this.gridClient.getUsedComputedThreads() * 2;
         }

         var4.clear();

         for (int var6 = 0; var6 < var5; var6++) {
            IGPNode var7 = this.gpSettings.factory.generateRandomCandidate(this.gpSettings.rng);
            var1.nodeIndex = var3;
            var7.setGPIDs(var1);
            if (Log.isDebugEnabled()) {
               Log.debug("Generated Candidate " + var7.getAsString() + " - " + var7.toString());
            }

            var3++;
            var4.add(var7);
         }

         this.sendEvolutionStatusMessage(90, null);
         this.evaluateGeneratedInitialPopulation(var4);
      }

      this.sendEvolutionStatusMessage(100, String.format("-- Additional population for island #%d generated", this.islandIndex + 1));
   }

   private void moveNewPopulationToGlobalPopulation(List<T> var1) {
      for (int var2 = 0; var2 < this.population.size(); var2++) {
         IGPNode var3 = this.population.get(var2);
         if (!this.existsInNewPopulation((T)var3, var1)) {
            var3.destroy();
         }
      }

      this.population.clear();
      this.population.addAll(var1);
   }

   private boolean existsInNewPopulation(T var1, List<T> var2) {
      for (int var3 = 0; var3 < var2.size(); var3++) {
         if (var1 == var2.get(var3)) {
            return true;
         }
      }

      return false;
   }

   private void printPopulation(String var1) {
      if (this.gpSettings.printPopulationToLog) {
         synchronized (Log) {
            if (Log.isDebugEnabled()) {
               Log.debug("---------------------------------------------------------");
               Log.debug("--- Population for island : " + this.islandIndex + " generation: " + this.currentGeneration);
               Log.debug("--- Text : " + var1);
               Log.debug("---------------------------------------------------------");

               for (int var3 = 0; var3 < this.population.size(); var3++) {
                  IGPNode var4 = this.population.get(var3);
                  Log.debug("Candidate " + var4.getAsString() + " - fitness: " + var4.getFitness((byte)11) + " - " + var4.toString());
               }

               Log.debug("---------------------------------------------------------");
            }
         }
      }
   }

   private void printPopulation2(String var1, ArrayList<T> var2) {
      Log.info("---------------------------------------------------------");
      Log.info("--- Population for island : " + this.islandIndex + " generation: " + this.currentGeneration);
      Log.info("--- Text : " + var1);
      Log.info("---------------------------------------------------------");

      for (int var3 = 0; var3 < var2.size(); var3++) {
         IGPNode var4 = (IGPNode)var2.get(var3);
         Log.info(
            "Candidate {} - fitness IST: {}, IS: {}, ISV: {}, OOS: {} - {}",
            new Object[]{
               var4.getAsString(), var4.getFitness((byte)11), var4.getFitness((byte)10), var4.getFitness((byte)40), var4.getFitness((byte)20), var4.toString()
            }
         );
      }

      Log.info("---------------------------------------------------------");
   }

   private void migrateIndividuals() throws Exception {
      if (this.gpSettings.numberOfIslands != 1) {
         if (this.gpSettings.migrationRate != 0.0) {
            if (this.currentGeneration != 0 && this.currentGeneration % this.gpSettings.migrationGenerationsModulo == 0) {
               int var1 = (int)(this.population.size() * this.gpSettings.migrationRate);
               if (this.gpSettings.migrationRate > 0.0 && var1 == 0) {
                  var1 = 1;
               }

               ArrayList var2 = new ArrayList();
               if (var1 > 0) {
                  for (int var3 = 0; var3 < var1; var3++) {
                     IGPNode var4 = this.population.get(var3).cloneForMigration();
                     var2.add(var4);
                  }

                  this.sendMigrationCandidates(var2);
               }
            }
         }
      }
   }

   private void decimateInitialPopulation() {
      int var1 = this.population.size() - this.gpSettings.populationSize + this.initialPopulationSize;

      for (int var2 = 0; var2 < var1; var2++) {
         if (this.population.isEmpty()) {
            return;
         }

         int var3 = this.population.size() - 1;
         this.population.get(var3).destroy();
         this.population.remove(var3);
      }
   }

   private void sortEvaluatedPopulation() {
      Collections.sort(this.population, this.gpComparator);
   }

   private void generateInitialPopulation() throws Exception {
      this.population.clear();
      this.candidatesInbox.clear();
      this.sendEvolutionStatusMessage(10, String.format("-- Generating initial population for island #%d", this.islandIndex + 1));
      if (this.gpSettings.decimationCoefficient <= 0) {
         this.gpSettings.decimationCoefficient = 1;
      }

      GPIDs var1 = new GPIDs();
      var1.islandIndex = this.islandIndex;
      var1.generationIndex = 0;
      var1.generationType = "Initial";
      int var2 = (this.gpSettings.populationSize - this.initialPopulationSize) * this.gpSettings.decimationCoefficient;
      int var3 = this.population.size();
      ArrayList var4 = new ArrayList();

      while (true) {
         Log.debug("Generating initial - current population: {}, required: {}", this.population.size(), var2);
         if (this.population.size() >= var2) {
            this.gpSettings.factory.generateRandomCandidate(this.gpSettings.rng);
            break;
         }

         this.checkPaused();
         if (this.checkStopped()) {
            break;
         }

         int var5 = var2 - this.population.size();
         if (var5 < 5) {
            var5 *= 2;
         } else if (var5 < 3) {
            var5 *= 4;
         }

         if (var5 > this.gridClient.getUsedComputedThreads() * 2) {
            var5 = this.gridClient.getUsedComputedThreads() * 2;
         }

         var4.clear();

         for (int var6 = 0; var6 < var5; var6++) {
            IGPNode var7 = this.gpSettings.factory.generateRandomCandidate(this.gpSettings.rng);
            var1.nodeIndex = var3;
            var7.setGPIDs(var1);
            if (Log.isDebugEnabled()) {
               Log.debug("Generated Candidate " + var7.getAsString() + " - " + var7.toString());
            }

            var3++;
            var4.add(var7);
         }

         this.sendEvolutionStatusMessage(50, null);
         this.evaluateGeneratedInitialPopulation(var4);
      }

      this.sendEvolutionStatusMessage(20, String.format("-- Initial population for island #%d generated", this.islandIndex + 1));
   }

   private void addExistingInitialPopulation() throws Exception {
      if (this.initialPopulationSize != 0 && this.initialPopulation != null) {
         GPIDs var1 = new GPIDs();
         var1.islandIndex = this.islandIndex;
         var1.generationIndex = 0;
         var1.generationType = "Initial";
         int var2 = this.findBiggestExistingNodeIndexInPopulation();
         Collections.shuffle(this.initialPopulation);

         for (IGPNode var4 : this.initialPopulation) {
            if (this.population.size() >= this.gpSettings.populationSize) {
               break;
            }

            var2++;
            IGPNode var5 = var4.clonePassed();
            var1.nodeIndex = var2;
            var5.setGPIDs(var1);
            this.population.add((T)var5);
         }

         this.evaluatePopulation(2);
         this.sortEvaluatedPopulation();
      }
   }

   private int findBiggestExistingNodeIndexInPopulation() {
      int var1 = 0;

      for (IGPNode var3 : this.population) {
         GPIDs var4 = var3.getGPIDs();
         if (var4.nodeIndex > var1) {
            var1 = var4.nodeIndex;
         }
      }

      return var1;
   }

   private void evaluatePopulation(int var1) throws Exception {
      ArrayList var2 = new ArrayList();

      for (int var3 = 0; var3 < this.population.size(); var3++) {
         IGPNode var4 = this.population.get(var3);
         int var5 = var4.getHash();
         if (var4.getFitness((byte)11) >= 0.0 && !this.computedFitnessMap.containsKey(var5)) {
            this.computedFitnessMap.put(var5, var4);
         }
      }

      for (int var10 = 0; var10 < this.population.size(); var10++) {
         IGPNode var11 = this.population.get(var10);
         if (var11.getFitness((byte)11) < 0.0) {
            int var12 = var11.getHash();
            if (this.computedFitnessMap.containsKey(var12)) {
               IGPNode var6 = (IGPNode)this.computedFitnessMap.get(var12);
               var11.setFitness(var6);
            } else {
               GPIDs var13 = var11.getGPIDs();
               String var7 = String.format("%s-Generation %d.%d.%d", this.gpIslandJob.getJobId(), var13.islandIndex + 1, var13.generationIndex, var13.nodeIndex);
               HashMap var8 = new HashMap();
               var8.put("evaluator", (Serializable)this.gpSettings.evaluator.getClone());
               var8.put("candidate", var11);
               var8.put("populationType", var1);
               var8.put("sendLastPopulation", this.gpSettings.sendLastPopulation);
               GPEvaluationJob var9 = new GPEvaluationJob(var7, var8);
               if (this.parentGridJob != null) {
                  var9.setPriority(this.parentGridJob.getPriority() + 1);
               }

               var2.add(var9);
            }
         }
      }

      Log.debug("Created {} evaluation jobs in island {}", var2.size(), this.islandIndex + 1);
      if (var2.size() != 0) {
         this.removeUncomputedFitnessCandidates();
         if (this.gpSettings.singleThreaded) {
            this.executeInSingleThread(var2);
         } else {
            this.executeInGrid(var2);
         }
      }
   }

   private void evaluateGeneratedInitialPopulation(ArrayList<T> var1) throws Exception {
      byte var2 = 1;
      ArrayList var3 = new ArrayList();

      for (int var4 = 0; var4 < var1.size(); var4++) {
         IGPNode var5 = (IGPNode)var1.get(var4);
         GPIDs var6 = var5.getGPIDs();
         String var7 = String.format("%s-Generation %d.%d.%d", this.gpIslandJob.getJobId(), var6.islandIndex + 1, var6.generationIndex, var6.nodeIndex);
         HashMap var8 = new HashMap();
         var8.put("evaluator", (Serializable)this.gpSettings.evaluator.getClone());
         var8.put("candidate", var5);
         var8.put("populationType", Integer.valueOf(var2));
         var8.put("sendLastPopulation", this.gpSettings.sendLastPopulation);
         GPEvaluationJob var9 = new GPEvaluationJob(var7, var8);
         if (this.parentGridJob != null) {
            var9.setPriority(this.parentGridJob.getPriority() + 1);
         }

         var3.add(var9);
      }

      if (var3.size() != 0) {
         if (this.gpSettings.singleThreaded) {
            this.executeInSingleThread(var3);
         } else {
            this.executeInGrid(var3);
         }
      }
   }

   private void executeInSingleThread(ArrayList<GPEvaluationJob<T>> var1) {
      for (int var2 = 0; var2 < var1.size(); var2++) {
         GPEvaluationJob var3 = (GPEvaluationJob)var1.get(var2);
         IGPNode var4 = null;
         JobDetails var5 = new JobDetails(var3.getJobId());

         try {
            long var6 = System.currentTimeMillis();
            var4 = var3.call();
            long var8 = System.currentTimeMillis() - var6;
            this.checkPaused();
            if (this.checkStopped()) {
               break;
            }

            var5.setDuration(var8);
            this.addToPopulation((T)var4, var5);
         } catch (Throwable var10) {
            var5.setException(SQUtils.getStackTrace(var10));
            this.addToPopulation((T)var4, var5);
            Log.error("Error while running task #{}", var2, var10);
         }
      }
   }

   private void executeInGrid(ArrayList<GPEvaluationJob<T>> var1) throws Exception {
      int var2 = var1.size();
      this.finishedJobs = 0;
      this.gridClient.executeOnGrid(this.evaluationGroupID, var1);
      this.gridClient.waitForFinish(this, this.islandJobID, this.gpGroupID, this.evaluationGroupID);
      if (this.finishedJobs != var2) {
      }

      if (this.checkStopped()) {
         throw new InterruptedException();
      }
   }

   private void removeUncomputedFitnessCandidates() {
      Iterator var1 = this.population.iterator();

      while (var1.hasNext()) {
         IGPNode var2 = (IGPNode)var1.next();
         if (var2.getFitness((byte)11) < 0.0) {
            var1.remove();
         }
      }
   }

   protected void processMessage(GridMessage var1) {
      if (var1.getMessageID() == 1) {
         this.messageJobFinished(var1);
      } else if (var1.getMessageID() == 5 && var1.getCustomID().equals("GPThresholdFitness")) {
         this.messageThresholdFitness(var1);
      }
   }

   private void messageThresholdFitness(GridMessage var1) {
      double var2 = (Double)var1.getData();
      if (this.gpSettings.naturalFitness) {
         if (var2 > this.thresholdFitness) {
            this.thresholdFitness = var2;
         }
      } else if (var2 < this.thresholdFitness) {
         this.thresholdFitness = var2;
      }
   }

   private void messageJobFinished(GridMessage var1) {
      IGPNode var2 = (IGPNode)var1.getData();
      if (var2 == null) {
         try {
            GridMessage var3 = new GridMessage(5, "GPException", "Evaluated candidate is null - exception?");
            this.gridClient.sendMessage(this.gpGroupID, null, var3);
            this.gridClient.stop(this.evaluationGroupID);
            this.gridClient.removeMessageListener(this.evaluationGroupID);
            var3 = new GridMessage(1);
            this.gridClient.sendMessage(this.gpGroupID, null, var3);
         } catch (Exception var6) {
            Log.error("Exception in messageJobFinished", var6);
         }
      }

      JobDetails var8 = var1.getJobDetails();
      if (var8 != null && var8.getException() != null && !this.checkStopped() && var8.getException().contains("OutOfMemoryError")) {
         try {
            this.gridClient.sendMessage(this.gpGroupID, null, var1);
            this.gridClient.stop(this.gpGroupID);
            this.stop();
         } catch (Exception var5) {
            Log.error("Cannot send Error message to grid client", var5);
         }
      }

      if (this.checkStopped()) {
         if (Log.isDebugEnabled()) {
            Log.debug("Destroying, engine is stopped");
         }

         if (var2 != null) {
            var2.destroy();
         }
      } else {
         this.finishedJobs++;
         this.addToPopulation((T)var2, var1.getJobDetails());
      }
   }

   private void addToPopulation(T var1, JobDetails var2) {
      if (this.currentGeneration == 0 && var1.getGPIDs().generationIndex == 0 && !var1.passedEvaluation()) {
         String var7 = var1.getGPIDs().toString();
         if (var1.getDismissalReason() == 10001) {
            Log.error("There was exception evaluating candidate, exception: " + var1.getException() + ", error: " + var1.getErrorMsg());
         }

         var1.destroy();
      } else {
         synchronized (this.population) {
            this.population.add((T)var1);
         }
      }

      try {
         this.sendToGPEngine((T)var1, var2);
      } catch (Exception var5) {
         Log.error("Cannot send candidate from Job to GPEngine", var5);
      }
   }

   private void sendToGPEngine(T var1, JobDetails var2) throws Exception {
      if (this.gpSettings.sendEveryCandidateEvent && !this.checkStopped()) {
         IGPNode var3;
         if (var1.passedEvaluation()) {
            GPIDs var4 = var1.getGPIDs();
            if (var4.generationIndex == 0) {
               var3 = var1.cloneDismissed(10005);
            } else if (this.thresholdFitness != 0.0 && !(var1.getFitness((byte)11) > this.thresholdFitness)) {
               var3 = var1.cloneDismissed(10000);
            } else {
               var3 = var1.clonePassed();
            }
         } else {
            var3 = var1.cloneDismissed(var1.getDismissalReason());
         }

         var3.setJobDetails(var2);
         GridMessage var5 = new GridMessage(5, "GPEvaluatedCandidate", var3);
         this.gridClient.sendMessage(this.gpGroupID, null, var5);
      }
   }

   private void sendLastPopulation() throws Exception {
      if (this.islandIndex == 0 && this.gpSettings.sendLastPopulation && !this.checkStopped()) {
         ArrayList var1 = new ArrayList();

         for (int var2 = 0; var2 < this.population.size(); var2++) {
            var1.add(this.population.get(var2).clonePassed());
         }

         GPEvolutionPopulationMessage var4 = new GPEvolutionPopulationMessage(this.currentGeneration, this.islandIndex, var1);
         GridMessage var3 = new GridMessage(5, "GPLastPopulation", var4);
         this.gridClient.sendMessage(this.gpGroupID, null, var3);
      }
   }

   private void sendEvolutionStatusMessage(int var1, String var2) throws Exception {
      GPEvolutionMessage var3 = new GPEvolutionMessage(var1, this.currentGeneration, this.islandIndex, this.population.size(), var2);
      var3.fitnessEvolutionData = this.gpFitnessEvolution.getData();
      this.updateEvolutionData(var3.fitnessEvolutionData);
      GridMessage var4 = new GridMessage(5, "GPEvolutionStatus", var3);
      this.gridClient.sendMessage(this.gpGroupID, null, var4);
   }

   private void updateEvolutionData(GPFitnessEvolutionData var1) {
      var1.populationSize = this.population.size();
      var1.generation = this.currentGeneration;
   }

   public void pause() {
      if (this.runningStatus == 1) {
         this.runningStatus = 2;
      }
   }

   public void restart() {
      if (this.runningStatus == 2) {
         this.runningStatus = 1;
      }
   }

   public void stop() {
      if (this.runningStatus != 3) {
         this.runningStatus = 3;
         this.gridClient.stop(this.evaluationGroupID);
      }
   }

   private boolean checkStopped() {
      return this.runningStatus == 3;
   }

   private void checkPaused() throws InterruptedException {
      if (this.runningStatus == 2) {
         while (this.runningStatus == 2) {
            Thread.sleep(100L);
         }
      }
   }

   public void destroy() {
   }

   private void sendMigrationCandidates(ArrayList<IGPNode> var1) throws Exception {
      if (this.gpSettings.numberOfIslands != 1) {
         int var2 = this.islandIndex + 1;
         if (var2 == this.gpSettings.numberOfIslands) {
            var2 = 0;
         }

         String var3 = GPIslandJob.createJobID(var2, this.parentGridJob, this.source);
         GridMessage var4 = new GridMessage(5, "GBIslandExchange", var1);
         this.gridClient.sendMessage(this.gpGroupID, var3, var4);
      }
   }

   private void receiveImmigrants() {
      if (this.currentGeneration >= this.gpSettings.migrationGenerationsModulo) {
         synchronized (this.candidatesInbox) {
            if (this.candidatesInbox.size() > 0) {
               int var2 = (int)(0.2 * this.population.size());
               if (var2 == 0) {
                  var2 = 1;
               }

               if (this.candidatesInbox.size() < var2) {
                  this.candidatesInbox.size();
               }

               for (int var3 = 0; var3 < this.candidatesInbox.size(); var3++) {
                  if (var3 < this.population.size() / 2) {
                     int var4 = this.population.size() - 1 - var3;
                     this.population.get(var4).destroy();
                     this.population.remove(var4);
                     this.population.add(this.candidatesInbox.get(var3));
                  }
               }

               this.candidatesInbox.clear();
               this.sortEvaluatedPopulation();
            }
         }
      }
   }

   public void addToInbox(List<T> var1) {
      synchronized (this.candidatesInbox) {
         int var3 = (int)(0.2 * this.gpSettings.populationSize);
         if (var3 == 0) {
            var3 = 1;
         }

         this.candidatesInbox.addAll(var1);
         if (this.candidatesInbox.size() > var3) {
            Collections.shuffle(this.candidatesInbox);
            this.shrinkTo(this.candidatesInbox, var3);
         }
      }
   }

   private void shrinkTo(List var1, int var2) {
      int var3 = var1.size();
      if (var2 < var3) {
         for (int var4 = var2; var4 < var3; var4++) {
            var1.remove(var1.size() - 1);
         }
      }
   }
}
