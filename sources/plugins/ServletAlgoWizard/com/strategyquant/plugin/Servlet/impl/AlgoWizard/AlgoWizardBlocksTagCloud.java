package com.strategyquant.plugin.Servlet.impl.AlgoWizard;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import java.io.File;
import java.util.HashMap;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AlgoWizardBlocksTagCloud {
   private static final Logger Log = LoggerFactory.getLogger(AlgoWizardBlocksTagCloud.class);
   public static final String configFilePath = SQPaths.settingsDirPath + "/AlgoWizardBlocksTagCloud.xml";
   private static AlgoWizardBlocksTagCloud instance = null;
   private HashMap<String, Integer> loadedBlocks = new HashMap<>();

   private AlgoWizardBlocksTagCloud() {
      instance = this;

      try {
         this.load();
      } catch (Exception var2) {
         Log.error("Cannot load AlgoWizard Blocks Tag Cloud", var2);
      }
   }

   private void load() throws Exception {
      this.loadedBlocks.clear();
      File var1 = new File(configFilePath);
      if (!var1.exists()) {
         Log.info("AlgoWizardBlocksTagCloud.xml doesn't exist yet");
      } else {
         Element var2 = XMLUtil.fileToXmlElement(var1);

         for (Element var4 : var2.getChildren("Block")) {
            try {
               this.loadedBlocks.put(var4.getText(), Integer.parseInt(var4.getAttributeValue("number")));
            } catch (Exception var6) {
               Log.error("Cannot load block", var6);
            }
         }
      }
   }

   public static JSONArray listJSON() {
      JSONArray var0 = new JSONArray();

      for (String var2 : get().loadedBlocks.keySet()) {
         JSONObject var3 = new JSONObject();
         var3.put("block", var2);
         var3.put("number", get().loadedBlocks.get(var2));
         var0.put(var3);
      }

      return var0;
   }

   public static void save(String var0, String var1, String var2) {
      get()._save(var0, var1, var2);
   }

   private static AlgoWizardBlocksTagCloud get() {
      if (instance == null) {
         instance = new AlgoWizardBlocksTagCloud();
      }

      return instance;
   }

   public void _save(String var1, String var2, String var3) {
      String var4 = var1 + "++" + var2 + "++" + var3;
      if (this.loadedBlocks.containsKey(var4)) {
         this.loadedBlocks.put(var4, this.loadedBlocks.get(var4) + 1);
      } else {
         this.loadedBlocks.put(var4, 1);
      }

      Element var5 = new Element("Blocks");

      for (String var7 : this.loadedBlocks.keySet()) {
         Element var8 = new Element("Block");
         var8.setText(var7);
         var8.setAttribute("number", this.loadedBlocks.get(var7).toString());
         var5.addContent(var8);
      }

      SQUtils.stringToFile(new File(configFilePath), XMLUtil.elementToString(var5));
   }
}
