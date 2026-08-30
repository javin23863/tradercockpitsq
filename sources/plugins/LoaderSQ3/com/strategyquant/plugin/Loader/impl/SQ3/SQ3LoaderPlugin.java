package com.strategyquant.plugin.Loader.impl.SQ3;

import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.pluginlib.annotations.Category;
import com.strategyquant.pluginlib.annotations.License;
import com.strategyquant.pluginlib.annotations.Name;
import com.strategyquant.pluginlib.annotations.ShortDesc;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.results.file.ILoaderPlugin;
import com.strategyquant.tradinglib.results.file.MissingInstrumentException;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.meta.Author;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Author(name = "Mark Fric")
@Name(name = "SQ3LoaderPlugin plugin")
@Category(name = "File Loader")
@License(text = "")
@ShortDesc(text = "Short description")
@PluginImplementation
public class SQ3LoaderPlugin implements ILoaderPlugin {
   public static final Logger Log = LoggerFactory.getLogger("SQ3LoaderPlugin");
   public static final String[] extensions = new String[]{"str", "stp", "sqa"};

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

   public ResultsGroup load(String var1, boolean var2, String var3, String var4) throws Exception {
      try {
         SQ3FileLoader var5 = new SQ3FileLoader();
         ResultsGroup var6 = var5.load(var1, var2, var3);
         var6.computeAllStats();
         String var7 = SQUtils.getExtension(var1).toLowerCase();
         if (var7.equals("str")) {
         }

         return var6;
      } catch (MissingInstrumentException var8) {
         throw var8;
      } catch (Exception var9) {
         Log.error("Failed to load SQStrategy report file: " + var1 + " Exc: ", var9);
         throw new Exception(L.t("Failed to load SQStrategy report file: %s - %s", new Object[]{var1, var9.getMessage()}));
      }
   }

   public ResultsGroup finishLoad(ResultsGroup var1) throws Exception {
      return var1;
   }
}
