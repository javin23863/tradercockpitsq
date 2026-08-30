package com.strategyquant.tradinglib.talib;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SettingsMap;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.engine.stockpicker.backtester.BacktestData;
import com.strategyquant.tradinglib.engine.stockpicker.data.symbols.LoadedSymbolData;
import com.strategyquant.tradinglib.engine.stockpicker.data.utils.PickerDataUtils;
import com.tictactec.ta.lib.Core;
import com.tictactec.ta.lib.MAType;
import com.tictactec.ta.lib.MInteger;
import com.tictactec.ta.lib.RetCode;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TALibIndicators {
   private static final Logger Log = LoggerFactory.getLogger(TALibIndicators.class);
   private static final String PERIOD_IDENTIFIER = "period";
   private static final Set<String> HLC_INDICATORS = Set.of(
      "AD",
      "ADX",
      "DX",
      "ADOSC",
      "ADXR",
      "BOP",
      "CDL3BLACKCROWS",
      "CCI",
      "CDL2CROWS",
      "ATR",
      "AROON",
      "AVGPRICE",
      "AROONOSC",
      "CDLRISEFALL3METHODS",
      "CDLXSIDEGAP3METHODS",
      "CDLGAPSIDESIDEWHITE",
      "CDLCONCEALBABYSWALL",
      "CDLDOJI",
      "CDLDRAGONFLYDOJI",
      "CDLEVENINGSTAR",
      "CDLDOJISTAR",
      "CDLGRAVESTONEDOJI",
      "CDLENGULFING",
      "CDLCOUNTERATTACK",
      "CDLEVENINGDOJISTAR",
      "CDLHAMMER",
      "CDLHANGINGMAN",
      "CDLHARAMI",
      "CDLDARKCLOUDCOVER",
      "CDL3WHITESOLDIERS",
      "CDL3OUTSIDE",
      "CDLBREAKAWAY",
      "CDL3LINESTRIKE",
      "CDL3STARSINSOUTH",
      "CDL3INSIDE",
      "CDLABANDONEDBABY",
      "CDLCLOSINGMARUBOZU",
      "CDLBELTHOLD",
      "CDLADVANCEBLOCK",
      "CDLMATCHINGLOW",
      "CDLLONGLINE",
      "CDLMORNINGDOJISTAR",
      "CDLONNECK",
      "CDLPIERCING",
      "CDLMATHOLD",
      "CDLMORNINGSTAR",
      "CDLSEPARATINGLINES",
      "CDLLONGLEGGEDDOJI",
      "CDLMARUBOZU",
      "CDLRICKSHAWMAN",
      "CDLSHOOTINGSTAR",
      "CDLHARAMICROSS",
      "CDLLADDERBOTTOM",
      "CDLHIGHWAVE",
      "CDLHIKKAKEMOD",
      "CDLKICKING",
      "CDLKICKINGBYLENGTH",
      "CDLINNECK",
      "CDLIDENTICAL3CROWS",
      "CDLHIKKAKE",
      "CDLINVERTEDHAMMER",
      "CDLHOMINGPIGEON",
      "CDLTHRUSTING",
      "CDLTAKURI",
      "CDLSTICKSANDWICH",
      "CDLTASUKIGAP",
      "CDLSTALLEDPATTERN",
      "CDLSPINNINGTOP",
      "CDLUPSIDEGAP2CROWS",
      "CDLTRISTAR",
      "CDLUNIQUE3RIVER",
      "CDLSHORTLINE",
      "STOCHF",
      "STOCH",
      "MINUS_DM",
      "MIDPRICE",
      "MEDPRICE",
      "MFI",
      "MINUS_DI",
      "NATR",
      "PLUS_DI",
      "PLUS_DM",
      "SAR",
      "SAREXT",
      "TRANGE",
      "TYPPRICE",
      "ULTOSC",
      "WCLPRICE",
      "WILLR"
   );
   public static final int ValueNotDefined = -1;
   private Core core = new Core();
   public float[] o = null;
   public float[] h;
   public float[] l;
   public float[] c;
   public float[] v;
   public float[] m;
   public float[] t;
   public float[] w;
   private TALibIndicatorsCache cache = new TALibIndicatorsCache();

   public static boolean isHlcIndicator(String var0) {
      return HLC_INDICATORS.contains(var0);
   }

   public static boolean isPeriod(String var0) {
      return var0.toLowerCase().contains("period");
   }

   public Long2ObjectOpenHashMap<Object[]> getIndicatorHashes() {
      return this.cache.getIndicatorHashes();
   }

   public float[][] getIndicatorValue(long var1) {
      return this.cache.get(var1);
   }

   public float[][] calculate(String var1, SettingsMap var2, StrategyBase var3, int var4, int var5, String var6) throws TradingException {
      String var7 = this.getSymbol(var3, var2);
      long var8 = this.cache.getHash(var7, var1, var2);
      if (!this.cache.containsKey(var8)) {
         int var10 = var2.getInt("Chart");
         int var11 = var2.getInt("ComputedFrom");
         this.loadOHLC(var3, var10, var7, var11);
         this.calculate(var7, var1, var2, var3, var10, var4, var11, var5, var6);
      }

      return this.cache.get(var8);
   }

   public float[][] calculate(String var1, String var2, SettingsMap var3, StrategyBase var4, int var5, int var6, int var7, int var8, String var9) throws TradingException {
      long var10 = this.cache.getHash(var1, var2, var3);
      if (!this.cache.containsKey(var10)) {
         this.loadOHLC(var4, var5, var1, var7);
         this.calculate(var10, var7, var2, var1, var3, var4, var6, var8, var9);
      }

      return this.cache.get(var10);
   }

   private String getSymbol(StrategyBase var1, SettingsMap var2) {
      if (var1 == null) {
         return "NA";
      }

      int var3 = var2.getInt("Chart");
      BacktestData var4 = var1.Stockpicker.data;
      return var4.getSymbol(var3);
   }

   private MAType getMAType(int var1) {
      MAType[] var2 = MAType.values();
      return var2[var1];
   }

   private void calculate(long var1, int var3, String var4, String var5, SettingsMap var6, StrategyBase var7, int var8, int var9, String var10) throws TradingException {
      float[][] var11 = null;
      float[] var12 = this.computedFrom(var7, var6, var3, var8);
      float[] var13 = var12;
      double[][] var14 = null;
      boolean[] var15 = null;
      int[][] var16 = null;
      MInteger var17 = new MInteger();
      MInteger var18 = new MInteger();
      byte var19 = 0;
      int var20 = this.o.length - 1;
      RetCode var21 = null;
      switch (var4) {
         case "ADD":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.add(var19, var20, var12, var13, var17, var18, var14[0]);
            break;
         case "SIN":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.sin(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "COS":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.cos(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "TAN":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.tan(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "SQRT":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.sqrt(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "LOG10":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.log10(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "EXP":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.exp(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "MIN":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.min(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "MAX":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.max(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "FLOOR":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.floor(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "CEIL":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.ceil(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "AD":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.ad(var19, var20, this.h, this.l, this.c, this.v, var17, var18, var14[0]);
            break;
         case "SUM":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.sum(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "ASIN":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.asin(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "ACOS":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.acos(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "ATAN":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.atan(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "SINH":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.sinh(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "COSH":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.cosh(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "TANH":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.tanh(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "LN":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.ln(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "SUB":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.sub(var19, var20, var12, var13, var17, var18, var14[0]);
            break;
         case "T3":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.t3(var19, var20, var12, var6.getInt("TimePeriod"), var6.getDouble("VFactor"), var17, var18, var14[0]);
            break;
         case "MULT":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.mult(var19, var20, var12, var13, var17, var18, var14[0]);
            break;
         case "DIV":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.div(var19, var20, var12, var13, var17, var18, var14[0]);
            break;
         case "EMA":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.ema(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "TSF":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.tsf(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "ADX":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.adx(var19, var20, this.h, this.l, this.c, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "MOM":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.mom(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "SMA":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.sma(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "DX":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.dx(var19, var20, this.h, this.l, this.c, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "ADOSC":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core
               .adOsc(
                  var19,
                  var20,
                  this.h,
                  this.l,
                  this.c,
                  this.v,
                  this.getMin(var6.getInt("FastPeriod"), var6.getInt("SlowPeriod")),
                  this.getMax(var6.getInt("SlowPeriod"), var6.getInt("FastPeriod")),
                  var17,
                  var18,
                  var14[0]
               );
            break;
         case "ADXR":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.adxr(var19, var20, this.h, this.l, this.c, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "APO":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core
               .apo(
                  var19,
                  var20,
                  var12,
                  this.getMin(var6.getInt("FastPeriod"), var6.getInt("SlowPeriod")),
                  this.getMax(var6.getInt("SlowPeriod"), var6.getInt("FastPeriod")),
                  this.getMAType(var6.getInt("MAType")),
                  var17,
                  var18,
                  var14[0]
               );
            break;
         case "AROONOSC":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.aroonOsc(var19, var20, this.h, this.l, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "AROON":
            var14 = new double[2][this.o.length];
            var15 = new boolean[]{true, true};
            var21 = this.core.aroon(var19, var20, this.h, this.l, var6.getInt("TimePeriod"), var17, var18, var14[0], var14[1]);
            break;
         case "ATR":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.atr(var19, var20, this.h, this.l, this.c, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "AVGPRICE":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.avgPrice(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var14[0]);
            break;
         case "BBANDS":
            var14 = new double[3][this.o.length];
            var15 = new boolean[]{true, true, true};
            var21 = this.core
               .bbands(
                  var19,
                  var20,
                  var12,
                  var6.getInt("TimePeriod"),
                  var6.getDouble("NbDevUp"),
                  var6.getDouble("NbDevDn"),
                  this.getMAType(var6.getInt("MAType")),
                  var17,
                  var18,
                  var14[0],
                  var14[1],
                  var14[2]
               );
            break;
         case "CCI":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.cci(var19, var20, this.h, this.l, this.c, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "BETA":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.beta(var19, var20, var12, var13, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "BOP":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.bop(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var14[0]);
            break;
         case "CDLCONCEALBABYSWALL":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlConcealBabysWall(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLGAPSIDESIDEWHITE":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlGapSideSideWhite(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLADVANCEBLOCK":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlAdvanceBlock(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLBELTHOLD":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlBeltHold(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDL3LINESTRIKE":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdl3LineStrike(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDL2CROWS":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdl2Crows(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDL3WHITESOLDIERS":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdl3WhiteSoldiers(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDL3BLACKCROWS":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdl3BlackCrows(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDL3INSIDE":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdl3Inside(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDL3STARSINSOUTH":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdl3StarsInSouth(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLABANDONEDBABY":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlAbandonedBaby(var19, var20, this.o, this.h, this.l, this.c, var6.getDouble("Penetration"), var17, var18, var16[0]);
            break;
         case "CDL3OUTSIDE":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdl3Outside(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLENGULFING":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlEngulfing(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLCOUNTERATTACK":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlCounterAttack(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLDOJI":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlDoji(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLEVENINGDOJISTAR":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlEveningDojiStar(var19, var20, this.o, this.h, this.l, this.c, var6.getDouble("Penetration"), var17, var18, var16[0]);
            break;
         case "CDLCLOSINGMARUBOZU":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlClosingMarubozu(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLBREAKAWAY":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlBreakaway(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLDOJISTAR":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlDojiStar(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLEVENINGSTAR":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlEveningStar(var19, var20, this.o, this.h, this.l, this.c, var6.getDouble("Penetration"), var17, var18, var16[0]);
            break;
         case "CDLDARKCLOUDCOVER":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlDarkCloudCover(var19, var20, this.o, this.h, this.l, this.c, var6.getDouble("Penetration"), var17, var18, var16[0]);
            break;
         case "CDLDRAGONFLYDOJI":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlDragonflyDoji(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLHIGHWAVE":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlHignWave(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLHIKKAKEMOD":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlHikkakeMod(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLHAMMER":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlHammer(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLHANGINGMAN":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlHangingMan(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLHIKKAKE":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlHikkake(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLIDENTICAL3CROWS":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlIdentical3Crows(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLINVERTEDHAMMER":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlInvertedHammer(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLINNECK":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlInNeck(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLHARAMI":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlHarami(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLHOMINGPIGEON":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlHomingPigeon(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLGRAVESTONEDOJI":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlGravestoneDoji(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLHARAMICROSS":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlHaramiCross(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLRISEFALL3METHODS":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlRiseFall3Methods(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLMORNINGSTAR":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlMorningStar(var19, var20, this.o, this.h, this.l, this.c, var6.getDouble("Penetration"), var17, var18, var16[0]);
            break;
         case "CDLLONGLINE":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlLongLine(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLLADDERBOTTOM":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlLadderBottom(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLLONGLEGGEDDOJI":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlLongLeggedDoji(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLMATHOLD":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlMatHold(var19, var20, this.o, this.h, this.l, this.c, var6.getDouble("Penetration"), var17, var18, var16[0]);
            break;
         case "CDLONNECK":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlOnNeck(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLMARUBOZU":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlMarubozu(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLMORNINGDOJISTAR":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlMorningDojiStar(var19, var20, this.o, this.h, this.l, this.c, var6.getDouble("Penetration"), var17, var18, var16[0]);
            break;
         case "CDLKICKINGBYLENGTH":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlKickingByLength(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLMATCHINGLOW":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlMatchingLow(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLKICKING":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlKicking(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLSHOOTINGSTAR":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlShootingStar(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLSHORTLINE":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlShortLine(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLSEPARATINGLINES":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlSeperatingLines(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLSPINNINGTOP":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlSpinningTop(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLTASUKIGAP":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlTasukiGap(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLSTALLEDPATTERN":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlStalledPattern(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLRICKSHAWMAN":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlRickshawMan(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLSTICKSANDWICH":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlStickSandwhich(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLTRISTAR":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlTristar(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLTAKURI":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlTakuri(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLTHRUSTING":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlThrusting(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLPIERCING":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlPiercing(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLXSIDEGAP3METHODS":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlXSideGap3Methods(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLUNIQUE3RIVER":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlUnique3River(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "CDLUPSIDEGAP2CROWS":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.cdlUpsideGap2Crows(var19, var20, this.o, this.h, this.l, this.c, var17, var18, var16[0]);
            break;
         case "HT_DCPHASE":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.htDcPhase(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "DEMA":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.dema(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "CMO":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.cmo(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "HT_DCPERIOD":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.htDcPeriod(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "CORREL":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.correl(var19, var20, var12, var13, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "HT_TRENDMODE":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.htTrendMode(var19, var20, var12, var17, var18, var16[0]);
            break;
         case "LINEARREG_ANGLE":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.linearRegAngle(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "LINEARREG":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.linearReg(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "LINEARREG_SLOPE":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.linearRegSlope(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "HT_TRENDLINE":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.htTrendline(var19, var20, var12, var17, var18, var14[0]);
            break;
         case "HT_SINE":
            var14 = new double[2][this.o.length];
            var15 = new boolean[]{true, true};
            var21 = this.core.htSine(var19, var20, var12, var17, var18, var14[0], var14[1]);
            break;
         case "HT_PHASOR":
            var14 = new double[2][this.o.length];
            var15 = new boolean[]{true, true};
            var21 = this.core.htPhasor(var19, var20, var12, var17, var18, var14[0], var14[1]);
            break;
         case "KAMA":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.kama(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "LINEARREG_INTERCEPT":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.linearRegIntercept(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "MIDPRICE":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.midPrice(var19, var20, this.h, this.l, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "MEDPRICE":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.medPrice(var19, var20, this.h, this.l, var17, var18, var14[0]);
            break;
         case "MAXINDEX":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.maxIndex(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var16[0]);
            break;
         case "MIDPOINT":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.midPoint(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "MININDEX":
            var16 = new int[1][this.o.length];
            var15 = new boolean[]{false};
            var21 = this.core.minIndex(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var16[0]);
            break;
         case "MFI":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.mfi(var19, var20, this.h, this.l, this.c, this.v, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "MINMAX":
            var14 = new double[2][this.o.length];
            var15 = new boolean[]{true, true};
            var21 = this.core.minMax(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0], var14[1]);
            break;
         case "MACDFIX":
            var14 = new double[3][this.o.length];
            var15 = new boolean[]{true, true, true};
            var21 = this.core.macdFix(var19, var20, var12, var6.getInt("SignalPeriod"), var17, var18, var14[0], var14[1], var14[2]);
            break;
         case "MACDEXT":
            var14 = new double[3][this.o.length];
            var15 = new boolean[]{true, true, true};
            var21 = this.core
               .macdExt(
                  var19,
                  var20,
                  var12,
                  this.getMin(var6.getInt("FastPeriod"), var6.getInt("SlowPeriod")),
                  this.getMAType(var6.getInt("FastMAType")),
                  this.getMax(var6.getInt("SlowPeriod"), var6.getInt("FastPeriod")),
                  this.getMAType(var6.getInt("SlowMAType")),
                  var6.getInt("SignalPeriod"),
                  this.getMAType(var6.getInt("SignalMAType")),
                  var17,
                  var18,
                  var14[0],
                  var14[1],
                  var14[2]
               );
            break;
         case "MACD":
            var14 = new double[3][this.o.length];
            var15 = new boolean[]{true, true, true};
            var21 = this.core
               .macd(
                  var19,
                  var20,
                  var12,
                  this.getMin(var6.getInt("FastPeriod"), var6.getInt("SlowPeriod")),
                  this.getMax(var6.getInt("SlowPeriod"), var6.getInt("FastPeriod")),
                  var6.getInt("SignalPeriod"),
                  var17,
                  var18,
                  var14[0],
                  var14[1],
                  var14[2]
               );
            break;
         case "MA":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.movingAverage(var19, var20, var12, var6.getInt("TimePeriod"), this.getMAType(var6.getInt("MAType")), var17, var18, var14[0]);
            break;
         case "MAMA":
            var14 = new double[2][this.o.length];
            var15 = new boolean[]{true, true};
            var21 = this.core.mama(var19, var20, var12, var6.getDouble("FastLimit"), var6.getDouble("SlowLimit"), var17, var18, var14[0], var14[1]);
            break;
         case "MAVP":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core
               .movingAverageVariablePeriod(
                  var19,
                  var20,
                  var12,
                  this.o,
                  this.getMin(var6.getInt("MinPeriod"), var6.getInt("MaxPeriod")),
                  this.getMax(var6.getInt("MaxPeriod"), var6.getInt("MinPeriod")),
                  this.getMAType(var6.getInt("MAType")),
                  var17,
                  var18,
                  var14[0]
               );
            break;
         case "MINMAXINDEX":
            var16 = new int[2][this.o.length];
            var15 = new boolean[]{false, false};
            var21 = this.core.minMaxIndex(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var16[0], var16[1]);
            break;
         case "OBV":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.obv(var19, var20, var12, this.v, var17, var18, var14[0]);
            break;
         case "MINUS_DM":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.minusDM(var19, var20, this.h, this.l, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "PLUS_DI":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.plusDI(var19, var20, this.h, this.l, this.c, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "NATR":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.natr(var19, var20, this.h, this.l, this.c, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "MINUS_DI":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.minusDI(var19, var20, this.h, this.l, this.c, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "PLUS_DM":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.plusDM(var19, var20, this.h, this.l, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "PPO":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core
               .ppo(
                  var19,
                  var20,
                  var12,
                  this.getMin(var6.getInt("FastPeriod"), var6.getInt("SlowPeriod")),
                  this.getMax(var6.getInt("SlowPeriod"), var6.getInt("FastPeriod")),
                  this.getMAType(var6.getInt("MAType")),
                  var17,
                  var18,
                  var14[0]
               );
            break;
         case "ROCP":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.rocP(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "SAR":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.sar(var19, var20, this.h, this.l, var6.getDouble("Acceleration"), var6.getDouble("Maximum"), var17, var18, var14[0]);
            break;
         case "SAREXT":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core
               .sarExt(
                  var19,
                  var20,
                  this.h,
                  this.l,
                  var6.getDouble("StartValue"),
                  var6.getDouble("OffsetOnReverse"),
                  var6.getDouble("AccelerationInitLong"),
                  var6.getDouble("AccelerationLong"),
                  var6.getDouble("AccelerationMaxLong"),
                  var6.getDouble("AccelerationInitShort"),
                  var6.getDouble("AccelerationShort"),
                  var6.getDouble("AccelerationMaxShort"),
                  var17,
                  var18,
                  var14[0]
               );
            break;
         case "ROC":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.roc(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "ROCR100":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.rocR100(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "RSI":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.rsi(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "ROCR":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.rocR(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "STOCHRSI":
            var14 = new double[2][this.o.length];
            var15 = new boolean[]{true, true};
            var21 = this.core
               .stochRsi(
                  var19,
                  var20,
                  var12,
                  var6.getInt("TimePeriod"),
                  var6.getInt("FastK_Period"),
                  var6.getInt("FastD_Period"),
                  this.getMAType(var6.getInt("FastD_MAType")),
                  var17,
                  var18,
                  var14[0],
                  var14[1]
               );
            break;
         case "STOCHF":
            var14 = new double[2][this.o.length];
            var15 = new boolean[]{true, true};
            var21 = this.core
               .stochF(
                  var19,
                  var20,
                  this.h,
                  this.l,
                  this.c,
                  var6.getInt("FastK_Period"),
                  var6.getInt("FastD_Period"),
                  this.getMAType(var6.getInt("FastD_MAType")),
                  var17,
                  var18,
                  var14[0],
                  var14[1]
               );
            break;
         case "TEMA":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.tema(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "TRANGE":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.trueRange(var19, var20, this.h, this.l, this.c, var17, var18, var14[0]);
            break;
         case "STOCH":
            var14 = new double[2][this.o.length];
            var15 = new boolean[]{true, true};
            var21 = this.core
               .stoch(
                  var19,
                  var20,
                  this.h,
                  this.l,
                  this.c,
                  var6.getInt("FastK_Period"),
                  var6.getInt("SlowK_Period"),
                  this.getMAType(var6.getInt("SlowK_MAType")),
                  var6.getInt("SlowD_Period"),
                  this.getMAType(var6.getInt("SlowD_MAType")),
                  var17,
                  var18,
                  var14[0],
                  var14[1]
               );
            break;
         case "STDDEV":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.stdDev(var19, var20, var12, var6.getInt("TimePeriod"), var6.getDouble("NbDev"), var17, var18, var14[0]);
            break;
         case "VAR":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.variance(var19, var20, var12, var6.getInt("TimePeriod"), var6.getDouble("NbDev"), var17, var18, var14[0]);
            break;
         case "TRIMA":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.trima(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "WCLPRICE":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.wclPrice(var19, var20, this.h, this.l, this.c, var17, var18, var14[0]);
            break;
         case "TYPPRICE":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.typPrice(var19, var20, this.h, this.l, this.c, var17, var18, var14[0]);
            break;
         case "WILLR":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.willR(var19, var20, this.h, this.l, this.c, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "WMA":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.wma(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "TRIX":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core.trix(var19, var20, var12, var6.getInt("TimePeriod"), var17, var18, var14[0]);
            break;
         case "ULTOSC":
            var14 = new double[1][this.o.length];
            var15 = new boolean[]{true};
            var21 = this.core
               .ultOsc(
                  var19,
                  var20,
                  this.h,
                  this.l,
                  this.c,
                  var6.getInt("TimePeriod1"),
                  var6.getInt("TimePeriod2"),
                  var6.getInt("TimePeriod3"),
                  var17,
                  var18,
                  var14[0]
               );
            break;
         default:
            TALibCustomIndicator var24 = null;

            try {
               var24 = (TALibCustomIndicator)TALibCustomIndicatorsList.getInstance().findClassByName(var4);
            } catch (Exception var31) {
               throw new TradingException(String.format("Indicator '%s' not implemented.", var4));
            }

            try {
               var24.init(var6, var7, this.o, this.h, this.l, this.c, this.v, var12, var13);
               var11 = var24.calculate();
               this.cache.put(var5, var4, var1, var11, var8, var9, var10, var6);
            } catch (Exception var30) {
               throw new TradingException(String.format("Error while calculating TALibCustomIndicator '%s'. %s", var4, var30.getMessage()));
            }
      }

      if (var21 != null) {
         if (var21 != RetCode.Success) {
            throw new TradingException(String.format("Error while calculating indicator '%s'. %s", var4, var21.toString()));
         }

         if (var15 != null) {
            int var39 = 0;
            int var40 = 0;
            var11 = new float[var15.length][this.o.length];
            int var42 = 0;

            for (boolean var28 : var15) {
               for (int var29 = var17.value; var29 < var17.value + var18.value; var29++) {
                  if (var28) {
                     var11[var42][var29] = (float)var14[var39][var29 - var17.value];
                  } else {
                     var11[var42][var29] = var16[var40][var29 - var17.value];
                  }
               }

               if (var28) {
                  var39++;
               } else {
                  var40++;
               }

               var42++;
            }

            this.cache.put(var5, var4, var1, var11, var8, var9, var10, var6);
         }
      }

      this.o = null;
      this.h = null;
      this.l = null;
      this.c = null;
      this.v = null;
      this.m = null;
      this.t = null;
      this.w = null;
      Object var34 = null;
      Object var35 = null;
      var14 = null;
      var16 = null;
      var15 = null;
   }

   private int getMin(int var1, int var2) {
      return var1 < var2 ? var1 : var2;
   }

   private int getMax(int var1, int var2) {
      return var1 > var2 ? var1 : var2;
   }

   private float[] computedFrom(StrategyBase var1, SettingsMap var2, int var3, int var4) throws TradingException {
      if (var3 == -1) {
         return null;
      }

      switch (var3) {
         case 0:
            return this.c;
         case 1:
            return this.o;
         case 2:
            return this.h;
         case 3:
            return this.l;
         case 4:
            return this.m;
         case 5:
            return this.t;
         case 6:
            return this.w;
         case 7:
            return this.v;
         default:
            throw new TradingException("Invalid ComputedFrom definition, unrecognized value " + var3);
      }
   }

   private void loadOHLC(StrategyBase var1, int var2, String var3, int var4) {
      if (var1 != null) {
         BacktestData var5 = var1.Stockpicker.data;
         LoadedSymbolData var6 = var5.getChartSymbolData(var2, var3);
         String var7 = var5.getChartTimeframe(var2);
         switch (var7) {
            case "D1":
               this.o = var6.OpenD;
               this.h = var6.HighD;
               this.l = var6.LowD;
               this.c = var6.CloseD;
               this.v = var6.VolumeD;
               if (var4 == 4) {
                  this.m = this.calculateMedianValues(var6.OpenD, var6.HighD, var6.LowD, var6.CloseD);
               }

               if (var4 == 5) {
                  this.t = this.calculateTypicalValues(var6.OpenD, var6.HighD, var6.LowD, var6.CloseD);
               }

               if (var4 == 6) {
                  this.w = this.calculateWeightedValues(var6.OpenD, var6.HighD, var6.LowD, var6.CloseD);
               }
               break;
            case "Weekly":
               this.o = var6.OpenW;
               this.h = var6.HighW;
               this.l = var6.LowW;
               this.c = var6.CloseW;
               this.v = var6.VolumeW;
               if (var4 == 4) {
                  this.m = this.calculateMedianValues(var6.OpenW, var6.HighW, var6.LowW, var6.CloseW);
               }

               if (var4 == 5) {
                  this.t = this.calculateTypicalValues(var6.OpenW, var6.HighW, var6.LowW, var6.CloseW);
               }

               if (var4 == 6) {
                  this.w = this.calculateWeightedValues(var6.OpenW, var6.HighW, var6.LowW, var6.CloseW);
               }
               break;
            case "Monthly":
               this.o = var6.OpenM;
               this.h = var6.HighM;
               this.l = var6.LowM;
               this.c = var6.CloseM;
               this.v = var6.VolumeM;
               if (var4 == 4) {
                  this.m = this.calculateMedianValues(var6.OpenM, var6.HighM, var6.LowM, var6.CloseM);
               }

               if (var4 == 5) {
                  this.t = this.calculateTypicalValues(var6.OpenM, var6.HighM, var6.LowM, var6.CloseM);
               }

               if (var4 == 6) {
                  this.w = this.calculateWeightedValues(var6.OpenM, var6.HighM, var6.LowM, var6.CloseM);
               }
         }
      }
   }

   private float[] calculateWeightedValues(float[] var1, float[] var2, float[] var3, float[] var4) {
      float[] var5 = new float[var1.length];

      for (int var6 = 0; var6 < var1.length; var6++) {
         var5[var6] = PickerDataUtils.computeWeightedValue(var1[var6], var2[var6], var3[var6], var4[var6]);
      }

      return var5;
   }

   private float[] calculateTypicalValues(float[] var1, float[] var2, float[] var3, float[] var4) {
      float[] var5 = new float[var1.length];

      for (int var6 = 0; var6 < var1.length; var6++) {
         var5[var6] = PickerDataUtils.computeTypicalValue(var1[var6], var2[var6], var3[var6], var4[var6]);
      }

      return var5;
   }

   private float[] calculateMedianValues(float[] var1, float[] var2, float[] var3, float[] var4) {
      float[] var5 = new float[var1.length];

      for (int var6 = 0; var6 < var1.length; var6++) {
         var5[var6] = PickerDataUtils.computeMedianValue(var1[var6], var2[var6], var3[var6], var4[var6]);
      }

      return var5;
   }

   public void clear() {
      if (this.cache != null) {
         this.cache.clear();
      }
   }
}
