package com.strategyquant.tradinglib.generator;

import com.strategyquant.lib.IRandomGenerator;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class RandomAndOrList extends ArrayList<RandomAndOr> {
   public static final Logger Log = LoggerFactory.getLogger("RandomAndOrList");

   void setRAO(int var1, String var2) {
      int var3 = var2.hashCode();

      for (RandomAndOr var5 : this) {
         if ((var5.id == 0 || var5.id == var3) && var5.type == -1) {
            var5.type = var1;
         }
      }
   }

   String getGeneratedKey(int var1) {
      for (RandomAndOr var3 : this) {
         if (var3.type == 1 && var3.id == var1) {
            return var3.key;
         }
      }

      return null;
   }

   void fixValues() {
      for (RandomAndOr var2 : this) {
         if (var2.type == -1) {
            var2.type = 1;
         } else if (var2.type == 2 && !this.checkRandomIdExistsInList(var2.id)) {
            var2.type = 1;
         }
      }
   }

   private boolean checkRandomIdExistsInList(int var1) {
      for (RandomAndOr var3 : this) {
         if (var3.id == var1 && var3.type == 1) {
            return true;
         }
      }

      return false;
   }

   void replaceValues(int var1, IRandomGenerator var2, String var3) throws GenerateException {
      for (RandomAndOr var5 : this) {
         if (var1 == 1) {
            if (var5.type != 1) {
               continue;
            }

            var5.key = var2.nextInt(2) == 0 ? "AND" : "OR";
            var5.elRnd.setAttribute("key", var5.key);
         } else {
            if (var5.type != 2) {
               continue;
            }

            String var6 = this.getGeneratedKey(var5.id);
            if (var6 == null) {
               throw new GenerateException("This shouldn't happen!");
            }

            var5.elRnd.setAttribute("key", var6);
         }

         var5.elRnd.setAttribute("generated", var3);
      }
   }
}
