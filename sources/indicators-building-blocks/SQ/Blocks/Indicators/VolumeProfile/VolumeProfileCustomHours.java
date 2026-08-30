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
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(
   name = "(VP) Volume Profile Custom Hours",
   display = "VolumeProfileCH(#SessionStartHours#:#SessionStartMinutes#-#SessionEndHours#:#SessionEndMinutes#,#ProfileRows#,#ValueAreaPct#).#Line#[#Shift#]",
   returnType = 2
)
@Help(
   "Volume Profile with Custom Hours. Define your own session window by setting start/end hours and minutes. Outputs POC / VAH / VAL for the custom session period."
)
@ForEngine("MT4,MT5,TS,MC")
@ParameterSets(
   {
         @ParameterSet(set = "SessionStartHours=8,SessionStartMinutes=0,SessionEndHours=16,SessionEndMinutes=0,ProfileRows=100,ValueAreaPct=70"),
         @ParameterSet(set = "SessionStartHours=9,SessionStartMinutes=30,SessionEndHours=16,SessionEndMinutes=0,ProfileRows=50,ValueAreaPct=70"),
         @ParameterSet(set = "SessionStartHours=0,SessionStartMinutes=0,SessionEndHours=8,SessionEndMinutes=0,ProfileRows=100,ValueAreaPct=70")
   }
)
public class VolumeProfileCustomHours extends VolumeProfileIndicatorChart {
   @Parameter(defaultChartIndex = 0)
   public ChartData Chart;
   @Parameter(defaultValue = "8", name = "SessionStartHours", minValue = 0.0, maxValue = 23.0, step = 1.0)
   @Help("Session start hour (0-23)")
   public int SessionStartHours;
   @Parameter(defaultValue = "0", name = "SessionStartMinutes", minValue = 0.0, maxValue = 59.0, step = 1.0)
   @Help("Session start minutes (0-59)")
   public int SessionStartMinutes;
   @Parameter(defaultValue = "16", name = "SessionEndHours", minValue = 0.0, maxValue = 23.0, step = 1.0)
   @Help("Session end hour (0-23)")
   public int SessionEndHours;
   @Parameter(defaultValue = "0", name = "SessionEndMinutes", minValue = 0.0, maxValue = 59.0, step = 1.0)
   @Help("Session end minutes (0-59)")
   public int SessionEndMinutes;
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

   protected void OnBarUpdate() throws TradingException {
      this.ensureArrays();
      long var1 = this.Chart.Time(0);
      boolean var3 = this.SessionMode == 2;
      if (!var3) {
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
      long var5 = SQTime.setTime(var3, this.SessionStartHours, this.SessionStartMinutes, 0, 0);
      int var9 = this.SessionStartHours * 60 + this.SessionStartMinutes;
      int var10 = this.SessionEndHours * 60 + this.SessionEndMinutes;
      long var7;
      if (var10 > var9) {
         var7 = SQTime.setTime(var3, this.SessionEndHours, this.SessionEndMinutes, 0, 0);
      } else {
         var7 = SQTime.setTime(SQTime.addDays(var3, 1), this.SessionEndHours, this.SessionEndMinutes, 0, 0);
      }

      if (var1 < var5) {
         var5 = SQTime.addDays(var5, -1);
         var7 = SQTime.addDays(var7, -1);
      }

      if (var1 >= var7) {
         var5 = SQTime.addDays(var5, 1);
         var7 = SQTime.addDays(var7, 1);
      }

      this.currentSessionStart = var5;
      this.currentSessionEnd = var7;
      if (this.isSunday(this.currentSessionStart)) {
         this.currentSessionStart = SQTime.addDays(this.currentSessionStart, -2);
         this.currentSessionEnd = SQTime.addDays(this.currentSessionEnd, -2);
      }
   }

   private String sessionLabel() {
      return String.format("%02d:%02d-%02d:%02d", this.SessionStartHours, this.SessionStartMinutes, this.SessionEndHours, this.SessionEndMinutes);
   }
}
