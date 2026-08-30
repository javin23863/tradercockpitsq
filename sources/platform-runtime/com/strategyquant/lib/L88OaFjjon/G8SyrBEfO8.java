/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.L88OaFjjon;

import com.strategyquant.lib.L88OaFjjon.DVGBR8rirw;
import com.strategyquant.lib.L88OaFjjon.DqO7PIpn7z;
import com.strategyquant.lib.L88OaFjjon.Eob3xULZVY;
import com.strategyquant.lib.L88OaFjjon.KIozbup0X4;
import com.strategyquant.lib.L88OaFjjon.Lewgre21g;
import com.strategyquant.lib.L88OaFjjon.WbwPVQGc2h;
import com.strategyquant.lib.L88OaFjjon.YbM3Hb5g8g;
import com.strategyquant.lib.SQTime;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.crypting.SQDecoderEncoder;
import com.strategyquant.lib.hw.HardwareInfo;
import com.strategyquant.lib.whitelabel.AbstractBroker;
import com.strategyquant.lib.whitelabel.Brokers;
import java.io.File;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class G8SyrBEfO8 {
    public static final Logger Log = LoggerFactory.getLogger(G8SyrBEfO8.class);
    private static G8SyrBEfO8 instance;
    private String lastCheckedCode = null;
    private String product = null;
    private String reseller = "1";
    private String licenseFilePath = "";
    private WbwPVQGc2h license = null;
    private String MACaddress = null;
    private String diskSN = null;
    private String uniqID = null;
    private String hardwareid = null;
    private String newhardwareid = null;
    private long dateid;
    private String hwid;
    private static StampedLock lock;
    private int validUntilSec = -1;
    private HashMap<String, Integer> builds = new HashMap<String, Integer>(){
        {
            this.put("GENBUILDER", 144);
            this.put("SQEDITOR", 144);
            this.put("BACKTESTNODE", 144);
            this.put("QDM", 125);
            this.put("SQBR", 144);
        }
    };

    public void g5Tq3ZdPHp(String string) throws Exception {
        if (!this.builds.containsKey(string)) {
            throw new Exception(String.format("Unrecognized product %s.", string));
        }
        this.product = string;
    }

    public void rR6Ivukeqw(String string) {
        this.licenseFilePath = string;
    }

    public String getProduct() {
        return this.license == null ? null : this.license.getString("product");
    }

    public String getLicenseType() {
        return this.license == null ? null : this.license.getString("type");
    }

    public int getLicenseEdition() {
        return this.license == null ? -1 : Integer.parseInt(this.license.getString("edition"));
    }

    public String getLicenseOwner() {
        return this.license == null ? null : this.license.getString("name");
    }

    public String getLicenseCode() {
        return this.license == null ? null : this.license.getString("license");
    }

    public String getDataTrialExpires() {
        return this.license == null ? null : this.license.getString("data_trial_expires");
    }

    public boolean isSpecialTrial() {
        return this.license == null ? false : this.license.isSpecialTrial();
    }

    public String getLastCheckedLicenseCode() {
        return this.lastCheckedCode;
    }

    public String getValidUntil() {
        return this.license == null ? null : this.license.getString("valid_until");
    }

    public int getValidUntilSec() {
        if (this.validUntilSec == -1) {
            try {
                this.validUntilSec = Integer.parseInt(this.license.getString("valid_until_sec"));
            }
            catch (Exception exception) {
                return 0;
            }
        }
        return this.validUntilSec;
    }

    public String getEmail() {
        return this.license == null ? null : this.license.getString("email");
    }

    public boolean isPersonalized() {
        return this.license == null ? false : this.license.isPersonalized();
    }

    public boolean isItFullMbg() {
        return this.license == null ? false : this.license.isItFullMbg();
    }

    public boolean isOnlineLicense() {
        if (this.license == null) {
            return false;
        }
        return this.license.isItFullMbg() || this.license.isItTrial() || this.license.isItPartnerTrial();
    }

    public boolean isItTrial() {
        return this.license == null ? false : this.license.isItTrial();
    }

    public boolean isItPartnerTrial() {
        return this.license == null ? false : this.license.isItPartnerTrial();
    }

    public boolean isItFreeVersion() {
        return this.license == null ? false : this.license.isItFreeVersion();
    }

    public boolean verified() {
        return this.license != null;
    }

    public AbstractBroker getBroker() {
        if (this.license == null) {
            return null;
        }
        String string = this.license.getString("broker");
        if (string == null) {
            return null;
        }
        return Brokers.get(string);
    }

    public String getProperty(String string) {
        if (this.license == null || !this.license.contains(string)) {
            return "?";
        }
        return this.license.get(string).toString();
    }

    public String getHardwareID() {
        return this.newhardwareid;
    }

    public String getUniqID() {
        return this.uniqID;
    }

    public int getProductVersion() {
        return this.builds.containsKey(this.product) ? this.builds.get(this.product) : -1;
    }

    public static G8SyrBEfO8 getInstance() {
        long l = lock.writeLock();
        try {
            if (instance == null) {
                instance = new G8SyrBEfO8();
            }
            G8SyrBEfO8 g8SyrBEfO8 = instance;
            return g8SyrBEfO8;
        }
        finally {
            lock.unlock(l);
        }
    }

    private G8SyrBEfO8() {
        String string = "0";
        try {
            this.MACaddress = HardwareInfo.getMacAddress();
            string = string + "1";
            this.diskSN = HardwareInfo.getDiskSN();
            string = string + "2";
            this.uniqID = UUID.randomUUID().toString();
            string = string + "3";
            this.dateid = HardwareInfo.getMachineGuid();
            string = string + "4";
            this.hardwareid = DqO7PIpn7z.generateHardwareId(this.MACaddress, this.diskSN, this.dateid);
            string = string + "5";
            this.hwid = HardwareInfo.getId();
            string = string + "6";
            this.newhardwareid = !this.hwid.isEmpty() && !this.hwid.startsWith("error") ? "C" + DqO7PIpn7z.generateHardwareId(this.hwid, "", 0L) : (this.dateid == -1L ? "D" + DqO7PIpn7z.generateHardwareId(this.MACaddress, this.diskSN, this.dateid) : "R" + DqO7PIpn7z.generateHardwareId("", "", this.dateid));
            string = string + "7";
        }
        catch (Error error) {
            string = string + "Er: " + error.getMessage();
        }
        catch (Exception exception) {
            string = string + "Ex: " + exception.getMessage();
        }
        Log.debug("L: " + string);
    }

    public void resetLicense() {
        this.license = null;
    }

    public void verifyLicenseOnline(String string) throws Eob3xULZVY {
        this.verifyLicenseOnline(string, false);
    }

    private void verifyLicenseOnline(String string, boolean bl) throws Eob3xULZVY {
        try {
            Log.debug("Verifying license online");
            DVGBR8rirw dVGBR8rirw = this._verifyLicenseOnServer(this.MACaddress, this.diskSN, string, bl);
            this.verifyLicenseXmlSignature(dVGBR8rirw);
            this.license = DqO7PIpn7z.parseLicenseXml(dVGBR8rirw);
            this.checkLicense(this.license);
            String string2 = this.license.getString("uniqid");
            if (string2 == null) {
                throw new Eob3xULZVY(3);
            }
            if (!string2.equals(this.uniqID)) {
                throw new Eob3xULZVY(24);
            }
            if (!bl) {
                if (!(this.license.isItTrial() || this.license.isItPartnerTrial() || this.license.isItFullMbg())) {
                    DqO7PIpn7z.saveLicenseFile(dVGBR8rirw, this.licenseFilePath);
                } else {
                    this.removeLicenseFile();
                }
            }
        }
        catch (Eob3xULZVY eob3xULZVY) {
            if (eob3xULZVY.getErrorCode() != 2) {
                this.resetLicense();
            }
            throw eob3xULZVY;
        }
        catch (Exception exception) {
            this.resetLicense();
            throw exception;
        }
        finally {
            MainApp.clearLicenseInfo();
        }
    }

    public void updateLicense(String string) throws Eob3xULZVY {
        try {
            DVGBR8rirw dVGBR8rirw = this._verifyLicenseOnServer(this.MACaddress, this.diskSN, string, false);
            this.verifyLicenseXmlSignature(dVGBR8rirw);
            WbwPVQGc2h wbwPVQGc2h = DqO7PIpn7z.parseLicenseXml(dVGBR8rirw);
            this.checkLicense(wbwPVQGc2h);
            String string2 = wbwPVQGc2h.getString("uniqid");
            if (string2 == null) {
                throw new Eob3xULZVY(3);
            }
            if (!string2.equals(this.uniqID)) {
                throw new Eob3xULZVY(24);
            }
            if (!(wbwPVQGc2h.isItTrial() || wbwPVQGc2h.isItPartnerTrial() || wbwPVQGc2h.isItFullMbg())) {
                DqO7PIpn7z.saveLicenseFile(dVGBR8rirw, this.licenseFilePath);
            } else {
                this.removeLicenseFile();
            }
            this.license = wbwPVQGc2h;
        }
        catch (Exception exception) {
            throw exception;
        }
        finally {
            MainApp.clearLicenseInfo();
        }
    }

    public boolean removeLicenseFile() {
        return DqO7PIpn7z.removeLicenseFile(this.licenseFilePath);
    }

    public void verifyLicenseFromFile() throws Eob3xULZVY {
        try {
            if (this.product == null) {
                throw new Eob3xULZVY(3);
            }
            DVGBR8rirw dVGBR8rirw = DqO7PIpn7z.loadLicenseXmlFromFile(this.licenseFilePath);
            this.license = DqO7PIpn7z.parseLicenseXml(dVGBR8rirw);
            if (!this.license.getString("license").equalsIgnoreCase("FXVMTRIAL")) {
                this.verifyLicenseXmlSignature(dVGBR8rirw);
            } else {
                String string = this.license.getString("valid_until_int");
                Date date = new Date();
                if (date.getTime() / 1000L > (long)Integer.parseInt(string)) {
                    throw new Eob3xULZVY(5);
                }
            }
            this.checkLicense(this.license);
        }
        catch (Eob3xULZVY eob3xULZVY) {
            this.resetLicense();
            throw eob3xULZVY;
        }
    }

    private DVGBR8rirw _verifyLicenseOnServer(String string, String string2, String string3, boolean bl) throws Eob3xULZVY {
        try {
            DVGBR8rirw dVGBR8rirw = new DVGBR8rirw();
            dVGBR8rirw.type = 5;
            String string4 = URLEncoder.encode("productid", "UTF-8") + "=" + URLEncoder.encode(this.product, "UTF-8");
            string4 = string4 + "&" + URLEncoder.encode("mac", "UTF-8") + "=" + URLEncoder.encode(string, "UTF-8");
            string4 = string4 + "&" + URLEncoder.encode("dsn", "UTF-8") + "=" + URLEncoder.encode(string2, "UTF-8");
            string4 = string4 + "&" + URLEncoder.encode("license", "UTF-8") + "=" + URLEncoder.encode(string3, "UTF-8");
            string4 = string4 + "&" + URLEncoder.encode("resellerid", "UTF-8") + "=" + URLEncoder.encode(this.reseller, "UTF-8");
            string4 = string4 + "&" + URLEncoder.encode("uniqid", "UTF-8") + "=" + URLEncoder.encode(this.uniqID, "UTF-8");
            string4 = string4 + "&" + URLEncoder.encode("hardwareid", "UTF-8") + "=" + URLEncoder.encode(this.hardwareid, "UTF-8");
            string4 = string4 + "&" + URLEncoder.encode("newhardwareid", "UTF-8") + "=" + URLEncoder.encode(this.newhardwareid, "UTF-8");
            string4 = string4 + "&" + URLEncoder.encode("dateid", "UTF-8") + "=" + URLEncoder.encode(this.dateid + "", "UTF-8");
            string4 = string4 + "&" + URLEncoder.encode("os", "UTF-8") + "=" + URLEncoder.encode(HardwareInfo.getOSKey() + "", "UTF-8");
            string4 = string4 + "&" + URLEncoder.encode("hwparams", "UTF-8") + "=" + URLEncoder.encode(this.hwid, "UTF-8");
            if (bl) {
                string4 = string4 + "&" + URLEncoder.encode("offline", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8");
            }
            if (this.builds.containsKey(this.product)) {
                string4 = string4 + "&" + URLEncoder.encode("build", "UTF-8") + "=" + this.builds.get(this.product);
            }
            string4 = string4 + "&" + URLEncoder.encode("ival", "UTF-8") + "=" + URLEncoder.encode("3", "UTF-8");
            string4 = string4 + "&" + URLEncoder.encode("version", "UTF-8") + "=" + URLEncoder.encode(MainApp.getAppVersion(), "UTF-8");
            if (MainApp.isFirstRun()) {
                string4 = string4 + "&" + URLEncoder.encode("licterms", "UTF-8") + "=" + URLEncoder.encode("1", "UTF-8");
            }
            YbM3Hb5g8g ybM3Hb5g8g = new YbM3Hb5g8g();
            String string5 = null;
            String string6 = null;
            Lewgre21g.g5Tq3ZdPHp("LRequest-" + ybM3Hb5g8g.aAcrrGmF6h() + "?" + string4);
            String string7 = KIozbup0X4.call(ybM3Hb5g8g.aAcrrGmF6h(), string4);
            if (string7 == null) {
                throw new Eob3xULZVY(2);
            }
            Lewgre21g.g5Tq3ZdPHp("LResponse-" + string7);
            if (string7 != null) {
                String[] stringArray = string7.split("\n");
                if (stringArray.length != 2) {
                    throw new Exception("Invalid response, 2 lines expected.");
                }
                string5 = new String(SQDecoderEncoder.decode(stringArray[0]), "UTF-8");
                string6 = stringArray[1];
                Lewgre21g.g5Tq3ZdPHp("LResponseXml-" + string5);
                Lewgre21g.g5Tq3ZdPHp("LResponseHash-" + string6);
            }
            if (string5 == null || string6 == null) {
                throw new Eob3xULZVY(9);
            }
            dVGBR8rirw.hash = SQDecoderEncoder.decode(string6);
            dVGBR8rirw.xml = string5;
            return dVGBR8rirw;
        }
        catch (Eob3xULZVY eob3xULZVY) {
            throw eob3xULZVY;
        }
        catch (Exception exception) {
            Log.error("Exc.", (Throwable)exception);
            throw new Eob3xULZVY(20);
        }
    }

    private void verifyLicenseXmlSignature(DVGBR8rirw dVGBR8rirw) throws Eob3xULZVY {
        Lewgre21g.g5Tq3ZdPHp("LGenerating hash for xml: " + dVGBR8rirw.xml);
        String string = DqO7PIpn7z.decryptLicenseXmlSignature(dVGBR8rirw.hash);
        String string2 = DqO7PIpn7z.generateControlHash(dVGBR8rirw.xml);
        Lewgre21g.g5Tq3ZdPHp("Lhash-" + string);
        Lewgre21g.g5Tq3ZdPHp("LNewHash-" + string2);
        if (!string.equals(string2)) {
            Lewgre21g.g5Tq3ZdPHp("LSignature Invalid");
            throw new Eob3xULZVY(8, "Error - Invalid license signature.");
        }
        Lewgre21g.g5Tq3ZdPHp("LSignature OK");
    }

    private void checkLicense(WbwPVQGc2h wbwPVQGc2h) throws Eob3xULZVY {
        int n;
        this.lastCheckedCode = wbwPVQGc2h.getString("license");
        if (wbwPVQGc2h.getString("status") == null) {
            throw new Eob3xULZVY(3);
        }
        if (wbwPVQGc2h.getString("status").equals("invalid")) {
            if (wbwPVQGc2h.contains("error_code") && wbwPVQGc2h.getString("error_code").equals("7")) {
                throw new Eob3xULZVY(40, wbwPVQGc2h.getString("error") + ".");
            }
            if (wbwPVQGc2h.getString("error") != null) {
                throw new Eob3xULZVY(3, "License is invalid - " + wbwPVQGc2h.getString("error") + ".");
            }
            throw new Eob3xULZVY(3);
        }
        if (this.builds.containsKey(this.product)) {
            try {
                n = this.builds.get(this.product);
                int n2 = Integer.parseInt(wbwPVQGc2h.getString("build"));
                if (n != n2) {
                    throw new Exception("License not supported for this build.");
                }
            }
            catch (Exception exception) {
                throw new Eob3xULZVY(30);
            }
        }
        try {
            Integer.parseInt(wbwPVQGc2h.getString("edition"));
        }
        catch (Exception exception) {
            throw new Eob3xULZVY(26);
        }
        if (wbwPVQGc2h.getString("type") == null) {
            throw new Eob3xULZVY(3);
        }
        if (!wbwPVQGc2h.isPersonalized() && wbwPVQGc2h.getString("checkDSN").equals("1")) {
            this.checkHardwareID(wbwPVQGc2h);
        }
        if (wbwPVQGc2h.getString("product") == null) {
            throw new Eob3xULZVY(3);
        }
        if (!wbwPVQGc2h.getString("product").equals(this.product)) {
            throw new Eob3xULZVY(7);
        }
        if (wbwPVQGc2h.getString("reseller") == null) {
            throw new Eob3xULZVY(3);
        }
        if (!wbwPVQGc2h.getString("reseller").equals(this.reseller)) {
            throw new Eob3xULZVY(23);
        }
        try {
            n = Integer.parseInt(wbwPVQGc2h.getString("ival"));
            if (n < 1) {
                throw new Exception("License not supported for this build.");
            }
        }
        catch (Exception exception) {
            throw new Eob3xULZVY(30);
        }
        if (wbwPVQGc2h.contains("date_verified")) {
            long l = SQTime.parseToMilis(wbwPVQGc2h.getString("date_verified"), "yyyy-MM-dd HH:mm:ss");
            if (System.currentTimeMillis() - l > TimeUnit.DAYS.toMillis(15L)) {
                throw new Eob3xULZVY(31);
            }
        }
    }

    private void checkHardwareID(WbwPVQGc2h wbwPVQGc2h) throws Eob3xULZVY {
        if (!wbwPVQGc2h.getString("newhardwareid").equals(this.newhardwareid)) {
            throw new Eob3xULZVY(29);
        }
    }

    public boolean licenseFileExists() {
        return DqO7PIpn7z.licenseFileExists(this.licenseFilePath);
    }

    public static void checkLicense() {
        block2: {
            G8SyrBEfO8 g8SyrBEfO8 = G8SyrBEfO8.getInstance();
            try {
                g8SyrBEfO8.verifyLicenseOnline(g8SyrBEfO8.getLicenseCode());
            }
            catch (Eob3xULZVY eob3xULZVY) {
                if (eob3xULZVY.getErrorCode() == 2) break block2;
                MainApp.exitJVM("invalid license", 1);
            }
        }
    }

    public String eyGcvZ1s() {
        String string = this.newhardwareid.startsWith("D") ? "Zo3P1y4y" : SQDecoderEncoder.encode2String((this.newhardwareid + "").getBytes()).substring(0, 8);
        File file = new File(MainApp.getDataPath() + "internal/web/HOME/" + string + ".js");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        return file.getAbsolutePath();
    }

    static {
        lock = new StampedLock();
    }
}

