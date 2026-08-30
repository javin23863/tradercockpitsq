//+------------------------------------------------------------------+
//|                                              SqVPDisplay.mqh     |
//|                           Copyright © 2026, StrategyQuant s.r.o. |
//|                                     http://www.strategyquant.com |
//+------------------------------------------------------------------+
//| MQ4 Canvas-based display for Volume Profile / TPO indicators.    |
//| Renders all sessions to a single OBJ_BITMAP_LABEL via            |
//| ResourceCreate() for maximum performance.                        |
//+------------------------------------------------------------------+
#ifndef SQ_VP_DISPLAY_MQH
#define SQ_VP_DISPLAY_MQH

#define VP_MAX_HIST 50

struct VPDisplaySession
{
   datetime sesStart;
   datetime sesEnd;
   double   sessionHigh;
   double   sessionLow;
   double   pocPrice;
   double   vahPrice;
   double   valPrice;
   double   ibhPrice;
   double   iblPrice;
   double   hvn[5];
   double   lvn[5];
   int      numBins;
   double   volumeBins[];
   double   bullBins[];
   double   bearBins[];
   int      tpoBins[];
   bool     isTPO;
   double   totalVolume;
   double   totalBullVolume;
   double   totalBearVolume;
   string   sessionName;
};

string g_vpPrefix = "";
bool   g_vpDisplayEnabled = true;
bool   g_vpShowStats = false;
int    g_vpSesCount = 0;
VPDisplaySession g_vpSessions[];

string g_vpResName = "";
string g_vpBmpName = "";
bool   g_vpDirty   = true;
bool   g_vpRendering = false;
bool   g_vpForceRender = false;
int    g_vpMaxDisplay = 10;
uint   g_vpPixels[];
int    g_vpLastW = 0;
int    g_vpLastH = 0;

//+------------------------------------------------------------------+
void VPDisplayInit(string prefix, int maxSessions)
{
   g_vpPrefix = prefix;
   g_vpDisplayEnabled = false;
   g_vpSesCount = 0;
   g_vpMaxDisplay = maxSessions;
   ArrayResize(g_vpSessions, maxSessions);

   g_vpResName = "::VP_" + prefix + "cvs";
   g_vpBmpName = prefix + "Canvas";

   ObjectCreate(g_vpBmpName, OBJ_BITMAP_LABEL, 0, 0, 0);
   ObjectSet(g_vpBmpName, OBJPROP_CORNER, CORNER_LEFT_UPPER);
   ObjectSet(g_vpBmpName, OBJPROP_XDISTANCE, 0);
   ObjectSet(g_vpBmpName, OBJPROP_YDISTANCE, 0);
   ObjectSet(g_vpBmpName, OBJPROP_BACK, true);
   ObjectSetInteger(0, g_vpBmpName, OBJPROP_SELECTABLE, false);
   ObjectSetInteger(0, g_vpBmpName, OBJPROP_HIDDEN, true);
   ObjectSetInteger(0, g_vpBmpName, OBJPROP_ZORDER, 0);

   VPCreateToggleButton();

   g_vpDirty = true;
   g_vpLastW = 0;
   g_vpLastH = 0;
}

//+------------------------------------------------------------------+
void VPDisplayDeinit()
{
   ResourceFree(g_vpResName);
   ObjectDelete(g_vpBmpName);

   int total = ObjectsTotal();
   for(int i = total - 1; i >= 0; i--)
   {
      string name = ObjectName(i);
      if(StringFind(name, g_vpPrefix) == 0)
         ObjectDelete(name);
   }
   g_vpSesCount = 0;
}

//+------------------------------------------------------------------+
void VPDisplayOnChartEvent(const int id, const long &lparam, const double &dparam, const string &sparam)
{
   if(id == CHARTEVENT_CHART_CHANGE)
   {
      static int    s_prevFirstBar = -1;
      static int    s_prevVisBars  = -1;
      static int    s_prevW        = -1;
      static int    s_prevH        = -1;
      static double s_prevPrMin    = 0;
      static double s_prevPrMax    = 0;
      static long   s_prevBgClr    = -1;

      int    fb  = (int)ChartGetInteger(0, CHART_FIRST_VISIBLE_BAR);
      int    vb  = (int)ChartGetInteger(0, CHART_VISIBLE_BARS);
      int    cw  = (int)ChartGetInteger(0, CHART_WIDTH_IN_PIXELS);
      int    ch  = (int)ChartGetInteger(0, CHART_HEIGHT_IN_PIXELS);
      double pmn = ChartGetDouble(0, CHART_PRICE_MIN);
      double pmx = ChartGetDouble(0, CHART_PRICE_MAX);
      long   bgc = ChartGetInteger(0, CHART_COLOR_BACKGROUND);

      if(g_vpForceRender || fb != s_prevFirstBar || vb != s_prevVisBars ||
         cw != s_prevW || ch != s_prevH ||
         pmn != s_prevPrMin || pmx != s_prevPrMax ||
         bgc != s_prevBgClr)
      {
         g_vpForceRender = false;
         s_prevFirstBar = fb;
         s_prevVisBars  = vb;
         s_prevW        = cw;
         s_prevH        = ch;
         s_prevPrMin    = pmn;
         s_prevPrMax    = pmx;
         s_prevBgClr    = bgc;

         if(g_vpDisplayEnabled && g_vpSesCount > 0)
            VPDisplayRender();
         else if(g_vpSesCount > 0)
            VPUpdateLabels();
      }
   }

   if(id == CHARTEVENT_OBJECT_CLICK)
   {
      if(sparam == g_vpPrefix + "ToggleBtn")
      {
         g_vpDisplayEnabled = !g_vpDisplayEnabled;

         string btnName = g_vpPrefix + "ToggleBtn";
         ObjectSetText(btnName, "VP");
         ObjectSet(btnName, OBJPROP_BGCOLOR, g_vpDisplayEnabled ? C'40,80,40' : C'80,40,40');

         if(g_vpDisplayEnabled)
         {
            ObjectSet(g_vpBmpName, OBJPROP_TIMEFRAMES, OBJ_ALL_PERIODS);
            if(g_vpSesCount > 0) VPDisplayRender();
         }
         else
         {
            ObjectSet(g_vpBmpName, OBJPROP_TIMEFRAMES, OBJ_NO_PERIODS);
            VPDeleteAllLabels();
         }
         ChartRedraw(0);
      }
   }
}

//+------------------------------------------------------------------+
void VPDisplayPushSession(
   datetime sesStart, datetime sesEnd,
   double sessionHigh, double sessionLow,
   double poc, double vah, double val,
   double ibh, double ibl,
   const double &hvn[], const double &lvn[],
   int numBins,
   const double &volBins[], const double &bullBinsArr[], const double &bearBinsArr[],
   int maxDisplay)
{
   int i;
   for(i = g_vpSesCount - 1; i >= MathMax(0, g_vpSesCount - 8); i--)
   {
      if(g_vpSessions[i].sesStart == sesStart)
      {
         FillSessionData(g_vpSessions[i], sesStart, sesEnd, sessionHigh, sessionLow,
                         poc, vah, val, ibh, ibl, hvn, lvn, numBins,
                         volBins, bullBinsArr, bearBinsArr, false);
         g_vpDirty = true;
         VPThrottledRender();
         return;
      }
   }

   int maxSes = ArraySize(g_vpSessions);
   if(g_vpSesCount >= maxSes)
   {
      for(i = 0; i < g_vpSesCount - 1; i++)
         CopySession(g_vpSessions[i], g_vpSessions[i + 1]);
      g_vpSesCount--;
   }

   int idx = g_vpSesCount;
   FillSessionData(g_vpSessions[idx], sesStart, sesEnd, sessionHigh, sessionLow,
                   poc, vah, val, ibh, ibl, hvn, lvn, numBins,
                   volBins, bullBinsArr, bearBinsArr, false);
   g_vpSesCount++;

   g_vpDirty = true;
   VPThrottledRender();
}

//+------------------------------------------------------------------+
void VPDisplayPushTPOSession(
   datetime sesStart, datetime sesEnd,
   double sessionHigh, double sessionLow,
   double poc, double vah, double val,
   double ibh, double ibl,
   int numBins,
   const int &tpoBinsArr[],
   int maxDisplay)
{
   int i;
   for(i = g_vpSesCount - 1; i >= MathMax(0, g_vpSesCount - 8); i--)
   {
      if(g_vpSessions[i].sesStart == sesStart)
      {
         FillTPOSessionData(g_vpSessions[i], sesStart, sesEnd, sessionHigh, sessionLow,
                            poc, vah, val, ibh, ibl, numBins, tpoBinsArr);
         g_vpDirty = true;
         VPThrottledRender();
         return;
      }
   }

   int maxSes = ArraySize(g_vpSessions);
   if(g_vpSesCount >= maxSes)
   {
      for(i = 0; i < g_vpSesCount - 1; i++)
         CopySession(g_vpSessions[i], g_vpSessions[i + 1]);
      g_vpSesCount--;
   }

   int idx = g_vpSesCount;
   FillTPOSessionData(g_vpSessions[idx], sesStart, sesEnd, sessionHigh, sessionLow,
                      poc, vah, val, ibh, ibl, numBins, tpoBinsArr);
   g_vpSesCount++;

   g_vpDirty = true;
   VPThrottledRender();
}

//+------------------------------------------------------------------+
void FillSessionData(VPDisplaySession &ses,
                     datetime sesStart, datetime sesEnd,
                     double sessionHigh, double sessionLow,
                     double poc, double vah, double val,
                     double ibh, double ibl,
                     const double &hvn[], const double &lvn[],
                     int numBins,
                     const double &volBins[], const double &bullBinsArr[], const double &bearBinsArr[],
                     bool isTPO)
{
   ses.sesStart    = sesStart;
   ses.sesEnd      = sesEnd;
   ses.sessionHigh = sessionHigh;
   ses.sessionLow  = sessionLow;
   ses.pocPrice    = poc;
   ses.vahPrice    = vah;
   ses.valPrice    = val;
   ses.ibhPrice    = ibh;
   ses.iblPrice    = ibl;
   ses.numBins     = numBins;
   ses.isTPO       = isTPO;

   for(int n = 0; n < 5; n++)
   {
      ses.hvn[n] = hvn[n];
      ses.lvn[n] = lvn[n];
   }

   ArrayResize(ses.volumeBins, numBins);
   ArrayResize(ses.bullBins, numBins);
   ArrayResize(ses.bearBins, numBins);
   ses.totalVolume = 0;
   ses.totalBullVolume = 0;
   ses.totalBearVolume = 0;
   for(int j = 0; j < numBins; j++)
   {
      ses.volumeBins[j] = volBins[j];
      ses.bullBins[j]   = bullBinsArr[j];
      ses.bearBins[j]   = bearBinsArr[j];
      ses.totalVolume     += volBins[j];
      ses.totalBullVolume += bullBinsArr[j];
      ses.totalBearVolume += bearBinsArr[j];
   }
}

//+------------------------------------------------------------------+
void CopySession(VPDisplaySession &dst, VPDisplaySession &src)
{
   dst.sesStart    = src.sesStart;
   dst.sesEnd      = src.sesEnd;
   dst.sessionHigh = src.sessionHigh;
   dst.sessionLow  = src.sessionLow;
   dst.pocPrice    = src.pocPrice;
   dst.vahPrice    = src.vahPrice;
   dst.valPrice    = src.valPrice;
   dst.ibhPrice    = src.ibhPrice;
   dst.iblPrice    = src.iblPrice;
   dst.numBins     = src.numBins;
   dst.isTPO       = src.isTPO;
   dst.totalVolume     = src.totalVolume;
   dst.totalBullVolume = src.totalBullVolume;
   dst.totalBearVolume = src.totalBearVolume;
   for(int n = 0; n < 5; n++)
   {
      dst.hvn[n] = src.hvn[n];
      dst.lvn[n] = src.lvn[n];
   }
   int nb = src.numBins;
   ArrayResize(dst.volumeBins, nb);
   ArrayResize(dst.bullBins, nb);
   ArrayResize(dst.bearBins, nb);
   ArrayResize(dst.tpoBins, nb);
   for(int j = 0; j < nb; j++)
   {
      if(j < ArraySize(src.volumeBins)) dst.volumeBins[j] = src.volumeBins[j]; else dst.volumeBins[j] = 0;
      if(j < ArraySize(src.bullBins))   dst.bullBins[j]   = src.bullBins[j];   else dst.bullBins[j] = 0;
      if(j < ArraySize(src.bearBins))   dst.bearBins[j]   = src.bearBins[j];   else dst.bearBins[j] = 0;
      if(j < ArraySize(src.tpoBins))    dst.tpoBins[j]    = src.tpoBins[j];    else dst.tpoBins[j] = 0;
   }
}

//+------------------------------------------------------------------+
void FillTPOSessionData(VPDisplaySession &ses,
                        datetime sesStart, datetime sesEnd,
                        double sessionHigh, double sessionLow,
                        double poc, double vah, double val,
                        double ibh, double ibl,
                        int numBins, const int &tpoBinsArr[])
{
   ses.sesStart    = sesStart;
   ses.sesEnd      = sesEnd;
   ses.sessionHigh = sessionHigh;
   ses.sessionLow  = sessionLow;
   ses.pocPrice    = poc;
   ses.vahPrice    = vah;
   ses.valPrice    = val;
   ses.ibhPrice    = ibh;
   ses.iblPrice    = ibl;
   ses.numBins     = numBins;
   ses.isTPO       = true;
   ses.totalVolume     = 0;
   ses.totalBullVolume = 0;
   ses.totalBearVolume = 0;

   for(int n = 0; n < 5; n++)
   {
      ses.hvn[n] = 0;
      ses.lvn[n] = 0;
   }

   ArrayResize(ses.tpoBins, numBins);
   for(int j = 0; j < numBins; j++)
      ses.tpoBins[j] = tpoBinsArr[j];
}

//+------------------------------------------------------------------+
void VPThrottledRender()
{
   static uint s_lastRender = 0;
   uint now = GetTickCount();
   if(now - s_lastRender >= 500 || now < s_lastRender)
   {
      if(g_vpDisplayEnabled)
         VPDisplayRender();
      else if(g_vpSesCount > 0)
         VPUpdateLabels();
      s_lastRender = now;
   }
}

//+------------------------------------------------------------------+
void VPDisplayRender()
{
   if(g_vpRendering) return;
   g_vpRendering = true;
   g_vpDirty = false;

   int chartW = (int)ChartGetInteger(0, CHART_WIDTH_IN_PIXELS);
   int chartH = (int)ChartGetInteger(0, CHART_HEIGHT_IN_PIXELS);
   if(chartW <= 0 || chartH <= 0) { g_vpRendering = false; return; }

   int totalPx = chartW * chartH;
   if(chartW != g_vpLastW || chartH != g_vpLastH)
   {
      ArrayResize(g_vpPixels, totalPx);
      g_vpLastW = chartW;
      g_vpLastH = chartH;
   }
   ArrayInitialize(g_vpPixels, 0x00000000);

   VPDeleteAllLabels();

   int startSes = MathMax(0, g_vpSesCount - g_vpMaxDisplay);

   // Pass 1: VA shading
   int s;
   for(s = startSes; s < g_vpSesCount; s++)
   {
      if(g_vpSessions[s].numBins <= 0) continue;
      double vah = g_vpSessions[s].vahPrice;
      double valP = g_vpSessions[s].valPrice;
      if(vah <= valP) continue;

      int x1s, y1s, x2s, y2s;
      if(!ChartTimePriceToXY(0, 0, g_vpSessions[s].sesStart, vah, x1s, y1s)) continue;
      if(!ChartTimePriceToXY(0, 0, g_vpSessions[s].sesEnd,   valP, x2s, y2s)) continue;
      VPFillRect(g_vpPixels, chartW, chartH, x1s, y1s, x2s, y2s, 0x25FF9800);
   }

   // Pass 2: Session boundaries
   for(s = startSes; s < g_vpSesCount; s++)
   {
      if(g_vpSessions[s].numBins <= 0) continue;
      int bx, by;
      if(ChartTimePriceToXY(0, 0, g_vpSessions[s].sesStart, g_vpSessions[s].sessionHigh, bx, by))
         VPDrawDottedVLine(g_vpPixels, chartW, chartH, bx, 0x50555555);
   }

   // Pass 3: Histogram bars
   for(s = startSes; s < g_vpSesCount; s++)
   {
      if(g_vpSessions[s].isTPO)
         VPRenderTPOBars(s, chartW, chartH);
      else
         VPRenderVPBars(s, chartW, chartH);
   }

   // Pass 4: IB boxes
   for(s = startSes; s < g_vpSesCount; s++)
   {
      double ibh = g_vpSessions[s].ibhPrice;
      double ibl = g_vpSessions[s].iblPrice;
      if(ibh <= 0 || ibl <= 0 || ibh <= ibl) continue;

      int ix1, iy1, ix2, iy2;
      if(!ChartTimePriceToXY(0, 0, g_vpSessions[s].sesStart, ibh, ix1, iy1)) continue;
      if(!ChartTimePriceToXY(0, 0, g_vpSessions[s].sesEnd,   ibl, ix2, iy2)) continue;
      VPDrawDashedRect(g_vpPixels, chartW, chartH, ix1, iy1, ix2, iy2, 0xB000BCD4);
   }

   // Pass 5: Level labels
   VPUpdateLabels();

   if(!ResourceCreate(g_vpResName, g_vpPixels, (uint)chartW, (uint)chartH,
                       0, 0, (uint)chartW, COLOR_FORMAT_ARGB_NORMALIZE))
   {
      g_vpRendering = false;
      return;
   }

   ObjectSetString(0, g_vpBmpName, OBJPROP_BMPFILE, g_vpResName);
   ObjectSetInteger(0, g_vpBmpName, OBJPROP_XSIZE, chartW);
   ObjectSetInteger(0, g_vpBmpName, OBJPROP_YSIZE, chartH);
   ObjectSet(g_vpBmpName, OBJPROP_TIMEFRAMES, OBJ_ALL_PERIODS);

   g_vpRendering = false;
}

//+------------------------------------------------------------------+
void VPUpdateLabels()
{
   VPDeleteAllLabels();
   int startSes = MathMax(0, g_vpSesCount - g_vpMaxDisplay);

   int s;
   for(s = startSes; s < g_vpSesCount; s++)
   {
      if(g_vpSessions[s].numBins <= 0) continue;
      string sk = IntegerToString(s);
      datetime lt = g_vpSessions[s].sesEnd;

      string sn = (g_vpSessions[s].sessionName != "") ? g_vpSessions[s].sessionName + " " : "";

      if(g_vpSessions[s].pocPrice > 0)
         VPCreateLabel(g_vpPrefix + "LP_" + sk, lt, g_vpSessions[s].pocPrice, sn + "POC", C'255,165,0');
      if(g_vpSessions[s].vahPrice > 0)
         VPCreateLabel(g_vpPrefix + "LV_" + sk, lt, g_vpSessions[s].vahPrice, sn + "VAH", C'76,175,80');
      if(g_vpSessions[s].valPrice > 0)
         VPCreateLabel(g_vpPrefix + "LL_" + sk, lt, g_vpSessions[s].valPrice, sn + "VAL", C'76,175,80');
      if(g_vpSessions[s].ibhPrice > 0)
         VPCreateLabel(g_vpPrefix + "LI_" + sk, lt, g_vpSessions[s].ibhPrice, sn + "IBH", C'30,144,255');
      if(g_vpSessions[s].iblPrice > 0)
         VPCreateLabel(g_vpPrefix + "LJ_" + sk, lt, g_vpSessions[s].iblPrice, sn + "IBL", C'30,144,255');

      // --- Session stats panel (only when stats enabled AND VP visible) ---
      if(g_vpShowStats && g_vpDisplayEnabled && !g_vpSessions[s].isTPO)
      {
         double sHigh = g_vpSessions[s].sessionHigh;
         double sLow  = g_vpSessions[s].sessionLow;
         double range = sHigh - sLow;
         double tv    = g_vpSessions[s].totalVolume;
         double bv    = g_vpSessions[s].totalBullVolume;
         double sv    = g_vpSessions[s].totalBearVolume;
         double delta = bv - sv;
         double poc   = g_vpSessions[s].pocPrice;
         double vah   = g_vpSessions[s].vahPrice;
         double valP  = g_vpSessions[s].valPrice;

         double pocFromHigh = (range > 0) ? ((sHigh - poc) / range * 100.0) : 0;
         double pocFromLow  = (range > 0) ? ((poc - sLow)  / range * 100.0) : 0;

         double dPOC = 0, dVA = 0;
         if(s > startSes && g_vpSessions[s-1].pocPrice > 0)
         {
            dPOC = poc - g_vpSessions[s-1].pocPrice;
            double curVAMid  = (vah + valP) * 0.5;
            double prevVAMid = (g_vpSessions[s-1].vahPrice + g_vpSessions[s-1].valPrice) * 0.5;
            if(prevVAMid > 0) dVA = curVAMid - prevVAMid;
         }

         double prMin = ChartGetDouble(0, CHART_PRICE_MIN);
         double prMax = ChartGetDouble(0, CHART_PRICE_MAX);
         double lineStep = (prMax - prMin) * 0.018;
         double topY = prMax - lineStep * 1.5;

         color bgClr = (color)ChartGetInteger(0, CHART_COLOR_BACKGROUND);
         int brightness = ((bgClr & 0xFF) + ((bgClr >> 8) & 0xFF) + ((bgClr >> 16) & 0xFF)) / 3;
         color statClr = (brightness < 128) ? clrWhite : clrBlack;
         color bullClr = C'76,175,80';
         color bearClr = C'239,83,80';

         // Use sesStart for X-position (where VP histogram anchors)
         datetime statsT = g_vpSessions[s].sesStart;
         int row = 0;

         // Show session name if available (e.g. London, New York, etc.)
         if(g_vpSessions[s].sessionName != "")
         {
            VPCreateLabel(g_vpPrefix + "LSN_" + sk, statsT, topY - lineStep * row,
                          g_vpSessions[s].sessionName, statClr);
            row++;
         }

         VPCreateLabel(g_vpPrefix + "LS0_" + sk, statsT, topY - lineStep * row,
                       "Vol: " + VPFormatVolume(tv), statClr);
         VPCreateLabel(g_vpPrefix + "LS1_" + sk, statsT, topY - lineStep * (row+1),
                       "Bull: " + VPFormatVolume(bv), bullClr);
         VPCreateLabel(g_vpPrefix + "LS2_" + sk, statsT, topY - lineStep * (row+2),
                       "Bear: " + VPFormatVolume(sv), bearClr);
         VPCreateLabel(g_vpPrefix + "LS3_" + sk, statsT, topY - lineStep * (row+3),
                       "Delta: " + (delta >= 0 ? "+" : "") + VPFormatVolume(delta), 
                       delta >= 0 ? bullClr : bearClr);
         VPCreateLabel(g_vpPrefix + "LS4_" + sk, statsT, topY - lineStep * (row+4),
                       "Delta POC: " + DoubleToString(dPOC, _Digits), dPOC >= 0 ? bullClr : bearClr);
         VPCreateLabel(g_vpPrefix + "LS5_" + sk, statsT, topY - lineStep * (row+5),
                       "Delta VA: " + DoubleToString(dVA, _Digits), dVA >= 0 ? bullClr : bearClr);
         VPCreateLabel(g_vpPrefix + "LS6_" + sk, statsT, topY - lineStep * (row+6),
                       "Range: " + DoubleToString(range, _Digits), statClr);
         VPCreateLabel(g_vpPrefix + "LS7_" + sk, statsT, topY - lineStep * (row+7),
                       "POC%: " + DoubleToString(pocFromHigh, 1) + "%", statClr);
         VPCreateLabel(g_vpPrefix + "LS8_" + sk, statsT, topY - lineStep * (row+8),
                       "POC%: " + DoubleToString(pocFromLow, 1) + "%", statClr);
      }

      if(!g_vpSessions[s].isTPO)
      {
         int n;
         for(n = 0; n < 5; n++)
         {
            if(g_vpSessions[s].hvn[n] > 0)
               VPCreateLabel(g_vpPrefix + "LH" + IntegerToString(n) + "_" + sk,
                             lt, g_vpSessions[s].hvn[n], "HVN" + IntegerToString(n+1), C'156,39,176');
         }
         for(n = 0; n < 5; n++)
         {
            if(g_vpSessions[s].lvn[n] > 0)
               VPCreateLabel(g_vpPrefix + "LN" + IntegerToString(n) + "_" + sk,
                             lt, g_vpSessions[s].lvn[n], "LVN" + IntegerToString(n+1), C'0,150,136');
         }
      }
   }
}

//+------------------------------------------------------------------+
void VPRenderVPBars(int idx, int cW, int cH)
{
   int nBins = g_vpSessions[idx].numBins;
   if(nBins <= 0) return;
   double sH = g_vpSessions[idx].sessionHigh;
   double sL = g_vpSessions[idx].sessionLow;
   double range = sH - sL;
   if(range <= 0) return;

   double maxVol = 0;
   int pocIdx = 0;
   int j;
   for(j = 0; j < nBins; j++)
   {
      if(g_vpSessions[idx].volumeBins[j] > maxVol)
      {
         maxVol = g_vpSessions[idx].volumeBins[j];
         pocIdx = j;
      }
   }
   if(maxVol <= 0) return;

   int sesX1, sesX2, tmpY;
   if(!ChartTimePriceToXY(0, 0, g_vpSessions[idx].sesStart, sH, sesX1, tmpY)) return;
   if(!ChartTimePriceToXY(0, 0, g_vpSessions[idx].sesEnd,   sH, sesX2, tmpY)) return;
   int sesPxW = sesX2 - sesX1;
   if(sesPxW <= 0) return;

   int topY, botY;
   ChartTimePriceToXY(0, 0, g_vpSessions[idx].sesStart, sH, tmpY, topY);
   ChartTimePriceToXY(0, 0, g_vpSessions[idx].sesStart, sL, tmpY, botY);
   double pxPerPrice = (botY != topY) ? (double)(botY - topY) / range : 1.0;

   double binSize = range / nBins;
   double valP = g_vpSessions[idx].valPrice;
   double vah  = g_vpSessions[idx].vahPrice;

   for(j = 0; j < nBins; j++)
   {
      if(g_vpSessions[idx].volumeBins[j] <= 0) continue;

      double binLow  = sL + j * binSize;
      double binHigh = binLow + binSize;
      double binMid  = binLow + binSize * 0.5;

      int y1 = topY + (int)((sH - binHigh) * pxPerPrice);
      int y2 = topY + (int)((sH - binLow)  * pxPerPrice);

      double ratio = g_vpSessions[idx].volumeBins[j] / maxVol;
      int barW = (int)(ratio * sesPxW * 0.85);
      int x2 = sesX1 + barW;

      bool isPOC = (j == pocIdx);
      bool isVA  = (binMid >= valP && binMid <= vah);
      double bullV = g_vpSessions[idx].bullBins[j];
      double bearV = g_vpSessions[idx].bearBins[j];

      uint clr;
      if(isPOC)
         clr = 0xC0FFD700;
      else if(bullV >= bearV)
         clr = isVA ? 0xA04CAF50 : 0xA02E7D32;
      else
         clr = isVA ? 0xA0EF5350 : 0xA0B43C3C;

      VPFillRect(g_vpPixels, cW, cH, sesX1, y1, x2, y2, clr);
   }
}

//+------------------------------------------------------------------+
void VPRenderTPOBars(int idx, int cW, int cH)
{
   int nBins = g_vpSessions[idx].numBins;
   if(nBins <= 0) return;
   double sH = g_vpSessions[idx].sessionHigh;
   double sL = g_vpSessions[idx].sessionLow;
   double range = sH - sL;
   if(range <= 0) return;

   int maxTPO = 0, pocIdx = 0;
   int j;
   for(j = 0; j < nBins; j++)
   {
      if(g_vpSessions[idx].tpoBins[j] > maxTPO)
      {
         maxTPO = g_vpSessions[idx].tpoBins[j];
         pocIdx = j;
      }
   }
   if(maxTPO <= 0) return;

   int sesX1, sesX2, tmpY;
   if(!ChartTimePriceToXY(0, 0, g_vpSessions[idx].sesStart, sH, sesX1, tmpY)) return;
   if(!ChartTimePriceToXY(0, 0, g_vpSessions[idx].sesEnd,   sH, sesX2, tmpY)) return;
   int sesPxW = sesX2 - sesX1;
   if(sesPxW <= 0) return;

   int topY, botY;
   ChartTimePriceToXY(0, 0, g_vpSessions[idx].sesStart, sH, tmpY, topY);
   ChartTimePriceToXY(0, 0, g_vpSessions[idx].sesStart, sL, tmpY, botY);
   double pxPerPrice = (botY != topY) ? (double)(botY - topY) / range : 1.0;

   double binSize = range / nBins;
   double valP = g_vpSessions[idx].valPrice;
   double vah  = g_vpSessions[idx].vahPrice;

   for(j = 0; j < nBins; j++)
   {
      if(g_vpSessions[idx].tpoBins[j] <= 0) continue;

      double binLow  = sL + j * binSize;
      double binHigh = binLow + binSize;
      double binMid  = binLow + binSize * 0.5;

      int y1 = topY + (int)((sH - binHigh) * pxPerPrice);
      int y2 = topY + (int)((sH - binLow)  * pxPerPrice);

      double ratio = (double)g_vpSessions[idx].tpoBins[j] / (double)maxTPO;
      int barW = (int)(ratio * sesPxW * 0.85);
      int x2 = sesX1 + barW;

      bool isPOC = (j == pocIdx);
      bool isVA  = (binMid >= valP && binMid <= vah);

      uint clr;
      if(isPOC)
         clr = 0xC0FFD700;
      else if(isVA)
         clr = 0xA06495ED;
      else
         clr = 0xA04664AA;

      VPFillRect(g_vpPixels, cW, cH, sesX1, y1, x2, y2, clr);
   }
}

//+------------------------------------------------------------------+
void VPFillRect(uint &px[], int cW, int cH, int x1, int y1, int x2, int y2, uint clr)
{
   int t;
   if(x1 > x2) { t = x1; x1 = x2; x2 = t; }
   if(y1 > y2) { t = y1; y1 = y2; y2 = t; }
   if(x1 < 0) x1 = 0;
   if(y1 < 0) y1 = 0;
   if(x2 >= cW) x2 = cW - 1;
   if(y2 >= cH) y2 = cH - 1;
   if(x1 > x2 || y1 > y2) return;

   for(int y = y1; y <= y2; y++)
   {
      int row = y * cW;
      for(int x = x1; x <= x2; x++)
         px[row + x] = clr;
   }
}

//+------------------------------------------------------------------+
void VPDrawDottedVLine(uint &px[], int cW, int cH, int x, uint clr)
{
   if(x < 0 || x >= cW) return;
   for(int y = 0; y < cH; y += 3)
      px[y * cW + x] = clr;
}

//+------------------------------------------------------------------+
void VPDrawDashedRect(uint &px[], int cW, int cH, int x1, int y1, int x2, int y2, uint clr)
{
   int t;
   if(x1 > x2) { t = x1; x1 = x2; x2 = t; }
   if(y1 > y2) { t = y1; y1 = y2; y2 = t; }

   for(int x = MathMax(0, x1); x <= MathMin(cW - 1, x2); x += 4)
   {
      if(y1 >= 0 && y1 < cH) px[y1 * cW + x] = clr;
      if(y2 >= 0 && y2 < cH) px[y2 * cW + x] = clr;
      int x2nd = x + 1;
      if(x2nd <= MathMin(cW - 1, x2))
      {
         if(y1 >= 0 && y1 < cH) px[y1 * cW + x2nd] = clr;
         if(y2 >= 0 && y2 < cH) px[y2 * cW + x2nd] = clr;
      }
   }
   for(int y = MathMax(0, y1); y <= MathMin(cH - 1, y2); y += 4)
   {
      if(x1 >= 0 && x1 < cW) px[y * cW + x1] = clr;
      if(x2 >= 0 && x2 < cW) px[y * cW + x2] = clr;
      int y2nd = y + 1;
      if(y2nd <= MathMin(cH - 1, y2))
      {
         if(x1 >= 0 && x1 < cW) px[y2nd * cW + x1] = clr;
         if(x2 >= 0 && x2 < cW) px[y2nd * cW + x2] = clr;
      }
   }
}

//+------------------------------------------------------------------+
string VPFormatVolume(double vol)
{
   if(vol >= 1000000000) return DoubleToString(vol / 1000000000, 1) + "B";
   if(vol >= 1000000)    return DoubleToString(vol / 1000000, 1) + "M";
   if(vol >= 1000)       return DoubleToString(vol / 1000, 1) + "K";
   return DoubleToString(vol, 0);
}

//+------------------------------------------------------------------+
void VPCreateLabel(string name, datetime time, double price, string text, color clr)
{
   ObjectCreate(name, OBJ_TEXT, 0, time, price);
   ObjectSetText(name, " " + text, 9, "Arial Bold", clr);
   ObjectSet(name, OBJPROP_ANCHOR, ANCHOR_LEFT_LOWER);
   ObjectSetInteger(0, name, OBJPROP_SELECTABLE, false);
   ObjectSetInteger(0, name, OBJPROP_HIDDEN, true);
}

//+------------------------------------------------------------------+
void VPDeleteAllLabels()
{
   string tag = g_vpPrefix + "L";
   int total = ObjectsTotal();
   for(int i = total - 1; i >= 0; i--)
   {
      string name = ObjectName(i);
      if(StringFind(name, tag) == 0)
         ObjectDelete(name);
   }
}

//+------------------------------------------------------------------+
void VPCreateToggleButton()
{
   string btnName = g_vpPrefix + "ToggleBtn";
   ObjectCreate(btnName, OBJ_BUTTON, 0, 0, 0);
   ObjectSet(btnName, OBJPROP_CORNER, CORNER_LEFT_LOWER);
   ObjectSet(btnName, OBJPROP_XDISTANCE, (int)(ChartGetInteger(0, CHART_WIDTH_IN_PIXELS) / 2 - 27));
   ObjectSet(btnName, OBJPROP_YDISTANCE, 25);
   ObjectSet(btnName, OBJPROP_XSIZE, 55);
   ObjectSet(btnName, OBJPROP_YSIZE, 20);
   ObjectSetText(btnName, "VP", 8, "Arial Bold", clrWhite);
   ObjectSet(btnName, OBJPROP_BGCOLOR, C'80,40,40');
   ObjectSet(btnName, OBJPROP_BORDER_COLOR, C'60,60,60');
   ObjectSet(btnName, OBJPROP_STATE, 0);
   ObjectSetInteger(0, btnName, OBJPROP_SELECTABLE, false);
   ObjectSetInteger(0, btnName, OBJPROP_HIDDEN, true);
   ObjectSetInteger(0, btnName, OBJPROP_ZORDER, 1000);
}

#endif // SQ_VP_DISPLAY_MQH
