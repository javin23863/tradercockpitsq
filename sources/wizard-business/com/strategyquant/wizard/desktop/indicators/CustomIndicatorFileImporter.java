package com.strategyquant.wizard.desktop.indicators;

import com.strategyquant.lib.app.MainApp;
import java.awt.Component;
import java.io.File;
import org.jdom2.Element;
import org.jdom2.output.XMLOutputter;

public class CustomIndicatorFileImporter {
   private Component parent;

   public CustomIndicatorFileImporter(Component var1) {
      this.parent = var1;
   }

   public String load(File var1) {
      Element var2 = new Element("customIndicators");

      try {
         if (!var1.isDirectory() && (var1.getName().contains(".mq4") || var1.getName().contains(".xml"))) {
            SCustomIndicator var3 = SCustomIndicatorFileParser.parse(var1);
            Element var4 = this.getCustomIndicatorNode(var3);
            var2.addContent(var4);
            return new XMLOutputter().outputString(var2);
         } else {
            MainApp.showErrorDialog("File must be mq4 or xml");
            return null;
         }
      } catch (Exception var5) {
         MainApp.showErrorDialog("Error while parsing mq4 file:" + var5.getMessage());
         return null;
      }
   }

   private Element getCustomIndicatorNode(SCustomIndicator var1) throws Exception {
      Element var2 = new Element("CustomIndicator")
         .setAttribute("fileName", var1.fileName)
         .setAttribute("shortName", var1.shortName)
         .setAttribute("longName", var1.longName);
      var2.setAttribute("returnType", var1.returnType);
      Element var3 = new Element("outputs");
      var2.addContent(var3);
      int var4 = 1;

      for (String var6 : var1.outputList) {
         var3.addContent(new Element("output").setAttribute("index", Integer.toString(var4)).setAttribute("name", var6));
         var4++;
      }

      Element var9 = new Element("params");
      var2.addContent(var9);

      for (SParameter var7 : var1.parameterList) {
         Element var8 = new Element("param").setAttribute("index", String.valueOf(var7.index)).setAttribute("name", var7.name).setAttribute("type", var7.type);
         var8.setAttribute("defaultValue", var7.value);
         var9.addContent(var8);
      }

      return var2;
   }
}
