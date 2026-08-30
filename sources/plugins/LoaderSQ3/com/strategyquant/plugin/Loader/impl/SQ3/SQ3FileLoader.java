package com.strategyquant.plugin.Loader.impl.SQ3;

import com.strategyquant.tradinglib.ResultsGroup;

public class SQ3FileLoader {
   private SQ3FileLoaderOldXml oldXmlLoader = null;

   public ResultsGroup load(String var1, boolean var2, String var3) throws Exception {
      if (this.oldXmlLoader == null) {
         this.oldXmlLoader = new SQ3FileLoaderOldXml();
      }

      return this.oldXmlLoader.load(var1, var2, var3);
   }
}
