package com.strategyquant.tradinglib.whatif;

import com.strategyquant.lib.snippets.CustomClasses;
import com.strategyquant.tradinglib.WhatIf;

public class WhatIfMethodsList extends CustomClasses<WhatIf> {
   private static WhatIfMethodsList instance;

   public static WhatIfMethodsList get() {
      if (instance == null) {
         instance = new WhatIfMethodsList();
      }

      return instance;
   }

   private WhatIfMethodsList() {
      this.setDirName("WhatIf");
      this.setExpectedClassType(WhatIf.class);
      this.loadAvailableClasses();
   }
}
