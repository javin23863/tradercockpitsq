package com.strategyquant.tradinglib.engine.stockpicker.signals.exit;

import com.strategyquant.tradinglib.Directions;
import com.strategyquant.tradinglib.OrderCloseTypes;

public class PickerExitSignal {
   public String symbol;
   public byte direction;
   public byte closeType;

   public PickerExitSignal(String var1, byte var2, byte var3) {
      this.symbol = var1;
      this.direction = var2;
      this.closeType = var3;
   }

   @Override
   public String toString() {
      StringBuilder var1 = new StringBuilder();
      this.toString(var1);
      return var1.toString();
   }

   public void toString(StringBuilder var1) {
      var1.append(this.symbol);
      var1.append(" [");
      var1.append(OrderCloseTypes.toString(this.closeType));
      var1.append(", ");
      var1.append(Directions.typeToString(this.direction));
      var1.append("]");
   }
}
