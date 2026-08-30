package SQ.Blocks.Price;

import com.strategyquant.datalib.TradingException;
import com.strategyquant.lib.SQTime;
import com.strategyquant.tradinglib.ChartData;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionOHLCCalculator {
   public static final Logger Log = LoggerFactory.getLogger(SessionOHLCCalculator.class);
   private static final long DAY_MILLIS = 86400000L;
   public static final byte OPEN = 1;
   public static final byte HIGH = 2;
   public static final byte LOW = 3;
   public static final byte CLOSE = 4;
   private byte type;
   private int startHours;
   private int startMinutes;
   private int endHours;
   private int endMinutes;
   private int daysShift;
   private ChartData Chart;
   int startHHMM = -1;
   int endHHMM = -1;
   private long sessionStartTime = -1L;
   private long sessionEndTime = -1L;
   private ArrayList<Double> sessionOpen = null;
   private ArrayList<Double> sessionHigh = null;
   private ArrayList<Double> sessionLow = null;
   private ArrayList<Double> sessionClose = null;
   private boolean waitingForSession = true;
   private int dataShift = 0;
   private int lastCalculatedBar = 0;

   public SessionOHLCCalculator() {
      this.Chart = null;
   }

   public SessionOHLCCalculator(byte var1, int var2, int var3, int var4, int var5, int var6, ChartData var7) {
      this.type = var1;
      this.startHours = var2;
      this.startMinutes = var3;
      this.endHours = var4;
      this.endMinutes = var5;
      this.daysShift = var6;
      this.Chart = var7;
      this.startHHMM = var2 * 100 + var3;
      this.endHHMM = var4 * 100 + var5;
      this.sessionOpen = new ArrayList<>();
      this.sessionHigh = new ArrayList<>();
      this.sessionLow = new ArrayList<>();
      this.sessionClose = new ArrayList<>();
   }

   public double get() throws TradingException {
      if (this.Chart == null) {
         return 0.0;
      }

      int var1 = this.Chart.Time.size();
      if (var1 > this.lastCalculatedBar) {
         for (this.dataShift = var1 - this.lastCalculatedBar; this.dataShift >= 0; this.dataShift--) {
            this.calculate();
         }

         this.lastCalculatedBar = var1;
      }

      if (this.daysShift >= this.sessionOpen.size()) {
         return 0.0;
      }

      if (this.sessionOpen.size() > this.daysShift + 1) {
         int var2 = this.sessionOpen.size() - 1;
         this.sessionOpen.remove(var2);
         this.sessionHigh.remove(var2);
         this.sessionLow.remove(var2);
         this.sessionClose.remove(var2);
      }

      switch (this.type) {
         case 1:
            return this.sessionOpen.get(this.daysShift);
         case 2:
            return this.sessionHigh.get(this.daysShift);
         case 3:
            return this.sessionLow.get(this.daysShift);
         case 4:
            return this.sessionClose.get(this.daysShift);
         default:
            return 0.0;
      }
   }

   private void calculate() throws TradingException {
      long var1 = this.Chart.Time(this.dataShift);
      if (var1 >= this.sessionEndTime) {
         if (!this.sessionClose.isEmpty()) {
            this.sessionClose.set(0, this.Chart.Close(this.dataShift + 1));
            long var3 = this.Chart.Time(this.dataShift + 1);
            if (var3 >= this.sessionStartTime && var3 < this.sessionEndTime) {
               this.sessionHigh.set(0, Math.max(this.Chart.High(this.dataShift + 1), this.sessionHigh.get(0)));
               this.sessionLow.set(0, Math.min(this.Chart.Low(this.dataShift + 1), this.sessionLow.get(0)));
            }
         }

         this.calculateSessionTimes(var1);
         this.waitingForSession = true;
      }

      if (var1 >= this.sessionStartTime) {
         if (this.waitingForSession) {
            this.waitingForSession = false;
            this.sessionOpen.add(0, this.Chart.Open(this.dataShift));
            this.sessionHigh.add(0, this.Chart.High(this.dataShift));
            this.sessionLow.add(0, this.Chart.Low(this.dataShift));
            this.sessionClose.add(0, this.Chart.Close(this.dataShift));
         } else {
            this.sessionHigh.set(0, Math.max(this.Chart.High(this.dataShift), this.sessionHigh.get(0)));
            this.sessionLow.set(0, Math.min(this.Chart.Low(this.dataShift), this.sessionLow.get(0)));
            this.sessionClose.set(0, this.Chart.Close(this.dataShift));
         }
      }
   }

   private void calculateSessionTimes(long var1) {
      long var3 = SQTime.correctDayStart(var1);
      long var5 = var3 + (this.startHours * 60 + this.startMinutes) * 60000;
      long var7 = var3 + (this.endHours * 60 + this.endMinutes) * 60000;
      if (this.startHHMM >= this.endHHMM) {
         var7 += 86400000L;
      }

      if (var1 < var7 - 86400000L) {
         var5 -= 86400000L;
         var7 -= 86400000L;
      } else if (var1 > var7 || var7 == this.sessionEndTime) {
         var5 += 86400000L;
         var7 += 86400000L;
      }

      this.sessionStartTime = var5;
      this.sessionEndTime = var7;
   }
}
