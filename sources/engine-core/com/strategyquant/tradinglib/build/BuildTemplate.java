package com.strategyquant.tradinglib.build;

import com.strategyquant.lib.settings.IXMLAble;
import org.jdom2.Element;
import org.json.JSONObject;

public class BuildTemplate implements IXMLAble {
   private static final String elementName = "Template";
   public String name;
   public String description;
   public String filename;

   public JSONObject toJSON() {
      JSONObject var1 = new JSONObject();
      var1.put("name", this.name);
      var1.put("description", this.description);
      var1.put("filename", this.filename);
      return var1;
   }

   public Element getXML() {
      Element var1 = new Element("Template");
      var1.setAttribute("name", this.name);
      var1.setAttribute("description", this.description);
      var1.setAttribute("filename", this.filename);
      return var1;
   }

   public void setFromXML(Element var1) throws Exception {
      this.name = var1.getAttributeValue("name");
      this.description = var1.getAttributeValue("description");
      this.filename = var1.getAttributeValue("filename");
   }
}
