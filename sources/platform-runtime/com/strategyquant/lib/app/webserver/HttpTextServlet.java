/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.app.webserver;

import com.strategyquant.lib.app.webserver.AbstractTextServlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpTextServlet
extends AbstractTextServlet {
    private static final Logger Log = LoggerFactory.getLogger(HttpTextServlet.class);
    public static long requestsHandled = 0L;
    public static long requestErrors = 0L;
    public static final Object lock = new Object();

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        String string;
        super.doGet(httpServletRequest, httpServletResponse);
        Object object = lock;
        synchronized (object) {
            ++requestsHandled;
        }
        object = "ok";
        int n = 200;
        HashMap<String, String[]> hashMap = new HashMap<String, String[]>();
        try {
            if (httpServletRequest.getParameterMap() != null && !httpServletRequest.getParameterMap().isEmpty()) {
                hashMap.putAll(httpServletRequest.getParameterMap());
                this.tryLoadFilePart(httpServletRequest, hashMap);
            } else {
                this.getRequestParams(httpServletRequest, hashMap);
            }
            string = httpServletRequest.getRemoteHost();
            String string2 = "" + httpServletRequest.getRemotePort();
            hashMap.put("remoteIP", new String[]{string});
            hashMap.put("remotePort", new String[]{string2});
            String string3 = httpServletRequest.getPathInfo().substring(1);
            if (Log.isDebugEnabled()) {
                Log.debug("Incoming command: " + httpServletRequest.getContextPath() + " " + httpServletRequest.getPathInfo());
            }
            this.dumpParams(httpServletRequest.getParameterMap());
            object = this.execute(httpServletRequest.getPathInfo(), string3, hashMap, httpServletRequest.getMethod(), httpServletRequest.getQueryString());
            if (object == null) {
                throw new NullPointerException("Request not processed.");
            }
        }
        catch (Exception exception) {
            Log.error("Execution failed. Request URL: " + httpServletRequest.getRequestURI() + ". ", (Throwable)exception);
            Object object2 = lock;
            synchronized (object2) {
                ++requestErrors;
            }
            n = 200;
            object = HttpTextServlet.apiError(null, exception);
        }
        catch (Error error) {
            Log.error("Execution failed. Request URL: " + httpServletRequest.getRequestURI() + ". ", (Throwable)error);
            Object object3 = lock;
            synchronized (object3) {
                ++requestErrors;
            }
            n = 200;
            object = HttpTextServlet.apiError(null, error);
        }
        httpServletResponse.setCharacterEncoding("utf-8");
        httpServletResponse.setHeader("Access-Control-Allow-Origin", "*");
        httpServletResponse.addHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, PUT, DELETE, HEAD");
        httpServletResponse.addHeader("Access-Control-Allow-Headers", "*");
        httpServletResponse.addHeader("Connection", "close");
        string = "text/html";
        httpServletResponse.getWriter().print((String)object);
        httpServletResponse.setContentType(string);
        httpServletResponse.setStatus(n);
        httpServletResponse.flushBuffer();
    }

    public static String apiError(String string, Throwable throwable) {
        if (throwable == null && string == null) {
            string = "Internal server error";
        } else {
            String string2 = string = string == null ? "" : string.replace("\"", "'") + "<br>";
            if (throwable != null) {
                String string3 = throwable.getMessage() != null ? throwable.getMessage().replace("\"", "'") : throwable.getClass().getName();
                string = string + string3;
            }
        }
        Log.error(string, throwable);
        return "Error: " + string;
    }
}

