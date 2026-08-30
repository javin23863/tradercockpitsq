//+------------------------------------------------------------------+
//|                                SqVolumeProfileCustomHours.mq5    |
//|                           Copyright © 2026, StrategyQuant s.r.o. |
//|                                     http://www.strategyquant.com |
//+------------------------------------------------------------------+
#property copyright   "Copyright © 2026, StrategyQuant s.r.o."
#property link        "http://www.strategyquant.com"
#property version     "2.00"
#property description "Volume Profile Custom Hours — mirrors Java VolumeProfileCustomHours"

#include "../Include/SqVPDisplay.mqh"
#property indicator_chart_window
#property indicator_buffers 23
#property indicator_plots   23

#property indicator_label1  "POC"
#property indicator_type1   DRAW_LINE
#property indicator_color1  clrYellow
#property indicator_width1  2

#property indicator_label2  "VAH"
#property indicator_type2   DRAW_LINE
#property indicator_color2  clrGreen
#property indicator_width2  1

#property indicator_label3  "VAL"
#property indicator_type3   DRAW_LINE
#property indicator_color3  clrGreen
#property indicator_width3  1

#property indicator_label4  "IBH"
#property indicator_type4   DRAW_LINE
#property indicator_color4  clrDodgerBlue
#property indicator_style4  STYLE_DASH
#property indicator_width4  1

#property indicator_label5  "IBL"
#property indicator_type5   DRAW_LINE
#property indicator_color5  clrDodgerBlue
#property indicator_style5  STYLE_DASH
#property indicator_width5  1

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

#property indicator_label16 "POC"
#property indicator_type16  DRAW_LINE
#property indicator_color16 clrAqua
#property indicator_width16 1

#property indicator_label17 "VAH"
#property indicator_type17  DRAW_LINE
#property indicator_color17 clrGreen
#property indicator_width17 1

#property indicator_label18 "VAL"
#property indicator_type18  DRAW_LINE
#property indicator_color18 clrGreen
#property indicator_width18 1

#property indicator_label19 "BullPOC"
#property indicator_type19  DRAW_NONE

#property indicator_label20 "BearPOC"
#property indicator_type20  DRAW_NONE

#property indicator_label21 "TotalVolume"
#property indicator_type21  DRAW_NONE
#property indicator_color21 clrGray

#property indicator_label22 "TotalBullVolume"
#property indicator_type22  DRAW_NONE
#property indicator_color22 clrGreen

#property indicator_label23 "TotalBearVolume"
#property indicator_type23  DRAW_NONE
#property indicator_color23 clrRed

//--- Input parameters
input int    InpSessionStartHours    = 8;      // Session start hour (0-23)
input int    InpSessionStartMinutes  = 0;      // Session start minutes (0-59)
input int    InpSessionEndHours      = 16;     // Session end hour (0-23)
input int    InpSessionEndMinutes    = 0;      // Session end minutes (0-59)
input int    InpSessionMode          = 1;      // 1=Previous, 2=Actual
input int    InpProfileRows          = 150;    // Price bins (Range-Based mode)
input int    InpBinSizeMode          = 2;      // 1=Range-Based, 2=Fixed Tick Size
input int    InpTicksPerBin          = 3;      // Ticks per bin (Fixed mode)
input double InpValueAreaPct         = 70.0;   // Value Area %
input int    InpHvnCount             = 5;      // HVN nodes (1-10)
input int    InpHvnThresholdPct      = 20;     // Min % for HVN
input int    InpLvnThresholdPct      = 40;     // Max % for LVN
input bool   InpEnableLVN            = false;  // Enable LVN detection
input bool   InpEnableVCP            = false;  // Enable Volume Cluster Profile
input double InpClusterSpread        = 6.0;    // VCP Gaussian sigma
input int    InpMaxClusterCenters    = 2;      // VCP max cluster peaks
input int    InpIBMinutes            = 0;      // IB override in minutes (0 = 60min default)

//--- Display parameters (visual profile on chart)
input bool   InpShowProfile          = true;   // Show visual profile on chart
input bool   InpShowVAShading        = true;   // Show Value Area shading
input bool   InpShowIBBox            = true;   // Show Initial Balance box
input bool   InpShowLevelLabels      = true;   // Show level labels (POC, VAH, ...)
input bool   InpShowStats            = false;  // Show session statistics panel
input int    InpMaxDisplaySess       = 15;     // Max sessions to display (1-50)

//--- Output buffers
double POCBuffer[], VAHBuffer[], VALBuffer[], IBHBuffer[], IBLBuffer[];
double HVN1Buffer[], HVN2Buffer[], HVN3Buffer[], HVN4Buffer[], HVN5Buffer[];
double LVN1Buffer[], LVN2Buffer[], LVN3Buffer[], LVN4Buffer[], LVN5Buffer[];
double VPOCBuffer[], VVAHBuffer[], VVALBuffer[];
double BullPOCBuffer[], BearPOCBuffer[];
double TotalVolumeBuffer[], TotalBullVolumeBuffer[], TotalBearVolumeBuffer[];

#define MAX_BINS 2000
double volumeBins[], bullVolumeBins[], bearVolumeBins[], clusterBins[];

datetime currentSessionStart = 0, currentSessionEnd = 0;
datetime prevSessionStart = 0, prevSessionEnd = 0;

double prevPOC = 0, prevVAH = 0, prevVAL = 0;
double prevIBH = 0, prevIBL = 0;
double prevHVN[5], prevLVN[5];
double prevVPOC = 0, prevVVAH = 0, prevVVAL = 0;
double prevBullPOC = 0, prevBearPOC = 0;
double prevTotalVolume = 0, prevTotalBullVolume = 0, prevTotalBearVolume = 0;

int ExtProfileRows;
double ExtValueAreaPct;
int lastNumBins = 0;

//--- Display tracking
double lastSessionHigh = 0;
double lastSessionLow  = 0;

//+------------------------------------------------------------------+
void OnInit()
{
   ExtProfileRows  = (int)MathMax(10, MathMin(500, InpProfileRows));
   ExtValueAreaPct = MathMax(30.0, MathMin(95.0, InpValueAreaPct));

   ArrayResize(volumeBins, MAX_BINS);
   ArrayResize(bullVolumeBins, MAX_BINS);
   ArrayResize(bearVolumeBins, MAX_BINS);
   ArrayResize(clusterBins, MAX_BINS);
   ArrayInitialize(volumeBins, 0.0);
   ArrayInitialize(bullVolumeBins, 0.0);
   ArrayInitialize(bearVolumeBins, 0.0);
   ArrayInitialize(clusterBins, 0.0);

   SetIndexBuffer(0,  POCBuffer,  INDICATOR_DATA);
   SetIndexBuffer(1,  VAHBuffer,  INDICATOR_DATA);
   SetIndexBuffer(2,  VALBuffer,  INDICATOR_DATA);
   SetIndexBuffer(3,  IBHBuffer,  INDICATOR_DATA);
   SetIndexBuffer(4,  IBLBuffer,  INDICATOR_DATA);
   SetIndexBuffer(5,  HVN1Buffer, INDICATOR_DATA);
   SetIndexBuffer(6,  HVN2Buffer, INDICATOR_DATA);
   SetIndexBuffer(7,  HVN3Buffer, INDICATOR_DATA);
   SetIndexBuffer(8,  HVN4Buffer, INDICATOR_DATA);
   SetIndexBuffer(9,  HVN5Buffer, INDICATOR_DATA);
   SetIndexBuffer(10, LVN1Buffer, INDICATOR_DATA);
   SetIndexBuffer(11, LVN2Buffer, INDICATOR_DATA);
   SetIndexBuffer(12, LVN3Buffer, INDICATOR_DATA);
   SetIndexBuffer(13, LVN4Buffer, INDICATOR_DATA);
   SetIndexBuffer(14, LVN5Buffer, INDICATOR_DATA);
   SetIndexBuffer(15, VPOCBuffer, INDICATOR_DATA);
   SetIndexBuffer(16, VVAHBuffer, INDICATOR_DATA);
   SetIndexBuffer(17, VVALBuffer, INDICATOR_DATA);
   SetIndexBuffer(18, BullPOCBuffer, INDICATOR_DATA);
   SetIndexBuffer(19, BearPOCBuffer, INDICATOR_DATA);
   SetIndexBuffer(20, TotalVolumeBuffer, INDICATOR_DATA);
   SetIndexBuffer(21, TotalBullVolumeBuffer, INDICATOR_DATA);
   SetIndexBuffer(22, TotalBearVolumeBuffer, INDICATOR_DATA);

   ArraySetAsSeries(POCBuffer, true);  ArraySetAsSeries(VAHBuffer, true);
   ArraySetAsSeries(VALBuffer, true);  ArraySetAsSeries(IBHBuffer, true);
   ArraySetAsSeries(IBLBuffer, true);
   ArraySetAsSeries(HVN1Buffer, true); ArraySetAsSeries(HVN2Buffer, true);
   ArraySetAsSeries(HVN3Buffer, true); ArraySetAsSeries(HVN4Buffer, true);
   ArraySetAsSeries(HVN5Buffer, true);
   ArraySetAsSeries(LVN1Buffer, true); ArraySetAsSeries(LVN2Buffer, true);
   ArraySetAsSeries(LVN3Buffer, true); ArraySetAsSeries(LVN4Buffer, true);
   ArraySetAsSeries(LVN5Buffer, true);
   ArraySetAsSeries(VPOCBuffer, true); ArraySetAsSeries(VVAHBuffer, true);
   ArraySetAsSeries(VVALBuffer, true);
   ArraySetAsSeries(BullPOCBuffer, true); ArraySetAsSeries(BearPOCBuffer, true);
   ArraySetAsSeries(TotalVolumeBuffer, true);
   ArraySetAsSeries(TotalBullVolumeBuffer, true);
   ArraySetAsSeries(TotalBearVolumeBuffer, true);

   IndicatorSetInteger(INDICATOR_DIGITS, _Digits);

   string binDesc = (InpBinSizeMode == 2) ? ("F" + IntegerToString(InpTicksPerBin)) : IntegerToString(ExtProfileRows);
   IndicatorSetString(INDICATOR_SHORTNAME,
      "VPCH(" + IntegerToString(InpSessionStartHours) + ":" + IntegerToString(InpSessionStartMinutes) +
      "-" + IntegerToString(InpSessionEndHours) + ":" + IntegerToString(InpSessionEndMinutes) +
      "," + binDesc + "," + DoubleToString(ExtValueAreaPct, 0) + ")");

   ResetSessionState();

   // Initialize visual display
   if(InpShowProfile)
   {
      string prefix = "VPCH" + IntegerToString(ChartID()) + "_" + IntegerToString(InpSessionStartHours*100+InpSessionEndHours) + "_";
      VPDisplayInit(prefix, MathMin(VP_MAX_HIST, MathMax(1, InpMaxDisplaySess)));
      g_vpShowStats = InpShowStats;
   }
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
   currentSessionStart = 0; currentSessionEnd = 0;
   prevSessionStart = 0; prevSessionEnd = 0;
   prevPOC = 0; prevVAH = 0; prevVAL = 0;
   prevIBH = 0; prevIBL = 0;
   ArrayInitialize(prevHVN, 0.0); ArrayInitialize(prevLVN, 0.0);
   prevVPOC = 0; prevVVAH = 0; prevVVAL = 0;
   prevBullPOC = 0; prevBearPOC = 0;
   prevTotalVolume = 0; prevTotalBullVolume = 0; prevTotalBearVolume = 0;
}

//+------------------------------------------------------------------+
bool IsSunday(datetime t)
{
   MqlDateTime dt; TimeToStruct(t, dt);
   return (dt.day_of_week == 0);
}

//+------------------------------------------------------------------+
int GetIBPeriodSeconds()
{
   if(InpIBMinutes > 0) return InpIBMinutes * 60;
   return 3600; // 60 minutes default
}

//+------------------------------------------------------------------+
void CalculateSessionBoundaries(datetime curTime)
{
   MqlDateTime dt; TimeToStruct(curTime, dt);
   dt.hour = 0; dt.min = 0; dt.sec = 0;
   datetime dayStart = StructToTime(dt);

   int startMins = InpSessionStartHours * 60 + InpSessionStartMinutes;
   int endMins   = InpSessionEndHours * 60 + InpSessionEndMinutes;

   MqlDateTime stDt; TimeToStruct(dayStart, stDt);
   stDt.hour = InpSessionStartHours; stDt.min = InpSessionStartMinutes;
   datetime sessionStartToday = StructToTime(stDt);

   datetime sessionEndToday;
   if(endMins > startMins)
   {
      MqlDateTime edDt; TimeToStruct(dayStart, edDt);
      edDt.hour = InpSessionEndHours; edDt.min = InpSessionEndMinutes;
      sessionEndToday = StructToTime(edDt);
   }
   else
   {
      MqlDateTime edDt; TimeToStruct(dayStart + 86400, edDt);
      edDt.hour = InpSessionEndHours; edDt.min = InpSessionEndMinutes;
      sessionEndToday = StructToTime(edDt);
   }

   if(curTime < sessionStartToday)
   {
      sessionStartToday -= 86400;
      sessionEndToday   -= 86400;
   }
   if(curTime >= sessionEndToday)
   {
      sessionStartToday += 86400;
      sessionEndToday   += 86400;
   }

   currentSessionStart = sessionStartToday;
   currentSessionEnd   = sessionEndToday;

   if(IsSunday(currentSessionStart))
   {
      currentSessionStart -= 2 * 86400;
      currentSessionEnd   -= 2 * 86400;
   }
}

//+------------------------------------------------------------------+
int OnCalculate(const int rates_total, const int prev_calculated,
                const datetime &time[], const double &open[],
                const double &high[], const double &low[],
                const double &close[], const long &tick_volume[],
                const long &volume[], const int &spread[])
{
   if(rates_total < 2) return(0);

   ArraySetAsSeries(time, true);  ArraySetAsSeries(open, true);
   ArraySetAsSeries(high, true);  ArraySetAsSeries(low, true);
   ArraySetAsSeries(close, true); ArraySetAsSeries(tick_volume, true);
   ArraySetAsSeries(volume, true);

   int startBar;
   if(prev_calculated == 0) { ResetSessionState(); startBar = rates_total - 1; }
   else { startBar = rates_total - prev_calculated; if(startBar < 0) startBar = 0; }

   bool actualMode = (InpSessionMode == 2);

   for(int i = startBar; i >= 0 && !IsStopped(); i--)
   {
      datetime barTime = time[i];

      if(!actualMode)
      {
         if(currentSessionEnd == 0 || barTime >= currentSessionEnd)
         {
            prevSessionStart = currentSessionStart;
            prevSessionEnd = currentSessionEnd;
            CalculateSessionBoundaries(barTime);

            if(prevSessionStart > 0 && IsSunday(prevSessionStart))
            {
               MqlDateTime sd; TimeToStruct(prevSessionStart, sd);
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
         if(currentSessionEnd == 0 || barTime >= currentSessionEnd)
            CalculateSessionBoundaries(barTime);
         prevSessionStart = currentSessionStart;
         prevSessionEnd = (datetime)MathMin((double)currentSessionEnd, (double)barTime);
         if(prevSessionStart > 0 && prevSessionEnd > prevSessionStart)
            CalculateVolumeProfile(i, time, open, high, low, close, volume, tick_volume, rates_total);
      }

      POCBuffer[i] = prevPOC; VAHBuffer[i] = prevVAH; VALBuffer[i] = prevVAL;
      IBHBuffer[i] = (prevIBH == 0) ? EMPTY_VALUE : prevIBH; IBLBuffer[i] = (prevIBL == 0) ? EMPTY_VALUE : prevIBL;
      HVN1Buffer[i] = (prevHVN[0]==0)?EMPTY_VALUE:prevHVN[0]; HVN2Buffer[i] = (prevHVN[1]==0)?EMPTY_VALUE:prevHVN[1]; HVN3Buffer[i] = (prevHVN[2]==0)?EMPTY_VALUE:prevHVN[2];
      HVN4Buffer[i] = (prevHVN[3]==0)?EMPTY_VALUE:prevHVN[3]; HVN5Buffer[i] = (prevHVN[4]==0)?EMPTY_VALUE:prevHVN[4];
      LVN1Buffer[i] = (prevLVN[0]==0)?EMPTY_VALUE:prevLVN[0]; LVN2Buffer[i] = (prevLVN[1]==0)?EMPTY_VALUE:prevLVN[1]; LVN3Buffer[i] = (prevLVN[2]==0)?EMPTY_VALUE:prevLVN[2];
      LVN4Buffer[i] = (prevLVN[3]==0)?EMPTY_VALUE:prevLVN[3]; LVN5Buffer[i] = (prevLVN[4]==0)?EMPTY_VALUE:prevLVN[4];
      VPOCBuffer[i] = prevVPOC; VVAHBuffer[i] = prevVVAH; VVALBuffer[i] = prevVVAL;
      BullPOCBuffer[i] = prevBullPOC; BearPOCBuffer[i] = prevBearPOC;
      TotalVolumeBuffer[i] = prevTotalVolume;
      TotalBullVolumeBuffer[i] = prevTotalBullVolume;
      TotalBearVolumeBuffer[i] = prevTotalBearVolume;
   }
   return(rates_total);
}

//+------------------------------------------------------------------+
void CalculateVolumeProfile(int currentBar, const datetime &time[],
                            const double &open[], const double &high[],
                            const double &low[], const double &close[],
                            const long &volume[], const long &tick_volume[],
                            int totalBars)
{
   double sessionHigh = -DBL_MAX, sessionLow = DBL_MAX;
   int barsInSession = 0;
   datetime ibEndTime = prevSessionStart + GetIBPeriodSeconds();
   double ibHigh = -DBL_MAX, ibLow = DBL_MAX;

   for(int i = currentBar; i < totalBars; i++)
   {
      datetime barTime = time[i];
      if(barTime < prevSessionStart) break;
      if(barTime >= prevSessionStart && barTime < prevSessionEnd)
      {
         if(IsSunday(barTime)) continue;
         double hi = high[i], lo = low[i];
         if(hi > sessionHigh) sessionHigh = hi;
         if(lo < sessionLow)  sessionLow  = lo;
         barsInSession++;
         if(barTime < ibEndTime) { if(hi > ibHigh) ibHigh = hi; if(lo < ibLow) ibLow = lo; }
      }
   }

   if(barsInSession == 0 || sessionHigh <= sessionLow) return;
   if(ibHigh > ibLow) { prevIBH = ibHigh; prevIBL = ibLow; }

   // Store session extents for display
   lastSessionHigh = sessionHigh;
   lastSessionLow  = sessionLow;

   double range = sessionHigh - sessionLow;
   int numBins; double binSize;
   if(InpBinSizeMode == 2)
   {
      binSize = InpTicksPerBin * _Point;
      numBins = (int)MathCeil(range / binSize);
      numBins = (int)MathMax(1, MathMin(numBins, MAX_BINS));
   }
   else { numBins = ExtProfileRows; binSize = range / numBins; }
   lastNumBins = numBins;

   for(int j = 0; j < numBins; j++) { volumeBins[j] = 0; bullVolumeBins[j] = 0; bearVolumeBins[j] = 0; }

   double totalVolume = 0, totalBullVolume = 0, totalBearVolume = 0;
   for(int i = currentBar; i < totalBars; i++)
   {
      datetime barTime = time[i];
      if(barTime < prevSessionStart) break;
      if(barTime >= prevSessionStart && barTime < prevSessionEnd)
      {
         if(IsSunday(barTime)) continue;
         double cl = close[i], op = open[i], hi = high[i], lo = low[i];
         long v = volume[i] > 0 ? volume[i] : tick_volume[i];
         double barVol = (double)v;
         int vBinHigh = (int)MathMax(0, MathMin(numBins-1, (int)((hi - sessionLow) / binSize)));
         int vBinLow  = (int)MathMax(0, MathMin(numBins-1, (int)((lo - sessionLow) / binSize)));
         double perBin = barVol / (double)(vBinHigh - vBinLow + 1);
         bool isBull = (cl >= op);
         for(int b = vBinLow; b <= vBinHigh; b++)
         {
            volumeBins[b] += perBin;
            if(isBull) bullVolumeBins[b] += perBin; else bearVolumeBins[b] += perBin;
         }
         totalVolume += barVol;
         if(isBull) totalBullVolume += barVol; else totalBearVolume += barVol;
      }
   }

   if(totalVolume == 0) return;

   int pocIndex = 0; double maxVolume = volumeBins[0];
   for(int j = 1; j < numBins; j++)
      if(volumeBins[j] > maxVolume) { maxVolume = volumeBins[j]; pocIndex = j; }
   prevPOC = sessionLow + (pocIndex + 0.5) * binSize;

   CalculateValueArea(pocIndex, totalVolume, binSize, sessionLow, numBins);
   FindHVNs(pocIndex, binSize, sessionLow, numBins);
   if(InpEnableLVN) FindLVNs(pocIndex, binSize, sessionLow, numBins);
   else ArrayInitialize(prevLVN, 0.0);

   if(InpEnableVCP) ApplyClusterEnhancement(numBins, binSize, sessionLow, totalVolume);
   else { prevVPOC = prevPOC; prevVVAH = prevVAH; prevVVAL = prevVAL; }

   int bullPocIdx = 0; double bullMax = bullVolumeBins[0];
   int bearPocIdx = 0; double bearMax = bearVolumeBins[0];
   for(int j = 1; j < numBins; j++)
   {
      if(bullVolumeBins[j] > bullMax) { bullMax = bullVolumeBins[j]; bullPocIdx = j; }
      if(bearVolumeBins[j] > bearMax) { bearMax = bearVolumeBins[j]; bearPocIdx = j; }
   }
   prevBullPOC = (bullMax > 0) ? sessionLow + (bullPocIdx + 0.5) * binSize : 0;
   prevBearPOC = (bearMax > 0) ? sessionLow + (bearPocIdx + 0.5) * binSize : 0;
   prevTotalVolume = totalVolume;
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
void CalculateValueArea(int pocIndex, double totalVolume, double binSize, double sessionLow, int numBins)
{
   double targetVolume = totalVolume * (ExtValueAreaPct / 100.0);
   double accumulatedVolume = volumeBins[pocIndex];
   int upperIndex = pocIndex, lowerIndex = pocIndex;
   while(accumulatedVolume < targetVolume)
   {
      bool canGoUp = (upperIndex + 1) < numBins;
      bool canGoDown = (lowerIndex - 1) >= 0;
      if(!canGoUp && !canGoDown) break;
      double volumeAbove = canGoUp ? volumeBins[upperIndex + 1] : -1;
      double volumeBelow = canGoDown ? volumeBins[lowerIndex - 1] : -1;
      if(volumeAbove >= volumeBelow) { upperIndex++; accumulatedVolume += volumeBins[upperIndex]; }
      else { lowerIndex--; accumulatedVolume += volumeBins[lowerIndex]; }
   }
   prevVAL = sessionLow + lowerIndex * binSize;
   prevVAH = sessionLow + (upperIndex + 1) * binSize;
}

//+------------------------------------------------------------------+
void FindHVNs(int pocIndex, double binSize, double sessionLow, int numBins)
{
   ArrayInitialize(prevHVN, 0.0);
   double maxVol = 0;
   for(int j = 0; j < numBins; j++) if(volumeBins[j] > maxVol) maxVol = volumeBins[j];
   if(maxVol <= 0) return;
   double threshold = maxVol * InpHvnThresholdPct / 100.0;
   int maxNodes = (int)MathMin(InpHvnCount, 5);
   int candidateIdx[]; double candidateVol[];
   ArrayResize(candidateIdx, numBins); ArrayResize(candidateVol, numBins);
   int candidateCount = 0;
   for(int j = 0; j < numBins; j++)
   {
      if(j == pocIndex) continue;
      if(volumeBins[j] < threshold) continue;
      double left = (j > 0) ? volumeBins[j-1] : -1;
      double right = (j < numBins-1) ? volumeBins[j+1] : -1;
      if(volumeBins[j] > left && volumeBins[j] > right)
      { candidateIdx[candidateCount] = j; candidateVol[candidateCount] = volumeBins[j]; candidateCount++; }
   }
   for(int a = 0; a < candidateCount - 1; a++)
   {
      int best = a;
      for(int b = a+1; b < candidateCount; b++) if(candidateVol[b] > candidateVol[best]) best = b;
      if(best != a)
      { double tmpV = candidateVol[a]; candidateVol[a] = candidateVol[best]; candidateVol[best] = tmpV;
        int tmpI = candidateIdx[a]; candidateIdx[a] = candidateIdx[best]; candidateIdx[best] = tmpI; }
   }
   int accepted[]; ArrayResize(accepted, maxNodes); int acceptedCount = 0;
   for(int c = 0; c < candidateCount && acceptedCount < maxNodes; c++)
   {
      int idx = candidateIdx[c]; double vol = candidateVol[c]; bool ok = true;
      for(int k = 0; k < acceptedCount; k++)
      {
         int prevIdx = accepted[k];
         int rangeStart = (int)MathMin(idx, prevIdx) + 1;
         int rangeEnd = (int)MathMax(idx, prevIdx);
         double minBetween = DBL_MAX;
         for(int m = rangeStart; m < rangeEnd; m++) if(volumeBins[m] < minBetween) minBetween = volumeBins[m];
         double smallerPeak = MathMin(vol, volumeBins[prevIdx]);
         if(minBetween >= smallerPeak * 0.5) { ok = false; break; }
      }
      if(ok) { accepted[acceptedCount] = idx; prevHVN[acceptedCount] = sessionLow + (idx + 0.5) * binSize; acceptedCount++; }
   }
}

//+------------------------------------------------------------------+
void FindLVNs(int pocIndex, double binSize, double sessionLow, int numBins)
{
   ArrayInitialize(prevLVN, 0.0);
   double maxVol = 0;
   for(int j = 0; j < numBins; j++) if(volumeBins[j] > maxVol) maxVol = volumeBins[j];
   if(maxVol <= 0) return;
   double threshold = maxVol * InpLvnThresholdPct / 100.0;
   int maxNodes = (int)MathMin(InpHvnCount, 5);
   int candidateIdx[]; double candidateVol[];
   ArrayResize(candidateIdx, numBins); ArrayResize(candidateVol, numBins);
   int candidateCount = 0;
   for(int j = 1; j < numBins - 1; j++)
   {
      if(volumeBins[j] > threshold) continue;
      if(volumeBins[j] < volumeBins[j-1] && volumeBins[j] < volumeBins[j+1])
      { candidateIdx[candidateCount] = j; candidateVol[candidateCount] = volumeBins[j]; candidateCount++; }
   }
   for(int a = 0; a < candidateCount - 1; a++)
   {
      int best = a;
      for(int b = a+1; b < candidateCount; b++) if(candidateVol[b] < candidateVol[best]) best = b;
      if(best != a)
      { double tmpV = candidateVol[a]; candidateVol[a] = candidateVol[best]; candidateVol[best] = tmpV;
        int tmpI = candidateIdx[a]; candidateIdx[a] = candidateIdx[best]; candidateIdx[best] = tmpI; }
   }
   int count = (int)MathMin(maxNodes, candidateCount);
   for(int n = 0; n < count; n++) prevLVN[n] = sessionLow + (candidateIdx[n] + 0.5) * binSize;
}

//+------------------------------------------------------------------+
void ApplyClusterEnhancement(int numBins, double binSize, double sessionLow, double totalVolume)
{
   double avg = totalVolume / MathMax(1, numBins);
   int peakIndices[]; double peakValues[];
   ArrayResize(peakIndices, numBins); ArrayResize(peakValues, numBins);
   int peakCount = 0;
   for(int j = 0; j < numBins; j++)
   {
      bool leftOk = (j == 0) || (volumeBins[j] >= volumeBins[j-1]);
      bool rightOk = (j == numBins-1) || (volumeBins[j] >= volumeBins[j+1]);
      if(leftOk && rightOk && volumeBins[j] > avg)
      { peakIndices[peakCount] = j; peakValues[peakCount] = volumeBins[j]; peakCount++; }
   }
   int maxCenters = (int)MathMin(InpMaxClusterCenters, peakCount);
   if(maxCenters == 0) { prevVPOC = prevPOC; prevVVAH = prevVAH; prevVVAL = prevVAL; return; }
   for(int a = 0; a < maxCenters; a++)
   {
      int bestIdx = a;
      for(int b = a+1; b < peakCount; b++) if(peakValues[b] > peakValues[bestIdx]) bestIdx = b;
      if(bestIdx != a)
      { int tmpI = peakIndices[a]; peakIndices[a] = peakIndices[bestIdx]; peakIndices[bestIdx] = tmpI;
        double tmpV = peakValues[a]; peakValues[a] = peakValues[bestIdx]; peakValues[bestIdx] = tmpV; }
   }
   double sigma = InpClusterSpread;
   for(int j = 0; j < numBins; j++) clusterBins[j] = 0;
   for(int c = 0; c < maxCenters; c++)
   {
      int center = peakIndices[c]; double centerVol = peakValues[c];
      for(int j = 0; j < numBins; j++)
      { double dist = (j - center) / sigma; clusterBins[j] += centerVol * MathExp(-0.5 * dist * dist); }
   }
   int vpocIdx = 0; double vpocMax = clusterBins[0];
   for(int j = 1; j < numBins; j++) if(clusterBins[j] > vpocMax) { vpocMax = clusterBins[j]; vpocIdx = j; }
   prevVPOC = sessionLow + (vpocIdx + 0.5) * binSize;

   double clusterTotal = 0;
   for(int j = 0; j < numBins; j++) clusterTotal += clusterBins[j];
   double targetVolume = clusterTotal * (ExtValueAreaPct / 100.0);
   double accumulated = clusterBins[vpocIdx];
   int lo = vpocIdx, hi = vpocIdx;
   while(accumulated < targetVolume && (lo > 0 || hi < numBins - 1))
   {
      double expandLo = (lo > 0) ? clusterBins[lo-1] : 0;
      double expandHi = (hi < numBins-1) ? clusterBins[hi+1] : 0;
      if(expandLo >= expandHi && lo > 0) { lo--; accumulated += clusterBins[lo]; }
      else if(hi < numBins-1) { hi++; accumulated += clusterBins[hi]; }
      else { lo--; accumulated += clusterBins[lo]; }
   }
   prevVVAL = sessionLow + lo * binSize;
   prevVVAH = sessionLow + (hi + 1) * binSize;
}
//+------------------------------------------------------------------+
