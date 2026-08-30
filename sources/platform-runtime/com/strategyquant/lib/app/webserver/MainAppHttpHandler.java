/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.app.webserver;

import com.strategyquant.lib.L;
import com.strategyquant.lib.L88OaFjjon.Eob3xULZVY;
import com.strategyquant.lib.L88OaFjjon.V571hfnsHw;
import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.app.impl.IConsoleImpl;
import com.strategyquant.lib.app.webserver.HttpTextServlet;
import java.util.Map;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainAppHttpHandler
extends HttpTextServlet {
    private static final Logger Log = LoggerFactory.getLogger(MainAppHttpHandler.class);

    @Override
    public String execute(String string, String string2, Map<String, String[]> map, String string3, String string4) throws Exception {
        if (string.startsWith("/license/")) {
            return this.onLicense(string.replaceFirst("/license/", ""));
        }
        switch (string2) {
            case "status": {
                return this.onStatus();
            }
            case "call": {
                return this.onCall(string4);
            }
            case "exit": {
                return this.onExit();
            }
        }
        return "Unable to resolve the request " + string2;
    }

    private String onExit() {
        JSONObject jSONObject = new JSONObject();
        new Thread(){

            @Override
            public void run() {
                try {
                    Thread.sleep(1000L);
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
                MainApp.exitApp();
            }
        }.start();
        jSONObject.put("success", (Object)L.t("Exiting app...", new Object[0]));
        return jSONObject.toString();
    }

    private String onCall(String string) throws Exception {
        IConsoleImpl iConsoleImpl = MainApp.cli;
        if (iConsoleImpl == null) {
            throw new Exception("CLI not ready.");
        }
        if (!string.startsWith("cmd=")) {
            throw new Exception("Parameter 'cmd' is missing.");
        }
        String string2 = string.replaceFirst("cmd=", "");
        String[] stringArray = string2.replace("%20", " ").split("\\s+");
        String string3 = iConsoleImpl.runRemoteCommand(stringArray);
        return string3;
    }

    private String onStatus() {
        JSONObject jSONObject = new JSONObject();
        jSONObject.put("product", (Object)MainApp.getProduct());
        jSONObject.put("path", (Object)MainApp.getDataPath());
        return jSONObject.toString();
    }

    public String onLicense(String string) throws Exception {
        String string2;
        String[] stringArray = string.split(":");
        switch (string2 = stringArray[0]) {
            case "getdetails": {
                return this.onGetdetails();
            }
            case "verify": {
                return this.onVerify(stringArray.length > 1 ? stringArray[1] : "");
            }
            case "help": {
                return this.onHelp();
            }
            case "btn1": {
                return this.onBtn1();
            }
            case "btn2": {
                return this.onBtn2();
            }
            case "btn3": {
                return this.onBtn3();
            }
            case "btn4": {
                return this.onBtn4();
            }
            case "licproblem": {
                return this.onLicenseProblem();
            }
            case "upgrade": {
                return this.onUpgrade();
            }
            case "downgrade": {
                return this.onDowngrade();
            }
            case "showAlert": {
                return this.onShowAlert(stringArray[1], stringArray[2]);
            }
            case "showLicenseTerms": {
                return this.onShowLicenseTerms();
            }
        }
        throw new Exception("Unknown license command '" + string2 + "'.");
    }

    private String onUpgrade() {
        JSONObject jSONObject = new JSONObject();
        SQUtils.openUrlInDefaultWebBrowser("https://strategyquant.com/support-updates");
        return jSONObject.toString();
    }

    private String onDowngrade() {
        JSONObject jSONObject = new JSONObject();
        SQUtils.openUrlInDefaultWebBrowser("https://strategyquant.com/doc/strategyquant/how-to-downgrade");
        return jSONObject.toString();
    }

    private String onLicenseProblem() {
        JSONObject jSONObject = new JSONObject();
        SQUtils.openUrlInDefaultWebBrowser("www.strategyquant.com/contactus");
        return jSONObject.toString();
    }

    private String onBtn1() {
        JSONObject jSONObject = new JSONObject();
        if (MainApp.checkProduct("QDM")) {
            SQUtils.openUrlInDefaultWebBrowser("https://strategyquant.com/quantdatamanager#proversion");
        } else {
            SQUtils.openUrlInDefaultWebBrowser("www.strategyquant.com/purchase");
        }
        return jSONObject.toString();
    }

    private String onBtn2() {
        JSONObject jSONObject = new JSONObject();
        if (MainApp.checkProduct("QDM")) {
            SQUtils.openUrlInDefaultWebBrowser("https://strategyquant.com/quantdatamanager");
        } else {
            SQUtils.openUrlInDefaultWebBrowser("www.strategyquant.com");
        }
        return jSONObject.toString();
    }

    private String onBtn4() {
        JSONObject jSONObject = new JSONObject();
        if (MainApp.checkProduct("QDM")) {
            SQUtils.openUrlInDefaultWebBrowser("https://strategyquant.com/quantdatamanager#freeversion");
        }
        return jSONObject.toString();
    }

    private String onBtn3() {
        JSONObject jSONObject = new JSONObject();
        V571hfnsHw v571hfnsHw = MainApp.v571hfnsHw();
        if (!v571hfnsHw.gWfGtoRYJG()) {
            int n = MainApp.awaitUserConfirmation(L.t("User Confirmation", new Object[0]), L.t("Are you sure you want to exit?", new Object[0]));
            if (n == 1) {
                MainApp.exitApp();
            }
        } else {
            MainApp.license.licenseChecked = true;
        }
        return jSONObject.toString();
    }

    private String onHelp() {
        JSONObject jSONObject = new JSONObject();
        SQUtils.openUrlInDefaultWebBrowser("http://www.strategyquant.com/license-troubleshooting");
        return jSONObject.toString();
    }

    private String onGetdetails() {
        JSONObject jSONObject = new JSONObject();
        V571hfnsHw v571hfnsHw = MainApp.v571hfnsHw();
        jSONObject.put("appversion", (Object)MainApp.printAppVersion(true));
        jSONObject.put("hardwareid", (Object)v571hfnsHw.nmFllxIfvN());
        if (MainApp.license.errorMsg == null) {
            jSONObject.put("success", true);
            jSONObject.put("license_info", (Object)this.getLicenseInfo());
        } else {
            jSONObject.put("success", false);
            jSONObject.put("license_info", (Object)MainApp.license.errorMsg);
        }
        if (MainApp.license.lc != null) {
            jSONObject.put("license", (Object)MainApp.license.lc);
        }
        return jSONObject.toString();
    }

    private String onShowAlert(String string, String string2) {
        JSONObject jSONObject = new JSONObject();
        MainApp.showErrorDialog(string, string2);
        return jSONObject.toString();
    }

    private String onShowLicenseTerms() {
        JSONObject jSONObject = new JSONObject();
        String string = "file:///" + MainApp.getDataPath() + "SQ_License_and_Service_Terms.pdf";
        JSONObject jSONObject2 = new JSONObject().put("url", (Object)string).put("width", 1024).put("height", 728).put("title", (Object)L.t("License and Service Terms", new Object[0]));
        MainApp.sendMessage("modalWindow", jSONObject2);
        return jSONObject.toString();
    }

    private String onVerify(String string) {
        Log.debug("Verifying license: '" + string + "'");
        JSONObject jSONObject = new JSONObject();
        string = string.trim();
        if (string.equalsIgnoreCase("")) {
            jSONObject.put("license_panel_info", (Object)L.t("Please enter valid license number!", new Object[0]));
            jSONObject.put("success", false);
            return jSONObject.toString();
        }
        try {
            MainApp.rsq3UErJhC(string);
            jSONObject.put("license_panel_info", (Object)L.t("License OK", new Object[0]));
            jSONObject.put("license_info", (Object)this.getLicenseInfo());
            jSONObject.put("success", true);
            V571hfnsHw v571hfnsHw = MainApp.v571hfnsHw();
            if (v571hfnsHw.yIifbIrWD3()) {
                jSONObject.put("btn3", (Object)(L.t("Continue Trial", new Object[0]) + " >"));
            } else if (v571hfnsHw.cJMHUhmpvk()) {
                jSONObject.put("btn3", (Object)(L.t("Continue Eval", new Object[0]) + " >"));
            }
            MainApp.license.licenseChecked = true;
        }
        catch (Eob3xULZVY eob3xULZVY) {
            Log.error("Exc.", (Throwable)eob3xULZVY);
            jSONObject.put("license_panel_info", (Object)L.t("License Verification Failed!", new Object[0]));
            jSONObject.put("license_info", (Object)eob3xULZVY.getMessage());
            jSONObject.put("license_support_expired", eob3xULZVY.getErrorCode() == 40);
            jSONObject.put("success", false);
        }
        catch (Exception exception) {
            Log.error("Exc.", (Throwable)exception);
            jSONObject.put("license_panel_info", (Object)L.t("License Verification Failed!", new Object[0]));
            jSONObject.put("license_info", (Object)exception.getMessage());
            jSONObject.put("success", false);
        }
        return jSONObject.toString();
    }

    private String getLicenseInfo() {
        V571hfnsHw v571hfnsHw = MainApp.v571hfnsHw();
        if (!v571hfnsHw.gWfGtoRYJG()) {
            return "";
        }
        String string = "";
        string = L.t("License number: %s, licensed to %s", v571hfnsHw.eHvc2JguAd(), v571hfnsHw.oOkoIeUsMG());
        if (v571hfnsHw.rbaiBK0Thx()) {
            string = string + " - " + v571hfnsHw.fSECzwVwpK();
        }
        string = string + "<br>" + L.t("Pro version", new Object[0]) + "<br>";
        if (v571hfnsHw.yIifbIrWD3()) {
            if (!v571hfnsHw.a1wUchdumV()) {
                string = string + String.format(" (%s) - %s %s", L.t("TRIAL license", new Object[0]), L.t("valid until", new Object[0]), v571hfnsHw.cGFrCLK8fN());
            }
        } else {
            string = v571hfnsHw.cJMHUhmpvk() ? string + String.format(" (%s) - %s %s", L.t("PARTNER EVALUATION license", new Object[0]), L.t("valid until", new Object[0]), v571hfnsHw.cGFrCLK8fN()) : string + String.format(" (%s)", L.t("FULL license", new Object[0]));
        }
        return string;
    }
}
