package com.strategyquant.datalib.data.io.newDataFormat;

import com.strategyquant.datalib.InstrumentInfo;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.crypting.AESCrypterDecrypter;
import com.strategyquant.lib.historyData.ICryptable;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.NoSuchPaddingException;

public abstract class DataManipulatorNew implements ICryptable {
   public static final String HEADER_PREFIX = "ABCDEFGH";
   public static final String HEADER_SUFFIX = "SnRbTs";
   public static final String CRYPTED_DATA_TYPE = "C";
   public static final String DATA_TYPE = "D";
   public static final String SUPPORTED_OLD_VERSION = "4.1";
   public static final String VERSION = "4.2";
   private AESCrypterDecrypter crypterDecrypter;
   private boolean crypted;
   private String password;

   protected byte[] encryptModificators(byte[] var1) throws Exception {
      this.ensureCrypterDecrypter();
      return this.crypterDecrypter.encrypt(var1);
   }

   protected byte[] decryptModificators(byte[] var1) throws Exception {
      this.ensureCrypterDecrypter();
      return this.crypterDecrypter.decrypt(var1);
   }

   public boolean isCrypted() {
      return this.crypted;
   }

   protected void setCrypted(boolean var1) {
      this.crypted = var1;
   }

   public static void setDecimals(InstrumentInfo var0, DataManipulatorNew var1) {
      if (var0 != null && (var0.instrument.equals("ZF") || var0.instrument.equals("ZF - CBOT")) && var0.dataType == 2) {
         var1.overrideDecimals(7);
      } else if (var0 != null && var0.dataType == 7) {
         var1.overrideDecimals(8);
      }
   }

   private void ensureCrypterDecrypter() throws InvalidKeyException, NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeySpecException, NoSuchPaddingException, InvalidParameterSpecException, InvalidAlgorithmParameterException {
      if (this.crypterDecrypter == null) {
         this.crypterDecrypter = new AESCrypterDecrypter(this.getPassword());
      }
   }

   private String getPassword() {
      return this.password != null ? this.password : MainApp.v571hfnsHw().eHvc2JguAd();
   }

   public void setPassword(String var1) {
      this.password = var1;
   }

   public abstract void overrideDecimals(int var1);
}
