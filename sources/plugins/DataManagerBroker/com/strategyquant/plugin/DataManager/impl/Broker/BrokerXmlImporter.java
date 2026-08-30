package com.strategyquant.plugin.DataManager.impl.Broker;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.datalib.session.Session;
import com.strategyquant.datalib.session.SessionElement;
import com.strategyquant.lib.XMLUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.jdom2.Element;
import org.jdom2.JDOMException;

public class BrokerXmlImporter {
   public List<Session> importSessions(String var1, List<String> var2) throws JDOMException, IOException, Exception {
      LinkedList var3 = new LinkedList();
      Element var4 = XMLUtil.fileToXmlElement(new File(var1));

      for (Element var7 : var4.getChildren("Session")) {
         String var8 = var7.getAttributeValue("name");
         if (var2 == null || var2.contains(var8)) {
            ArrayList var9 = new ArrayList();

            for (Element var11 : var7.getChildren("Element")) {
               var9.add(
                  new SessionElement(
                     var11.getAttributeValue("dayFrom"),
                     var11.getAttributeValue("timeFrom"),
                     var11.getAttributeValue("dayTo"),
                     var11.getAttributeValue("timeTo"),
                     Boolean.valueOf(var11.getAttributeValue("eod"))
                  )
               );
            }

            var3.add(new Session(var8, var9));
         }
      }

      return var3;
   }

   public List<InstrumentInfo> importInstruments(String var1, List<String> var2) throws JDOMException, IOException, Exception {
      LinkedList var3 = new LinkedList();
      Element var4 = XMLUtil.fileToXmlElement(new File(var1));

      for (Element var7 : var4.getChildren("InstrumentInfo")) {
         String var8 = var7.getAttributeValue("instrument");
         if (var2 == null || var2.contains(var8)) {
            InstrumentInfo var9 = new InstrumentInfo(var7);
            var3.add(var9);
         }
      }

      return var3;
   }
}
