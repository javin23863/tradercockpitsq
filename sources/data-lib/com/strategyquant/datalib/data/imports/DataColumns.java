package com.strategyquant.datalib.data.imports;

import com.strategyquant.datalib.data.io.columns.AskCol;
import com.strategyquant.datalib.data.io.columns.BidCol;
import com.strategyquant.datalib.data.io.columns.CloseCol;
import com.strategyquant.datalib.data.io.columns.CustomValue;
import com.strategyquant.datalib.data.io.columns.DateCol;
import com.strategyquant.datalib.data.io.columns.DateTimeCol;
import com.strategyquant.datalib.data.io.columns.DefaultCol;
import com.strategyquant.datalib.data.io.columns.HighCol;
import com.strategyquant.datalib.data.io.columns.LowCol;
import com.strategyquant.datalib.data.io.columns.OpenCol;
import com.strategyquant.datalib.data.io.columns.TimeCol;
import com.strategyquant.datalib.data.io.columns.UnusedCol;
import com.strategyquant.datalib.data.io.columns.VolumeCol;
import java.util.ArrayList;

public class DataColumns {
   private static DataColumns instance = new DataColumns();
   private ArrayList<DefaultCol> availableColTypes = new ArrayList<>();

   private DataColumns() {
      this.initAvailableColTypes();
   }

   public ArrayList<DefaultCol> getAvailableColTypes() {
      return this.availableColTypes;
   }

   public static DataColumns getInstance() {
      return instance;
   }

   private void initAvailableColTypes() {
      this.availableColTypes.add(new AskCol());
      this.availableColTypes.add(new BidCol());
      this.availableColTypes.add(new CloseCol());
      this.availableColTypes.add(new DateCol());
      this.availableColTypes.add(new TimeCol());
      this.availableColTypes.add(new DateTimeCol());
      this.availableColTypes.add(new HighCol());
      this.availableColTypes.add(new LowCol());
      this.availableColTypes.add(new OpenCol());
      this.availableColTypes.add(new UnusedCol());
      this.availableColTypes.add(new VolumeCol());

      for (int var1 = 1; var1 < 30; var1++) {
         this.availableColTypes.add(new CustomValue(var1));
      }
   }

   public DefaultCol findColTypeByName(String var1) {
      for (DefaultCol var3 : this.availableColTypes) {
         if (var3.getName().equalsIgnoreCase(var1)) {
            return var3;
         }
      }

      return null;
   }
}
