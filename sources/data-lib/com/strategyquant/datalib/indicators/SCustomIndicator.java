package com.strategyquant.datalib.indicators;

import java.util.ArrayList;

public class SCustomIndicator {
   public String fileName;
   public String shortName;
   public String longName;
   public int returnType;
   public ArrayList<SParameter> parameterList = new ArrayList<>();
   public ArrayList<String> outputList = new ArrayList<>();

   @Override
   public String toString() {
      String var1 = "-----------------------------------------------------------------\nCustom indicator: "
         + this.fileName
         + ", shortName: "
         + this.shortName
         + ", longName: "
         + this.longName
         + ", type:"
         + this.returnType;

      for (int var2 = 0; var2 < this.parameterList.size(); var2++) {
         var1 = var1 + "\nparameter " + var2 + ": " + this.parameterList.get(var2).toString();
      }

      for (int var3 = 0; var3 < this.outputList.size(); var3++) {
         var1 = var1 + "\noutput " + var3 + ": " + this.outputList.get(var3);
      }

      return var1 + "\n-----------------------------------------------------------------";
   }
}
