package com.strategyquant.tradinglib;

import java.io.Serializable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StatsTypeCombination implements Serializable {
   public static final Logger Log = LoggerFactory.getLogger("StatsTypeCombination");
   private byte direction = 0;
   private byte plType = 10;
   private byte sampleType = 127;
   private int key;

   public StatsTypeCombination(byte var1, byte var2, byte var3) {
      this.direction = var1;
      this.plType = var2;
      this.sampleType = var3;
      this.key = createKey(var1, var2, var3);
   }

   public StatsTypeCombination set(byte var1, byte var2, byte var3) {
      this.direction = var1;
      this.plType = var2;
      this.sampleType = var3;
      this.key = createKey(var1, var2, var3);
      return this;
   }

   public static int createKey(byte var0, byte var1, byte var2) {
      int var3 = var0 << 16 | var1 << 8 | var2;
      if (var3 < 0) {
         var3 *= -1;
      }

      return var3;
   }

   public byte getDirection() {
      return this.direction;
   }

   public byte getPLType() {
      return this.plType;
   }

   public byte getSampleType() {
      return this.sampleType;
   }

   public int getKey() {
      return this.key;
   }

   public String getKeyAsStr() {
      return "Dir: "
         + Directions.typeToString(this.direction)
         + ", PL: "
         + PlTypes.typeToString(this.plType)
         + ", Sample: "
         + SampleTypes.typeToString(this.sampleType);
   }

   @Override
   public String toString() {
      return Integer.toString(this.key);
   }

   public boolean isMainOne() {
      return this.direction == 0 && this.plType == 10 && this.sampleType == 127;
   }
}
