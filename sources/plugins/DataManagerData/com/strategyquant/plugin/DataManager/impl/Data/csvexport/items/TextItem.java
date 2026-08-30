package com.strategyquant.plugin.DataManager.impl.Data.csvexport.items;

import com.strategyquant.datalib.data.io.VersatileData;

public class TextItem extends AbstractItem {
   public TextItem() {
      super("TextItem", "TextItem", "TextItem");
   }

   @Override
   public String printValue(String var1, VersatileData var2, int var3) {
      return this.value;
   }

   @Override
   public AbstractItem clone() {
      return new TextItem();
   }
}
