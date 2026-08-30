package com.strategyquant.tradinglib.task.settings.blocks;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.snippets.CustomClassesLoader;
import com.strategyquant.lib.snippets.compile.SQStructure;
import com.strategyquant.tradinglib.IBlock;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BlocksList {
   private static final Logger Log = LoggerFactory.getLogger("BlocksList");

   public static ArrayList<IBlock> getBlocks(String var0) {
      String var1 = SQStructure.getCompiledCustomDirPath(var0);
      ArrayList var2 = new ArrayList();
      loadClasses(var2, var0);

      for (String var4 : SQUtils.listSubdirectories(var1)) {
         loadClasses(var2, var0 + "/" + var4);
      }

      return var2;
   }

   private static void loadClasses(ArrayList<IBlock> var0, String var1) {
      try {
         CustomClassesLoader var2 = new CustomClassesLoader(var1);

         while (var2.hasNext()) {
            String var3 = var2.getNext();
            Object var4 = var2.createInstance(var3);
            if (var4 instanceof IBlock) {
               IBlock var5 = (IBlock)var4;
               var0.add(var5);
            } else if (Log.isDebugEnabled()) {
               Log.debug("Class: " + var3 + " is not an instance of block");
            }
         }
      } catch (Exception var6) {
         Log.error("An error occured while loading custom classes.", var6);
      }
   }
}
