package com.strategyquant.tradinglib.talib;

import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.tradinglib.BlockDefinitionException;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TALibBlocks {
   public static final Logger Log = LoggerFactory.getLogger("TALibBlocks");
   private static TALibBlocks instance;
   private HashMap<String, Element> talibBlocks = new HashMap<>();

   public static void init() throws Exception {
      if (instance != null) {
         throw new Exception("TALibBlocks.init() called more than once!");
      }

      instance = new TALibBlocks();
   }

   private TALibBlocks() throws Exception {
      String var1 = MainApp.getDataPath() + "internal/web/SQWIZARD/branding/global/config.xml";
      File var2 = new File(var1);
      Element var3 = XMLUtil.fileToXmlElement(var2);
      Element var4 = var3.getChild("Blocks");
      List var5 = var4.getChildren();

      for (int var6 = 0; var6 < var5.size(); var6++) {
         Element var7 = (Element)var5.get(var6);
         List var8 = var7.getChildren();

         for (int var9 = 0; var9 < var8.size(); var9++) {
            Element var10 = (Element)var8.get(var9);
            List var11 = var10.getChildren();

            for (int var12 = 0; var12 < var11.size(); var12++) {
               Element var13 = (Element)var11.get(var12);
               String var14 = var13.getAttributeValue("key");
               if (var14.startsWith("talib")) {
                  var13 = this.removeParamCategory(var13);
                  this.talibBlocks.put(var14, var13);
               }
            }
         }
      }

      Log.debug("Loading TALib blocks finished");
   }

   private Element removeParamCategory(Element var1) {
      Element var2 = var1.getChild("paramCategory");
      var2.detach();
      List var3 = var2.getChildren();

      for (Attribute var6 : var2.getAttributes()) {
         var2.removeAttribute(var6);
      }

      for (Attribute var9 : var1.getAttributes()) {
         var2.setAttribute(var9.getName(), var9.getValue());
      }

      var2.setName("Item");
      return var2;
   }

   public static Element getBlock(String var0) throws BlockDefinitionException {
      return instance._getBlock(var0);
   }

   private Element _getBlock(String var1) throws BlockDefinitionException {
      if (this.talibBlocks.containsKey(var1)) {
         return this.talibBlocks.get(var1);
      } else {
         throw new BlockDefinitionException("Cannot find TALib block '" + var1 + "'");
      }
   }
}
