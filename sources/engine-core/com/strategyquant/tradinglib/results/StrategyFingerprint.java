package com.strategyquant.tradinglib.results;

import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.tradinglib.Order;
import com.strategyquant.tradinglib.OrdersList;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyFingerprint implements IXMLAble {
   public static final Logger Log = LoggerFactory.getLogger("StrategyFingerprint");
   public String recordName;
   public int exactFingerprint;
   public String exactFingerprintStr;
   public int trades;
   public double profit;
   public double drawdown;
   public double fitness;
   public int tradesHash;

   public StrategyFingerprint() {
   }

   public StrategyFingerprint(ResultsGroup var1) {
      this.recordName = var1.getName();
      this.fitness = var1.getFitness();

      try {
         SQStats var2 = var1.portfolio().stats((byte)0, (byte)10, (byte)127);
         if (var2 != null) {
            this.trades = var2.getInt("NumberOfTrades");
            this.profit = var2.getDouble("NetProfit");
            this.drawdown = var2.getDouble("Drawdown");
         }

         this.tradesHash = 0;
         OrdersList var3 = var1.orders();

         for (int var4 = 0; var4 < Math.min(5, var3.size()); var4++) {
            Order var5 = var3.get(var4);
            long var6 = var5.OpenTime;
            var6 = SQTime.setSecond(var6, 0);
            var6 = SQTime.setMiliSeconds(var6, 0);
            this.tradesHash = (int)(this.tradesHash + var6);
            var6 = var5.OpenTime;
            var6 = SQTime.setSecond(var6, 0);
            var6 = SQTime.setMiliSeconds(var6, 0);
            this.tradesHash = (int)(this.tradesHash + var6);
         }

         this.exactFingerprintStr = String.format("%.2f__%d__%d", this.profit, this.trades, this.tradesHash);
         this.exactFingerprint = this.exactFingerprintStr.hashCode();
      } catch (Exception var8) {
         Log.error("Cannot initialize strateyg fingerprint");
      }
   }

   public Element getXML() {
      Element var1 = new Element("Fingerprint");
      XMLUtil.trySetAttr(var1, "strategyName", this.recordName);
      XMLUtil.trySetAttr(var1, "exact", this.exactFingerprint);
      XMLUtil.trySetAttr(var1, "trades", this.trades);
      XMLUtil.trySetAttr(var1, "profit", this.profit);
      XMLUtil.trySetAttr(var1, "drawdown", this.drawdown);
      XMLUtil.trySetAttr(var1, "fitness", this.fitness);
      XMLUtil.trySetAttr(var1, "tradesHash", this.tradesHash);
      return var1;
   }

   public void setFromXML(Element var1) {
      this.recordName = var1.getAttributeValue("strategyName");
      this.exactFingerprint = XMLUtil.tryGetIntAttr(var1, "exact");
      this.trades = XMLUtil.tryGetIntAttr(var1, "trades");
      this.profit = XMLUtil.tryGetDoubleAttr(var1, "profit");
      this.drawdown = XMLUtil.tryGetDoubleAttr(var1, "drawdown");
      this.fitness = XMLUtil.tryGetDoubleAttr(var1, "fitness");
      this.tradesHash = XMLUtil.tryGetIntAttr(var1, "tradesHash");
   }
}
