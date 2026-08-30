package com.strategyquant.tradinglib.backtest;

import com.strategyquant.datalib.consts.Precisions;
import com.strategyquant.datalib.data.DataManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.CommissionsMethod;
import com.strategyquant.tradinglib.SwapMethod;
import com.strategyquant.tradinglib.project.ProjectConfigHelper;
import com.strategyquant.tradinglib.simulator.Engines;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;

public class AutoRetestSettings implements Serializable {
   private boolean useCustomEngine = false;
   private boolean useCustomSymbol = false;
   private boolean useCustomTimeframe = false;
   private boolean useCustomDates = false;
   private boolean useCustomSubCharts = false;
   private boolean useCustomPrecision = false;
   private boolean useCustomMinDistance = false;
   private boolean useCustomSpread = false;
   private boolean useCustomSwap = false;
   private boolean useCustomSlippage = false;
   private boolean useCustomCommissions = false;
   private boolean useCustomMoneyManagement = false;
   private boolean useCustomTradingOptions = false;
   private String engine;
   private String symbol;
   private String timeframe;
   private String dateFrom;
   private String dateTo;
   private int precision = 0;
   private double spread = 0.0;
   private double slippage = 0.0;
   private double minDistance = 0.0;
   private CommissionsMethod commissions = null;
   private SwapMethod swap = null;
   private ArrayList<AutoRetestSettings.Subchart> subcharts = new ArrayList<>();

   public void init(Element var1) throws Exception {
      Element var2 = XMLUtil.getChildElem(var1, "CustomData");
      Element var3 = XMLUtil.getChildElem(var2, "Setups");
      Element var4 = XMLUtil.getChildElem(var3, "Setup");
      List var5 = var4.getChildren("Chart");
      Element var6 = (Element)var5.get(0);
      Element var7 = XMLUtil.getChildElem(var4, "MainTestValues");
      this.useCustomEngine = !XMLUtil.getBooleanAttr(var7, "engine", true);
      this.useCustomSymbol = !XMLUtil.getBooleanAttr(var7, "symbol", true);
      this.useCustomTimeframe = !XMLUtil.getBooleanAttr(var7, "timeframe", true);
      this.useCustomDates = !XMLUtil.getBooleanAttr(var7, "dates", true);
      this.useCustomSubCharts = !XMLUtil.getBooleanAttr(var7, "subcharts", true);
      this.useCustomPrecision = !XMLUtil.getBooleanAttr(var7, "precision", true);
      String var8 = XMLUtil.getStringAttr(var7, "spread", "strategy");
      String var9 = XMLUtil.getStringAttr(var7, "distance", "strategy");
      String var10 = XMLUtil.getStringAttr(var7, "commissions", "strategy");
      String var11 = XMLUtil.getStringAttr(var7, "slippage", "strategy");
      String var12 = XMLUtil.getStringAttr(var7, "swap", "strategy");
      this.useCustomSpread = var8.equals("false") || var8.equals("custom") || var8.equals("instrument");
      this.useCustomMinDistance = var9.equals("false") || var9.equals("custom") || var9.equals("instrument");
      this.useCustomCommissions = var10.equals("false") || var10.equals("custom") || var10.equals("instrument");
      this.useCustomSlippage = var11.equals("false") || var11.equals("custom") || var11.equals("instrument");
      this.useCustomSwap = var12.equals("false") || var12.equals("custom") || var12.equals("instrument");
      if (this.useCustomSymbol) {
         try {
            this.symbol = var6.getAttributeValue("symbol");
            if (this.symbol == null) {
               throw new Exception(L.t("Main chart symbol not set", new Object[0]));
            }
         } catch (Exception var26) {
            throw new Exception(L.t("Cannot load main chart settings - %s", new Object[]{var26.getMessage()}));
         }
      }

      for (int var13 = 1; var13 < var5.size(); var13++) {
         Element var14 = (Element)var5.get(var13);

         try {
            String var15 = var14.getAttributeValue("symbol");
            if (var15 == null) {
               throw new Exception(L.t("Subchart #%d symbol not set", new Object[]{var13}));
            }

            DataManager.getDataInfo("History", var15);
            String var16 = var14.getAttributeValue("timeframe");
            if (var16 == null) {
               throw new Exception(L.t("Subchart #%d timeframe not set", new Object[]{var13}));
            }

            this.subcharts.add(new AutoRetestSettings.Subchart(var15, var16));
         } catch (Exception var27) {
            throw new Exception(L.t("Cannot load subchart settings - %s", new Object[]{var27.getMessage()}));
         }
      }

      if (this.useCustomEngine) {
         try {
            this.engine = var4.getAttributeValue("engine");
            Engines.getEngine(this.engine);
         } catch (Exception var25) {
            throw new Exception(L.t("Cannot get engine - %s", new Object[]{var25.getMessage()}));
         }
      }

      if (this.useCustomTimeframe) {
         try {
            this.timeframe = var6.getAttributeValue("timeframe");
         } catch (Exception var24) {
            throw new Exception(L.t("Cannot get timeframe - %s", new Object[]{var24.getMessage()}));
         }
      }

      if (this.useCustomDates) {
         this.dateFrom = var4.getAttributeValue("dateFrom", "");
         this.dateTo = var4.getAttributeValue("dateTo", "");
         if (this.dateFrom.isEmpty() || this.dateTo.isEmpty()) {
            throw new Exception(L.t("Backtest dates not set", new Object[0]));
         }

         try {
            SQTime.parseDateToMilis(this.dateFrom);
            SQTime.parseDateToMilis(this.dateTo);
         } catch (Exception var23) {
            throw new Exception(L.t("Cannot parse backtest dates - %s", new Object[]{var23.getMessage()}));
         }
      }

      if (this.useCustomPrecision) {
         try {
            this.precision = Precisions.getPrecision(Integer.parseInt(var4.getAttributeValue("testPrecision", "-1")));
         } catch (Exception var22) {
            throw new Exception(L.t("Cannot get test precision - %s", new Object[]{var22.getMessage()}));
         }
      }

      if (this.useCustomMinDistance) {
         try {
            this.minDistance = Double.parseDouble(var4.getAttributeValue("minDist", "-1"));
            if (this.minDistance < 0.0) {
               throw new Exception("Min. distance not set");
            }
         } catch (Exception var21) {
            throw new Exception(L.t("Cannot get min. distance - %s", new Object[]{var21.getMessage()}));
         }
      }

      if (this.useCustomSpread) {
         try {
            this.spread = Double.parseDouble(var6.getAttributeValue("spread", "-1"));
            if (this.spread < 0.0) {
               throw new Exception(L.t("Spread not set", new Object[0]));
            }
         } catch (Exception var20) {
            throw new Exception(L.t("Cannot get spread - %s", new Object[]{var20.getMessage()}));
         }
      }

      if (this.useCustomSlippage) {
         try {
            this.slippage = Double.parseDouble(var4.getAttributeValue("slippage", "-1"));
            if (this.slippage < 0.0) {
               throw new Exception(L.t("Slippage not set", new Object[0]));
            }
         } catch (Exception var19) {
            throw new Exception(L.t("Cannot get slippage - %s", new Object[]{var19.getMessage()}));
         }
      }

      if (this.useCustomCommissions) {
         try {
            this.commissions = ProjectConfigHelper.getCommissionsMethod(var4);
         } catch (Exception var18) {
            throw new Exception(L.t("Cannot get commissions settings - %s", new Object[]{var18.getMessage()}));
         }
      }

      if (this.useCustomSwap) {
         try {
            this.swap = ProjectConfigHelper.getSwapMethod(var4);
         } catch (Exception var17) {
            throw new Exception(L.t("Cannot get swap settings - %s", new Object[]{var17.getMessage()}));
         }
      }

      Element var28 = var1.getChild("RiskMoneyManagement");
      if (var28 == null) {
         this.useCustomMoneyManagement = false;
      } else {
         this.useCustomMoneyManagement = XMLUtil.getBooleanAttr(var28, "customSettings", false);
      }

      Element var29 = var1.getChild("Options");
      if (var29 == null) {
         this.useCustomTradingOptions = false;
      } else {
         this.useCustomTradingOptions = XMLUtil.getBooleanAttr(var29, "customSettings", false);
      }
   }

   public boolean useCustomEngine() {
      return this.useCustomEngine;
   }

   public boolean useCustomSymbol() {
      return this.useCustomSymbol;
   }

   public boolean useCustomTimeframe() {
      return this.useCustomTimeframe;
   }

   public boolean useCustomDates() {
      return this.useCustomDates;
   }

   public boolean useCustomSubCharts() {
      return this.useCustomSubCharts;
   }

   public boolean useCustomPrecision() {
      return this.useCustomPrecision;
   }

   public boolean useCustomMinDistance() {
      return this.useCustomMinDistance;
   }

   public boolean useCustomSwap() {
      return this.useCustomSwap;
   }

   public boolean useCustomSpread() {
      return this.useCustomSpread;
   }

   public boolean useCustomSlippage() {
      return this.useCustomSlippage;
   }

   public boolean useCustomCommissions() {
      return this.useCustomCommissions;
   }

   public String getEngine() {
      return this.engine;
   }

   public String getSymbol() {
      return this.symbol;
   }

   public String getTimeframe() {
      return this.timeframe;
   }

   public String getDateFrom() {
      return this.dateFrom;
   }

   public String getDateTo() {
      return this.dateTo;
   }

   public int getPrecision() {
      return this.precision;
   }

   public double getSpread() {
      return this.spread;
   }

   public double getSlippage() {
      return this.slippage;
   }

   public double getMinDistance() {
      return this.minDistance;
   }

   public CommissionsMethod getCommissions() {
      return this.commissions;
   }

   public SwapMethod getSwap() {
      return this.swap;
   }

   public boolean useCustomMoneyManagement() {
      return this.useCustomMoneyManagement;
   }

   public boolean useCustomTradingOptions() {
      return this.useCustomTradingOptions;
   }

   public ArrayList<AutoRetestSettings.Subchart> getSubcharts() {
      return this.subcharts;
   }

   public class Subchart {
      public String symbol;
      public String timeframe;

      public Subchart(String nullx, String nullxx) {
         this.symbol = nullx;
         this.timeframe = nullxx;
      }
   }
}
