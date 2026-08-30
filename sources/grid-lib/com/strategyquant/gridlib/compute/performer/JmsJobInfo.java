package com.strategyquant.gridlib.compute.performer;

import java.io.Serializable;
import java.util.Map;

public class JmsJobInfo implements Serializable {
   private static final long serialVersionUID = 1L;
   private String className;
   private Map<String, Serializable> params;
   private int flag;

   public JmsJobInfo() {
   }

   public JmsJobInfo(String var1, Map<String, Serializable> var2, int var3) {
      this.className = var1;
      this.params = var2;
      this.flag = var3;
   }

   public String getClassName() {
      return this.className;
   }

   public void setClassName(String var1) {
      this.className = var1;
   }

   public Map<String, Serializable> getParams() {
      return this.params;
   }

   public void setParams(Map<String, Serializable> var1) {
      this.params = var1;
   }

   public int getFlag() {
      return this.flag;
   }

   public void setFlag(int var1) {
      this.flag = var1;
   }
}
