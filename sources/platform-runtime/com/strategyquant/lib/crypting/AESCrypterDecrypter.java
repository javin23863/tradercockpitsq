/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.crypting;

import com.strategyquant.lib.L;
import com.strategyquant.lib.crypting.DecryptException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class AESCrypterDecrypter {
    private static final String initVector = "encryptionIntVec";
    private static final String SALT = "F5q95cx06_3rJ7Zse(840-dfg5wR9C";
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH = 256;
    private Cipher ecipher;
    private Cipher dcipher;
    private static SecretKeySpec secret = null;

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public AESCrypterDecrypter(String string) throws NoSuchAlgorithmException, UnsupportedEncodingException, InvalidKeySpecException, NoSuchPaddingException, InvalidKeyException, InvalidParameterSpecException, InvalidAlgorithmParameterException {
        Object object = AESCrypterDecrypter.class;
        synchronized (AESCrypterDecrypter.class) {
            if (secret == null) {
                SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
                PBEKeySpec pBEKeySpec = new PBEKeySpec(string.toCharArray(), SALT.getBytes(), 65536, 256);
                SecretKey secretKey = secretKeyFactory.generateSecret(pBEKeySpec);
                secret = new SecretKeySpec(secretKey.getEncoded(), "AES");
            }
            // ** MonitorExit[var2_2] (shouldn't be in output)
            object = new IvParameterSpec(initVector.getBytes("UTF-8"));
            this.ecipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            this.ecipher.init(1, (Key)secret, (AlgorithmParameterSpec)object);
            this.dcipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            this.dcipher.init(2, (Key)secret, (AlgorithmParameterSpec)object);
            return;
        }
    }

    public byte[] encrypt(String string) throws Exception {
        byte[] byArray = string.getBytes("UTF8");
        byte[] byArray2 = this.encrypt(byArray);
        return byArray2;
    }

    public byte[] encrypt(byte[] byArray) throws Exception {
        return this.ecipher.doFinal(byArray);
    }

    public byte[] decrypt(byte[] byArray) throws Exception {
        try {
            return this.dcipher.doFinal(byArray);
        }
        catch (BadPaddingException badPaddingException) {
            throw new DecryptException(L.t("Error while decrypting data file. Data are corrupted, or your licence was changed. Please download data again, or contact support.", new Object[0]));
        }
    }
}

