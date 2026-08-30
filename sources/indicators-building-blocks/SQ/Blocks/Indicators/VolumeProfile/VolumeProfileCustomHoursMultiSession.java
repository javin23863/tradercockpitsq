package SQ.Blocks.Indicators.VolumeProfile;

import SQ.Internal.VolumeProfileIndicatorChart;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;

@BuildingBlock(
   name = "(VP) Volume Profile Custom Hours MultiSession",
   display = "VolumeProfileCHMulti(#ProfileRows#,#ValueAreaPct#).#Line#[#Shift#]",
   returnType = 2
)
@Help(
   "Volume Profile with 4 configurable sessions (London, New York, Sydney, Tokyo). Each session can be enabled/disabled independently and outputs its own POC/VAH/VAL."
)
@ForEngine("MT4,MT5,TS,MC")
@ParameterSet(set = "EnableLondon=true,EnableNewYork=true,EnableSydney=false,EnableTokyo=true,ProfileRows=100,ValueAreaPct=70")
public class VolumeProfileCustomHoursMultiSession extends VolumeProfileIndicatorChart {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(defaultValue = "true", name = "EnableLondon")
   @Help("Enable London session profile")
   public boolean EnableLondon;
   @Parameter(defaultValue = "7", name = "LondonStartHour", minValue = 0.0, maxValue = 23.0, step = 1.0)
   @Help("London session start hour (0-23)")
   public int LondonStartHour;
   @Parameter(defaultValue = "0", name = "LondonStartMin", minValue = 0.0, maxValue = 59.0, step = 1.0)
   @Help("London session start minutes (0-59)")
   public int LondonStartMin;
   @Parameter(defaultValue = "16", name = "LondonEndHour", minValue = 0.0, maxValue = 23.0, step = 1.0)
   @Help("London session end hour (0-23)")
   public int LondonEndHour;
   @Parameter(defaultValue = "0", name = "LondonEndMin", minValue = 0.0, maxValue = 59.0, step = 1.0)
   @Help("London session end minutes (0-59)")
   public int LondonEndMin;
   @Parameter(defaultValue = "true", name = "EnableNewYork")
   @Help("Enable New York session profile")
   public boolean EnableNewYork;
   @Parameter(defaultValue = "13", name = "NewYorkStartHour", minValue = 0.0, maxValue = 23.0, step = 1.0)
   @Help("New York session start hour (0-23)")
   public int NewYorkStartHour;
   @Parameter(defaultValue = "0", name = "NewYorkStartMin", minValue = 0.0, maxValue = 59.0, step = 1.0)
   @Help("New York session start minutes (0-59)")
   public int NewYorkStartMin;
   @Parameter(defaultValue = "22", name = "NewYorkEndHour", minValue = 0.0, maxValue = 23.0, step = 1.0)
   @Help("New York session end hour (0-23)")
   public int NewYorkEndHour;
   @Parameter(defaultValue = "0", name = "NewYorkEndMin", minValue = 0.0, maxValue = 59.0, step = 1.0)
   @Help("New York session end minutes (0-59)")
   public int NewYorkEndMin;
   @Parameter(defaultValue = "false", name = "EnableSydney")
   @Help("Enable Sydney session profile")
   public boolean EnableSydney;
   @Parameter(defaultValue = "21", name = "SydneyStartHour", minValue = 0.0, maxValue = 23.0, step = 1.0)
   @Help("Sydney session start hour (0-23)")
   public int SydneyStartHour;
   @Parameter(defaultValue = "0", name = "SydneyStartMin", minValue = 0.0, maxValue = 59.0, step = 1.0)
   @Help("Sydney session start minutes (0-59)")
   public int SydneyStartMin;
   @Parameter(defaultValue = "6", name = "SydneyEndHour", minValue = 0.0, maxValue = 23.0, step = 1.0)
   @Help("Sydney session end hour (0-23)")
   public int SydneyEndHour;
   @Parameter(defaultValue = "0", name = "SydneyEndMin", minValue = 0.0, maxValue = 59.0, step = 1.0)
   @Help("Sydney session end minutes (0-59)")
   public int SydneyEndMin;
   @Parameter(defaultValue = "true", name = "EnableTokyo")
   @Help("Enable Tokyo session profile")
   public boolean EnableTokyo;
   @Parameter(defaultValue = "0", name = "TokyoStartHour", minValue = 0.0, maxValue = 23.0, step = 1.0)
   @Help("Tokyo session start hour (0-23)")
   public int TokyoStartHour;
   @Parameter(defaultValue = "0", name = "TokyoStartMin", minValue = 0.0, maxValue = 59.0, step = 1.0)
   @Help("Tokyo session start minutes (0-59)")
   public int TokyoStartMin;
   @Parameter(defaultValue = "9", name = "TokyoEndHour", minValue = 0.0, maxValue = 23.0, step = 1.0)
   @Help("Tokyo session end hour (0-23)")
   public int TokyoEndHour;
   @Parameter(defaultValue = "0", name = "TokyoEndMin", minValue = 0.0, maxValue = 59.0, step = 1.0)
   @Help("Tokyo session end minutes (0-59)")
   public int TokyoEndMin;
   @Parameter(defaultValue = "1", name = "SessionMode")
   @Help("Previous = last completed session. Actual = current developing session.")
   @Editor(type = 40, values = "Previous=1,Actual=2")
   public int SessionMode;
   @Parameter(defaultValue = "150", name = "ProfileRows", minValue = 10.0, maxValue = 500.0, step = 10.0)
   @Help("Number of price levels (bins) in the profile (used in Range-Based mode)")
   public int ProfileRows;
   @Parameter(defaultValue = "2", name = "BinSizeMode")
   @Help("1=Range-Based: bin size = range/ProfileRows. 2=Fixed: bin size = TicksPerBin x tick size.")
   @Editor(type = 40, values = "Range-Based=1,Fixed Tick Size=2")
   public int BinSizeMode;
   @Parameter(defaultValue = "3", name = "TicksPerBin", minValue = 1.0, maxValue = 1000.0, step = 1.0)
   @Help("Number of ticks per bin (used only in Fixed Tick Size mode)")
   public int TicksPerBin;
   @Parameter(defaultValue = "70", name = "ValueAreaPct", minValue = 30.0, maxValue = 95.0, step = 5.0)
   @Help("Percentage of total volume that defines the Value Area")
   public double ValueAreaPct;
   @Parameter(defaultValue = "5", name = "HvnCount", minValue = 1.0, maxValue = 10.0, step = 1.0)
   @Help("Number of High Volume Nodes (local peaks) to detect in the profile")
   public int HvnCount;
   @Parameter(defaultValue = "20", name = "HvnThresholdPct", minValue = 10.0, maxValue = 90.0, step = 5.0)
   @Help("Minimum volume as % of max bin volume for a local peak to qualify as HVN")
   public int HvnThresholdPct;
   @Parameter(defaultValue = "40", name = "LvnThresholdPct", minValue = 10.0, maxValue = 90.0, step = 5.0)
   @Help("Maximum volume as % of max bin volume for a local valley to qualify as LVN")
   public int LvnThresholdPct;
   @Parameter(defaultValue = "false", name = "EnableLVN")
   @Help("If true, detect Low Volume Nodes (LVN1-LVN5). If false, LVN outputs remain 0.")
   public boolean EnableLVN;
   @Parameter(defaultValue = "false", name = "EnableVolumeCluster")
   @Help("Enable Volume Cluster Profile: Gaussian-enhanced clustering that smooths the profile around dominant volume peaks")
   public boolean EnableVCP;
   @Parameter(defaultValue = "6.0", name = "VolumeClusterSpread", minValue = 0.5, maxValue = 20.0, step = 0.5)
   @Help("Gaussian sigma controlling how wide each cluster center's influence reaches (higher = smoother/wider)")
   public double ClusterSpread;
   @Parameter(defaultValue = "2", name = "MaxVolumeClusterCenters", minValue = 1.0, maxValue = 10.0, step = 1.0)
   @Help("Maximum number of cluster peaks used in Gaussian enhancement")
   public int MaxClusterCenters;
   @Parameter(defaultValue = "0", name = "IBMinutes", minValue = 0.0, maxValue = 14400.0, step = 1.0)
   @Help("Initial Balance period override in minutes. 0 = use default (60min). Values > 0 override the default.")
   public int IBMinutes;
   @Parameter(defaultValue = "true", name = "ShowCandlesticks", showIfDefault = false)
   @Help("If true, show candlestick price chart alongside each session profile in SVG export")
   public boolean ShowCandlesticks;
   @Parameter(defaultValue = "false", name = "ShowVolumeSubchart", showIfDefault = false)
   @Help("If true, show a volume bar subplot below the main chart in SVG export")
   public boolean ShowVolumeSubchart;
   @Parameter(defaultValue = "20", name = "VolumeMALength", minValue = 2.0, maxValue = 200.0, step = 1.0, showIfDefault = false)
   @Help("Period for the moving average line overlaid on the volume subchart")
   public int VolumeMALength;
   @Parameter(defaultValue = "true", name = "ShowPOCDelta", showIfDefault = false)
   @Help("Show POC price change vs previous session in SVG export")
   public boolean ShowPOCDelta;
   @Parameter(defaultValue = "true", name = "ShowVADelta", showIfDefault = false)
   @Help("Show Value Area midpoint change vs previous session in SVG export")
   public boolean ShowVADelta;
   @Parameter(defaultValue = "true", name = "ShowProfileRange", showIfDefault = false)
   @Help("Show session high-low range in SVG export")
   public boolean ShowProfileRange;
   @Parameter(defaultValue = "true", name = "ShowPOCPosition", showIfDefault = false)
   @Help("Show POC position as pct from top and bottom of profile range in SVG export")
   public boolean ShowPOCPosition;
   @Parameter(defaultValue = "false", name = "ShowDeltaPerLevel", showIfDefault = false)
   @Help("If true, show per-level delta (bull minus bear) labels on each bin of the volume profile in SVG export")
   public boolean ShowDeltaPerLevel;
   @Output(name = "POC", color = "#FFFF00")
   public DataSeries POC;
   @Output(name = "VAH", color = "#008000")
   public DataSeries VAH;
   @Output(name = "VAL", color = "#FF0000")
   public DataSeries VAL;
   @Output(name = "IBH", color = "#F0FFFF", show = false)
   public DataSeries IBH;
   @Output(name = "IBL", color = "#FF00FF", show = false)
   public DataSeries IBL;
   @Output(name = "HVN1", color = "#800080", show = false)
   public DataSeries HVN1;
   @Output(name = "HVN2", color = "#800080", show = false)
   public DataSeries HVN2;
   @Output(name = "HVN3", color = "#800080", show = false)
   public DataSeries HVN3;
   @Output(name = "HVN4", color = "#800080", show = false)
   public DataSeries HVN4;
   @Output(name = "HVN5", color = "#800080", show = false)
   public DataSeries HVN5;
   @Output(name = "LVN1", color = "#FFA500", show = false)
   public DataSeries LVN1;
   @Output(name = "LVN2", color = "#FFA500", show = false)
   public DataSeries LVN2;
   @Output(name = "LVN3", color = "#FFA500", show = false)
   public DataSeries LVN3;
   @Output(name = "LVN4", color = "#FFA500", show = false)
   public DataSeries LVN4;
   @Output(name = "LVN5", color = "#FFA500", show = false)
   public DataSeries LVN5;
   @Output(name = "VPOC", color = "#F0FFFF", show = false)
   public DataSeries VPOC;
   @Output(name = "VVAH", color = "#90EE90", show = false)
   public DataSeries VVAH;
   @Output(name = "VVAL", color = "#F5F5DC", show = false)
   public DataSeries VVAL;
   @Output(name = "BullPOC", color = "#008000", show = false)
   public DataSeries BullPOC;
   @Output(name = "BearPOC", color = "#FF0000", show = false)
   public DataSeries BearPOC;
   @Output(name = "TotalVolume", color = "#808080", show = false)
   public DataSeries TotalVolume;
   @Output(name = "TotalBullVolume", color = "#008000", show = false)
   public DataSeries TotalBullVolume;
   @Output(name = "TotalBearVolume", color = "#FF0000", show = false)
   public DataSeries TotalBearVolume;
   @Output(name = "Delta", color = "#F0FFFF", show = false)
   public DataSeries Delta;
   @Output(name = "DeltaPOC", color = "#F0FFFF", show = false)
   public DataSeries DeltaPOC;
   @Output(name = "DeltaVA", color = "#F0FFFF", show = false)
   public DataSeries DeltaVA;
   @Output(name = "ProfileRange", color = "#808080", show = false)
   public DataSeries ProfileRange;
   @Output(name = "POCPctUp", color = "#808080", show = false)
   public DataSeries POCPctUp;
   @Output(name = "POCPctDown", color = "#808080", show = false)
   public DataSeries POCPctDown;
   @Output(name = "LondonPOC", color = "#FFFF00", show = false)
   public DataSeries LondonPOC;
   @Output(name = "LondonVAH", color = "#008000", show = false)
   public DataSeries LondonVAH;
   @Output(name = "LondonVAL", color = "#FF0000", show = false)
   public DataSeries LondonVAL;
   @Output(name = "NewYorkPOC", color = "#FFFF00", show = false)
   public DataSeries NewYorkPOC;
   @Output(name = "NewYorkVAH", color = "#008000", show = false)
   public DataSeries NewYorkVAH;
   @Output(name = "NewYorkVAL", color = "#FF0000", show = false)
   public DataSeries NewYorkVAL;
   @Output(name = "SydneyPOC", color = "#FFFF00", show = false)
   public DataSeries SydneyPOC;
   @Output(name = "SydneyVAH", color = "#008000", show = false)
   public DataSeries SydneyVAH;
   @Output(name = "SydneyVAL", color = "#FF0000", show = false)
   public DataSeries SydneyVAL;
   @Output(name = "TokyoPOC", color = "#FFFF00", show = false)
   public DataSeries TokyoPOC;
   @Output(name = "TokyoVAH", color = "#008000", show = false)
   public DataSeries TokyoVAH;
   @Output(name = "TokyoVAL", color = "#FF0000", show = false)
   public DataSeries TokyoVAL;
   private static final int NUM_SESSIONS = 4;
   private static final int S_LONDON = 0;
   private static final int S_NEWYORK = 1;
   private static final int S_SYDNEY = 2;
   private static final int S_TOKYO = 3;
   private static final String[] SESSION_NAMES = new String[]{"London", "NewYork", "Sydney", "Tokyo"};
   private static final String[] SESSION_COLORS = new String[]{"#2196f3", "#ff9800", "#4caf50", "#e040fb"};
   private long[] currentSessionStart = new long[4];
   private long[] currentSessionEnd = new long[4];
   private long[] prevSessionStart = new long[4];
   private long[] prevSessionEnd = new long[4];
   private double[] prevPOC = new double[4];
   private double[] prevVAH = new double[4];
   private double[] prevVAL = new double[4];
   private double[] prevIBH = new double[4];
   private double[] prevIBL = new double[4];
   private double[][] prevHVN = new double[4][5];
   private double[][] prevLVN = new double[4][5];
   private double[] prevVPOC = new double[4];
   private double[] prevVVAH = new double[4];
   private double[] prevVVAL = new double[4];
   private double[] prevBullPOC = new double[4];
   private double[] prevBearPOC = new double[4];
   private double[] prevTotalVolume = new double[4];
   private double[] prevTotalBullVolume = new double[4];
   private double[] prevTotalBearVolume = new double[4];
   private double[] prevDelta = new double[4];
   private double[] prevDeltaPOC = new double[4];
   private double[] prevDeltaVA = new double[4];
   private double[] prevRange = new double[4];
   private double[] prevPOCPctUp = new double[4];
   private double[] prevPOCPctDown = new double[4];
   private double[] volumeBins;
   private double[] bullVolumeBins;
   private double[] bearVolumeBins;
   private double[] clusterBins;
   private int M1chartNumber = -1;
   private int lastNumBins = 0;
   private int historyCount = 0;
   private long[] histSessionStart;
   private long[] histSessionEnd;
   private double[] histPOC;
   private double[] histVAH;
   private double[] histVAL;
   private double[] histIBH;
   private double[] histIBL;
   private double[] histSessionHigh;
   private double[] histSessionLow;
   private double[][] histVolumeBins;
   private double[][] histBullBins;
   private double[][] histBearBins;
   private double[][] histHVN;
   private double[][] histLVN;
   private int[] histNumBins;
   private double[] histTotalVolume;
   private double[] histBullVolume;
   private double[] histBearVolume;
   private int activeSes = 0;
   private String[] histSessionType;

   private void copyToParentScalars(int var1) {
      super.prevSessionStart = this.prevSessionStart[var1];
      super.prevSessionEnd = this.prevSessionEnd[var1];
      super.prevPOC = this.prevPOC[var1];
      super.prevVAH = this.prevVAH[var1];
      super.prevVAL = this.prevVAL[var1];
      super.prevIBH = this.prevIBH[var1];
      super.prevIBL = this.prevIBL[var1];

      for (int var2 = 0; var2 < 5; var2++) {
         super.prevHVN[var2] = this.prevHVN[var1][var2];
         super.prevLVN[var2] = this.prevLVN[var1][var2];
      }

      super.prevVPOC = this.prevVPOC[var1];
      super.prevVVAH = this.prevVVAH[var1];
      super.prevVVAL = this.prevVVAL[var1];
      super.prevBullPOC = this.prevBullPOC[var1];
      super.prevBearPOC = this.prevBearPOC[var1];
      super.prevTotalVolume = this.prevTotalVolume[var1];
      super.prevTotalBullVolume = this.prevTotalBullVolume[var1];
      super.prevTotalBearVolume = this.prevTotalBearVolume[var1];
      super.prevDelta = this.prevDelta[var1];
      super.prevDeltaPOC = this.prevDeltaPOC[var1];
      super.prevDeltaVA = this.prevDeltaVA[var1];
      super.prevRange = this.prevRange[var1];
      super.prevPOCPctUp = this.prevPOCPctUp[var1];
      super.prevPOCPctDown = this.prevPOCPctDown[var1];
   }

   private void copyFromParentScalars(int var1) {
      this.prevPOC[var1] = super.prevPOC;
      this.prevVAH[var1] = super.prevVAH;
      this.prevVAL[var1] = super.prevVAL;
      this.prevIBH[var1] = super.prevIBH;
      this.prevIBL[var1] = super.prevIBL;

      for (int var2 = 0; var2 < 5; var2++) {
         this.prevHVN[var1][var2] = super.prevHVN[var2];
         this.prevLVN[var1][var2] = super.prevLVN[var2];
      }

      this.prevVPOC[var1] = super.prevVPOC;
      this.prevVVAH[var1] = super.prevVVAH;
      this.prevVVAL[var1] = super.prevVVAL;
      this.prevBullPOC[var1] = super.prevBullPOC;
      this.prevBearPOC[var1] = super.prevBearPOC;
      this.prevTotalVolume[var1] = super.prevTotalVolume;
      this.prevTotalBullVolume[var1] = super.prevTotalBullVolume;
      this.prevTotalBearVolume[var1] = super.prevTotalBearVolume;
      this.prevDelta[var1] = super.prevDelta;
      this.prevDeltaPOC[var1] = super.prevDeltaPOC;
      this.prevDeltaVA[var1] = super.prevDeltaVA;
      this.prevRange[var1] = super.prevRange;
      this.prevPOCPctUp[var1] = super.prevPOCPctUp;
      this.prevPOCPctDown[var1] = super.prevPOCPctDown;
   }

   @Override
   protected ChartData cfgChart() {
      return this.Chart;
   }

   @Override
   protected int cfgBinSizeMode() {
      return this.BinSizeMode;
   }

   @Override
   protected int cfgProfileRows() {
      return this.ProfileRows;
   }

   @Override
   protected int cfgTicksPerBin() {
      return this.TicksPerBin;
   }

   @Override
   protected double cfgValueAreaPct() {
      return this.ValueAreaPct;
   }

   @Override
   protected int cfgHvnCount() {
      return this.HvnCount;
   }

   @Override
   protected int cfgHvnThresholdPct() {
      return this.HvnThresholdPct;
   }

   @Override
   protected int cfgLvnThresholdPct() {
      return this.LvnThresholdPct;
   }

   @Override
   protected boolean cfgEnableLVN() {
      return this.EnableLVN;
   }

   @Override
   protected boolean cfgEnableVCP() {
      return this.EnableVCP;
   }

   @Override
   protected double cfgClusterSpread() {
      return this.ClusterSpread;
   }

   @Override
   protected int cfgMaxClusterCenters() {
      return this.MaxClusterCenters;
   }

   @Override
   protected int cfgIBMinutes() {
      return this.IBMinutes;
   }

   @Override
   protected int cfgSessionType() {
      return this.SessionMode == 2 ? 5 : 0;
   }

   @Override
   protected String cfgSessionLabel() {
      return this.SessionMode == 2 ? "Actual Session" : "Previous Session";
   }

   @Override
   protected String cfgCurrentSessionLabel() {
      return this.activeSes >= 0 && this.activeSes < SESSION_NAMES.length ? SESSION_NAMES[this.activeSes] : null;
   }

   @Override
   protected boolean cfgShowCandlesticks() {
      return this.ShowCandlesticks;
   }

   @Override
   protected boolean cfgShowVolumeSubchart() {
      return this.ShowVolumeSubchart;
   }

   @Override
   protected int cfgVolumeMALength() {
      return this.VolumeMALength;
   }

   @Override
   protected boolean cfgShowPOCDelta() {
      return this.ShowPOCDelta;
   }

   @Override
   protected boolean cfgShowVADelta() {
      return this.ShowVADelta;
   }

   @Override
   protected boolean cfgShowProfileRange() {
      return this.ShowProfileRange;
   }

   @Override
   protected boolean cfgShowPOCPosition() {
      return this.ShowPOCPosition;
   }

   @Override
   protected boolean cfgShowDeltaPerLevel() {
      return this.ShowDeltaPerLevel;
   }

   @Override
   protected int cfgPivotMethod() {
      return 0;
   }

   @Override
   protected double cfgPivotPct() {
      return 0.5;
   }

   @Override
   protected int cfgPivotTicks() {
      return 50;
   }

   @Override
   protected double cfgPivotATRMultiple() {
      return 1.5;
   }

   @Override
   protected int cfgPivotATRPeriod() {
      return 14;
   }

   private boolean isSessionEnabled(int var1) {
      switch (var1) {
         case 0:
            return this.EnableLondon;
         case 1:
            return this.EnableNewYork;
         case 2:
            return this.EnableSydney;
         case 3:
            return this.EnableTokyo;
         default:
            return false;
      }
   }

   private int[] getSessionTimes(int var1) {
      switch (var1) {
         case 0:
            return new int[]{this.LondonStartHour, this.LondonStartMin, this.LondonEndHour, this.LondonEndMin};
         case 1:
            return new int[]{this.NewYorkStartHour, this.NewYorkStartMin, this.NewYorkEndHour, this.NewYorkEndMin};
         case 2:
            return new int[]{this.SydneyStartHour, this.SydneyStartMin, this.SydneyEndHour, this.SydneyEndMin};
         case 3:
            return new int[]{this.TokyoStartHour, this.TokyoStartMin, this.TokyoEndHour, this.TokyoEndMin};
         default:
            return new int[]{0, 0, 0, 0};
      }
   }

   protected void OnBarUpdate() throws TradingException {
      this.ensureArrays();
      long var1 = this.Chart.Time(0);
      boolean var3 = this.SessionMode == 2;

      for (int var4 = 0; var4 < 4; var4++) {
         if (this.isSessionEnabled(var4)) {
            this.activeSes = var4;
            if (!var3) {
               if (this.currentSessionEnd[var4] == 0L || var1 >= this.currentSessionEnd[var4]) {
                  this.prevSessionStart[var4] = this.currentSessionStart[var4];
                  this.prevSessionEnd[var4] = this.currentSessionEnd[var4];
                  this.calculateSessionBoundaries(var1, var4);
                  if (this.prevSessionStart[var4] > 0L && this.isSunday(this.prevSessionStart[var4])) {
                     this.prevSessionStart[var4] = SQTime.addDays(SQTime.setTime(this.prevSessionStart[var4], 0, 0, 0, 0), -2);
                     this.prevSessionEnd[var4] = SQTime.addDays(this.prevSessionStart[var4], 1);
                  }

                  if (this.prevSessionStart[var4] > 0L && this.prevSessionEnd[var4] > 0L) {
                     this.copyToParentScalars(var4);
                     this.calculateVolumeProfile();
                     this.copyFromParentScalars(var4);
                  }
               }
            } else {
               if (this.currentSessionEnd[var4] == 0L || var1 >= this.currentSessionEnd[var4]) {
                  this.calculateSessionBoundaries(var1, var4);
               }

               if (var1 >= this.currentSessionStart[var4] && var1 < this.currentSessionEnd[var4]) {
                  this.prevSessionStart[var4] = this.currentSessionStart[var4];
                  this.prevSessionEnd[var4] = Math.min(this.currentSessionEnd[var4], var1);
                  if (this.prevSessionStart[var4] > 0L && this.prevSessionEnd[var4] > this.prevSessionStart[var4]) {
                     this.copyToParentScalars(var4);
                     this.calculateVolumeProfile();
                     this.copyFromParentScalars(var4);
                  }
               }
            }
         }
      }

      int var6 = -1;

      for (int var5 = 0; var5 < 4; var5++) {
         if (this.isSessionEnabled(var5)) {
            var6 = var5;
            break;
         }
      }

      if (var6 >= 0) {
         this.POC.set(0, this.prevPOC[var6]);
         this.VAH.set(0, this.prevVAH[var6]);
         this.VAL.set(0, this.prevVAL[var6]);
         this.IBH.set(0, this.prevIBH[var6]);
         this.IBL.set(0, this.prevIBL[var6]);
         this.HVN1.set(0, this.prevHVN[var6][0]);
         this.HVN2.set(0, this.prevHVN[var6][1]);
         this.HVN3.set(0, this.prevHVN[var6][2]);
         this.HVN4.set(0, this.prevHVN[var6][3]);
         this.HVN5.set(0, this.prevHVN[var6][4]);
         this.LVN1.set(0, this.prevLVN[var6][0]);
         this.LVN2.set(0, this.prevLVN[var6][1]);
         this.LVN3.set(0, this.prevLVN[var6][2]);
         this.LVN4.set(0, this.prevLVN[var6][3]);
         this.LVN5.set(0, this.prevLVN[var6][4]);
         this.VPOC.set(0, this.prevVPOC[var6]);
         this.VVAH.set(0, this.prevVVAH[var6]);
         this.VVAL.set(0, this.prevVVAL[var6]);
         this.BullPOC.set(0, this.prevBullPOC[var6]);
         this.BearPOC.set(0, this.prevBearPOC[var6]);
         this.TotalVolume.set(0, this.prevTotalVolume[var6]);
         this.TotalBullVolume.set(0, this.prevTotalBullVolume[var6]);
         this.TotalBearVolume.set(0, this.prevTotalBearVolume[var6]);
         this.Delta.set(0, this.prevDelta[var6]);
         this.DeltaPOC.set(0, this.prevDeltaPOC[var6]);
         this.DeltaVA.set(0, this.prevDeltaVA[var6]);
         this.ProfileRange.set(0, this.prevRange[var6]);
         this.POCPctUp.set(0, this.prevPOCPctUp[var6]);
         this.POCPctDown.set(0, this.prevPOCPctDown[var6]);
      }

      this.LondonPOC.set(0, this.prevPOC[0]);
      this.LondonVAH.set(0, this.prevVAH[0]);
      this.LondonVAL.set(0, this.prevVAL[0]);
      this.NewYorkPOC.set(0, this.prevPOC[1]);
      this.NewYorkVAH.set(0, this.prevVAH[1]);
      this.NewYorkVAL.set(0, this.prevVAL[1]);
      this.SydneyPOC.set(0, this.prevPOC[2]);
      this.SydneyVAH.set(0, this.prevVAH[2]);
      this.SydneyVAL.set(0, this.prevVAL[2]);
      this.TokyoPOC.set(0, this.prevPOC[3]);
      this.TokyoVAH.set(0, this.prevVAH[3]);
      this.TokyoVAL.set(0, this.prevVAL[3]);
   }

   private void calculateSessionBoundaries(long var1, int var3) throws TradingException {
      int[] var4 = this.getSessionTimes(var3);
      int var5 = var4[0];
      int var6 = var4[1];
      int var7 = var4[2];
      int var8 = var4[3];
      long var9 = SQTime.setTime(var1, 0, 0, 0, 0);
      long var11 = SQTime.setTime(var9, var5, var6, 0, 0);
      int var15 = var5 * 60 + var6;
      int var16 = var7 * 60 + var8;
      long var13;
      if (var16 > var15) {
         var13 = SQTime.setTime(var9, var7, var8, 0, 0);
      } else {
         var13 = SQTime.setTime(SQTime.addDays(var9, 1), var7, var8, 0, 0);
      }

      if (var1 < var11) {
         var11 = SQTime.addDays(var11, -1);
         var13 = SQTime.addDays(var13, -1);
      }

      if (var1 >= var13) {
         var11 = SQTime.addDays(var11, 1);
         var13 = SQTime.addDays(var13, 1);
      }

      this.currentSessionStart[var3] = var11;
      this.currentSessionEnd[var3] = var13;
      if (this.isSunday(this.currentSessionStart[var3])) {
         this.currentSessionStart[var3] = SQTime.addDays(this.currentSessionStart[var3], -2);
         this.currentSessionEnd[var3] = SQTime.addDays(this.currentSessionEnd[var3], -2);
      }
   }

   private String sessionLabel() {
      int[] var1 = this.getSessionTimes(this.activeSes);
      String var2 = this.SessionMode == 1 ? "Previous" : "Actual";
      return var2 + " " + SESSION_NAMES[this.activeSes] + " " + String.format("%02d:%02d-%02d:%02d", var1[0], var1[1], var1[2], var1[3]);
   }
}
