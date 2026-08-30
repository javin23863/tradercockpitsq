package com.strategyquant.plugin.Settings.impl.AutomaticPortfolioBuilder;

import com.strategyquant.lib.L;
import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.SampleTypes;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterFitness;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterFitnessList;
import com.strategyquant.tradinglib.portfolioMaster.PortfolioMasterUtils;
import com.strategyquant.tradinglib.project.ProjectEngine;
import com.strategyquant.tradinglib.project.SQProject;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.math.BigInteger;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SettingsAutomaticPortfolioBuilderServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(SettingsAutomaticPortfolioBuilderServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "getNumberOfPossiblePortfolios":
            return this.getNumberOfPossiblePortfolios(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String getNumberOfPossiblePortfolios(Map<String, String[]> var1) throws Exception {
      JSONObject var2 = new JSONObject();
      int var3 = Integer.valueOf(this.tryGetParamValue(var1, "selected"));
      int var4 = Integer.valueOf(this.tryGetParamValue(var1, "min"));
      int var5 = Integer.valueOf(this.tryGetParamValue(var1, "max"));
      if (var3 < 0) {
         Databank var6 = this.getDatabank(var1);
         var3 = var6.size();
      }

      BigInteger var7 = PortfolioMasterUtils.calculateMaxNumberOfPortfolios(var3, var4, var5);
      var2.put("numberOfPossiblePortfolios", var7);
      var2.put("success", "ok");
      return var2.toString();
   }

   private Databank getDatabank(Map<String, String[]> var1) throws Exception {
      String var2 = this.tryGetParam(var1, "project")[0];
      String var3 = this.tryGetParam(var1, "databank")[0];
      SQProject var4 = ProjectEngine.get(var2);
      if (!var4.getDatabanks().containsKey(var3)) {
         throw new Exception(L.t("Databank '%s' doesn't exist.", new Object[]{var3}));
      } else {
         return (Databank)var4.getDatabanks().get(var3);
      }
   }

   public JSONArray getFitnessTypes() throws Exception {
      JSONArray var1 = new JSONArray();

      for (PortfolioMasterFitness var3 : PortfolioMasterFitnessList.getInstance().getAvailableClasses()) {
         String var4 = var3.getClass().getSimpleName();
         var1.put(
            new JSONObject().put("type", var4 + PortfolioMasterFitness.SampleKeyIS).put("name", var3.getName() + " " + SampleTypes.typeToFullString((byte)10))
         );
         var1.put(
            new JSONObject()
               .put("type", var4 + PortfolioMasterFitness.SampleKeyFull)
               .put("name", var3.getName() + " " + SampleTypes.typeToFullString((byte)127))
         );
      }

      return var1;
   }

   public static String sampleTypeKey(byte var0) {
      switch (var0) {
         case 10:
            return "IS";
         case 127:
            return "Full";
         default:
            return "IS";
      }
   }
}
