package com.strategyquant.tradinglib.engine.stockpicker.signals.exit;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;

public class PickerExitSignals extends ObjectArrayList<PickerExitSignal> {
   public String toString() {
      StringBuilder var1 = new StringBuilder();
      this.toString(var1);
      return var1.toString();
   }

   public void toString(StringBuilder var1) {
      ObjectListIterator var2 = this.iterator();

      while (var2.hasNext()) {
         PickerExitSignal var3 = (PickerExitSignal)var2.next();
         var3.toString(var1);
         var1.append(", ");
      }
   }

   public void reduceCount(IntOpenHashSet var1) {
      for (int var3 = this.size() - 1; var3 >= 0; var3--) {
         int var2 = ((PickerExitSignal)this.get(var3)).symbol.hashCode();
         if (!var1.contains(var2)) {
            this.remove(var3);
         }
      }
   }
}
