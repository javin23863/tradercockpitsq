package com.strategyquant.tradinglib.generator;

import com.strategyquant.lib.IRandomGenerator;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import java.io.File;
import java.io.IOException;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StrategyStockpickerSingleAssetTemplate extends StrategyStockpickerTemplate {
   public static final Logger Log = LoggerFactory.getLogger("StrategyStockpickerTemplate");

   public StrategyStockpickerSingleAssetTemplate(Element var1, IRandomGenerator var2) {
      super(var1, var2);
   }

   @Override
   protected void addRules(Element var1) throws Exception {
      this.addRules(var1, "entryexitrule.xml");
      this.addPositionScoreRule(var1, "positionscorerulelong.xml");
      this.addPositionScoreRule(var1, "positionscoreruleshort.xml");
   }

   private void addPositionScoreRule(Element var1, String var2) throws JDOMException, IOException, Exception {
      Element var3 = XMLUtil.fileToXmlElement(new File(MainApp.getDataPath() + "internal/ctemplate/build/sp/" + var2)).detach();
      var1.addContent(var3);
   }

   @Override
   public Element generate() throws Exception {
      Element var1 = super.generate();
      Element var2 = var1.getChild("Strategy");
      var2.setAttribute("singleAsset", "true");
      this.setExitsProbability(var1);
      return var1;
   }
}
