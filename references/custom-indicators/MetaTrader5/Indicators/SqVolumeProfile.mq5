//+------------------------------------------------------------------+
//|                                            SqVolumeProfile.mq5   |
//|                           Copyright © 2026, StrategyQuant s.r.o. |
//|                                     http://www.strategyquant.com |
//+------------------------------------------------------------------+
#property copyright   "Copyright © 2026, StrategyQuant s.r.o."
#property link        "http://www.strategyquant.com"
#property version     "4.00"
#property description "Volume Profile — Full feature parity with Java snippet"

#include "../Include/SqVPDisplay.mqh"

#property indicator_chart_window
#property indicator_buffers 23
#property indicator_plots   23

//--- Buffer 0: POC
#property indicator_label1  "POC"
#property indicator_type1   DRAW_LINE
#property indicator_color1  clrYellow
#property indicator_width1  2

//--- Buffer 1: VAH
#property indicator_label2  "VAH"
#property indicator_type2   DRAW_LINE
#property indicator_color2  clrGreen
#property indicator_width2  1

//--- Buffer 2: VAL
#property indicator_label3  "VAL"
#property indicator_type3   DRAW_LINE
#property indicator_color3  clrGreen
#property indicator_width3  1

//--- Buffer 3: IBH
#property indicator_label4  "IBH"
#property indicator_type4   DRAW_LINE
#property indicator_color4  clrDodgerBlue
#property indicator_style4  STYLE_DASH
#property indicator_width4  1

//--- Buffer 4: IBL
#property indicator_label5  "IBL"
#property indicator_type5   DRAW_LINE
#property indicator_color5  clrDodgerBlue
#property indicator_style5  STYLE_DASH
#property indicator_width5  1

//--- Buffers 5-9: HVN1-HVN5
#property indicator_label6  "HVN1"
#property indicator_type6   DRAW_LINE
#property indicator_color6  clrPurple
#property indicator_width6  1

#property indicator_label7  "HVN2"
#property indicator_type7   DRAW_LINE
#property indicator_color7  clrPurple
#property indicator_width7  1

#property indicator_label8  "HVN3"
#property indicator_type8   DRAW_LINE
#property indicator_color8  clrPurple
#property indicator_width8  1

#property indicator_label9  "HVN4"
#property indicator_type9   DRAW_LINE
#property indicator_color9  clrPurple
#property indicator_width9  1

#property indicator_label10 "HVN5"
#property indicator_type10  DRAW_LINE
#property indicator_color10 clrPurple
#property indicator_width10 1

//--- Buffers 10-14: LVN1-LVN5
#property indicator_label11 "LVN1"
#property indicator_type11  DRAW_LINE
#property indicator_color11 clrOrange
#property indicator_width11 1

#property indicator_label12 "LVN2"
#property indicator_type12  DRAW_LINE
#property indicator_color12 clrOrange
#property indicator_width12 1

#property indicator_label13 "LVN3"
#property indicator_type13  DRAW_LINE
#property indicator_color13 clrOrange
#property indicator_width13 1

#property indicator_label14 "LVN4"
#property indicator_type14  DRAW_LINE
#property indicator_color14 clrOrange
#property indicator_width14 1

#property indicator_label15 "LVN5"
#property indicator_type15  DRAW_LINE
#property indicator_color15 clrOrange
#property indicator_width15 1

//--- Buffer 15: POC (same as POC — kept for backward compatibility)
#property indicator_label16 "POC"
#property indicator_type16  DRAW_LINE
#property indicator_color16 clrAqua
#property indicator_width16 1

//--- Buffer 16: VAH (same as VAH — kept for backward compatibility)
#property indicator_label17 "VAH"
#property indicator_type17  DRAW_LINE
#property indicator_color17 clrGreen
#property indicator_width17 1

//--- Buffer 17: VAL (same as VAL — kept for backward compatibility)
#property indicator_label18 "VAL"
#property indicator_type18  DRAW_LINE
#property indicator_color18 clrGreen
#property indicator_width18 1

//--- Buffer 18: BullPOC
#property indicator_label19 "BullPOC"
#property indicator_type19  DRAW_NONE

//--- Buffer 19: BearPOC
#property indicator_label20 "BearPOC"
#property indicator_type20  DRAW_NONE

//--- Buffer 20: TotalVolume
#property indicator_label21 "TotalVolume"
#property indicator_type21  DRAW_NONE
#property indicator_color21 clrGray

//--- Buffer 21: TotalBullVolume
#property indicator_label22 "TotalBullVolume"
#property indicator_type22  DRAW_NONE
#property indicator_color22 clrGreen

//--- Buffer 22: TotalBearVolume
#property indicator_label23 "TotalBearVolume"
#property indicator_type23  DRAW_NONE
#property indicator_color23 clrRed

//--- Input parameters — MUST match Java @Parameter annotations exactly
input int    InpSessionType      = 1;      // Session type: 1-4=Prev Day/Week/Month/Year, 5-8=Actual, 9=PrevSwing, 10=ActualSwing
input int    InpProfileRows      = 150;    // Number of bins for Range-Based mode (10-500)
input int    InpBinSizeMode      = 2;      // Bin size: 1=Range-Based, 2=Fixed Tick Size
input int    InpTicksPerBin      = 3;      // Ticks per bin — Fixed Tick Size mode only (1-1000)
input double InpValueAreaPct     = 70.0;   // Value Area percentage (30-95)
input int    InpHvnCount         = 5;      // HVN nodes to detect (1-10)
input int    InpHvnThresholdPct  = 20;     // Min % of max vol for HVN (10-90)
input int    InpLvnThresholdPct  = 40;     // Max % of max vol for LVN (10-90)
input bool   InpEnableLVN        = false;  // Enable Low Volume Node detection
input bool   InpEnableVCP        = false;  // Enable Volume Cluster Profile (Gaussian)
input double InpClusterSpread    = 6.0;    // VCP Gaussian sigma (0.5-20.0)
input int    InpMaxClusterCenters= 2;      // VCP max cluster peaks (1-10)
input int    InpPivotMethod      = 1;      // ZigZag reversal: 1=Percentage, 2=Fixed Ticks, 3=ATR Multiple
input double InpPivotPct         = 0.5;    // Reversal threshold % of price (PivotMethod=1)
input int    InpPivotTicks       = 50;     // Reversal threshold in ticks (PivotMethod=2)
input double InpPivotATRMultiple = 1.5;    // Reversal threshold as ATR multiple (PivotMethod=3)
input int    InpPivotATRPeriod   = 14;     // ATR period for pivot (PivotMethod=3)
input int    InpIBMinutes        = 0;      // IB period override in minutes (0 = use session-type default)

//--- Display parameters (visual profile on chart)
input bool   InpShowProfile      = true;   // Show visual profile on chart
input bool   InpShowVAShading    = true;   // Show Value Area shading
input bool   InpShowIBBox        = true;   // Show Initial Balance box
input bool   InpShowLevelLabels  = true;   // Show level labels (POC, VAH, ...)
input bool   InpShowStats        = false;  // Show session statistics panel
input int    InpMaxDisplaySess   = 15;     // Max sessions to display (1-50)

//--- Output buffers
double POCBuffer[];
double VAHBuffer[];
double VALBuffer[];
double IBHBuffer[];
double IBLBuffer[];
double HVN1Buffer[];
double HVN2Buffer[];
double HVN3Buffer[];
double HVN4Buffer[];
double HVN5Buffer[];
double LVN1Buffer[];
double LVN2Buffer[];
double LVN3Buffer[];
double LVN4Buffer[];
double LVN5Buffer[];
double VPOCBuffer[];
double VVAHBuffer[];
double VVALBuffer[];
double BullPOCBuffer[];
double BearPOCBuffer[];
double TotalVolumeBuffer[];
double TotalBullVolumeBuffer[];
double TotalBearVolumeBuffer[];

//--- Volume bins (reused each calculation, sized to MAX_BINS)
#define MAX_BINS 2000
double volumeBins[];
double bullVolumeBins[];
double bearVolumeBins[];
double clusterBins[];

//--- Session tracking — mirrors Java instance variables exactly
datetime currentSessionStart = 0;
datetime currentSessionEnd   = 0;
datetime prevSessionStart    = 0;
datetime prevSessionEnd      = 0;

//--- Previous session profile results
double prevPOC    = 0;
double prevVAH    = 0;
double prevVAL    = 0;
double prevIBH    = 0;
double prevIBL    = 0;
double prevHVN[5];
double prevLVN[5];

//--- VCP results
double prevVPOC = 0;
double prevVVAH = 0;
double prevVVAL = 0;

//--- Bull/Bear POC results
double prevBullPOC = 0;
double prevBearPOC = 0;

//--- Total volume results
double prevTotalVolume     = 0;
double prevTotalBullVolume = 0;
double prevTotalBearVolume = 0;

//--- ZigZag state — mirrors Java fields
int      zzDirection       = 0;     // +1 up, -1 down, 0 uninitialized
double   zzPivotHigh       = 0;
double   zzPivotLow        = DBL_MAX;
datetime zzPivotHighTime   = 0;
datetime zzPivotLowTime    = 0;
datetime zzSessionStart    = 0;
datetime zzLastPivotTime   = 0;
double   zzLastPivotPrice  = 0;
int      zzLastPivotDir    = 0;     // 1=swing high, -1=swing low

//--- Validated parameters
int    ExtProfileRows;
double ExtValueAreaPct;
int    ExtSessionType;
int    lastNumBins = 0;

//--- Display tracking
double lastSessionHigh = 0;
double lastSessionLow  = 0;

//+------------------------------------------------------------------+
void OnInit()
{
   // Validate parameters — mirrors Java SQUtils.fixAllowedRange
   ExtSessionType  = (int)MathMax(1, MathMin(10, InpSessionType));
   ExtProfileRows  = (int)MathMax(10, MathMin(500, InpProfileRows));
   ExtValueAreaPct = MathMax(30.0, MathMin(95.0, InpValueAreaPct));

   ArrayResize(volumeBins,     MAX_BINS);
   ArrayResize(bullVolumeBins, MAX_BINS);
   ArrayResize(bearVolumeBins, MAX_BINS);
   ArrayResize(clusterBins,    MAX_BINS);
   ArrayInitialize(volumeBins,     0.0);
   ArrayInitialize(bullVolumeBins, 0.0);
   ArrayInitialize(bearVolumeBins, 0.0);
   ArrayInitialize(clusterBins,    0.0);

   // Bind all 23 output buffers
   SetIndexBuffer(0,  POCBuffer,             INDICATOR_DATA);
   SetIndexBuffer(1,  VAHBuffer,             INDICATOR_DATA);
   SetIndexBuffer(2,  VALBuffer,             INDICATOR_DATA);
   SetIndexBuffer(3,  IBHBuffer,             INDICATOR_DATA);
   SetIndexBuffer(4,  IBLBuffer,             INDICATOR_DATA);
   SetIndexBuffer(5,  HVN1Buffer,            INDICATOR_DATA);
   SetIndexBuffer(6,  HVN2Buffer,            INDICATOR_DATA);
   SetIndexBuffer(7,  HVN3Buffer,            INDICATOR_DATA);
   SetIndexBuffer(8,  HVN4Buffer,            INDICATOR_DATA);
   SetIndexBuffer(9,  HVN5Buffer,            INDICATOR_DATA);
   SetIndexBuffer(10, LVN1Buffer,            INDICATOR_DATA);
   SetIndexBuffer(11, LVN2Buffer,            INDICATOR_DATA);
   SetIndexBuffer(12, LVN3Buffer,            INDICATOR_DATA);
   SetIndexBuffer(13, LVN4Buffer,            INDICATOR_DATA);
   SetIndexBuffer(14, LVN5Buffer,            INDICATOR_DATA);
   SetIndexBuffer(15, VPOCBuffer,            INDICATOR_DATA);
   SetIndexBuffer(16, VVAHBuffer,            INDICATOR_DATA);
   SetIndexBuffer(17, VVALBuffer,            INDICATOR_DATA);
   SetIndexBuffer(18, BullPOCBuffer,         INDICATOR_DATA);
   SetIndexBuffer(19, BearPOCBuffer,         INDICATOR_DATA);
   SetIndexBuffer(20, TotalVolumeBuffer,     INDICATOR_DATA);
   SetIndexBuffer(21, TotalBullVolumeBuffer, INDICATOR_DATA);
   SetIndexBuffer(22, TotalBearVolumeBuffer, INDICATOR_DATA);

   // All buffers use series indexing (shift 0 = newest bar)
   ArraySetAsSeries(POCBuffer,             true);
   ArraySetAsSeries(VAHBuffer,             true);
   ArraySetAsSeries(VALBuffer,             true);
   ArraySetAsSeries(IBHBuffer,             true);
   ArraySetAsSeries(IBLBuffer,             true);
   ArraySetAsSeries(HVN1Buffer,            true);
   ArraySetAsSeries(HVN2Buffer,            true);
   ArraySetAsSeries(HVN3Buffer,            true);
   ArraySetAsSeries(HVN4Buffer,            true);
   ArraySetAsSeries(HVN5Buffer,            true);
   ArraySetAsSeries(LVN1Buffer,            true);
   ArraySetAsSeries(LVN2Buffer,            true);
   ArraySetAsSeries(LVN3Buffer,            true);
   ArraySetAsSeries(LVN4Buffer,            true);
   ArraySetAsSeries(LVN5Buffer,            true);
   ArraySetAsSeries(VPOCBuffer,            true);
   ArraySetAsSeries(VVAHBuffer,            true);
   ArraySetAsSeries(VVALBuffer,            true);
   ArraySetAsSeries(BullPOCBuffer,         true);
   ArraySetAsSeries(BearPOCBuffer,         true);
   ArraySetAsSeries(TotalVolumeBuffer,     true);
   ArraySetAsSeries(TotalBullVolumeBuffer, true);
   ArraySetAsSeries(TotalBearVolumeBuffer, true);

   IndicatorSetInteger(INDICATOR_DIGITS, _Digits);

   string binDesc = (InpBinSizeMode == 2)
      ? ("F" + IntegerToString(InpTicksPerBin))
      : IntegerToString(ExtProfileRows);
   IndicatorSetString(INDICATOR_SHORTNAME,
      "VP(" + GetSessionName(ExtSessionType) + "," +
      binDesc + "," +
      DoubleToString(ExtValueAreaPct, 0) + ")");

   ResetSessionState();

   // Initialize visual display
   if(InpShowProfile)
   {
      string prefix = "VP" + IntegerToString(ChartID()) + "_" + IntegerToString(GetHashCode()) + "_";
      VPDisplayInit(prefix, MathMin(VP_MAX_HIST, MathMax(1, InpMaxDisplaySess)));
      g_vpShowStats = InpShowStats;
   }
}

//+------------------------------------------------------------------+
int GetHashCode()
{
   // Simple hash from parameters for unique prefix
   return (ExtSessionType * 1000 + ExtProfileRows + InpBinSizeMode * 100 + InpTicksPerBin);
}

//+------------------------------------------------------------------+
void OnDeinit(const int reason)
{
   if(InpShowProfile)
      VPDisplayDeinit();
}

//+------------------------------------------------------------------+
void OnChartEvent(const int id, const long &lparam, const double &dparam, const string &sparam)
{
   if(InpShowProfile)
      VPDisplayOnChartEvent(id, lparam, dparam, sparam);
}

//+------------------------------------------------------------------+
void ResetSessionState()
{
   currentSessionStart  = 0;
   currentSessionEnd    = 0;
   prevSessionStart     = 0;
   prevSessionEnd       = 0;
   prevPOC = 0; prevVAH = 0; prevVAL = 0;
   prevIBH = 0; prevIBL = 0;
   ArrayInitialize(prevHVN, 0.0);
   ArrayInitialize(prevLVN, 0.0);
   prevVPOC = 0; prevVVAH = 0; prevVVAL = 0;
   prevBullPOC = 0; prevBearPOC = 0;
   prevTotalVolume = 0; prevTotalBullVolume = 0; prevTotalBearVolume = 0;

   // Reset ZigZag state
   zzDirection      = 0;
   zzPivotHigh      = 0;
   zzPivotLow       = DBL_MAX;
   zzPivotHighTime  = 0;
   zzPivotLowTime   = 0;
   zzSessionStart   = 0;
   zzLastPivotTime  = 0;
   zzLastPivotPrice = 0;
   zzLastPivotDir   = 0;
}

//+------------------------------------------------------------------+
//| Returns true if datetime falls on a Sunday.                      |
//| Java: Calendar.DAY_OF_WEEK == Calendar.SUNDAY                    |
//| MQL5: MqlDateTime.day_of_week == 0 (0=Sunday)                   |
//+------------------------------------------------------------------+
bool IsSunday(datetime t)
{
   MqlDateTime dt;
   TimeToStruct(t, dt);
   return (dt.day_of_week == 0);
}

//+------------------------------------------------------------------+
//| IB period in seconds per session type — matches Java exactly     |
//| Java: getIBPeriodMillis() ÷ 1000                                |
//+------------------------------------------------------------------+
int GetIBPeriodSeconds()
{
   if(InpIBMinutes > 0)
      return InpIBMinutes * 60;

   int baseType;
   if     (ExtSessionType == 1 || ExtSessionType == 5)  baseType = 1;
   else if(ExtSessionType == 2 || ExtSessionType == 6)  baseType = 2;
   else if(ExtSessionType == 3 || ExtSessionType == 7)  baseType = 3;
   else if(ExtSessionType == 4 || ExtSessionType == 8)  baseType = 4;
   else if(ExtSessionType == 9 || ExtSessionType == 10) baseType = 5; // ZigZag
   else baseType = 1;

   switch(baseType)
   {
      case 1: return 3600;        // 60 minutes  (Daily)
      case 2: return 43200;       // 12 hours    (Weekly)
      case 3: return 86400;       // 24 hours    (Monthly)
      case 4: return 6 * 86400;   // 6 days      (Yearly)
      case 5: return 3600;        // 60 minutes  (ZigZag default)
      default: return 3600;
   }
}

//+------------------------------------------------------------------+
//| Compute ATR inline — matches Java computeATR()                   |
//+------------------------------------------------------------------+
double ComputeATR(int period, int currentBar,
                  const double &high[], const double &low[], const double &close[],
                  int totalBars)
{
   int available = MathMin(period, currentBar);
   if(available <= 0)
      return high[currentBar] - low[currentBar];

   double sum = 0;
   for(int i = currentBar; i < currentBar + available && i < totalBars; i++)
   {
      double hi = high[i];
      double lo = low[i];
      double pc = (i + 1 < totalBars) ? close[i + 1] : hi;
      double tr = MathMax(hi - lo, MathMax(MathAbs(hi - pc), MathAbs(lo - pc)));
      sum += tr;
   }
   return sum / available;
}

//+------------------------------------------------------------------+
//| Compute pivot reversal threshold — matches Java                  |
//+------------------------------------------------------------------+
double ComputePivotThreshold(int currentBar,
                             const double &high[], const double &low[], const double &close[],
                             int totalBars)
{
   double tickSize = _Point;
   switch(InpPivotMethod)
   {
      case 2: // Fixed Ticks
         return InpPivotTicks * tickSize;
      case 3: // ATR Multiple
         return ComputeATR(InpPivotATRPeriod, currentBar, high, low, close, totalBars) * InpPivotATRMultiple;
      default: // 1 = Percentage
      {
         double refPrice = (zzDirection == 1) ? zzPivotHigh : zzPivotLow;
         if(refPrice <= 0)
            refPrice = close[currentBar];
         return refPrice * (InpPivotPct / 100.0);
      }
   }
}

//+------------------------------------------------------------------+
//| Detect ZigZag pivot — matches Java detectZigZagPivot()           |
//| Returns true if a new pivot was confirmed on this bar.           |
//+------------------------------------------------------------------+
bool DetectZigZagPivot(int currentBar,
                       const datetime &time[], const double &high[], const double &low[], const double &close[],
                       int totalBars)
{
   datetime curTime = time[currentBar];
   double hi = high[currentBar];
   double lo = low[currentBar];

   // Initialize on first bar
   if(zzDirection == 0)
   {
      zzPivotHigh     = hi;
      zzPivotLow      = lo;
      zzPivotHighTime = curTime;
      zzPivotLowTime  = curTime;
      zzDirection     = 1; // start assuming up
      zzSessionStart  = curTime;
      return false;
   }

   bool pivotFound = false;
   double threshold = ComputePivotThreshold(currentBar, high, low, close, totalBars);

   if(zzDirection == 1)
   {
      // Tracking upward: looking for swing high
      if(hi > zzPivotHigh)
      {
         zzPivotHigh     = hi;
         zzPivotHighTime = curTime;
      }
      // Check if price reversed down enough
      if((zzPivotHigh - lo) >= threshold)
      {
         zzLastPivotPrice = zzPivotHigh;
         zzLastPivotDir   = 1; // swing high
         zzLastPivotTime  = zzPivotHighTime;
         zzDirection      = -1; // now looking down
         zzPivotLow       = lo;
         zzPivotLowTime   = curTime;
         pivotFound       = true;
      }
   }
   else
   {
      // Tracking downward: looking for swing low
      if(lo < zzPivotLow)
      {
         zzPivotLow     = lo;
         zzPivotLowTime = curTime;
      }
      // Check if price reversed up enough
      if((hi - zzPivotLow) >= threshold)
      {
         zzLastPivotPrice = zzPivotLow;
         zzLastPivotDir   = -1; // swing low
         zzLastPivotTime  = zzPivotLowTime;
         zzDirection      = 1; // now looking up
         zzPivotHigh      = hi;
         zzPivotHighTime  = curTime;
         pivotFound       = true;
      }
   }

   return pivotFound;
}

//+------------------------------------------------------------------+
//| Main calculation — mirrors Java OnBarUpdate()                    |
//+------------------------------------------------------------------+
int OnCalculate(const int rates_total,
                const int prev_calculated,
                const datetime &time[],
                const double   &open[],
                const double   &high[],
                const double   &low[],
                const double   &close[],
                const long     &tick_volume[],
                const long     &volume[],
                const int      &spread[])
{
   if(rates_total < 2) return(0);

   // Reverse all input arrays to match buffer direction
   ArraySetAsSeries(time,        true);
   ArraySetAsSeries(open,        true);
   ArraySetAsSeries(high,        true);
   ArraySetAsSeries(low,         true);
   ArraySetAsSeries(close,       true);
   ArraySetAsSeries(tick_volume, true);
   ArraySetAsSeries(volume,      true);

   int startBar;
   if(prev_calculated == 0) {
      ResetSessionState();
      startBar = rates_total - 1;
   } else {
      startBar = rates_total - prev_calculated;
      if(startBar < 0) startBar = 0;
   }

   bool isZigZag   = (ExtSessionType == 9 || ExtSessionType == 10);
   bool actualMode = (ExtSessionType >= 5);

   // Process bars from oldest (startBar) to newest (0)
   for(int i = startBar; i >= 0 && !IsStopped(); i--)
   {
      datetime barTime = time[i];

      if(isZigZag)
      {
         // ZigZag session mode — matches Java exactly
         bool actualZZ    = (ExtSessionType == 10);
         bool pivotFound  = DetectZigZagPivot(i, time, high, low, close, rates_total);

         if(!actualZZ)
         {
            // Previous ZigZag: recalculate when a new pivot is confirmed
            if(pivotFound && zzSessionStart > 0 && zzLastPivotTime > zzSessionStart)
            {
               prevSessionStart = zzSessionStart;
               prevSessionEnd   = zzLastPivotTime;
               CalculateVolumeProfile(i, time, open, high, low, close, volume, tick_volume, rates_total);
               zzSessionStart = zzLastPivotTime;
            }
         }
         else
         {
            // Actual ZigZag: recalculate every bar from session start
            if(pivotFound && zzSessionStart > 0)
            {
               prevSessionStart = zzSessionStart;
               prevSessionEnd   = zzLastPivotTime;
               CalculateVolumeProfile(i, time, open, high, low, close, volume, tick_volume, rates_total);
               zzSessionStart = zzLastPivotTime;
            }
            if(zzSessionStart > 0 && barTime > zzSessionStart)
            {
               prevSessionStart = zzSessionStart;
               prevSessionEnd   = barTime;
               CalculateVolumeProfile(i, time, open, high, low, close, volume, tick_volume, rates_total);
            }
         }
      }
      else if(!actualMode)
      {
         // PREVIOUS session mode (SessionType 1-4)
         if(currentSessionEnd == 0 || barTime >= currentSessionEnd)
         {
            prevSessionStart = currentSessionStart;
            prevSessionEnd   = currentSessionEnd;
            CalculateSessionBoundaries(barTime);

            // Sunday skip — Java applies this check twice
            if(prevSessionStart > 0 && IsSunday(prevSessionStart))
            {
               MqlDateTime sd;
               TimeToStruct(prevSessionStart, sd);
               sd.hour = 0; sd.min = 0; sd.sec = 0;
               prevSessionStart = StructToTime(sd) - 2 * 86400;
               prevSessionEnd   = prevSessionStart + 86400;
            }
            if(prevSessionStart > 0 && IsSunday(prevSessionStart))
            {
               MqlDateTime sd;
               TimeToStruct(prevSessionStart, sd);
               sd.hour = 0; sd.min = 0; sd.sec = 0;
               prevSessionStart = StructToTime(sd) - 2 * 86400;
               prevSessionEnd   = prevSessionStart + 86400;
            }

            if(prevSessionStart > 0 && prevSessionEnd > 0)
               CalculateVolumeProfile(i, time, open, high, low, close, volume, tick_volume, rates_total);
         }
      }
      else
      {
         // ACTUAL session mode (SessionType 5-8)
         if(currentSessionEnd == 0 || barTime >= currentSessionEnd)
            CalculateSessionBoundaries(barTime);

         prevSessionStart = currentSessionStart;
         prevSessionEnd   = (datetime)MathMin((double)currentSessionEnd, (double)barTime);

         if(prevSessionStart > 0 && prevSessionEnd > prevSessionStart)
            CalculateVolumeProfile(i, time, open, high, low, close, volume, tick_volume, rates_total);
      }

      // Assign all 23 output values to this bar
      POCBuffer[i]             = prevPOC;
      VAHBuffer[i]             = prevVAH;
      VALBuffer[i]             = prevVAL;
      IBHBuffer[i]             = (prevIBH == 0) ? EMPTY_VALUE : prevIBH;
      IBLBuffer[i]             = (prevIBL == 0) ? EMPTY_VALUE : prevIBL;
      HVN1Buffer[i]            = (prevHVN[0] == 0) ? EMPTY_VALUE : prevHVN[0];
      HVN2Buffer[i]            = (prevHVN[1] == 0) ? EMPTY_VALUE : prevHVN[1];
      HVN3Buffer[i]            = (prevHVN[2] == 0) ? EMPTY_VALUE : prevHVN[2];
      HVN4Buffer[i]            = (prevHVN[3] == 0) ? EMPTY_VALUE : prevHVN[3];
      HVN5Buffer[i]            = (prevHVN[4] == 0) ? EMPTY_VALUE : prevHVN[4];
      LVN1Buffer[i]            = (prevLVN[0] == 0) ? EMPTY_VALUE : prevLVN[0];
      LVN2Buffer[i]            = (prevLVN[1] == 0) ? EMPTY_VALUE : prevLVN[1];
      LVN3Buffer[i]            = (prevLVN[2] == 0) ? EMPTY_VALUE : prevLVN[2];
      LVN4Buffer[i]            = (prevLVN[3] == 0) ? EMPTY_VALUE : prevLVN[3];
      LVN5Buffer[i]            = (prevLVN[4] == 0) ? EMPTY_VALUE : prevLVN[4];
      VPOCBuffer[i]            = prevVPOC;
      VVAHBuffer[i]            = prevVVAH;
      VVALBuffer[i]            = prevVVAL;
      BullPOCBuffer[i]         = prevBullPOC;
      BearPOCBuffer[i]         = prevBearPOC;
      TotalVolumeBuffer[i]     = prevTotalVolume;
      TotalBullVolumeBuffer[i] = prevTotalBullVolume;
      TotalBearVolumeBuffer[i] = prevTotalBearVolume;
   }

   return(rates_total);
}

//+------------------------------------------------------------------+
//| Compute session start/end boundaries — matches Java exactly      |
//+------------------------------------------------------------------+
void CalculateSessionBoundaries(datetime curTime)
{
   MqlDateTime dt;
   TimeToStruct(curTime, dt);
   dt.hour = 0; dt.min = 0; dt.sec = 0;
   datetime dayStart = StructToTime(dt);

   int baseType;
   if     (ExtSessionType == 1 || ExtSessionType == 5) baseType = 1; // Daily
   else if(ExtSessionType == 2 || ExtSessionType == 6) baseType = 2; // Weekly
   else if(ExtSessionType == 3 || ExtSessionType == 7) baseType = 3; // Monthly
   else if(ExtSessionType == 4 || ExtSessionType == 8) baseType = 4; // Yearly
   else                                                baseType = 1;

   switch(baseType)
   {
      case 1: // Daily — 00:00 to 00:00 next day
         currentSessionStart = dayStart;
         currentSessionEnd   = dayStart + 86400;
         // Java: if(isSunday(currentSessionStart)) shift to Friday
         if(IsSunday(currentSessionStart))
         {
            currentSessionStart -= 2 * 86400;
            currentSessionEnd    = currentSessionStart + 86400;
         }
         break;

      case 2: { // Weekly — Monday to Monday
         MqlDateTime wd;
         TimeToStruct(dayStart, wd);
         int daysFromMon = (wd.day_of_week == 0) ? 6 : wd.day_of_week - 1;
         currentSessionStart = dayStart - (datetime)(daysFromMon * 86400);
         currentSessionEnd   = currentSessionStart + 7 * 86400;
         break;
      }

      case 3: { // Monthly — 1st of month to 1st of next month
         MqlDateTime md;
         TimeToStruct(dayStart, md);
         md.day = 1;
         currentSessionStart = StructToTime(md);
         md.mon++;
         if(md.mon > 12) { md.mon = 1; md.year++; }
         currentSessionEnd = StructToTime(md);
         break;
      }

      case 4: { // Yearly — Jan 1 to Jan 1 next year
         MqlDateTime yd;
         TimeToStruct(curTime, yd);
         yd.mon = 1; yd.day = 1; yd.hour = 0; yd.min = 0; yd.sec = 0;
         currentSessionStart = StructToTime(yd);
         yd.year++;
         currentSessionEnd = StructToTime(yd);
         break;
      }
   }
}

//+------------------------------------------------------------------+
//| Core volume profile calculation — matches Java exactly           |
//+------------------------------------------------------------------+
void CalculateVolumeProfile(int              currentBar,
                            const datetime  &time[],
                            const double    &open[],
                            const double    &high[],
                            const double    &low[],
                            const double    &close[],
                            const long      &volume[],
                            const long      &tick_volume[],
                            int              totalBars)
{
   //--- First pass: find session high/low + Initial Balance ---
   double sessionHigh        = -DBL_MAX;
   double sessionLow         =  DBL_MAX;
   int    barsInSession      = 0;

   datetime ibEndTime = prevSessionStart + GetIBPeriodSeconds();
   double   ibHigh    = -DBL_MAX;
   double   ibLow     =  DBL_MAX;

   for(int i = currentBar; i < totalBars; i++)
   {
      datetime barTime = time[i];
      if(barTime < prevSessionStart) break;
      if(barTime >= prevSessionStart && barTime < prevSessionEnd)
      {
         // Skip Sunday bars — Java: if(isSunday(barTime)) continue
         if(IsSunday(barTime)) continue;

         double hi = high[i];
         double lo = low[i];
         if(hi > sessionHigh) sessionHigh = hi;
         if(lo < sessionLow)  sessionLow  = lo;
         barsInSession++;

         // Track IB
         if(barTime < ibEndTime)
         {
            if(hi > ibHigh) ibHigh = hi;
            if(lo < ibLow)  ibLow  = lo;
         }
      }
   }

   if(barsInSession == 0 || sessionHigh <= sessionLow) return;

   // Store session extents for display
   lastSessionHigh = sessionHigh;
   lastSessionLow  = sessionLow;

   // Store IB — Java: if(ibHigh > ibLow)
   if(ibHigh > ibLow)
   {
      prevIBH = ibHigh;
      prevIBL = ibLow;
   }

   double range = sessionHigh - sessionLow;

   //--- Compute bin size and count based on BinSizeMode ---
   int    numBins;
   double binSize;

   if(InpBinSizeMode == 2)
   {
      // Fixed tick size: bin size = TicksPerBin * tickSize
      binSize = InpTicksPerBin * _Point;
      numBins = (int)MathCeil(range / binSize);
      numBins = (int)MathMax(1, MathMin(numBins, MAX_BINS));
   }
   else
   {
      // Range-based: bin size = range / ProfileRows
      numBins = ExtProfileRows;
      binSize = range / numBins;
   }
   lastNumBins = numBins;

   // Clear only the bins we will use
   for(int j = 0; j < numBins; j++)
   {
      volumeBins[j]     = 0.0;
      bullVolumeBins[j] = 0.0;
      bearVolumeBins[j] = 0.0;
   }

   //--- Second pass: accumulate volume into bins ---
   double totalVolume     = 0;
   double totalBullVolume = 0;
   double totalBearVolume = 0;

   for(int i = currentBar; i < totalBars; i++)
   {
      datetime barTime = time[i];
      if(barTime < prevSessionStart) break;
      if(barTime >= prevSessionStart && barTime < prevSessionEnd)
      {
         // Skip Sunday bars
         if(IsSunday(barTime)) continue;

         double cl = close[i];
         double op = open[i];
         double hi = high[i];
         double lo = low[i];
         // Volume fallback: prefer real volume, fall back to tick volume
         long v    = volume[i] > 0 ? volume[i] : tick_volume[i];
         double barVol = (double)v;

         // Bin index calculation — scale by 1000 to reduce floating-point precision issues
         int vBinHigh = (int)(((hi - sessionLow) * 1000) / (binSize * 1000));
         int vBinLow  = (int)(((lo - sessionLow) * 1000) / (binSize * 1000));

         vBinHigh = (int)MathMax(0, MathMin(numBins - 1, vBinHigh));
         vBinLow  = (int)MathMax(0, MathMin(numBins - 1, vBinLow));

         double binsSpanned = (double)(vBinHigh - vBinLow + 1);
         double perBin = barVol / binsSpanned;
         bool isBull = (cl >= op);

         for(int b = vBinLow; b <= vBinHigh; b++)
         {
            volumeBins[b] += perBin;
            if(isBull)
               bullVolumeBins[b] += perBin;
            else
               bearVolumeBins[b] += perBin;
         }

         totalVolume += barVol;
         if(isBull)
            totalBullVolume += barVol;
         else
            totalBearVolume += barVol;
      }
   }

   if(totalVolume == 0) return;

   //--- Find POC ---
   int    pocIndex  = 0;
   double maxVolume = volumeBins[0];
   for(int j = 1; j < numBins; j++)
   {
      if(volumeBins[j] > maxVolume)
      {
         maxVolume = volumeBins[j];
         pocIndex  = j;
      }
   }

   prevPOC = sessionLow + (pocIndex + 0.5) * binSize;

   //--- Calculate Value Area ---
   CalculateValueArea(pocIndex, totalVolume, binSize, sessionLow, numBins);

   //--- Find HVN and optionally LVN ---
   FindHVNs(pocIndex, binSize, sessionLow, numBins);
   if(InpEnableLVN)
      FindLVNs(pocIndex, binSize, sessionLow, numBins);
   else
      ArrayInitialize(prevLVN, 0.0);

   //--- Volume Cluster Profile (VCP) enhancement ---
   if(InpEnableVCP)
      ApplyClusterEnhancement(numBins, binSize, sessionLow, totalVolume);
   else
   {
      prevVPOC = prevPOC;
      prevVVAH = prevVAH;
      prevVVAL = prevVAL;
   }

   //--- Bull/Bear POC ---
   int bullPocIdx = 0;
   double bullMax = bullVolumeBins[0];
   int bearPocIdx = 0;
   double bearMax = bearVolumeBins[0];
   for(int j = 1; j < numBins; j++)
   {
      if(bullVolumeBins[j] > bullMax)
      {
         bullMax    = bullVolumeBins[j];
         bullPocIdx = j;
      }
      if(bearVolumeBins[j] > bearMax)
      {
         bearMax    = bearVolumeBins[j];
         bearPocIdx = j;
      }
   }
   prevBullPOC = (bullMax > 0) ? sessionLow + (bullPocIdx + 0.5) * binSize : 0;
   prevBearPOC = (bearMax > 0) ? sessionLow + (bearPocIdx + 0.5) * binSize : 0;

   //--- Store total volume figures ---
   prevTotalVolume     = totalVolume;
   prevTotalBullVolume = totalBullVolume;
   prevTotalBearVolume = totalBearVolume;

   //--- Push to visual display ---
   if(InpShowProfile)
   {
      if(InpEnableVCP)
         VPDisplayPushSession(
            prevSessionStart, prevSessionEnd,
            lastSessionHigh, lastSessionLow,
            prevPOC, prevVAH, prevVAL,
            prevIBH, prevIBL,
            prevHVN, prevLVN,
            lastNumBins,
            clusterBins, bullVolumeBins, bearVolumeBins,
            InpMaxDisplaySess);
      else
         VPDisplayPushSession(
            prevSessionStart, prevSessionEnd,
            lastSessionHigh, lastSessionLow,
            prevPOC, prevVAH, prevVAL,
            prevIBH, prevIBL,
            prevHVN, prevLVN,
            lastNumBins,
            volumeBins, bullVolumeBins, bearVolumeBins,
            InpMaxDisplaySess);
   }
}

//+------------------------------------------------------------------+
//| Value Area calculation — mirrors Java calculateValueArea()       |
//+------------------------------------------------------------------+
void CalculateValueArea(int pocIndex, double totalVolume, double binSize, double sessionLow, int numBins)
{
   double targetVolume      = totalVolume * (ExtValueAreaPct / 100.0);
   double accumulatedVolume = volumeBins[pocIndex];

   int upperIndex = pocIndex;
   int lowerIndex = pocIndex;

   while(accumulatedVolume < targetVolume)
   {
      bool canGoUp   = (upperIndex + 1) < numBins;
      bool canGoDown = (lowerIndex - 1) >= 0;
      if(!canGoUp && !canGoDown) break;

      double volumeAbove = canGoUp   ? volumeBins[upperIndex + 1] : -1;
      double volumeBelow = canGoDown ? volumeBins[lowerIndex - 1] : -1;

      if(volumeAbove >= volumeBelow)
      {
         upperIndex++;
         accumulatedVolume += volumeBins[upperIndex];
      }
      else
      {
         lowerIndex--;
         accumulatedVolume += volumeBins[lowerIndex];
      }
   }

   prevVAL = sessionLow + lowerIndex * binSize;
   prevVAH = sessionLow + (upperIndex + 1) * binSize;
}

//+------------------------------------------------------------------+
//| Find High Volume Nodes — mirrors Java findHVNs()                 |
//+------------------------------------------------------------------+
void FindHVNs(int pocIndex, double binSize, double sessionLow, int numBins)
{
   ArrayInitialize(prevHVN, 0.0);

   double maxVol = 0;
   for(int j = 0; j < numBins; j++)
      if(volumeBins[j] > maxVol) maxVol = volumeBins[j];
   if(maxVol <= 0) return;

   double threshold = maxVol * InpHvnThresholdPct / 100.0;
   int    maxNodes  = (int)MathMin(InpHvnCount, 5);

   // Collect candidates: local maxima above threshold, excluding POC
   int    candidateIdx[];
   double candidateVol[];
   ArrayResize(candidateIdx, numBins);
   ArrayResize(candidateVol, numBins);
   int candidateCount = 0;

   for(int j = 0; j < numBins; j++)
   {
      if(j == pocIndex) continue;
      if(volumeBins[j] < threshold) continue;

      double left  = (j > 0)           ? volumeBins[j - 1] : -1;
      double right = (j < numBins - 1) ? volumeBins[j + 1] : -1;

      if(volumeBins[j] > left && volumeBins[j] > right)
      {
         candidateIdx[candidateCount] = j;
         candidateVol[candidateCount] = volumeBins[j];
         candidateCount++;
      }
   }

   // Sort by volume descending (selection sort)
   for(int a = 0; a < candidateCount - 1; a++)
   {
      int best = a;
      for(int b = a + 1; b < candidateCount; b++)
         if(candidateVol[b] > candidateVol[best]) best = b;
      if(best != a)
      {
         double tmpV = candidateVol[a]; candidateVol[a] = candidateVol[best]; candidateVol[best] = tmpV;
         int    tmpI = candidateIdx[a]; candidateIdx[a] = candidateIdx[best]; candidateIdx[best] = tmpI;
      }
   }

   // Greedy selection with valley check
   int accepted[];
   ArrayResize(accepted, maxNodes);
   int acceptedCount = 0;

   for(int c = 0; c < candidateCount && acceptedCount < maxNodes; c++)
   {
      int    idx = candidateIdx[c];
      double vol = candidateVol[c];
      bool   ok  = true;

      for(int k = 0; k < acceptedCount; k++)
      {
         int    prevIdx    = accepted[k];
         int    rangeStart = (int)MathMin(idx, prevIdx) + 1;
         int    rangeEnd   = (int)MathMax(idx, prevIdx);
         double minBetween = DBL_MAX;

         for(int m = rangeStart; m < rangeEnd; m++)
            if(volumeBins[m] < minBetween) minBetween = volumeBins[m];

         double smallerPeak = MathMin(vol, volumeBins[prevIdx]);
         if(minBetween >= smallerPeak * 0.5) { ok = false; break; }
      }

      if(ok)
      {
         accepted[acceptedCount]  = idx;
         prevHVN[acceptedCount]   = sessionLow + (idx + 0.5) * binSize;
         acceptedCount++;
      }
   }
}

//+------------------------------------------------------------------+
//| Find Low Volume Nodes — mirrors Java findLVNs()                  |
//+------------------------------------------------------------------+
void FindLVNs(int pocIndex, double binSize, double sessionLow, int numBins)
{
   ArrayInitialize(prevLVN, 0.0);

   double maxVol = 0;
   for(int j = 0; j < numBins; j++)
      if(volumeBins[j] > maxVol) maxVol = volumeBins[j];
   if(maxVol <= 0) return;

   double threshold = maxVol * InpLvnThresholdPct / 100.0;
   int    maxNodes  = (int)MathMin(InpHvnCount, 5); // Java reuses HvnCount

   // Collect candidates: local minima below threshold (j=1..numBins-2)
   int    candidateIdx[];
   double candidateVol[];
   ArrayResize(candidateIdx, numBins);
   ArrayResize(candidateVol, numBins);
   int candidateCount = 0;

   for(int j = 1; j < numBins - 1; j++)
   {
      if(volumeBins[j] > threshold) continue;
      if(volumeBins[j] < volumeBins[j - 1] && volumeBins[j] < volumeBins[j + 1])
      {
         candidateIdx[candidateCount] = j;
         candidateVol[candidateCount] = volumeBins[j];
         candidateCount++;
      }
   }

   // Sort by volume ascending (selection sort)
   for(int a = 0; a < candidateCount - 1; a++)
   {
      int best = a;
      for(int b = a + 1; b < candidateCount; b++)
         if(candidateVol[b] < candidateVol[best]) best = b;
      if(best != a)
      {
         double tmpV = candidateVol[a]; candidateVol[a] = candidateVol[best]; candidateVol[best] = tmpV;
         int    tmpI = candidateIdx[a]; candidateIdx[a] = candidateIdx[best]; candidateIdx[best] = tmpI;
      }
   }

   int count = (int)MathMin(maxNodes, candidateCount);
   for(int n = 0; n < count; n++)
      prevLVN[n] = sessionLow + (candidateIdx[n] + 0.5) * binSize;
}

//+------------------------------------------------------------------+
//| Gaussian Volume Cluster enhancement — matches Java exactly       |
//+------------------------------------------------------------------+
void ApplyClusterEnhancement(int numBins, double binSize, double sessionLow, double totalVolume)
{
   // 1. Find local peaks (bins >= both neighbors and above average)
   double avg = totalVolume / MathMax(1, numBins);
   int    peakIndices[];
   double peakValues[];
   ArrayResize(peakIndices, numBins);
   ArrayResize(peakValues,  numBins);
   int peakCount = 0;

   for(int j = 0; j < numBins; j++)
   {
      bool leftOk  = (j == 0)           || (volumeBins[j] >= volumeBins[j - 1]);
      bool rightOk = (j == numBins - 1) || (volumeBins[j] >= volumeBins[j + 1]);
      if(leftOk && rightOk && volumeBins[j] > avg)
      {
         peakIndices[peakCount] = j;
         peakValues[peakCount]  = volumeBins[j];
         peakCount++;
      }
   }

   // 2. Keep strongest N peaks
   int maxCenters = (int)MathMin(InpMaxClusterCenters, peakCount);
   if(maxCenters == 0)
   {
      prevVPOC = prevPOC;
      prevVVAH = prevVAH;
      prevVVAL = prevVAL;
      return;
   }

   // Simple selection sort for top N
   for(int a = 0; a < maxCenters; a++)
   {
      int bestIdx = a;
      for(int b = a + 1; b < peakCount; b++)
         if(peakValues[b] > peakValues[bestIdx]) bestIdx = b;
      if(bestIdx != a)
      {
         int    tmpI = peakIndices[a]; peakIndices[a] = peakIndices[bestIdx]; peakIndices[bestIdx] = tmpI;
         double tmpV = peakValues[a];  peakValues[a]  = peakValues[bestIdx];  peakValues[bestIdx]  = tmpV;
      }
   }

   // 3. Build Gaussian-enhanced profile
   double sigma = InpClusterSpread;
   for(int j = 0; j < numBins; j++)
      clusterBins[j] = 0;

   for(int c = 0; c < maxCenters; c++)
   {
      int    center    = peakIndices[c];
      double centerVol = peakValues[c];
      for(int j = 0; j < numBins; j++)
      {
         double dist   = (j - center) / sigma;
         double weight = MathExp(-0.5 * dist * dist);
         clusterBins[j] += centerVol * weight;
      }
   }

   // 4. Find VPOC from enhanced profile
   int    vpocIdx = 0;
   double vpocMax = clusterBins[0];
   for(int j = 1; j < numBins; j++)
   {
      if(clusterBins[j] > vpocMax)
      {
         vpocMax = clusterBins[j];
         vpocIdx = j;
      }
   }
   prevVPOC = sessionLow + (vpocIdx + 0.5) * binSize;

   // 5. Calculate Value Area on enhanced profile
   double clusterTotal = 0;
   for(int j = 0; j < numBins; j++)
      clusterTotal += clusterBins[j];

   double targetVolume = clusterTotal * (ExtValueAreaPct / 100.0);
   double accumulated  = clusterBins[vpocIdx];
   int    lo = vpocIdx;
   int    hi = vpocIdx;

   while(accumulated < targetVolume && (lo > 0 || hi < numBins - 1))
   {
      double expandLo = (lo > 0)           ? clusterBins[lo - 1] : 0;
      double expandHi = (hi < numBins - 1) ? clusterBins[hi + 1] : 0;
      if(expandLo >= expandHi && lo > 0)
      {
         lo--;
         accumulated += clusterBins[lo];
      }
      else if(hi < numBins - 1)
      {
         hi++;
         accumulated += clusterBins[hi];
      }
      else
      {
         lo--;
         accumulated += clusterBins[lo];
      }
   }

   prevVVAL = sessionLow + lo * binSize;
   prevVVAH = sessionLow + (hi + 1) * binSize;
}

//+------------------------------------------------------------------+
string GetSessionName(int sessionType)
{
   switch(sessionType)
   {
      case 1:  return "PrevDaily";
      case 2:  return "PrevWeekly";
      case 3:  return "PrevMonthly";
      case 4:  return "PrevYearly";
      case 5:  return "ActualDaily";
      case 6:  return "ActualWeekly";
      case 7:  return "ActualMonthly";
      case 8:  return "ActualYearly";
      case 9:  return "PrevSwing";
      case 10: return "ActualSwing";
      default: return "Unknown";
   }
}
//+------------------------------------------------------------------+
