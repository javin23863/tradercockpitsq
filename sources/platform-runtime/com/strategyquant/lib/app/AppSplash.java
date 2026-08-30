/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.app;

import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;
import com.strategyquant.lib.app.MainApp;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.UIManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AppSplash {
    public static final Logger Log = LoggerFactory.getLogger((String)"AppSplash");
    private BufferedImage image;
    private BufferedImage imageResized;
    private int width = 600;
    private int height = 350;
    private Window w;

    public AppSplash() {
        File file = new File("");
        String string = file.getAbsolutePath();
        int n = 300;
        String string2 = "";
        if (MainApp.checkProduct("QDM")) {
            string2 = "-qdm";
        } else if (MainApp.checkProduct("SQEDITOR")) {
            string2 = "-ce";
        } else if (MainApp.checkProduct("AlgoWizard") || MainApp.checkProduct("AlgoWizardStandalone")) {
            string2 = "-aw";
        }
        String string3 = string + "/internal/icons/sq_splash" + string2 + "@" + n + ".png";
        try {
            this.image = ImageIO.read(new File(string3));
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
        catch (Exception exception) {
            Log.error("error reading file or setting look and feel");
        }
        int n2 = 1800;
        int n3 = 1050;
        double d = (double)Toolkit.getDefaultToolkit().getScreenResolution() / 96.0 * 100.0;
        double d2 = d / (double)n;
        int n4 = (int)Math.round((double)n2 * d2);
        int n5 = (int)Math.round((double)n3 * d2);
        ResampleOp resampleOp = new ResampleOp(n4, n5);
        resampleOp.setFilter(ResampleFilters.getLanczos3Filter());
        this.imageResized = resampleOp.filter(this.image, null);
        this.w = new Window(null){

            @Override
            public void paint(Graphics graphics) {
                graphics.drawImage(AppSplash.this.imageResized, 0, 0, AppSplash.this.width, AppSplash.this.height, this);
            }

            @Override
            public void update(Graphics graphics) {
                this.paint(graphics);
            }
        };
        Dimension dimension = Toolkit.getDefaultToolkit().getScreenSize();
        this.w.setAlwaysOnTop(true);
        this.w.setBounds(new Rectangle(dimension.width / 2 - this.width / 2, dimension.height / 2 - this.height / 2, this.width, this.height));
        this.w.setBackground(new Color(0, false));
        this.w.setVisible(true);
    }

    public void dispose() {
        this.w.dispose();
    }
}

