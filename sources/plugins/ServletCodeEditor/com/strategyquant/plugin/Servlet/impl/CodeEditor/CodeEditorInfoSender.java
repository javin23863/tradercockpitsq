package com.strategyquant.plugin.Servlet.impl.CodeEditor;

import com.strategyquant.tradinglib.project.websocket.DataToSend;
import com.strategyquant.tradinglib.project.websocket.SQWebSocketManager;
import com.strategyquant.tradinglib.project.websocket.SynchronizedWebSocketPublisher;
import org.json.JSONObject;

public class CodeEditorInfoSender extends SynchronizedWebSocketPublisher {
   private final DataToSend toSend;
   private JSONObject data = null;

   public CodeEditorInfoSender() {
      this.toSend = new DataToSend();
      this.toSend.setName("codeEditor");
      SQWebSocketManager.getInstance().addAppPublisher("SQEDITOR", this);
   }

   public DataToSend getData() {
      if (this.data != null) {
         this.toSend.setDataObject(new JSONObject(this.data.toString()));
         this.data = null;
         return this.toSend;
      } else {
         return null;
      }
   }

   public void sendData(JSONObject var1) {
      this.data = var1;
   }

   public void resetLastData() {
   }
}
