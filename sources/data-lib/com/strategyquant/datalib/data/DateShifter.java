package com.strategyquant.datalib.data;

import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.datalib.timezone.Timezone;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class DateShifter {
   private final DateTimeZone sourceTz;
   private final DateTimeZone targetTz;
   private int shiftHours = 0;

   public DateShifter(String var1, String var2) {
      if (var1 != null) {
         if (var1.equals("EETUS")) {
            var1 = "America/New_York";
            this.shiftHours = -7;
         }

         this.sourceTz = DateTimeZone.forID(Timezone.parseId(var1));
      } else {
         this.sourceTz = null;
      }

      if (var2 != null && !var2.equals("")) {
         if (var2.equals("EETUS")) {
            var2 = "America/New_York";
            this.shiftHours = 7;
         }

         this.targetTz = DateTimeZone.forID(Timezone.parseId(var2));
      } else {
         this.targetTz = null;
      }
   }

   public void transformToTimeZone(VersatileData var1) {
      if (this.sourceTz != null && this.targetTz != null) {
         DateTime var2 = new DateTime(var1.time).withZoneRetainFields(this.sourceTz);
         DateTime var3 = var2.withZone(this.targetTz).toLocalDateTime().toDateTime();
         if (this.shiftHours != 0) {
            var3 = var3.plusHours(this.shiftHours);
         }

         var1.time = var3.getMillis();
      }
   }
}
