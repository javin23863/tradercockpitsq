package com.strategyquant.plugin.Saver.impl.PDF;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.ReportGenerator;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.StatsTypeCombination;
import com.strategyquant.tradinglib.results.file.ISaverPlugin;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zefer.pd4ml.PD4ML;

@Author(name = "Mark Fric")
@Name(name = "PDF report")
@Category(name = "File Saver")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class PDFReportPlugin extends ReportGenerator implements ISaverPlugin, IProgram {
   public static final Logger Log = LoggerFactory.getLogger(PDFReportPlugin.class);
   public static final String[] extensions = new String[]{"pdf"};

   public PDFReportPlugin() {
      super("pdf");

      try {
         this.setName(L.tsq("SQ Default"));
         this.setHtmlTemplateName("sqdefault_template.htm");
         String var1 = MainApp.getDataPath() + "internal/plugins/SaverHTML/templates/" + this.htmlTemplateName;
         this.templateOrig = SQUtils.fileToString(new File(var1));
      } catch (Exception var2) {
         Log.error("Overview template couldn't be loaded. Exc. ", var2);
      }
   }

   public String getProduct() {
      return "any";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
      Program.register("SaverPDF", this);
   }

   public String[] getFileExtensions() {
      return extensions;
   }

   public Object call(String var1, Object... var2) throws Exception {
      if (var1.equals("save")) {
         this.save((ResultsGroup)var2[0], (String)var2[1], (Map<String, String[]>)var2[2]);
      } else if (var1.equals("resolveCharts")) {
         this.resolveCharts((JSONObject)var2[0]);
      }

      return null;
   }

   public void save(ResultsGroup var1, String var2, Map<String, String[]> var3) throws Exception {
      try {
         PD4ML var4 = new PD4ML();
         String var5 = SQStructure.TEMP_DIR_PATH + "pdf_" + UUID.randomUUID() + "/";
         String var6 = var5 + SQUtils.stripExtension(new File(var2).getName()) + ".html";
         this.file = new File(var6);
         if (!this.file.exists() && this.file.getParentFile() != null) {
            this.file.getParentFile().mkdirs();
         }

         String var7 = this.drawValues(var1, "Portfolio", new StatsTypeCombination((byte)0, (byte)10, (byte)127));
         SQUtils.stringToFile(var6, var7);
         this.file = new File(var2);
         if (!this.file.exists() && this.file.getParentFile() != null) {
            this.file.getParentFile().mkdirs();
         }

         InputStreamReader var8 = new InputStreamReader(new FileInputStream(var6), StandardCharsets.UTF_8);
         FileOutputStream var9 = new FileOutputStream(this.file);
         URL var10 = new URL("file:" + var5);
         var4.adjustHtmlWidth();
         var4.enableImgSplit(false);
         var4.render(var8, var9, var10);
         Log.info("Saved to PDF at {}, temp: {}", var2, var5);
         SQUtils.deleteDirectory(var5);
      } catch (Exception var11) {
         Log.error("Failed to save report as PDF '" + var1.getName() + "'. Exc: ", var11);
         throw new Exception("\"Failed to save report as PDF '" + var1.getName() + "'. Reason: " + var11.getMessage());
      }
   }

   public ISaverPlugin clone() {
      return new PDFReportPlugin();
   }
}
