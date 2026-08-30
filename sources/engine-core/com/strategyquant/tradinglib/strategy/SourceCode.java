package com.strategyquant.tradinglib.strategy;

import com.strategyquant.datalib.customData.CustomDataManager;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.constants.SQPaths;
import com.strategyquant.lib.sourcecode.SourceCodeGenerator;
import com.strategyquant.lib.sourcecode.SourceCodeGenerators;
import com.strategyquant.lib.sourcecode.XMLIndicatorsAnalyzer;
import com.strategyquant.pluginlib.program.IProgram;
import com.strategyquant.pluginlib.program.Program;
import com.strategyquant.tradinglib.ResultsGroup;
import com.strategyquant.tradinglib.blocks.CustomBlocks;
import com.strategyquant.tradinglib.project.StrategyXMLModifier;
import com.strategyquant.tradinglib.strategy.xml.StrategyFixer;
import java.io.File;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceCode {
   private static final Logger Log = LoggerFactory.getLogger(SourceCode.class);

   public void generate(String var1, String var2, String var3) throws Exception {
      if (var1 == null || var2 == null || var3 == null) {
         if (var1 == null) {
            throw new Exception("generatorName cannot be null");
         }

         if (var2 == null) {
            throw new Exception("inputFileName cannot be null");
         }

         if (var3 == null) {
            throw new Exception("outputFileName cannot be null");
         }
      }

      SourceCodeGenerator var4 = SourceCodeGenerators.getInstance().getGeneratorFromName(getGeneratorName(var1));
      if (var4 != null) {
         Log.info("Generating " + var1 + " code...");

         try {
            File var5 = new File(var2);
            String var6 = var5.getName();
            String var7 = null;
            if (var6.toLowerCase().endsWith(".sqx")) {
               IProgram var8 = Program.get("Loader");
               ResultsGroup var9 = (ResultsGroup)var8.call("loadFile", new Object[]{var5.getAbsolutePath(), false});
               var7 = XMLUtil.elementToString(StrategyXMLModifier.getUpdatedStrategyXML(var9));
            } else {
               var7 = SQUtils.fileToString(var5);
            }

            Element var13 = StrategyFixer.fixStrategy(var7);
            var13.setAttribute("hasHeikenAshi", var7.contains("HeikenAshi") ? "true" : "false");
            var13.setAttribute("isBrazilianVersion", var4.isBrazilianVersion() ? "true" : "false");
            Element var14 = var13.getChild("Strategy");
            var14.setAttribute("Engine", var1);
            StrategyXMLModifier.addOptions(var13, var6, var5.getAbsolutePath());
            XMLIndicatorsAnalyzer.init();
            XMLIndicatorsAnalyzer.fixItemAttributes(var14);
            CustomDataManager.init(SQPaths.customDataDirPath);
            CustomDataManager.addCDataIndySourceCodes(var13);
            CustomBlocks.addCBlockSourceCodes(var13);
            String var10 = var4.getSource(SQUtils.stripExtension(var6), var13);
            SQUtils.stringToFile(new File(var3), var10);
            Log.info("Source code generated");
         } catch (Exception var11) {
            Log.error("Generation failed", var11);
            throw new Exception("Generation failed - " + var11.getMessage(), var11);
         }
      } else {
         Log.error("Generator '" + var1 + "' not found");
         throw new Exception("Generator '" + var1 + "' not found");
      }
   }

   private static String getGeneratorName(String var0) {
      switch (var0) {
         case "MetaTrader4":
            return SourceCodeGenerator.MetaTrader4;
         case "MetaTrader5":
            return SourceCodeGenerator.MetaTrader5;
         case "EasyLanguage":
            return SourceCodeGenerator.EasyLanguage;
         case "PseudoCode":
            return SourceCodeGenerator.PseudoCode;
         default:
            return var0;
      }
   }
}
