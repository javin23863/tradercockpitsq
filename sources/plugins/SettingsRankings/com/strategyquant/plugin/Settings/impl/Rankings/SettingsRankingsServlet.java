package com.strategyquant.plugin.Settings.impl.Rankings;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.pluginlib.SQPluginManager;
import com.strategyquant.tradinglib.crosscheck.ICrossCheck;
import com.strategyquant.tradinglib.fitnessfunction.IFitnessFunction;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.util.Map;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsRankingsServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(SettingsRankingsServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "getDefaultWFConditions":
            return this.onGetDefaultWFConditions(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   public JSONArray getFitnessMethods() throws Exception {
      JSONArray var1 = new JSONArray();

      for (IFitnessFunction var3 : SQPluginManager.getPlugins(IFitnessFunction.class)) {
         if (!var3.getFitnessKey().equals("ComputeFromWFResult")) {
            var1.put(
               new JSONObject()
                  .put("value", var3.getFitnessKey())
                  .put("name", var3.getFitnessName())
                  .put("crosscheck", ICrossCheck.class.isAssignableFrom(var3.getClass()))
            );
         }
      }

      return var1;
   }

   private String onGetDefaultWFConditions(Map<String, String[]> var1) throws Exception {
      this.checkParamExists(var1, new String[]{"conditions", "action"});
      String var2 = ((String[])var1.get("conditions"))[0].toString();
      String var3 = ((String[])var1.get("action"))[0].toString();
      JSONObject var4 = new JSONObject();
      Element var5 = null;
      if (var3.equals("replace")) {
         var5 = XMLUtil.fileToXmlElement(new File(SQPaths.wfDefaultConditionsPath));
      } else {
         var5 = XMLUtil.stringToElement(var2);
         Element var6 = XMLUtil.fileToXmlElement(new File(SQPaths.wfDefaultConditionsPath));

         for (Element var8 : var6.getChildren("Condition")) {
            var5.addContent(var8.clone());
         }
      }

      var4.put("conditions", XMLUtil.elementToString(var5));
      var4.put("success", "ok");
      return var4.toString();
   }
}
