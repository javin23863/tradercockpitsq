package com.strategyquant.jobslib;

import java.util.ArrayList;

public class JobsList {
   private ArrayList<SQJob> list = new ArrayList<>();

   public void add(SQJob var1) {
      this.list.add(var1);
   }

   public void remove(SQJob var1) {
      this.list.remove(var1);
   }

   public int size() {
      return this.list.size();
   }
}
