package com.strategyquant.datalib.session;

import com.strategyquant.datalib.TickEvent;
import com.strategyquant.datalib.data.io.VersatileData;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.settings.IXMLAble;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Session implements Serializable, IXMLAble {
   public static final Logger Log = LoggerFactory.getLogger("Session");
   public static final long WEEK_MILLIS = 604800000L;
   public static final String NoSession = "No Session";
   public static final String Forex_247 = "Forex_247";
   public static final String Forex_245 = "Forex_245";
   public static final String US_Index_Futures = "US_Index_Futures";
   public static final String US_Stocks = "US_Stocks";
   private String sessionName;
   private int broker = -1;
   private ArrayList<SessionElement> sessionElements;
   private long lastTimeChecked = -1L;
   private SessionStatus lastSessionStatus = new SessionStatus();

   public Session() {
   }

   public Session(String var1, ArrayList<SessionElement> var2) {
      this.sessionName = var1;
      this.sessionElements = var2;
   }

   public String getSessionName() {
      return this.sessionName;
   }

   public ArrayList<SessionElement> getElements() {
      return this.sessionElements;
   }

   public void checkTimeIsInSession(long var1, SessionStatus var3, String var4) {
      if (this.sessionElements == null) {
         var3.isInSession = true;
         var3.sessionStartTime = Long.MIN_VALUE;
         var3.sessionEndTime = Long.MIN_VALUE;
      } else if (this.lastTimeChecked == var1) {
         var3.isInSession = this.lastSessionStatus.isInSession;
         var3.sessionStartTime = this.lastSessionStatus.sessionStartTime;
         var3.sessionEndTime = this.lastSessionStatus.sessionEndTime;
      } else {
         var3.isInSession = false;
         SessionElement var5 = null;
         int var6 = -1;

         for (int var7 = 0; var7 < this.sessionElements.size(); var7++) {
            SessionElement var8 = this.sessionElements.get(var7);
            if (var8.includesTime(var1)) {
               var5 = var8;
               var6 = var7;
            }
         }

         if (var5 != null) {
            var3.isInSession = true;
            var4 = var4 != null ? var4 : "H1";
            if (var4.equals("Weekly")) {
               var3.sessionStartTime = this.sessionElements.get(0).getStartTime();
               var3.sessionEndTime = this.sessionElements.get(this.sessionElements.size() - 1).getEndTime();
               if (var3.sessionStartTime > var3.sessionEndTime) {
                  var3.sessionStartTime -= 604800000L;
               }

               if (var3.sessionEndTime < var1) {
                  var3.sessionStartTime += 604800000L;
                  var3.sessionEndTime += 604800000L;
               }
            } else if (var4.equals("Monthly")) {
               var3.sessionStartTime = MonthlyRangeCalculator.getSessionStartTime(
                  var1, this.sessionElements.get(0).getStartTime(), this.sessionElements.get(this.sessionElements.size() - 1).getEndTime()
               );
               var3.sessionEndTime = MonthlyRangeCalculator.getSessionEndTime(
                  var1, this.sessionElements.get(0).getStartTime(), this.sessionElements.get(this.sessionElements.size() - 1).getEndTime()
               );
            } else if (var4.equals("D1")) {
               var3.sessionStartTime = this.findStartElement(var6).getStartTime();
               var3.sessionEndTime = this.findEndElement(var6).getEndTime();
               if (var3.sessionStartTime > var3.sessionEndTime) {
                  var3.sessionStartTime -= 604800000L;
               }

               if (var3.sessionEndTime < var1) {
                  var3.sessionStartTime += 604800000L;
                  var3.sessionEndTime += 604800000L;
               }
            } else {
               var3.sessionStartTime = var5.getStartTime();
               var3.sessionEndTime = var5.getEndTime();
               if (var3.sessionStartTime > var3.sessionEndTime) {
                  long var10 = var3.sessionStartTime;
                  var3.sessionStartTime = var3.sessionEndTime;
                  var3.sessionEndTime = var10;
               }
            }

            this.lastTimeChecked = var1;
            this.lastSessionStatus.isInSession = var3.isInSession;
            this.lastSessionStatus.sessionStartTime = var3.sessionStartTime;
            this.lastSessionStatus.sessionEndTime = var3.sessionEndTime;
         }
      }
   }

   private SessionElement findStartElement(int var1) {
      byte var2 = 50;
      int var3 = 0;
      int var4 = var1;
      SessionElement var5 = null;

      while (var3 < var2) {
         if (--var4 < 0) {
            var4 = this.sessionElements.size() - 1;
         }

         if (this.sessionElements.get(var4).isEOD()) {
            int var6 = var4 + 1;
            if (var6 == this.sessionElements.size()) {
               var6 = 0;
            }

            var5 = this.sessionElements.get(var6);
            break;
         }

         var3++;
      }

      if (var5 == null) {
         Log.error("Session start element not found");
         return this.sessionElements.get(var1);
      } else {
         return var5;
      }
   }

   private SessionElement findEndElement(int var1) {
      byte var2 = 50;
      int var3 = 0;
      int var4 = var1;
      SessionElement var5 = null;

      while (var3 < var2) {
         if (var4 >= this.sessionElements.size()) {
            var4 = 0;
         }

         if (this.sessionElements.get(var4).isEOD()) {
            var5 = this.sessionElements.get(var4);
            break;
         }

         var4++;
         var3++;
      }

      if (var5 == null) {
         Log.error("Session start element not found");
         return this.sessionElements.get(var1);
      } else {
         return var5;
      }
   }

   public Session clone() {
      return new Session(this.sessionName, this.cloneElements());
   }

   public void clearSessionTempData() {
      if (this.sessionElements != null) {
         for (int var1 = 0; var1 < this.sessionElements.size(); var1++) {
            SessionElement var2 = this.sessionElements.get(var1);
            var2.clearSessionTempData();
         }
      }
   }

   public ArrayList<SessionElement> cloneElements() {
      if (this.sessionElements == null) {
         return null;
      }

      ArrayList var1 = new ArrayList();

      for (SessionElement var3 : this.sessionElements) {
         var1.add(var3.clone());
      }

      return var1;
   }

   public void fixD1DataTime(VersatileData var1) {
      if (this.sessionElements != null && this.sessionElements.size() != 0) {
         SessionElement var2 = null;

         for (int var3 = 0; var3 < this.sessionElements.size(); var3++) {
            SessionElement var4 = this.sessionElements.get(var3);
            if (var4.includesTime(var1.time)) {
               var2 = var4;
            }
         }

         if (var2 != null) {
            var2.fixD1DataTime(var1);
         }
      }
   }

   public void fixD1DataTime(TickEvent var1) {
      if (this.sessionElements != null && this.sessionElements.size() != 0) {
         SessionElement var2 = null;

         for (int var3 = 0; var3 < this.sessionElements.size(); var3++) {
            SessionElement var4 = this.sessionElements.get(var3);
            if (var4.includesTime(var1.getTime())) {
               var2 = var4;
            }
         }

         if (var2 != null) {
            var2.fixD1DataTime(var1);
         }
      }
   }

   public Element getXML() {
      try {
         Element var1 = new Element("Session");
         var1.setAttribute("name", this.sessionName);
         ArrayList var2 = this.getElements();

         for (int var3 = 0; var3 < var2.size(); var3++) {
            SessionElement var4 = (SessionElement)var2.get(var3);
            Element var5 = new Element("Element");
            var5.setAttribute("dayFrom", var4.getDayFromStr());
            var5.setAttribute("dayTo", var4.getDayToStr());
            var5.setAttribute("timeFrom", var4.getTimeFromStr());
            var5.setAttribute("timeTo", var4.getTimeToStr());
            var5.setAttribute("eod", String.valueOf(var4.isEOD()));
            var1.addContent(var5);
         }

         return var1;
      } catch (Exception var6) {
         Log.error("Cannot get xml. Exc.", var6);
         return null;
      }
   }

   public void setFromXML(Element var1) throws Exception {
      this.sessionName = var1.getAttributeValue("name");
      this.sessionElements = new ArrayList<>();
      List var2 = var1.getChildren("Element");

      for (int var3 = 0; var3 < var2.size(); var3++) {
         Element var4 = (Element)var2.get(var3);
         String var5 = var4.getAttributeValue("dayFrom");
         String var6 = var4.getAttributeValue("dayTo");
         String var7 = var4.getAttributeValue("timeFrom");
         String var8 = var4.getAttributeValue("timeTo");
         boolean var9 = Boolean.parseBoolean(var4.getAttributeValue("eod"));
         this.sessionElements.add(new SessionElement(var5, var7, var6, var8, var9));
      }
   }

   public String toText() {
      Element var1 = this.getXML();
      return var1 == null ? this.sessionName : XMLUtil.elementToString(var1);
   }

   public int getBroker() {
      return this.broker;
   }

   public void setBroker(int var1) {
      this.broker = var1;
   }
}
