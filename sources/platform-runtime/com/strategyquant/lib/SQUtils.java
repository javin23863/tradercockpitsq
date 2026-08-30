/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib;

import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import com.google.common.hash.Hashing;
import com.strategyquant.lib.L;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.hw.OperatingSystem;
import com.strategyquant.lib.utils.ISQCloneable;
import com.strategyquant.lib.utils.IUniqueNameChecker;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import java.awt.Component;
import java.awt.Desktop;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;
import javax.net.ssl.SSLContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.comparator.NameFileComparator;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.AllowAllHostnameVerifier;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.X509HostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQUtils {
    private static final Logger Log = LoggerFactory.getLogger(SQUtils.class);
    private static final HashFunction hashFunction = Hashing.sipHash24();
    private static ThreadLocal<DecimalFormat> instrumentDF = ThreadLocal.withInitial(() -> new DecimalFormat("#.########", new DecimalFormatSymbols(Locale.ENGLISH)));
    static final double p6 = 1000000.0;
    static final double p7 = 1.0E7;
    static final double factor2 = 1.0E-10;
    private static ThreadLocal<NumberFormat> d10Form = ThreadLocal.withInitial(() -> new DecimalFormat("0.0000000000", DecimalFormatSymbols.getInstance(Locale.ROOT)));
    private static ThreadLocal<NumberFormat> decimalFormater = ThreadLocal.withInitial(() -> new DecimalFormat("#.#####", DecimalFormatSymbols.getInstance(Locale.ROOT)));
    private static int BuyLimit = 3;
    private static int SellLimit = 4;
    private static int BuyStop = 5;
    private static int SellStop = 6;
    private static int BuyToCoverLimit = 101;
    private static int SellToCoverLimit = 103;
    private static int BuyToCoverStop = 100;
    private static int SellToCoverStop = 102;
    private static final int[] POW10 = new int[]{1, 10, 100, 1000, 10000, 100000, 1000000};
    private static final String nullStringCorrection = "";

    public static void openUrlInDefaultWebBrowser(String string) {
        try {
            OperatingSystem operatingSystem = new OperatingSystem();
            if (operatingSystem.isUnix()) {
                Runtime.getRuntime().exec(new String[]{"xdg-open", string});
            } else {
                if (!Desktop.isDesktopSupported()) {
                    throw new Exception("This function is not supported.");
                }
                Desktop desktop = Desktop.getDesktop();
                if (!desktop.isSupported(Desktop.Action.BROWSE)) {
                    throw new Exception("This function is not supported.");
                }
                URI uRI = new URI(string);
                desktop.browse(uRI);
            }
        }
        catch (Exception exception) {
            Log.error("Failed to open url in default web browser - " + exception.getMessage(), (Throwable)exception);
        }
    }

    public static void forceOpenUrlInDefaultWebBrowser(String string, Component component) {
        if (!Desktop.isDesktopSupported()) {
            MainApp.showInfoDialog("Info", "This function is not supported.");
            return;
        }
        Desktop desktop = Desktop.getDesktop();
        if (!desktop.isSupported(Desktop.Action.BROWSE)) {
            MainApp.showInfoDialog("Info", "This function is not supported.");
            return;
        }
        try {
            URI uRI = new URI(string);
            desktop.browse(uRI);
        }
        catch (Exception exception) {
            System.err.println(exception.getMessage());
        }
    }

    public static byte[] fileToBytes(File file) throws Exception {
        byte[] byArray = null;
        FilterInputStream filterInputStream = null;
        try {
            byArray = new byte[(int)file.length()];
            filterInputStream = new DataInputStream(new FileInputStream(file));
            ((DataInputStream)filterInputStream).readFully(byArray);
        }
        catch (Exception exception) {
            throw new Exception("IO problem. Convert FileToBytes failed. File:" + file.getAbsolutePath(), exception);
        }
        finally {
            try {
                if (filterInputStream != null) {
                    filterInputStream.close();
                }
            }
            catch (IOException iOException) {}
        }
        return byArray;
    }

    public static String md5CheckSum(byte[] byArray) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("MD5");
        try {
            byte[] byArray2 = messageDigest.digest(byArray);
            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < byArray2.length; ++i) {
                stringBuffer.append(Integer.toString((byArray2[i] & 0xFF) + 256, 16).substring(1));
            }
            return stringBuffer.toString();
        }
        catch (Exception exception) {
            throw new Exception("Generate MD5 check sum failed. Exc. ", exception);
        }
    }

    public static String file2md5Hash(File file) {
        try {
            String string;
            try (FileInputStream fileInputStream = new FileInputStream(file);){
                string = DigestUtils.md5Hex((InputStream)fileInputStream);
            }
            return string;
        }
        catch (IOException iOException) {
            throw new RuntimeException(L.t("Error reading file to hash in %s", file.getAbsolutePath()), iOException);
        }
    }

    public static String fileToString(String string) throws Exception {
        return SQUtils.fileToString(new File(string));
    }

    public static String fileToString(File file) throws Exception {
        return SQUtils.fileToString(file, "UTF-8");
    }

    public static String fileToString(File file, String string) throws Exception {
        String string2 = null;
        FilterInputStream filterInputStream = null;
        try {
            if (!file.exists()) {
                throw new Exception(String.format("File '%s' doesn't exist.", file.getAbsolutePath()));
            }
            string2 = new String(Files.readAllBytes(file.toPath()), Charset.forName(string));
            string2 = SQUtils.removeUTF8BOM(string2);
        }
        catch (Exception exception) {
            throw exception;
        }
        finally {
            try {
                if (filterInputStream != null) {
                    filterInputStream.close();
                }
            }
            catch (IOException iOException) {}
        }
        return string2;
    }

    public static void createAndHandleBackupFiles(String string, String string2, int n) {
        Object object;
        File file = new File(string);
        File file2 = new File(file.getParent());
        String string3 = file.getName();
        string3 = string3.substring(0, string3.lastIndexOf("."));
        Path path = Paths.get(file2.getPath(), string3 + "-backups");
        File file3 = path.toFile();
        if (!file3.exists()) {
            file3.mkdir();
        }
        String string4 = nullStringCorrection;
        if (string2 != nullStringCorrection) {
            try {
                string4 = SQUtils.fileToString(file);
            }
            catch (Exception exception) {
                Log.error(exception.getMessage());
            }
        }
        if (file.exists() && !string2.equals(string4)) {
            object = Long.toString(new Date(System.currentTimeMillis()).getTime());
            String string5 = "(.*)\\.(\\S{2,4})$";
            String object2 = string.replaceAll(string5, (String)object + ".$2");
            Path path2 = Paths.get(path.toString(), object2);
            file.renameTo(path2.toFile());
        }
        object = file3.listFiles();
        Arrays.sort(object, NameFileComparator.NAME_COMPARATOR);
        ArrayUtils.reverse((Object[])object);
        int n2 = 0;
        for (Object object2 : object) {
            if (n2 >= n) {
                ((File)object2).delete();
            }
            ++n2;
        }
    }

    public static void createAndHandleBackupFiles(String string) {
        SQUtils.createAndHandleBackupFiles(string, nullStringCorrection, 10);
    }

    public static JSONArray listBackupFiles(String string) {
        JSONArray jSONArray = new JSONArray();
        File file = new File(string);
        File file2 = new File(file.getParent());
        String string2 = file.getName();
        string2 = string2.substring(0, string2.lastIndexOf("."));
        Path path = Paths.get(file2.getPath(), string2 + "-backups");
        File file3 = path.toFile();
        if (!file3.exists()) {
            file3.mkdir();
        }
        Object[] objectArray = file3.listFiles();
        Arrays.sort(objectArray, NameFileComparator.NAME_COMPARATOR);
        ArrayUtils.reverse((Object[])objectArray);
        for (Object object : objectArray) {
            String string3 = ((File)object).getName();
            string3 = string3.substring(0, string3.lastIndexOf("."));
            try {
                Long l = Long.parseLong(string3);
                String string4 = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(new Date(l));
                String string5 = SQUtils.fileToString((File)object);
                int n = StringUtils.countMatches((CharSequence)string5, (CharSequence)"<Item ");
                int n2 = StringUtils.countMatches((CharSequence)string5, (CharSequence)"<Group ");
                int n3 = StringUtils.countMatches((CharSequence)string5, (CharSequence)"<Item key=\"CBlock_");
                jSONArray.put((Object)new JSONObject().put("path", (Object)((File)object).toString()).put("date", (Object)string4).put("building_blocks", (Object)Integer.toString(n -= n3)).put("groups_random", (Object)Integer.toString(n2)).put("groups_custom", (Object)Integer.toString(n3)));
            }
            catch (Exception exception) {
                Log.error(exception.getMessage());
            }
        }
        return jSONArray;
    }

    public static void stringToFile(String string, String string2) {
        SQUtils.stringToFile(new File(string), string2);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static void stringToFile(File file, String string) {
        BufferedWriter bufferedWriter = null;
        try {
            if (!file.exists() && file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            bufferedWriter = new BufferedWriter(new OutputStreamWriter((OutputStream)new FileOutputStream(file), StandardCharsets.UTF_8));
            bufferedWriter.write(string);
        }
        catch (Exception exception) {
            Log.error("Write to file failed! File=" + file.getAbsolutePath(), (Throwable)exception);
        }
        finally {
            try {
                if (bufferedWriter != null) {
                    bufferedWriter.close();
                }
            }
            catch (IOException iOException) {}
        }
    }

    public static File getFirstFileFromDir(File file) throws Exception {
        File[] fileArray;
        if (file == null || !file.exists() || !file.isDirectory()) {
            return null;
        }
        for (File file2 : fileArray = file.listFiles()) {
            if (file2.isDirectory()) {
                File file3 = SQUtils.getFirstFileFromDir(file2);
                if (file3 == null) continue;
                return file3;
            }
            return file2;
        }
        return null;
    }

    public static boolean checkWorkDirectoryIsWritable(String string) {
        String string2 = string + "temp.txt";
        try {
            PrintWriter printWriter = new PrintWriter(new BufferedWriter(new FileWriter(string2, StandardCharsets.UTF_8)));
            printWriter.write("test");
            printWriter.close();
            File file = new File(string2);
            file.delete();
        }
        catch (Exception exception) {
            return false;
        }
        return true;
    }

    public static int convertPriceToInt(double d, int n) {
        switch (n) {
            case 0: {
                return (int)d;
            }
            case 1: {
                return (int)(d * 10.0);
            }
            case 2: {
                return (int)(d * 100.0);
            }
            case 3: {
                return (int)(d * 1000.0);
            }
            case 4: {
                return (int)(d * 10000.0);
            }
            case 5: {
                return (int)(d * 100000.0);
            }
        }
        return 0;
    }

    public static double round(double d, int n) {
        if (d == 0.0) {
            return 0.0;
        }
        double d2 = 0.0;
        d += 1.0E-10;
        switch (n) {
            case 1: {
                d2 = 10.0;
                break;
            }
            case 2: {
                d2 = 100.0;
                break;
            }
            case 3: {
                d2 = 1000.0;
                break;
            }
            case 4: {
                d2 = 10000.0;
                break;
            }
            case 5: {
                d2 = 100000.0;
                break;
            }
            case 6: {
                d2 = 1000000.0;
                break;
            }
            case 7: {
                d2 = 1.0E7;
                break;
            }
            case 8: {
                d2 = 1.0E8;
                break;
            }
            default: {
                d2 = Math.pow(10.0, n);
            }
        }
        double d3 = Math.round(d *= d2);
        return d3 / d2;
    }

    public static double round7(double d) {
        d = (d + 1.0E-10) * 1.0E7;
        double d2 = Math.round(d);
        return d2 / 1.0E7;
    }

    public static double round6(double d) {
        d = (d + 1.0E-10) * 1000000.0;
        double d2 = Math.round(d);
        return d2 / 1000000.0;
    }

    public static Integer round(double d) {
        float f = Math.round(d);
        return (int)f;
    }

    public static Long roundLong(double d) {
        Double d2 = d;
        long l = d2.longValue();
        return l;
    }

    public static double adjustToTickSize(double d, double d2) {
        d = d % d2 >= d2 / 2.0 ? d + d2 : d;
        return (double)((int)(d / d2)) * d2;
    }

    public static double round2(double d) {
        return SQUtils.round(d, 2);
    }

    public static double safeDivide(double d, double d2) {
        if (d2 == 0.0 || Math.abs(d2) < 1.0E-14) {
            return 0.0;
        }
        return d / d2;
    }

    public static String trimFilePath(String string, String string2) {
        string = string.replaceAll(Pattern.quote("\\"), "/").replaceAll(Pattern.quote("//"), "/");
        string2 = string2.replaceAll(Pattern.quote("\\"), "/").replaceAll(Pattern.quote("//"), "/");
        return string.replaceAll(Pattern.quote(string2), nullStringCorrection);
    }

    public static String remapFilePathByMap(String string, Map<String, String> map) {
        String string2 = string.replaceAll(Pattern.quote("\\"), "/");
        NavigableMap<String, String> navigableMap = new TreeMap<String, String>(map).descendingMap();
        String string3 = string2;
        for (Map.Entry entry : navigableMap.entrySet()) {
            String string4 = (String)entry.getKey();
            String string5 = (String)entry.getValue();
            if (!string2.startsWith(string4)) continue;
            string3 = string2.replaceFirst(Pattern.quote(string4), Matcher.quoteReplacement(string5));
            break;
        }
        return string3;
    }

    public static String getExtension(String string) {
        if (string.lastIndexOf(46) == -1) {
            return string;
        }
        int n = string.lastIndexOf(46);
        if (n == string.length()) {
            return string;
        }
        return string.substring(n + 1).trim();
    }

    public static String stripExtension(String string) {
        int n = string.lastIndexOf(46);
        if (n >= 0) {
            string = string.substring(0, n);
        }
        return string;
    }

    public static String replaceLast(String string, String string2, String string3) {
        int n = string.lastIndexOf(string2);
        if (n < 0) {
            return string;
        }
        String string4 = string.substring(n).replaceFirst(string2, string3);
        return string.substring(0, n) + string4;
    }

    public static String replaceAll(String string, String string2, String string3) {
        if (string == null) {
            return null;
        }
        int n = 0;
        if ((n = string.indexOf(string2, n)) >= 0) {
            char[] cArray = string.toCharArray();
            char[] cArray2 = string3.toCharArray();
            int n2 = string2.length();
            StringBuilder stringBuilder = new StringBuilder(cArray.length);
            stringBuilder.append(cArray, 0, n).append(cArray2);
            int n3 = n += n2;
            while ((n = string.indexOf(string2, n)) > 0) {
                stringBuilder.append(cArray, n3, n - n3).append(cArray2);
                n3 = n += n2;
            }
            stringBuilder.append(cArray, n3, cArray.length - n3);
            string = stringBuilder.toString();
            stringBuilder.setLength(0);
        }
        return string;
    }

    public static String joinStrings(String ... stringArray) {
        String string = nullStringCorrection;
        for (String string2 : stringArray) {
            string = string + string2 + ",";
        }
        string = SQUtils.replaceLast(string, ",", nullStringCorrection);
        return string;
    }

    public static String[] listToStringArray(ArrayList<String> arrayList) {
        String[] stringArray = new String[arrayList.size()];
        for (int i = 0; i < arrayList.size(); ++i) {
            stringArray[i] = arrayList.get(i);
        }
        return stringArray;
    }

    public static boolean deleteRecursive(File file) throws FileNotFoundException {
        if (!file.exists()) {
            return true;
        }
        boolean bl = true;
        if (file.isDirectory()) {
            for (File file2 : file.listFiles()) {
                bl = bl && SQUtils.deleteRecursive(file2);
            }
        }
        return bl && file.delete();
    }

    public static boolean deleteNestedFiles(File file) {
        if (!file.exists()) {
            return true;
        }
        boolean bl = true;
        if (file.isDirectory()) {
            for (File file2 : file.listFiles()) {
                bl = bl && SQUtils.deleteNestedFiles(file2);
            }
        } else {
            bl = bl && file.delete();
        }
        return bl;
    }

    public static String generateUniqueName(String string, IUniqueNameChecker iUniqueNameChecker) throws Exception {
        if (!iUniqueNameChecker.checkNameExist(string)) {
            return string.intern();
        }
        int n = string.indexOf("(");
        if (n > 0) {
            string = string.substring(0, n);
        }
        String string2 = string;
        int n2 = 2;
        while (true) {
            StringBuilder stringBuilder = new StringBuilder(string);
            stringBuilder.append("(");
            stringBuilder.append(n2);
            stringBuilder.append(")");
            string2 = stringBuilder.toString();
            if (!iUniqueNameChecker.checkNameExist(string2)) {
                return string2.intern();
            }
            ++n2;
        }
    }

    public static double roundDown(double d, int n) {
        double d2 = 0.0;
        switch (n) {
            case 0: {
                return (int)d;
            }
            case 1: {
                d2 = 10.0;
                break;
            }
            case 2: {
                d2 = 100.0;
                break;
            }
            case 3: {
                d2 = 1000.0;
                break;
            }
            case 4: {
                d2 = 10000.0;
                break;
            }
            case 5: {
                d2 = 100000.0;
                break;
            }
            case 6: {
                d2 = 1000000.0;
                break;
            }
            default: {
                d2 = Math.pow(10.0, n);
            }
        }
        double d3 = Math.floor((d *= d2) + 1.0E-10);
        return d3 / d2;
    }

    public static <T> T invokeUnchecked(Constructor<T> constructor, Object ... objectArray) {
        try {
            return constructor.newInstance(objectArray);
        }
        catch (IllegalAccessException illegalAccessException) {
            throw new IllegalArgumentException("Constructor is not publicly accessible.", illegalAccessException);
        }
        catch (InstantiationException instantiationException) {
            throw new IllegalArgumentException("Constructor is part of an abstract class.", instantiationException);
        }
        catch (InvocationTargetException invocationTargetException) {
            if (invocationTargetException.getCause() instanceof Error) {
                throw (Error)invocationTargetException.getCause();
            }
            throw (RuntimeException)invocationTargetException.getCause();
        }
    }

    public static String inputStreamToString(InputStream inputStream) throws Exception {
        try {
            String string;
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder stringBuilder = new StringBuilder();
            while ((string = bufferedReader.readLine()) != null) {
                stringBuilder.append(string);
                stringBuilder.append('\n');
            }
            bufferedReader.close();
            return stringBuilder.toString();
        }
        catch (Exception exception) {
            throw new Exception("Converting InputStream to String failed!");
        }
    }

    public static byte[] loadDataFromServer(String string) throws Exception {
        try {
            CloseableHttpClient closeableHttpClient = HttpClientBuilder.create().build();
            HttpGet httpGet = new HttpGet(string);
            HttpResponse httpResponse = closeableHttpClient.execute((HttpUriRequest)httpGet);
            if (httpResponse.getStatusLine().getStatusCode() != 200) {
                throw new Exception("HTML status code - " + httpResponse.getStatusLine().getStatusCode());
            }
            return SQUtils.inputStreamToBytes(httpResponse.getEntity().getContent());
        }
        catch (Error error) {
            error.printStackTrace();
            throw new Exception("Load file from server failed! Url:" + string + "\nExc.", error);
        }
        catch (Exception exception) {
            throw new Exception("Load file from server failed! Url:" + string + "\nExc.", exception);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    public static byte[] inputStreamToBytes(InputStream inputStream) throws Exception {
        byte[] byArray = new byte[32768];
        ByteArrayOutputStream byteArrayOutputStream = null;
        try {
            try {
                byteArrayOutputStream = new ByteArrayOutputStream(byArray.length);
                int n = 0;
                while (n != -1) {
                    n = inputStream.read(byArray);
                    if (n <= 0) continue;
                    byteArrayOutputStream.write(byArray, 0, n);
                }
            }
            finally {
                inputStream.close();
            }
        }
        catch (Exception exception) {
            throw new Exception("IO problem. InputStreamToBytes failed. Exc. ", exception);
        }
        return byteArrayOutputStream.toByteArray();
    }

    public static void inputStreamToFile(InputStream inputStream, File file) throws Exception {
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            IOUtils.copy((InputStream)inputStream, (OutputStream)fileOutputStream);
        }
        catch (IOException iOException) {
            throw new Exception("IO problem. InputStreamToFile failed. Exc. ", iOException);
        }
    }

    public static String saveFileAs(byte[] byArray, String string, String string2) throws Exception {
        try {
            boolean bl = true;
            String string3 = string + string2;
            File file = new File(string3);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
                bl = file.createNewFile();
            }
            if (!bl) {
                throw new Exception("Access denied! File " + string3);
            }
            BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(file));
            bufferedOutputStream.write(byArray);
            bufferedOutputStream.flush();
            bufferedOutputStream.close();
            return string3;
        }
        catch (Exception exception) {
            throw new Exception("File save-as failed. Exc: " + exception.getMessage());
        }
    }

    public static String formatDate(long l, String string) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(string);
        return simpleDateFormat.format(new Date(l));
    }

    public static long getTimeFromDate(String string, String string2) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(string2);
        try {
            Date date = simpleDateFormat.parse(string);
            return date.getTime();
        }
        catch (Exception exception) {
            Log.error("Parse error", (Throwable)exception);
            return 0L;
        }
    }

    public static boolean dateFormatValid(String string, String string2) {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(string2);
        try {
            if (string2 != null && string != null && string.length() == string2.length()) {
                Date date = simpleDateFormat.parse(string);
                long l = date.getTime();
                if (l < 0L) {
                    throw new Exception("Negative number.");
                }
                return true;
            }
        }
        catch (Exception exception) {
            // empty catch block
        }
        return false;
    }

    public static Object callMethod(Object object, String string, Object object2) throws Exception {
        Class<?> clazz = Class.forName(object.getClass().getName());
        Method method = clazz.getMethod(string, object2.getClass());
        return method.invoke(object, object2);
    }

    public static Object callMethod(Object object, String string, Object object2, Class clazz) throws Exception {
        Class<?> clazz2 = Class.forName(object.getClass().getName());
        Method method = clazz2.getMethod(string, clazz);
        return method.invoke(object, object2);
    }

    public static Object callMethod(Object object, String string, Object[] objectArray, boolean bl) throws Exception {
        Class<?> clazz = Class.forName(object.getClass().getName());
        Class[] classArray = new Class[objectArray.length];
        for (int i = 0; i < objectArray.length; ++i) {
            classArray[i] = objectArray[i].getClass();
            if (!bl) continue;
            if (classArray[i].getSimpleName().equals(Integer.class.getSimpleName())) {
                classArray[i] = Integer.TYPE;
                continue;
            }
            if (!classArray[i].getSimpleName().equals(Double.class.getSimpleName())) continue;
            classArray[i] = Double.TYPE;
        }
        Method method = clazz.getMethod(string, classArray);
        return method.invoke(object, objectArray);
    }

    public static Object callMethod(Object object, String string, Object[] objectArray, Class[] classArray) throws Exception {
        Class<?> clazz = Class.forName(object.getClass().getName());
        Method method = clazz.getMethod(string, classArray);
        return method.invoke(object, objectArray);
    }

    public static String removeUTF8BOM(String string) {
        if (string.startsWith("\ufeff")) {
            string = string.substring(1);
        }
        return string;
    }

    public static ArrayList<String> listSubdirectories(String string) {
        ArrayList<String> arrayList = new ArrayList<String>();
        SQUtils.listf(string, nullStringCorrection, arrayList);
        return arrayList;
    }

    private static void listf(String string, String string2, ArrayList<String> arrayList) {
        File[] fileArray;
        File file = new File(string);
        if (!string2.equals(nullStringCorrection)) {
            string2 = string2 + "/";
        }
        if ((fileArray = file.listFiles()) != null) {
            for (File file2 : fileArray) {
                if (!file2.isDirectory()) continue;
                String string3 = string2 + file2.getName();
                arrayList.add(string3);
                SQUtils.listf(file2.getAbsolutePath(), string3, arrayList);
            }
        }
    }

    public static String encodeXmlKey(String string) {
        string = string.replace("[", "_LQ1_");
        string = string.replace("]", "_RQ1_");
        string = string.replace("|", "_L1_");
        string = string.replace(":", "_DD_");
        return string.intern();
    }

    public static String decodeXmlKey(String string) {
        string = string.replace("_LQ1_", "[");
        string = string.replace("_RQ1_", "]");
        string = string.replace("_L1_", "|");
        string = string.replace("_DD_", ":");
        return string.intern();
    }

    public static String d2(Number number) {
        if (number == null) {
            return "0.00";
        }
        return SQUtils.d2(number.doubleValue());
    }

    public static String dCommon(double d, long l) {
        double d2;
        StringBuilder stringBuilder = new StringBuilder();
        int n = (int)d;
        if (d < 0.0) {
            n = -n;
            d2 = -d - (double)n;
        } else {
            d2 = d - (double)n;
        }
        long l2 = Math.round(d2 * (double)l);
        if (l2 == l) {
            ++n;
            l2 = 0L;
        }
        if (d < 0.0) {
            stringBuilder.append('-');
        }
        stringBuilder.append(n);
        if (l > 1L) {
            stringBuilder.append('.');
            for (long i = 10L; i < l; i *= 10L) {
                if (l2 >= i) continue;
                stringBuilder.append("0");
            }
            stringBuilder.append(l2);
        }
        return stringBuilder.toString();
    }

    public static String d2(double d) {
        return SQUtils.dCommon(d, 100L);
    }

    public static String d2String(double d, int n) {
        switch (n) {
            case 0: {
                return SQUtils.dCommon(d, 1L);
            }
            case 1: {
                return SQUtils.dCommon(d, 10L);
            }
            case 2: {
                return SQUtils.dCommon(d, 100L);
            }
            case 3: {
                return SQUtils.dCommon(d, 1000L);
            }
            case 4: {
                return SQUtils.dCommon(d, 10000L);
            }
            case 5: {
                return SQUtils.dCommon(d, 100000L);
            }
            case 6: {
                return SQUtils.dCommon(d, 1000000L);
            }
            case 7: {
                return SQUtils.dCommon(d, 10000000L);
            }
            case 10: {
                return d10Form.get().format(d);
            }
        }
        return SQUtils.dCommon(d, 100L);
    }

    public static String doubleToString(double d) {
        if (d == 1.0) {
            return "1";
        }
        if (d == 0.0) {
            return "0";
        }
        String string = decimalFormater.get().format(d);
        if (string.endsWith(".0")) {
            string = string.substring(0, string.length() - 2);
        }
        return string;
    }

    private static String effectiveFormat(double d) {
        StringBuilder stringBuilder = new StringBuilder();
        if (d < 0.0) {
            stringBuilder.append('-');
            d = -d;
        }
        if (d * 1000000.0 + 0.5 > 9.223372036854776E18) {
            throw new IllegalArgumentException("number too large");
        }
        long l = (long)(d * 1000000.0 + 0.5);
        long l2 = 100000000L;
        int n = 9;
        long l3 = l / 10L;
        while (l2 <= l3) {
            l2 *= 10L;
            ++n;
        }
        while (n > 0) {
            if (n == 8) {
                stringBuilder.append('.');
            }
            long l4 = l / l2 % 10L;
            l2 /= 10L;
            stringBuilder.append((char)(48L + l4));
            --n;
        }
        return stringBuilder.toString();
    }

    public static String getStackTrace() {
        StringWriter stringWriter = new StringWriter();
        new Throwable().printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    public static String getStackTrace(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

    public static String correctResultKeyToDirname(String string) {
        string = string.replace("/", "_LOM_");
        string = string.replace("*", "_STAR_");
        return string;
    }

    public static String correctDirnameToResultKey(String string) {
        string = string.replace("_LOM_", "/");
        string = string.replace("_STAR_", "*");
        return string;
    }

    public static double fixPrice(double d, double d2) {
        return SQUtils.fixPrice(d, d2, 5);
    }

    public static double fixPrice(double d, double d2, int n) {
        if (d == Math.pow(10.0, -n)) {
            return SQUtils.round(d2, n);
        }
        d2 = SQUtils.round(d2, n);
        d2 = (double)Math.round(d2 / d) * d;
        d2 = SQUtils.round(d2, n);
        return d2;
    }

    public static double fixPriceSpecial(int n, double d, double d2, int n2) {
        if (d == Math.pow(10.0, -n2)) {
            return SQUtils.round(d2, n2);
        }
        d2 = n == BuyLimit || n == BuyToCoverLimit || n == SellToCoverStop || n == SellStop ? Math.floor(d2 / d) * d : (n == SellToCoverLimit || n == SellLimit || n == BuyToCoverStop || n == BuyStop ? Math.ceil(d2 / d) * d : (double)Math.round(d2 / d) * d);
        d2 = SQUtils.round(d2, n2);
        return d2;
    }

    public static String format(double d, int n) {
        StringBuilder stringBuilder = new StringBuilder();
        if (d < 0.0) {
            stringBuilder.append('-');
            d = -d;
        }
        int n2 = POW10[n];
        long l = (long)(d * (double)n2 + 0.5);
        stringBuilder.append(l / (long)n2).append('.');
        long l2 = l % (long)n2;
        for (int i = n - 1; i > 0 && l2 < (long)POW10[i]; --i) {
            stringBuilder.append('0');
        }
        stringBuilder.append(l2);
        return stringBuilder.toString();
    }

    public static int compare(double d, double d2, int n) {
        double d3 = 1.0 / Math.pow(10.0, (double)n + 2.0);
        double d4 = Math.abs(d - d2);
        if (d4 < d3) {
            return 0;
        }
        return d < d2 ? -1 : 1;
    }

    public static int getDecimalPlaces(double d) {
        int n = 0;
        String string = instrumentDF.get().format(d);
        if (string.contains(".")) {
            String[] stringArray = string.split("\\.");
            n = stringArray[1].length();
        } else if (string.contains(",")) {
            String[] stringArray = string.split(",");
            n = stringArray[1].length();
        }
        if (n > 7) {
            n = 7;
        }
        return n;
    }

    public static String insertUppercaseSpaces(String string) {
        return StringUtils.join((Object[])StringUtils.splitByCharacterTypeCamelCase((String)string), (String)" ");
    }

    public static void fileToArrayList(ArrayList<String> arrayList, String string, String string2) throws Exception {
        String[] stringArray;
        arrayList.clear();
        String string3 = SQUtils.fileToString(new File(string));
        for (String string4 : stringArray = string3.split(string2)) {
            if (arrayList.contains(string4)) continue;
            arrayList.add(string4);
        }
    }

    public static void arrayListToFile(ArrayList<String> arrayList, String string, String string2) throws Exception {
        String string3 = nullStringCorrection;
        for (String string4 : arrayList) {
            string3 = string3 + string4 + string2;
        }
        string3 = string3.substring(0, string3.length() - string2.length());
        try {
            SQUtils.stringToFile(new File(string), string3);
        }
        catch (Exception exception) {
            throw new Exception("Saving to file failed");
        }
    }

    public static int countFilesInDirectory(String string) {
        File file = new File(string);
        File[] fileArray = file.listFiles();
        if (fileArray == null) {
            return 0;
        }
        return fileArray.length;
    }

    public static File createFile(String string) throws Exception {
        File file = new File(string);
        if (!file.exists()) {
            File file2 = file.getParentFile();
            if (!file2.exists() && !file2.mkdirs()) {
                throw new Exception("Cannot create parent directories for file '" + string + "'");
            }
            if (!file.createNewFile()) {
                throw new Exception("Cannot create file '" + string + "'");
            }
        }
        return file;
    }

    public static String correctNullString(String string) {
        if (string == null) {
            return nullStringCorrection;
        }
        return string;
    }

    public static int computeFileHash(String string, int n) throws FileNotFoundException, IOException {
        int n2 = 0;
        int n3 = 0;
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(string)));){
            String string2;
            while ((string2 = bufferedReader.readLine()) != null) {
                if (++n3 <= n) continue;
                n2 += string2.hashCode();
            }
        }
        return n2;
    }

    public static void ensureDirExists(String string) {
        File file = new File(string);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static String removeXmlns(String string) {
        return string.replace("xmlns=\"\"", nullStringCorrection);
    }

    public static String stackTraceToString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        throwable.printStackTrace(printWriter);
        return stringWriter.toString();
    }

    public static String extractNumber(String string) {
        return string.replaceAll("[^\\d-.]", nullStringCorrection);
    }

    public static String removeHtmlTags(String string) {
        return string.replaceAll("<[^>]*>", nullStringCorrection);
    }

    public static String shortenWord(String string, int n) {
        if (string.length() <= n) {
            return string;
        }
        String string2 = string.substring(0, 1);
        for (int i = 1; i < string.length(); ++i) {
            char c = string.charAt(i);
            int n2 = n - string2.length();
            if (string2.length() == n) break;
            if (string.length() - i == n2) {
                return string2 + string.substring(i);
            }
            if (SQUtils.isVowel(c)) continue;
            string2 = string2 + c;
        }
        return string2;
    }

    public static boolean isVowel(char c) {
        return "AEIOUaeiou".indexOf(c) != -1;
    }

    public static String getClasses(Class<?> clazz) {
        Class<?> clazz2;
        StringBuilder stringBuilder = new StringBuilder(clazz.getSimpleName());
        while ((clazz2 = clazz.getSuperclass()) != null) {
            stringBuilder.append(".");
            stringBuilder.append(clazz2.getSimpleName());
            clazz = clazz2;
        }
        return stringBuilder.toString();
    }

    public static ArrayList<String> addToErrors(ArrayList<String> arrayList, String string) {
        if (arrayList == null) {
            arrayList = new ArrayList();
        }
        arrayList.add(string);
        return arrayList;
    }

    public static boolean deleteDirectory(String string) {
        File[] fileArray;
        File file = new File(string);
        if (file.exists() && null != (fileArray = file.listFiles())) {
            for (File file2 : fileArray) {
                if (file2.isDirectory()) {
                    SQUtils.deleteDirectory(file2.getAbsolutePath());
                    continue;
                }
                file2.delete();
            }
        }
        return file.delete();
    }

    public static boolean isInStringArray(String string, ArrayList<String> arrayList) {
        for (int i = 0; i < arrayList.size(); ++i) {
            if (!arrayList.get(i).equalsIgnoreCase(string)) continue;
            return true;
        }
        return false;
    }

    public static boolean isInByteArray(byte by, byte[] byArray) {
        for (int i = 0; i < byArray.length; ++i) {
            if (byArray[i] != by) continue;
            return true;
        }
        return false;
    }

    public static String fixRenamedActionParams(String string) {
        String[] stringArray = new String[]{"#MoveSL2BE#", "#SL2BEAddPips#", "#ProfitTarget#", "#StopLoss#", "#TrailingStop#", "#TrailingActivation#", "#ExitAfterBars#"};
        String[] stringArray2 = new String[]{"#MoveSL2BE.MoveSL2BE#", "#MoveSL2BE.SL2BEAddPips#", "#ProfitTarget.ProfitTarget#", "#StopLoss.StopLoss#", "#TrailingStop.TrailingStop#", "#TrailingStop.TrailingActivation#", "#ExitAfterBars.ExitAfterBars#"};
        string = StringUtils.replaceEach((String)string, (String[])stringArray, (String[])stringArray2);
        return string;
    }

    public static String renameActionParams(String string) {
        String[] stringArray = new String[]{"#MoveSL2BE.MoveSL2BE#", "#MoveSL2BE.SL2BEAddPips#", "#ProfitTarget.ProfitTarget#", "#StopLoss.StopLoss#", "#TrailingStop.TrailingStop#", "#TrailingStop.TrailingActivation#", "#ExitAfterBars.ExitAfterBars#"};
        String[] stringArray2 = new String[]{"#MoveSL2BE#", "#SL2BEAddPips#", "#ProfitTarget#", "#StopLoss#", "#TrailingStop#", "#TrailingActivation#", "#ExitAfterBars#"};
        string = StringUtils.replaceEach((String)string, (String[])stringArray, (String[])stringArray2);
        return string;
    }

    public static String fixHeikenAshi(String string) {
        string = string.replace("\"HeikenAshi\"", "\"HeikenAshiClose\"");
        return string;
    }

    public static void fixExitmethodAttributes(Element element) {
        List list = element.getChildren();
        if (list != null) {
            for (int i = 0; i < list.size(); ++i) {
                Object object;
                Element element2 = (Element)list.get(i);
                if (element2.getName().equals("CustomIndicators")) continue;
                if (element2.getName().equals("Param")) {
                    object = element2.getAttributeValue("key");
                    if (((String)object).equals("#MoveSL2BE.MoveSL2BE#")) {
                        element2.setAttribute("exitMethod", "true");
                        element2.setAttribute("exitMethodType", "SL");
                    } else if (((String)object).equals("#MoveSL2BE.SL2BEAddPips#")) {
                        element2.setAttribute("exitMethod", "true");
                        element2.setAttribute("exitMethodType", "SL");
                        element2.setAttribute("dependentOn", "MoveSL2BE.MoveSL2BE");
                    } else if (((String)object).equals("#ProfitTarget.ProfitTarget#")) {
                        element2.setAttribute("exitMethod", "true");
                        element2.setAttribute("exitMethodType", "PT");
                    } else if (((String)object).equals("#StopLoss.StopLoss#")) {
                        element2.setAttribute("exitMethod", "true");
                        element2.setAttribute("exitMethodType", "SL");
                    } else if (((String)object).equals("#TrailingStop.TrailingStop#")) {
                        element2.setAttribute("exitMethod", "true");
                        element2.setAttribute("exitMethodType", "SL");
                    } else if (((String)object).equals("#TrailingStop.TrailingActivation#")) {
                        element2.setAttribute("exitMethod", "true");
                        element2.setAttribute("exitMethodType", "SL");
                        element2.setAttribute("dependentOn", "TrailingStop.TrailingStop");
                    } else if (((String)object).equals("#ExitAfterBars.ExitAfterBars#")) {
                        element2.setAttribute("exitMethod", "true");
                    }
                }
                if ((object = element2.getChildren()) == null) continue;
                SQUtils.fixExitmethodAttributes(element2);
            }
        }
    }

    public static ArrayList<String[]> loadLines(String string, String string2) throws Exception {
        ArrayList<String[]> arrayList = new ArrayList<String[]>();
        try {
            String string3;
            BufferedReader bufferedReader = new BufferedReader(new FileReader(new File(string)));
            while ((string3 = bufferedReader.readLine()) != null && !string3.equals(nullStringCorrection)) {
                String[] stringArray = string3.split(string2);
                arrayList.add(stringArray);
            }
            bufferedReader.close();
            return arrayList;
        }
        catch (Exception exception) {
            throw new Exception("Exception in file '" + string + "', :" + exception.getMessage(), exception);
        }
    }

    public static double pctToDouble(int n) {
        return (double)n / 100.0;
    }

    public static String httpUploadFile(String string, String string2, Map<String, String> map, String string3, String string4, byte[] byArray) throws Exception {
        HttpURLConnection httpURLConnection = null;
        FilterOutputStream filterOutputStream = null;
        InputStream inputStream = null;
        BufferedReader bufferedReader = null;
        try {
            URL uRL = new URL(string);
            String string5 = "\r\n";
            String string6 = "--";
            String string7 = "*****";
            httpURLConnection = (HttpURLConnection)uRL.openConnection();
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setDoInput(true);
            httpURLConnection.setRequestMethod("POST");
            httpURLConnection.setRequestProperty("Connection", "Keep-Alive");
            httpURLConnection.setRequestProperty("Cache-Control", "no-cache");
            httpURLConnection.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + string7);
            if (map != null) {
                for (String charSequence2 : map.keySet()) {
                    httpURLConnection.setRequestProperty(charSequence2, map.get(charSequence2));
                }
            }
            filterOutputStream = new DataOutputStream(httpURLConnection.getOutputStream());
            ((DataOutputStream)filterOutputStream).writeBytes(string5);
            ((DataOutputStream)filterOutputStream).writeBytes(string5);
            ((DataOutputStream)filterOutputStream).writeBytes(string6 + string7 + string5);
            ((DataOutputStream)filterOutputStream).writeBytes("Content-Disposition: form-data; name=\"" + string3 + "\"" + string5);
            ((DataOutputStream)filterOutputStream).writeBytes(string5);
            filterOutputStream.write(byArray);
            ((DataOutputStream)filterOutputStream).writeBytes(string5);
            ((DataOutputStream)filterOutputStream).writeBytes(string6 + string7 + string5);
            ((DataOutputStream)filterOutputStream).flush();
            filterOutputStream.close();
            inputStream = new BufferedInputStream(httpURLConnection.getInputStream());
            bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            Object object = nullStringCorrection;
            StringBuilder stringBuilder = new StringBuilder();
            while ((object = bufferedReader.readLine()) != null) {
                stringBuilder.append((String)object).append("\n");
            }
            bufferedReader.close();
            String string8 = stringBuilder.toString();
            inputStream.close();
            httpURLConnection.disconnect();
            return string8;
        }
        catch (Exception exception) {
            Log.error("Exc.", (Throwable)exception);
            if (filterOutputStream != null) {
                filterOutputStream.close();
            }
            if (bufferedReader != null) {
                bufferedReader.close();
            }
            if (inputStream != null) {
                inputStream.close();
            }
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            throw new Exception("Request error: POST, " + string);
        }
    }

    public static String httpPost(String string, String string2, Map<String, String> map) throws Exception {
        try {
            String string3;
            URL uRL = new URL(string);
            URLConnection uRLConnection = uRL.openConnection();
            uRLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            uRLConnection.setRequestProperty("Content-Language", "en-US");
            if (map != null) {
                for (String object2 : map.keySet()) {
                    uRLConnection.setRequestProperty(object2, map.get(object2));
                }
            }
            uRLConnection.setDoOutput(true);
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(uRLConnection.getOutputStream());
            outputStreamWriter.write(string2);
            outputStreamWriter.flush();
            outputStreamWriter.close();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(uRLConnection.getInputStream(), StandardCharsets.UTF_8));
            String string4 = nullStringCorrection;
            while ((string3 = bufferedReader.readLine()) != null) {
                string4 = string4 + string3 + "\n";
            }
            return string4;
        }
        catch (Exception exception) {
            Log.error("Exc.", (Throwable)exception);
            throw new Exception("Request error: POST, " + string);
        }
    }

    public static String httpsPost(String string2, List<NameValuePair> list, boolean bl) throws Exception {
        try {
            SSLContext sSLContext = new SSLContextBuilder().loadTrustMaterial(null, (x509CertificateArray, string) -> true).build();
            CloseableHttpClient closeableHttpClient = HttpClients.custom().setSslcontext(sSLContext).setHostnameVerifier((X509HostnameVerifier)new AllowAllHostnameVerifier()).build();
            HttpPost httpPost = new HttpPost(string2);
            httpPost.setEntity((HttpEntity)new UrlEncodedFormEntity(list));
            CloseableHttpResponse closeableHttpResponse = closeableHttpClient.execute((HttpUriRequest)httpPost);
            if (closeableHttpResponse.getStatusLine().getStatusCode() != 200) {
                if (bl) {
                    throw new Exception("HTML status code - " + closeableHttpResponse.getStatusLine().getStatusCode());
                }
                Log.error("Request {} finished with code is: {}", (Object)string2, (Object)closeableHttpResponse.getStatusLine().getStatusCode());
            }
            HttpEntity httpEntity = closeableHttpResponse.getEntity();
            return EntityUtils.toString((HttpEntity)httpEntity, (String)"UTF-8");
        }
        catch (Exception exception) {
            Log.error("Exc.", (Throwable)exception);
            throw new Exception("Request error: POST, " + string2);
        }
        catch (Error error) {
            Log.error("Exc.", (Throwable)error);
            throw new Exception("Request error: POST, " + string2);
        }
    }

    public static String httpGet(String string) throws Exception {
        return SQUtils.httpGet(string, -1);
    }

    public static String httpGet(String string, int n) throws Exception {
        HttpURLConnection httpURLConnection = null;
        try {
            String string2;
            URL uRL = new URL(string);
            httpURLConnection = (HttpURLConnection)uRL.openConnection();
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setRequestProperty("Content-Type", "text/html");
            httpURLConnection.setRequestProperty("Content-Language", "en-US");
            httpURLConnection.setUseCaches(false);
            httpURLConnection.setDoOutput(true);
            if (n > 0) {
                httpURLConnection.setConnectTimeout(n);
            }
            InputStream inputStream = httpURLConnection.getInputStream();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            StringBuilder stringBuilder = new StringBuilder();
            while ((string2 = bufferedReader.readLine()) != null) {
                stringBuilder.append(string2);
                stringBuilder.append('\r');
            }
            bufferedReader.close();
            String string3 = stringBuilder.toString();
            return string3;
        }
        catch (Exception exception) {
            throw new Exception("Request error: GET, " + string + " - " + exception.getMessage());
        }
        finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
    }

    public static boolean doublesAreEqual(double d, double d2) {
        return Math.abs(d - d2) < 1.0E-8;
    }

    public static String convertIntTimetoHHMM(int n) {
        String string;
        int n2 = n / 100;
        int n3 = n % 100;
        String string2 = Integer.toString(n2);
        if (string2.length() == 1) {
            string2 = "0" + string2;
        }
        if ((string = Integer.toString(n3)).length() == 1) {
            string = "0" + string;
        }
        return String.format("%s:%s", string2, string);
    }

    public static String formatBytesToHumanFormat(long l) {
        if (l < 0x100000L) {
            return String.format("%.2f KB", (double)l / 1024.0);
        }
        if (l < 0x40000000L) {
            return String.format("%.0f MB", (double)l / 1048576.0);
        }
        if (l < 1048576000000L) {
            return String.format("%.2f GB", (double)l / 1.073741824E9);
        }
        return String.format("%.2f TB", (double)l / 1.048576E12);
    }

    public static String formatDuration(long l) {
        if (l < 1000L) {
            return (l + " ms. ").trim();
        }
        long l2 = l / 1000L;
        long l3 = l2 / 86400L;
        long l4 = (l2 %= 86400L) / 3600L;
        long l5 = (l2 %= 3600L) / 60L;
        l2 %= 60L;
        StringBuilder stringBuilder = new StringBuilder();
        if (l3 > 0L) {
            stringBuilder.append(l3);
            stringBuilder.append(l3 > 1L ? " days " : " day ");
        }
        if (l4 > 0L) {
            stringBuilder.append(l4);
            stringBuilder.append(l4 > 1L ? " hrs. " : " hr. ");
        }
        if (l3 == 0L && l5 > 0L) {
            stringBuilder.append(l5);
            stringBuilder.append(" min. ");
        }
        if (l3 == 0L && l4 == 0L && l2 > 0L) {
            stringBuilder.append(l2);
            stringBuilder.append(" s. ");
        }
        return stringBuilder.toString().trim();
    }

    public static int levenshteinDistance(CharSequence charSequence, CharSequence charSequence2) {
        return StringUtils.getLevenshteinDistance((CharSequence)charSequence, (CharSequence)charSequence2);
    }

    public static HashMap<String, Object> cloneHashMap(HashMap<String, Object> hashMap) {
        HashMap<String, Object> hashMap2 = new HashMap<String, Object>();
        for (String string : hashMap.keySet()) {
            Object object = hashMap.get(string);
            if (object == null) {
                hashMap2.put(string, null);
                continue;
            }
            Object object2 = SQUtils.cloneObject(object);
            if (object2 == null) {
                Log.info("Object with key {} and class {} cannot be cloned!", (Object)string, (Object)object.getClass().getName());
                continue;
            }
            hashMap2.put(string, object2);
        }
        return hashMap2;
    }

    public static HashMap<Integer, Object> cloneValuesMapOld(HashMap<Integer, Object> hashMap) {
        HashMap<Integer, Object> hashMap2 = new HashMap<Integer, Object>();
        for (int n : hashMap.keySet()) {
            Object object = hashMap.get(n);
            if (object == null) {
                hashMap2.put(n, null);
                continue;
            }
            Object object2 = SQUtils.cloneObject(object);
            if (object2 == null) {
                Log.info("Object with key {} and class {} cannot be cloned!", (Object)n, (Object)object.getClass().getName());
                continue;
            }
            hashMap2.put(n, object2);
        }
        return hashMap2;
    }

    public static Int2ObjectOpenHashMap<Object> cloneValuesMap(Int2ObjectOpenHashMap<Object> int2ObjectOpenHashMap) {
        Int2ObjectOpenHashMap int2ObjectOpenHashMap2 = new Int2ObjectOpenHashMap();
        IntIterator intIterator = int2ObjectOpenHashMap.keySet().iterator();
        while (intIterator.hasNext()) {
            int n = (Integer)intIterator.next();
            Object object = int2ObjectOpenHashMap.get(n);
            if (object == null) {
                int2ObjectOpenHashMap2.put(n, null);
                continue;
            }
            Object object2 = SQUtils.cloneObject(object);
            if (object2 == null) {
                Log.info("Object with key {} and class {} cannot be cloned!", (Object)n, (Object)object.getClass().getName());
                continue;
            }
            int2ObjectOpenHashMap2.put(n, object2);
        }
        return int2ObjectOpenHashMap2;
    }

    public static ArrayList cloneArrayList(ArrayList arrayList) {
        ArrayList<Object> arrayList2 = new ArrayList<Object>();
        for (Object e : arrayList) {
            if (e == null) {
                arrayList2.add(null);
                continue;
            }
            Object object = SQUtils.cloneObject(e);
            if (object == null) {
                Log.info("Object with class {} cannot be cloned!", (Object)e.getClass().getName());
                continue;
            }
            arrayList2.add(object);
        }
        return arrayList2;
    }

    public static Object cloneObject(Object object) {
        if (object == null) {
            return null;
        }
        if (object instanceof String || object instanceof Byte || object instanceof Short || object instanceof Integer || object instanceof Long || object instanceof Float || object instanceof Double || object instanceof Boolean || object instanceof Character) {
            return object;
        }
        if (object instanceof Element) {
            return ((Element)object).clone();
        }
        if (object instanceof ISQCloneable) {
            try {
                return ((ISQCloneable)object).getClone();
            }
            catch (Exception exception) {
                Log.error("Exception cloning object '{}'", (Object)object.getClass().getName(), (Object)exception);
                return null;
            }
        }
        if (object instanceof ArrayList) {
            return SQUtils.cloneArrayList((ArrayList)object);
        }
        return object;
    }

    public static String optimizeLastSettings(String string) {
        int n = StringUtils.indexOf((CharSequence)string, (CharSequence)"<Blocks ");
        if (n == -1) {
            return string;
        }
        int n2 = StringUtils.indexOf((CharSequence)string, (CharSequence)"</Blocks>");
        if (n2 == -1) {
            return string;
        }
        StringBuilder stringBuilder = new StringBuilder(StringUtils.substring((String)string, (int)0, (int)n));
        stringBuilder.append(StringUtils.substring((String)string, (int)(n2 + 9)));
        return StringUtils.replaceAll((String)stringBuilder.toString(), (String)"\n\\s+<", (String)"<");
    }

    public static void waitForKey(String string) {
        System.out.println("==================================");
        if (string != null) {
            System.out.println(string);
        }
        System.out.println("Press Enter to continue");
        System.out.println("==================================");
        try {
            System.in.read();
        }
        catch (Exception exception) {
            // empty catch block
        }
        System.out.println("Continuing...");
    }

    public static JSONArray toJSONArray(String string) {
        String[] stringArray = string.split(",");
        JSONArray jSONArray = new JSONArray();
        for (int i = 0; i < stringArray.length; ++i) {
            jSONArray.put((Object)stringArray[i]);
        }
        return jSONArray;
    }

    public static void writeUTF(ObjectOutput objectOutput, String string) throws IOException {
        if (string == null) {
            objectOutput.writeByte(0);
        } else {
            objectOutput.writeByte(1);
            objectOutput.writeUTF(string);
        }
    }

    public static String readUTFIntern(ObjectInput objectInput) throws IOException {
        String string = SQUtils.readUTF(objectInput);
        return string != null ? string.intern() : null;
    }

    public static String readUTF(ObjectInput objectInput) throws IOException {
        byte by = objectInput.readByte();
        if (by == 0) {
            return null;
        }
        return objectInput.readUTF();
    }

    public static int betterHashCode(String string) {
        if (string == null) {
            return 0;
        }
        if (string.length() == 0) {
            return 0;
        }
        int n = 17;
        for (char c : string.toCharArray()) {
            n = 109 * n + c;
        }
        return n;
    }

    public static long longHashCode(String string) {
        return hashFunction.hashUnencodedChars((CharSequence)string).asLong();
    }

    public static int betterHashNumbers(int ... nArray) {
        Hasher hasher = hashFunction.newHasher();
        for (int i = 0; i < nArray.length; ++i) {
            hasher.putInt(nArray[i]);
        }
        return hasher.hash().asInt();
    }

    public static int intsHash(int ... nArray) {
        int n = 37;
        for (int i = 0; i < nArray.length; ++i) {
            n = 31 * n + nArray[i];
        }
        return n;
    }

    public static int doublesHash(double ... dArray) {
        int n = 37;
        for (int i = 0; i < dArray.length; ++i) {
            n = 31 * n + Double.hashCode(dArray[i]);
        }
        return n;
    }

    public static int booleansHash(boolean ... blArray) {
        int n = 37;
        for (int i = 0; i < blArray.length; ++i) {
            n = 31 * n + Boolean.hashCode(blArray[i]);
        }
        return n;
    }

    public static String xmlPrettyFormat(String string) throws Exception {
        return new XMLOutputter(Format.getPrettyFormat()).outputString(new SAXBuilder().build((Reader)new StringReader(string)));
    }

    public static int fixAllowedRange(int n, int n2, int n3, int n4) {
        if (n < n2 || n > n3) {
            return n4;
        }
        return n;
    }

    public static String correctClassName(String string) {
        string = string.substring(0, 1).toUpperCase() + string.substring(1);
        if ((string = string.replaceAll("[^A-Za-z0-9]", "_")).charAt(string.length() - 1) == '_') {
            string = string.substring(0, string.length() - 1);
        }
        while (string.contains("__")) {
            string = string.replaceAll("__", "_");
        }
        return string;
    }

    public static long getFileCrc(File file) throws Exception {
        try (InputStream inputStream = null;){
            int n;
            inputStream = new BufferedInputStream(new FileInputStream(file));
            CRC32 cRC32 = new CRC32();
            while ((n = inputStream.read()) != -1) {
                cRC32.update(n);
            }
            long l = cRC32.getValue();
            return l;
        }
    }

    public static boolean checkEngine(String string, String string2) {
        return !string2.contains('-' + string) && (string2.contains(string) || string2.contains("*"));
    }
}

