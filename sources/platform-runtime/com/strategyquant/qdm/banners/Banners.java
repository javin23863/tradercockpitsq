/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.qdm.banners;

import com.strategyquant.lib.SQUtils;
import com.strategyquant.lib.XMLUtil;
import com.strategyquant.lib.app.MainApp;
import com.strategyquant.qdm.QDMDb;
import com.strategyquant.qdm.QDMStats;
import com.strategyquant.qdm.banners.Banner;
import com.strategyquant.qdm.banners.BannerStats;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.List;
import org.jdom2.Element;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Banners
extends QDMStats {
    public static final Logger Log = LoggerFactory.getLogger(Banners.class);
    private Int2ObjectOpenHashMap<Banner> availableBanners = new Int2ObjectOpenHashMap();
    private Int2ObjectOpenHashMap<BannerStats> bannersStats = new Int2ObjectOpenHashMap();
    public int topBannersInMinutes = 10;
    public int popupBannersInMinutes = 10;

    public Banners(QDMDb qDMDb) {
        super(qDMDb);
    }

    public void loadAvailable() {
        try {
            File file = new File(MainApp.getDataPath() + "/internal/web/QDM/data/config.xml");
            Element element = XMLUtil.fileToXmlElement(file);
            Element element2 = XMLUtil.getChildElem(element, "settings");
            this.topBannersInMinutes = XMLUtil.getInt(element2, "topBannersInMinutes", 10);
            this.popupBannersInMinutes = XMLUtil.getInt(element2, "popupBannersInMinutes", 10);
            Element element3 = XMLUtil.getChildElem(element, "banners");
            List list = element3.getChildren("banner");
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
            for (int i = 0; i < list.size(); ++i) {
                Element element4 = (Element)list.get(i);
                long l = simpleDateFormat.parse(XMLUtil.getNodeValue(element4, "dateActiveFrom")).getTime();
                long l2 = simpleDateFormat.parse(XMLUtil.getNodeValue(element4, "dateActiveTo")).getTime();
                long l3 = System.currentTimeMillis();
                if (l > l3 || l2 < l3) continue;
                Banner banner = new Banner();
                banner.id = XMLUtil.getNodeIntValue(element4, "id");
                banner.name = XMLUtil.getNodeValue(element4, "name");
                banner.image = XMLUtil.getNodeValue(element4, "image");
                banner.type = XMLUtil.getNodeValue(element4, "type");
                banner.url = XMLUtil.getNodeValue(element4, "url");
                banner.weight = XMLUtil.getNodeIntValue(element4, "weight");
                this.availableBanners.put(banner.id, (Object)banner);
            }
        }
        catch (Exception exception) {
            Log.error("Error while loading banners. Exc.", (Throwable)exception);
        }
    }

    public Banner getNext(String string) {
        Banner banner = null;
        double d = Math.random() * this.getTotalWeight(string);
        double d2 = 0.0;
        IntSet intSet = this.availableBanners.keySet();
        IntIterator intIterator = intSet.iterator();
        while (intIterator.hasNext()) {
            int n = (Integer)intIterator.next();
            if (!((Banner)this.availableBanners.get((int)n)).type.equals(string)) continue;
            banner = (Banner)this.availableBanners.get(n);
            if (!(d < (d2 += (double)banner.weight))) continue;
            break;
        }
        if (banner == null) {
            return null;
        }
        ++this.getBannerStats((int)banner.id).views;
        return banner;
    }

    private BannerStats getBannerStats(int n) {
        if (this.bannersStats.containsKey(n)) {
            return (BannerStats)this.bannersStats.get(n);
        }
        BannerStats bannerStats = new BannerStats();
        this.bannersStats.put(n, (Object)bannerStats);
        return bannerStats;
    }

    private double getTotalWeight(String string) {
        double d = 0.0;
        IntSet intSet = this.availableBanners.keySet();
        IntIterator intIterator = intSet.iterator();
        while (intIterator.hasNext()) {
            int n = (Integer)intIterator.next();
            if (!((Banner)this.availableBanners.get((int)n)).type.equals(string)) continue;
            d += (double)((Banner)this.availableBanners.get((int)n)).weight;
        }
        return d;
    }

    public void click(int n) throws Exception {
        Banner banner = this.getBannerById(n);
        if (banner == null) {
            Log.error("Banner with id " + n + " not found.");
            return;
        }
        ++this.getBannerStats((int)banner.id).clicks;
        SQUtils.openUrlInDefaultWebBrowser(banner.url);
    }

    public Banner getBannerById(int n) {
        return (Banner)this.availableBanners.get(n);
    }

    @Override
    public void init() throws Exception {
        String string = "CREATE TABLE IF NOT EXISTS banner_stats (`id_banner` int(12) NOT NULL,`views` int(11) NOT NULL,`clicks` int(11) NOT NULL)";
        this.db.sqlCommand(string);
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void saveStats() {
        this.cleanDb();
        Connection connection = null;
        try {
            connection = this.db.getConnection();
            IntSet intSet = this.bannersStats.keySet();
            IntIterator intIterator = intSet.iterator();
            while (intIterator.hasNext()) {
                int n = (Integer)intIterator.next();
                BannerStats bannerStats = (BannerStats)this.bannersStats.get(n);
                String string = "INSERT INTO banner_stats (id_banner, views, clicks) VALUES (" + n + ", " + bannerStats.views + ", " + bannerStats.clicks + ")";
                this.db.sqlCommand(connection, string);
            }
        }
        catch (Exception exception) {
            Log.error("DB Exception", (Throwable)exception);
        }
        finally {
            this.db.close(connection);
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    @Override
    public void loadStats() {
        block6: {
            Connection connection = null;
            Statement statement = null;
            ResultSet resultSet = null;
            try {
                connection = this.db.getConnection();
                String string = "SELECT * FROM banner_stats";
                statement = connection.createStatement();
                resultSet = statement.executeQuery(string);
                while (resultSet.next()) {
                    BannerStats bannerStats = new BannerStats();
                    bannerStats.views = resultSet.getInt("views");
                    bannerStats.clicks = resultSet.getInt("clicks");
                    int n = resultSet.getInt("id_banner");
                    this.bannersStats.put(n, (Object)bannerStats);
                }
                this.db.close(resultSet);
            }
            catch (Exception exception) {
                Log.error("DB Exception", (Throwable)exception);
                break block6;
            }
            finally {
                this.db.close(resultSet);
                this.db.close(statement);
                this.db.close(connection);
            }
            this.db.close(statement);
            this.db.close(connection);
        }
    }

    @Override
    public void resetStats() {
        this.bannersStats.clear();
        this.cleanDb();
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void cleanDb() {
        Connection connection = null;
        Statement statement = null;
        ResultSet resultSet = null;
        try {
            connection = this.db.getConnection();
            String string = "DELETE FROM banner_stats;";
            statement = connection.createStatement();
            statement.execute(string);
        }
        catch (Exception exception) {
            Log.error("DB Exception", (Throwable)exception);
        }
        finally {
            this.db.close(resultSet);
            this.db.close(statement);
            this.db.close(connection);
        }
    }

    public String toString() {
        JSONArray jSONArray = new JSONArray();
        IntSet intSet = this.bannersStats.keySet();
        IntIterator intIterator = intSet.iterator();
        while (intIterator.hasNext()) {
            int n = (Integer)intIterator.next();
            BannerStats bannerStats = (BannerStats)this.bannersStats.get(n);
            JSONObject jSONObject = new JSONObject();
            jSONObject.put("id", n);
            jSONObject.put("views", bannerStats.views);
            jSONObject.put("clicks", bannerStats.clicks);
            jSONArray.put((Object)jSONObject);
        }
        return jSONArray.toString();
    }
}

