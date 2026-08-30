package com.strategyquant.plugin.DataManager.impl.Sessions;

import com.strategyquant.datalib.broker.BrokerManager;
import com.strategyquant.datalib.session.Session;
import com.strategyquant.datalib.session.SessionElement;
import com.strategyquant.datalib.session.SessionManager;
import com.strategyquant.lib.L;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.tradinglib.project.websocket.DataManagerAddProgressSender;
import com.strategyquant.tradinglib.project.websocket.DataManagerConfirm;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.WSDataObjects;
import com.strategyquant.webguilib.servlet.HttpJSONServlet;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import org.jdom2.Element;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SessionsServlet extends HttpJSONServlet {
   private static final Logger Log = LoggerFactory.getLogger(SessionsServlet.class);

   protected String execute(String var1, Map<String, String[]> var2, String var3) throws Exception {
      switch (var1) {
         case "listSessions":
            return this.onListSessions();
         case "addSession":
            return this.onAddSession(var2);
         case "updateSession":
            return this.onUpdateSession(var2);
         case "removeSession":
            return this.onRemoveSession(var2);
         case "load":
            return this.onLoad(var2);
         case "save":
            return this.onSave(var2);
         case "clone":
            return this.onClone(var2);
         default:
            throw new Exception("Unknown command '" + var1 + "'.");
      }
   }

   private String onListSessions() {
      JSONObject var1 = new JSONObject();

      try {
         SQWebSocketManager.addToDataQueue(WSDataObjects.getSessions(), new String[]{"SQUANT", "QDM"});
      } catch (Exception var3) {
         return apiErrorJSON(L.t("Cannot list sessions", new Object[0]), var3);
      }

      var1.put("success", L.t("Session listed", new Object[0]));
      return var1.toString();
   }

   private String onAddSession(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "name")[0];
         int var4 = Integer.valueOf(this.tryGetParam(var1, "broker")[0]);
         String[] var5 = this.tryGetParam(var1, "sessions[]");
         var3 = BrokerManager.getInstance().getBrokerDependantName(var3, var4);
         Session var6 = new Session(var3, this.getElements(var5));
         var6.setBroker(var4);
         SessionManager.addSession(var6);
         SQWebSocketManager.addToDataQueue(WSDataObjects.getSessions(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var7) {
         return apiErrorJSON(L.t("Cannot add session", new Object[0]), var7);
      }

      var2.put("success", L.t("Session added", new Object[0]));
      return var2.toString();
   }

   private String onRemoveSession(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String[] var3 = this.tryGetParam(var1, "names")[0].split(",");
         if (var3.length > 10) {
            this.removeSessionsAsync(var3);
            var2.put("success", L.t("Sessions removing.", new Object[0]));
            return var2.toString();
         }

         for (int var4 = 0; var4 < var3.length; var4++) {
            SessionManager.removeSession(var3[var4]);
         }

         SQWebSocketManager.addToDataQueue(WSDataObjects.getSessions(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         var2.put("success", L.t("Session removed", new Object[0]));
         return var2.toString();
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot remove session", new Object[0]), var5);
      }
   }

   private void removeSessionsAsync(final String[] var1) {
      final int var2 = var1.length;
      (new Thread() {
         @Override
         public void run() {
            JSONObject var1x = new JSONObject();
            LinkedList var2x = new LinkedList();
            int var3 = 0;

            for (int var4 = 0; var4 < var1.length; var4++) {
               try {
                  SessionManager.removeSession(var1[var4]);
                  Integer var5 = SQUtils.round(100.0 * var3 / var2);
                  var1x.put("percent", var5);
                  var1x.put("info", L.t("Removed session '%s'", new Object[]{var1[var4]}));
                  if (var5 == 100) {
                     var1x.put("message", "Sessions removed");
                  }

                  DataManagerAddProgressSender.getInstance().sendData(var1x);
               } catch (Exception var11) {
                  var2x.add(var1[var4]);
               } finally {
                  var3++;
               }
            }

            try {
               SQWebSocketManager.addToDataQueue(WSDataObjects.getSessions(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
            } catch (Exception var10) {
               SessionsServlet.Log.error("", var10);
            }

            var1x.put("percent", 100);
            var1x.put("info", L.t("Completed", new Object[0]));
            var1x.put("message", "Sessions removed");
            if (var2x.size() > 0) {
               var1x.put("error", L.t("Could'd be deleted: %s", new Object[]{String.join(",", var2x)}));
            }

            DataManagerAddProgressSender.getInstance().sendData(var1x);
         }
      }).start();
   }

   private String onUpdateSession(Map<String, String[]> var1) {
      JSONObject var2 = new JSONObject();

      try {
         String var3 = this.tryGetParam(var1, "name")[0];
         String[] var4 = this.tryGetParam(var1, "sessions[]");
         SessionManager.updateSession(var3, this.getElements(var4));
         SQWebSocketManager.addToDataQueue(WSDataObjects.getSessions(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
      } catch (Exception var5) {
         return apiErrorJSON(L.t("Cannot modify session", new Object[0]), var5);
      }

      var2.put("success", L.t("Session modified", new Object[0]));
      return var2.toString();
   }

   private ArrayList<SessionElement> getElements(String[] var1) throws Exception {
      ArrayList var2 = new ArrayList();

      for (String var6 : var1) {
         String[] var7 = var6.split(",");
         if (var7.length != 5) {
            throw new Exception(L.t("Invalid session data length. Expected 5, got %d", new Object[]{var7.length}));
         }

         var2.add(new SessionElement(var7[0], var7[1], var7[2], var7[3], var7[4].equals("Yes")));
      }

      return var2;
   }

   private String onLoad(Map<String, String[]> var1) {
      try {
         String var2 = this.tryGetParam(var1, "filePath")[0];
         File var3 = new File(var2);
         JSONObject var4 = new JSONObject();
         ArrayList var5 = new ArrayList();
         Element var6 = XMLUtil.fileToXmlElement(var3);
         if (!var6.getName().equals("Sessions")) {
            throw new Exception(L.t("No Sessions found to import.", new Object[0]));
         }

         for (Element var8 : var6.getChildren("Session")) {
            Session var9 = new Session();
            var9.setFromXML(var8);
            if (!SessionManager.checkSessionExists(var9.getSessionName())) {
               SessionManager.addSession(var9);
               var5.add(var9.getSessionName());
            } else {
               DataManagerConfirm.get()
                  .awaitConfirmation(
                     L.t("Overwrite confirm", new Object[0]),
                     L.t("Session '%s' already exists, do you want to overwrite it with the imported one?", new Object[]{var9.getSessionName()})
                  );
               String var10 = DataManagerConfirm.get().getConfirmedAction();
               if (!var10.equals("skip")) {
                  if (var10.equals("cancel")) {
                     break;
                  }

                  if (var10.equals("overwrite")) {
                     SessionManager.updateSession(var9);
                     var5.add(var9.getSessionName());
                  }
               }
            }
         }

         SQWebSocketManager.addToDataQueue(WSDataObjects.getSessions(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         var4.put("success", L.t("Sessions (%d) loaded.", new Object[]{var5.size()}));
         return var4.toString();
      } catch (Exception var11) {
         return apiErrorJSON(L.t("Cannot load sessions.", new Object[0]), var11);
      }
   }

   private String onSave(Map<String, String[]> var1) {
      try {
         String var2 = this.tryGetParam(var1, "filePath")[0];
         File var3 = new File(var2);
         JSONObject var4 = new JSONObject();
         Element var5 = new Element("Sessions");
         String[] var6 = this.tryGetParam(var1, "names")[0].split(",");

         for (String var10 : var6) {
            Session var11 = SessionManager.getSession(var10);
            var5.addContent(var11.getXML());
         }

         XMLUtil.xmlToFile(var5, var3);
         var4.put("success", L.t("Sessions saved.", new Object[0]));
         return var4.toString();
      } catch (Exception var12) {
         return apiErrorJSON(L.t("Cannot save sessions.", new Object[0]), var12);
      }
   }

   private String onClone(Map<String, String[]> var1) {
      try {
         String var2 = this.tryGetParam(var1, "name")[0];
         int var3 = Integer.valueOf(this.tryGetParam(var1, "broker")[0]);
         String var4 = this.tryGetParam(var1, "clone")[0];
         var4 = BrokerManager.getInstance().getBrokerDependantName(var4, var3);
         Session var5 = SessionManager.getSession(var4);
         if (var5 != null) {
            throw new Exception(L.t("Session with name '%s' already exists.", new Object[]{var2}));
         }

         var5 = SessionManager.getSession(var2);
         if (var5 == null) {
            throw new Exception(L.t("Session with name '%s' doesn't exist.", new Object[]{var2}));
         }

         Session var6 = new Session(var4, var5.cloneElements());
         var6.setBroker(var3);
         SessionManager.addSession(var6);
         SQWebSocketManager.addToDataQueue(WSDataObjects.getSessions(), new String[]{"SQUANT", "QDM", "AlgoWizard"});
         JSONObject var7 = new JSONObject();
         var7.put("success", L.t("Session cloned.", new Object[0]));
         return var7.toString();
      } catch (Exception var8) {
         return apiErrorJSON(L.t("Cannot clone session.", new Object[0]), var8);
      }
   }
}
