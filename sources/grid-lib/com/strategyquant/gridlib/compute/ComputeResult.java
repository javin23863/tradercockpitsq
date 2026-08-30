package com.strategyquant.gridlib.compute;

import java.io.Serializable;

public class ComputeResult<T extends Serializable> implements Serializable {
   private static final long serialVersionUID = 1L;
   private T data;
   private String taskId;

   public ComputeResult() {
   }

   public ComputeResult(T var1, String var2) {
      this.data = (T)var1;
      this.taskId = var2;
   }

   public T getData() {
      return this.data;
   }

   public void setData(T var1) {
      this.data = (T)var1;
   }

   public String getTaskId() {
      return this.taskId;
   }

   public void setTaskId(String var1) {
      this.taskId = var1;
   }
}
