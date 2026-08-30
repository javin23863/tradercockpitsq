package com.strategyquant.tradinglib;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NegatersList extends ObjectArrayList<Negater> {
   public static final Logger Log = LoggerFactory.getLogger("NegatersList");

   public IBlock negate(IBlock var1, StrategyBase var2) throws BlockDefinitionException {
      int var3;
      int var4;
      try {
         var3 = Blocks.getBlockType(var1);
         var4 = Blocks.getReturnType(var1);
      } catch (Exception var10) {
         throw new BlockDefinitionException("Cannot get block type!", var10);
      }

      for (int var5 = 0; var5 < this.size(); var5++) {
         Negater var6 = (Negater)this.get(var5);
         Log.debug("- Using negater : " + var6.getClass().getSimpleName() + " for block " + var1.getClass().getSimpleName());

         IBlock var7;
         try {
            var7 = var6.negate(this, var1, var3, var4, var2);
         } catch (BlockDefinitionException var9) {
            Log.info(" - Negater " + var6.getClass().getSimpleName() + " exception, continuing.", var9);
            return null;
         }

         if (var7 != null) {
            return var7;
         }
      }

      throw new BlockDefinitionException("No Negater worked for block '" + var1.getClass().getSimpleName() + "'");
   }
}
