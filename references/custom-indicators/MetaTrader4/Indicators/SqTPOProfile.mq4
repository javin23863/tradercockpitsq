//+------------------------------------------------------------------+
//|                                              SqTPOProfile.mq4    |
//|                           Copyright © 2026, StrategyQuant s.r.o. |
//|                                     http://www.strategyquant.com |
//+------------------------------------------------------------------+
#property copyright   "Copyright © 2026, StrategyQuant s.r.o."
#property link        "http://www.strategyquant.com"
#property version     "2.00"
#property strict
#property description "TPO (Market Profile) — POC/VAH/VAL + structure signals"

#include "../Include/SqVPDisplay.mqh"
#property indicator_chart_window
#property indicator_buffers 15

//--- Input parameters — MUST match Java defaults exactly
// Session: 1=PrevHour,2=PrevDay,3=PrevWeek,4=PrevMonth,5=PrevYear
//          6=ActHour, 7=ActDay, 8=ActWeek, 9=ActMonth, 10=ActYear
input int    InpSessionType    = 2;     // Session type (Java default = 2 = Previous Day)
input int    InpProfileRows    = 50;    // Number of bins / ProfileRows (Java default = 50)
input int    InpBinSizeMode    = 1;     // 1=Range-Based, 2=Fixed Tick Size (Java default = 1)
input int    InpTicksPerBin    = 1;     // Ticks per bin for Fixed mode (Java default = 1)
input double InpValueAreaPct   = 70.0;  // Value Area % (Java default = 70)
input bool   InpUseCustomHours = false; // Custom daily session hours (Java default = false)
input int    InpStartHour      = 0;     // Custom start hour
input int    InpStartMinute    = 0;     // Custom start minute
input int    InpEndHour        = 23;    // Custom end hour
input int    InpEndMinute      = 59;    // Custom end minute

//--- Display parameters (visual TPO profile on chart)
input bool   InpShowProfile    = true;   // Show visual TPO profile on chart
input bool   InpShowVAShading  = true;   // Show Value Area shading
input bool   InpShowIBBox      = true;   // Show Initial Balance box
input bool   InpShowLevelLabels = true;  // Show level labels (POC, VAH, ...)

//--- Bracket parameters (configurable TPO bracket size per session type — matches Java)
input int    InpBracketMinDaily   = 30;   // TPO bracket size in minutes for Daily sessions
input int    InpBracketMinWeekly  = 240;  // TPO bracket size in minutes for Weekly sessions
input int    InpBracketMinMonthly = 720;  // TPO bracket size in minutes for Monthly sessions
input int    InpBracketMinYearly  = 720;  // TPO bracket size in minutes for Yearly sessions

input int    InpMaxDisplaySess = 15;     // Max sessions to display (1-50)

//--- Output buffers (15 total — plot buffers must come first per gotcha #5)
double ExtTPO_POC[];
double ExtTPO_VAH[];
double ExtTPO_VAL[];
double ExtPoorHigh[];
double ExtPoorLow[];
double ExtExcessHigh[];
double ExtExcessLow[];
double ExtSinglePrints[];
double ExtBuyingTailLen[];
double ExtSellingTailLen[];
double ExtProfileShape[];
double ExtLedgeUp[];
double ExtLedgeDown[];
double ExtIBH[];
double ExtIBL[];

//--- Internal working arrays (reused each calculation)
int    ExtTPOBins[];
long   ExtTPOMask[];

//--- Session state
datetime ExtCurSessionStart  = 0;
datetime ExtCurSessionEnd    = 0;
datetime ExtPrevSessionStart = 0;
datetime ExtPrevSessionEnd   = 0;

//--- Last computed profile values
double ExtPrevPOC = 0;
double ExtPrevVAH = 0;
double ExtPrevVAL = 0;
double ExtPrevIBH = 0;
double ExtPrevIBL = 0;

//--- Last computed signal values
double ExtLastPoorHigh     = 0;
double ExtLastPoorLow      = 0;
double ExtLastExcessHigh   = 0;
double ExtLastExcessLow    = 0;
double ExtLastSinglePrints = 0;
double ExtLastBuyingTail   = 0;
double ExtLastSellingTail  = 0;
double ExtLastShape        = 5;
double ExtLastLedgeUp      = 0;
double ExtLastLedgeDown    = 0;

//--- Validated parameters
int    ExtProfileRows;
double ExtValueAreaPct;
int    ExtBinSizeMode;
int    ExtTicksPerBin;

#define MAX_BINS 2000

//--- Display tracking
double lastTPOSesHigh = 0;
double lastTPOSesLow  = 0;
int    lastTPONumBins = 0;

//+------------------------------------------------------------------+
int OnInit()
{
   // SQUtils.fixAllowedRange equivalents
   ExtProfileRows  = (int)MathMax(10, MathMin(500, InpProfileRows));
   ExtValueAreaPct = MathMax(30.0, MathMin(95.0, InpValueAreaPct));
   ExtBinSizeMode  = (InpBinSizeMode == 2) ? 2 : 1;
   ExtTicksPerBin  = (int)MathMax(1, MathMin(1000, InpTicksPerBin));

   // Pre-allocate working arrays
   ArrayResize(ExtTPOBins, MAX_BINS);
   ArrayResize(ExtTPOMask, MAX_BINS);
   ArrayInitialize(ExtTPOBins, 0);

   // Bind output buffers (gotcha #5: plot buffers first)
   IndicatorBuffers(15);
   SetIndexBuffer(0,  ExtTPO_POC);
   SetIndexBuffer(1,  ExtTPO_VAH);
   SetIndexBuffer(2,  ExtTPO_VAL);
   SetIndexBuffer(3,  ExtPoorHigh);
   SetIndexBuffer(4,  ExtPoorLow);
   SetIndexBuffer(5,  ExtExcessHigh);
   SetIndexBuffer(6,  ExtExcessLow);
   SetIndexBuffer(7,  ExtSinglePrints);
   SetIndexBuffer(8,  ExtBuyingTailLen);
   SetIndexBuffer(9,  ExtSellingTailLen);
   SetIndexBuffer(10, ExtProfileShape);
   SetIndexBuffer(11, ExtLedgeUp);
   SetIndexBuffer(12, ExtLedgeDown);
   SetIndexBuffer(13, ExtIBH);
   SetIndexBuffer(14, ExtIBL);

   // Visual styles — price-level buffers drawn as lines, data-only buffers hidden
   SetIndexStyle(0,  DRAW_LINE,  STYLE_SOLID, 2, clrRed);
   SetIndexStyle(1,  DRAW_LINE,  STYLE_SOLID, 1, clrDodgerBlue);
   SetIndexStyle(2,  DRAW_LINE,  STYLE_SOLID, 1, clrDodgerBlue);
   SetIndexStyle(3,  DRAW_NONE);
   SetIndexStyle(4,  DRAW_NONE);
   SetIndexStyle(5,  DRAW_NONE);
   SetIndexStyle(6,  DRAW_NONE);
   SetIndexStyle(7,  DRAW_NONE);
   SetIndexStyle(8,  DRAW_NONE);
   SetIndexStyle(9,  DRAW_NONE);
   SetIndexStyle(10, DRAW_NONE);
   SetIndexStyle(11, DRAW_LINE,  STYLE_DOT,   1, clrLimeGreen);
   SetIndexStyle(12, DRAW_LINE,  STYLE_DOT,   1, clrOrangeRed);
   SetIndexStyle(13, DRAW_LINE,  STYLE_DASH,  1, clrGold);
   SetIndexStyle(14, DRAW_LINE,  STYLE_DASH,  1, clrGold);

   // Buffer labels (shown in Data Window and tooltips)
   SetIndexLabel(0,  "POC");
   SetIndexLabel(1,  "VAH");
   SetIndexLabel(2,  "VAL");
   SetIndexLabel(3,  "PoorHigh");
   SetIndexLabel(4,  "PoorLow");
   SetIndexLabel(5,  "ExcessHigh");
   SetIndexLabel(6,  "ExcessLow");
   SetIndexLabel(7,  "SinglePrints");
   SetIndexLabel(8,  "BuyingTail");
   SetIndexLabel(9,  "SellingTail");
   SetIndexLabel(10, "ProfileShape");
   SetIndexLabel(11, "LedgeUp");
   SetIndexLabel(12, "LedgeDown");
   SetIndexLabel(13, "IBH");
   SetIndexLabel(14, "IBL");

   // Reverse indexing — time[0] = newest bar (gotcha #2)
   ArraySetAsSeries(ExtTPO_POC,        true);
   ArraySetAsSeries(ExtTPO_VAH,        true);
   ArraySetAsSeries(ExtTPO_VAL,        true);
   ArraySetAsSeries(ExtPoorHigh,       true);
   ArraySetAsSeries(ExtPoorLow,        true);
   ArraySetAsSeries(ExtExcessHigh,     true);
   ArraySetAsSeries(ExtExcessLow,      true);
   ArraySetAsSeries(ExtSinglePrints,   true);
   ArraySetAsSeries(ExtBuyingTailLen,  true);
   ArraySetAsSeries(ExtSellingTailLen, true);
   ArraySetAsSeries(ExtProfileShape,   true);
   ArraySetAsSeries(ExtLedgeUp,        true);
   ArraySetAsSeries(ExtLedgeDown,      true);
   ArraySetAsSeries(ExtIBH,            true);
   ArraySetAsSeries(ExtIBL,            true);

   IndicatorShortName("TPO(" + GetSessionName(InpSessionType) + ",R" + string(ExtProfileRows) + ")");
   IndicatorDigits(_Digits);

   ResetSessionState();

   // Initialize visual display
   if(InpShowProfile)
   {
      string prefix = "TPO4_" + IntegerToString(InpSessionType) + "_";
      VPDisplayInit(prefix, MathMin(VP_MAX_HIST, MathMax(1, InpMaxDisplaySess)));
   }

   return(INIT_SUCCEEDED);
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
   ExtCurSessionStart  = 0;
   ExtCurSessionEnd    = 0;
   ExtPrevSessionStart = 0;
   ExtPrevSessionEnd   = 0;
   ExtPrevPOC          = 0;
   ExtPrevVAH          = 0;
   ExtPrevVAL          = 0;
   ExtPrevIBH          = 0;
   ExtPrevIBL          = 0;
   ExtLastPoorHigh     = 0;
   ExtLastPoorLow      = 0;
   ExtLastExcessHigh   = 0;
   ExtLastExcessLow    = 0;
   ExtLastSinglePrints = 0;
   ExtLastBuyingTail   = 0;
   ExtLastSellingTail  = 0;
   ExtLastShape        = 5;
   ExtLastLedgeUp      = 0;
   ExtLastLedgeDown    = 0;
}

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

   // Reverse all input arrays to match buffer direction (gotcha #2)
   ArraySetAsSeries(time,        true);
   ArraySetAsSeries(high,        true);
   ArraySetAsSeries(low,         true);
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

   bool actualMode = (InpSessionType >= 6);

   for(int i = startBar; i >= 0 && !IsStopped(); i--) {
      datetime barTime = time[i];

      if(!actualMode) {
         if(ExtCurSessionEnd == 0 || barTime >= ExtCurSessionEnd) {
            ExtPrevSessionStart = ExtCurSessionStart;
            ExtPrevSessionEnd   = ExtCurSessionEnd;

            CalculateSessionBoundaries(barTime);

            // Sunday skip — matches Java OnBarUpdate() sunday shift
            if(ExtPrevSessionStart > 0 && IsSundayTime(ExtPrevSessionStart)) {
               MqlDateTime sd;
               TimeToStruct(ExtPrevSessionStart, sd);
               sd.hour = 0; sd.min = 0; sd.sec = 0;
               datetime dayStart = StructToTime(sd);
               ExtPrevSessionStart = dayStart - (datetime)(2 * 86400);
               ExtPrevSessionEnd   = ExtPrevSessionStart + 86400;
               if(IsSundayTime(ExtPrevSessionStart)) {
                  ExtPrevSessionStart -= (datetime)(2 * 86400);
                  ExtPrevSessionEnd    = ExtPrevSessionStart + 86400;
               }
            }

            if(ExtPrevSessionStart > 0 && ExtPrevSessionEnd > 0) {
               CalculateTPOProfile(i, time, high, low, tick_volume, volume, rates_total);
            }
         }
      } else {
         if(ExtCurSessionEnd == 0 || barTime >= ExtCurSessionEnd) {
            CalculateSessionBoundaries(barTime);
         }

         ExtPrevSessionStart = ExtCurSessionStart;
         ExtPrevSessionEnd   = (ExtCurSessionEnd < barTime) ? ExtCurSessionEnd : barTime;

         if(ExtPrevSessionStart > 0 && ExtPrevSessionEnd > ExtPrevSessionStart) {
            CalculateTPOProfile(i, time, high, low, tick_volume, volume, rates_total);
         }
      }

      ExtTPO_POC[i]        = ExtPrevPOC;
      ExtTPO_VAH[i]        = ExtPrevVAH;
      ExtTPO_VAL[i]        = ExtPrevVAL;
      ExtPoorHigh[i]       = ExtLastPoorHigh;
      ExtPoorLow[i]        = ExtLastPoorLow;
      ExtExcessHigh[i]     = ExtLastExcessHigh;
      ExtExcessLow[i]      = ExtLastExcessLow;
      ExtSinglePrints[i]   = ExtLastSinglePrints;
      ExtBuyingTailLen[i]  = ExtLastBuyingTail;
      ExtSellingTailLen[i] = ExtLastSellingTail;
      ExtProfileShape[i]   = ExtLastShape;
      ExtLedgeUp[i]        = ExtLastLedgeUp;
      ExtLedgeDown[i]      = ExtLastLedgeDown;
      ExtIBH[i]            = (ExtPrevIBH == 0) ? EMPTY_VALUE : ExtPrevIBH;
      ExtIBL[i]            = (ExtPrevIBL == 0) ? EMPTY_VALUE : ExtPrevIBL;
   }

   return(rates_total);
}

//+------------------------------------------------------------------+
void CalculateSessionBoundaries(datetime curTime)
{
   MqlDateTime dt;
   TimeToStruct(curTime, dt);
   dt.hour = 0; dt.min = 0; dt.sec = 0;
   datetime dayStart = StructToTime(dt);

   int baseType;
   if     (InpSessionType == 1 || InpSessionType == 6)  baseType = 0;
   else if(InpSessionType == 2 || InpSessionType == 7)  baseType = 1;
   else if(InpSessionType == 3 || InpSessionType == 8)  baseType = 2;
   else if(InpSessionType == 4 || InpSessionType == 9)  baseType = 3;
   else if(InpSessionType == 5 || InpSessionType == 10) baseType = 4;
   else                                                  baseType = 1;

   switch(baseType) {
      case 0: { // Hourly
         MqlDateTime hd;
         TimeToStruct(curTime, hd);
         hd.min = 0; hd.sec = 0;
         ExtCurSessionStart = StructToTime(hd);
         ExtCurSessionEnd   = ExtCurSessionStart + 3600;
         break;
      }

      case 1: { // Daily
         if(InpUseCustomHours) {
            datetime start = dayStart + (datetime)(InpStartHour * 3600 + InpStartMinute * 60);
            datetime end   = dayStart + (datetime)(InpEndHour   * 3600 + InpEndMinute   * 60);
            if(end <= start) end += 86400;
            if(curTime < start) { start -= 86400; end -= 86400; }
            ExtCurSessionStart = start;
            ExtCurSessionEnd   = end;
         } else {
            ExtCurSessionStart = dayStart;
            ExtCurSessionEnd   = dayStart + 86400;
            if(IsSundayTime(ExtCurSessionStart)) {
               ExtCurSessionStart -= (datetime)(2 * 86400);
               ExtCurSessionEnd    = ExtCurSessionStart + 86400;
            }
         }
         break;
      }

      case 2: { // Weekly
         MqlDateTime wd;
         TimeToStruct(dayStart, wd);
         int daysFromMon = (wd.day_of_week == 0) ? 6 : wd.day_of_week - 1;
         ExtCurSessionStart = dayStart - (datetime)(daysFromMon * 86400);
         ExtCurSessionEnd   = ExtCurSessionStart + (datetime)(7 * 86400);
         break;
      }

      case 3: { // Monthly
         MqlDateTime md;
         TimeToStruct(dayStart, md);
         md.day = 1;
         ExtCurSessionStart = StructToTime(md);
         md.mon++;
         if(md.mon > 12) { md.mon = 1; md.year++; }
         ExtCurSessionEnd = StructToTime(md);
         break;
      }

      case 4: { // Yearly
         MqlDateTime yd;
         TimeToStruct(curTime, yd);
         yd.mon = 1; yd.day = 1; yd.hour = 0; yd.min = 0; yd.sec = 0;
         ExtCurSessionStart = StructToTime(yd);
         yd.year++;
         ExtCurSessionEnd = StructToTime(yd);
         break;
      }
   }
}

//+------------------------------------------------------------------+
int GetBracketSeconds()
{
   int baseType;
   if     (InpSessionType == 1 || InpSessionType == 6)  baseType = 0;
   else if(InpSessionType == 2 || InpSessionType == 7)  baseType = 1;
   else if(InpSessionType == 3 || InpSessionType == 8)  baseType = 2;
   else if(InpSessionType == 4 || InpSessionType == 9)  baseType = 3;
   else if(InpSessionType == 5 || InpSessionType == 10) baseType = 4;
   else                                                  baseType = 1;

   switch(baseType) {
      case 0: return 60;                              // 1 minute (Hourly — fixed)
      case 1: return InpBracketMinDaily   * 60;       // Daily bracket (default 30 min)
      case 2: return InpBracketMinWeekly  * 60;       // Weekly bracket (default 240 min)
      case 3: return InpBracketMinMonthly * 60;       // Monthly bracket (default 720 min)
      case 4: return InpBracketMinYearly  * 60;       // Yearly bracket (default 720 min)
      default: return InpBracketMinDaily  * 60;
   }
}

//+------------------------------------------------------------------+
int GetIBSeconds()
{
   int baseType;
   if     (InpSessionType == 1 || InpSessionType == 6)  baseType = 0;
   else if(InpSessionType == 2 || InpSessionType == 7)  baseType = 1;
   else if(InpSessionType == 3 || InpSessionType == 8)  baseType = 2;
   else if(InpSessionType == 4 || InpSessionType == 9)  baseType = 3;
   else if(InpSessionType == 5 || InpSessionType == 10) baseType = 4;
   else                                                  baseType = 1;

   switch(baseType) {
      case 0: return 900;
      case 1: return 3600;
      case 2: return 43200;
      case 3: return 86400;
      case 4: return 518400;
      default: return 3600;
   }
}

//+------------------------------------------------------------------+
int BitCount(long v)
{
   int    n  = 0;
   ulong  uv = (ulong)v;
   while(uv != 0) {
      n += (int)(uv & (ulong)1);
      uv >>= 1;
   }
   return n;
}

//+------------------------------------------------------------------+
bool IsSundayTime(datetime t)
{
   MqlDateTime sd;
   TimeToStruct(t, sd);
   return (sd.day_of_week == 0);
}

//+------------------------------------------------------------------+
void CalculateTPOProfile(int              currentBar,
                         const datetime  &time[],
                         const double    &high[],
                         const double    &low[],
                         const long      &tick_volume[],
                         const long      &volume[],
                         int              totalBars)
{
   // Pass 1: session high/low + IB tracking
   double sessionHigh = -DBL_MAX;
   double sessionLow  =  DBL_MAX;
   int    barsInSession = 0;

   datetime ibEndTime = ExtPrevSessionStart + (datetime)GetIBSeconds();
   double ibHigh = -DBL_MAX;
   double ibLow  =  DBL_MAX;

   for(int i = currentBar; i < totalBars; i++) {
      datetime barTime = time[i];
      if(barTime < ExtPrevSessionStart) break;

      if(barTime >= ExtPrevSessionStart && barTime < ExtPrevSessionEnd) {
         if(IsSundayTime(barTime)) continue;
         if(high[i] > sessionHigh) sessionHigh = high[i];
         if(low[i]  < sessionLow)  sessionLow  = low[i];
         barsInSession++;
         if(barTime < ibEndTime) {
            if(high[i] > ibHigh) ibHigh = high[i];
            if(low[i]  < ibLow)  ibLow  = low[i];
         }
      }
   }

   if(barsInSession == 0 || sessionHigh <= sessionLow) return;

   if(ibHigh > ibLow) { ExtPrevIBH = ibHigh; ExtPrevIBL = ibLow; }

   // Store session extents for display
   lastTPOSesHigh = sessionHigh;
   lastTPOSesLow  = sessionLow;

   double range = sessionHigh - sessionLow;

   int    numBins;
   double binSize;

   if(ExtBinSizeMode == 2) {
      double tickSize = MarketInfo(Symbol(), MODE_TICKSIZE);
      if(tickSize <= 0) tickSize = Point;
      binSize = ExtTicksPerBin * tickSize;
      numBins = (int)MathCeil(range / binSize);
      numBins = (int)MathMax(1, MathMin(MAX_BINS, numBins));
   } else {
      numBins = ExtProfileRows;
      binSize = range / numBins;
   }

   for(int j = 0; j < numBins; j++) {
      ExtTPOBins[j] = 0;
      ExtTPOMask[j] = 0;
   }

   // Pass 2: build TPO bitmasks — newest → oldest (matches Java exactly)
   // nextBarTime starts at prevSessionEnd; all non-Sunday bars update it.
   int    bracketSecs  = GetBracketSeconds();
   long   sessionDur   = (long)(ExtPrevSessionEnd - ExtPrevSessionStart);
   int    bracketCount = (int)MathCeil((double)sessionDur / (double)bracketSecs);
   if(bracketCount < 1)  bracketCount = 1;
   if(bracketCount > 62) bracketCount = 62;

   datetime nextBarTime = ExtPrevSessionEnd;
   for(int i = currentBar; i < totalBars; i++) {
      datetime barTime = time[i];
      if(barTime < ExtPrevSessionStart) break;

      if(barTime >= ExtPrevSessionStart && barTime < ExtPrevSessionEnd) {
         if(IsSundayTime(barTime)) {
            // Java: Sunday continue skips nextBarTime update
            continue;
         }

         datetime barEndTime = (nextBarTime < ExtPrevSessionEnd) ? nextBarTime : ExtPrevSessionEnd;

         int startBracket = (int)((barTime - ExtPrevSessionStart) / bracketSecs);
         datetime effEnd  = (barEndTime > barTime) ? (barEndTime - 1) : barTime;
         int endBracket   = (int)((effEnd - ExtPrevSessionStart) / bracketSecs);
         if(startBracket < 0)  startBracket = 0;
         if(startBracket > 61) startBracket = 61;
         if(endBracket   < 0)  endBracket   = 0;
         if(endBracket   > 61) endBracket   = 61;

         long combinedBits = 0;
         for(int bk = startBracket; bk <= endBracket; bk++) {
            combinedBits |= ((long)1 << bk);
         }

         double lo = low[i];
         double hi = high[i];
         int loBin = (int)((lo - sessionLow) / binSize);
         int hiBin = (int)((hi - sessionLow) / binSize);
         loBin = (int)MathMax(0, MathMin(numBins - 1, loBin));
         hiBin = (int)MathMax(0, MathMin(numBins - 1, hiBin));
         if(hiBin < loBin) { int tmp = hiBin; hiBin = loBin; loBin = tmp; }

         for(int b = loBin; b <= hiBin; b++) {
            ExtTPOMask[b] |= combinedBits;
         }
      }

      // Update nextBarTime for all bars (non-session and in-session non-Sunday)
      nextBarTime = barTime;
   }

   // Convert masks → TPO counts
   long totalTPO    = 0;
   int singlePrints = 0;
   int tpoHighBin   = -1;
   int tpoLowBin    = -1;

   for(int b = 0; b < numBins; b++) {
      int cnt = BitCount(ExtTPOMask[b]);
      ExtTPOBins[b] = cnt;
      totalTPO += cnt;
      if(cnt > 0) {
         if(tpoLowBin == -1) tpoLowBin = b;
         tpoHighBin = b;
         if(cnt == 1) singlePrints++;
      }
   }

   ExtLastSinglePrints = singlePrints;
   ExtLastExcessHigh = (tpoHighBin >= 0 && ExtTPOBins[tpoHighBin] == 1) ? 1.0 : 0.0;
   ExtLastExcessLow  = (tpoLowBin  >= 0 && ExtTPOBins[tpoLowBin]  == 1) ? 1.0 : 0.0;
   ExtLastPoorHigh   = (tpoHighBin >= 0 && ExtTPOBins[tpoHighBin] >= 2) ? 1.0 : 0.0;
   ExtLastPoorLow    = (tpoLowBin  >= 0 && ExtTPOBins[tpoLowBin]  >= 2) ? 1.0 : 0.0;

   int buyTail = 0;
   if(tpoLowBin >= 0) {
      for(int bt = tpoLowBin; bt <= tpoHighBin; bt++) {
         if(ExtTPOBins[bt] == 1) buyTail++;
         else                    break;
      }
   }
   ExtLastBuyingTail = buyTail;

   int sellTail = 0;
   if(tpoHighBin >= 0) {
      for(int st = tpoHighBin; st >= tpoLowBin; st--) {
         if(ExtTPOBins[st] == 1) sellTail++;
         else                    break;
      }
   }
   ExtLastSellingTail = sellTail;

   if(totalTPO <= 0) {
      ExtPrevPOC = 0; ExtPrevVAH = 0; ExtPrevVAL = 0;
      ExtLastShape = 5; ExtLastLedgeUp = 0; ExtLastLedgeDown = 0;
      return;
   }

   // POC
   int pocIdx    = 0;
   int maxTPOBin = ExtTPOBins[0];
   for(int j = 1; j < numBins; j++) {
      if(ExtTPOBins[j] > maxTPOBin) { maxTPOBin = ExtTPOBins[j]; pocIdx = j; }
   }
   ExtPrevPOC = sessionLow + (pocIdx + 0.5) * binSize;

   // Value Area expansion
   double targetTPO = totalTPO * (ExtValueAreaPct / 100.0);
   long   accTPO    = ExtTPOBins[pocIdx];
   int    upperIdx  = pocIdx;
   int    lowerIdx  = pocIdx;

   while(accTPO < targetTPO) {
      bool canUp   = (upperIdx + 1) < numBins;
      bool canDown = (lowerIdx - 1) >= 0;
      if(!canUp && !canDown) break;
      int above = canUp   ? ExtTPOBins[upperIdx + 1] : -1;
      int below = canDown ? ExtTPOBins[lowerIdx - 1] : -1;
      if(above >= below) { upperIdx++; accTPO += ExtTPOBins[upperIdx]; }
      else               { lowerIdx--; accTPO += ExtTPOBins[lowerIdx]; }
   }

   ExtPrevVAH = sessionLow + (upperIdx + 1) * binSize;
   ExtPrevVAL = sessionLow + lowerIdx * binSize;

   ExtLastShape = DetectProfileShape(tpoLowBin, tpoHighBin, pocIdx, numBins);

   // Ledge detection
   ExtLastLedgeUp = 0; ExtLastLedgeDown = 0;
   if(tpoLowBin >= 0 && tpoHighBin > tpoLowBin) {
      int activeBins       = tpoHighBin - tpoLowBin + 1;
      int midBin           = tpoLowBin + activeBins / 2;
      int bestLedgeUpLen   = 0, bestLedgeUpBin   = -1;
      int bestLedgeDownLen = 0, bestLedgeDownBin  = -1;
      int runStart         = tpoLowBin;

      for(int lb = tpoLowBin + 1; lb <= tpoHighBin; lb++) {
         if(ExtTPOBins[lb] == ExtTPOBins[runStart] && ExtTPOBins[lb] > 0) {
            int runLen = lb - runStart + 1;
            if(lb >= midBin && runLen > bestLedgeUpLen && runLen >= 3) {
               bestLedgeUpLen = runLen; bestLedgeUpBin = lb;
            }
            if(runStart <= midBin && runLen > bestLedgeDownLen && runLen >= 3) {
               bestLedgeDownLen = runLen; bestLedgeDownBin = runStart;
            }
         } else {
            runStart = lb;
         }
      }
      if(bestLedgeUpBin   >= 0) ExtLastLedgeUp   = sessionLow + (bestLedgeUpBin   + 0.5) * binSize;
      if(bestLedgeDownBin >= 0) ExtLastLedgeDown  = sessionLow + (bestLedgeDownBin + 0.5) * binSize;
   }

   // Store numBins for display
   lastTPONumBins = numBins;

   //--- Push to visual TPO display ---
   if(InpShowProfile)
   {
      VPDisplayPushTPOSession(
         ExtPrevSessionStart, ExtPrevSessionEnd,
         lastTPOSesHigh, lastTPOSesLow,
         ExtPrevPOC, ExtPrevVAH, ExtPrevVAL,
         ExtPrevIBH, ExtPrevIBL,
         numBins,
         ExtTPOBins,
         InpMaxDisplaySess);
   }
}

//+------------------------------------------------------------------+
int DetectProfileShape(int tpoLowBin, int tpoHighBin, int pocIdx, int numBins)
{
   if(tpoLowBin < 0 || tpoHighBin < 0 || tpoHighBin <= tpoLowBin) return 5;
   int activeBins = tpoHighBin - tpoLowBin + 1;
   if(activeBins < 3) return 5;

   double pocRelPos = (double)(pocIdx - tpoLowBin) / (double)(activeBins - 1);

   int thirdLow = tpoLowBin + activeBins / 4;
   int thirdHigh = tpoHighBin - activeBins / 4;
   bool inValley    = false;
   int  valleyStart = -1, valleyEnd = -1;

   for(int b = thirdLow; b <= thirdHigh; b++) {
      if(ExtTPOBins[b] <= 1) {
         if(!inValley) { valleyStart = b; inValley = true; }
         valleyEnd = b;
      } else {
         if(inValley && (valleyEnd - valleyStart + 1) >= 2) {
            int belowSum = 0, aboveSum = 0;
            for(int bb = tpoLowBin; bb < valleyStart; bb++)     belowSum += ExtTPOBins[bb];
            for(int ab = valleyEnd + 1; ab <= tpoHighBin; ab++) aboveSum += ExtTPOBins[ab];
            if(belowSum >= 3 && aboveSum >= 3) return 4;
         }
         inValley = false;
      }
   }
   if(inValley && (valleyEnd - valleyStart + 1) >= 2) {
      int belowSum = 0, aboveSum = 0;
      for(int bb = tpoLowBin; bb < valleyStart; bb++)     belowSum += ExtTPOBins[bb];
      for(int ab = valleyEnd + 1; ab <= tpoHighBin; ab++) aboveSum += ExtTPOBins[ab];
      if(belowSum >= 3 && aboveSum >= 3) return 4;
   }

   if(pocRelPos > 0.66) {
      int thirdSize = activeBins / 3;
      double bottomAvg = 0, topAvg = 0;
      for(int b = tpoLowBin; b < tpoLowBin + thirdSize; b++)        bottomAvg += ExtTPOBins[b];
      for(int b = tpoHighBin - thirdSize + 1; b <= tpoHighBin; b++) topAvg    += ExtTPOBins[b];
      bottomAvg /= MathMax(1, thirdSize);
      topAvg    /= MathMax(1, thirdSize);
      if(topAvg > bottomAvg * 1.5) return 1;
   }

   if(pocRelPos < 0.33) {
      int thirdSize = activeBins / 3;
      double topAvg = 0, bottomAvg = 0;
      for(int b = tpoHighBin - thirdSize + 1; b <= tpoHighBin; b++) topAvg    += ExtTPOBins[b];
      for(int b = tpoLowBin; b < tpoLowBin + thirdSize; b++)        bottomAvg += ExtTPOBins[b];
      topAvg    /= MathMax(1, thirdSize);
      bottomAvg /= MathMax(1, thirdSize);
      if(bottomAvg > topAvg * 1.5) return 2;
   }

   if(pocRelPos >= 0.33 && pocRelPos <= 0.66) {
      int mid = tpoLowBin + activeBins / 2;
      int bottomSum = 0, topSum = 0;
      for(int b = tpoLowBin; b < mid; b++)    bottomSum += ExtTPOBins[b];
      for(int b = mid; b <= tpoHighBin; b++)  topSum    += ExtTPOBins[b];
      double ratio = (double)MathMin(bottomSum, topSum) / (double)MathMax(1, MathMax(bottomSum, topSum));
      if(ratio > 0.6) return 3;
   }

   return 5;
}

//+------------------------------------------------------------------+
string GetSessionName(int sessionType)
{
   switch(sessionType) {
      case 1:  return "PrevHour";
      case 2:  return "PrevDay";
      case 3:  return "PrevWeek";
      case 4:  return "PrevMonth";
      case 5:  return "PrevYear";
      case 6:  return "ActHour";
      case 7:  return "ActDay";
      case 8:  return "ActWeek";
      case 9:  return "ActMonth";
      case 10: return "ActYear";
      default: return "Unknown";
   }
}
//+------------------------------------------------------------------+
