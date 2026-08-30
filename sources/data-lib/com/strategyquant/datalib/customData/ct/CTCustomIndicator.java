package com.strategyquant.datalib.customData.ct;

import com.strategyquant.lib.XMLUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jdom2.Element;

public class CTCustomIndicator {
   public String fileName;
   public String shortName;
   public String longName;
   public String returnType;
   public List<CTCustomIndicatorParam> params = new ArrayList<>();
   public Map<String, CTCustomIndicatorOutput> outputs = new HashMap<>();

   public CTCustomIndicator(Element var1) throws Exception {
      this.fileName = XMLUtil.getAttr(var1, "fileName");
      this.shortName = XMLUtil.getAttr(var1, "shortName");
      this.longName = XMLUtil.getAttr(var1, "longName");
      this.returnType = XMLUtil.getAttr(var1, "returnType");

      for (Element var4 : var1.getChildren("Param")) {
         CTCustomIndicatorParam var5 = new CTCustomIndicatorParam(var4);
         this.params.add(var5);
      }

      for (Element var9 : var1.getChildren("Output")) {
         CTCustomIndicatorOutput var6 = new CTCustomIndicatorOutput(var9);
         this.outputs.put(var6.name, var6);
      }
   }
}
