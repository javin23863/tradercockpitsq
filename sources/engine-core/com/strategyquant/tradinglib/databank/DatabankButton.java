package com.strategyquant.tradinglib.databank;

import com.strategyquant.tradinglib.Databank;
import com.strategyquant.tradinglib.ResultsGroup;
import java.util.ArrayList;
import java.util.Comparator;

public abstract class DatabankButton {
   private String name = "No Name";
   public static Comparator<DatabankButton> PositionComparator = new Comparator<DatabankButton>() {
      public int compare(DatabankButton var1, DatabankButton var2) {
         int var3 = var1.getPreferredPosition();
         int var4 = var2.getPreferredPosition();
         return var3 > var4 ? 1 : (var3 < var4 ? -1 : 0);
      }
   };

   public void setName(String var1) {
      this.name = var1;
   }

   public String getName() {
      return this.name;
   }

   public String[] parseName() {
      return this.name.split(":");
   }

   public abstract void execute(Databank var1, ArrayList<ResultsGroup> var2) throws Exception;

   public String getProduct() {
      return "*";
   }

   public int getPreferredPosition() {
      return 100;
   }
}
