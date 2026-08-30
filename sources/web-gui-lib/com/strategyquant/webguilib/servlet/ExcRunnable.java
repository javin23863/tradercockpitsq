package com.strategyquant.webguilib.servlet;

import java.awt.EventQueue;

public class ExcRunnable<T> implements Runnable {
   public Exception exc;
   public T output;

   @Override
   public void run() {
   }

   public T invoke() throws Exception {
      EventQueue.invokeAndWait(this);
      if (this.exc != null) {
         throw this.exc;
      } else {
         return this.output;
      }
   }
}
