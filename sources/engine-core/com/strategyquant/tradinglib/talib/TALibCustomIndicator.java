package com.strategyquant.tradinglib.talib;

import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.debug.Debugger;

public abstract class TALibCustomIndicator extends Debugger {
   protected SettingsMap params;
   protected StrategyBase Strategy;
   protected float[] o;
   protected float[] h;
   protected float[] l;
   protected float[] c;
   protected float[] v;
   protected float[] in0;
   protected float[] in1;

   public abstract float[][] calculate();

   protected void init(SettingsMap var1, StrategyBase var2, float[] var3, float[] var4, float[] var5, float[] var6, float[] var7, float[] var8, float[] var9) {
      this.params = var1;
      this.Strategy = var2;
      this.o = var3;
      this.h = var4;
      this.l = var5;
      this.c = var6;
      this.v = var7;
      this.in0 = var8;
      this.in1 = var9;
   }
}
