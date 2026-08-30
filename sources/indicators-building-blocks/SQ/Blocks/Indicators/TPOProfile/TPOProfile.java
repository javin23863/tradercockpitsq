package SQ.Blocks.Indicators.TPOProfile;

import SQ.Internal.TPOProfileIndicatorChart;
import com.strategyquant.datalib.DataSeries;
import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.tradinglib.BuildingBlock;
import com.strategyquant.tradinglib.ChartData;
import com.strategyquant.tradinglib.Editor;
import com.strategyquant.tradinglib.Help;
import com.strategyquant.tradinglib.Output;
import com.strategyquant.tradinglib.Parameter;
import com.strategyquant.tradinglib.ParameterSet;
import com.strategyquant.tradinglib.blocks.annotations.ParameterSets;

@BuildingBlock(name = "(TPO) Market Profile", display = "TPO_Letters(#SessionType#,#ProfileRows#,#ValueAreaPct#).#Line#[#Shift#]", returnType = 2)
@Help("TPO (Market Profile). Outputs POC/VAH/VAL based on Time Price Opportunities.")
@ParameterSets({@ParameterSet(set = "SessionType=1,ProfileRows=50,ValueAreaPct=70"), @ParameterSet(set = "SessionType=2,ProfileRows=100,ValueAreaPct=70")})
public class TPOProfile extends TPOProfileIndicatorChart {
   @Parameter(defaultChartIndex = 0, category = "General")
   public ChartData Chart;
   @Parameter(defaultValue = "1", name = "SessionType", category = "General")
   @Help("Session selection: Previous/Actual Day/Week/Month/Year for profile calculation")
   @Editor(type = 40, values = "Previous Day=1,Previous Week=2,Previous Month=3,Previous Year=4,Actual Day=5,Actual Week=6,Actual Month=7,Actual Year=8")
   public int SessionType;
   @Parameter(defaultValue = "150", name = "ProfileRows", minValue = 10.0, maxValue = 500.0, step = 10.0, category = "Profile Calculation")
   @Help("Number of price levels (bins) in the profile (used in Range-Based mode)")
   public int ProfileRows;
   @Parameter(defaultValue = "2", name = "BinSizeMode", category = "Profile Calculation")
   @Help("1=Range-Based: bin size = range/ProfileRows. 2=Fixed: bin size = TicksPerBin x tick size.")
   @Editor(type = 40, values = "Range-Based=1,Fixed Tick Size=2")
   public int BinSizeMode;
   @Parameter(defaultValue = "1", name = "TicksPerBin", minValue = 1.0, maxValue = 1000.0, step = 1.0, category = "Profile Calculation")
   @Help("Number of ticks per bin (used only in Fixed Tick Size mode)")
   public int TicksPerBin;
   @Parameter(defaultValue = "70", name = "ValueAreaPct", minValue = 30.0, maxValue = 95.0, step = 5.0, category = "Profile Calculation")
   @Help("Percentage of total volume that defines the Value Area")
   public double ValueAreaPct;
   @Parameter(defaultValue = "false", name = "UseCustomHours", category = "Session / Time")
   @Help("If true (Daily session only), session is StartHour:StartMinute -> EndHour:EndMinute (can cross midnight). Useful for RTH-only profiles.")
   public boolean UseCustomHours;
   @Parameter(defaultValue = "0", name = "StartHour", minValue = 0.0, maxValue = 23.0, step = 1.0, category = "Session / Time")
   public int StartHour;
   @Parameter(defaultValue = "0", name = "StartMinute", minValue = 0.0, maxValue = 59.0, step = 1.0, category = "Session / Time")
   public int StartMinute;
   @Parameter(defaultValue = "23", name = "EndHour", minValue = 0.0, maxValue = 23.0, step = 1.0, category = "Session / Time")
   public int EndHour;
   @Parameter(defaultValue = "59", name = "EndMinute", minValue = 0.0, maxValue = 59.0, step = 1.0, category = "Session / Time")
   public int EndMinute;
   @Parameter(defaultValue = "30", name = "BracketMinDaily", minValue = 1.0, maxValue = 120.0, step = 1.0, category = "Time Structure")
   @Help("TPO bracket size in minutes for Daily sessions")
   public int BracketMinDaily;
   @Parameter(defaultValue = "240", name = "BracketMinWeekly", minValue = 1.0, maxValue = 10000.0, step = 1.0, category = "Time Structure")
   @Help("TPO bracket size in minutes for Weekly sessions")
   public int BracketMinWeekly;
   @Parameter(defaultValue = "720", name = "BracketMinMonthly", minValue = 1.0, maxValue = 10000.0, step = 1.0, category = "Time Structure")
   @Help("TPO bracket size in minutes for Monthly sessions")
   public int BracketMinMonthly;
   @Parameter(defaultValue = "720", name = "BracketMinYearly", minValue = 1.0, maxValue = 10000.0, step = 1.0, category = "Time Structure")
   @Help("TPO bracket size in minutes for Yearly sessions")
   public int BracketMinYearly;
   @Parameter(defaultValue = "true", name = "ShowCandlesticks", category = "Chart Display", showIfDefault = false)
   @Help("Show candlestick price chart alongside each session profile in SVG export")
   public boolean ShowCandlesticks;
   @Parameter(defaultValue = "true", name = "ShowShapeLabel", category = "Chart Display", showIfDefault = false)
   @Help("Annotate detected profile shape (P/b/D/DD) on each session panel in SVG export")
   public boolean ShowShapeLabel;
   @Parameter(defaultValue = "false", name = "UseBlockMode", category = "Chart Display", showIfDefault = false)
   @Help("Draw TPO profile as colored blocks instead of letters in SVG export")
   public boolean UseBlockMode;
   @Output(name = "TPO_POC", color = "#FFA500")
   public DataSeries TPO_POC;
   @Output(name = "TPO_VAH", color = "#90EE90")
   public DataSeries TPO_VAH;
   @Output(name = "TPO_VAL", color = "#FFC0CB")
   public DataSeries TPO_VAL;
   @Output(name = "TPO_PoorHigh", color = "#FF0000", show = false)
   public DataSeries TPO_PoorHigh;
   @Output(name = "TPO_PoorLow", color = "#FF0000", show = false)
   public DataSeries TPO_PoorLow;
   @Output(name = "TPO_ExcessHigh", color = "#008000", show = false)
   public DataSeries TPO_ExcessHigh;
   @Output(name = "TPO_ExcessLow", color = "#008000", show = false)
   public DataSeries TPO_ExcessLow;
   @Output(name = "TPO_SinglePrints", color = "#808080", show = false)
   public DataSeries TPO_SinglePrints;
   @Output(name = "TPO_BuyingTailLen", color = "#90EE90", show = false)
   public DataSeries TPO_BuyingTailLen;
   @Output(name = "TPO_SellingTailLen", color = "#FFC0CB", show = false)
   public DataSeries TPO_SellingTailLen;
   @Output(name = "TPO_ProfileShape", color = "#FFFF00", show = false)
   public DataSeries TPO_ProfileShape;
   @Output(name = "TPO_LedgeUp", color = "#90EE90", show = false)
   public DataSeries TPO_LedgeUp;
   @Output(name = "TPO_LedgeDown", color = "#FFC0CB", show = false)
   public DataSeries TPO_LedgeDown;
   @Output(name = "TPO_IBH", color = "#F0FFFF", show = false)
   public DataSeries TPO_IBH;
   @Output(name = "TPO_IBL", color = "#FF00FF", show = false)
   public DataSeries TPO_IBL;
   @Output(name = "TPO_VPOC", color = "#F0FFFF", show = false)
   public DataSeries TPO_VPOC;
   @Output(name = "TPO_VVAH", color = "#90EE90", show = false)
   public DataSeries TPO_VVAH;
   @Output(name = "TPO_VVAL", color = "#F5F5DC", show = false)
   public DataSeries TPO_VVAL;
   @Output(name = "TPO_BullPOC", color = "#008000", show = false)
   public DataSeries TPO_BullPOC;
   @Output(name = "TPO_BearPOC", color = "#FF0000", show = false)
   public DataSeries TPO_BearPOC;
   @Output(name = "TPO_TotalVolume", color = "#808080", show = false)
   public DataSeries TPO_TotalVolume;
   @Output(name = "TPO_TotalBullVolume", color = "#008000", show = false)
   public DataSeries TPO_TotalBullVolume;
   @Output(name = "TPO_TotalBearVolume", color = "#FF0000", show = false)
   public DataSeries TPO_TotalBearVolume;

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
   protected int cfgIBMinutes() {
      return 0;
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
      return false;
   }

   @Override
   protected int cfgVolumeMALength() {
      return 20;
   }

   @Override
   protected boolean cfgShowPOCDelta() {
      return false;
   }

   @Override
   protected boolean cfgShowVADelta() {
      return false;
   }

   @Override
   protected boolean cfgShowProfileRange() {
      return false;
   }

   @Override
   protected boolean cfgShowPOCPosition() {
      return false;
   }

   @Override
   protected int cfgBracketMinDaily() {
      return this.BracketMinDaily;
   }

   @Override
   protected int cfgBracketMinWeekly() {
      return this.BracketMinWeekly;
   }

   @Override
   protected int cfgBracketMinMonthly() {
      return this.BracketMinMonthly;
   }

   @Override
   protected int cfgBracketMinYearly() {
      return this.BracketMinYearly;
   }

   @Override
   protected boolean cfgShowShapeLabel() {
      return this.ShowShapeLabel;
   }

   @Override
   protected boolean cfgUseBlockMode() {
      return this.UseBlockMode;
   }

   protected void OnBarUpdate() throws TradingException {
      this.SessionType = SQUtils.fixAllowedRange(this.SessionType, 1, 8, 1);
      this.ensureArrays();
      long var1 = this.Chart.Time(0);
      boolean var3 = this.SessionType >= 5 && this.SessionType <= 8;
      if (!var3) {
         if (this.currentSessionEnd == 0L || var1 >= this.currentSessionEnd) {
            this.debug("VP", "*** NEW SESSION DETECTED ***");
            this.prevSessionStart = this.currentSessionStart;
            this.prevSessionEnd = this.currentSessionEnd;
            this.debug(
               "VP",
               "Saved prev session: Start=" + SQTime.toFullDateTimeString(this.prevSessionStart) + ", End=" + SQTime.toFullDateTimeString(this.prevSessionEnd)
            );
            this.calculateSessionBoundaries(var1);
            if (this.prevSessionStart > 0L && this.isSunday(this.prevSessionStart)) {
               this.prevSessionStart = SQTime.addDays(SQTime.setTime(this.prevSessionStart, 0, 0, 0, 0), -2);
               this.prevSessionEnd = SQTime.addDays(this.prevSessionStart, 1);
            }

            if (this.prevSessionStart > 0L && this.isSunday(this.prevSessionStart)) {
               this.prevSessionStart = SQTime.addDays(SQTime.setTime(this.prevSessionStart, 0, 0, 0, 0), -2);
               this.prevSessionEnd = SQTime.addDays(this.prevSessionStart, 1);
            }

            this.debug(
               "VP",
               "New session boundaries: Start="
                  + SQTime.toFullDateTimeString(this.currentSessionStart)
                  + ", End="
                  + SQTime.toFullDateTimeString(this.currentSessionEnd)
            );
            if (this.prevSessionStart > 0L && this.prevSessionEnd > 0L) {
               this.debug("VP", "Calling calculateTPOProfile()...");
               this.calculateTPOProfile();
               this.debug("VP", "Profile calculated: POC=" + this.prevTPO_POC + ", VAH=" + this.prevTPO_VAH + ", VAL=" + this.prevTPO_VAL);
            } else {
               this.debug("VP", "Skipping profile calc - no valid prev session boundaries");
            }
         }
      } else {
         if (this.currentSessionEnd == 0L || var1 >= this.currentSessionEnd) {
            this.calculateSessionBoundaries(var1);
         }

         this.prevSessionStart = this.currentSessionStart;
         this.prevSessionEnd = Math.min(this.currentSessionEnd, var1);
         if (this.prevSessionStart > 0L && this.prevSessionEnd > this.prevSessionStart) {
            this.calculateTPOProfile();
         }
      }

      this.TPO_POC.set(0, this.prevTPO_POC);
      this.TPO_VAH.set(0, this.prevTPO_VAH);
      this.TPO_VAL.set(0, this.prevTPO_VAL);
      this.TPO_PoorHigh.set(0, this.lastPoorHigh ? 1.0 : 0.0);
      this.TPO_PoorLow.set(0, this.lastPoorLow ? 1.0 : 0.0);
      this.TPO_ExcessHigh.set(0, this.lastExcessHigh ? 1.0 : 0.0);
      this.TPO_ExcessLow.set(0, this.lastExcessLow ? 1.0 : 0.0);
      this.TPO_SinglePrints.set(0, this.lastSinglePrintCount);
      this.TPO_BuyingTailLen.set(0, this.lastBuyingTailLen);
      this.TPO_SellingTailLen.set(0, this.lastSellingTailLen);
      this.TPO_ProfileShape.set(0, this.lastProfileShape);
      this.TPO_LedgeUp.set(0, this.lastLedgeUpPrice);
      this.TPO_LedgeDown.set(0, this.lastLedgeDownPrice);
      this.TPO_IBH.set(0, this.prevIBH);
      this.TPO_IBL.set(0, this.prevIBL);
      this.TPO_VPOC.set(0, this.prevTPO_VPOC);
      this.TPO_VVAH.set(0, this.prevTPO_VVAH);
      this.TPO_VVAL.set(0, this.prevTPO_VVAL);
      this.TPO_BullPOC.set(0, this.prevTPO_BullPOC);
      this.TPO_BearPOC.set(0, this.prevTPO_BearPOC);
      this.TPO_TotalVolume.set(0, this.prevTotalVolume);
      this.TPO_TotalBullVolume.set(0, this.prevTotalBullVolume);
      this.TPO_TotalBearVolume.set(0, this.prevTotalBearVolume);
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

      this.debug(
         "VP",
         "calculateSessionBoundaries: curTime="
            + SQTime.toFullDateTimeString(var1)
            + ", dayStart="
            + SQTime.toFullDateTimeString(var3)
            + ", SessionType="
            + this.SessionType
            + ", baseType="
            + var5
      );
      switch (var5) {
         case 1:
            if (this.UseCustomHours) {
               long var6 = SQTime.setTime(var3, this.StartHour, this.StartMinute, 0, 0);
               long var8 = SQTime.setTime(var3, this.EndHour, this.EndMinute, 0, 0);
               if (var8 <= var6) {
                  var8 = SQTime.addDays(var8, 1);
               }

               if (var1 < var6) {
                  var6 = SQTime.addDays(var6, -1);
                  var8 = SQTime.addDays(var8, -1);
               }

               this.currentSessionStart = var6;
               this.currentSessionEnd = var8;
               this.debug(
                  "VP",
                  "Daily CUSTOM session: Start="
                     + SQTime.toFullDateTimeString(this.currentSessionStart)
                     + ", End="
                     + SQTime.toFullDateTimeString(this.currentSessionEnd)
               );
            } else {
               this.currentSessionStart = var3;
               this.currentSessionEnd = SQTime.addDays(var3, 1);
               if (this.isSunday(this.currentSessionStart)) {
                  this.currentSessionStart = SQTime.addDays(this.currentSessionStart, -2);
                  this.currentSessionEnd = SQTime.addDays(this.currentSessionStart, 1);
               }

               this.debug(
                  "VP",
                  "Daily session: Start="
                     + SQTime.toFullDateTimeString(this.currentSessionStart)
                     + ", End="
                     + SQTime.toFullDateTimeString(this.currentSessionEnd)
               );
            }
            break;
         case 2:
            this.currentSessionStart = SQTime.setDayOfWeek(var3, 1);
            if (this.currentSessionStart > var3) {
               this.currentSessionStart = SQTime.addDays(this.currentSessionStart, -7);
            }

            this.currentSessionEnd = SQTime.addDays(this.currentSessionStart, 7);
            this.debug(
               "VP",
               "Weekly session: Start="
                  + SQTime.toFullDateTimeString(this.currentSessionStart)
                  + ", End="
                  + SQTime.toFullDateTimeString(this.currentSessionEnd)
            );
            break;
         case 3:
            this.currentSessionStart = SQTime.setDayOfMonth(var3, 1);
            this.currentSessionEnd = SQTime.addMonths(this.currentSessionStart, 1);
            this.debug(
               "VP",
               "Monthly session: Start="
                  + SQTime.toFullDateTimeString(this.currentSessionStart)
                  + ", End="
                  + SQTime.toFullDateTimeString(this.currentSessionEnd)
            );
            break;
         case 4:
            this.currentSessionStart = SQTime.setTime(SQTime.setDayOfMonth(SQTime.setMonthOfYear(var1, 1), 1), 0, 0, 0, 0);
            this.currentSessionEnd = SQTime.addYears(this.currentSessionStart, 1);
            this.debug(
               "VP",
               "Yearly session: Start="
                  + SQTime.toFullDateTimeString(this.currentSessionStart)
                  + ", End="
                  + SQTime.toFullDateTimeString(this.currentSessionEnd)
            );
            break;
         default:
            this.currentSessionStart = var3;
            this.currentSessionEnd = SQTime.addDays(var3, 1);
            this.debug(
               "VP",
               "Default (daily) session: Start="
                  + SQTime.toFullDateTimeString(this.currentSessionStart)
                  + ", End="
                  + SQTime.toFullDateTimeString(this.currentSessionEnd)
            );
      }
   }
}
