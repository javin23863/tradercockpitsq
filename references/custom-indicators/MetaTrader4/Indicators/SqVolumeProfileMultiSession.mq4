//+------------------------------------------------------------------+
//|                           SqVolumeProfileMultiSession.mq4        |
//|                           Copyright © 2026, StrategyQuant s.r.o. |
//|                                     http://www.strategyquant.com |
//+------------------------------------------------------------------+
#property copyright "Copyright © 2026, StrategyQuant s.r.o."
#property link      "http://www.strategyquant.com"
#property version   "2.00"
#property strict
#property description "Volume Profile MultiSession — 4 configurable sessions (London, NewYork, Sydney, Tokyo)"

#include "../Include/SqVPDisplay.mqh"
#property indicator_chart_window
#property indicator_buffers 35
#property indicator_plots   35

//--- Primary outputs (first enabled session)
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

//--- Per-session outputs (buffers 23-34)
#property indicator_label24 "LondonPOC"
#property indicator_type24  DRAW_LINE
#property indicator_color24 clrYellow
#property indicator_width24 1

#property indicator_label25 "LondonVAH"
#property indicator_type25  DRAW_LINE
#property indicator_color25 clrGreen
#property indicator_width25 1

#property indicator_label26 "LondonVAL"
#property indicator_type26  DRAW_LINE
#property indicator_color26 clrRed
#property indicator_width26 1

#property indicator_label27 "NewYorkPOC"
#property indicator_type27  DRAW_LINE
#property indicator_color27 clrYellow
#property indicator_width27 1

#property indicator_label28 "NewYorkVAH"
#property indicator_type28  DRAW_LINE
#property indicator_color28 clrGreen
#property indicator_width28 1

#property indicator_label29 "NewYorkVAL"
#property indicator_type29  DRAW_LINE
#property indicator_color29 clrRed
#property indicator_width29 1

#property indicator_label30 "SydneyPOC"
#property indicator_type30  DRAW_LINE
#property indicator_color30 clrYellow
#property indicator_width30 1

#property indicator_label31 "SydneyVAH"
#property indicator_type31  DRAW_LINE
#property indicator_color31 clrGreen
#property indicator_width31 1

#property indicator_label32 "SydneyVAL"
#property indicator_type32  DRAW_LINE
#property indicator_color32 clrRed
#property indicator_width32 1

#property indicator_label33 "TokyoPOC"
#property indicator_type33  DRAW_LINE
#property indicator_color33 clrYellow
#property indicator_width33 1

#property indicator_label34 "TokyoVAH"
#property indicator_type34  DRAW_LINE
#property indicator_color34 clrGreen
#property indicator_width34 1

#property indicator_label35 "TokyoVAL"
#property indicator_type35  DRAW_LINE
#property indicator_color35 clrRed
#property indicator_width35 1

//--- Session inputs
input bool   EnableLondon       = true;
input int    LondonStartHour    = 7;
input int    LondonStartMin     = 0;
input int    LondonEndHour      = 16;
input int    LondonEndMin       = 0;

input bool   EnableNewYork      = true;
input int    NewYorkStartHour   = 13;
input int    NewYorkStartMin    = 0;
input int    NewYorkEndHour     = 22;
input int    NewYorkEndMin      = 0;

input bool   EnableSydney       = false;
input int    SydneyStartHour    = 21;
input int    SydneyStartMin     = 0;
input int    SydneyEndHour      = 6;
input int    SydneyEndMin       = 0;

input bool   EnableTokyo        = true;
input int    TokyoStartHour     = 0;
input int    TokyoStartMin      = 0;
input int    TokyoEndHour       = 9;
input int    TokyoEndMin        = 0;

input int    SessionMode        = 1;      // 1=Previous, 2=Actual
input int    ProfileRows        = 150;
input int    BinSizeMode        = 2;      // 1=Range-Based, 2=Fixed
input int    TicksPerBin        = 3;
input double ValueAreaPct       = 70.0;
input int    HvnCount           = 5;
input int    HvnThresholdPct    = 20;
input int    LvnThresholdPct    = 40;
input bool   EnableLVN          = false;
input bool   EnableVCP          = false;
input double ClusterSpread      = 6.0;
input int    MaxClusterCenters  = 2;
input int    IBMinutes          = 0;      // IB override in minutes (0 = 60min default)

//--- Display parameters (visual profile on chart)
input bool   InpShowProfile     = true;   // Show visual profile on chart
input bool   InpShowVAShading   = true;   // Show Value Area shading
input bool   InpShowIBBox       = true;   // Show Initial Balance box
input bool   InpShowLevelLabels = true;   // Show level labels (POC, VAH, ...)
input bool   InpShowStats       = false;  // Show session statistics panel
input int    InpMaxDisplaySess  = 15;     // Max sessions to display (1-50)

//--- Output buffers
double POCBuffer[], VAHBuffer[], VALBuffer[], IBHBuffer[], IBLBuffer[];
double HVN1Buffer[], HVN2Buffer[], HVN3Buffer[], HVN4Buffer[], HVN5Buffer[];
double LVN1Buffer[], LVN2Buffer[], LVN3Buffer[], LVN4Buffer[], LVN5Buffer[];
double VPOCBuffer[], VVAHBuffer[], VVALBuffer[];
double BullPOCBuffer[], BearPOCBuffer[];
double TotalVolumeBuffer[], TotalBullVolBuffer[], TotalBearVolBuffer[];
double LonPOCBuf[], LonVAHBuf[], LonVALBuf[];
double NYPOCBuf[], NYVAHBuf[], NYVALBuf[];
double SydPOCBuf[], SydVAHBuf[], SydVALBuf[];
double TkyPOCBuf[], TkyVAHBuf[], TkyVALBuf[];

#define MAX_BINS 2000
#define NUM_SESSIONS 4
#define S_LONDON 0
#define S_NEWYORK 1
#define S_SYDNEY 2
#define S_TOKYO 3

double volumeBins[MAX_BINS], bullVolumeBins[MAX_BINS], bearVolumeBins[MAX_BINS], clusterBins[MAX_BINS];

// Per-session state
datetime curSessStart[NUM_SESSIONS], curSessEnd[NUM_SESSIONS];
datetime prvSessStart[NUM_SESSIONS], prvSessEnd[NUM_SESSIONS];
double sPOC[NUM_SESSIONS], sVAH[NUM_SESSIONS], sVAL[NUM_SESSIONS];
double sIBH[NUM_SESSIONS], sIBL[NUM_SESSIONS];
double sHVN[NUM_SESSIONS * 5], sLVN[NUM_SESSIONS * 5];
double sVPOC[NUM_SESSIONS], sVVAH[NUM_SESSIONS], sVVAL[NUM_SESSIONS];
double sBullPOC[NUM_SESSIONS], sBearPOC[NUM_SESSIONS];
double sTotalVol[NUM_SESSIONS], sTotalBullVol[NUM_SESSIONS], sTotalBearVol[NUM_SESSIONS];
int activeSes = 0;

//--- Display tracking per session
double lastSesHigh[NUM_SESSIONS];
double lastSesLow[NUM_SESSIONS];
int    lastSesNB[NUM_SESSIONS];

//+------------------------------------------------------------------+
int OnInit()
{
   IndicatorBuffers(35);
   SetIndexBuffer(0,  POCBuffer);   SetIndexBuffer(1,  VAHBuffer);
   SetIndexBuffer(2,  VALBuffer);   SetIndexBuffer(3,  IBHBuffer);
   SetIndexBuffer(4,  IBLBuffer);   SetIndexBuffer(5,  HVN1Buffer);
   SetIndexBuffer(6,  HVN2Buffer);  SetIndexBuffer(7,  HVN3Buffer);
   SetIndexBuffer(8,  HVN4Buffer);  SetIndexBuffer(9,  HVN5Buffer);
   SetIndexBuffer(10, LVN1Buffer);  SetIndexBuffer(11, LVN2Buffer);
   SetIndexBuffer(12, LVN3Buffer);  SetIndexBuffer(13, LVN4Buffer);
   SetIndexBuffer(14, LVN5Buffer);  SetIndexBuffer(15, VPOCBuffer);
   SetIndexBuffer(16, VVAHBuffer);  SetIndexBuffer(17, VVALBuffer);
   SetIndexBuffer(18, BullPOCBuffer); SetIndexBuffer(19, BearPOCBuffer);
   SetIndexBuffer(20, TotalVolumeBuffer); SetIndexBuffer(21, TotalBullVolBuffer);
   SetIndexBuffer(22, TotalBearVolBuffer);
   SetIndexBuffer(23, LonPOCBuf);  SetIndexBuffer(24, LonVAHBuf);  SetIndexBuffer(25, LonVALBuf);
   SetIndexBuffer(26, NYPOCBuf);   SetIndexBuffer(27, NYVAHBuf);   SetIndexBuffer(28, NYVALBuf);
   SetIndexBuffer(29, SydPOCBuf);  SetIndexBuffer(30, SydVAHBuf);  SetIndexBuffer(31, SydVALBuf);
   SetIndexBuffer(32, TkyPOCBuf);  SetIndexBuffer(33, TkyVAHBuf);  SetIndexBuffer(34, TkyVALBuf);

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
   ArraySetAsSeries(TotalBullVolBuffer, true);
   ArraySetAsSeries(TotalBearVolBuffer, true);
   ArraySetAsSeries(LonPOCBuf, true); ArraySetAsSeries(LonVAHBuf, true); ArraySetAsSeries(LonVALBuf, true);
   ArraySetAsSeries(NYPOCBuf, true);  ArraySetAsSeries(NYVAHBuf, true);  ArraySetAsSeries(NYVALBuf, true);
   ArraySetAsSeries(SydPOCBuf, true); ArraySetAsSeries(SydVAHBuf, true); ArraySetAsSeries(SydVALBuf, true);
   ArraySetAsSeries(TkyPOCBuf, true); ArraySetAsSeries(TkyVAHBuf, true); ArraySetAsSeries(TkyVALBuf, true);

   IndicatorSetString(INDICATOR_SHORTNAME, "VPMulti(" + IntegerToString(ProfileRows) + "," + DoubleToString(ValueAreaPct, 0) + ")");
   ResetState();

   // Initialize visual display
   if(InpShowProfile)
   {
      string prefix = "VPMS4_";
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
   ArrayInitialize(curSessStart, 0); ArrayInitialize(curSessEnd, 0);
   ArrayInitialize(prvSessStart, 0); ArrayInitialize(prvSessEnd, 0);
   ArrayInitialize(sPOC, 0); ArrayInitialize(sVAH, 0); ArrayInitialize(sVAL, 0);
   ArrayInitialize(sIBH, 0); ArrayInitialize(sIBL, 0);
   ArrayInitialize(sHVN, 0); ArrayInitialize(sLVN, 0);
   ArrayInitialize(sVPOC, 0); ArrayInitialize(sVVAH, 0); ArrayInitialize(sVVAL, 0);
   ArrayInitialize(sBullPOC, 0); ArrayInitialize(sBearPOC, 0);
   ArrayInitialize(sTotalVol, 0); ArrayInitialize(sTotalBullVol, 0); ArrayInitialize(sTotalBearVol, 0);
   ArrayInitialize(lastSesHigh, 0); ArrayInitialize(lastSesLow, 0); ArrayInitialize(lastSesNB, 0);
}

bool IsSunday(datetime t) { MqlDateTime dt; TimeToStruct(t, dt); return (dt.day_of_week == 0); }

int GetIBPeriodSeconds() { if(IBMinutes > 0) return IBMinutes * 60; return 3600; }

bool IsSessionEnabled(int s)
{
   if(s == S_LONDON) return EnableLondon; if(s == S_NEWYORK) return EnableNewYork;
   if(s == S_SYDNEY) return EnableSydney; if(s == S_TOKYO) return EnableTokyo;
   return false;
}

void GetSessionTimes(int s, int &sh, int &sm, int &eh, int &em)
{
   if(s == S_LONDON)  { sh = LondonStartHour; sm = LondonStartMin; eh = LondonEndHour; em = LondonEndMin; }
   else if(s == S_NEWYORK) { sh = NewYorkStartHour; sm = NewYorkStartMin; eh = NewYorkEndHour; em = NewYorkEndMin; }
   else if(s == S_SYDNEY)  { sh = SydneyStartHour; sm = SydneyStartMin; eh = SydneyEndHour; em = SydneyEndMin; }
   else { sh = TokyoStartHour; sm = TokyoStartMin; eh = TokyoEndHour; em = TokyoEndMin; }
}

void CalcBoundaries(datetime curTime, int s)
{
   int sh, sm, eh, em; GetSessionTimes(s, sh, sm, eh, em);
   MqlDateTime dt; TimeToStruct(curTime, dt);
   dt.hour = 0; dt.min = 0; dt.sec = 0;
   datetime dayStart = StructToTime(dt);
   int startMins = sh * 60 + sm, endMins = eh * 60 + em;
   MqlDateTime stDt; TimeToStruct(dayStart, stDt); stDt.hour = sh; stDt.min = sm;
   datetime ss = StructToTime(stDt);
   datetime se;
   if(endMins > startMins) { MqlDateTime edDt; TimeToStruct(dayStart, edDt); edDt.hour = eh; edDt.min = em; se = StructToTime(edDt); }
   else { MqlDateTime edDt; TimeToStruct(dayStart + 86400, edDt); edDt.hour = eh; edDt.min = em; se = StructToTime(edDt); }
   if(curTime < ss) { ss -= 86400; se -= 86400; }
   if(curTime >= se) { ss += 86400; se += 86400; }
   curSessStart[s] = ss; curSessEnd[s] = se;
   if(IsSunday(curSessStart[s])) { curSessStart[s] -= 2*86400; curSessEnd[s] -= 2*86400; }
}

//+------------------------------------------------------------------+
int OnCalculate(const int rates_total, const int prev_calculated,
                const datetime &time[], const double &open[],
                const double &high[], const double &low[],
                const double &close[], const long &tick_volume[],
                const long &volume[], const int &spread[])
{
   if(rates_total < 2) return 0;
   ArraySetAsSeries(time, true); ArraySetAsSeries(open, true);
   ArraySetAsSeries(high, true); ArraySetAsSeries(low, true);
   ArraySetAsSeries(close, true);

   if(prev_calculated == 0) ResetState();
   int limit = rates_total - (prev_calculated > 0 ? prev_calculated : 1);
   bool actualMode = (SessionMode == 2);

   for(int i = limit; i >= 0; i--)
   {
      datetime curTime = time[i];
      for(int s = 0; s < NUM_SESSIONS; s++)
      {
         if(!IsSessionEnabled(s)) continue;
         activeSes = s;
         if(!actualMode)
         {
            if(curSessEnd[s] == 0 || curTime >= curSessEnd[s])
            {
               prvSessStart[s] = curSessStart[s]; prvSessEnd[s] = curSessEnd[s];
               CalcBoundaries(curTime, s);
               if(prvSessStart[s] > 0 && IsSunday(prvSessStart[s]))
               { MqlDateTime sd; TimeToStruct(prvSessStart[s], sd); sd.hour=0; sd.min=0; sd.sec=0;
                 prvSessStart[s] = StructToTime(sd) - 2*86400; prvSessEnd[s] = prvSessStart[s] + 86400; }
               if(prvSessStart[s] > 0 && prvSessEnd[s] > 0) CalculateVP(s);
            }
         }
         else
         {
            if(curSessEnd[s] == 0 || curTime >= curSessEnd[s]) CalcBoundaries(curTime, s);
            prvSessStart[s] = curSessStart[s];
            prvSessEnd[s] = (datetime)MathMin((double)curSessEnd[s], (double)curTime);
            if(prvSessStart[s] > 0 && prvSessEnd[s] > prvSessStart[s]) CalculateVP(s);
         }
      }

      // First enabled session drives primary outputs
      int first = -1;
      for(int s = 0; s < NUM_SESSIONS; s++) { if(IsSessionEnabled(s)) { first = s; break; } }
      if(first >= 0)
      {
         POCBuffer[i] = sPOC[first]; VAHBuffer[i] = sVAH[first]; VALBuffer[i] = sVAL[first];
         IBHBuffer[i] = (sIBH[first] == 0) ? EMPTY_VALUE : sIBH[first]; IBLBuffer[i] = (sIBL[first] == 0) ? EMPTY_VALUE : sIBL[first];
         HVN1Buffer[i] = (sHVN[first*5]==0)?EMPTY_VALUE:sHVN[first*5]; HVN2Buffer[i] = (sHVN[first*5+1]==0)?EMPTY_VALUE:sHVN[first*5+1]; HVN3Buffer[i] = (sHVN[first*5+2]==0)?EMPTY_VALUE:sHVN[first*5+2];
         HVN4Buffer[i] = (sHVN[first*5+3]==0)?EMPTY_VALUE:sHVN[first*5+3]; HVN5Buffer[i] = (sHVN[first*5+4]==0)?EMPTY_VALUE:sHVN[first*5+4];
         LVN1Buffer[i] = (sLVN[first*5]==0)?EMPTY_VALUE:sLVN[first*5]; LVN2Buffer[i] = (sLVN[first*5+1]==0)?EMPTY_VALUE:sLVN[first*5+1]; LVN3Buffer[i] = (sLVN[first*5+2]==0)?EMPTY_VALUE:sLVN[first*5+2];
         LVN4Buffer[i] = (sLVN[first*5+3]==0)?EMPTY_VALUE:sLVN[first*5+3]; LVN5Buffer[i] = (sLVN[first*5+4]==0)?EMPTY_VALUE:sLVN[first*5+4];
         VPOCBuffer[i] = sPOC[first]; VVAHBuffer[i] = sVVAH[first]; VVALBuffer[i] = sVVAL[first];
         BullPOCBuffer[i] = sBullPOC[first]; BearPOCBuffer[i] = sBearPOC[first];
         TotalVolumeBuffer[i] = sTotalVol[first];
         TotalBullVolBuffer[i] = sTotalBullVol[first];
         TotalBearVolBuffer[i] = sTotalBearVol[first];
      }

      // Per-session outputs
      LonPOCBuf[i] = sPOC[S_LONDON]; LonVAHBuf[i] = sVAH[S_LONDON]; LonVALBuf[i] = sVAL[S_LONDON];
      NYPOCBuf[i]  = sPOC[S_NEWYORK]; NYVAHBuf[i] = sVAH[S_NEWYORK]; NYVALBuf[i] = sVAL[S_NEWYORK];
      SydPOCBuf[i] = sPOC[S_SYDNEY]; SydVAHBuf[i] = sVAH[S_SYDNEY]; SydVALBuf[i] = sVAL[S_SYDNEY];
      TkyPOCBuf[i] = sPOC[S_TOKYO]; TkyVAHBuf[i] = sVAH[S_TOKYO]; TkyVALBuf[i] = sVAL[S_TOKYO];
   }
   return rates_total;
}

//+------------------------------------------------------------------+
void CalculateVP(int s)
{
   double sessionHigh = -DBL_MAX, sessionLow = DBL_MAX;
   int barsInSession = 0;
   datetime ibEndTime = prvSessStart[s] + GetIBPeriodSeconds();
   double ibHigh = -DBL_MAX, ibLow = DBL_MAX;

   for(int i = 0; i < Bars; i++)
   {
      datetime bt = iTime(NULL, 0, i);
      if(bt < prvSessStart[s]) break;
      if(bt >= prvSessStart[s] && bt < prvSessEnd[s])
      {
         if(IsSunday(bt)) continue;
         double hi = iHigh(NULL, 0, i), lo = iLow(NULL, 0, i);
         if(hi > sessionHigh) sessionHigh = hi;
         if(lo < sessionLow) sessionLow = lo;
         barsInSession++;
         if(bt < ibEndTime) { if(hi > ibHigh) ibHigh = hi; if(lo < ibLow) ibLow = lo; }
      }
   }
   if(barsInSession == 0 || sessionHigh <= sessionLow) return;
   if(ibHigh > ibLow) { sIBH[s] = ibHigh; sIBL[s] = ibLow; }

   // Store for display
   lastSesHigh[s] = sessionHigh;
   lastSesLow[s]  = sessionLow;

   double range = sessionHigh - sessionLow;
   int numBins; double binSize;
   if(BinSizeMode == 2)
   { binSize = TicksPerBin * _Point; numBins = (int)MathCeil(range / binSize); numBins = (int)MathMax(1, MathMin(numBins, MAX_BINS)); }
   else { numBins = ProfileRows; binSize = range / numBins; }

   for(int j = 0; j < numBins; j++) { volumeBins[j] = 0; bullVolumeBins[j] = 0; bearVolumeBins[j] = 0; }

   double totalVol = 0, totalBull = 0, totalBear = 0;
   for(int i = 0; i < Bars; i++)
   {
      datetime bt = iTime(NULL, 0, i);
      if(bt < prvSessStart[s]) break;
      if(bt >= prvSessStart[s] && bt < prvSessEnd[s])
      {
         if(IsSunday(bt)) continue;
         double cl = iClose(NULL, 0, i), op = iOpen(NULL, 0, i);
         double hi = iHigh(NULL, 0, i), lo = iLow(NULL, 0, i);
         double bv = (double)iVolume(NULL, 0, i);
         int bh = (int)MathMax(0, MathMin(numBins-1, (int)((hi - sessionLow)/binSize)));
         int bl = (int)MathMax(0, MathMin(numBins-1, (int)((lo - sessionLow)/binSize)));
         double pb = bv / (bh - bl + 1);
         bool isBull = (cl >= op);
         for(int b = bl; b <= bh; b++)
         { volumeBins[b] += pb; if(isBull) bullVolumeBins[b] += pb; else bearVolumeBins[b] += pb; }
         totalVol += bv; if(isBull) totalBull += bv; else totalBear += bv;
      }
   }

   int pocIndex = 0; double maxVol = volumeBins[0];
   for(int j = 1; j < numBins; j++) if(volumeBins[j] > maxVol) { maxVol = volumeBins[j]; pocIndex = j; }
   sPOC[s] = sessionLow + (pocIndex + 0.5) * binSize;

   // Value Area
   double target = totalVol * (ValueAreaPct / 100.0);
   double acc = volumeBins[pocIndex]; int up = pocIndex, dn = pocIndex;
   while(acc < target)
   {
      bool canUp = (up+1) < numBins, canDn = (dn-1) >= 0;
      if(!canUp && !canDn) break;
      double vUp = canUp ? volumeBins[up+1] : -1, vDn = canDn ? volumeBins[dn-1] : -1;
      if(vUp >= vDn) { up++; acc += volumeBins[up]; } else { dn--; acc += volumeBins[dn]; }
   }
   sVAL[s] = sessionLow + dn * binSize;
   sVAH[s] = sessionLow + (up + 1) * binSize;

   // HVN
   for(int h = 0; h < 5; h++) sHVN[s*5+h] = 0;
   double mxV = 0; for(int j = 0; j < numBins; j++) if(volumeBins[j] > mxV) mxV = volumeBins[j];
   if(mxV > 0)
   {
      double thr = mxV * HvnThresholdPct / 100.0;
      int cIdx[MAX_BINS]; double cVol[MAX_BINS]; int cc = 0;
      for(int j = 0; j < numBins; j++)
      {
         if(j == pocIndex || volumeBins[j] < thr) continue;
         double lt = (j > 0) ? volumeBins[j-1] : -1, rt = (j < numBins-1) ? volumeBins[j+1] : -1;
         if(volumeBins[j] > lt && volumeBins[j] > rt) { cIdx[cc] = j; cVol[cc] = volumeBins[j]; cc++; }
      }
      for(int a = 0; a < cc-1; a++) { int best = a; for(int b = a+1; b < cc; b++) if(cVol[b] > cVol[best]) best = b;
         if(best != a) { double tv = cVol[a]; cVol[a] = cVol[best]; cVol[best] = tv; int ti = cIdx[a]; cIdx[a] = cIdx[best]; cIdx[best] = ti; } }
      int mx = MathMin(HvnCount, 5), ac2 = 0;
      for(int c = 0; c < cc && ac2 < mx; c++) { sHVN[s*5+ac2] = sessionLow + (cIdx[c]+0.5)*binSize; ac2++; }
   }

   // LVN (simplified)
   for(int h = 0; h < 5; h++) sLVN[s*5+h] = 0;
   if(EnableLVN && mxV > 0)
   {
      double thr2 = mxV * LvnThresholdPct / 100.0;
      int lIdx[MAX_BINS]; double lVol[MAX_BINS]; int lc = 0;
      for(int j = 1; j < numBins-1; j++)
      {
         if(volumeBins[j] > thr2) continue;
         if(volumeBins[j] < volumeBins[j-1] && volumeBins[j] < volumeBins[j+1])
         { lIdx[lc] = j; lVol[lc] = volumeBins[j]; lc++; }
      }
      for(int a = 0; a < lc-1; a++) { int best = a; for(int b = a+1; b < lc; b++) if(lVol[b] < lVol[best]) best = b;
         if(best != a) { double tv = lVol[a]; lVol[a] = lVol[best]; lVol[best] = tv; int ti = lIdx[a]; lIdx[a] = lIdx[best]; lIdx[best] = ti; } }
      int mx2 = MathMin(HvnCount, 5), cnt = MathMin(mx2, lc);
      for(int n = 0; n < cnt; n++) sLVN[s*5+n] = sessionLow + (lIdx[n]+0.5)*binSize;
   }

   // VCP
   if(EnableVCP)
   {
      double avg = totalVol / MathMax(1, numBins);
      int pIdx[MAX_BINS]; double pVal[MAX_BINS]; int pc = 0;
      for(int j = 0; j < numBins; j++)
      { bool lok = (j==0)||(volumeBins[j]>=volumeBins[j-1]); bool rok = (j==numBins-1)||(volumeBins[j]>=volumeBins[j+1]);
        if(lok && rok && volumeBins[j] > avg) { pIdx[pc] = j; pVal[pc] = volumeBins[j]; pc++; } }
      int mc2 = MathMin(MaxClusterCenters, pc);
      if(mc2 > 0)
      {
         for(int a = 0; a < mc2; a++) { int best = a; for(int b = a+1; b < pc; b++) if(pVal[b] > pVal[best]) best = b;
            if(best!=a) { int ti=pIdx[a]; pIdx[a]=pIdx[best]; pIdx[best]=ti; double tv=pVal[a]; pVal[a]=pVal[best]; pVal[best]=tv; } }
         for(int j = 0; j < numBins; j++) clusterBins[j] = 0;
         for(int c = 0; c < mc2; c++)
         { int cn = pIdx[c]; double cv = pVal[c];
           for(int j = 0; j < numBins; j++) { double d = (j-cn)/ClusterSpread; clusterBins[j] += cv * MathExp(-0.5*d*d); } }
         int vi = 0; double vm = clusterBins[0];
         for(int j = 1; j < numBins; j++) if(clusterBins[j] > vm) { vm = clusterBins[j]; vi = j; }
         sVPOC[s] = sessionLow + (vi+0.5)*binSize;
         double ct = 0; for(int j = 0; j < numBins; j++) ct += clusterBins[j];
         double tg = ct * (ValueAreaPct/100.0); double ac3 = clusterBins[vi];
         int l2 = vi, h2 = vi;
         while(ac3 < tg && (l2 > 0 || h2 < numBins-1))
         { double el = (l2>0)?clusterBins[l2-1]:0; double eh = (h2<numBins-1)?clusterBins[h2+1]:0;
           if(el >= eh && l2 > 0) { l2--; ac3 += clusterBins[l2]; }
           else if(h2 < numBins-1) { h2++; ac3 += clusterBins[h2]; }
           else { l2--; ac3 += clusterBins[l2]; } }
         sVVAL[s] = sessionLow + l2 * binSize;
         sVVAH[s] = sessionLow + (h2+1) * binSize;
      }
      else { sVPOC[s] = sPOC[s]; sVVAH[s] = sVAH[s]; sVVAL[s] = sVAL[s]; }
   }
   else { sVPOC[s] = sPOC[s]; sVVAH[s] = sVAH[s]; sVVAL[s] = sVAL[s]; }

   // Bull/Bear POC
   int bpi = 0; double bpm = bullVolumeBins[0]; int epi = 0; double epm = bearVolumeBins[0];
   for(int j = 1; j < numBins; j++)
   { if(bullVolumeBins[j] > bpm) { bpm = bullVolumeBins[j]; bpi = j; }
     if(bearVolumeBins[j] > epm) { epm = bearVolumeBins[j]; epi = j; } }
   sBullPOC[s] = (bpm > 0) ? sessionLow + (bpi+0.5)*binSize : 0;
   sBearPOC[s] = (epm > 0) ? sessionLow + (epi+0.5)*binSize : 0;
   sTotalVol[s] = totalVol; sTotalBullVol[s] = totalBull; sTotalBearVol[s] = totalBear;

   // Store numBins for display
   lastSesNB[s] = numBins;

   //--- Push to visual display ---
   if(InpShowProfile)
   {
      double dummyLVN[5];
      for(int h2 = 0; h2 < 5; h2++) dummyLVN[h2] = sLVN[s*5+h2];
      double dummyHVN[5];
      for(int h2 = 0; h2 < 5; h2++) dummyHVN[h2] = sHVN[s*5+h2];
      if(EnableVCP)
         VPDisplayPushSession(
            prvSessStart[s], prvSessEnd[s],
            lastSesHigh[s], lastSesLow[s],
            sPOC[s], sVAH[s], sVAL[s],
            sIBH[s], sIBL[s],
            dummyHVN, dummyLVN,
            numBins,
            clusterBins, bullVolumeBins, bearVolumeBins,
            InpMaxDisplaySess);
      else
         VPDisplayPushSession(
            prvSessStart[s], prvSessEnd[s],
            lastSesHigh[s], lastSesLow[s],
            sPOC[s], sVAH[s], sVAL[s],
            sIBH[s], sIBL[s],
            dummyHVN, dummyLVN,
            numBins,
            volumeBins, bullVolumeBins, bearVolumeBins,
            InpMaxDisplaySess);
      // Set session name for stats display
      string sesNames[4];
      sesNames[0] = "London"; sesNames[1] = "New York";
      sesNames[2] = "Sydney"; sesNames[3] = "Tokyo";
      for(int si = g_vpSesCount - 1; si >= MathMax(0, g_vpSesCount - 8); si--)
      {
         if(g_vpSessions[si].sesStart == prvSessStart[s])
         { g_vpSessions[si].sessionName = sesNames[s]; break; }
      }
   }
}
//+------------------------------------------------------------------+
