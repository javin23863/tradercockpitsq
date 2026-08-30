package SQ.Functions;

import com.strategyquant.tradinglib.Order;
import java.util.Comparator;

public class ComparatorByProfit implements Comparator<Order> {
   private byte type = 10;

   public ComparatorByProfit(byte var1) {
      this.type = var1;
   }

   public int compare(Order var1, Order var2) {
      if (this.type == 30) {
         if (var1.PipsPL > var2.PipsPL) {
            return -1;
         }

         if (var1.PipsPL < var2.PipsPL) {
            return 1;
         }
      } else if (this.type == 20) {
         if (var1.PctPL > var2.PctPL) {
            return -1;
         }

         if (var1.PctPL < var2.PctPL) {
            return 1;
         }
      } else {
         if (var1.PL > var2.PL) {
            return -1;
         }

         if (var1.PL < var2.PL) {
            return 1;
         }
      }

      return 0;
   }
}
