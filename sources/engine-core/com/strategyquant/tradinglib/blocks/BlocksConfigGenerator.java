package com.strategyquant.tradinglib.blocks;

import com.strategyquant.lib.L;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.Blocks;
import com.strategyquant.tradinglib.formulas.Formulas;
import com.strategyquant.tradinglib.moneymanagement.MoneyManagementMethodsList;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlocksConfigGenerator {
   public static final Logger Log = LoggerFactory.getLogger(BlocksConfigGenerator.class);

   public void run(String var1) throws Exception {
      Log.debug("Regenerating Wizard config.xml");
      Document var2 = this.generateBasicDocument();
      Blocks.generateBlocksXml(var2.getRootElement());
      Formulas.generateFormulasXml(var2.getRootElement().getChild("Global").getChild("Formulas"));
      Blocks.generateExitRulesXml(var2.getRootElement().getChild("Global").getChild("ExitRules"));
      MoneyManagementMethodsList.generateMethodsXml(var2.getRootElement().getChild("Global").getChild("MoneyManagement"));
      this.addTaLibsIndicators(var2);
      XMLOutputter var3 = new XMLOutputter(Format.getPrettyFormat());
      BufferedOutputStream var4 = new BufferedOutputStream(new FileOutputStream(var1));
      var3.output(var2, var4);
      var4.close();
      Log.debug("Wizard config.xml has been regenerated successfully");
   }

   private void addTaLibsIndicators(Document var1) throws JDOMException, IOException, Exception {
      File var2 = new File(MainApp.getDataPath() + "internal/ctemplate/talibs.xml");
      if (var2.exists()) {
         Element var3 = XMLUtil.fileToXmlElement(var2).detach();
         Element var4 = var3.getChild("Candles");
         Element var5 = var3.getChild("Indicators");
         Element var6 = var3.getChild("Functions");
         Element var7 = var3.getChild("Prices");
         Element var8 = var1.getRootElement().getChild("Blocks").getChild("Values");
         List var9 = var8.getChildren("Category");
         Element var10 = this.getCategoryElement(var9, "Indicators");
         Element var11 = this.getCategoryElement(var9, "Functions");
         Element var12 = this.getCategoryElement(var9, "Price");
         Element var13 = var1.getRootElement().getChild("Blocks").getChild("Conditions");
         List var14 = var13.getChildren("Category");
         Element var15 = this.getCategoryElement(var14, "CandlePatterns");
         this.addTalib(var10, var5.getChildren());
         this.addTalib(var11, var6.getChildren());
         this.addTalib(var15, var4.getChildren());
         this.addTalib(var12, var7.getChildren());
      }
   }

   private void addTalib(Element var1, List<Element> var2) {
      if (var1 != null) {
         for (Element var4 : var2) {
            Element var5 = var4.clone();
            var1.addContent(var5);
         }
      }
   }

   private Element getCategoryElement(List<Element> var1, String var2) {
      for (Element var4 : var1) {
         Attribute var5 = var4.getAttribute("key");
         if (var5 != null && var2.equals(var5.getValue())) {
            return var4;
         }
      }

      return null;
   }

   private Document generateBasicDocument() throws Exception {
      SAXBuilder var1 = new SAXBuilder();
      var1.setIgnoringElementContentWhitespace(true);
      var1.setIgnoringBoundaryWhitespace(true);
      var1.setFeature("http://apache.org/xml/features/xinclude", true);
      File var2 = new File(MainApp.getDataPath() + "internal/ctemplate/config.xml");
      if (var2.exists()) {
         BufferedReader var3 = new BufferedReader(
            new InputStreamReader(new FileInputStream(MainApp.getDataPath() + "internal/ctemplate/config.xml"), StandardCharsets.UTF_8)
         );
         return var1.build(var3);
      } else {
         throw new Exception(L.t("File '%s internal/ctemplate/config.xml' doesn't exist", new Object[]{MainApp.getDataPath()}));
      }
   }
}
