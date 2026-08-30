package com.strategyquant.wizard.desktop.loader;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.sourcecode.XMLIndicatorsAnalyzer;
import com.strategyquant.lib.utils.SQFileChooserFactory;
import com.strategyquant.tradinglib.strategy.xml.StrategyFixerWizardOld;
import com.strategyquant.wizard.desktop.settings.Settings;
import com.strategyquant.wizard.desktop.utils.FileUtils;
import freemarker.cache.FileTemplateLoader;
import freemarker.ext.dom.NodeModel;
import freemarker.template.Configuration;
import freemarker.template.TemplateMethodModel;
import freemarker.template.TemplateModelException;
import java.awt.Component;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.xml.sax.InputSource;

public class TransformEngineLoader {
   private Component parent;

   public TransformEngineLoader(Component var1) {
      this.parent = var1;
   }

   public String load() {
      String var1 = FileUtils.getAppFolder();
      File var2 = new File(FileUtils.getCodeFolder());
      File[] var3 = var2.listFiles();
      LinkedList var4 = new LinkedList();
      new Template();
      if (var3 != null) {
         for (File var9 : var3) {
            if (var9.isDirectory() && this.dirContainsMainTpl(var9)) {
               try {
                  var4.add(this.createTemplate(var9));
               } catch (IOException var11) {
                  var11.printStackTrace();
               }
            }
         }
      }

      StringBuilder var12 = new StringBuilder();
      var12.append("{\"templates\":[");
      boolean var13 = true;

      for (Template var15 : var4) {
         if (!var13) {
            var12.append(",");
         }

         var12.append(var15.toJson());
         var13 = false;
      }

      var12.append("]}");
      return var12.toString();
   }

   private boolean dirContainsMainTpl(File var1) {
      return new File(var1, "Main.tpl").exists();
   }

   private String getResult(boolean var1, String var2) {
      String var3 = var1 ? "success" : "failure";
      var2 = var2.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\\\n").replace("\r", "\\\\r").replace("\t", "\\\\t");
      return "{\"result\":\"" + var3 + "\",\"result\":\"" + var2 + "\"}";
   }

   public String transformCode(String var1, String var2) {
      try {
         String var3 = this.performTransformCode(var1, var2);
         return this.getResult(true, var3);
      } catch (Exception var4) {
         return this.getResult(false, var4.getMessage());
      }
   }

   public String performTransformCode(String var1, String var2) {
      try {
         Element var3 = XMLUtil.stringToXmlElement(SQUtils.fixRenamedActionParams(var1));
         XMLIndicatorsAnalyzer.init();
         XMLIndicatorsAnalyzer.fixItemAttributes(var3);
         var1 = XMLUtil.elementToString(var3);
         var1 = var1.replace("\n", "").replace("\r", "").replace("\t", "").replaceAll(" +", " ").replace("> <", "><");
         Configuration var4 = new Configuration(Configuration.VERSION_2_3_0);
         System.out.println("Using folder:" + FileUtils.getCodeFolder());
         String var5 = FileUtils.getAppFolder();
         FileTemplateLoader var6 = new FileTemplateLoader(new File(FileUtils.getCodeFolder()), true);
         var4.setTemplateLoader(var6);
         freemarker.template.Template var7 = null;

         try {
            System.out.println("Using template:" + var2 + "\\Main.tpl");
            var7 = var4.getTemplate(var2 + "\\Main.tpl");
         } catch (IOException var11) {
            throw new RuntimeException(L.t("Error while loading template - '%s'", new Object[]{var11.getMessage()}));
         }

         var4.setDefaultEncoding("UTF-8");
         InputSource var8 = new InputSource();
         var8.setCharacterStream(new StringReader(var1));
         HashMap var9 = new HashMap();
         var9.put("doc", NodeModel.parse(var8));
         var9.put("blockFileExists", new TransformEngineLoader.BlockFileExistsDirectivaModel(var2));
         StringWriter var10 = new StringWriter();
         var7.process(var9, var10);
         return var10.toString();
      } catch (Exception var12) {
         throw new RuntimeException(L.t("Error while generating template - '%s'", new Object[]{var12.getMessage()}));
      }
   }

   public void transformCode(String var1, String var2, String var3) throws JDOMException, Exception {
      File var4 = new File(var1);
      if (!var4.exists()) {
         System.err.println("File " + var1 + " doesn't exists.");
      } else {
         String var5 = null;

         try {
            var5 = new String(Files.readAllBytes(var4.toPath()));
            var5 = StrategyFixerWizardOld.fixStrategy(var5, FileUtils.getAppFolder() + "//branding//global//config.xml", true);
            int var6 = var5.indexOf("<Strategy ");
            String var7 = var5.substring(0, var6);
            String var8 = var5.substring(var6);
            String var9 = this.getInformationsXml(var4, var3);
            var5 = var7 + var9 + "<CustomForm></CustomForm>" + var8;
            var5 = var5.replace("\n", "").replace("\r", "").replace("\t", "").replaceAll(" +", " ").replace("> <", "><");
         } catch (IOException var11) {
            System.err.println("Error while reading file's content " + var1);
            return;
         }

         String var16 = this.performTransformCode(var5, var3);

         try {
            this.saveFile(var16, new File(var2));
            System.out.println("Saved file " + var2);
         } catch (IOException var10) {
            System.err.println("Error while writing result file");
         }
      }
   }

   private String getInformationsXml(File var1, String var2) {
      String var3 = var1.getName();
      return "<options><StrategyName>"
         + var3
         + "</StrategyName><Engine>"
         + var2
         + "</Engine><Version></Version><Date>"
         + new SimpleDateFormat("dd/mm/yyyy HH:MM").format(new Date())
         + "</Date></options>";
   }

   public void save(String var1, String var2, String var3) {
      String var4 = Settings.get().get("resultFolder");
      JFileChooser var5 = SQFileChooserFactory.createPersistingJFileChooser(var4.isEmpty() ? null : var4).getJFileChooser();
      FileNameExtensionFilter var6 = new FileNameExtensionFilter(var2, "*." + var3);
      var5.setFileFilter(var6);
      int var7 = var5.showSaveDialog(this.parent);
      if (var7 == 0) {
         File var8 = var5.getSelectedFile();
         Settings.get().set("resultFolder", var8.getParent());

         try {
            this.saveFile(var1, var8);
         } catch (IOException var10) {
            var10.printStackTrace();
         }
      }
   }

   private void saveFile(String var1, File var2) throws IOException {
      FileWriter var3 = null;
      var3 = new FileWriter(var2);
      var3.write(var1);
      var3.close();
   }

   private Template createTemplate(File var1) throws IOException {
      String var2 = new String(Files.readAllBytes(new File(var1, "Main.tpl").toPath()));
      String var3 = this.getStringBetween(var2, "<name>", "</name>");
      String var4 = this.getStringBetween(var2, "<extension>", "</extension>");
      String var5 = this.getStringBetween(var2, "<description>", "</description>");
      if (var5 != null) {
         var5 = var5.replaceAll("( +)", " ");
         var5 = var5.replaceAll("&lt;", "<");
         var5 = var5.replaceAll("&gt;", ">");
         var5 = var5.replace("\"", "'");
         var5 = var5.replace("\n", "");
         var5 = var5.replace("\r", "");
      } else {
         var5 = "";
      }

      String var6 = this.getStringBetween(var2, "<form>", "</form>");
      Template var7 = new Template();
      var7.setDescriptions(var5);
      var7.setName(var3);
      var7.setExtension(var4);
      var7.setId(var1.getName());
      var7.setForm(var6);
      return var7;
   }

   private String getStringBetween(String var1, String var2, String var3) {
      int var4 = var1.indexOf(var2) + var2.length();
      if (var4 < 0) {
         return null;
      }

      int var5 = var1.indexOf(var3, var4);
      return var5 >= 0 && var5 > var4 ? var1.substring(var4, var5) : null;
   }

   public void dispose() {
      this.parent = null;
   }

   public static class BlockFileExistsDirectivaModel implements TemplateMethodModel {
      private String engine;

      public BlockFileExistsDirectivaModel(String var1) {
         this.engine = var1;
      }

      public Object exec(List var1) throws TemplateModelException {
         String var2 = FileUtils.getCodeFolder();
         String var3 = var2 + "\\" + this.engine + "\\blocks\\";
         String var4 = (String)var1.get(0);
         boolean var5 = new File(var3, var4 + ".tpl").exists();
         return var5;
      }
   }
}
