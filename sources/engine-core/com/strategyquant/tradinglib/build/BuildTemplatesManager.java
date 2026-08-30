package com.strategyquant.tradinglib.build;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import java.io.File;
import java.util.ArrayList;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BuildTemplatesManager {
   private static final Logger Log = LoggerFactory.getLogger(BuildTemplatesManager.class);
   private static final String templatesElementName = "Templates";
   private static final String templatesXMLPath = SQPaths.buildTemplatesDirPath + "/templates.xml";
   private ArrayList<BuildTemplate> templates = new ArrayList<>();
   private static BuildTemplatesManager instance;

   public BuildTemplatesManager() {
      try {
         this.loadTemplates();
      } catch (Exception var2) {
         Log.error("Cannot load templates. ", var2);
      }

      instance = this;
   }

   private static BuildTemplatesManager get() {
      if (instance == null) {
         new BuildTemplatesManager();
      }

      return instance;
   }

   public static ArrayList<BuildTemplate> getTemplates() {
      return get().templates;
   }

   public static Element getTemplateXML(String var0) throws Exception {
      for (BuildTemplate var2 : get().templates) {
         if (var2.name.equals(var0)) {
            String var3 = SQPaths.buildTemplatesDirPath + "/" + var2.filename;
            return XMLUtil.fileToXmlElement(new File(var3));
         }
      }

      throw new Exception(L.t("Template with name '%s' doesn't exist.", new Object[]{var0}));
   }

   public static void saveTemplates() throws Exception {
      get()._saveTemplates();
   }

   private void loadTemplates() throws Exception {
      Element var1 = XMLUtil.fileToXmlElement(new File(templatesXMLPath));
      this.templates.clear();

      for (Element var3 : XMLUtil.getNestedElements(var1, "Template")) {
         BuildTemplate var4 = new BuildTemplate();
         var4.setFromXML(var3);
         this.templates.add(var4);
      }
   }

   private void _saveTemplates() throws Exception {
      Element var1 = new Element("Templates");

      for (BuildTemplate var3 : this.templates) {
         var1.addContent(var3.getXML());
      }

      File var4 = SQUtils.createFile(templatesXMLPath);
   }
}
