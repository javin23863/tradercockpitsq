//+------------------------------------------------------------------+
//|                                SqVolumeProfileCustomHours.mq4    |
//|                           Copyright © 2026, StrategyQuant s.r.o. |
//|                                     http://www.strategyquant.com |
//+------------------------------------------------------------------+
#property copyright "Copyright © 2026, StrategyQuant s.r.o."
#property link      "http://www.strategyquant.com"
#property version   "2.00"
#property strict
#property description "Volume Profile Custom Hours — mirrors Java VolumeProfileCustomHours"

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

//--- Input parameters
input int    SessionStartHours  = 8;      // Session start hour (0-23)
input int    SessionStartMinutes= 0;      // Session start minutes (0-59)
input int    SessionEndHours    = 16;     // Session end hour (0-23)
input int    SessionEndMinutes  = 0;      // Session end minutes (0-59)
input int    SessionMode        = 1;      // 1=Previous, 2=Actual
input int    ProfileRows        = 150;    // Price bins for Range-Based mode (10-500)
input int    BinSizeMode        = 2;      // 1=Range-Based, 2=Fixed Tick Size
input int    TicksPerBin        = 3;      // Ticks per bin (Fixed mode only, 1-1000)
input double ValueAreaPct       = 70.0;   // Value Area percentage (30-95)
input int    HvnCount           = 5;      // HVN nodes to detect (1-10)
input int    HvnThresholdPct    = 20;     // Min % of max vol for HVN (10-90)
input int    LvnThresholdPct    = 40;     // Max % of max vol for LVN (10-90)
input bool   EnableLVN          = false;  // Enable Low Volume Node detection
input bool   EnableVCP          = false;  // Enable Volume Cluster Profile
input double ClusterSpread      = 6.0;    // VCP Gaussian sigma (0.5-20.0)
input int    MaxClusterCenters  = 2;      // VCP max cluster peaks (1-10)
input int    IBMinutes          = 0;      // IB override in minutes (0 = 60min default)

//--- Display parameters (visual profile on chart)
input bool   InpShowProfile     = true;   // Show visual profile on chart
input bool   InpShowVAShading   = true;   // Show Value Area shading
input bool   InpShowIBBox       = true;   // Show Initial Balance box
input bool   InpShowLevelLabels = true;   // Show level labels (POC, VAH, ...)
input bool   InpShowStats       = false;  // Show session statistics panel
input int    InpMaxDisplaySess  = 15;     // Max sessions to display (1-50)

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

#define MAX_BINS 2000
double volumeBins[MAX_BINS];
double bullVolumeBins[MAX_BINS];
double bearVolumeBins[MAX_BINS];
double clusterBins[MAX_BINS];

datetime currentSessionStart = 0;
datetime currentSessionEnd   = 0;
datetime prevSessionStart    = 0;
datetime prevSessionEnd      = 0;

double prevPOC = 0, prevVAH = 0, prevVAL = 0;
double prevIBH = 0, prevIBL = 0;
double prevHVN[5];
double prevLVN[5];
double prevVPOC = 0, prevVVAH = 0, prevVVAL = 0;
double prevBullPOC = 0, prevBearPOC = 0;
double prevTotalVolume = 0, prevTotalBullVolume = 0, prevTotalBearVolume = 0;

//--- Display tracking
double lastSessionHigh = 0;
double lastSessionLow  = 0;
int    lastNumBins     = 0;

//+------------------------------------------------------------------+
int OnInit()
{
   IndicatorBuffers(23);
   SetIndexBuffer(0,  POCBuffer);
   SetIndexBuffer(1,  VAHBuffer);
   SetIndexBuffer(2,  VALBuffer);
   SetIndexBuffer(3,  IBHBuffer);
   SetIndexBuffer(4,  IBLBuffer);
   SetIndexBuffer(5,  HVN1Buffer);
   SetIndexBuffer(6,  HVN2Buffer);
   SetIndexBuffer(7,  HVN3Buffer);
   SetIndexBuffer(8,  HVN4Buffer);
   SetIndexBuffer(9,  HVN5Buffer);
   SetIndexBuffer(10, LVN1Buffer);
   SetIndexBuffer(11, LVN2Buffer);
   SetIndexBuffer(12, LVN3Buffer);
   SetIndexBuffer(13, LVN4Buffer);
   SetIndexBuffer(14, LVN5Buffer);
   SetIndexBuffer(15, VPOCBuffer);
   SetIndexBuffer(16, VVAHBuffer);
   SetIndexBuffer(17, VVALBuffer);
   SetIndexBuffer(18, BullPOCBuffer);
   SetIndexBuffer(19, BearPOCBuffer);
   SetIndexBuffer(20, TotalVolumeBuffer);
   SetIndexBuffer(21, TotalBullVolumeBuffer);
   SetIndexBuffer(22, TotalBearVolumeBuffer);

   for(int b = 0; b < 23; b++)
   {
      // All buffers use series indexing
      switch(b)
      {
         case 0: ArraySetAsSeries(POCBuffer, true); break;
         case 1: ArraySetAsSeries(VAHBuffer, true); break;
         case 2: ArraySetAsSeries(VALBuffer, true); break;
         case 3: ArraySetAsSeries(IBHBuffer, true); break;
         case 4: ArraySetAsSeries(IBLBuffer, true); break;
         case 5: ArraySetAsSeries(HVN1Buffer, true); break;
         case 6: ArraySetAsSeries(HVN2Buffer, true); break;
         case 7: ArraySetAsSeries(HVN3Buffer, true); break;
         case 8: ArraySetAsSeries(HVN4Buffer, true); break;
         case 9: ArraySetAsSeries(HVN5Buffer, true); break;
         case 10: ArraySetAsSeries(LVN1Buffer, true); break;
         case 11: ArraySetAsSeries(LVN2Buffer, true); break;
         case 12: ArraySetAsSeries(LVN3Buffer, true); break;
         case 13: ArraySetAsSeries(LVN4Buffer, true); break;
         case 14: ArraySetAsSeries(LVN5Buffer, true); break;
         case 15: ArraySetAsSeries(VPOCBuffer, true); break;
         case 16: ArraySetAsSeries(VVAHBuffer, true); break;
         case 17: ArraySetAsSeries(VVALBuffer, true); break;
         case 18: ArraySetAsSeries(BullPOCBuffer, true); break;
         case 19: ArraySetAsSeries(BearPOCBuffer, true); break;
         case 20: ArraySetAsSeries(TotalVolumeBuffer, true); break;
         case 21: ArraySetAsSeries(TotalBullVolumeBuffer, true); break;
         case 22: ArraySetAsSeries(TotalBearVolumeBuffer, true); break;
      }
   }

   ArrayInitialize(volumeBins, 0.0);
   ArrayInitialize(bullVolumeBins, 0.0);
   ArrayInitialize(bearVolumeBins, 0.0);
   ArrayInitialize(clusterBins, 0.0);

   string binDesc = (BinSizeMode == 2) ? ("F" + IntegerToString(TicksPerBin)) : IntegerToString(ProfileRows);
   string shortName = "VPCH(" + IntegerToString(SessionStartHours) + ":" + IntegerToString(SessionStartMinutes) +
                      "-" + IntegerToString(SessionEndHours) + ":" + IntegerToString(SessionEndMinutes) +
                      "," + binDesc + "," + DoubleToString(ValueAreaPct, 0) + ")";
   IndicatorSetString(INDICATOR_SHORTNAME, shortName);
   ResetState();

   // Initialize visual display
   if(InpShowProfile)
   {
      string prefix = "VPCH4_" + IntegerToString(SessionStartHours*100+SessionEndHours) + "_";
      VPDisplayInit(prefix, MathMin(VP_MAX_HIST, MathMax(1, InpMaxDisplaySess)));
      g_vpShowStats = InpShowStats;
   }

   return INIT_SUCCEEDED;
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
void ResetState()
{
   currentSessionStart = 0; currentSessionEnd = 0;
   prevSessionStart = 0; prevSessionEnd = 0;
   prevPOC = 0; prevVAH = 0; prevVAL = 0;
   prevIBH = 0; prevIBL = 0;
   ArrayInitialize(prevHVN, 0.0);
   ArrayInitialize(prevLVN, 0.0);
   prevVPOC = 0; prevVVAH = 0; prevVVAL = 0;
   prevBullPOC = 0; prevBearPOC = 0;
   prevTotalVolume = 0; prevTotalBullVolume = 0; prevTotalBearVolume = 0;
}

//+------------------------------------------------------------------+
bool IsSunday(datetime t)
{
   MqlDateTime dt;
   TimeToStruct(t, dt);
   return (dt.day_of_week == 0);
}

//+------------------------------------------------------------------+
int GetIBPeriodSeconds()
{
   if(IBMinutes > 0)
      return IBMinutes * 60;
   return 3600; // 60 minutes default for custom hours
}

//+------------------------------------------------------------------+
void CalculateSessionBoundaries(datetime curTime)
{
   MqlDateTime dt;
   TimeToStruct(curTime, dt);
   dt.hour = 0; dt.min = 0; dt.sec = 0;
   datetime dayStart = StructToTime(dt);

   int startMins = SessionStartHours * 60 + SessionStartMinutes;
   int endMins   = SessionEndHours * 60 + SessionEndMinutes;

   MqlDateTime stDt;
   TimeToStruct(dayStart, stDt);
   stDt.hour = SessionStartHours;
   stDt.min  = SessionStartMinutes;
   datetime sessionStartToday = StructToTime(stDt);

   datetime sessionEndToday;
   if(endMins > startMins)
   {
      MqlDateTime edDt;
      TimeToStruct(dayStart, edDt);
      edDt.hour = SessionEndHours;
      edDt.min  = SessionEndMinutes;
      sessionEndToday = StructToTime(edDt);
   }
   else
   {
      // Overnight session
      MqlDateTime edDt;
      TimeToStruct(dayStart + 86400, edDt);
      edDt.hour = SessionEndHours;
      edDt.min  = SessionEndMinutes;
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
int OnCalculate(const int rates_total,
                const int prev_calculated,
                const datetime &time[],
                const double &open[],
                const double &high[],
                const double &low[],
                const double &close[],
                const long &tick_volume[],
                const long &volume[],
                const int &spread[])
{
   if(rates_total < 2) return 0;

   ArraySetAsSeries(time,  true);
   ArraySetAsSeries(open,  true);
   ArraySetAsSeries(high,  true);
   ArraySetAsSeries(low,   true);
   ArraySetAsSeries(close, true);

   if(prev_calculated == 0)
      ResetState();

   int limit = rates_total - (prev_calculated > 0 ? prev_calculated : 1);
   bool actualMode = (SessionMode == 2);

   for(int i = limit; i >= 0; i--)
   {
      datetime curTime = time[i];

      if(!actualMode)
      {
         if(currentSessionEnd == 0 || curTime >= currentSessionEnd)
         {
            prevSessionStart = currentSessionStart;
            prevSessionEnd   = currentSessionEnd;
            CalculateSessionBoundaries(curTime);

            if(prevSessionStart > 0 && IsSunday(prevSessionStart))
            {
               MqlDateTime sd;
               TimeToStruct(prevSessionStart, sd);
               sd.hour = 0; sd.min = 0; sd.sec = 0;
               prevSessionStart = StructToTime(sd) - 2 * 86400;
               prevSessionEnd   = prevSessionStart + 86400;
            }

            if(prevSessionStart > 0 && prevSessionEnd > 0)
               CalculateVolumeProfile();
         }
      }
      else
      {
         if(currentSessionEnd == 0 || curTime >= currentSessionEnd)
            CalculateSessionBoundaries(curTime);

         prevSessionStart = currentSessionStart;
         prevSessionEnd   = (datetime)MathMin((double)currentSessionEnd, (double)curTime);

         if(prevSessionStart > 0 && prevSessionEnd > prevSessionStart)
            CalculateVolumeProfile();
      }

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
      VPOCBuffer[i]            = prevPOC;
      VVAHBuffer[i]            = prevVVAH;
      VVALBuffer[i]            = prevVVAL;
      BullPOCBuffer[i]         = prevBullPOC;
      BearPOCBuffer[i]         = prevBearPOC;
      TotalVolumeBuffer[i]     = prevTotalVolume;
      TotalBullVolumeBuffer[i] = prevTotalBullVolume;
      TotalBearVolumeBuffer[i] = prevTotalBearVolume;
   }

   return rates_total;
}

//+------------------------------------------------------------------+
void CalculateVolumeProfile()
{
   double sessionHigh = -DBL_MAX;
   double sessionLow  =  DBL_MAX;
   int    barsInSession = 0;

   datetime ibEndTime = prevSessionStart + GetIBPeriodSeconds();
   double   ibHigh = -DBL_MAX;
   double   ibLow  =  DBL_MAX;

   for(int i = 0; i < Bars; i++)
   {
      datetime barTime = iTime(NULL, 0, i);
      if(barTime < prevSessionStart) break;
      if(barTime >= prevSessionStart && barTime < prevSessionEnd)
      {
         if(IsSunday(barTime)) continue;
         double hi = iHigh(NULL, 0, i);
         double lo = iLow(NULL, 0, i);
         if(hi > sessionHigh) sessionHigh = hi;
         if(lo < sessionLow)  sessionLow  = lo;
         barsInSession++;
         if(barTime < ibEndTime)
         {
            if(hi > ibHigh) ibHigh = hi;
            if(lo < ibLow)  ibLow  = lo;
         }
      }
   }

   if(barsInSession == 0 || sessionHigh <= sessionLow) return;
   if(ibHigh > ibLow) { prevIBH = ibHigh; prevIBL = ibLow; }

   // Store session extents for display
   lastSessionHigh = sessionHigh;
   lastSessionLow  = sessionLow;

   double range = sessionHigh - sessionLow;
   int numBins; double binSize;

   if(BinSizeMode == 2)
   {
      binSize = TicksPerBin * _Point;
      numBins = (int)MathCeil(range / binSize);
      numBins = (int)MathMax(1, MathMin(numBins, MAX_BINS));
   }
   else
   {
      numBins = ProfileRows;
      binSize = range / numBins;
   }

   for(int j = 0; j < numBins; j++)
   {
      volumeBins[j] = 0.0;
      bullVolumeBins[j] = 0.0;
      bearVolumeBins[j] = 0.0;
   }

   double totalVolume = 0, totalBullVolume = 0, totalBearVolume = 0;

   for(int i = 0; i < Bars; i++)
   {
      datetime barTime = iTime(NULL, 0, i);
      if(barTime < prevSessionStart) break;
      if(barTime >= prevSessionStart && barTime < prevSessionEnd)
      {
         if(IsSunday(barTime)) continue;
         double cl = iClose(NULL, 0, i);
         double op = iOpen(NULL, 0, i);
         double hi = iHigh(NULL, 0, i);
         double lo = iLow(NULL, 0, i);
         double barVol = (double)iVolume(NULL, 0, i);

         int vBinHigh = (int)((hi - sessionLow) / binSize);
         int vBinLow  = (int)((lo - sessionLow) / binSize);
         vBinHigh = (int)MathMax(0, MathMin(numBins - 1, vBinHigh));
         vBinLow  = (int)MathMax(0, MathMin(numBins - 1, vBinLow));
         double binsSpanned = (double)(vBinHigh - vBinLow + 1);
         double perBin = barVol / binsSpanned;
         bool isBull = (cl >= op);

         for(int b = vBinLow; b <= vBinHigh; b++)
         {
            volumeBins[b] += perBin;
            if(isBull) bullVolumeBins[b] += perBin;
            else       bearVolumeBins[b] += perBin;
         }
         totalVolume += barVol;
         if(isBull) totalBullVolume += barVol;
         else       totalBearVolume += barVol;
      }
   }

   // Find POC
   int pocIndex = 0; double maxVolume = volumeBins[0];
   for(int j = 1; j < numBins; j++)
   {
      if(volumeBins[j] > maxVolume) { maxVolume = volumeBins[j]; pocIndex = j; }
   }
   prevPOC = sessionLow + (pocIndex + 0.5) * binSize;

   // Value Area
   CalculateValueArea(pocIndex, totalVolume, binSize, sessionLow, numBins);

   // HVN / LVN
   FindHVNs(pocIndex, binSize, sessionLow, numBins);
   if(EnableLVN) FindLVNs(pocIndex, binSize, sessionLow, numBins);
   else ArrayInitialize(prevLVN, 0.0);

   // VCP
   if(EnableVCP) ApplyClusterEnhancement(numBins, binSize, sessionLow, totalVolume);
   else { prevVPOC = prevPOC; prevVVAH = prevVAH; prevVVAL = prevVAL; }

   // Bull/Bear POC
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

   // Store numBins for display
   lastNumBins = numBins;

   //--- Push to visual display ---
   if(InpShowProfile)
   {
      if(EnableVCP)
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
   double targetVolume = totalVolume * (ValueAreaPct / 100.0);
   double accumulatedVolume = volumeBins[pocIndex];
   int upperIndex = pocIndex, lowerIndex = pocIndex;

   while(accumulatedVolume < targetVolume)
   {
      bool canGoUp = (upperIndex + 1) < numBins;
      bool canGoDown = (lowerIndex - 1) >= 0;
      if(!canGoUp && !canGoDown) break;
      double volumeAbove = canGoUp   ? volumeBins[upperIndex + 1] : -1;
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

   double threshold = maxVol * HvnThresholdPct / 100.0;
   int maxNodes = MathMin(HvnCount, 5);
   int candidateIdx[MAX_BINS]; double candidateVol[MAX_BINS]; int candidateCount = 0;

   for(int j = 0; j < numBins; j++)
   {
      if(j == pocIndex) continue;
      if(volumeBins[j] < threshold) continue;
      double left  = (j > 0) ? volumeBins[j-1] : -1;
      double right = (j < numBins-1) ? volumeBins[j+1] : -1;
      if(volumeBins[j] > left && volumeBins[j] > right)
      {
         candidateIdx[candidateCount] = j;
         candidateVol[candidateCount] = volumeBins[j];
         candidateCount++;
      }
   }

   for(int a = 0; a < candidateCount - 1; a++)
   {
      int best = a;
      for(int b = a+1; b < candidateCount; b++)
         if(candidateVol[b] > candidateVol[best]) best = b;
      if(best != a)
      {
         double tmpV = candidateVol[a]; candidateVol[a] = candidateVol[best]; candidateVol[best] = tmpV;
         int tmpI = candidateIdx[a]; candidateIdx[a] = candidateIdx[best]; candidateIdx[best] = tmpI;
      }
   }

   int accepted[5]; int acceptedCount = 0;
   for(int c = 0; c < candidateCount && acceptedCount < maxNodes; c++)
   {
      int idx = candidateIdx[c]; double vol = candidateVol[c]; bool ok = true;
      for(int k = 0; k < acceptedCount; k++)
      {
         int prevIdx = accepted[k];
         int rangeStart = MathMin(idx, prevIdx) + 1;
         int rangeEnd = MathMax(idx, prevIdx);
         double minBetween = DBL_MAX;
         for(int m = rangeStart; m < rangeEnd; m++)
            if(volumeBins[m] < minBetween) minBetween = volumeBins[m];
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

   double threshold = maxVol * LvnThresholdPct / 100.0;
   int maxNodes = MathMin(HvnCount, 5);
   int candidateIdx[MAX_BINS]; double candidateVol[MAX_BINS]; int candidateCount = 0;

   for(int j = 1; j < numBins - 1; j++)
   {
      if(volumeBins[j] > threshold) continue;
      if(volumeBins[j] < volumeBins[j-1] && volumeBins[j] < volumeBins[j+1])
      {
         candidateIdx[candidateCount] = j;
         candidateVol[candidateCount] = volumeBins[j];
         candidateCount++;
      }
   }

   for(int a = 0; a < candidateCount - 1; a++)
   {
      int best = a;
      for(int b = a+1; b < candidateCount; b++)
         if(candidateVol[b] < candidateVol[best]) best = b;
      if(best != a)
      {
         double tmpV = candidateVol[a]; candidateVol[a] = candidateVol[best]; candidateVol[best] = tmpV;
         int tmpI = candidateIdx[a]; candidateIdx[a] = candidateIdx[best]; candidateIdx[best] = tmpI;
      }
   }

   int count = MathMin(maxNodes, candidateCount);
   for(int n = 0; n < count; n++)
      prevLVN[n] = sessionLow + (candidateIdx[n] + 0.5) * binSize;
}

//+------------------------------------------------------------------+
void ApplyClusterEnhancement(int numBins, double binSize, double sessionLow, double totalVolume)
{
   double avg = totalVolume / MathMax(1, numBins);
   int peakIndices[MAX_BINS]; double peakValues[MAX_BINS]; int peakCount = 0;

   for(int j = 0; j < numBins; j++)
   {
      bool leftOk  = (j == 0) || (volumeBins[j] >= volumeBins[j-1]);
      bool rightOk = (j == numBins-1) || (volumeBins[j] >= volumeBins[j+1]);
      if(leftOk && rightOk && volumeBins[j] > avg)
      {
         peakIndices[peakCount] = j; peakValues[peakCount] = volumeBins[j]; peakCount++;
      }
   }

   int maxCenters = MathMin(MaxClusterCenters, peakCount);
   if(maxCenters == 0) { prevVPOC = prevPOC; prevVVAH = prevVAH; prevVVAL = prevVAL; return; }

   for(int a = 0; a < maxCenters; a++)
   {
      int bestIdx = a;
      for(int b = a+1; b < peakCount; b++)
         if(peakValues[b] > peakValues[bestIdx]) bestIdx = b;
      if(bestIdx != a)
      {
         int tmpI = peakIndices[a]; peakIndices[a] = peakIndices[bestIdx]; peakIndices[bestIdx] = tmpI;
         double tmpV = peakValues[a]; peakValues[a] = peakValues[bestIdx]; peakValues[bestIdx] = tmpV;
      }
   }

   double sigma = ClusterSpread;
   for(int j = 0; j < numBins; j++) clusterBins[j] = 0;
   for(int c = 0; c < maxCenters; c++)
   {
      int center = peakIndices[c]; double centerVol = peakValues[c];
      for(int j = 0; j < numBins; j++)
      {
         double dist = (j - center) / sigma;
         clusterBins[j] += centerVol * MathExp(-0.5 * dist * dist);
      }
   }

   int vpocIdx = 0; double vpocMax = clusterBins[0];
   for(int j = 1; j < numBins; j++)
      if(clusterBins[j] > vpocMax) { vpocMax = clusterBins[j]; vpocIdx = j; }
   prevVPOC = sessionLow + (vpocIdx + 0.5) * binSize;

   double clusterTotal = 0;
   for(int j = 0; j < numBins; j++) clusterTotal += clusterBins[j];
   double targetVolume = clusterTotal * (ValueAreaPct / 100.0);
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
