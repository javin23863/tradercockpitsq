package com.strategyquant.tradinglib.robustnesstests;

import com.strategyquant.lib.ValuesMap;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.Variable;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;

public class SequentialOptimizationResults extends ArrayList<FitnessData> {
   private static final String XML_VALUE_SEPARATOR = ";";
   private ValuesMap paramTypes;
   private boolean applyToStrategy = false;
   private boolean symmetricVariables = false;
   private boolean passed = false;

   public SequentialOptimizationResults() {
   }

   public SequentialOptimizationResults(ValuesMap var1, boolean var2, boolean var3) {
      this.paramTypes = var1;
      this.symmetricVariables = var2;
      this.applyToStrategy = var3;
   }

   public Element toXML() {
      Element var1 = new Element("ChainOptimizationResults");
      var1.setAttribute("applyToStrategy", "" + this.applyToStrategy);
      var1.setAttribute("symmetricVariables", "" + this.symmetricVariables);
      var1.setAttribute("passed", "" + this.passed);
      Element var2 = this.paramTypes.getXML();
      var2.setName("ParamTypes");
      var1.addContent(var2);
      Element var3 = new Element("Parameters");
      var1.addContent(var3);

      for (int var4 = 0; var4 < this.size(); var4++) {
         FitnessData var5 = this.get(var4);
         Element var6 = new Element("Parameter");
         var6.setAttribute("originalValue", "" + var5.originalValue);
         Element var7 = new Element("Values");
         Element var8 = new Element("Fitness");
         String var9 = "";
         String var10 = "";

         for (int var11 = 0; var11 < var5.values.length; var11++) {
            var9 = var9 + (var11 > 0 ? ";" : "") + var5.values[var11];
            var10 = var10 + (var11 > 0 ? ";" : "") + var5.fitness[var11];
         }

         var7.setText(var9);
         var8.setText(var10);
         Element var12 = new Element("Results");
         var12.addContent(new Element("BestAreaStartValue").setText("" + var5.bestAreaStartValue));
         var12.addContent(new Element("BestAreaEndValue").setText("" + var5.bestAreaEndValue));
         var12.addContent(new Element("BestValue").setText("" + var5.bestValue));
         var12.addContent(new Element("StableAreaFound").setText("" + var5.stableAreaFound));
         var6.addContent(var5.variable.getXML().clone());
         var6.addContent(var7);
         var6.addContent(var8);
         var6.addContent(var12);
         var3.addContent(var6);
      }

      return var1;
   }

   public SequentialOptimizationResults fromXML(Element var1) throws Exception {
      this.clear();
      this.applyToStrategy = XMLUtil.getBooleanAttr(var1, "applyToStrategy", false);
      this.symmetricVariables = XMLUtil.getBooleanAttr(var1, "symmetricVariables", false);
      this.passed = XMLUtil.getBooleanAttr(var1, "passed", false);
      Element var2 = XMLUtil.getChildElem(var1, "ParamTypes");
      this.paramTypes = new ValuesMap();
      this.paramTypes.setFromXML(var2);
      Element var3 = XMLUtil.getChildElem(var1, "Parameters");
      List var4 = var3.getChildren("Parameter");

      for (int var5 = 0; var5 < var4.size(); var5++) {
         Element var6 = (Element)var4.get(var5);
         FitnessData var7 = new FitnessData();
         var7.originalValue = XMLUtil.getDoubleAttr(var6, "originalValue", 0.0);
         var7.variable = new Variable(XMLUtil.getChildElem(var6, "variable"));
         var7.values = this.loadXMLValues(XMLUtil.getChildElem(var6, "Values"));
         var7.fitness = this.loadXMLValues(XMLUtil.getChildElem(var6, "Fitness"));
         Element var8 = XMLUtil.getChildElem(var6, "Results");
         var7.bestAreaStartValue = Double.parseDouble(XMLUtil.getNodeValue(var8, "BestAreaStartValue"));
         var7.bestAreaEndValue = Double.parseDouble(XMLUtil.getNodeValue(var8, "BestAreaEndValue"));
         var7.bestValue = Double.parseDouble(XMLUtil.getNodeValue(var8, "BestValue"));
         var7.stableAreaFound = Boolean.parseBoolean(XMLUtil.getNodeValue(var8, "StableAreaFound"));
         this.add(var7);
      }

      return this;
   }

   private double[] loadXMLValues(Element var1) {
      String[] var2 = var1.getText().split(";");
      double[] var3 = new double[var2.length];

      for (int var4 = 0; var4 < var2.length; var4++) {
         var3[var4] = Double.parseDouble(var2[var4]);
      }

      return var3;
   }

   public boolean isApplyToStrategy() {
      return this.applyToStrategy;
   }

   public void setApplyToStrategy(boolean var1) {
      this.applyToStrategy = var1;
   }

   public boolean isSymmetricVariables() {
      return this.symmetricVariables;
   }

   public ValuesMap getParamTypes() {
      return this.paramTypes;
   }

   public boolean isPassed() {
      return this.passed;
   }

   public void setPassed(boolean var1) {
      this.passed = var1;
   }

   public SequentialOptimizationResults getClone() {
      SequentialOptimizationResults var1 = new SequentialOptimizationResults();
      var1.paramTypes = this.paramTypes.clone();
      var1.applyToStrategy = this.applyToStrategy;
      var1.symmetricVariables = this.symmetricVariables;

      for (int var2 = 0; var2 < this.size(); var2++) {
         var1.add(this.get(var2).getClone());
      }

      return var1;
   }
}
