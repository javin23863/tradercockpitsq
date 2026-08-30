package com.strategyquant.plugin.App.impl.SQXBusiness;

import com.strategyquant.lib.XMLUtil;
import org.jdom2.Element;

public class EAParameter {
   public String originalName;
   public String newName;
   public String type;
   public String value;
   public boolean isCategory;
   public int position;

   public EAParameter(Element var1) {
      this.originalName = var1.getAttributeValue("name");
      this.newName = var1.getAttributeValue("newName");
      this.type = var1.getAttributeValue("type");
      this.value = var1.getAttributeValue("value");
      this.isCategory = XMLUtil.getBooleanAttr(var1, "isCategory", false);
      this.position = XMLUtil.getIntAttr(var1, "position", 0);
   }
}
