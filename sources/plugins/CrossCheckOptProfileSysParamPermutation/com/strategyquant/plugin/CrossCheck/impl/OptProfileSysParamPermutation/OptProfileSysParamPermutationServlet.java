package com.strategyquant.plugin.CrossCheck.impl.OptProfileSysParamPermutation;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OptProfileSysParamPermutationServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(OptProfileSysParamPermutationServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "getDefaultSPPConditions":
            return this.onGetDefaultSPPConditions();
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onGetDefaultSPPConditions() throws Exception {
      JSONObject var1 = new JSONObject();
      String var2 = SQPaths.pluginsDirPath + "/CrossCheckOptProfileSysParamPermutation/SPPDefaultConditions.xml";
      String var3 = SQUtils.fileToString(new File(var2));
      var1.put("conditions", var3);
      var1.put("success", "ok");
      return var1.toString();
   }
}
