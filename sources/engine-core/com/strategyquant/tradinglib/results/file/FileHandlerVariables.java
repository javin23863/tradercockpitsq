package com.strategyquant.tradinglib.results.file;

import com.strategyquant.tradinglib.ResultsGroup;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.ArrayList;

public class FileHandlerVariables {
   public ResultsGroup loadedResult = null;
   public String loadedFilePath = null;
   public ILoaderPlugin pluginLoaderUsed = null;
   public String loadingErrorMessages = null;
   public Exception lastLoaderExc = null;
   public Int2ObjectOpenHashMap<ArrayList<ILoaderPlugin>> mapLoaders = new Int2ObjectOpenHashMap();
}
