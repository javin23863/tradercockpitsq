package com.strategyquant.datalib.data.io.columns;

public class VolumeCol extends DefaultCol {
   public VolumeCol() {
      super("Volume", "VOLUME");
   }

   @Override
   public int getType() {
      return 9;
   }

   @Override
   public int getDataType() {
      return 1;
   }
}
