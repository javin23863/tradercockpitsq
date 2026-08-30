package com.strategyquant.plugin.Saver.impl.SQ3;

import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.file.ISaverPlugin;
import java.util.Map;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Mark Fric")
@Name(name = "SQ3SaverPlugin plugin")
@Category(name = "File Saver")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SQ3SaverPlugin implements ISaverPlugin {
   public static final Logger Log = LoggerFactory.getLogger(SQ3SaverPlugin.class);
   public static final String[] extensions = new String[]{"str"};

   public String getProduct() {
      return "any";
   }

   public int getPreferredPosition() {
      return 0;
   }

   public void initPlugin() throws Exception {
   }

   public String[] getFileExtensions() {
      return extensions;
   }

   public void save(ResultsGroup var1, String var2, Map<String, String[]> var3) throws Exception {
      try {
         SQ3FileSaver var4 = new SQ3FileSaver();
         var4.save(var1, var2, var3);
      } catch (Exception var5) {
         Log.error("Failed to save SQ3 file '" + var2 + "'. Exc: ", var5);
         throw new Exception("Failed to save SQ3 file '" + var2 + "' - " + var5.getMessage());
      }
   }

   public ISaverPlugin clone() {
      return new SQ3SaverPlugin();
   }
}
