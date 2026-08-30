package com.strategyquant.tradinglib.generator;

import com.strategyquant.tradinglib.Checker;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CheckersList extends ObjectArrayList<Checker> {
   private static final long serialVersionUID = 4262797492784530749L;
   public static final Logger Log = LoggerFactory.getLogger("CheckersList");

   public boolean check(Element var1) {
      for (int var2 = 0; var2 < this.size(); var2++) {
         Checker var3 = (Checker)this.get(var2);
         if (!var3.check(var1)) {
            return false;
         }
      }

      return true;
   }
}
