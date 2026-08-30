package com.strategyquant.datalib.ticksimulator;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.data.io.VersatileData;
import java.security.InvalidParameterException;

public class TSTickSimulator implements ITickSimulator {
   private VersatileData data = new VersatileData();
   private int step = -1;
   private SpreadsMap spreadsMap;
   private int lastSpreadHash = -1;
   private double lastSpread;
   private double avgTickVolume;
   private double volume;
   private boolean lowFirst;
   private double spread;

   @Override
   public void init(VersatileData var1) {
      this.data.copyFrom(var1);
      this.step = 0;
      double var2 = Math.abs(this.data.open - this.data.low);
      double var4 = Math.abs(this.data.open - this.data.high);
      if (var2 <= var4) {
         this.lowFirst = true;
      } else {
         this.lowFirst = false;
      }

      this.avgTickVolume = this.data.volume / 4.0;
      this.volume = this.avgTickVolume;
      if (var1.spread == -1.0) {
         this.spread = -1.0;
      } else {
         this.spread = this.getSpread(this.data.connectionHash, this.data.symbolHash);
         this.spread = this.spread >= 0.0 ? this.spread : 0.0;
      }
   }

   @Override
   public boolean getNextTick(TickEvent var1) {
      if (this.step < 0) {
         return false;
      }

      if (this.data.type == 1 && this.step > 0) {
         return false;
      }

      if (this.data.type == 2 && this.step > 3) {
         return false;
      }

      if (this.data.type == 1) {
         double var2 = this.spread == -1.0 ? this.data.ask : this.data.bid + this.spread;
         var1.set(
            this.data.time, this.data.connectionHash, this.data.symbolHash, this.data.bid, var2, this.data.volume, this.data.sessionStartTime, false, false
         );
         this.step++;
         return true;
      }

      if (this.data.type == 2) {
         if (this.step == 3) {
            this.volume = this.data.volume - 3.0 * this.avgTickVolume;
         }

         switch (this.step) {
            case 0:
               var1.set(
                  this.data.time,
                  this.data.connectionHash,
                  this.data.symbolHash,
                  this.data.open,
                  this.data.open + this.spread,
                  this.volume,
                  this.data.sessionStartTime,
                  true,
                  false
               );
               this.step++;
               return true;
            case 1:
               if (this.lowFirst) {
                  var1.set(
                     this.data.time,
                     this.data.connectionHash,
                     this.data.symbolHash,
                     this.data.low,
                     this.data.low + this.spread,
                     this.volume,
                     this.data.sessionStartTime,
                     false,
                     false
                  );
               } else {
                  var1.set(
                     this.data.time,
                     this.data.connectionHash,
                     this.data.symbolHash,
                     this.data.high,
                     this.data.high + this.spread,
                     this.volume,
                     this.data.sessionStartTime,
                     false,
                     false
                  );
               }

               this.step++;
               return true;
            case 2:
               if (this.lowFirst) {
                  var1.set(
                     this.data.time,
                     this.data.connectionHash,
                     this.data.symbolHash,
                     this.data.high,
                     this.data.high + this.spread,
                     this.volume,
                     this.data.sessionStartTime,
                     false,
                     false
                  );
               } else {
                  var1.set(
                     this.data.time,
                     this.data.connectionHash,
                     this.data.symbolHash,
                     this.data.low,
                     this.data.low + this.spread,
                     this.volume,
                     this.data.sessionStartTime,
                     false,
                     false
                  );
               }

               this.step++;
               return true;
            case 3:
               var1.set(
                  this.data.time,
                  this.data.connectionHash,
                  this.data.symbolHash,
                  this.data.close,
                  this.data.close + this.spread,
                  this.volume,
                  this.data.sessionStartTime,
                  false,
                  true
               );
               this.step++;
               return true;
         }
      }

      throw new InvalidParameterException("Shouldnt get here");
   }

   private double getSpread(int var1, int var2) {
      int var3 = var1 + var2;
      if (var3 == this.lastSpreadHash) {
         return this.lastSpread;
      } else if (this.spreadsMap != null && this.spreadsMap.containsKey(var3)) {
         double var4 = this.spreadsMap.getSpread(var3);
         this.lastSpreadHash = var3;
         this.lastSpread = var4;
         return var4;
      } else {
         return 0.0;
      }
   }

   @Override
   public void setSymbolsSpread(SpreadsMap var1) {
      this.spreadsMap = var1;
   }
}
