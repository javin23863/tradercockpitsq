package com.strategyquant.tradinglib.engine.stockpicker.signals.entry;

import it.unimi.dsi.fastutil.Stack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PickerEntrySignals {
   public static final Logger Log = LoggerFactory.getLogger("PickerEntrySignals");
   private ObjectArrayList<PickerEntrySignal> signalsForSymbol = null;
   private Stack<PickerEntrySignal> unusedSignalsStack = null;
   private PriorityQueue<PickerEntrySignal> queue = null;
   private ObjectArrayList<PickerEntrySignal> list = null;

   public void add(String var1, byte var2, double var3, int var5, double var6, double var8, int var10, int var11, byte var12) {
      if (this.signalsForSymbol == null) {
         this.signalsForSymbol = new ObjectArrayList();
      }

      PickerEntrySignal var13 = this.createEntrySignalObject(var1, var2, var3, var5, var6, var8, var10, var11, var12);
      this.signalsForSymbol.add(var13);
   }

   private PickerEntrySignal createEntrySignalObject(String var1, byte var2, double var3, int var5, double var6, double var8, int var10, int var11, byte var12) {
      if (this.unusedSignalsStack != null && !this.unusedSignalsStack.isEmpty()) {
         PickerEntrySignal var13 = (PickerEntrySignal)this.unusedSignalsStack.pop();
         var13.init(var1, var2, var3, var5, var6, var8, var10, var11, var12);
         return var13;
      } else {
         return new PickerEntrySignal(var1, var2, var3, var5, var6, var8, var10, var11, var12);
      }
   }

   public ObjectArrayList<PickerEntrySignal> list() {
      if (this.list == null) {
         this.list = this.createListFromQueue(this.queue);
      }

      return this.list;
   }

   public void toString(StringBuilder var1) {
      ObjectArrayList var2 = this.list();

      for (int var3 = 0; var3 < var2.size(); var3++) {
         PickerEntrySignal var4 = (PickerEntrySignal)var2.get(var3);
         var1.append(var4.toString());
         var1.append(", ");
      }
   }

   @Override
   public String toString() {
      StringBuilder var1 = new StringBuilder();
      this.toString(var1);
      return var1.toString();
   }

   public void consolidateEntrySignals(int var1, Comparator<PickerEntrySignal> var2) {
      if (this.signalsForSymbol != null) {
         if (this.queue == null) {
            this.queue = new PriorityQueue<>(var2);
         }

         this.queue.addAll(this.signalsForSymbol);

         for (int var3 = this.queue.size() - 1; var3 >= var1; var3--) {
            PickerEntrySignal var4 = this.queue.poll();
            this.addToUnusedStack(var4);
         }

         this.signalsForSymbol.clear();
      }
   }

   private ObjectArrayList<PickerEntrySignal> createListFromQueue(PriorityQueue<PickerEntrySignal> var1) {
      ObjectArrayList var2 = new ObjectArrayList();
      if (var1 != null) {
         while (!var1.isEmpty()) {
            PickerEntrySignal var3 = (PickerEntrySignal)var1.poll();
            var2.add(var3);
         }

         var1.addAll(var2);
         Collections.reverse(var2);
      }

      return var2;
   }

   private boolean dontCompare(ObjectArrayList<PickerEntrySignal> var1, ObjectArrayList<PickerEntrySignal> var2) {
      if (var1 == null) {
         return true;
      }

      if (var2 == null) {
         return true;
      }

      if (var1.size() != var2.size()) {
         return true;
      }

      for (int var3 = 0; var3 < var1.size(); var3++) {
         PickerEntrySignal var4 = (PickerEntrySignal)var1.get(var3);
         PickerEntrySignal var5 = (PickerEntrySignal)var2.get(var3);
         if (!var4.symbol.equals(var5.symbol)) {
            return true;
         }
      }

      return false;
   }

   private void addToUnusedStack(PickerEntrySignal var1) {
      if (this.unusedSignalsStack == null) {
         this.unusedSignalsStack = new ObjectArrayList();
      }

      var1.setUnused();
      this.unusedSignalsStack.push(var1);
   }

   public boolean isEmpty() {
      return this.queue == null || this.queue.isEmpty();
   }

   public ObjectArrayList<PickerEntrySignal> getEntrySignals() {
      return this.signalsForSymbol;
   }

   public int size() {
      int var1 = 0;
      if (this.queue != null) {
         var1 += this.queue.size();
      }

      if (this.signalsForSymbol != null) {
         var1 += this.signalsForSymbol.size();
      }

      return var1;
   }
}
