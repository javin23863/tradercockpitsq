package com.strategyquant.plugin.Saver.impl.HTML;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
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
import java.util.Map;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Mark Fric")
@Name(name = "HTML report")
@Category(name = "File Saver")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class HTMLReportPlugin extends ReportGenerator implements ISaverPlugin, IProgram {
   public static final Logger Log = LoggerFactory.getLogger(HTMLReportPlugin.class);
   public static final String[] extensions = new String[]{"html"};

   public HTMLReportPlugin() {
      super("html");

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
      Program.register("SaverHTML", this);
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
         this.file = new File(var2);
         if (!this.file.exists() && this.file.getParentFile() != null) {
            this.file.getParentFile().mkdirs();
         }

         String var4 = this.drawValues(var1, "Portfolio", new StatsTypeCombination((byte)0, (byte)10, (byte)127));
         Log.info("---- SAVING TO FILE {}", this.file.getAbsolutePath());
         SQUtils.stringToFile(this.file, var4);
      } catch (Exception var5) {
         Log.error("Failed to save report as HTML '" + var1.getName() + "'. Exc: ", var5);
         throw new Exception("\"Failed to save report as HTML '" + var1.getName() + "'. Reason: " + var5.getMessage());
      }
   }

   public ISaverPlugin clone() {
      return new HTMLReportPlugin();
   }
}
