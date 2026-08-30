package com.strategyquant.datalib.customData.ct;

import com.strategyquant.lib.XMLUtil;
import org.jdom2.Element;

public class CTCustomIndicatorParam {
   public String name;
   public String type;
   public String value;

   public CTCustomIndicatorParam(Element var1) throws Exception {
      this.name = XMLUtil.getAttr(var1, "name");
      this.type = XMLUtil.getAttr(var1, "type");
      this.value = XMLUtil.getAttr(var1, "defaultValue");
   }
}
