package com.strategyquant.tradinglib.gp.strategies;

import com.strategyquant.gridlib.client.JobDetails;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import com.strategyquant.tradinglib.backtestrunner.DurationStats;
import com.strategyquant.tradinglib.gp.GPIDs;
import com.strategyquant.tradinglib.gp.IGPNode;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Node implements IGPNode {
   private static final Logger Log = LoggerFactory.getLogger("Node");
   private final GPIDs gpIDs = new GPIDs();
   private double fitnessIS = -1.0;
   private double fitnessIST = -1.0;
   private double fitnessISV = -1.0;
   private double fitnessOOS = -1.0;
   private Element elStrategy;
   private int dismissalReason = 0;
   private String errorMsg = null;
   private Exception exception;
   private GeneratedObjectsMap genObjMap = null;
   private ResultsGroup resultsGroup = null;
   private JobDetails jobDetails;
   private DurationStats durationStats;
   private boolean modified = false;
   private int strategyHash = -1;

   public Node(Element var1) {
      this.elStrategy = var1;
   }

   public Node clone() {
      Node var1 = new Node(this.elStrategy.clone());
      var1.fitnessIS = this.fitnessIS;
      var1.fitnessIST = this.fitnessIST;
      var1.fitnessISV = this.fitnessISV;
      var1.fitnessOOS = this.fitnessOOS;
      var1.errorMsg = this.errorMsg;
      var1.exception = this.exception;
      var1.gpIDs.set(this.gpIDs);
      var1.durationStats = this.durationStats;
      var1.resultsGroup = this.resultsGroup;
      var1.modified = this.modified;
      return var1;
   }

   @Override
   public IGPNode clonePassed() {
      Node var1 = new Node(this.elStrategy.clone());
      var1.fitnessIS = this.fitnessIS;
      var1.fitnessIST = this.fitnessIST;
      var1.fitnessISV = this.fitnessISV;
      var1.fitnessOOS = this.fitnessOOS;
      var1.errorMsg = this.errorMsg;
      var1.exception = this.exception;
      var1.gpIDs.set(this.gpIDs);
      var1.durationStats = this.durationStats;
      var1.modified = this.modified;
      if (this.resultsGroup != null) {
         var1.resultsGroup = this.resultsGroup.clone();
      }

      return var1;
   }

   @Override
   public IGPNode cloneFull() {
      Node var1 = new Node(this.elStrategy.clone());
      var1.fitnessIS = this.fitnessIS;
      var1.fitnessIST = this.fitnessIST;
      var1.fitnessISV = this.fitnessISV;
      var1.fitnessOOS = this.fitnessOOS;
      var1.errorMsg = this.errorMsg;
      var1.exception = this.exception;
      var1.gpIDs.set(this.gpIDs);
      var1.durationStats = this.durationStats;
      var1.modified = this.modified;
      if (this.resultsGroup != null) {
         var1.resultsGroup = this.resultsGroup.clone();
         this.resultsGroup.clearTempData();
      }

      return var1;
   }

   @Override
   public IGPNode cloneDismissed(int var1) {
      Node var2 = new Node(null);
      var2.fitnessIS = this.fitnessIS;
      var2.fitnessIST = this.fitnessIST;
      var2.fitnessISV = this.fitnessISV;
      var2.fitnessOOS = this.fitnessOOS;
      var2.errorMsg = this.errorMsg;
      var2.exception = this.exception;
      var2.gpIDs.set(this.gpIDs);
      var2.dismissalReason = var1;
      var2.durationStats = this.durationStats;
      var2.resultsGroup = this.resultsGroup;
      var2.modified = this.modified;
      return var2;
   }

   @Override
   public IGPNode cloneForMigration() {
      Node var1 = new Node(this.elStrategy.clone());
      var1.fitnessIS = this.fitnessIS;
      var1.fitnessIST = this.fitnessIST;
      var1.fitnessISV = this.fitnessISV;
      var1.fitnessOOS = this.fitnessOOS;
      var1.errorMsg = this.errorMsg;
      var1.exception = this.exception;
      var1.gpIDs.set(this.gpIDs);
      var1.durationStats = this.durationStats;
      var1.modified = this.modified;
      var1.resultsGroup = null;
      return var1;
   }

   @Override
   public void destroy() {
      String var1 = this.gpIDs.toShortString();
      if (this.resultsGroup != null) {
         this.resultsGroup.clear();
         this.resultsGroup = null;
      }
   }

   @Override
   public void setFitness(double var1, byte var3) {
      switch (var3) {
         case 10:
            this.fitnessIS = var1;
            break;
         case 11:
            this.fitnessIST = var1;
            break;
         case 20:
            this.fitnessOOS = var1;
            break;
         case 40:
            this.fitnessISV = var1;
      }
   }

   @Override
   public void setFitness(IGPNode var1) {
      Node var2 = (Node)var1;
      this.fitnessIS = var2.getFitness((byte)10);
      this.fitnessIST = var2.getFitness((byte)11);
      this.fitnessISV = var2.getFitness((byte)40);
      this.fitnessOOS = var2.getFitness((byte)20);
      this.resultsGroup = var2.getResultGroup();
   }

   @Override
   public double getFitness(byte var1) {
      switch (var1) {
         case 10:
            return this.fitnessIS;
         case 11:
            return this.fitnessIST;
         case 20:
            return this.fitnessOOS;
         case 40:
            return this.fitnessISV;
         default:
            return -1.0;
      }
   }

   @Override
   public void setGPIDs(GPIDs var1) {
      this.gpIDs.set(var1);
   }

   @Override
   public GPIDs getGPIDs() {
      return this.gpIDs;
   }

   public Element getStrategy() {
      return this.elStrategy;
   }

   public void setError(int var1, String var2, Exception var3) {
      this.fitnessIS = 0.0;
      this.fitnessIST = 0.0;
      this.fitnessISV = 0.0;
      this.fitnessOOS = 0.0;
      this.dismissalReason = var1;
      this.errorMsg = var2;
      this.exception = var3;
   }

   public GeneratedObjectsMap getGeneratedObjectsMap() {
      if (this.genObjMap == null) {
         this.createGeneratedObjectsMap();
      }

      return this.genObjMap;
   }

   private void createGeneratedObjectsMap() {
      this.genObjMap = new GeneratedObjectsMap();
      Element var1 = this.elStrategy.getChild("Strategy").getChild("Rules").getChild("Events");
      this.goThroughXML(var1);
   }

   public void clearGeneratedObjectsMap() {
      if (this.genObjMap != null) {
         this.genObjMap.clear();
         this.genObjMap = null;
      }
   }

   private void goThroughXML(Element var1) {
      List var2 = var1.getChildren();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         String var5 = var4.getAttributeValue("generated");
         if (var5 != null && var5.equals("random")) {
            String var6 = var4.getName();
            if (var6.equals("Item")) {
               this.genObjMap.addItem(var4);
            } else if (var6.equals("Param")) {
               this.genObjMap.addParam(var4);
            }
         }

         this.goThroughXML(var4);
      }
   }

   public void replaceObjectById(int var1, Element var2) {
      Element var3 = this.elStrategy.getChild("Strategy").getChild("Rules").getChild("Events");
      String var4 = Integer.toString(var1);
      this.goThroughXMLAndReplace(var3, var4, var2);
   }

   private void goThroughXMLAndReplace(Element var1, String var2, Element var3) {
      List var4 = var1.getChildren();

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);
         String var7 = var6.getAttributeValue("gid");
         if (var7 != null && var7.equals(var2)) {
            Element var8 = var3.clone();
            String var9 = var6.getAttributeValue("generated");
            String var10 = var6.getAttributeValue("randomId");
            if (var9 != null) {
               var8.setAttribute("generated", var9);
            }

            if (var10 != null) {
               var8.setAttribute("randomId", var10);
            }

            var1.setContent(var5, var8);
            return;
         }

         this.goThroughXMLAndReplace(var6, var2, var3);
      }
   }

   public String getStrategyName(String var1) {
      return var1 != null
         ? String.format("%s%d.%d.%d", var1, this.gpIDs.islandIndex + 1, this.gpIDs.generationIndex, this.gpIDs.nodeIndex)
         : String.format("Strategy %d.%d.%d", this.gpIDs.islandIndex + 1, this.gpIDs.generationIndex, this.gpIDs.nodeIndex);
   }

   public void setResultsGroup(ResultsGroup var1) {
      this.resultsGroup = var1;
   }

   public ResultsGroup getResultGroup() {
      return this.resultsGroup;
   }

   @Override
   public boolean passedEvaluation() {
      return this.errorMsg == null;
   }

   @Override
   public String getErrorMsg() {
      return this.errorMsg;
   }

   @Override
   public int getDismissalReason() {
      return this.dismissalReason;
   }

   @Override
   public void setJobDetails(JobDetails var1) {
      this.jobDetails = var1;
   }

   @Override
   public JobDetails getJobDetails() {
      return this.jobDetails;
   }

   @Override
   public Exception getException() {
      return this.exception;
   }

   @Override
   public String getAsString() {
      return this.gpIDs.toString();
   }

   public DurationStats getDurationStats() {
      return this.durationStats;
   }

   public void setDurationStats(DurationStats var1) {
      this.durationStats = var1;
   }

   public void setModified() {
      this.modified = true;
   }

   public boolean isModified() {
      return this.modified;
   }

   @Override
   public int getHash() {
      if (this.elStrategy == null) {
         return 0;
      }

      if (this.strategyHash == -1) {
         this.strategyHash = XMLUtil.xmlToStringRaw(this.elStrategy).hashCode();
      }

      return this.strategyHash;
   }

   @Override
   public int getFingerprint() {
      if (this.resultsGroup == null) {
         return -1;
      }

      double var1 = this.resultsGroup.getFitness();

      try {
         SQStats var3 = this.resultsGroup.portfolio().stats((byte)0, (byte)10, (byte)127);
         if (var3 != null) {
            int var4 = var3.getInt("NumberOfTrades");
            double var5 = var3.getDouble("NetProfit");
            String var7 = String.format("%.2f__%d", var5, var4);
            return var7.hashCode();
         }
      } catch (Exception var9) {
         Log.error("Cannot compute strategy fingerprint in Gen Evo", var9);
      }

      return -2;
   }
}
