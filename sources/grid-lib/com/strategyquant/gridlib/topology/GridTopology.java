package com.strategyquant.gridlib.topology;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

public class GridTopology implements Serializable {
   private static final long serialVersionUID = 1L;
   private List<NodeInfo> nodes = new LinkedList<>();
   private int totalCores;

   public int getRunningThreads() {
      synchronized (this) {
         int var2 = 0;

         for (NodeInfo var4 : this.nodes) {
            var2 += var4.getRunningThreads();
         }

         return var2;
      }
   }

   public List<NodeInfo> getNodes() {
      synchronized (this) {
         return this.nodes;
      }
   }

   public void setNodes(List<NodeInfo> var1) {
      synchronized (this) {
         this.nodes = var1;
      }
   }

   public int getTotalCores() {
      return this.totalCores;
   }

   public void setTotalCores(int var1) {
      this.totalCores = var1;
   }
}
