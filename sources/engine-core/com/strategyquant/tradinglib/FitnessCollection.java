package com.strategyquant.tradinglib;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import org.jdom2.Element;

public class FitnessCollection {
   private double fitnessIS = 0.0;
   private double fitnessOOS = 0.0;
   private double fitnessFS = 0.0;
   private double fitnessIST = 0.0;
   private double fitnessISV = 0.0;
   private double fitnessISV1 = 0.0;
   private double fitnessISV2 = 0.0;
   private double fitnessISV3 = 0.0;
   private double fitnessISV4 = 0.0;
   private double fitnessISV5 = 0.0;
   private double fitnessISV6 = 0.0;
   private double fitnessISV7 = 0.0;
   private double fitnessISV8 = 0.0;
   private double fitnessISV9 = 0.0;
   private double fitnessISV10 = 0.0;
   private double fitnessOOS1 = 0.0;
   private double fitnessOOS2 = 0.0;
   private double fitnessOOS3 = 0.0;
   private double fitnessOOS4 = 0.0;
   private double fitnessOOS5 = 0.0;
   private double fitnessOOS6 = 0.0;
   private double fitnessOOS7 = 0.0;
   private double fitnessOOS8 = 0.0;
   private double fitnessOOS9 = 0.0;
   private double fitnessOOS10 = 0.0;

   public double getFitness(byte var1) {
      switch (var1) {
         case 10:
            return this.fitnessIS;
         case 11:
            return this.fitnessIST;
         case 20:
            return this.fitnessOOS;
         case 21:
            return this.fitnessOOS1;
         case 22:
            return this.fitnessOOS2;
         case 23:
            return this.fitnessOOS3;
         case 24:
            return this.fitnessOOS4;
         case 25:
            return this.fitnessOOS5;
         case 26:
            return this.fitnessOOS6;
         case 27:
            return this.fitnessOOS7;
         case 28:
            return this.fitnessOOS8;
         case 29:
            return this.fitnessOOS9;
         case 30:
            return this.fitnessOOS10;
         case 40:
            return this.fitnessISV;
         case 41:
            return this.fitnessISV1;
         case 42:
            return this.fitnessISV2;
         case 43:
            return this.fitnessISV3;
         case 44:
            return this.fitnessISV4;
         case 45:
            return this.fitnessISV5;
         case 46:
            return this.fitnessISV6;
         case 47:
            return this.fitnessISV7;
         case 48:
            return this.fitnessISV8;
         case 49:
            return this.fitnessISV9;
         case 50:
            return this.fitnessISV10;
         case 127:
            return this.fitnessFS;
         default:
            return 0.0;
      }
   }

   public void setFitness(byte var1, double var2) {
      switch (var1) {
         case 10:
            this.fitnessIS = var2;
            break;
         case 11:
            this.fitnessIST = var2;
            break;
         case 20:
            this.fitnessOOS = var2;
            break;
         case 21:
            this.fitnessOOS1 = var2;
            break;
         case 22:
            this.fitnessOOS2 = var2;
            break;
         case 23:
            this.fitnessOOS3 = var2;
            break;
         case 24:
            this.fitnessOOS4 = var2;
            break;
         case 25:
            this.fitnessOOS5 = var2;
            break;
         case 26:
            this.fitnessOOS6 = var2;
            break;
         case 27:
            this.fitnessOOS7 = var2;
            break;
         case 28:
            this.fitnessOOS8 = var2;
            break;
         case 29:
            this.fitnessOOS9 = var2;
            break;
         case 30:
            this.fitnessOOS10 = var2;
            break;
         case 40:
            this.fitnessISV = var2;
            break;
         case 41:
            this.fitnessISV1 = var2;
            break;
         case 42:
            this.fitnessISV2 = var2;
            break;
         case 43:
            this.fitnessISV3 = var2;
            break;
         case 44:
            this.fitnessISV4 = var2;
            break;
         case 45:
            this.fitnessISV5 = var2;
            break;
         case 46:
            this.fitnessISV6 = var2;
            break;
         case 47:
            this.fitnessISV7 = var2;
            break;
         case 48:
            this.fitnessISV8 = var2;
            break;
         case 49:
            this.fitnessISV9 = var2;
            break;
         case 50:
            this.fitnessISV10 = var2;
            break;
         case 127:
            this.fitnessFS = var2;
      }
   }

   public FitnessCollection clone() {
      FitnessCollection var1 = new FitnessCollection();
      var1.fitnessIS = this.fitnessIS;
      var1.fitnessOOS = this.fitnessOOS;
      var1.fitnessFS = this.fitnessFS;
      var1.fitnessIST = this.fitnessIST;
      var1.fitnessISV = this.fitnessISV;
      var1.fitnessISV1 = this.fitnessISV1;
      var1.fitnessISV2 = this.fitnessISV2;
      var1.fitnessISV3 = this.fitnessISV3;
      var1.fitnessISV4 = this.fitnessISV4;
      var1.fitnessISV5 = this.fitnessISV5;
      var1.fitnessISV6 = this.fitnessISV6;
      var1.fitnessISV7 = this.fitnessISV7;
      var1.fitnessISV8 = this.fitnessISV8;
      var1.fitnessISV9 = this.fitnessISV9;
      var1.fitnessISV10 = this.fitnessISV10;
      var1.fitnessOOS1 = this.fitnessOOS1;
      var1.fitnessOOS2 = this.fitnessOOS2;
      var1.fitnessOOS3 = this.fitnessOOS3;
      var1.fitnessOOS4 = this.fitnessOOS4;
      var1.fitnessOOS5 = this.fitnessOOS5;
      var1.fitnessOOS6 = this.fitnessOOS6;
      var1.fitnessOOS7 = this.fitnessOOS7;
      var1.fitnessOOS8 = this.fitnessOOS8;
      var1.fitnessOOS9 = this.fitnessOOS9;
      var1.fitnessOOS10 = this.fitnessOOS10;
      return var1;
   }

   public void clear() {
      this.fitnessIS = 0.0;
      this.fitnessOOS = 0.0;
      this.fitnessFS = 0.0;
      this.fitnessIST = 0.0;
      this.fitnessISV = 0.0;
      this.fitnessISV1 = 0.0;
      this.fitnessISV2 = 0.0;
      this.fitnessISV3 = 0.0;
      this.fitnessISV4 = 0.0;
      this.fitnessISV5 = 0.0;
      this.fitnessISV6 = 0.0;
      this.fitnessISV7 = 0.0;
      this.fitnessISV8 = 0.0;
      this.fitnessISV9 = 0.0;
      this.fitnessISV10 = 0.0;
      this.fitnessOOS1 = 0.0;
      this.fitnessOOS2 = 0.0;
      this.fitnessOOS3 = 0.0;
      this.fitnessOOS4 = 0.0;
      this.fitnessOOS5 = 0.0;
      this.fitnessOOS6 = 0.0;
      this.fitnessOOS7 = 0.0;
      this.fitnessOOS8 = 0.0;
      this.fitnessOOS9 = 0.0;
      this.fitnessOOS10 = 0.0;
   }

   public Element getXML() {
      Element var1 = new Element("Fitnesses");
      var1.setAttribute("IS", Double.toString(SQUtils.round6(this.fitnessIS)));
      var1.setAttribute("FS", Double.toString(SQUtils.round6(this.fitnessFS)));
      var1.setAttribute("IST", Double.toString(SQUtils.round6(this.fitnessIST)));
      var1.setAttribute("ISV", Double.toString(SQUtils.round6(this.fitnessISV)));
      var1.setAttribute("ISV1", Double.toString(SQUtils.round6(this.fitnessISV1)));
      var1.setAttribute("ISV2", Double.toString(SQUtils.round6(this.fitnessISV2)));
      var1.setAttribute("ISV3", Double.toString(SQUtils.round6(this.fitnessISV3)));
      var1.setAttribute("ISV4", Double.toString(SQUtils.round6(this.fitnessISV4)));
      var1.setAttribute("ISV5", Double.toString(SQUtils.round6(this.fitnessISV5)));
      var1.setAttribute("ISV6", Double.toString(SQUtils.round6(this.fitnessISV6)));
      var1.setAttribute("ISV7", Double.toString(SQUtils.round6(this.fitnessISV7)));
      var1.setAttribute("ISV8", Double.toString(SQUtils.round6(this.fitnessISV8)));
      var1.setAttribute("ISV9", Double.toString(SQUtils.round6(this.fitnessISV9)));
      var1.setAttribute("ISV10", Double.toString(SQUtils.round6(this.fitnessISV10)));
      var1.setAttribute("OOS", Double.toString(SQUtils.round6(this.fitnessOOS)));
      var1.setAttribute("OOS1", Double.toString(SQUtils.round6(this.fitnessOOS1)));
      var1.setAttribute("OOS2", Double.toString(SQUtils.round6(this.fitnessOOS2)));
      var1.setAttribute("OOS3", Double.toString(SQUtils.round6(this.fitnessOOS3)));
      var1.setAttribute("OOS4", Double.toString(SQUtils.round6(this.fitnessOOS4)));
      var1.setAttribute("OOS5", Double.toString(SQUtils.round6(this.fitnessOOS5)));
      var1.setAttribute("OOS6", Double.toString(SQUtils.round6(this.fitnessOOS6)));
      var1.setAttribute("OOS7", Double.toString(SQUtils.round6(this.fitnessOOS7)));
      var1.setAttribute("OOS8", Double.toString(SQUtils.round6(this.fitnessOOS8)));
      var1.setAttribute("OOS9", Double.toString(SQUtils.round6(this.fitnessOOS9)));
      var1.setAttribute("OOS10", Double.toString(SQUtils.round6(this.fitnessOOS10)));
      return var1;
   }

   public void setFromXML(Element var1) {
      Element var2 = var1.getChild("Fitnesses");
      if (var2 != null) {
         this.fitnessIS = XMLUtil.getDoubleAttr(var2, "IS", 0.0);
         this.fitnessFS = XMLUtil.getDoubleAttr(var2, "FS", 0.0);
         this.fitnessIST = XMLUtil.getDoubleAttr(var2, "IST", 0.0);
         this.fitnessISV = XMLUtil.getDoubleAttr(var2, "ISV", 0.0);
         this.fitnessISV1 = XMLUtil.getDoubleAttr(var2, "ISV1", 0.0);
         this.fitnessISV2 = XMLUtil.getDoubleAttr(var2, "ISV2", 0.0);
         this.fitnessISV3 = XMLUtil.getDoubleAttr(var2, "ISV3", 0.0);
         this.fitnessISV4 = XMLUtil.getDoubleAttr(var2, "ISV4", 0.0);
         this.fitnessISV5 = XMLUtil.getDoubleAttr(var2, "ISV5", 0.0);
         this.fitnessISV6 = XMLUtil.getDoubleAttr(var2, "ISV6", 0.0);
         this.fitnessISV7 = XMLUtil.getDoubleAttr(var2, "ISV7", 0.0);
         this.fitnessISV8 = XMLUtil.getDoubleAttr(var2, "ISV8", 0.0);
         this.fitnessISV9 = XMLUtil.getDoubleAttr(var2, "ISV9", 0.0);
         this.fitnessISV10 = XMLUtil.getDoubleAttr(var2, "ISV10", 0.0);
         this.fitnessOOS = XMLUtil.getDoubleAttr(var2, "OOS", 0.0);
         this.fitnessOOS1 = XMLUtil.getDoubleAttr(var2, "OOS1", 0.0);
         this.fitnessOOS2 = XMLUtil.getDoubleAttr(var2, "OOS2", 0.0);
         this.fitnessOOS3 = XMLUtil.getDoubleAttr(var2, "OOS3", 0.0);
         this.fitnessOOS4 = XMLUtil.getDoubleAttr(var2, "OOS4", 0.0);
         this.fitnessOOS5 = XMLUtil.getDoubleAttr(var2, "OOS5", 0.0);
         this.fitnessOOS6 = XMLUtil.getDoubleAttr(var2, "OOS6", 0.0);
         this.fitnessOOS7 = XMLUtil.getDoubleAttr(var2, "OOS7", 0.0);
         this.fitnessOOS8 = XMLUtil.getDoubleAttr(var2, "OOS8", 0.0);
         this.fitnessOOS9 = XMLUtil.getDoubleAttr(var2, "OOS9", 0.0);
         this.fitnessOOS10 = XMLUtil.getDoubleAttr(var2, "OOS10", 0.0);
      }
   }
}
