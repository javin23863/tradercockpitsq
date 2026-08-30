package com.strategyquant.gridlib.sync.updater;

import java.io.IOException;
import javax.jms.JMSException;

public interface IFolderUpdater {
   String getFolder();

   void updateHash() throws IOException;

   boolean isDirty();

   void close() throws JMSException;

   void addFolderUpdatedListener(IFolderUpdatedListener var1);
}
