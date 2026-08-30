package SQ.Internal;

import com.strategyquant.tradinglib.indicator.IndicatorsCache;
import com.strategyquant.tradinglib.indicator.IIndicatorsHolder;
import com.strategyquant.tradinglib.indicator.IndicatorsObj;
import SQ.Blocks.Functions.*;
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
import SQ.Calculators.*;
import SQ.Calculators.AverageCalculator;
import SQ.Calculators.HighestCalculator;
import SQ.Calculators.LowestCalculator;
import SQ.Calculators.RSICalculator;
import SQ.Calculators.StdDevCalculator;
import SQ.Calculators.SumCalculator;
import com.strategyquant.datalib.*;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TimeframeManager;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.datalib.dataseries.TimeDataSeries;
import com.strategyquant.lib.*;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.*;
import com.strategyquant.tradinglib.Buffer;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Colors;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Editors;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.IgnoreInBuilder;
import com.strategyquant.tradinglib.Indicator;
import com.strategyquant.tradinglib.NoShift;
import com.strategyquant.tradinglib.OppositeBlock;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.ReturnTypes;
import com.strategyquant.tradinglib.StrategyBase;
import com.strategyquant.tradinglib.simulator.Engines;
import static java.lang.Math.*;
import static java.lang.Math.cos;
import static java.lang.Math.exp;

/**
 * this is a cache class that caches all indicators used in a trading setup
 * @author Mark Fric
 */
 public class Indicators extends IndicatorsObj {
     private boolean hasZeroShift;

     public void setHasZeroShift(boolean hasZeroShift) {
         this.hasZeroShift = hasZeroShift;
     }

	 private StrategyBase Strategy;
	 public Indicators(StrategyBase strategy) {
		 this.Strategy = strategy;
	 }

	public ADX ADX(ChartData Input, int Period) throws TradingException {
		long key = this.Engine+(2649910181429105554L)+Input.chartHashCode()+Period;
		ADX indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new ADX();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (ADX) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public ATR ATR(ChartData Chart, int Period) throws TradingException {
		long key = this.Engine+(-9079487113293377728L)+Chart.chartHashCode()+Period;
		ATR indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new ATR();
			indicator.Chart = Chart;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (ATR) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public AnchoredVWAP AnchoredVWAP(ChartData Chart, int SessionType, double StdDevMult) throws TradingException {
		long key = this.Engine+(-3346673825811679000L)+Chart.chartHashCode()+SessionType+((int) (37*StdDevMult));
		AnchoredVWAP indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new AnchoredVWAP();
			indicator.Chart = Chart;
			indicator.SessionType = SessionType;
			indicator.StdDevMult = StdDevMult;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (AnchoredVWAP) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public Aroon Aroon(ChartData Chart, int Period) throws TradingException {
		long key = this.Engine+(5912219844429856438L)+Chart.chartHashCode()+Period;
		Aroon indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new Aroon();
			indicator.Chart = Chart;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (Aroon) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public AvgVolume AvgVolume(ChartData Chart, int Period) throws TradingException {
		long key = this.Engine+(3572842574091906926L)+Chart.chartHashCode()+Period;
		AvgVolume indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new AvgVolume();
			indicator.Chart = Chart;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (AvgVolume) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public AwesomeOscillator AwesomeOscillator(ChartData Input) throws TradingException {
		long key = this.Engine+(-1180824349519950413L)+Input.chartHashCode();
		AwesomeOscillator indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new AwesomeOscillator();
			indicator.Input = Input;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (AwesomeOscillator) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public BBRange BBRange(ChartData Chart, int Period, double Deviation, int ComputedFrom) throws TradingException {
		long key = this.Engine+(1893038667322575367L)+Chart.chartHashCode()+SQUtils.intsHash(Period,ComputedFrom)+((int) (37*Deviation));
		BBRange indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new BBRange();
			indicator.Chart = Chart;
			indicator.Period = Period;
			indicator.Deviation = Deviation;
			indicator.ComputedFrom = ComputedFrom;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (BBRange) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public BBWidthRatio BBWidthRatio(ChartData Input, int Period, double Deviation, int ComputedFrom) throws TradingException {
		long key = this.Engine+(-2041758268165272493L)+Input.chartHashCode()+SQUtils.intsHash(Period,ComputedFrom)+((int) (37*Deviation));
		BBWidthRatio indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new BBWidthRatio();
			indicator.Input = Input;
			indicator.Period = Period;
			indicator.Deviation = Deviation;
			indicator.ComputedFrom = ComputedFrom;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (BBWidthRatio) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public BarRange BarRange(ChartData Chart) throws TradingException {
		long key = this.Engine+(-9221856496902370897L)+Chart.chartHashCode();
		BarRange indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new BarRange();
			indicator.Chart = Chart;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (BarRange) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public BearsPower BearsPower(ChartData Input, int Period, int ComputedFrom) throws TradingException {
		long key = this.Engine+(-4241382397975899889L)+Input.chartHashCode()+SQUtils.intsHash(Period,ComputedFrom);
		BearsPower indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new BearsPower();
			indicator.Input = Input;
			indicator.Period = Period;
			indicator.ComputedFrom = ComputedFrom;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (BearsPower) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public BiggestRange BiggestRange(ChartData Input, int Period) throws TradingException {
		long key = this.Engine+(771253393487848173L)+Input.chartHashCode()+Period;
		BiggestRange indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new BiggestRange();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (BiggestRange) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public BollingerBands BollingerBands(DataSeries Input, int Period, double Deviation) throws TradingException {
		long key = this.Engine+(-7318124958737053846L)+Input.chartHashCode()+Period+((int) (37*Deviation));
		BollingerBands indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new BollingerBands();
			indicator.Input = Input;
			indicator.Period = Period;
			indicator.Deviation = Deviation;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (BollingerBands) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public BullsPower BullsPower(ChartData Input, int Period, int ComputedFrom) throws TradingException {
		long key = this.Engine+(5612962924337078367L)+Input.chartHashCode()+SQUtils.intsHash(Period,ComputedFrom);
		BullsPower indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new BullsPower();
			indicator.Input = Input;
			indicator.Period = Period;
			indicator.ComputedFrom = ComputedFrom;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (BullsPower) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public CCI CCI(DataSeries Input, int Period) throws TradingException {
		long key = this.Engine+(6145588920263969609L)+Input.chartHashCode()+Period;
		CCI indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new CCI();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (CCI) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public DataLoggingIndy DataLoggingIndy(TimeDataSeries Tinput, DataSeries Oinput, DataSeries Hinput, DataSeries Linput, DataSeries Cinput, DataSeries Vinput) throws TradingException {
		long key = this.Engine+(6747168366745394951L)+Tinput.hashCode()+Oinput.chartHashCode()+Hinput.chartHashCode()+Linput.chartHashCode()+Cinput.chartHashCode()+Vinput.chartHashCode();
		DataLoggingIndy indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new DataLoggingIndy();
			indicator.Tinput = Tinput;
			indicator.Oinput = Oinput;
			indicator.Hinput = Hinput;
			indicator.Linput = Linput;
			indicator.Cinput = Cinput;
			indicator.Vinput = Vinput;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (DataLoggingIndy) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public DeMarker DeMarker(ChartData Chart, int Period) throws TradingException {
		long key = this.Engine+(-1043877220955214314L)+Chart.chartHashCode()+Period;
		DeMarker indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new DeMarker();
			indicator.Chart = Chart;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (DeMarker) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public EMA EMA(DataSeries Input, int Period) throws TradingException {
		long key = this.Engine+(2318062612325506369L)+Input.chartHashCode()+Period;
		EMA indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new EMA();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (EMA) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public Fibo Fibo(ChartData Chart, int FiboRange, double FiboLevel) throws TradingException {
		long key = this.Engine+(4234090606858028209L)+Chart.chartHashCode()+FiboRange+((int) (37*FiboLevel));
		Fibo indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new Fibo();
			indicator.Chart = Chart;
			indicator.FiboRange = FiboRange;
			indicator.FiboLevel = FiboLevel;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (Fibo) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public Fractal Fractal(ChartData Chart, int Fractal) throws TradingException {
		long key = this.Engine+(471270077278654726L)+Chart.chartHashCode()+Fractal;
		Fractal indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new Fractal();
			indicator.Chart = Chart;
			indicator.Fractal = Fractal;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (Fractal) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public GannHiLo GannHiLo(ChartData Chart, int Period) throws TradingException {
		long key = this.Engine+(7159229647037632676L)+Chart.chartHashCode()+Period;
		GannHiLo indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new GannHiLo();
			indicator.Chart = Chart;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (GannHiLo) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public HeikenAshi HeikenAshi(ChartData Chart) throws TradingException {
		long key = this.Engine+(-5943901234518539156L)+Chart.chartHashCode();
		HeikenAshi indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new HeikenAshi();
			indicator.Chart = Chart;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (HeikenAshi) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public Highest Highest(DataSeries Input, int Period) throws TradingException {
		long key = this.Engine+(9153961714451168276L)+Input.chartHashCode()+Period;
		Highest indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new Highest();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (Highest) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public HighestInRange HighestInRange(ChartData Input, int TimeFrom, int TimeTo) throws TradingException {
		long key = this.Engine+(6612367740298608714L)+Input.chartHashCode()+SQUtils.intsHash(TimeFrom,TimeTo);
		HighestInRange indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new HighestInRange();
			indicator.Input = Input;
			indicator.TimeFrom = TimeFrom;
			indicator.TimeTo = TimeTo;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (HighestInRange) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public HighestIndex HighestIndex(DataSeries Input, int Period) throws TradingException {
		long key = this.Engine+(-5183852730716672033L)+Input.chartHashCode()+Period;
		HighestIndex indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new HighestIndex();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (HighestIndex) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public HullMovingAverage HullMovingAverage(DataSeries Input, int Period) throws TradingException {
		long key = this.Engine+(-7699848334758540619L)+Input.chartHashCode()+Period;
		HullMovingAverage indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new HullMovingAverage();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (HullMovingAverage) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public Ichimoku Ichimoku(ChartData Chart, int TenkanPeriod, int KijunPeriod, int SenkouPeriod) throws TradingException {
		long key = this.Engine+(-7097731500378219577L)+Chart.chartHashCode()+SQUtils.intsHash(TenkanPeriod,KijunPeriod,SenkouPeriod);
		Ichimoku indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new Ichimoku();
			indicator.Chart = Chart;
			indicator.TenkanPeriod = TenkanPeriod;
			indicator.KijunPeriod = KijunPeriod;
			indicator.SenkouPeriod = SenkouPeriod;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (Ichimoku) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public KAMA KAMA(ChartData Chart, int ERPeriod, int ShortPeriod, int LongPeriod) throws TradingException {
		long key = this.Engine+(8362797796748016681L)+Chart.chartHashCode()+SQUtils.intsHash(ERPeriod,ShortPeriod,LongPeriod);
		KAMA indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new KAMA();
			indicator.Chart = Chart;
			indicator.ERPeriod = ERPeriod;
			indicator.ShortPeriod = ShortPeriod;
			indicator.LongPeriod = LongPeriod;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (KAMA) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public KaufmanEfficiencyRatio KaufmanEfficiencyRatio(ChartData Chart, int Period) throws TradingException {
		long key = this.Engine+(-6296598236630776930L)+Chart.chartHashCode()+Period;
		KaufmanEfficiencyRatio indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new KaufmanEfficiencyRatio();
			indicator.Chart = Chart;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (KaufmanEfficiencyRatio) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public KeltnerChannel KeltnerChannel(ChartData Input, int Period, double Deviation) throws TradingException {
		long key = this.Engine+(-4250889032845815297L)+Input.chartHashCode()+Period+((int) (37*Deviation));
		KeltnerChannel indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new KeltnerChannel();
			indicator.Input = Input;
			indicator.Period = Period;
			indicator.Deviation = Deviation;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (KeltnerChannel) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public LWMA LWMA(DataSeries Input, int Period) throws TradingException {
		long key = this.Engine+(4622713547143785778L)+Input.chartHashCode()+Period;
		LWMA indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new LWMA();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (LWMA) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public LaguerreRSI LaguerreRSI(ChartData Chart, double Gamma) throws TradingException {
		long key = this.Engine+(-6895453554246873703L)+Chart.chartHashCode()+((int) (37*Gamma));
		LaguerreRSI indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new LaguerreRSI();
			indicator.Chart = Chart;
			indicator.Gamma = Gamma;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (LaguerreRSI) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public LinearRegression LinearRegression(DataSeries Input, int Period) throws TradingException {
		long key = this.Engine+(3205014151374460577L)+Input.chartHashCode()+Period;
		LinearRegression indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new LinearRegression();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (LinearRegression) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public Lowest Lowest(DataSeries Input, int Period) throws TradingException {
		long key = this.Engine+(-8981191732789932475L)+Input.chartHashCode()+Period;
		Lowest indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new Lowest();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (Lowest) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public LowestInRange LowestInRange(ChartData Input, int TimeFrom, int TimeTo) throws TradingException {
		long key = this.Engine+(-2087190515400894412L)+Input.chartHashCode()+SQUtils.intsHash(TimeFrom,TimeTo);
		LowestInRange indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new LowestInRange();
			indicator.Input = Input;
			indicator.TimeFrom = TimeFrom;
			indicator.TimeTo = TimeTo;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (LowestInRange) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public LowestIndex LowestIndex(DataSeries Input, int Period) throws TradingException {
		long key = this.Engine+(2022011787699968335L)+Input.chartHashCode()+Period;
		LowestIndex indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new LowestIndex();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (LowestIndex) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public MACD MACD(DataSeries Input, int Fast, int Slow, int Smooth) throws TradingException {
		long key = this.Engine+(5167397476533699938L)+Input.chartHashCode()+SQUtils.intsHash(Fast,Slow,Smooth);
		MACD indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new MACD();
			indicator.Input = Input;
			indicator.Fast = Fast;
			indicator.Slow = Slow;
			indicator.Smooth = Smooth;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (MACD) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public MTATR MTATR(ChartData Chart, int Period) throws TradingException {
		long key = this.Engine+(-7081782697645202279L)+Chart.chartHashCode()+Period;
		MTATR indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new MTATR();
			indicator.Chart = Chart;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (MTATR) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public MTKeltnerChannel MTKeltnerChannel(ChartData Input, int Period, double Deviation) throws TradingException {
		long key = this.Engine+(-3125520992811289534L)+Input.chartHashCode()+Period+((int) (37*Deviation));
		MTKeltnerChannel indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new MTKeltnerChannel();
			indicator.Input = Input;
			indicator.Period = Period;
			indicator.Deviation = Deviation;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (MTKeltnerChannel) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public Momentum Momentum(DataSeries Input, int Period) throws TradingException {
		long key = this.Engine+(8499554868961660647L)+Input.chartHashCode()+Period;
		Momentum indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new Momentum();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (Momentum) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public MovingAverage MovingAverage(DataSeries Input, int Period, int MAMethod) throws TradingException {
		long key = this.Engine+(-8322236142805281440L)+Input.chartHashCode()+SQUtils.intsHash(Period,MAMethod);
		MovingAverage indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new MovingAverage();
			indicator.Input = Input;
			indicator.Period = Period;
			indicator.MAMethod = MAMethod;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (MovingAverage) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public OSMA OSMA(DataSeries Input, int FastEMA, int SlowEMA, int SignalPeriod) throws TradingException {
		long key = this.Engine+(8755995658102603068L)+Input.chartHashCode()+SQUtils.intsHash(FastEMA,SlowEMA,SignalPeriod);
		OSMA indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new OSMA();
			indicator.Input = Input;
			indicator.FastEMA = FastEMA;
			indicator.SlowEMA = SlowEMA;
			indicator.SignalPeriod = SignalPeriod;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (OSMA) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public ParabolicSAR ParabolicSAR(ChartData Input, double Step, double Maximum) throws TradingException {
		long key = this.Engine+(-957642607645990205L)+Input.chartHashCode()+SQUtils.doublesHash(Step,Maximum);
		ParabolicSAR indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new ParabolicSAR();
			indicator.Input = Input;
			indicator.Step = Step;
			indicator.Maximum = Maximum;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (ParabolicSAR) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public Pivots Pivots(ChartData Chart, int StartHour, int StartMinute) throws TradingException {
		long key = this.Engine+(963150467221572839L)+Chart.chartHashCode()+SQUtils.intsHash(StartHour,StartMinute);
		Pivots indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new Pivots();
			indicator.Chart = Chart;
			indicator.StartHour = StartHour;
			indicator.StartMinute = StartMinute;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (Pivots) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public QQE QQE(ChartData Chart, int RSIPeriod, int sF, double wF) throws TradingException {
		long key = this.Engine+(-5774469501628734501L)+Chart.chartHashCode()+SQUtils.intsHash(RSIPeriod,sF)+((int) (37*wF));
		QQE indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new QQE();
			indicator.Chart = Chart;
			indicator.RSIPeriod = RSIPeriod;
			indicator.sF = sF;
			indicator.wF = wF;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (QQE) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public ROC ROC(ChartData Input, int Period) throws TradingException {
		long key = this.Engine+(9050015835207583142L)+Input.chartHashCode()+Period;
		ROC indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new ROC();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (ROC) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public RSI RSI(DataSeries Input, int Period) throws TradingException {
		long key = this.Engine+(4375848095962754478L)+Input.chartHashCode()+Period;
		RSI indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new RSI();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (RSI) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public Reflex Reflex(ChartData Input, int Period) throws TradingException {
		long key = this.Engine+(3942973377422065824L)+Input.chartHashCode()+Period;
		Reflex indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new Reflex();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (Reflex) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public SMA SMA(DataSeries Input, int Period) throws TradingException {
		long key = this.Engine+(-5078669517565619308L)+Input.chartHashCode()+Period;
		SMA indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new SMA();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (SMA) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public SMMA SMMA(DataSeries Input, int Period) throws TradingException {
		long key = this.Engine+(5761672542590343460L)+Input.chartHashCode()+Period;
		SMMA indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new SMMA();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (SMMA) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public SRPercentRank SRPercentRank(ChartData Chart, int Mode, int Length, int ATRPeriod) throws TradingException {
		long key = this.Engine+(4879138412592424510L)+Chart.chartHashCode()+SQUtils.intsHash(Mode,Length,ATRPeriod);
		SRPercentRank indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new SRPercentRank();
			indicator.Chart = Chart;
			indicator.Mode = Mode;
			indicator.Length = Length;
			indicator.ATRPeriod = ATRPeriod;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (SRPercentRank) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public SchaffTrendCycle SchaffTrendCycle(ChartData Input, int StochPeriod, int FastPeriod, int SlowPeriod) throws TradingException {
		long key = this.Engine+(3648820954109573374L)+Input.chartHashCode()+SQUtils.intsHash(StochPeriod,FastPeriod,SlowPeriod);
		SchaffTrendCycle indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new SchaffTrendCycle();
			indicator.Input = Input;
			indicator.StochPeriod = StochPeriod;
			indicator.FastPeriod = FastPeriod;
			indicator.SlowPeriod = SlowPeriod;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (SchaffTrendCycle) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public SmallestRange SmallestRange(ChartData Input, int Period) throws TradingException {
		long key = this.Engine+(1059494935463611065L)+Input.chartHashCode()+Period;
		SmallestRange indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new SmallestRange();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (SmallestRange) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public StdDev StdDev(DataSeries Input, int Period) throws TradingException {
		long key = this.Engine+(4208228923129027359L)+Input.chartHashCode()+Period;
		StdDev indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new StdDev();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (StdDev) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public Stochastic Stochastic(ChartData Input, int KPeriod, int DPeriod, int Slowing, int MAMethod, int PriceField) throws TradingException {
		long key = this.Engine+(2306031294302496378L)+Input.chartHashCode()+SQUtils.intsHash(KPeriod,DPeriod,Slowing,MAMethod,PriceField);
		Stochastic indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new Stochastic();
			indicator.Input = Input;
			indicator.KPeriod = KPeriod;
			indicator.DPeriod = DPeriod;
			indicator.Slowing = Slowing;
			indicator.MAMethod = MAMethod;
			indicator.PriceField = PriceField;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (Stochastic) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public SuperTrend SuperTrend(ChartData Input, int Mode, int ATRPeriod, double ATRMult) throws TradingException {
		long key = this.Engine+(-134614647241418174L)+Input.chartHashCode()+SQUtils.intsHash(Mode,ATRPeriod)+((int) (37*ATRMult));
		SuperTrend indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new SuperTrend();
			indicator.Input = Input;
			indicator.Mode = Mode;
			indicator.ATRPeriod = ATRPeriod;
			indicator.ATRMult = ATRMult;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (SuperTrend) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public TEMA TEMA(DataSeries Input, int Period) throws TradingException {
		long key = this.Engine+(8092699764052100094L)+Input.chartHashCode()+Period;
		TEMA indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new TEMA();
			indicator.Input = Input;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (TEMA) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public TPOProfile TPOProfile(ChartData Chart, int SessionType, int ProfileRows, int BinSizeMode, int TicksPerBin, double ValueAreaPct, boolean UseCustomHours, int StartHour, int StartMinute, int EndHour, int EndMinute, int BracketMinDaily, int BracketMinWeekly, int BracketMinMonthly, int BracketMinYearly, boolean ShowCandlesticks, boolean ShowShapeLabel, boolean UseBlockMode) throws TradingException {
		long key = this.Engine+(-4769500882741990806L)+Chart.chartHashCode()+SQUtils.intsHash(SessionType,ProfileRows,BinSizeMode,TicksPerBin,StartHour,StartMinute,EndHour,EndMinute,BracketMinDaily,BracketMinWeekly,BracketMinMonthly,BracketMinYearly)+((int) (37*ValueAreaPct))+SQUtils.booleansHash(UseCustomHours,ShowCandlesticks,ShowShapeLabel,UseBlockMode);
		TPOProfile indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new TPOProfile();
			indicator.Chart = Chart;
			indicator.SessionType = SessionType;
			indicator.ProfileRows = ProfileRows;
			indicator.BinSizeMode = BinSizeMode;
			indicator.TicksPerBin = TicksPerBin;
			indicator.ValueAreaPct = ValueAreaPct;
			indicator.UseCustomHours = UseCustomHours;
			indicator.StartHour = StartHour;
			indicator.StartMinute = StartMinute;
			indicator.EndHour = EndHour;
			indicator.EndMinute = EndMinute;
			indicator.BracketMinDaily = BracketMinDaily;
			indicator.BracketMinWeekly = BracketMinWeekly;
			indicator.BracketMinMonthly = BracketMinMonthly;
			indicator.BracketMinYearly = BracketMinYearly;
			indicator.ShowCandlesticks = ShowCandlesticks;
			indicator.ShowShapeLabel = ShowShapeLabel;
			indicator.UseBlockMode = UseBlockMode;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (TPOProfile) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public TrueRange TrueRange(ChartData Chart) throws TradingException {
		long key = this.Engine+(7822877233330299681L)+Chart.chartHashCode();
		TrueRange indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new TrueRange();
			indicator.Chart = Chart;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (TrueRange) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public UlcerIndex UlcerIndex(ChartData Input, int Mode, int Period) throws TradingException {
		long key = this.Engine+(-2053987696432323927L)+Input.chartHashCode()+SQUtils.intsHash(Mode,Period);
		UlcerIndex indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new UlcerIndex();
			indicator.Input = Input;
			indicator.Mode = Mode;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (UlcerIndex) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public VWAP VWAP(ChartData Input, int VWAPPeriod) throws TradingException {
		long key = this.Engine+(-8239802216863368915L)+Input.chartHashCode()+VWAPPeriod;
		VWAP indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new VWAP();
			indicator.Input = Input;
			indicator.VWAPPeriod = VWAPPeriod;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (VWAP) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public VolumeProfile VolumeProfile(ChartData Chart, int SessionType, int ProfileRows, int BinSizeMode, int TicksPerBin, double ValueAreaPct, int IBMinutes, int HvnCount, int HvnThresholdPct, int LvnThresholdPct, boolean EnableLVN, boolean EnableVCP, double ClusterSpread, int MaxClusterCenters, int PivotMethod, double PivotPct, int PivotTicks, double PivotATRMultiple, int PivotATRPeriod, boolean ShowCandlesticks, boolean ShowVolumeSubchart, int VolumeMALength, boolean ShowPOCDelta, boolean ShowVADelta, boolean ShowProfileRange, boolean ShowPOCPosition, boolean ShowDeltaPerLevel, boolean ShowZigZagLine) throws TradingException {
		long key = this.Engine+(-2777649303129309491L)+Chart.chartHashCode()+SQUtils.intsHash(SessionType,ProfileRows,BinSizeMode,TicksPerBin,IBMinutes,HvnCount,HvnThresholdPct,LvnThresholdPct,MaxClusterCenters,PivotMethod,PivotTicks,PivotATRPeriod,VolumeMALength)+SQUtils.doublesHash(ValueAreaPct,ClusterSpread,PivotPct,PivotATRMultiple)+SQUtils.booleansHash(EnableLVN,EnableVCP,ShowCandlesticks,ShowVolumeSubchart,ShowPOCDelta,ShowVADelta,ShowProfileRange,ShowPOCPosition,ShowDeltaPerLevel,ShowZigZagLine);
		VolumeProfile indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new VolumeProfile();
			indicator.Chart = Chart;
			indicator.SessionType = SessionType;
			indicator.ProfileRows = ProfileRows;
			indicator.BinSizeMode = BinSizeMode;
			indicator.TicksPerBin = TicksPerBin;
			indicator.ValueAreaPct = ValueAreaPct;
			indicator.IBMinutes = IBMinutes;
			indicator.HvnCount = HvnCount;
			indicator.HvnThresholdPct = HvnThresholdPct;
			indicator.LvnThresholdPct = LvnThresholdPct;
			indicator.EnableLVN = EnableLVN;
			indicator.EnableVCP = EnableVCP;
			indicator.ClusterSpread = ClusterSpread;
			indicator.MaxClusterCenters = MaxClusterCenters;
			indicator.PivotMethod = PivotMethod;
			indicator.PivotPct = PivotPct;
			indicator.PivotTicks = PivotTicks;
			indicator.PivotATRMultiple = PivotATRMultiple;
			indicator.PivotATRPeriod = PivotATRPeriod;
			indicator.ShowCandlesticks = ShowCandlesticks;
			indicator.ShowVolumeSubchart = ShowVolumeSubchart;
			indicator.VolumeMALength = VolumeMALength;
			indicator.ShowPOCDelta = ShowPOCDelta;
			indicator.ShowVADelta = ShowVADelta;
			indicator.ShowProfileRange = ShowProfileRange;
			indicator.ShowPOCPosition = ShowPOCPosition;
			indicator.ShowDeltaPerLevel = ShowDeltaPerLevel;
			indicator.ShowZigZagLine = ShowZigZagLine;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (VolumeProfile) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public VolumeProfileCustomHours VolumeProfileCustomHours(ChartData Chart, int SessionStartHours, int SessionStartMinutes, int SessionEndHours, int SessionEndMinutes, int SessionMode, int ProfileRows, int BinSizeMode, int TicksPerBin, double ValueAreaPct, int HvnCount, int HvnThresholdPct, int LvnThresholdPct, boolean EnableLVN, boolean EnableVCP, double ClusterSpread, int MaxClusterCenters, int IBMinutes, boolean ShowCandlesticks, boolean ShowVolumeSubchart, int VolumeMALength, boolean ShowPOCDelta, boolean ShowVADelta, boolean ShowProfileRange, boolean ShowPOCPosition, boolean ShowDeltaPerLevel) throws TradingException {
		long key = this.Engine+(2364873747930282726L)+Chart.chartHashCode()+SQUtils.intsHash(SessionStartHours,SessionStartMinutes,SessionEndHours,SessionEndMinutes,SessionMode,ProfileRows,BinSizeMode,TicksPerBin,HvnCount,HvnThresholdPct,LvnThresholdPct,MaxClusterCenters,IBMinutes,VolumeMALength)+SQUtils.doublesHash(ValueAreaPct,ClusterSpread)+SQUtils.booleansHash(EnableLVN,EnableVCP,ShowCandlesticks,ShowVolumeSubchart,ShowPOCDelta,ShowVADelta,ShowProfileRange,ShowPOCPosition,ShowDeltaPerLevel);
		VolumeProfileCustomHours indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new VolumeProfileCustomHours();
			indicator.Chart = Chart;
			indicator.SessionStartHours = SessionStartHours;
			indicator.SessionStartMinutes = SessionStartMinutes;
			indicator.SessionEndHours = SessionEndHours;
			indicator.SessionEndMinutes = SessionEndMinutes;
			indicator.SessionMode = SessionMode;
			indicator.ProfileRows = ProfileRows;
			indicator.BinSizeMode = BinSizeMode;
			indicator.TicksPerBin = TicksPerBin;
			indicator.ValueAreaPct = ValueAreaPct;
			indicator.HvnCount = HvnCount;
			indicator.HvnThresholdPct = HvnThresholdPct;
			indicator.LvnThresholdPct = LvnThresholdPct;
			indicator.EnableLVN = EnableLVN;
			indicator.EnableVCP = EnableVCP;
			indicator.ClusterSpread = ClusterSpread;
			indicator.MaxClusterCenters = MaxClusterCenters;
			indicator.IBMinutes = IBMinutes;
			indicator.ShowCandlesticks = ShowCandlesticks;
			indicator.ShowVolumeSubchart = ShowVolumeSubchart;
			indicator.VolumeMALength = VolumeMALength;
			indicator.ShowPOCDelta = ShowPOCDelta;
			indicator.ShowVADelta = ShowVADelta;
			indicator.ShowProfileRange = ShowProfileRange;
			indicator.ShowPOCPosition = ShowPOCPosition;
			indicator.ShowDeltaPerLevel = ShowDeltaPerLevel;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (VolumeProfileCustomHours) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public VolumeProfileCustomHoursMultiSession VolumeProfileCustomHoursMultiSession(ChartData Chart, boolean EnableLondon, int LondonStartHour, int LondonStartMin, int LondonEndHour, int LondonEndMin, boolean EnableNewYork, int NewYorkStartHour, int NewYorkStartMin, int NewYorkEndHour, int NewYorkEndMin, boolean EnableSydney, int SydneyStartHour, int SydneyStartMin, int SydneyEndHour, int SydneyEndMin, boolean EnableTokyo, int TokyoStartHour, int TokyoStartMin, int TokyoEndHour, int TokyoEndMin, int SessionMode, int ProfileRows, int BinSizeMode, int TicksPerBin, double ValueAreaPct, int HvnCount, int HvnThresholdPct, int LvnThresholdPct, boolean EnableLVN, boolean EnableVCP, double ClusterSpread, int MaxClusterCenters, int IBMinutes, boolean ShowCandlesticks, boolean ShowVolumeSubchart, int VolumeMALength, boolean ShowPOCDelta, boolean ShowVADelta, boolean ShowProfileRange, boolean ShowPOCPosition, boolean ShowDeltaPerLevel) throws TradingException {
		long key = this.Engine+(-243479629652240495L)+Chart.chartHashCode()+SQUtils.intsHash(LondonStartHour,LondonStartMin,LondonEndHour,LondonEndMin,NewYorkStartHour,NewYorkStartMin,NewYorkEndHour,NewYorkEndMin,SydneyStartHour,SydneyStartMin,SydneyEndHour,SydneyEndMin,TokyoStartHour,TokyoStartMin,TokyoEndHour,TokyoEndMin,SessionMode,ProfileRows,BinSizeMode,TicksPerBin,HvnCount,HvnThresholdPct,LvnThresholdPct,MaxClusterCenters,IBMinutes,VolumeMALength)+SQUtils.doublesHash(ValueAreaPct,ClusterSpread)+SQUtils.booleansHash(EnableLondon,EnableNewYork,EnableSydney,EnableTokyo,EnableLVN,EnableVCP,ShowCandlesticks,ShowVolumeSubchart,ShowPOCDelta,ShowVADelta,ShowProfileRange,ShowPOCPosition,ShowDeltaPerLevel);
		VolumeProfileCustomHoursMultiSession indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new VolumeProfileCustomHoursMultiSession();
			indicator.Chart = Chart;
			indicator.EnableLondon = EnableLondon;
			indicator.LondonStartHour = LondonStartHour;
			indicator.LondonStartMin = LondonStartMin;
			indicator.LondonEndHour = LondonEndHour;
			indicator.LondonEndMin = LondonEndMin;
			indicator.EnableNewYork = EnableNewYork;
			indicator.NewYorkStartHour = NewYorkStartHour;
			indicator.NewYorkStartMin = NewYorkStartMin;
			indicator.NewYorkEndHour = NewYorkEndHour;
			indicator.NewYorkEndMin = NewYorkEndMin;
			indicator.EnableSydney = EnableSydney;
			indicator.SydneyStartHour = SydneyStartHour;
			indicator.SydneyStartMin = SydneyStartMin;
			indicator.SydneyEndHour = SydneyEndHour;
			indicator.SydneyEndMin = SydneyEndMin;
			indicator.EnableTokyo = EnableTokyo;
			indicator.TokyoStartHour = TokyoStartHour;
			indicator.TokyoStartMin = TokyoStartMin;
			indicator.TokyoEndHour = TokyoEndHour;
			indicator.TokyoEndMin = TokyoEndMin;
			indicator.SessionMode = SessionMode;
			indicator.ProfileRows = ProfileRows;
			indicator.BinSizeMode = BinSizeMode;
			indicator.TicksPerBin = TicksPerBin;
			indicator.ValueAreaPct = ValueAreaPct;
			indicator.HvnCount = HvnCount;
			indicator.HvnThresholdPct = HvnThresholdPct;
			indicator.LvnThresholdPct = LvnThresholdPct;
			indicator.EnableLVN = EnableLVN;
			indicator.EnableVCP = EnableVCP;
			indicator.ClusterSpread = ClusterSpread;
			indicator.MaxClusterCenters = MaxClusterCenters;
			indicator.IBMinutes = IBMinutes;
			indicator.ShowCandlesticks = ShowCandlesticks;
			indicator.ShowVolumeSubchart = ShowVolumeSubchart;
			indicator.VolumeMALength = VolumeMALength;
			indicator.ShowPOCDelta = ShowPOCDelta;
			indicator.ShowVADelta = ShowVADelta;
			indicator.ShowProfileRange = ShowProfileRange;
			indicator.ShowPOCPosition = ShowPOCPosition;
			indicator.ShowDeltaPerLevel = ShowDeltaPerLevel;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (VolumeProfileCustomHoursMultiSession) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public Vortex Vortex(ChartData Chart, int Period) throws TradingException {
		long key = this.Engine+(-6079528525011855141L)+Chart.chartHashCode()+Period;
		Vortex indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new Vortex();
			indicator.Chart = Chart;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (Vortex) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public WaveTrend WaveTrend(ChartData Chart, int ChannelLength, int AverageLength) throws TradingException {
		long key = this.Engine+(-6366693378929283775L)+Chart.chartHashCode()+SQUtils.intsHash(ChannelLength,AverageLength);
		WaveTrend indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new WaveTrend();
			indicator.Chart = Chart;
			indicator.ChannelLength = ChannelLength;
			indicator.AverageLength = AverageLength;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (WaveTrend) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

	public WilliamsPR WilliamsPR(ChartData Chart, int Period) throws TradingException {
		long key = this.Engine+(9215265519793658407L)+Chart.chartHashCode()+Period;
		WilliamsPR indicator;

		if(!indicatorsCache.containsKey(key)) {
			indicator = new WilliamsPR();
			indicator.Chart = Chart;
			indicator.Period = Period;

			indicator.initialize(this, hasZeroShift);
			indicator.initializeStrategy(Strategy);

			indicatorsCache.put(key, indicator);
		}

		indicator = (WilliamsPR) indicatorsCache.get(key);

		indicator.refreshShift();

		return indicator;
	}

}