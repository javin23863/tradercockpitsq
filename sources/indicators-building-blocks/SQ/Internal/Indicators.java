package SQ.Internal;

import SQ.Blocks.Indicators.ADX.ADX;
import SQ.Blocks.Indicators.ATR.ATR;
import SQ.Blocks.Indicators.AnchoredVWAP.AnchoredVWAP;
import SQ.Blocks.Indicators.Aroon.Aroon;
import SQ.Blocks.Indicators.AvgVolume.AvgVolume;
import SQ.Blocks.Indicators.AwesomeOscillator.AwesomeOscillator;
import SQ.Blocks.Indicators.BarRange.BarRange;
import SQ.Blocks.Indicators.BarRange.BiggestRange;
import SQ.Blocks.Indicators.BarRange.HighestInRange;
import SQ.Blocks.Indicators.BarRange.LowestInRange;
import SQ.Blocks.Indicators.BarRange.SmallestRange;
import SQ.Blocks.Indicators.BearsPower.BearsPower;
import SQ.Blocks.Indicators.BollingerBands.BBRange;
import SQ.Blocks.Indicators.BollingerBands.BBWidthRatio;
import SQ.Blocks.Indicators.BollingerBands.BollingerBands;
import SQ.Blocks.Indicators.BullsPower.BullsPower;
import SQ.Blocks.Indicators.CCI.CCI;
import SQ.Blocks.Indicators.DeMarker.DeMarker;
import SQ.Blocks.Indicators.Fibo.Fibo;
import SQ.Blocks.Indicators.Fractal.Fractal;
import SQ.Blocks.Indicators.GannHiLo.GannHiLo;
import SQ.Blocks.Indicators.HeikenAshi.HeikenAshi;
import SQ.Blocks.Indicators.HighestLowest.Highest;
import SQ.Blocks.Indicators.HighestLowest.HighestIndex;
import SQ.Blocks.Indicators.HighestLowest.Lowest;
import SQ.Blocks.Indicators.HighestLowest.LowestIndex;
import SQ.Blocks.Indicators.HullMovingAverage.HullMovingAverage;
import SQ.Blocks.Indicators.Ichimoku.Ichimoku;
import SQ.Blocks.Indicators.KAMA.KAMA;
import SQ.Blocks.Indicators.KaufmanEfficiencyRatio.KaufmanEfficiencyRatio;
import SQ.Blocks.Indicators.KeltnerChannel.KeltnerChannel;
import SQ.Blocks.Indicators.LaguerreRSI.LaguerreRSI;
import SQ.Blocks.Indicators.LinReg.LinearRegression;
import SQ.Blocks.Indicators.MACD.MACD;
import SQ.Blocks.Indicators.MTATR.MTATR;
import SQ.Blocks.Indicators.MTKeltnerChannel.MTKeltnerChannel;
import SQ.Blocks.Indicators.Momentum.Momentum;
import SQ.Blocks.Indicators.MovingAverage.EMA;
import SQ.Blocks.Indicators.MovingAverage.LWMA;
import SQ.Blocks.Indicators.MovingAverage.MovingAverage;
import SQ.Blocks.Indicators.MovingAverage.SMA;
import SQ.Blocks.Indicators.MovingAverage.SMMA;
import SQ.Blocks.Indicators.MovingAverage.TEMA;
import SQ.Blocks.Indicators.OSMA.OSMA;
import SQ.Blocks.Indicators.Other.DataLoggingIndy;
import SQ.Blocks.Indicators.ParabolicSAR.ParabolicSAR;
import SQ.Blocks.Indicators.Pivots.Pivots;
import SQ.Blocks.Indicators.QQE.QQE;
import SQ.Blocks.Indicators.ROC.ROC;
import SQ.Blocks.Indicators.RSI.RSI;
import SQ.Blocks.Indicators.Reflex.Reflex;
import SQ.Blocks.Indicators.SRPercentRank.SRPercentRank;
import SQ.Blocks.Indicators.SchaffTrendCycle.SchaffTrendCycle;
import SQ.Blocks.Indicators.StdDev.StdDev;
import SQ.Blocks.Indicators.Stochastic.Stochastic;
import SQ.Blocks.Indicators.SuperTrend.SuperTrend;
import SQ.Blocks.Indicators.TPOProfile.TPOProfile;
import SQ.Blocks.Indicators.TrueRange.TrueRange;
import SQ.Blocks.Indicators.UlcerIndex.UlcerIndex;
import SQ.Blocks.Indicators.VWAP.VWAP;
import SQ.Blocks.Indicators.VolumeProfile.VolumeProfile;
import SQ.Blocks.Indicators.VolumeProfile.VolumeProfileCustomHours;
import SQ.Blocks.Indicators.VolumeProfile.VolumeProfileCustomHoursMultiSession;
import SQ.Blocks.Indicators.Vortex.Vortex;
import SQ.Blocks.Indicators.WaveTrend.WaveTrend;
import SQ.Blocks.Indicators.WilliamsPR.WilliamsPR;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.dataseries.TimeDataSeries;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.indicator.IndicatorsObj;

public class Indicators extends IndicatorsObj {
   private boolean hasZeroShift;
   private StrategyBase Strategy;

   public void setHasZeroShift(boolean var1) {
      this.hasZeroShift = var1;
   }

   public Indicators(StrategyBase var1) {
      this.Strategy = var1;
   }

   public ADX ADX(ChartData var1, int var2) throws TradingException {
      long var3 = this.Engine + 2649910181429105554L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         ADX var5 = new ADX();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      ADX var6 = (ADX)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public ATR ATR(ChartData var1, int var2) throws TradingException {
      long var3 = this.Engine + -9079487113293377728L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         ATR var5 = new ATR();
         var5.Chart = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      ATR var6 = (ATR)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public AnchoredVWAP AnchoredVWAP(ChartData var1, int var2, double var3) throws TradingException {
      long var5 = this.Engine + -3346673825811679000L + var1.chartHashCode() + var2 + (int)(37.0 * var3);
      if (!this.indicatorsCache.containsKey(var5)) {
         AnchoredVWAP var7 = new AnchoredVWAP();
         var7.Chart = var1;
         var7.SessionType = var2;
         var7.StdDevMult = var3;
         var7.initialize(this, this.hasZeroShift);
         var7.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var5, var7);
      }

      AnchoredVWAP var8 = (AnchoredVWAP)this.indicatorsCache.get(var5);
      var8.refreshShift();
      return var8;
   }

   public Aroon Aroon(ChartData var1, int var2) throws TradingException {
      long var3 = this.Engine + 5912219844429856438L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         Aroon var5 = new Aroon();
         var5.Chart = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      Aroon var6 = (Aroon)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public AvgVolume AvgVolume(ChartData var1, int var2) throws TradingException {
      long var3 = this.Engine + 3572842574091906926L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         AvgVolume var5 = new AvgVolume();
         var5.Chart = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      AvgVolume var6 = (AvgVolume)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public AwesomeOscillator AwesomeOscillator(ChartData var1) throws TradingException {
      long var2 = this.Engine + -1180824349519950413L + var1.chartHashCode();
      if (!this.indicatorsCache.containsKey(var2)) {
         AwesomeOscillator var4 = new AwesomeOscillator();
         var4.Input = var1;
         var4.initialize(this, this.hasZeroShift);
         var4.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var2, var4);
      }

      AwesomeOscillator var5 = (AwesomeOscillator)this.indicatorsCache.get(var2);
      var5.refreshShift();
      return var5;
   }

   public BBRange BBRange(ChartData var1, int var2, double var3, int var5) throws TradingException {
      long var6 = this.Engine + 1893038667322575367L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var5}) + (int)(37.0 * var3);
      if (!this.indicatorsCache.containsKey(var6)) {
         BBRange var8 = new BBRange();
         var8.Chart = var1;
         var8.Period = var2;
         var8.Deviation = var3;
         var8.ComputedFrom = var5;
         var8.initialize(this, this.hasZeroShift);
         var8.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var6, var8);
      }

      BBRange var9 = (BBRange)this.indicatorsCache.get(var6);
      var9.refreshShift();
      return var9;
   }

   public BBWidthRatio BBWidthRatio(ChartData var1, int var2, double var3, int var5) throws TradingException {
      long var6 = this.Engine + -2041758268165272493L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var5}) + (int)(37.0 * var3);
      if (!this.indicatorsCache.containsKey(var6)) {
         BBWidthRatio var8 = new BBWidthRatio();
         var8.Input = var1;
         var8.Period = var2;
         var8.Deviation = var3;
         var8.ComputedFrom = var5;
         var8.initialize(this, this.hasZeroShift);
         var8.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var6, var8);
      }

      BBWidthRatio var9 = (BBWidthRatio)this.indicatorsCache.get(var6);
      var9.refreshShift();
      return var9;
   }

   public BarRange BarRange(ChartData var1) throws TradingException {
      long var2 = this.Engine + -9221856496902370897L + var1.chartHashCode();
      if (!this.indicatorsCache.containsKey(var2)) {
         BarRange var4 = new BarRange();
         var4.Chart = var1;
         var4.initialize(this, this.hasZeroShift);
         var4.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var2, var4);
      }

      BarRange var5 = (BarRange)this.indicatorsCache.get(var2);
      var5.refreshShift();
      return var5;
   }

   public BearsPower BearsPower(ChartData var1, int var2, int var3) throws TradingException {
      long var4 = this.Engine + -4241382397975899889L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var3});
      if (!this.indicatorsCache.containsKey(var4)) {
         BearsPower var6 = new BearsPower();
         var6.Input = var1;
         var6.Period = var2;
         var6.ComputedFrom = var3;
         var6.initialize(this, this.hasZeroShift);
         var6.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var4, var6);
      }

      BearsPower var7 = (BearsPower)this.indicatorsCache.get(var4);
      var7.refreshShift();
      return var7;
   }

   public BiggestRange BiggestRange(ChartData var1, int var2) throws TradingException {
      long var3 = this.Engine + 771253393487848173L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         BiggestRange var5 = new BiggestRange();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      BiggestRange var6 = (BiggestRange)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public BollingerBands BollingerBands(DataSeries var1, int var2, double var3) throws TradingException {
      long var5 = this.Engine + -7318124958737053846L + var1.chartHashCode() + var2 + (int)(37.0 * var3);
      if (!this.indicatorsCache.containsKey(var5)) {
         BollingerBands var7 = new BollingerBands();
         var7.Input = var1;
         var7.Period = var2;
         var7.Deviation = var3;
         var7.initialize(this, this.hasZeroShift);
         var7.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var5, var7);
      }

      BollingerBands var8 = (BollingerBands)this.indicatorsCache.get(var5);
      var8.refreshShift();
      return var8;
   }

   public BullsPower BullsPower(ChartData var1, int var2, int var3) throws TradingException {
      long var4 = this.Engine + 5612962924337078367L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var3});
      if (!this.indicatorsCache.containsKey(var4)) {
         BullsPower var6 = new BullsPower();
         var6.Input = var1;
         var6.Period = var2;
         var6.ComputedFrom = var3;
         var6.initialize(this, this.hasZeroShift);
         var6.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var4, var6);
      }

      BullsPower var7 = (BullsPower)this.indicatorsCache.get(var4);
      var7.refreshShift();
      return var7;
   }

   public CCI CCI(DataSeries var1, int var2) throws TradingException {
      long var3 = this.Engine + 6145588920263969609L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         CCI var5 = new CCI();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      CCI var6 = (CCI)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public DataLoggingIndy DataLoggingIndy(TimeDataSeries var1, DataSeries var2, DataSeries var3, DataSeries var4, DataSeries var5, DataSeries var6) throws TradingException {
      long var7 = this.Engine
         + 6747168366745394951L
         + var1.hashCode()
         + var2.chartHashCode()
         + var3.chartHashCode()
         + var4.chartHashCode()
         + var5.chartHashCode()
         + var6.chartHashCode();
      if (!this.indicatorsCache.containsKey(var7)) {
         DataLoggingIndy var9 = new DataLoggingIndy();
         var9.Tinput = var1;
         var9.Oinput = var2;
         var9.Hinput = var3;
         var9.Linput = var4;
         var9.Cinput = var5;
         var9.Vinput = var6;
         var9.initialize(this, this.hasZeroShift);
         var9.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var7, var9);
      }

      DataLoggingIndy var10 = (DataLoggingIndy)this.indicatorsCache.get(var7);
      var10.refreshShift();
      return var10;
   }

   public DeMarker DeMarker(ChartData var1, int var2) throws TradingException {
      long var3 = this.Engine + -1043877220955214314L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         DeMarker var5 = new DeMarker();
         var5.Chart = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      DeMarker var6 = (DeMarker)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public EMA EMA(DataSeries var1, int var2) throws TradingException {
      long var3 = this.Engine + 2318062612325506369L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         EMA var5 = new EMA();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      EMA var6 = (EMA)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public Fibo Fibo(ChartData var1, int var2, double var3) throws TradingException {
      long var5 = this.Engine + 4234090606858028209L + var1.chartHashCode() + var2 + (int)(37.0 * var3);
      if (!this.indicatorsCache.containsKey(var5)) {
         Fibo var7 = new Fibo();
         var7.Chart = var1;
         var7.FiboRange = var2;
         var7.FiboLevel = var3;
         var7.initialize(this, this.hasZeroShift);
         var7.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var5, var7);
      }

      Fibo var8 = (Fibo)this.indicatorsCache.get(var5);
      var8.refreshShift();
      return var8;
   }

   public Fractal Fractal(ChartData var1, int var2) throws TradingException {
      long var3 = this.Engine + 471270077278654726L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         Fractal var5 = new Fractal();
         var5.Chart = var1;
         var5.Fractal = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      Fractal var6 = (Fractal)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public GannHiLo GannHiLo(ChartData var1, int var2) throws TradingException {
      long var3 = this.Engine + 7159229647037632676L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         GannHiLo var5 = new GannHiLo();
         var5.Chart = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      GannHiLo var6 = (GannHiLo)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public HeikenAshi HeikenAshi(ChartData var1) throws TradingException {
      long var2 = this.Engine + -5943901234518539156L + var1.chartHashCode();
      if (!this.indicatorsCache.containsKey(var2)) {
         HeikenAshi var4 = new HeikenAshi();
         var4.Chart = var1;
         var4.initialize(this, this.hasZeroShift);
         var4.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var2, var4);
      }

      HeikenAshi var5 = (HeikenAshi)this.indicatorsCache.get(var2);
      var5.refreshShift();
      return var5;
   }

   public Highest Highest(DataSeries var1, int var2) throws TradingException {
      long var3 = this.Engine + 9153961714451168276L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         Highest var5 = new Highest();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      Highest var6 = (Highest)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public HighestInRange HighestInRange(ChartData var1, int var2, int var3) throws TradingException {
      long var4 = this.Engine + 6612367740298608714L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var3});
      if (!this.indicatorsCache.containsKey(var4)) {
         HighestInRange var6 = new HighestInRange();
         var6.Input = var1;
         var6.TimeFrom = var2;
         var6.TimeTo = var3;
         var6.initialize(this, this.hasZeroShift);
         var6.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var4, var6);
      }

      HighestInRange var7 = (HighestInRange)this.indicatorsCache.get(var4);
      var7.refreshShift();
      return var7;
   }

   public HighestIndex HighestIndex(DataSeries var1, int var2) throws TradingException {
      long var3 = this.Engine + -5183852730716672033L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         HighestIndex var5 = new HighestIndex();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      HighestIndex var6 = (HighestIndex)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public HullMovingAverage HullMovingAverage(DataSeries var1, int var2) throws TradingException {
      long var3 = this.Engine + -7699848334758540619L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         HullMovingAverage var5 = new HullMovingAverage();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      HullMovingAverage var6 = (HullMovingAverage)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public Ichimoku Ichimoku(ChartData var1, int var2, int var3, int var4) throws TradingException {
      long var5 = this.Engine + -7097731500378219577L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var3, var4});
      if (!this.indicatorsCache.containsKey(var5)) {
         Ichimoku var7 = new Ichimoku();
         var7.Chart = var1;
         var7.TenkanPeriod = var2;
         var7.KijunPeriod = var3;
         var7.SenkouPeriod = var4;
         var7.initialize(this, this.hasZeroShift);
         var7.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var5, var7);
      }

      Ichimoku var8 = (Ichimoku)this.indicatorsCache.get(var5);
      var8.refreshShift();
      return var8;
   }

   public KAMA KAMA(ChartData var1, int var2, int var3, int var4) throws TradingException {
      long var5 = this.Engine + 8362797796748016681L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var3, var4});
      if (!this.indicatorsCache.containsKey(var5)) {
         KAMA var7 = new KAMA();
         var7.Chart = var1;
         var7.ERPeriod = var2;
         var7.ShortPeriod = var3;
         var7.LongPeriod = var4;
         var7.initialize(this, this.hasZeroShift);
         var7.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var5, var7);
      }

      KAMA var8 = (KAMA)this.indicatorsCache.get(var5);
      var8.refreshShift();
      return var8;
   }

   public KaufmanEfficiencyRatio KaufmanEfficiencyRatio(ChartData var1, int var2) throws TradingException {
      long var3 = this.Engine + -6296598236630776930L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         KaufmanEfficiencyRatio var5 = new KaufmanEfficiencyRatio();
         var5.Chart = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      KaufmanEfficiencyRatio var6 = (KaufmanEfficiencyRatio)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public KeltnerChannel KeltnerChannel(ChartData var1, int var2, double var3) throws TradingException {
      long var5 = this.Engine + -4250889032845815297L + var1.chartHashCode() + var2 + (int)(37.0 * var3);
      if (!this.indicatorsCache.containsKey(var5)) {
         KeltnerChannel var7 = new KeltnerChannel();
         var7.Input = var1;
         var7.Period = var2;
         var7.Deviation = var3;
         var7.initialize(this, this.hasZeroShift);
         var7.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var5, var7);
      }

      KeltnerChannel var8 = (KeltnerChannel)this.indicatorsCache.get(var5);
      var8.refreshShift();
      return var8;
   }

   public LWMA LWMA(DataSeries var1, int var2) throws TradingException {
      long var3 = this.Engine + 4622713547143785778L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         LWMA var5 = new LWMA();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      LWMA var6 = (LWMA)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public LaguerreRSI LaguerreRSI(ChartData var1, double var2) throws TradingException {
      long var4 = this.Engine + -6895453554246873703L + var1.chartHashCode() + (int)(37.0 * var2);
      if (!this.indicatorsCache.containsKey(var4)) {
         LaguerreRSI var6 = new LaguerreRSI();
         var6.Chart = var1;
         var6.Gamma = var2;
         var6.initialize(this, this.hasZeroShift);
         var6.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var4, var6);
      }

      LaguerreRSI var7 = (LaguerreRSI)this.indicatorsCache.get(var4);
      var7.refreshShift();
      return var7;
   }

   public LinearRegression LinearRegression(DataSeries var1, int var2) throws TradingException {
      long var3 = this.Engine + 3205014151374460577L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         LinearRegression var5 = new LinearRegression();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      LinearRegression var6 = (LinearRegression)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public Lowest Lowest(DataSeries var1, int var2) throws TradingException {
      long var3 = this.Engine + -8981191732789932475L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         Lowest var5 = new Lowest();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      Lowest var6 = (Lowest)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public LowestInRange LowestInRange(ChartData var1, int var2, int var3) throws TradingException {
      long var4 = this.Engine + -2087190515400894412L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var3});
      if (!this.indicatorsCache.containsKey(var4)) {
         LowestInRange var6 = new LowestInRange();
         var6.Input = var1;
         var6.TimeFrom = var2;
         var6.TimeTo = var3;
         var6.initialize(this, this.hasZeroShift);
         var6.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var4, var6);
      }

      LowestInRange var7 = (LowestInRange)this.indicatorsCache.get(var4);
      var7.refreshShift();
      return var7;
   }

   public LowestIndex LowestIndex(DataSeries var1, int var2) throws TradingException {
      long var3 = this.Engine + 2022011787699968335L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         LowestIndex var5 = new LowestIndex();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      LowestIndex var6 = (LowestIndex)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public MACD MACD(DataSeries var1, int var2, int var3, int var4) throws TradingException {
      long var5 = this.Engine + 5167397476533699938L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var3, var4});
      if (!this.indicatorsCache.containsKey(var5)) {
         MACD var7 = new MACD();
         var7.Input = var1;
         var7.Fast = var2;
         var7.Slow = var3;
         var7.Smooth = var4;
         var7.initialize(this, this.hasZeroShift);
         var7.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var5, var7);
      }

      MACD var8 = (MACD)this.indicatorsCache.get(var5);
      var8.refreshShift();
      return var8;
   }

   public MTATR MTATR(ChartData var1, int var2) throws TradingException {
      long var3 = this.Engine + -7081782697645202279L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         MTATR var5 = new MTATR();
         var5.Chart = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      MTATR var6 = (MTATR)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public MTKeltnerChannel MTKeltnerChannel(ChartData var1, int var2, double var3) throws TradingException {
      long var5 = this.Engine + -3125520992811289534L + var1.chartHashCode() + var2 + (int)(37.0 * var3);
      if (!this.indicatorsCache.containsKey(var5)) {
         MTKeltnerChannel var7 = new MTKeltnerChannel();
         var7.Input = var1;
         var7.Period = var2;
         var7.Deviation = var3;
         var7.initialize(this, this.hasZeroShift);
         var7.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var5, var7);
      }

      MTKeltnerChannel var8 = (MTKeltnerChannel)this.indicatorsCache.get(var5);
      var8.refreshShift();
      return var8;
   }

   public Momentum Momentum(DataSeries var1, int var2) throws TradingException {
      long var3 = this.Engine + 8499554868961660647L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         Momentum var5 = new Momentum();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      Momentum var6 = (Momentum)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public MovingAverage MovingAverage(DataSeries var1, int var2, int var3) throws TradingException {
      long var4 = this.Engine + -8322236142805281440L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var3});
      if (!this.indicatorsCache.containsKey(var4)) {
         MovingAverage var6 = new MovingAverage();
         var6.Input = var1;
         var6.Period = var2;
         var6.MAMethod = var3;
         var6.initialize(this, this.hasZeroShift);
         var6.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var4, var6);
      }

      MovingAverage var7 = (MovingAverage)this.indicatorsCache.get(var4);
      var7.refreshShift();
      return var7;
   }

   public OSMA OSMA(DataSeries var1, int var2, int var3, int var4) throws TradingException {
      long var5 = this.Engine + 8755995658102603068L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var3, var4});
      if (!this.indicatorsCache.containsKey(var5)) {
         OSMA var7 = new OSMA();
         var7.Input = var1;
         var7.FastEMA = var2;
         var7.SlowEMA = var3;
         var7.SignalPeriod = var4;
         var7.initialize(this, this.hasZeroShift);
         var7.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var5, var7);
      }

      OSMA var8 = (OSMA)this.indicatorsCache.get(var5);
      var8.refreshShift();
      return var8;
   }

   public ParabolicSAR ParabolicSAR(ChartData var1, double var2, double var4) throws TradingException {
      long var6 = this.Engine + -957642607645990205L + var1.chartHashCode() + SQUtils.doublesHash(new double[]{var2, var4});
      if (!this.indicatorsCache.containsKey(var6)) {
         ParabolicSAR var8 = new ParabolicSAR();
         var8.Input = var1;
         var8.Step = var2;
         var8.Maximum = var4;
         var8.initialize(this, this.hasZeroShift);
         var8.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var6, var8);
      }

      ParabolicSAR var9 = (ParabolicSAR)this.indicatorsCache.get(var6);
      var9.refreshShift();
      return var9;
   }

   public Pivots Pivots(ChartData var1, int var2, int var3) throws TradingException {
      long var4 = this.Engine + 963150467221572839L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var3});
      if (!this.indicatorsCache.containsKey(var4)) {
         Pivots var6 = new Pivots();
         var6.Chart = var1;
         var6.StartHour = var2;
         var6.StartMinute = var3;
         var6.initialize(this, this.hasZeroShift);
         var6.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var4, var6);
      }

      Pivots var7 = (Pivots)this.indicatorsCache.get(var4);
      var7.refreshShift();
      return var7;
   }

   public QQE QQE(ChartData var1, int var2, int var3, double var4) throws TradingException {
      long var6 = this.Engine + -5774469501628734501L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var3}) + (int)(37.0 * var4);
      if (!this.indicatorsCache.containsKey(var6)) {
         QQE var8 = new QQE();
         var8.Chart = var1;
         var8.RSIPeriod = var2;
         var8.sF = var3;
         var8.wF = var4;
         var8.initialize(this, this.hasZeroShift);
         var8.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var6, var8);
      }

      QQE var9 = (QQE)this.indicatorsCache.get(var6);
      var9.refreshShift();
      return var9;
   }

   public ROC ROC(ChartData var1, int var2) throws TradingException {
      long var3 = this.Engine + 9050015835207583142L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         ROC var5 = new ROC();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      ROC var6 = (ROC)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public RSI RSI(DataSeries var1, int var2) throws TradingException {
      long var3 = this.Engine + 4375848095962754478L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         RSI var5 = new RSI();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      RSI var6 = (RSI)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public Reflex Reflex(ChartData var1, int var2) throws TradingException {
      long var3 = this.Engine + 3942973377422065824L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         Reflex var5 = new Reflex();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      Reflex var6 = (Reflex)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public SMA SMA(DataSeries var1, int var2) throws TradingException {
      long var3 = this.Engine + -5078669517565619308L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         SMA var5 = new SMA();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      SMA var6 = (SMA)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public SMMA SMMA(DataSeries var1, int var2) throws TradingException {
      long var3 = this.Engine + 5761672542590343460L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         SMMA var5 = new SMMA();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      SMMA var6 = (SMMA)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public SRPercentRank SRPercentRank(ChartData var1, int var2, int var3, int var4) throws TradingException {
      long var5 = this.Engine + 4879138412592424510L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var3, var4});
      if (!this.indicatorsCache.containsKey(var5)) {
         SRPercentRank var7 = new SRPercentRank();
         var7.Chart = var1;
         var7.Mode = var2;
         var7.Length = var3;
         var7.ATRPeriod = var4;
         var7.initialize(this, this.hasZeroShift);
         var7.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var5, var7);
      }

      SRPercentRank var8 = (SRPercentRank)this.indicatorsCache.get(var5);
      var8.refreshShift();
      return var8;
   }

   public SchaffTrendCycle SchaffTrendCycle(ChartData var1, int var2, int var3, int var4) throws TradingException {
      long var5 = this.Engine + 3648820954109573374L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var3, var4});
      if (!this.indicatorsCache.containsKey(var5)) {
         SchaffTrendCycle var7 = new SchaffTrendCycle();
         var7.Input = var1;
         var7.StochPeriod = var2;
         var7.FastPeriod = var3;
         var7.SlowPeriod = var4;
         var7.initialize(this, this.hasZeroShift);
         var7.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var5, var7);
      }

      SchaffTrendCycle var8 = (SchaffTrendCycle)this.indicatorsCache.get(var5);
      var8.refreshShift();
      return var8;
   }

   public SmallestRange SmallestRange(ChartData var1, int var2) throws TradingException {
      long var3 = this.Engine + 1059494935463611065L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         SmallestRange var5 = new SmallestRange();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      SmallestRange var6 = (SmallestRange)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public StdDev StdDev(DataSeries var1, int var2) throws TradingException {
      long var3 = this.Engine + 4208228923129027359L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         StdDev var5 = new StdDev();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      StdDev var6 = (StdDev)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public Stochastic Stochastic(ChartData var1, int var2, int var3, int var4, int var5, int var6) throws TradingException {
      long var7 = this.Engine + 2306031294302496378L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var3, var4, var5, var6});
      if (!this.indicatorsCache.containsKey(var7)) {
         Stochastic var9 = new Stochastic();
         var9.Input = var1;
         var9.KPeriod = var2;
         var9.DPeriod = var3;
         var9.Slowing = var4;
         var9.MAMethod = var5;
         var9.PriceField = var6;
         var9.initialize(this, this.hasZeroShift);
         var9.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var7, var9);
      }

      Stochastic var10 = (Stochastic)this.indicatorsCache.get(var7);
      var10.refreshShift();
      return var10;
   }

   public SuperTrend SuperTrend(ChartData var1, int var2, int var3, double var4) throws TradingException {
      long var6 = this.Engine + -134614647241418174L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var3}) + (int)(37.0 * var4);
      if (!this.indicatorsCache.containsKey(var6)) {
         SuperTrend var8 = new SuperTrend();
         var8.Input = var1;
         var8.Mode = var2;
         var8.ATRPeriod = var3;
         var8.ATRMult = var4;
         var8.initialize(this, this.hasZeroShift);
         var8.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var6, var8);
      }

      SuperTrend var9 = (SuperTrend)this.indicatorsCache.get(var6);
      var9.refreshShift();
      return var9;
   }

   public TEMA TEMA(DataSeries var1, int var2) throws TradingException {
      long var3 = this.Engine + 8092699764052100094L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         TEMA var5 = new TEMA();
         var5.Input = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      TEMA var6 = (TEMA)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public TPOProfile TPOProfile(
      ChartData var1,
      int var2,
      int var3,
      int var4,
      int var5,
      double var6,
      boolean var8,
      int var9,
      int var10,
      int var11,
      int var12,
      int var13,
      int var14,
      int var15,
      int var16,
      boolean var17,
      boolean var18,
      boolean var19
   ) throws TradingException {
      long var20 = this.Engine
         + -4769500882741990806L
         + var1.chartHashCode()
         + SQUtils.intsHash(new int[]{var2, var3, var4, var5, var9, var10, var11, var12, var13, var14, var15, var16})
         + (int)(37.0 * var6)
         + SQUtils.booleansHash(new boolean[]{var8, var17, var18, var19});
      if (!this.indicatorsCache.containsKey(var20)) {
         TPOProfile var22 = new TPOProfile();
         var22.Chart = var1;
         var22.SessionType = var2;
         var22.ProfileRows = var3;
         var22.BinSizeMode = var4;
         var22.TicksPerBin = var5;
         var22.ValueAreaPct = var6;
         var22.UseCustomHours = var8;
         var22.StartHour = var9;
         var22.StartMinute = var10;
         var22.EndHour = var11;
         var22.EndMinute = var12;
         var22.BracketMinDaily = var13;
         var22.BracketMinWeekly = var14;
         var22.BracketMinMonthly = var15;
         var22.BracketMinYearly = var16;
         var22.ShowCandlesticks = var17;
         var22.ShowShapeLabel = var18;
         var22.UseBlockMode = var19;
         var22.initialize(this, this.hasZeroShift);
         var22.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var20, var22);
      }

      TPOProfile var23 = (TPOProfile)this.indicatorsCache.get(var20);
      var23.refreshShift();
      return var23;
   }

   public TrueRange TrueRange(ChartData var1) throws TradingException {
      long var2 = this.Engine + 7822877233330299681L + var1.chartHashCode();
      if (!this.indicatorsCache.containsKey(var2)) {
         TrueRange var4 = new TrueRange();
         var4.Chart = var1;
         var4.initialize(this, this.hasZeroShift);
         var4.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var2, var4);
      }

      TrueRange var5 = (TrueRange)this.indicatorsCache.get(var2);
      var5.refreshShift();
      return var5;
   }

   public UlcerIndex UlcerIndex(ChartData var1, int var2, int var3) throws TradingException {
      long var4 = this.Engine + -2053987696432323927L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var3});
      if (!this.indicatorsCache.containsKey(var4)) {
         UlcerIndex var6 = new UlcerIndex();
         var6.Input = var1;
         var6.Mode = var2;
         var6.Period = var3;
         var6.initialize(this, this.hasZeroShift);
         var6.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var4, var6);
      }

      UlcerIndex var7 = (UlcerIndex)this.indicatorsCache.get(var4);
      var7.refreshShift();
      return var7;
   }

   public VWAP VWAP(ChartData var1, int var2) throws TradingException {
      long var3 = this.Engine + -8239802216863368915L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         VWAP var5 = new VWAP();
         var5.Input = var1;
         var5.VWAPPeriod = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      VWAP var6 = (VWAP)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public VolumeProfile VolumeProfile(
      ChartData var1,
      int var2,
      int var3,
      int var4,
      int var5,
      double var6,
      int var8,
      int var9,
      int var10,
      int var11,
      boolean var12,
      boolean var13,
      double var14,
      int var16,
      int var17,
      double var18,
      int var20,
      double var21,
      int var23,
      boolean var24,
      boolean var25,
      int var26,
      boolean var27,
      boolean var28,
      boolean var29,
      boolean var30,
      boolean var31,
      boolean var32
   ) throws TradingException {
      long var33 = this.Engine
         + -2777649303129309491L
         + var1.chartHashCode()
         + SQUtils.intsHash(new int[]{var2, var3, var4, var5, var8, var9, var10, var11, var16, var17, var20, var23, var26})
         + SQUtils.doublesHash(new double[]{var6, var14, var18, var21})
         + SQUtils.booleansHash(new boolean[]{var12, var13, var24, var25, var27, var28, var29, var30, var31, var32});
      if (!this.indicatorsCache.containsKey(var33)) {
         VolumeProfile var35 = new VolumeProfile();
         var35.Chart = var1;
         var35.SessionType = var2;
         var35.ProfileRows = var3;
         var35.BinSizeMode = var4;
         var35.TicksPerBin = var5;
         var35.ValueAreaPct = var6;
         var35.IBMinutes = var8;
         var35.HvnCount = var9;
         var35.HvnThresholdPct = var10;
         var35.LvnThresholdPct = var11;
         var35.EnableLVN = var12;
         var35.EnableVCP = var13;
         var35.ClusterSpread = var14;
         var35.MaxClusterCenters = var16;
         var35.PivotMethod = var17;
         var35.PivotPct = var18;
         var35.PivotTicks = var20;
         var35.PivotATRMultiple = var21;
         var35.PivotATRPeriod = var23;
         var35.ShowCandlesticks = var24;
         var35.ShowVolumeSubchart = var25;
         var35.VolumeMALength = var26;
         var35.ShowPOCDelta = var27;
         var35.ShowVADelta = var28;
         var35.ShowProfileRange = var29;
         var35.ShowPOCPosition = var30;
         var35.ShowDeltaPerLevel = var31;
         var35.ShowZigZagLine = var32;
         var35.initialize(this, this.hasZeroShift);
         var35.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var33, var35);
      }

      VolumeProfile var36 = (VolumeProfile)this.indicatorsCache.get(var33);
      var36.refreshShift();
      return var36;
   }

   public VolumeProfileCustomHours VolumeProfileCustomHours(
      ChartData var1,
      int var2,
      int var3,
      int var4,
      int var5,
      int var6,
      int var7,
      int var8,
      int var9,
      double var10,
      int var12,
      int var13,
      int var14,
      boolean var15,
      boolean var16,
      double var17,
      int var19,
      int var20,
      boolean var21,
      boolean var22,
      int var23,
      boolean var24,
      boolean var25,
      boolean var26,
      boolean var27,
      boolean var28
   ) throws TradingException {
      long var29 = this.Engine
         + 2364873747930282726L
         + var1.chartHashCode()
         + SQUtils.intsHash(new int[]{var2, var3, var4, var5, var6, var7, var8, var9, var12, var13, var14, var19, var20, var23})
         + SQUtils.doublesHash(new double[]{var10, var17})
         + SQUtils.booleansHash(new boolean[]{var15, var16, var21, var22, var24, var25, var26, var27, var28});
      if (!this.indicatorsCache.containsKey(var29)) {
         VolumeProfileCustomHours var31 = new VolumeProfileCustomHours();
         var31.Chart = var1;
         var31.SessionStartHours = var2;
         var31.SessionStartMinutes = var3;
         var31.SessionEndHours = var4;
         var31.SessionEndMinutes = var5;
         var31.SessionMode = var6;
         var31.ProfileRows = var7;
         var31.BinSizeMode = var8;
         var31.TicksPerBin = var9;
         var31.ValueAreaPct = var10;
         var31.HvnCount = var12;
         var31.HvnThresholdPct = var13;
         var31.LvnThresholdPct = var14;
         var31.EnableLVN = var15;
         var31.EnableVCP = var16;
         var31.ClusterSpread = var17;
         var31.MaxClusterCenters = var19;
         var31.IBMinutes = var20;
         var31.ShowCandlesticks = var21;
         var31.ShowVolumeSubchart = var22;
         var31.VolumeMALength = var23;
         var31.ShowPOCDelta = var24;
         var31.ShowVADelta = var25;
         var31.ShowProfileRange = var26;
         var31.ShowPOCPosition = var27;
         var31.ShowDeltaPerLevel = var28;
         var31.initialize(this, this.hasZeroShift);
         var31.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var29, var31);
      }

      VolumeProfileCustomHours var32 = (VolumeProfileCustomHours)this.indicatorsCache.get(var29);
      var32.refreshShift();
      return var32;
   }

   public VolumeProfileCustomHoursMultiSession VolumeProfileCustomHoursMultiSession(
      ChartData var1,
      boolean var2,
      int var3,
      int var4,
      int var5,
      int var6,
      boolean var7,
      int var8,
      int var9,
      int var10,
      int var11,
      boolean var12,
      int var13,
      int var14,
      int var15,
      int var16,
      boolean var17,
      int var18,
      int var19,
      int var20,
      int var21,
      int var22,
      int var23,
      int var24,
      int var25,
      double var26,
      int var28,
      int var29,
      int var30,
      boolean var31,
      boolean var32,
      double var33,
      int var35,
      int var36,
      boolean var37,
      boolean var38,
      int var39,
      boolean var40,
      boolean var41,
      boolean var42,
      boolean var43,
      boolean var44
   ) throws TradingException {
      long var45 = this.Engine
         + -243479629652240495L
         + var1.chartHashCode()
         + SQUtils.intsHash(
            new int[]{
               var3,
               var4,
               var5,
               var6,
               var8,
               var9,
               var10,
               var11,
               var13,
               var14,
               var15,
               var16,
               var18,
               var19,
               var20,
               var21,
               var22,
               var23,
               var24,
               var25,
               var28,
               var29,
               var30,
               var35,
               var36,
               var39
            }
         )
         + SQUtils.doublesHash(new double[]{var26, var33})
         + SQUtils.booleansHash(new boolean[]{var2, var7, var12, var17, var31, var32, var37, var38, var40, var41, var42, var43, var44});
      if (!this.indicatorsCache.containsKey(var45)) {
         VolumeProfileCustomHoursMultiSession var47 = new VolumeProfileCustomHoursMultiSession();
         var47.Chart = var1;
         var47.EnableLondon = var2;
         var47.LondonStartHour = var3;
         var47.LondonStartMin = var4;
         var47.LondonEndHour = var5;
         var47.LondonEndMin = var6;
         var47.EnableNewYork = var7;
         var47.NewYorkStartHour = var8;
         var47.NewYorkStartMin = var9;
         var47.NewYorkEndHour = var10;
         var47.NewYorkEndMin = var11;
         var47.EnableSydney = var12;
         var47.SydneyStartHour = var13;
         var47.SydneyStartMin = var14;
         var47.SydneyEndHour = var15;
         var47.SydneyEndMin = var16;
         var47.EnableTokyo = var17;
         var47.TokyoStartHour = var18;
         var47.TokyoStartMin = var19;
         var47.TokyoEndHour = var20;
         var47.TokyoEndMin = var21;
         var47.SessionMode = var22;
         var47.ProfileRows = var23;
         var47.BinSizeMode = var24;
         var47.TicksPerBin = var25;
         var47.ValueAreaPct = var26;
         var47.HvnCount = var28;
         var47.HvnThresholdPct = var29;
         var47.LvnThresholdPct = var30;
         var47.EnableLVN = var31;
         var47.EnableVCP = var32;
         var47.ClusterSpread = var33;
         var47.MaxClusterCenters = var35;
         var47.IBMinutes = var36;
         var47.ShowCandlesticks = var37;
         var47.ShowVolumeSubchart = var38;
         var47.VolumeMALength = var39;
         var47.ShowPOCDelta = var40;
         var47.ShowVADelta = var41;
         var47.ShowProfileRange = var42;
         var47.ShowPOCPosition = var43;
         var47.ShowDeltaPerLevel = var44;
         var47.initialize(this, this.hasZeroShift);
         var47.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var45, var47);
      }

      VolumeProfileCustomHoursMultiSession var48 = (VolumeProfileCustomHoursMultiSession)this.indicatorsCache.get(var45);
      var48.refreshShift();
      return var48;
   }

   public Vortex Vortex(ChartData var1, int var2) throws TradingException {
      long var3 = this.Engine + -6079528525011855141L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         Vortex var5 = new Vortex();
         var5.Chart = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      Vortex var6 = (Vortex)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }

   public WaveTrend WaveTrend(ChartData var1, int var2, int var3) throws TradingException {
      long var4 = this.Engine + -6366693378929283775L + var1.chartHashCode() + SQUtils.intsHash(new int[]{var2, var3});
      if (!this.indicatorsCache.containsKey(var4)) {
         WaveTrend var6 = new WaveTrend();
         var6.Chart = var1;
         var6.ChannelLength = var2;
         var6.AverageLength = var3;
         var6.initialize(this, this.hasZeroShift);
         var6.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var4, var6);
      }

      WaveTrend var7 = (WaveTrend)this.indicatorsCache.get(var4);
      var7.refreshShift();
      return var7;
   }

   public WilliamsPR WilliamsPR(ChartData var1, int var2) throws TradingException {
      long var3 = this.Engine + 9215265519793658407L + var1.chartHashCode() + var2;
      if (!this.indicatorsCache.containsKey(var3)) {
         WilliamsPR var5 = new WilliamsPR();
         var5.Chart = var1;
         var5.Period = var2;
         var5.initialize(this, this.hasZeroShift);
         var5.initializeStrategy(this.Strategy);
         this.indicatorsCache.put(var3, var5);
      }

      WilliamsPR var6 = (WilliamsPR)this.indicatorsCache.get(var3);
      var6.refreshShift();
      return var6;
   }
}
