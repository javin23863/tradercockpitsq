/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils.mousehook;

import com.strategyquant.lib.utils.mousehook.IMouseHookListener;
import java.awt.Component;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MouseHook
extends Thread {
    public static final Logger Log = LoggerFactory.getLogger(MouseHook.class);
    private ArrayList<IMouseHookListener> listeners = new ArrayList();
    private Point mousePoint;
    private Point dragStartPoint;
    private Component component;
    private boolean quit = false;
    private boolean mousePressed;
    private static final int dragDelay = 200;

    public MouseHook(Component component) {
        this.component = component;
    }

    @Override
    public void run() {
        this.component.addMouseListener(new MouseAdapter(){

            @Override
            public void mousePressed(MouseEvent mouseEvent) {
                MouseHook.this.mousePressed = true;
                MouseHook.this.dragStartPoint = MouseInfo.getPointerInfo().getLocation();
                MouseHook.this.firePressEvent(MouseHook.this.dragStartPoint);
            }

            @Override
            public void mouseReleased(MouseEvent mouseEvent) {
                MouseHook.this.mousePressed = false;
                MouseHook.this.fireReleaseEvent(MouseInfo.getPointerInfo().getLocation());
            }
        });
        int n = 0;
        while (!this.quit) {
            Point point = MouseInfo.getPointerInfo().getLocation();
            if (!point.equals(this.mousePoint)) {
                this.mousePoint = point;
                this.fireMoveEvent(this.mousePoint);
            }
            n = !this.mousePressed ? 0 : (n += 10);
            if (n > 200) {
                int n2 = point.x - this.dragStartPoint.x;
                int n3 = point.y - this.dragStartPoint.y;
                this.fireDragEvent(this.dragStartPoint, n2, n3);
                this.dragStartPoint = point;
            }
            try {
                Thread.sleep(10L);
            }
            catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
        }
    }

    public void addListener(IMouseHookListener iMouseHookListener) {
        this.listeners.add(iMouseHookListener);
    }

    public void removeListener(IMouseHookListener iMouseHookListener) {
        this.listeners.remove(iMouseHookListener);
    }

    private void fireDragEvent(Point point, int n, int n2) {
        Point point2 = this.getComponentPoint(point);
        for (IMouseHookListener iMouseHookListener : this.listeners) {
            iMouseHookListener.onGlobalMouseDrag(point, n, n2);
            if (point2 == null) continue;
            iMouseHookListener.onComponentMouseDrag(point2, n, n2);
        }
    }

    private void firePressEvent(Point point) {
        Point point2 = this.getComponentPoint(point);
        for (IMouseHookListener iMouseHookListener : this.listeners) {
            iMouseHookListener.onGlobalMousePress(point);
            if (point2 == null) continue;
            iMouseHookListener.onComponentMousePress(point2);
        }
    }

    private void fireReleaseEvent(Point point) {
        Point point2 = this.getComponentPoint(point);
        for (IMouseHookListener iMouseHookListener : this.listeners) {
            iMouseHookListener.onGlobalMouseRelease(point);
            if (point2 == null) continue;
            iMouseHookListener.onComponentMouseRelease(point2);
        }
    }

    private void fireMoveEvent(Point point) {
        Point point2 = this.getComponentPoint(point);
        for (IMouseHookListener iMouseHookListener : this.listeners) {
            iMouseHookListener.onGlobalMouseMove(point);
            if (point2 == null) continue;
            iMouseHookListener.onComponentMouseMove(point2);
        }
    }

    private Point getComponentPoint(Point point) {
        Point point2 = new Point();
        point2.x = point.x - this.component.getBounds().x;
        point2.y = point.y - this.component.getBounds().y;
        if (point2.x >= 0 && point2.x <= this.component.getWidth() && point2.y >= 0 && point2.y <= this.component.getHeight()) {
            return point2;
        }
        return null;
    }

    public static Point getComponentPoint(Point point, Component component) {
        Point point2 = new Point();
        point2.x = point.x - component.getBounds().x;
        point2.y = point.y - component.getBounds().y;
        if (point2.x >= 0 && point2.x <= component.getWidth() && point2.y >= 0 && point2.y <= component.getHeight()) {
            return point2;
        }
        return null;
    }

    public void destroy() {
        this.quit = true;
    }
}

