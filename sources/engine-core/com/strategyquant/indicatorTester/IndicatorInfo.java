package com.strategyquant.indicatorTester;

import com.strategyquant.datalib.DataSeries;
import com.strategyquant.tradinglib.ChartData;
import java.io.File;
import java.util.ArrayList;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;

public class IndicatorInfo {
   public String simpleName;
   public String className;
   public String title;
   public boolean ignored = false;
   public int returnType;
   public int shift = 0;
   public ArrayList<String> outputBuffers = new ArrayList<>();
   public ArrayList<IndyParameter> parameters = new ArrayList<>();

   public ArrayList<JSONObject> getJSONList() {
      ArrayList var1 = new ArrayList();

      for (String var3 : this.outputBuffers) {
         JSONObject var4 = new JSONObject();
         JSONArray var5 = new JSONArray();
         String var6 = this.simpleName + (var3.equals("Value") ? "" : "." + var3);
         var4.put("name", var6);
         var4.put("value", var6);
         var4.put("className", this.className);
         var4.put("shift", this.shift);

         for (IndyParameter var8 : this.parameters) {
            if (!var8.className.equals(ChartData.class.getSimpleName())) {
               if (var8.className.equals(DataSeries.class.getSimpleName())) {
                  var8.value = "Close";
               } else {
                  var8.value = var8.defaultValue;
               }

               var5.put(var8.toJSON());
            }
         }

         var4.put("parameters", var5);
         var1.add(var4);
      }

      return var1;
   }

   public Element getXML(String var1) {
      Element var2 = new Element("Test");
      var2.setAttribute("use", "true");
      var2.setAttribute("indicator", var1);
      var2.setAttribute("parameters", this.getParams());
      var2.setAttribute("decimals", "6");
      var2.setAttribute("result", "");
      var2.setAttribute("shift", String.valueOf(this.shift));
      String var3 = var1 + ".csv";
      File var4 = new File(IndicatorTesterConfig.defaultConfigFilePath + "/" + var3);
      var2.setAttribute("fileName", var3);
      var2.setAttribute("exists", var4.exists() ? "yes" : "no");
      return var2;
   }

   private String getParams() {
      String var1 = "";
      if (this.parameters.isEmpty()) {
         return var1;
      }

      for (IndyParameter var3 : this.parameters) {
         if (!var3.className.equals(ChartData.class.getSimpleName())) {
            var1 = var1 + (var3.value != null ? var3.value : var3.defaultValue) + ",";
         }
      }

      return var1.length() > 0 ? var1.substring(0, var1.length() - 1) : var1;
   }
}
