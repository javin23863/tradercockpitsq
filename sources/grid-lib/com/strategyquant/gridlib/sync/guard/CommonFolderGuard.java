package com.strategyquant.gridlib.sync.guard;

import com.strategyquant.gridlib.FileHelper;
import com.strategyquant.gridlib.TopicIdent;
import com.strategyquant.gridlib.message.JmsConnectionInfo;
import com.strategyquant.gridlib.message.JmsPerformer;
import com.strategyquant.gridlib.message.MessageKind;
import com.strategyquant.gridlib.message.MessageProperties;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CommonFolderGuard implements IFolderGuard {
   private static final Logger LOGGER = LoggerFactory.getLogger(CommonFolderGuard.class);
   private String folderIdent;
   private String folder;
   private String hash;
   private boolean solid;
   private JmsPerformer sender;
   private JmsPerformer reciever;

   public CommonFolderGuard(String var1, String var2, JmsConnectionInfo var3, boolean var4) throws IOException, JMSException {
      this.folderIdent = var1;
      this.folder = var2;
      this.sender = new JmsPerformer(TopicIdent.dataFilesTopic, var3);
      this.reciever = new JmsPerformer(TopicIdent.dataFilesTopic, var3);
      this.solid = var4;
      this.updateHash(false);
      this.prepareJMS();
      this.sendStatus(null);
   }

   private void prepareJMS() throws JMSException {
      String var1 = MessageProperties.folder.name() + "='" + this.folderIdent + "'";
      this.reciever.prepareMessageClient(new MessageListener() {
         public void onMessage(Message var1) {
            try {
               MessageKind var2 = CommonFolderGuard.this.reciever.getMessageKind(var1);
               CommonFolderGuard.this.handleMessage(var1, var2);
            } catch (JMSException | IOException var4) {
               var4.printStackTrace();
            }
         }
      }, var1);
   }

   private void handleMessage(Message var1, MessageKind var2) throws JMSException, IOException {
      switch (var2) {
         case needFileData:
            this.sendFiles(var1.getStringProperty(MessageProperties.senderIdent.name()), (List<String>)((ObjectMessage)var1).getObject());
            break;
         case checkFileDataStatus:
            this.sendStatus(var1.getStringProperty(MessageProperties.senderIdent.name()));
      }
   }

   private void sendFiles(String var1, List<String> var2) throws JMSException, IOException {
      LOGGER.debug("Sending file for folder:" + this.folderIdent);
      File var3 = this.zipFiles(var2);
      this.sender.sendMessageFile(var3, var1, this.folderIdent);
      var3.delete();
   }

   private void sendStatus(String var1) throws FileNotFoundException, JMSException {
      LOGGER.debug("Sending status for folder:" + this.folderIdent);
      HashMap var2 = new HashMap();
      var2.put(MessageProperties.dataVersion, this.hash);
      var2.put(MessageProperties.folder, this.folderIdent);
      this.sender.sendSimpleMessage(MessageKind.dataStatus, var1, var2);
   }

   @Override
   public String getFolder() {
      return this.folder;
   }

   @Override
   public void updateHash() throws IOException, JMSException {
      this.updateHash(true);
   }

   private void updateHash(boolean var1) throws IOException, JMSException {
      this.hash = FileHelper.countHash(this.folder);
      if (this.solid) {
         this.hash = DigestUtils.md5Hex(this.hash);
      }

      if (var1) {
         this.sendStatus(null);
      }
   }

   private File zipFiles(List<String> var1) throws IOException {
      return this.solid ? FileHelper.zip(this.folder) : FileHelper.zip(this.folder, var1);
   }

   @Override
   public void close() throws JMSException {
      this.sender.close();
      this.reciever.close();
   }
}
