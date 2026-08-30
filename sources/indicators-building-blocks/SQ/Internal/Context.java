package SQ.Internal;

public class Context {
   private XmlStrategy xmlStrategy;
   private String event;
   private String subEvent;

   public Context(XmlStrategy var1, String var2, String var3) {
      this.xmlStrategy = var1;
      this.event = var2;
      this.subEvent = var3;
   }

   public String getSubEvent() {
      return this.subEvent;
   }

   public XmlStrategy getStrategy() {
      return this.xmlStrategy;
   }
}
