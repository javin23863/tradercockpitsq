package com.strategyquant.tradinglib.optimization;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.settings.IXMLAble;
import org.jdom2.Element;

public class OptProfileChecksLevels implements IXMLAble {
   public boolean evalProfitOptCheck;
   public boolean evalAvgProfitCheck;
   public boolean evalUniformDistrCheck;
   public boolean evalTopProfitCheck;
   public int profitableOptPct;
   public double avgProfit;
   public int uniformDistrChanges;
   public double stdevAvgProfit;

   public OptProfileChecksLevels clone() {
      OptProfileChecksLevels var1 = new OptProfileChecksLevels();
      var1.evalProfitOptCheck = this.evalProfitOptCheck;
      var1.evalAvgProfitCheck = this.evalAvgProfitCheck;
      var1.evalUniformDistrCheck = this.evalUniformDistrCheck;
      var1.evalTopProfitCheck = this.evalTopProfitCheck;
      var1.profitableOptPct = this.profitableOptPct;
      var1.avgProfit = this.avgProfit;
      var1.uniformDistrChanges = this.uniformDistrChanges;
      var1.stdevAvgProfit = this.stdevAvgProfit;
      return var1;
   }

   public Element getXML() {
      Element var1 = new Element("OptProfileChecksLevels");
      var1.addContent(new Element("ProfitableOptPct").setText(this.profitableOptPct + "").setAttribute("use", this.evalProfitOptCheck + ""));
      var1.addContent(new Element("AvgProfit").setText(this.avgProfit + "").setAttribute("use", this.evalAvgProfitCheck + ""));
      var1.addContent(new Element("UniformDistrChanges").setText(this.uniformDistrChanges + "").setAttribute("use", this.evalUniformDistrCheck + ""));
      var1.addContent(new Element("StdevAvgProfit").setText(this.stdevAvgProfit + "").setAttribute("use", this.evalTopProfitCheck + ""));
      return var1;
   }

   public void setFromXML(Element var1) throws Exception {
      this.profitableOptPct = XMLUtil.getInt(var1, "ProfitableOptPct", 30);
      this.avgProfit = XMLUtil.getDouble(var1, "AvgProfit", 0.0);
      this.uniformDistrChanges = XMLUtil.getInt(var1, "UniformDistrChanges", 5);
      this.stdevAvgProfit = XMLUtil.getDouble(var1, "StdevAvgProfit", 1.0);
      this.evalProfitOptCheck = XMLUtil.getBoolean(XMLUtil.tryAddElement(var1, "ProfitableOptPct"), "use", true);
      this.evalAvgProfitCheck = XMLUtil.getBoolean(XMLUtil.tryAddElement(var1, "AvgProfit"), "use", true);
      this.evalUniformDistrCheck = XMLUtil.getBoolean(XMLUtil.tryAddElement(var1, "UniformDistrChanges"), "use", true);
      this.evalTopProfitCheck = XMLUtil.getBoolean(XMLUtil.tryAddElement(var1, "StdevAvgProfit"), "use", true);
   }
}
