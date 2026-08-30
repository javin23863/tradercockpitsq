package com.strategyquant.tradinglib;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.settings.IXMLAble;
import org.jdom2.Element;

public class WalkForwardPeriod implements IXMLAble {
   public long optimizeFrom = 0L;
   public long optimizeTo = 0L;
   public long runFrom = 0L;
   public long runTo = 0L;
   public boolean futurePeriod = false;
   public String testParameters = null;
   public SQStats optimizationStatData = null;
   public SQStats runStatData = null;

   public Element getXML() {
      Element var1 = new Element("WalkForwardPeriod");
      XMLUtil.trySetAttr(var1, "optimizeFrom", this.optimizeFrom);
      XMLUtil.trySetAttr(var1, "optimizeTo", this.optimizeTo);
      XMLUtil.trySetAttr(var1, "runFrom", this.runFrom);
      XMLUtil.trySetAttr(var1, "runTo", this.runTo);
      XMLUtil.trySetAttr(var1, "futurePeriod", this.futurePeriod);
      XMLUtil.trySetAttr(var1, "testParameters", this.testParameters);
      Element var2 = new Element("OptimizationStats");
      var1.addContent(var2);
      Element var3 = XMLUtil.valueToElement("stats", this.optimizationStatData);
      if (var3 != null) {
         var2.addContent(var3);
      }

      Element var4 = new Element("RunStats");
      var1.addContent(var4);
      Element var5 = XMLUtil.valueToElement("stats", this.runStatData);
      if (var5 != null) {
         var4.addContent(var5);
      }

      return var1;
   }

   public void setFromXML(Element var1) throws Exception {
      this.optimizeFrom = XMLUtil.tryGetLongAttr(var1, "optimizeFrom");
      this.optimizeTo = XMLUtil.tryGetLongAttr(var1, "optimizeTo");
      this.runFrom = XMLUtil.tryGetLongAttr(var1, "runFrom");
      this.runTo = XMLUtil.tryGetLongAttr(var1, "runTo");
      this.futurePeriod = XMLUtil.tryGetBoolAttr(var1, "futurePeriod");
      this.testParameters = var1.getAttributeValue("testParameters");
      if (var1.getChild("OptimizationStats").getChild("stats").getChild("SQStats") != null) {
         if (this.optimizationStatData == null) {
            this.optimizationStatData = new SQStats();
         }

         this.optimizationStatData.setFromXML(var1.getChild("OptimizationStats").getChild("stats").getChild("SQStats"));
      }

      if (var1.getChild("RunStats").getChild("stats") != null && var1.getChild("RunStats").getChild("stats").getChild("SQStats") != null) {
         if (this.runStatData == null) {
            this.runStatData = new SQStats();
         }

         this.runStatData.setFromXML(var1.getChild("RunStats").getChild("stats").getChild("SQStats"));
      }
   }
}
