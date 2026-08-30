package com.strategyquant.tradinglib.wfo;

public class SimulationOrdersList {
   public long[] openTime;
   public long[] closeTime;
   public byte[] type;
   public byte[] closeType;
   public float[] pL;
   public float[] size;
   public float[] openPrice;
   public float[] closePrice;
   public short[] barsInTrade;
   public String[] symbol;
   public float[] mae;
   public float[] mfe;
   public float[] commswap;
   public float[] stoploss;
   private int index = 0;
   private double checksum = 0.0;

   public SimulationOrdersList(int var1) {
      this.openTime = new long[var1];
      this.closeTime = new long[var1];
      this.type = new byte[var1];
      this.closeType = new byte[var1];
      this.pL = new float[var1];
      this.size = new float[var1];
      this.openPrice = new float[var1];
      this.closePrice = new float[var1];
      this.barsInTrade = new short[var1];
      this.symbol = new String[var1];
      this.mae = new float[var1];
      this.mfe = new float[var1];
      this.commswap = new float[var1];
      this.stoploss = new float[var1];
   }

   public void add(
      long var1,
      long var3,
      byte var5,
      byte var6,
      float var7,
      float var8,
      float var9,
      float var10,
      short var11,
      String var12,
      float var13,
      float var14,
      float var15,
      float var16
   ) {
      this.openTime[this.index] = var1;
      this.closeTime[this.index] = var3;
      this.type[this.index] = var5;
      this.closeType[this.index] = var6;
      this.pL[this.index] = var7;
      this.size[this.index] = var8;
      this.openPrice[this.index] = var9;
      this.closePrice[this.index] = var10;
      this.barsInTrade[this.index] = var11;
      this.symbol[this.index] = var12;
      this.mae[this.index] = var13;
      this.mfe[this.index] = var14;
      this.commswap[this.index] = var15;
      this.stoploss[this.index] = var16;
      this.index++;
      this.checksum = this.checksum
         + ((float)(var1 + var3 + var5 + var6) + var7 + var8 + var9 + var10 + var11 + var12.hashCode() + var13 + var14 + var15 + var16);
   }

   public int getHash() {
      return Double.hashCode(this.checksum);
   }
}
