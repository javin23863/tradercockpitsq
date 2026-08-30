package com.strategyquant.plugin.Loader.impl.SQ3;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.whitelabel.AbstractBroker;
import com.strategyquant.tradinglib.ResultsGroup;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQ3FileLoaderOldXml {
   public static final Logger Log = LoggerFactory.getLogger("SQFileLoaderOldXml");

   public ResultsGroup load(String var1, boolean var2, String var3) throws Exception {
      File var4 = new File(var1);
      SAXBuilder var5 = new SAXBuilder();
      BufferedReader var6 = new BufferedReader(new InputStreamReader(new FileInputStream(var4), StandardCharsets.UTF_8));
      Document var7 = var5.build(var6);
      Element var8 = var7.getRootElement();
      AbstractBroker var9 = MainApp.v571hfnsHw().inygRmSMx7();
      if (var9 != null) {
         var9.modifyOldStrategyXML(var8);
      }

      ResultsGroup var10 = new ResultsGroup(SQUtils.stripExtension(var4.getName()));
      if (var8.getChild("Strategy") == null && var8.getChild("ResultsData") != null) {
         var10.specialValues().setString("SourceType", "EA Analyzer 3");
      } else {
         var10.specialValues().setString("SourceType", "EA StrategyQuant 3");
         SQFileLoaderSQ3Xml var11 = new SQFileLoaderSQ3Xml();
         var11.load(var10, var8, var2, var3);
      }

      return var10;
   }
}
