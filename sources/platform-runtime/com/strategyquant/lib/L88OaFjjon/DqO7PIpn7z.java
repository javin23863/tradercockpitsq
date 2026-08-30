/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.L88OaFjjon;

import com.strategyquant.lib.L88OaFjjon.DVGBR8rirw;
import com.strategyquant.lib.L88OaFjjon.Eob3xULZVY;
import com.strategyquant.lib.L88OaFjjon.Lewgre21g;
import com.strategyquant.lib.L88OaFjjon.WbwPVQGc2h;
import com.strategyquant.lib.L88OaFjjon.YbM3Hb5g8g;
import com.strategyquant.lib.crypting.SQDecoderEncoder;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESedeKeySpec;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class DqO7PIpn7z {
    public static final Logger Log = LoggerFactory.getLogger((String)"c.strategyquant.strategyquant.Log");

    public static DVGBR8rirw loadLicenseXmlFromFile(String string) throws Eob3xULZVY {
        YbM3Hb5g8g ybM3Hb5g8g = new YbM3Hb5g8g();
        return DqO7PIpn7z._loadLicenseXmlFromFile(string, ybM3Hb5g8g.qwRUlHnzJs());
    }

    public static DVGBR8rirw _loadLicenseXmlFromFile(String string, String string2) throws Eob3xULZVY {
        try {
            DVGBR8rirw dVGBR8rirw = new DVGBR8rirw();
            dVGBR8rirw.type = 10;
            String string3 = "";
            String string4 = null;
            DESedeKeySpec dESedeKeySpec = new DESedeKeySpec(string2.getBytes());
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("DESede");
            SecretKey secretKey = secretKeyFactory.generateSecret(dESedeKeySpec);
            Cipher cipher = Cipher.getInstance("DESede");
            cipher.init(2, secretKey);
            CipherInputStream cipherInputStream = new CipherInputStream(new FileInputStream(string), cipher);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader((InputStream)cipherInputStream, StandardCharsets.UTF_8));
            while ((string4 = bufferedReader.readLine()) != null) {
                string3 = string3 + string4;
            }
            int n = string3.indexOf("</license_verify>");
            dVGBR8rirw.xml = string3.substring(0, n + 17);
            String string5 = string3.substring(n + 17);
            dVGBR8rirw.hash = SQDecoderEncoder.decode(string5);
            Lewgre21g.g5Tq3ZdPHp("LFileLoadXml-" + dVGBR8rirw.xml);
            Lewgre21g.g5Tq3ZdPHp("LFileLoadHash-" + string5);
            cipherInputStream.close();
            return dVGBR8rirw;
        }
        catch (Exception exception) {
            throw new Eob3xULZVY(11);
        }
    }

    public static boolean removeLicenseFile(String string) {
        File file = new File(string);
        return file.delete();
    }

    public static boolean licenseFileExists(String string) {
        if (string == null || string.equalsIgnoreCase("")) {
            return false;
        }
        return new File(string).exists();
    }

    public static void saveLicenseFile(DVGBR8rirw dVGBR8rirw, String string) throws Eob3xULZVY {
        try {
            YbM3Hb5g8g ybM3Hb5g8g = new YbM3Hb5g8g();
            DESedeKeySpec dESedeKeySpec = new DESedeKeySpec(ybM3Hb5g8g.qwRUlHnzJs().getBytes());
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("DESede");
            SecretKey secretKey = secretKeyFactory.generateSecret(dESedeKeySpec);
            Cipher cipher = Cipher.getInstance("DESede");
            cipher.init(1, secretKey);
            Lewgre21g.g5Tq3ZdPHp("LFile-" + string);
            Lewgre21g.g5Tq3ZdPHp("LFileSaveXml-" + dVGBR8rirw.xml);
            CipherOutputStream cipherOutputStream = new CipherOutputStream(new FileOutputStream(string), cipher);
            cipherOutputStream.write(dVGBR8rirw.xml.getBytes());
            cipherOutputStream.write(SQDecoderEncoder.encode(dVGBR8rirw.hash));
            cipherOutputStream.flush();
            cipherOutputStream.close();
        }
        catch (Exception exception) {
            throw new Eob3xULZVY(12);
        }
    }

    public static WbwPVQGc2h parseLicenseXml(DVGBR8rirw dVGBR8rirw) throws Eob3xULZVY {
        try {
            WbwPVQGc2h wbwPVQGc2h = new WbwPVQGc2h();
            DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder documentBuilder = documentBuilderFactory.newDocumentBuilder();
            InputSource inputSource = new InputSource();
            inputSource.setCharacterStream(new StringReader(dVGBR8rirw.xml));
            Document document = documentBuilder.parse(inputSource);
            document.normalize();
            Element element = document.getDocumentElement();
            for (Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
                if (node.getNodeType() != 1) continue;
                if (node.getFirstChild() == null) {
                    wbwPVQGc2h.set(node.getNodeName(), "");
                    continue;
                }
                String string = node.getFirstChild().getNodeValue();
                if (string.equals("-")) {
                    string = "";
                }
                wbwPVQGc2h.set(node.getNodeName(), string);
            }
            return wbwPVQGc2h;
        }
        catch (Exception exception) {
            Log.error("Exc.", (Throwable)exception);
            if (dVGBR8rirw.type == 5) {
                throw new Eob3xULZVY(20);
            }
            throw new Eob3xULZVY(27);
        }
    }

    public static String decryptLicenseXmlSignature(byte[] byArray) throws Eob3xULZVY {
        YbM3Hb5g8g ybM3Hb5g8g = new YbM3Hb5g8g();
        return DqO7PIpn7z._decryptLicenseXmlSignature(byArray, ybM3Hb5g8g.sn97XxTCij());
    }

    private static String _decryptLicenseXmlSignature(byte[] byArray, String string) throws Eob3xULZVY {
        try {
            X509EncodedKeySpec x509EncodedKeySpec = new X509EncodedKeySpec(SQDecoderEncoder.decode(string));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            RSAPublicKey rSAPublicKey = (RSAPublicKey)keyFactory.generatePublic(x509EncodedKeySpec);
            Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1PADDING");
            cipher.init(2, rSAPublicKey);
            return new String(cipher.doFinal(byArray), "UTF-8");
        }
        catch (Exception exception) {
            throw new Eob3xULZVY(19);
        }
    }

    public static String generateControlHash(String string) throws Eob3xULZVY {
        try {
            return DqO7PIpn7z.md5(string);
        }
        catch (Exception exception) {
            throw new Eob3xULZVY(17);
        }
    }

    private static String md5(String string) throws Exception {
        try {
            return DigestUtils.md5Hex((String)string);
        }
        catch (Exception exception) {
            throw new Exception("Failed to create hash.", exception);
        }
    }

    public static String generateHardwareId(String string, String string2, long l) {
        try {
            String string3 = string + string2 + l;
            string3 = DqO7PIpn7z.md5(string3);
            string3 = string3.toUpperCase().substring(0, 11);
            return string3;
        }
        catch (Exception exception) {
            Log.error("Failed to generate hardware id. Exc.", (Throwable)exception);
            return "";
        }
    }

    public static String getAppPath() {
        File file = new File("");
        return file.getAbsolutePath() + "\\";
    }
}

