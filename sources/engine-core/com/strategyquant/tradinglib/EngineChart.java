package com.strategyquant.tradinglib;

import com.strategyquant.tradinglib.debug.Debugger;
import com.strategyquant.tradinglib.project.SQProject;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class EngineChart extends Debugger {
   public static final Logger Log = LoggerFactory.getLogger(EngineChart.class);
   public static final String DATA_TYPE_CHART = "chart";
   public static final String DATA_TYPE_GRID = "grid";
   public static final String DATA_TYPE_ROWS = "rows";
   public static final int TYPE_GLOBAL = 10;
   public static final int TYPE_PROJECT_BASED = 50;
   protected final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
   protected static final int MEGABYTE = 1048576;
   protected static final int GIGABYTE = 1073741824;
   protected float maxOffHeapMemory;
   protected float maxHeapMemory;
   protected SQProject project;
   public String name = "?";
   public int type;
   private String className = null;

   @Override
   public String toString() {
      return this.name;
   }

   public int getType() {
      return this.type;
   }

   public EngineChart(String var1, int var2) {
      this.name = var1;
      this.type = var2;
      this.maxOffHeapMemory = (float)this.memoryBean.getNonHeapMemoryUsage().getMax() / 1048576.0F;
      this.maxHeapMemory = (float)this.memoryBean.getHeapMemoryUsage().getMax() / 1048576.0F;
   }

   public void setProject(SQProject var1) {
      this.project = var1;
   }

   public abstract void addNextValue();

   public abstract JSONObject print();

   public String getClassName() {
      if (this.className == null) {
         this.className = this.getClass().getSimpleName();
      }

      return this.className;
   }
}
