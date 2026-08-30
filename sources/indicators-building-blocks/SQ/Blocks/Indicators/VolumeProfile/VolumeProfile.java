package SQ.Blocks.Indicators.VolumeProfile;

import SQ.Internal.VolumeProfileIndicatorChart;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.ForEngine;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(VP) Volume Profile", display = "VolumeProfile(#SessionType#,#ProfileRows#,#ValueAreaPct#).#Line#[#Shift#]", returnType = 2)
@Help("Volume Profile. Outputs POC / VAH / VAL. Use H1 Timeframe for monthly session, M30 for weekly session")
@ForEngine("MT4,MT5,TS,MC")
@ParameterSets(
   {
         @ParameterSet(set = "SessionType=1,ProfileRows=100,ValueAreaPct=70"),
         @ParameterSet(set = "SessionType=1,ProfileRows=50,ValueAreaPct=70"),
         @ParameterSet(set = "SessionType=2,ProfileRows=100,ValueAreaPct=70"),
         @ParameterSet(set = "SessionType=3,ProfileRows=100,ValueAreaPct=70")
   }
)
public class VolumeProfile extends VolumeProfileIndicatorChart {
   @Parameter(defaultChartIndex = 0, category = "General")
   public ChartData Chart;
   @Parameter(defaultValue = "2", name = "SessionType", category = "General")
   @Help("Session selection: Previous/Actual Day/Week/Month/Year/Swing for profile calculation")
   @Editor(
      type = 40,
      values = "Previous Day=1,Previous Week=2,Previous Month=3,Previous Year=4,Actual Day=5,Actual Week=6,Actual Month=7,Actual Year=8,Previous Swing=9,Actual Swing=10"
   )
   public int SessionType;
   @Parameter(defaultValue = "150", name = "ProfileRows", minValue = 10.0, maxValue = 500.0, step = 10.0, category = "Profile Calculation")
   @Help("Number of price levels (bins) in the profile (used in Range-Based mode)")
   public int ProfileRows;
   @Parameter(defaultValue = "2", name = "BinSizeMode", category = "Profile Calculation")
   @Help("1=Range-Based: bin size = range/ProfileRows. 2=Fixed: bin size = TicksPerBin x tick size.")
   @Editor(type = 40, values = "Range-Based=1,Fixed Tick Size=2")
   public int BinSizeMode;
   @Parameter(defaultValue = "3", name = "TicksPerBin", minValue = 1.0, maxValue = 1000.0, step = 1.0, category = "Profile Calculation")
   @Help("Number of ticks per bin (used only in Fixed Tick Size mode)")
   public int TicksPerBin;
   @Parameter(defaultValue = "70", name = "ValueAreaPct", minValue = 30.0, maxValue = 95.0, step = 5.0, category = "Profile Calculation")
   @Help("Percentage of total volume that defines the Value Area")
   public double ValueAreaPct;
   @Parameter(defaultValue = "0", name = "IBMinutes", minValue = 0.0, maxValue = 14400.0, step = 1.0, category = "Session / Time")
   @Help("Initial Balance period override in minutes. 0 = use session-type default (60min daily, 12h weekly, etc). Values > 0 override the default.")
   public int IBMinutes;
   @Parameter(defaultValue = "5", name = "HvnCount", minValue = 1.0, maxValue = 10.0, step = 1.0, category = "Volume Structure")
   @Help("Number of High Volume Nodes (local peaks) to detect in the profile")
   public int HvnCount;
   @Parameter(defaultValue = "20", name = "HvnThresholdPct", minValue = 10.0, maxValue = 90.0, step = 5.0, category = "Volume Structure")
   @Help("Minimum volume as % of max bin volume for a local peak to qualify as HVN")
   public int HvnThresholdPct;
   @Parameter(defaultValue = "40", name = "LvnThresholdPct", minValue = 10.0, maxValue = 90.0, step = 5.0, category = "Volume Structure")
   @Help("Maximum volume as % of max bin volume for a local valley to qualify as LVN")
   public int LvnThresholdPct;
   @Parameter(defaultValue = "false", name = "EnableLVN", category = "Volume Structure")
   @Help("If true, detect Low Volume Nodes (LVN1-LVN5). If false, LVN outputs remain 0.")
   public boolean EnableLVN;
   @Parameter(defaultValue = "false", name = "EnableVolumeCluster", category = "Advanced Features")
   @Help("Enable Volume Cluster Profile: Gaussian-enhanced clustering that smooths the profile around dominant volume peaks")
   public boolean EnableVCP;
   @Parameter(defaultValue = "6.0", name = "VolumeClusterSpread", minValue = 0.5, maxValue = 20.0, step = 0.5, category = "Advanced Features")
   @Help("Gaussian sigma controlling how wide each cluster center's influence reaches (higher = smoother/wider)")
   public double ClusterSpread;
   @Parameter(defaultValue = "2", name = "MaxVolumeClusterCenters", minValue = 1.0, maxValue = 10.0, step = 1.0, category = "Advanced Features")
   @Help("Maximum number of cluster peaks used in Gaussian enhancement")
   public int MaxClusterCenters;
   @Parameter(defaultValue = "1", name = "PivotMethod", category = "Advanced Features")
   @Help("ZigZag reversal method: 1=Percentage of price, 2=Fixed Ticks, 3=ATR Multiple")
   @Editor(type = 40, values = "Percentage=1,Fixed Ticks=2,ATR Multiple=3")
   public int PivotMethod;
   @Parameter(defaultValue = "2.0", name = "PivotPct", minValue = 0.01, maxValue = 10.0, step = 0.1, category = "Advanced Features")
   @Help("Reversal threshold as percentage of price (e.g. 2.0 = 2%). Used when PivotMethod=Percentage")
   public double PivotPct;
   @Parameter(defaultValue = "50", name = "PivotTicks", minValue = 1.0, maxValue = 10000.0, step = 1.0, category = "Advanced Features")
   @Help("Reversal threshold in ticks (e.g. 50 = 50 ticks). Used when PivotMethod=Fixed Ticks")
   public int PivotTicks;
   @Parameter(defaultValue = "1.5", name = "PivotATRMultiple", minValue = 0.1, maxValue = 10.0, step = 0.1, category = "Advanced Features")
   @Help("Reversal threshold as multiple of ATR (e.g. 1.5 = 1.5x ATR). Used when PivotMethod=ATR Multiple")
   public double PivotATRMultiple;
   @Parameter(defaultValue = "14", name = "PivotATRPeriod", minValue = 2.0, maxValue = 200.0, step = 1.0, category = "Advanced Features")
   @Help("ATR period in bars for computing average true range. Used when PivotMethod=ATR Multiple")
   public int PivotATRPeriod;
   @Parameter(defaultValue = "true", name = "ShowCandlesticks", category = "Chart Display", showIfDefault = false)
   @Help("Show candlestick price chart alongside each session profile in SVG export")
   public boolean ShowCandlesticks;
   @Parameter(defaultValue = "false", name = "ShowVolumeSubchart", category = "Chart Display", showIfDefault = false)
   @Help("Show a volume bar subplot below the main chart in SVG export")
   public boolean ShowVolumeSubchart;
   @Parameter(defaultValue = "20", name = "VolumeMALength", minValue = 2.0, maxValue = 200.0, step = 1.0, category = "Chart Display", showIfDefault = false)
   @Help("Period for the moving average line overlaid on the volume subchart in SVG export")
   public int VolumeMALength;
   @Parameter(defaultValue = "true", name = "ShowPOCDelta", category = "Chart Display", showIfDefault = false)
   @Help("Show POC price change vs previous session in SVG export")
   public boolean ShowPOCDelta;
   @Parameter(defaultValue = "true", name = "ShowVADelta", category = "Chart Display", showIfDefault = false)
   @Help("Show Value Area midpoint change vs previous session in SVG export")
   public boolean ShowVADelta;
   @Parameter(defaultValue = "true", name = "ShowProfileRange", category = "Chart Display", showIfDefault = false)
   @Help("Show session high-low range in SVG export")
   public boolean ShowProfileRange;
   @Parameter(defaultValue = "true", name = "ShowPOCPosition", category = "Chart Display", showIfDefault = false)
   @Help("Show POC position as pct from top and bottom of profile range in SVG export")
   public boolean ShowPOCPosition;
   @Parameter(defaultValue = "false", name = "ShowDeltaPerLevel", category = "Chart Display", showIfDefault = false)
   @Help("Show per-level delta (bull minus bear) labels on each bin in SVG export")
   public boolean ShowDeltaPerLevel;
   @Parameter(defaultValue = "false", name = "ShowZigZagLine", category = "Chart Display", showIfDefault = false)
   @Help("Show the ZigZag overlay line in SVG export (only applies to Swing session types)")
   public boolean ShowZigZagLine;
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
      return this.SessionType;
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
   protected boolean cfgShowZigZagLine() {
      return this.ShowZigZagLine;
   }

   @Override
   protected int cfgPivotMethod() {
      return this.PivotMethod;
   }

   @Override
   protected double cfgPivotPct() {
      return this.PivotPct;
   }

   @Override
   protected int cfgPivotTicks() {
      return this.PivotTicks;
   }

   @Override
   protected double cfgPivotATRMultiple() {
      return this.PivotATRMultiple;
   }

   @Override
   protected int cfgPivotATRPeriod() {
      return this.PivotATRPeriod;
   }

   protected void OnBarUpdate() throws TradingException {
      this.SessionType = SQUtils.fixAllowedRange(this.SessionType, 1, 10, 1);
      this.ensureArrays();
      long var1 = this.Chart.Time(0);
      boolean var3 = this.SessionType == 9 || this.SessionType == 10;
      boolean var4 = this.SessionType >= 5;
      if (var3) {
         boolean var5 = this.SessionType == 10;
         boolean var6 = this.detectZigZagPivot();
         if (!var5) {
            if (var6 && this.zzSessionStart > 0L && this.zzLastPivotTime > this.zzSessionStart) {
               this.prevSessionStart = this.zzSessionStart;
               this.prevSessionEnd = this.zzLastPivotTime;
               this.calculateVolumeProfile();
               this.zzSessionStart = this.zzLastPivotTime;
            }
         } else {
            if (var6 && this.zzSessionStart > 0L) {
               this.prevSessionStart = this.zzSessionStart;
               this.prevSessionEnd = this.zzLastPivotTime;
               this.calculateVolumeProfile();
               this.zzSessionStart = this.zzLastPivotTime;
            }

            if (this.zzSessionStart > 0L && var1 > this.zzSessionStart) {
               this.prevSessionStart = this.zzSessionStart;
               this.prevSessionEnd = var1;
               this.calculateVolumeProfile();
            }
         }
      } else if (!var4) {
         if (this.currentSessionEnd == 0L || var1 >= this.currentSessionEnd) {
            this.prevSessionStart = this.currentSessionStart;
            this.prevSessionEnd = this.currentSessionEnd;
            this.calculateSessionBoundaries(var1);
            if (this.prevSessionStart > 0L && this.isSunday(this.prevSessionStart)) {
               this.prevSessionStart = SQTime.addDays(SQTime.setTime(this.prevSessionStart, 0, 0, 0, 0), -2);
               this.prevSessionEnd = SQTime.addDays(this.prevSessionStart, 1);
            }

            if (this.prevSessionStart > 0L && this.isSunday(this.prevSessionStart)) {
               this.prevSessionStart = SQTime.addDays(SQTime.setTime(this.prevSessionStart, 0, 0, 0, 0), -2);
               this.prevSessionEnd = SQTime.addDays(this.prevSessionStart, 1);
            }

            if (this.prevSessionStart > 0L && this.prevSessionEnd > 0L) {
               this.calculateVolumeProfile();
            }
         }
      } else {
         if (this.currentSessionEnd == 0L || var1 >= this.currentSessionEnd) {
            this.calculateSessionBoundaries(var1);
         }

         this.prevSessionStart = this.currentSessionStart;
         this.prevSessionEnd = Math.min(this.currentSessionEnd, var1);
         if (this.prevSessionStart > 0L && this.prevSessionEnd > this.prevSessionStart) {
            this.calculateVolumeProfile();
         }
      }

      this.POC.set(0, this.prevPOC);
      this.VAH.set(0, this.prevVAH);
      this.VAL.set(0, this.prevVAL);
      this.IBH.set(0, this.prevIBH);
      this.IBL.set(0, this.prevIBL);
      this.HVN1.set(0, this.prevHVN[0]);
      this.HVN2.set(0, this.prevHVN[1]);
      this.HVN3.set(0, this.prevHVN[2]);
      this.HVN4.set(0, this.prevHVN[3]);
      this.HVN5.set(0, this.prevHVN[4]);
      this.LVN1.set(0, this.prevLVN[0]);
      this.LVN2.set(0, this.prevLVN[1]);
      this.LVN3.set(0, this.prevLVN[2]);
      this.LVN4.set(0, this.prevLVN[3]);
      this.LVN5.set(0, this.prevLVN[4]);
      this.VPOC.set(0, this.prevVPOC);
      this.VVAH.set(0, this.prevVVAH);
      this.VVAL.set(0, this.prevVVAL);
      this.BullPOC.set(0, this.prevBullPOC);
      this.BearPOC.set(0, this.prevBearPOC);
      this.TotalVolume.set(0, this.prevTotalVolume);
      this.TotalBullVolume.set(0, this.prevTotalBullVolume);
      this.TotalBearVolume.set(0, this.prevTotalBearVolume);
      this.Delta.set(0, this.prevDelta);
      this.DeltaPOC.set(0, this.prevDeltaPOC);
      this.DeltaVA.set(0, this.prevDeltaVA);
      this.ProfileRange.set(0, this.prevRange);
      this.POCPctUp.set(0, this.prevPOCPctUp);
      this.POCPctDown.set(0, this.prevPOCPctDown);
   }

   private void calculateSessionBoundaries(long var1) throws TradingException {
      long var3 = SQTime.setTime(var1, 0, 0, 0, 0);
      byte var5;
      if (this.SessionType == 1 || this.SessionType == 5) {
         var5 = 1;
      } else if (this.SessionType == 2 || this.SessionType == 6) {
         var5 = 2;
      } else if (this.SessionType == 3 || this.SessionType == 7) {
         var5 = 3;
      } else if (this.SessionType != 4 && this.SessionType != 8) {
         var5 = 1;
      } else {
         var5 = 4;
      }

      switch (var5) {
         case 0:
            this.currentSessionStart = SQTime.setTime(var1, SQTime.getHour(var1), 0, 0, 0);
            this.currentSessionEnd = SQTime.addHours(this.currentSessionStart, 1);
            break;
         case 1:
            this.currentSessionStart = var3;
            this.currentSessionEnd = SQTime.addDays(var3, 1);
            if (this.isSunday(this.currentSessionStart)) {
               this.currentSessionStart = SQTime.addDays(this.currentSessionStart, -2);
               this.currentSessionEnd = SQTime.addDays(this.currentSessionStart, 1);
            }
            break;
         case 2:
            this.currentSessionStart = SQTime.setDayOfWeek(var3, 1);
            if (this.currentSessionStart > var3) {
               this.currentSessionStart = SQTime.addDays(this.currentSessionStart, -7);
            }

            this.currentSessionEnd = SQTime.addDays(this.currentSessionStart, 7);
            break;
         case 3:
            this.currentSessionStart = SQTime.setDayOfMonth(var3, 1);
            this.currentSessionEnd = SQTime.addMonths(this.currentSessionStart, 1);
            break;
         case 4:
            this.currentSessionStart = SQTime.setTime(SQTime.setDayOfMonth(SQTime.setMonthOfYear(var1, 1), 1), 0, 0, 0, 0);
            this.currentSessionEnd = SQTime.addYears(this.currentSessionStart, 1);
            break;
         default:
            this.currentSessionStart = var3;
            this.currentSessionEnd = SQTime.addDays(var3, 1);
      }
   }
}
