package com.strategyquant.gridlib.sync.updater;

import com.strategyquant.gridlib.FileHelper;
import com.strategyquant.gridlib.TopicIdent;
import com.strategyquant.gridlib.message.JmsConnectionInfo;
import com.strategyquant.gridlib.message.JmsPerformer;
import com.strategyquant.gridlib.message.MessageKind;
import com.strategyquant.gridlib.message.MessageProperties;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractFolderUpdater implements IFolderUpdater {
   private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFolderUpdater.class);
   private String folder;
   private boolean dirty = true;
   protected String hash;
   private String folderIdent;
   private JmsPerformer sender;
   private JmsPerformer reciever;
   private List<IFolderUpdatedListener> listeners = new LinkedList<>();

   public AbstractFolderUpdater(String var1, String var2, JmsConnectionInfo var3) throws IOException, JMSException {
      this.folder = var2;
      this.folderIdent = var1;
      this.sender = new JmsPerformer(TopicIdent.dataFilesTopic, var3);
      this.reciever = new JmsPerformer(TopicIdent.dataFilesTopic, var3);
      this.updateHash();
      this.prepareJMS();
      this.checkVersions();
   }

   @Override
   public void addFolderUpdatedListener(IFolderUpdatedListener var1) {
      this.listeners.add(var1);
   }

   private void checkVersions() throws JMSException {
      this.setDirty(true);
      HashMap var1 = new HashMap();
      var1.put(MessageProperties.folder, this.folderIdent);
      LOGGER.debug("Send request for version for folder:" + this.folderIdent);
      this.sender.sendSimpleMessage(MessageKind.checkFileDataStatus, null, var1);
   }

   protected void setDirty(boolean var1) {
      this.dirty = var1;
      if (!var1) {
         this.notifyListeners();
      }
   }

   private void notifyListeners() {
      for (IFolderUpdatedListener var2 : this.listeners) {
         var2.onUpdated();
      }
   }

   @Override
   public boolean isDirty() {
      return this.dirty;
   }

   @Override
   public String getFolder() {
      return this.folder;
   }

   private void prepareJMS() throws JMSException {
      String var1 = MessageProperties.folder.name() + "='" + this.folderIdent + "'";
      this.reciever.prepareMessageClient(new MessageListener() {
         public void onMessage(Message var1) {
            try {
               MessageKind var2 = AbstractFolderUpdater.this.reciever.getMessageKind(var1);
               AbstractFolderUpdater.this.handleMessage(var1, var2);
            } catch (JMSException | IOException var4) {
               var4.printStackTrace();
            }
         }
      }, var1);
   }

   @Override
   public void close() throws JMSException {
      this.sender.close();
      this.reciever.close();
   }

   private void downloadData(BytesMessage var1) throws JMSException, IOException {
      this.setDirty(true);
      File var2 = File.createTempFile("strategyquant", ".zip");
      FileOutputStream var3 = new FileOutputStream(var2);
      BufferedOutputStream var4 = new BufferedOutputStream(var3);
      var1.setObjectProperty("JMS_AMQ_SaveStream", var4);
      var4.close();
      this.prepareForNewData();
      FileHelper.unzip(this.getFolder(), var2);
      this.updateHash();
      this.setDirty(false);
      var2.delete();
   }

   protected void prepareForNewData() throws IOException {
   }

   private void handleNewHash(String var1, String var2) throws FileNotFoundException, JMSException {
      HandleHashResult var3 = this.hashChanged(var2);
      if (var3.isNeedUpdate()) {
         LOGGER.debug("Node's data is different that client's - need to update:" + var1);
         this.setDirty(true);
         HashMap var4 = new HashMap();
         var4.put(MessageProperties.folder, var1);
         this.sender.sendObjectMessage(MessageKind.needFileData, null, var4, var3.getFilesToUpdate());
      } else {
         this.setDirty(false);
         LOGGER.debug("Node's data is same as client's ones:" + var1);
      }
   }

   protected abstract HandleHashResult hashChanged(String var1);

   private void handleMessage(Message var1, MessageKind var2) throws JMSException, IOException {
      switch (var2) {
         case sendingFileData:
            LOGGER.debug("Got data message");
            this.downloadData((BytesMessage)var1);
            break;
         case dataStatus:
            String var3 = var1.getStringProperty(MessageProperties.dataVersion.name());
            String var4 = var1.getStringProperty(MessageProperties.folder.name());
            LOGGER.debug("Got a version info for folder:" + var4);
            this.handleNewHash(var4, var3);
      }
   }
}
