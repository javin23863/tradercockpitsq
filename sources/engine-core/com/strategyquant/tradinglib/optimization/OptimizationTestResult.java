package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.settings.IXMLAble;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.SQStats;
import it.unimi.dsi.fastutil.ints.Int2DoubleOpenHashMap;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptimizationTestResult implements IXMLAble {
   public static final Logger Log = LoggerFactory.getLogger("OptimizationTestResult");
   public String params;
   public SQStats stats;
   private Int2DoubleOpenHashMap parsedParams = null;

   public OptimizationTestResult(ResultsGroup var1, String var2, OptimizationProfile var3) throws Exception {
      this.params = var2;
      this.stats = var1.mainResult().stats((byte)0, (byte)10, (byte)127).getClone();
      double var4 = var1.getFitness();
      double var6 = this.stats.getDouble("Fitness");
      if (var4 > 0.0 && var6 == 0.0) {
         this.stats.set("Fitness", var4);
      }

      var1.setOptimizationProfile(var3);
   }

   public OptimizationTestResult() {
   }

   public Element getXML() {
      Element var1 = new Element("Optimization");
      XMLUtil.trySetAttr(var1, "params", this.params);
      var1.addContent(this.stats.getXML());
      return var1;
   }

   public void setFromXML(Element var1) throws Exception {
      this.params = var1.getAttributeValue("params");
      this.stats = new SQStats();
      this.stats.setFromXML(var1.getChild("SQStats"));
   }

   public Int2DoubleOpenHashMap parseParams() {
      if (this.parsedParams == null) {
         this.parsedParams = new Int2DoubleOpenHashMap();
      }

      String[] var1 = this.params.split(",");

      for (int var2 = 0; var2 < var1.length; var2++) {
         if (var1[var2].contains("=")) {
            String[] var3 = var1[var2].split("=");

            double var4;
            try {
               var4 = Double.parseDouble(var3[1].trim());
            } catch (Exception var7) {
               if (var3[1].trim().equalsIgnoreCase("true")) {
                  var4 = 1.0;
               } else {
                  if (!var3[1].trim().equalsIgnoreCase("false")) {
                     continue;
                  }

                  var4 = 0.0;
               }
            }

            this.parsedParams.put(var3[0].trim().hashCode(), var4);
         }
      }

      return this.parsedParams;
   }

   public void serialize(ObjectOutput var1) throws IOException {
      var1.writeInt(1);
      SQUtils.writeUTF(var1, this.params);
      this.stats.serialize(var1);
   }

   public void deserialize(ObjectInput var1) throws IOException {
      int var2 = var1.readInt();
      if (var2 != 1) {
         throw new IllegalArgumentException("Incorrect format " + var2);
      }

      this.params = SQUtils.readUTF(var1);
      this.stats = new SQStats();
      this.stats.deserialize(var1);
   }
}
