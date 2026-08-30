package com.strategyquant.tradinglib.project;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.databank.IProgressListener;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Databanks extends LinkedHashMap<String, Databank> {
   public static final Logger Log = LoggerFactory.getLogger(Databanks.class);
   private String databanksFolder = null;
   private String projectName;
   private DatabankPositionComparator positionComparator = new DatabankPositionComparator();

   public Databanks(String var1) {
      this.projectName = var1;
      this.databanksFolder = SQPaths.projectsDirPath + "/" + this.projectName + "/databanks";
   }

   public Databank add(String var1) {
      if (this.containsKey(var1)) {
         return this.get(var1);
      }

      Databank var2 = new Databank(this.projectName, var1, this.databanksFolder);
      this.put(var1, var2);
      return var2;
   }

   public void add(Databank var1) throws Exception {
      String var2 = var1.getName();
      if (this.containsKey(var2)) {
         throw new Exception(L.t("Databank with name '%s' already exists.", new Object[]{var2}));
      }

      this.put(var2, var1);
   }

   public void remove(Databank var1) throws Exception {
      String var2 = var1.getName();
      if (this.containsKey(var2)) {
         throw new Exception(L.t("Databank with name '%s' does not exist.", new Object[]{var2}));
      }

      var1.destroyTimers();
      super.remove(var2);
   }

   public ArrayList<Databank> list() {
      return new ArrayList<>(this.values());
   }

   public boolean loadFromConfig(Element var1) throws Exception {
      Element var2 = XMLUtil.getChildElem(var1, "Databanks");
      List var3 = var2.getChildren("Databank");
      boolean var4 = false;

      for (int var5 = 0; var5 < var3.size(); var5++) {
         Element var6 = (Element)var3.get(var5);
         String var7 = var6.getAttributeValue("name");
         String var8 = var6.getAttributeValue("view");
         String var9 = var6.getAttributeValue("syncType");
         String var10 = var6.getAttributeValue("position");
         int var11 = this.getDatabankPosition(var5);

         try {
            var11 = Integer.parseInt(var10);
         } catch (Exception var13) {
         }

         if (!this.containsKey(var7)) {
            Databank var12 = new Databank(this.projectName, var7, this.databanksFolder);
            var12.setSyncType(var9 != null ? var9 : "Auto-sync never");
            var12.setPosition(var11);
            if (!var12.setViewByName(var8)) {
               var6.setAttribute("view", "Default - Main data");
               var4 = true;
            }

            this.add(var12);
         }
      }

      if (this.size() == 0) {
         this.add("Results");
         var4 = true;
      }

      return var4;
   }

   private synchronized void setProgressInfo(String var1) {
      double var2 = 0.0;
      int var4 = this.size();
      if (!this.loaded() && var4 != 0) {
         for (Databank var6 : this.list()) {
            var2 += var6.getPercentLoaded();
         }

         var2 /= var4;
      } else {
         var2 = 100.0;
      }

      int var10 = var2 == 100.0 ? 0 : 100;
      String var11 = var2 == 100.0 ? "" : "Loading strategies...";

      try {
         SQProject var7 = ProjectEngine.get(this.projectName);
         var7.getTrackingInfo().infiniteProgress = false;
         var7.getTrackingInfo().progressPercent = var2;
         var7.getTrackingInfo().info = var11;
         var7.progressEngine.setRunningStatus(var10);
         if (var1 != null) {
            var7.progressEngine.printToLog(var1);
         }
      } catch (Exception var8) {
         Log.error("Sending loading progress of project '" + this.projectName + "' failed. ", var8);
      }
   }

   public void loadStrategies() throws Exception {
      for (final Databank var2 : this.list()) {
         final long var3 = System.currentTimeMillis();
         var2.loadStrategies(
            new IProgressListener() {
               @Override
               public void onError(double var1, String var3x) {
                  String var4 = "Loading strategies to databank '" + var2.getProject() + "/" + var2.getName() + "' failed - " + var3x;
                  Databanks.Log.error(var4);
                  Databanks.this.setProgressInfo(var4);
               }

               @Override
               public void onDone() {
                  if (!var2.getRecords().isEmpty()) {
                     String var1 = "Databank '"
                        + var2.getProject()
                        + "/"
                        + var2.getName()
                        + "' loaded - "
                        + var2.getRecords().size()
                        + " strategies in "
                        + (System.currentTimeMillis() - var3)
                        + "ms";
                     Databanks.Log.info(var1);
                     Databanks.this.setProgressInfo(var1);
                  }
               }

               @Override
               public void onProgress(double var1) {
                  Databanks.this.setProgressInfo(null);
               }
            }
         );
      }
   }

   public boolean loaded() {
      for (Databank var2 : this.list()) {
         if (var2.isLoading()) {
            return false;
         }
      }

      return true;
   }

   public void clear(boolean var1, String var2) {
      for (Databank var4 : this.list()) {
         try {
            var4.clearRecords(true, var1, var2);
            var4.destroyTimers();
         } catch (Exception var6) {
            Log.error("Error clearing databank", var6);
         }
      }
   }

   public String getProjectName() {
      return this.projectName;
   }

   public static boolean isDefault(String var0) {
      switch (var0) {
         case "Results":
         case "Last generation":
         case "Initial population":
         case "Strategies to improve":
         case "Strategies to optimize":
         case "Existing portfolio":
         case "Simple strategies":
            return true;
         default:
            return false;
      }
   }

   public void setProjectName(String var1) {
      this.projectName = var1;
      this.databanksFolder = SQPaths.projectsDirPath + "/" + var1 + "/databanks";

      for (String var3 : this.keySet()) {
         this.get(var3).setProjectName(var1);
      }
   }

   public void move(String var1, boolean var2) throws Exception {
      ArrayList var3 = new ArrayList();
      int var4 = this.findAndListCustomDatabanks(var1, var3);
      int var5 = var4 + (var2 ? -1 : 1);
      if ((var4 != 0 || !var2) && (var4 != var3.size() - 1 || var2)) {
         var3.remove(var4);
         var3.add(var5, var1);
      }

      for (int var6 = 0; var6 < var3.size(); var6++) {
         Databank var7 = this.get(var3.get(var6));
         var7.setPosition(this.getDatabankPosition(var6));
      }

      this.list().sort(this.positionComparator);
   }

   public void moveToPosition(String var1, int var2) throws Exception {
      ArrayList var3 = new ArrayList();
      int var4 = this.findAndListCustomDatabanks(var1, var3);
      int var5 = var2 - 1;
      if (var2 >= 1 && var2 <= var3.size()) {
         var3.remove(var4);
         var3.add(var5, var1);

         for (int var6 = 0; var6 < var3.size(); var6++) {
            Databank var7 = this.get(var3.get(var6));
            var7.setPosition(this.getDatabankPosition(var6));
         }

         this.list().sort(this.positionComparator);
      } else {
         throw new Exception(L.t("Incorrect position chosen (%d). Must be in range 1-%d", new Object[]{var2, var3.size()}));
      }
   }

   private int findAndListCustomDatabanks(String var1, ArrayList<String> var2) throws Exception {
      if (!this.containsKey(var1)) {
         throw new Exception(L.t("Databank '%s' doesn't exist", new Object[]{var1}));
      }

      ArrayList var3 = this.list();
      var3.sort(this.positionComparator);
      int var4 = 0;
      int var5 = 0;

      for (int var6 = 0; var6 < var3.size(); var6++) {
         Databank var7 = (Databank)var3.get(var6);
         if (var7.isDefault()) {
            var7.setPosition(var5++);
         } else {
            var2.add(var7.getName());
            if (var7.getName().equals(var1)) {
               var4 = var2.size() - 1;
            }
         }
      }

      return var4;
   }

   public void rename(String var1, String var2) throws Exception {
      Databank var3 = this.remove(var1);
      if (var3 == null) {
         throw new Exception(L.t("Databank '%s' doesn't exist", new Object[]{var1}));
      }

      var3.rename(var2);
      this.put(var2, var3);
   }

   public void updatePositions() {
      ArrayList var1 = this.list();
      var1.sort(this.positionComparator);

      for (int var2 = 0; var2 < var1.size(); var2++) {
         Databank var3 = (Databank)var1.get(var2);
         if (var3.isDefault()) {
            var3.setPosition(var2);
         } else {
            var3.setPosition(this.getDatabankPosition(var2));
         }
      }
   }

   private int getDatabankPosition(int var1) {
      return 100 * (var1 + 1);
   }
}
