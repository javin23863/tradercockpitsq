package com.strategyquant.tradinglib.engine.stockpicker.signals;

import com.strategyquant.tradinglib.OrderTypes;
import com.strategyquant.tradinglib.engine.stockpicker.signals.entry.PickerEntrySignal;
import com.strategyquant.tradinglib.engine.stockpicker.signals.entry.PickerEntrySignals;
import com.strategyquant.tradinglib.engine.stockpicker.signals.exit.PickerExitSignal;
import com.strategyquant.tradinglib.engine.stockpicker.signals.exit.PickerExitSignals;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import java.util.Comparator;

public class Signals {
   private PickerEntrySignals entryLong = null;
   private PickerEntrySignals entryShort = null;
   public PickerExitSignals exit = null;

   public boolean longEntrySignalExists() {
      return this.entryLong != null && !this.entryLong.isEmpty();
   }

   public boolean shortEntrySignalExists() {
      return this.entryShort != null && !this.entryShort.isEmpty();
   }

   public PickerEntrySignals getLongEntrySignals() {
      return this.entryLong;
   }

   public PickerEntrySignals getShortEntrySignals() {
      return this.entryShort;
   }

   public ObjectArrayList<PickerEntrySignal> getLongEntrySignals(int var1) {
      return this.entryLong == null ? null : this.entryLong.getEntrySignals();
   }

   public ObjectArrayList<PickerEntrySignal> getShortEntrySignals(int var1) {
      return this.entryShort == null ? null : this.entryShort.getEntrySignals();
   }

   public String printLongEntrySignals() {
      return this.entryLong == null ? "" : this.entryLong.toString();
   }

   public String printShortEntrySignals() {
      return this.entryShort == null ? "" : this.entryShort.toString();
   }

   public String printExitSignals() {
      return this.exit == null ? "" : this.exit.toString();
   }

   public boolean exitSignalExists() {
      return this.exit != null && !this.exit.isEmpty();
   }

   public PickerExitSignals getExitSignals() {
      return this.exit;
   }

   public void addEntrySignal(int var1, String var2, byte var3, double var4, int var6, double var7, double var9, int var11, int var12, byte var13) {
      if (OrderTypes.isLongOrder(var3)) {
         if (this.entryLong == null) {
            this.entryLong = new PickerEntrySignals();
         }

         this.entryLong.add(var2, var3, var4, var6, var7, var9, var11, var12, var13);
      } else {
         if (this.entryShort == null) {
            this.entryShort = new PickerEntrySignals();
         }

         this.entryShort.add(var2, var3, var4, var6, var7, var9, var11, var12, var13);
      }
   }

   public void addExitSignal(PickerExitSignal var1) {
      if (this.exit == null) {
         this.exit = new PickerExitSignals();
      }

      this.exit.add(var1);
   }

   @Override
   public String toString() {
      return String.format(
         "entrySigLong=%d, entrySigShort=%d, exitsig=%d",
         this.entryLong == null ? 0 : this.entryLong.size(),
         this.entryShort == null ? 0 : this.entryShort.size(),
         this.exit == null ? 0 : this.exit.size()
      );
   }

   public void consolidateEntryExitSignals(int var1, int var2, Comparator<PickerEntrySignal> var3) {
      if (this.entryLong != null) {
         this.entryLong.consolidateEntrySignals(var1, var3);
      }

      if (this.entryShort != null) {
         this.entryShort.consolidateEntrySignals(var2, var3);
      }
   }
}
