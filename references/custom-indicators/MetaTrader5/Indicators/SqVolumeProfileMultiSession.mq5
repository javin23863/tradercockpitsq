//+------------------------------------------------------------------+
//|                           SqVolumeProfileMultiSession.mq5        |
//|                           Copyright © 2026, StrategyQuant s.r.o. |
//|                                     http://www.strategyquant.com |
//+------------------------------------------------------------------+
#property copyright   "Copyright © 2026, StrategyQuant s.r.o."
#property link        "http://www.strategyquant.com"
#property version     "2.00"
#property description "Volume Profile MultiSession — 4 configurable sessions"

#include "../Include/SqVPDisplay.mqh"
#property indicator_chart_window
#property indicator_buffers 35
#property indicator_plots   35

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

#property indicator_label5  "IBL"
#property indicator_type5   DRAW_LINE
#property indicator_color5  clrDodgerBlue
#property indicator_style5  STYLE_DASH

#property indicator_label6  "HVN1"
#property indicator_type6   DRAW_LINE
#property indicator_color6  clrPurple

#property indicator_label7  "HVN2"
#property indicator_type7   DRAW_LINE
#property indicator_color7  clrPurple

#property indicator_label8  "HVN3"
#property indicator_type8   DRAW_LINE
#property indicator_color8  clrPurple

#property indicator_label9  "HVN4"
#property indicator_type9   DRAW_LINE
#property indicator_color9  clrPurple

#property indicator_label10 "HVN5"
#property indicator_type10  DRAW_LINE
#property indicator_color10 clrPurple

#property indicator_label11 "LVN1"
#property indicator_type11  DRAW_LINE
#property indicator_color11 clrOrange

#property indicator_label12 "LVN2"
#property indicator_type12  DRAW_LINE
#property indicator_color12 clrOrange

#property indicator_label13 "LVN3"
#property indicator_type13  DRAW_LINE
#property indicator_color13 clrOrange

#property indicator_label14 "LVN4"
#property indicator_type14  DRAW_LINE
#property indicator_color14 clrOrange

#property indicator_label15 "LVN5"
#property indicator_type15  DRAW_LINE
#property indicator_color15 clrOrange

#property indicator_label16 "POC"
#property indicator_type16  DRAW_LINE
#property indicator_color16 clrAqua

#property indicator_label17 "VAH"
#property indicator_type17  DRAW_LINE
#property indicator_color17 clrGreen

#property indicator_label18 "VAL"
#property indicator_type18  DRAW_LINE
#property indicator_color18 clrGreen

#property indicator_label19 "BullPOC"
#property indicator_type19  DRAW_NONE

#property indicator_label20 "BearPOC"
#property indicator_type20  DRAW_NONE

#property indicator_label21 "TotalVolume"
#property indicator_type21  DRAW_NONE

#property indicator_label22 "TotalBullVolume"
#property indicator_type22  DRAW_NONE

#property indicator_label23 "TotalBearVolume"
#property indicator_type23  DRAW_NONE

//--- Per-session outputs (buffers 23-34)
#property indicator_label24 "LondonPOC"
#property indicator_type24  DRAW_LINE
#property indicator_color24 clrYellow

#property indicator_label25 "LondonVAH"
#property indicator_type25  DRAW_LINE
#property indicator_color25 clrGreen

#property indicator_label26 "LondonVAL"
#property indicator_type26  DRAW_LINE
#property indicator_color26 clrRed

#property indicator_label27 "NewYorkPOC"
#property indicator_type27  DRAW_LINE
#property indicator_color27 clrYellow

#property indicator_label28 "NewYorkVAH"
#property indicator_type28  DRAW_LINE
#property indicator_color28 clrGreen

#property indicator_label29 "NewYorkVAL"
#property indicator_type29  DRAW_LINE
#property indicator_color29 clrRed

#property indicator_label30 "SydneyPOC"
#property indicator_type30  DRAW_LINE
#property indicator_color30 clrYellow

#property indicator_label31 "SydneyVAH"
#property indicator_type31  DRAW_LINE
#property indicator_color31 clrGreen

#property indicator_label32 "SydneyVAL"
#property indicator_type32  DRAW_LINE
#property indicator_color32 clrRed

#property indicator_label33 "TokyoPOC"
#property indicator_type33  DRAW_LINE
#property indicator_color33 clrYellow

#property indicator_label34 "TokyoVAH"
#property indicator_type34  DRAW_LINE
#property indicator_color34 clrGreen

#property indicator_label35 "TokyoVAL"
#property indicator_type35  DRAW_LINE
#property indicator_color35 clrRed

//--- Session inputs
input bool   InpEnableLondon       = true;
input int    InpLondonStartHour    = 7;
input int    InpLondonStartMin     = 0;
input int    InpLondonEndHour      = 16;
input int    InpLondonEndMin       = 0;

input bool   InpEnableNewYork      = true;
input int    InpNewYorkStartHour   = 13;
input int    InpNewYorkStartMin    = 0;
input int    InpNewYorkEndHour     = 22;
input int    InpNewYorkEndMin      = 0;

input bool   InpEnableSydney       = false;
input int    InpSydneyStartHour    = 21;
input int    InpSydneyStartMin     = 0;
input int    InpSydneyEndHour      = 6;
input int    InpSydneyEndMin       = 0;

input bool   InpEnableTokyo        = true;
input int    InpTokyoStartHour     = 0;
input int    InpTokyoStartMin      = 0;
input int    InpTokyoEndHour       = 9;
input int    InpTokyoEndMin        = 0;

input int    InpSessionMode        = 1;
input int    InpProfileRows        = 150;
input int    InpBinSizeMode        = 2;
input int    InpTicksPerBin        = 3;
input double InpValueAreaPct       = 70.0;
input int    InpHvnCount           = 5;
input int    InpHvnThresholdPct    = 20;
input int    InpLvnThresholdPct    = 40;
input bool   InpEnableLVN          = false;
input bool   InpEnableVCP          = false;
input double InpClusterSpread      = 6.0;
input int    InpMaxClusterCenters  = 2;
input int    InpIBMinutes          = 0;

//--- Display parameters (visual profile on chart)
input bool   InpShowProfile        = true;   // Show visual profile on chart
input bool   InpShowVAShading      = true;   // Show Value Area shading
input bool   InpShowIBBox          = true;   // Show Initial Balance box
input bool   InpShowLevelLabels    = true;   // Show level labels (POC, VAH, ...)
input bool   InpShowStats          = false;  // Show session statistics panel
input int    InpMaxDisplaySess     = 15;     // Max sessions to display (1-50)

//--- Buffers
double POCBuf[], VAHBuf[], VALBuf[], IBHBuf[], IBLBuf[];
double HVN1Buf[], HVN2Buf[], HVN3Buf[], HVN4Buf[], HVN5Buf[];
double LVN1Buf[], LVN2Buf[], LVN3Buf[], LVN4Buf[], LVN5Buf[];
double VPOCBuf[], VVAHBuf[], VVALBuf[];
double BullPOCBuf[], BearPOCBuf[];
double TotalVolBuf[], TotalBullVolBuf[], TotalBearVolBuf[];
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

double volBins[], bullBins[], bearBins[], clustBins[];
datetime curSS[NUM_SESSIONS], curSE[NUM_SESSIONS];
datetime prvSS[NUM_SESSIONS], prvSE[NUM_SESSIONS];
double sPOC[NUM_SESSIONS], sVAH[NUM_SESSIONS], sVAL[NUM_SESSIONS];
double sIBH[NUM_SESSIONS], sIBL[NUM_SESSIONS];
double sHVN[]; // [NUM_SESSIONS * 5]
double sLVN[];
double sVPOC[NUM_SESSIONS], sVVAH[NUM_SESSIONS], sVVAL[NUM_SESSIONS];
double sBPOC[NUM_SESSIONS], sEPOC[NUM_SESSIONS];
double sTV[NUM_SESSIONS], sBV[NUM_SESSIONS], sEV[NUM_SESSIONS];

int ExtProfileRows;
double ExtValueAreaPct;

//--- Display tracking per session
double lastSesHigh[NUM_SESSIONS];
double lastSesLow[NUM_SESSIONS];
int    lastSesNB[NUM_SESSIONS];

//+------------------------------------------------------------------+
void OnInit()
{
   ExtProfileRows  = (int)MathMax(10, MathMin(500, InpProfileRows));
   ExtValueAreaPct = MathMax(30.0, MathMin(95.0, InpValueAreaPct));

   ArrayResize(volBins, MAX_BINS); ArrayResize(bullBins, MAX_BINS);
   ArrayResize(bearBins, MAX_BINS); ArrayResize(clustBins, MAX_BINS);
   ArrayResize(sHVN, NUM_SESSIONS * 5); ArrayResize(sLVN, NUM_SESSIONS * 5);

   SetIndexBuffer(0,  POCBuf,  INDICATOR_DATA); SetIndexBuffer(1,  VAHBuf,  INDICATOR_DATA);
   SetIndexBuffer(2,  VALBuf,  INDICATOR_DATA); SetIndexBuffer(3,  IBHBuf,  INDICATOR_DATA);
   SetIndexBuffer(4,  IBLBuf,  INDICATOR_DATA);
   SetIndexBuffer(5,  HVN1Buf, INDICATOR_DATA); SetIndexBuffer(6,  HVN2Buf, INDICATOR_DATA);
   SetIndexBuffer(7,  HVN3Buf, INDICATOR_DATA); SetIndexBuffer(8,  HVN4Buf, INDICATOR_DATA);
   SetIndexBuffer(9,  HVN5Buf, INDICATOR_DATA);
   SetIndexBuffer(10, LVN1Buf, INDICATOR_DATA); SetIndexBuffer(11, LVN2Buf, INDICATOR_DATA);
   SetIndexBuffer(12, LVN3Buf, INDICATOR_DATA); SetIndexBuffer(13, LVN4Buf, INDICATOR_DATA);
   SetIndexBuffer(14, LVN5Buf, INDICATOR_DATA);
   SetIndexBuffer(15, VPOCBuf, INDICATOR_DATA); SetIndexBuffer(16, VVAHBuf, INDICATOR_DATA);
   SetIndexBuffer(17, VVALBuf, INDICATOR_DATA);
   SetIndexBuffer(18, BullPOCBuf, INDICATOR_DATA); SetIndexBuffer(19, BearPOCBuf, INDICATOR_DATA);
   SetIndexBuffer(20, TotalVolBuf, INDICATOR_DATA);
   SetIndexBuffer(21, TotalBullVolBuf, INDICATOR_DATA);
   SetIndexBuffer(22, TotalBearVolBuf, INDICATOR_DATA);
   SetIndexBuffer(23, LonPOCBuf, INDICATOR_DATA);  SetIndexBuffer(24, LonVAHBuf, INDICATOR_DATA);  SetIndexBuffer(25, LonVALBuf, INDICATOR_DATA);
   SetIndexBuffer(26, NYPOCBuf,  INDICATOR_DATA);  SetIndexBuffer(27, NYVAHBuf,  INDICATOR_DATA);  SetIndexBuffer(28, NYVALBuf,  INDICATOR_DATA);
   SetIndexBuffer(29, SydPOCBuf, INDICATOR_DATA);  SetIndexBuffer(30, SydVAHBuf, INDICATOR_DATA);  SetIndexBuffer(31, SydVALBuf, INDICATOR_DATA);
   SetIndexBuffer(32, TkyPOCBuf, INDICATOR_DATA);  SetIndexBuffer(33, TkyVAHBuf, INDICATOR_DATA);  SetIndexBuffer(34, TkyVALBuf, INDICATOR_DATA);

   ArraySetAsSeries(POCBuf, true); ArraySetAsSeries(VAHBuf, true); ArraySetAsSeries(VALBuf, true);
   ArraySetAsSeries(IBHBuf, true); ArraySetAsSeries(IBLBuf, true);
   ArraySetAsSeries(HVN1Buf, true); ArraySetAsSeries(HVN2Buf, true); ArraySetAsSeries(HVN3Buf, true);
   ArraySetAsSeries(HVN4Buf, true); ArraySetAsSeries(HVN5Buf, true);
   ArraySetAsSeries(LVN1Buf, true); ArraySetAsSeries(LVN2Buf, true); ArraySetAsSeries(LVN3Buf, true);
   ArraySetAsSeries(LVN4Buf, true); ArraySetAsSeries(LVN5Buf, true);
   ArraySetAsSeries(VPOCBuf, true); ArraySetAsSeries(VVAHBuf, true); ArraySetAsSeries(VVALBuf, true);
   ArraySetAsSeries(BullPOCBuf, true); ArraySetAsSeries(BearPOCBuf, true);
   ArraySetAsSeries(TotalVolBuf, true); ArraySetAsSeries(TotalBullVolBuf, true); ArraySetAsSeries(TotalBearVolBuf, true);
   ArraySetAsSeries(LonPOCBuf, true); ArraySetAsSeries(LonVAHBuf, true); ArraySetAsSeries(LonVALBuf, true);
   ArraySetAsSeries(NYPOCBuf, true);  ArraySetAsSeries(NYVAHBuf, true);  ArraySetAsSeries(NYVALBuf, true);
   ArraySetAsSeries(SydPOCBuf, true); ArraySetAsSeries(SydVAHBuf, true); ArraySetAsSeries(SydVALBuf, true);
   ArraySetAsSeries(TkyPOCBuf, true); ArraySetAsSeries(TkyVAHBuf, true); ArraySetAsSeries(TkyVALBuf, true);

   IndicatorSetInteger(INDICATOR_DIGITS, _Digits);
   IndicatorSetString(INDICATOR_SHORTNAME, "VPMulti(" + IntegerToString(ExtProfileRows) + "," + DoubleToString(ExtValueAreaPct, 0) + ")");

   ResetState();

   // Initialize visual display
   if(InpShowProfile)
   {
      string prefix = "VPMS" + IntegerToString(ChartID()) + "_";
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
void ResetState()
{
   ArrayInitialize(curSS, 0); ArrayInitialize(curSE, 0);
   ArrayInitialize(prvSS, 0); ArrayInitialize(prvSE, 0);
   ArrayInitialize(sPOC, 0); ArrayInitialize(sVAH, 0); ArrayInitialize(sVAL, 0);
   ArrayInitialize(sIBH, 0); ArrayInitialize(sIBL, 0);
   ArrayInitialize(sHVN, 0); ArrayInitialize(sLVN, 0);
   ArrayInitialize(sVPOC, 0); ArrayInitialize(sVVAH, 0); ArrayInitialize(sVVAL, 0);
   ArrayInitialize(sBPOC, 0); ArrayInitialize(sEPOC, 0);
   ArrayInitialize(sTV, 0); ArrayInitialize(sBV, 0); ArrayInitialize(sEV, 0);
   ArrayInitialize(lastSesHigh, 0); ArrayInitialize(lastSesLow, 0); ArrayInitialize(lastSesNB, 0);
}

bool IsSunday(datetime t) { MqlDateTime dt; TimeToStruct(t, dt); return (dt.day_of_week == 0); }
int GetIBPeriodSeconds() { if(InpIBMinutes > 0) return InpIBMinutes * 60; return 3600; }

bool IsEnabled(int s)
{
   if(s==S_LONDON) return InpEnableLondon; if(s==S_NEWYORK) return InpEnableNewYork;
   if(s==S_SYDNEY) return InpEnableSydney; if(s==S_TOKYO) return InpEnableTokyo;
   return false;
}

void GetTimes(int s, int &sh, int &sm, int &eh, int &em)
{
   if(s==S_LONDON) { sh=InpLondonStartHour; sm=InpLondonStartMin; eh=InpLondonEndHour; em=InpLondonEndMin; }
   else if(s==S_NEWYORK) { sh=InpNewYorkStartHour; sm=InpNewYorkStartMin; eh=InpNewYorkEndHour; em=InpNewYorkEndMin; }
   else if(s==S_SYDNEY) { sh=InpSydneyStartHour; sm=InpSydneyStartMin; eh=InpSydneyEndHour; em=InpSydneyEndMin; }
   else { sh=InpTokyoStartHour; sm=InpTokyoStartMin; eh=InpTokyoEndHour; em=InpTokyoEndMin; }
}

void CalcBounds(datetime ct, int s)
{
   int sh, sm, eh, em; GetTimes(s, sh, sm, eh, em);
   MqlDateTime dt; TimeToStruct(ct, dt); dt.hour=0; dt.min=0; dt.sec=0;
   datetime ds = StructToTime(dt);
   int startMins = sh*60+sm, endMins = eh*60+em;
   MqlDateTime stDt; TimeToStruct(ds, stDt); stDt.hour=sh; stDt.min=sm;
   datetime ss = StructToTime(stDt);
   datetime se;
   if(endMins > startMins) { MqlDateTime ed; TimeToStruct(ds, ed); ed.hour=eh; ed.min=em; se = StructToTime(ed); }
   else { MqlDateTime ed; TimeToStruct(ds+86400, ed); ed.hour=eh; ed.min=em; se = StructToTime(ed); }
   if(ct < ss) { ss -= 86400; se -= 86400; }
   if(ct >= se) { ss += 86400; se += 86400; }
   curSS[s] = ss; curSE[s] = se;
   if(IsSunday(curSS[s])) { curSS[s] -= 2*86400; curSE[s] -= 2*86400; }
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
   ArraySetAsSeries(close, true); ArraySetAsSeries(tick_volume, true);
   ArraySetAsSeries(volume, true);

   int startBar;
   if(prev_calculated == 0) { ResetState(); startBar = rates_total - 1; }
   else { startBar = rates_total - prev_calculated; if(startBar < 0) startBar = 0; }

   bool actualMode = (InpSessionMode == 2);

   for(int i = startBar; i >= 0 && !IsStopped(); i--)
   {
      datetime ct = time[i];
      for(int s = 0; s < NUM_SESSIONS; s++)
      {
         if(!IsEnabled(s)) continue;
         if(!actualMode)
         {
            if(curSE[s] == 0 || ct >= curSE[s])
            {
               prvSS[s] = curSS[s]; prvSE[s] = curSE[s];
               CalcBounds(ct, s);
               if(prvSS[s] > 0 && IsSunday(prvSS[s]))
               { MqlDateTime sd; TimeToStruct(prvSS[s], sd); sd.hour=0; sd.min=0; sd.sec=0;
                 prvSS[s] = StructToTime(sd) - 2*86400; prvSE[s] = prvSS[s] + 86400; }
               if(prvSS[s] > 0 && prvSE[s] > 0)
                  CalcVP(s, i, time, open, high, low, close, volume, tick_volume, rates_total);
            }
         }
         else
         {
            if(curSE[s] == 0 || ct >= curSE[s]) CalcBounds(ct, s);
            prvSS[s] = curSS[s];
            prvSE[s] = (datetime)MathMin((double)curSE[s], (double)ct);
            if(prvSS[s] > 0 && prvSE[s] > prvSS[s])
               CalcVP(s, i, time, open, high, low, close, volume, tick_volume, rates_total);
         }
      }

      int first = -1;
      for(int s = 0; s < NUM_SESSIONS; s++) { if(IsEnabled(s)) { first = s; break; } }
      if(first >= 0)
      {
         POCBuf[i] = sPOC[first]; VAHBuf[i] = sVAH[first]; VALBuf[i] = sVAL[first];
         IBHBuf[i] = (sIBH[first] == 0) ? EMPTY_VALUE : sIBH[first]; IBLBuf[i] = (sIBL[first] == 0) ? EMPTY_VALUE : sIBL[first];
         HVN1Buf[i] = (sHVN[first*5]==0)?EMPTY_VALUE:sHVN[first*5]; HVN2Buf[i] = (sHVN[first*5+1]==0)?EMPTY_VALUE:sHVN[first*5+1]; HVN3Buf[i] = (sHVN[first*5+2]==0)?EMPTY_VALUE:sHVN[first*5+2];
         HVN4Buf[i] = (sHVN[first*5+3]==0)?EMPTY_VALUE:sHVN[first*5+3]; HVN5Buf[i] = (sHVN[first*5+4]==0)?EMPTY_VALUE:sHVN[first*5+4];
         LVN1Buf[i] = (sLVN[first*5]==0)?EMPTY_VALUE:sLVN[first*5]; LVN2Buf[i] = (sLVN[first*5+1]==0)?EMPTY_VALUE:sLVN[first*5+1]; LVN3Buf[i] = (sLVN[first*5+2]==0)?EMPTY_VALUE:sLVN[first*5+2];
         LVN4Buf[i] = (sLVN[first*5+3]==0)?EMPTY_VALUE:sLVN[first*5+3]; LVN5Buf[i] = (sLVN[first*5+4]==0)?EMPTY_VALUE:sLVN[first*5+4];
         VPOCBuf[i] = sVPOC[first]; VVAHBuf[i] = sVVAH[first]; VVALBuf[i] = sVVAL[first];
         BullPOCBuf[i] = sBPOC[first]; BearPOCBuf[i] = sEPOC[first];
         TotalVolBuf[i] = sTV[first]; TotalBullVolBuf[i] = sBV[first]; TotalBearVolBuf[i] = sEV[first];
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
void CalcVP(int s, int currentBar, const datetime &time[],
            const double &open[], const double &high[], const double &low[],
            const double &close[], const long &volume[], const long &tick_volume[],
            int totalBars)
{
   double sessionHigh = -DBL_MAX, sessionLow = DBL_MAX;
   int barsIn = 0;
   datetime ibEnd = prvSS[s] + GetIBPeriodSeconds();
   double ibH = -DBL_MAX, ibL = DBL_MAX;

   for(int i = currentBar; i < totalBars; i++)
   {
      datetime bt = time[i];
      if(bt < prvSS[s]) break;
      if(bt >= prvSS[s] && bt < prvSE[s])
      {
         if(IsSunday(bt)) continue;
         double hi = high[i], lo = low[i];
         if(hi > sessionHigh) sessionHigh = hi;
         if(lo < sessionLow) sessionLow = lo;
         barsIn++;
         if(bt < ibEnd) { if(hi > ibH) ibH = hi; if(lo < ibL) ibL = lo; }
      }
   }
   if(barsIn == 0 || sessionHigh <= sessionLow) return;
   if(ibH > ibL) { sIBH[s] = ibH; sIBL[s] = ibL; }

   // Store for display
   lastSesHigh[s] = sessionHigh;
   lastSesLow[s]  = sessionLow;

   double range = sessionHigh - sessionLow;
   int numBins; double binSize;
   if(InpBinSizeMode == 2)
   { binSize = InpTicksPerBin * _Point; numBins = (int)MathCeil(range / binSize); numBins = (int)MathMax(1, MathMin(numBins, MAX_BINS)); }
   else { numBins = ExtProfileRows; binSize = range / numBins; }

   for(int j = 0; j < numBins; j++) { volBins[j] = 0; bullBins[j] = 0; bearBins[j] = 0; }

   double tv = 0, bv = 0, ev = 0;
   for(int i = currentBar; i < totalBars; i++)
   {
      datetime bt = time[i];
      if(bt < prvSS[s]) break;
      if(bt >= prvSS[s] && bt < prvSE[s])
      {
         if(IsSunday(bt)) continue;
         double cl = close[i], op = open[i], hi = high[i], lo = low[i];
         long v = volume[i] > 0 ? volume[i] : tick_volume[i];
         double bvol = (double)v;
         int bhi = (int)MathMax(0, MathMin(numBins-1, (int)((hi-sessionLow)/binSize)));
         int blo = (int)MathMax(0, MathMin(numBins-1, (int)((lo-sessionLow)/binSize)));
         double pb = bvol / (double)(bhi - blo + 1);
         bool isBull = (cl >= op);
         for(int b = blo; b <= bhi; b++)
         { volBins[b] += pb; if(isBull) bullBins[b] += pb; else bearBins[b] += pb; }
         tv += bvol; if(isBull) bv += bvol; else ev += bvol;
      }
   }
   if(tv == 0) return;

   int pocIdx = 0; double maxV = volBins[0];
   for(int j = 1; j < numBins; j++) if(volBins[j] > maxV) { maxV = volBins[j]; pocIdx = j; }
   sPOC[s] = sessionLow + (pocIdx + 0.5) * binSize;

   // Value Area
   double target = tv * (ExtValueAreaPct / 100.0);
   double acc = volBins[pocIdx]; int up = pocIdx, dn = pocIdx;
   while(acc < target)
   {
      bool canUp = (up+1) < numBins, canDn = (dn-1) >= 0;
      if(!canUp && !canDn) break;
      double vUp = canUp ? volBins[up+1] : -1, vDn = canDn ? volBins[dn-1] : -1;
      if(vUp >= vDn) { up++; acc += volBins[up]; } else { dn--; acc += volBins[dn]; }
   }
   sVAL[s] = sessionLow + dn * binSize;
   sVAH[s] = sessionLow + (up+1) * binSize;

   // HVN
   for(int h = 0; h < 5; h++) sHVN[s*5+h] = 0;
   double mxV2 = 0; for(int j = 0; j < numBins; j++) if(volBins[j] > mxV2) mxV2 = volBins[j];
   if(mxV2 > 0)
   {
      double thr = mxV2 * InpHvnThresholdPct / 100.0;
      int cIdx[]; double cVol[];
      ArrayResize(cIdx, numBins); ArrayResize(cVol, numBins);
      int cc = 0;
      for(int j = 0; j < numBins; j++)
      {
         if(j == pocIdx || volBins[j] < thr) continue;
         double lt = (j>0) ? volBins[j-1] : -1, rt = (j<numBins-1) ? volBins[j+1] : -1;
         if(volBins[j] > lt && volBins[j] > rt) { cIdx[cc] = j; cVol[cc] = volBins[j]; cc++; }
      }
      for(int a = 0; a < cc-1; a++) { int best = a; for(int b = a+1; b < cc; b++) if(cVol[b] > cVol[best]) best = b;
         if(best!=a) { double tv2=cVol[a]; cVol[a]=cVol[best]; cVol[best]=tv2; int ti=cIdx[a]; cIdx[a]=cIdx[best]; cIdx[best]=ti; } }
      int mx = (int)MathMin(InpHvnCount, 5), ac2 = 0;
      for(int c = 0; c < cc && ac2 < mx; c++) { sHVN[s*5+ac2] = sessionLow + (cIdx[c]+0.5)*binSize; ac2++; }
   }

   // LVN
   for(int h = 0; h < 5; h++) sLVN[s*5+h] = 0;
   if(InpEnableLVN && mxV2 > 0)
   {
      double thr2 = mxV2 * InpLvnThresholdPct / 100.0;
      int lIdx[]; double lVol[];
      ArrayResize(lIdx, numBins); ArrayResize(lVol, numBins);
      int lc = 0;
      for(int j = 1; j < numBins-1; j++)
      {
         if(volBins[j] > thr2) continue;
         if(volBins[j] < volBins[j-1] && volBins[j] < volBins[j+1])
         { lIdx[lc] = j; lVol[lc] = volBins[j]; lc++; }
      }
      for(int a = 0; a < lc-1; a++) { int best = a; for(int b = a+1; b < lc; b++) if(lVol[b] < lVol[best]) best = b;
         if(best!=a) { double tv2=lVol[a]; lVol[a]=lVol[best]; lVol[best]=tv2; int ti=lIdx[a]; lIdx[a]=lIdx[best]; lIdx[best]=ti; } }
      int cnt = (int)MathMin((int)MathMin(InpHvnCount,5), lc);
      for(int n = 0; n < cnt; n++) sLVN[s*5+n] = sessionLow + (lIdx[n]+0.5)*binSize;
   }

   // VCP
   if(InpEnableVCP)
   {
      double avg2 = tv / MathMax(1, numBins);
      int pIdx[]; double pVal[];
      ArrayResize(pIdx, numBins); ArrayResize(pVal, numBins);
      int pc = 0;
      for(int j = 0; j < numBins; j++)
      { bool lok = (j==0)||(volBins[j]>=volBins[j-1]); bool rok = (j==numBins-1)||(volBins[j]>=volBins[j+1]);
        if(lok && rok && volBins[j] > avg2) { pIdx[pc] = j; pVal[pc] = volBins[j]; pc++; } }
      int mc = (int)MathMin(InpMaxClusterCenters, pc);
      if(mc > 0)
      {
         for(int a = 0; a < mc; a++) { int best = a; for(int b = a+1; b < pc; b++) if(pVal[b] > pVal[best]) best = b;
            if(best!=a) { int ti=pIdx[a]; pIdx[a]=pIdx[best]; pIdx[best]=ti; double tv2=pVal[a]; pVal[a]=pVal[best]; pVal[best]=tv2; } }
         for(int j = 0; j < numBins; j++) clustBins[j] = 0;
         for(int c = 0; c < mc; c++)
         { int cn = pIdx[c]; double cv2 = pVal[c];
           for(int j = 0; j < numBins; j++) { double d = (j-cn)/InpClusterSpread; clustBins[j] += cv2 * MathExp(-0.5*d*d); } }
         int vi = 0; double vm = clustBins[0];
         for(int j = 1; j < numBins; j++) if(clustBins[j] > vm) { vm = clustBins[j]; vi = j; }
         sVPOC[s] = sessionLow + (vi+0.5)*binSize;
         double ct2 = 0; for(int j = 0; j < numBins; j++) ct2 += clustBins[j];
         double tg = ct2 * (ExtValueAreaPct/100.0); double ac3 = clustBins[vi];
         int l2 = vi, h2 = vi;
         while(ac3 < tg && (l2 > 0 || h2 < numBins-1))
         { double el = (l2>0)?clustBins[l2-1]:0; double eh = (h2<numBins-1)?clustBins[h2+1]:0;
           if(el >= eh && l2 > 0) { l2--; ac3 += clustBins[l2]; } else if(h2<numBins-1) { h2++; ac3 += clustBins[h2]; } else { l2--; ac3 += clustBins[l2]; } }
         sVVAL[s] = sessionLow + l2 * binSize;
         sVVAH[s] = sessionLow + (h2+1) * binSize;
      }
      else { sVPOC[s] = sPOC[s]; sVVAH[s] = sVAH[s]; sVVAL[s] = sVAL[s]; }
   }
   else { sVPOC[s] = sPOC[s]; sVVAH[s] = sVAH[s]; sVVAL[s] = sVAL[s]; }

   // Bull/Bear POC
   int bpi = 0; double bpm = bullBins[0]; int epi = 0; double epm = bearBins[0];
   for(int j = 1; j < numBins; j++)
   { if(bullBins[j] > bpm) { bpm = bullBins[j]; bpi = j; }
     if(bearBins[j] > epm) { epm = bearBins[j]; epi = j; } }
   sBPOC[s] = (bpm > 0) ? sessionLow + (bpi+0.5)*binSize : 0;
   sEPOC[s] = (epm > 0) ? sessionLow + (epi+0.5)*binSize : 0;
   sTV[s] = tv; sBV[s] = bv; sEV[s] = ev;

   // Compute lastSesNB for display
   lastSesNB[s] = numBins;

   //--- Push to visual display ---
   if(InpShowProfile)
   {
      double dummyLVN[5];
      for(int h2 = 0; h2 < 5; h2++) dummyLVN[h2] = sLVN[s*5+h2];
      double dummyHVN[5];
      for(int h2 = 0; h2 < 5; h2++) dummyHVN[h2] = sHVN[s*5+h2];
      if(InpEnableVCP)
         VPDisplayPushSession(
            prvSS[s], prvSE[s],
            lastSesHigh[s], lastSesLow[s],
            sPOC[s], sVAH[s], sVAL[s],
            sIBH[s], sIBL[s],
            dummyHVN, dummyLVN,
            numBins,
            clustBins, bullBins, bearBins,
            InpMaxDisplaySess);
      else
         VPDisplayPushSession(
            prvSS[s], prvSE[s],
            lastSesHigh[s], lastSesLow[s],
            sPOC[s], sVAH[s], sVAL[s],
            sIBH[s], sIBL[s],
            dummyHVN, dummyLVN,
            numBins,
            volBins, bullBins, bearBins,
            InpMaxDisplaySess);
      // Set session name for stats display
      string sesNames[4];
      sesNames[0] = "London"; sesNames[1] = "New York";
      sesNames[2] = "Sydney"; sesNames[3] = "Tokyo";
      for(int si = g_vpSesCount - 1; si >= MathMax(0, g_vpSesCount - 8); si--)
      {
         if(g_vpSessions[si].sesStart == prvSS[s])
         { g_vpSessions[si].sessionName = sesNames[s]; break; }
      }
   }
}
//+------------------------------------------------------------------+
