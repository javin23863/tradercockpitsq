package com.strategyquant.gridlib.sync.updater;

import com.strategyquant.gridlib.FileHelper;
import com.strategyquant.gridlib.message.JmsConnectionInfo;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jms.JMSException;

public class SingleFolderUpdater extends AbstractFolderUpdater {
   private SingleFolderUpdater.FileStructures hashFileStructure;

   public SingleFolderUpdater(String var1, String var2, JmsConnectionInfo var3) throws IOException, JMSException {
      super(var1, var2, var3);
   }

   @Override
   public void updateHash() throws IOException {
      this.hash = FileHelper.countHash(this.getFolder());
      this.hashFileStructure = new SingleFolderUpdater.FileStructures(this.hash);
   }

   @Override
   public HandleHashResult hashChanged(String var1) {
      SingleFolderUpdater.FileStructures var2 = new SingleFolderUpdater.FileStructures(var1);
      SingleFolderUpdater.FileStructureCompareResult var3 = new SingleFolderUpdater.FileStructureComparator().compare(this.hashFileStructure, var2);
      this.removeUnecessaryFiles(var3.removedFiles);
      return var3.newOrChangedFiles.isEmpty() ? new HandleHashResult(false, null) : new HandleHashResult(true, (LinkedList<String>)var3.newOrChangedFiles);
   }

   private void removeUnecessaryFiles(List<String> var1) {
      for (String var3 : var1) {
         new File(this.getFolder(), var3).delete();
      }
   }

   private static class FileStructureComparator {
      private FileStructureComparator() {
      }

      public SingleFolderUpdater.FileStructureCompareResult compare(SingleFolderUpdater.FileStructures var1, SingleFolderUpdater.FileStructures var2) {
         SingleFolderUpdater.FileStructureCompareResult var3 = new SingleFolderUpdater.FileStructureCompareResult();
         this.evalRemovedOrChangedFiles(var3, var1, var2);
         this.evalAddedFiles(var3, var1, var2);
         return var3;
      }

      private void evalRemovedOrChangedFiles(
         SingleFolderUpdater.FileStructureCompareResult var1, SingleFolderUpdater.FileStructures var2, SingleFolderUpdater.FileStructures var3
      ) {
         for (String var5 : var2.getFileNames()) {
            var5 = var5.trim();
            if (!var3.hasFile(var5)) {
               var1.removedFiles.add(var5);
            } else if (!var3.equalsHash(var5, var2.getHash(var5))) {
               var1.newOrChangedFiles.add(var5);
            }
         }
      }

      private void evalAddedFiles(
         SingleFolderUpdater.FileStructureCompareResult var1, SingleFolderUpdater.FileStructures var2, SingleFolderUpdater.FileStructures var3
      ) {
         for (String var5 : var3.getFileNames()) {
            var5 = var5.trim();
            if (!var2.hasFile(var5)) {
               var1.newOrChangedFiles.add(var5);
            }
         }
      }
   }

   private static class FileStructureCompareResult {
      private List<String> removedFiles = new LinkedList<>();
      private List<String> newOrChangedFiles = new LinkedList<>();

      private FileStructureCompareResult() {
      }
   }

   private static class FileStructures {
      private Map<String, String> structure = new HashMap<>();

      public FileStructures(String var1) {
         this.parse(var1);
      }

      private void parse(String var1) {
         String[] var2 = var1.split("\n");

         for (String var6 : var2) {
            if (!var6.isEmpty()) {
               int var7 = var6.indexOf(32);
               String var8 = var6.substring(0, var7 - 1);
               String var9 = var6.substring(var7 + 1);
               this.structure.put(var9, var8);
            }
         }
      }

      public boolean equalsHash(String var1, String var2) {
         String var3 = this.structure.get(var1);
         return var2.equals(var3);
      }

      public boolean hasFile(String var1) {
         return this.structure.containsKey(var1);
      }

      public Set<String> getFileNames() {
         return this.structure.keySet();
      }

      public String getHash(String var1) {
         return this.structure.get(var1);
      }
   }
}
