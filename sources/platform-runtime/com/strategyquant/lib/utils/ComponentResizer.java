/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

public class ComponentResizer
extends MouseAdapter {
    private static final Dimension MINIMUM_SIZE = new Dimension(10, 10);
    private static final Dimension MAXIMUM_SIZE = new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
    private static Map<Integer, Integer> cursors = new HashMap<Integer, Integer>();
    private Insets dragInsets;
    private Dimension snapSize;
    private int direction;
    protected static final int NORTH = 1;
    protected static final int WEST = 2;
    protected static final int SOUTH = 4;
    protected static final int EAST = 8;
    private Cursor sourceCursor;
    private boolean resizing;
    private Rectangle bounds;
    private Point pressed;
    private boolean autoscrolls;
    private Dimension minimumSize;
    private Dimension maximumSize;

    public ComponentResizer() {
        this(new Insets(5, 5, 5, 5), new Dimension(1, 1), new Component[0]);
    }

    public ComponentResizer(Component ... componentArray) {
        this(new Insets(5, 5, 5, 5), new Dimension(1, 1), componentArray);
    }

    public ComponentResizer(Insets insets, Component ... componentArray) {
        this(insets, new Dimension(1, 1), componentArray);
    }

    public ComponentResizer(Insets insets, Dimension dimension, Component ... componentArray) {
        cursors.put(1, 8);
        cursors.put(2, 10);
        cursors.put(4, 9);
        cursors.put(8, 11);
        cursors.put(3, 6);
        cursors.put(9, 7);
        cursors.put(6, 4);
        cursors.put(12, 5);
        this.minimumSize = MINIMUM_SIZE;
        this.maximumSize = MAXIMUM_SIZE;
        this.setDragInsets(insets);
        this.setSnapSize(dimension);
        this.registerComponent(componentArray);
    }

    public Insets getDragInsets() {
        return this.dragInsets;
    }

    public void setDragInsets(Insets insets) {
        this.validateMinimumAndInsets(this.minimumSize, insets);
        this.dragInsets = insets;
    }

    public Dimension getMaximumSize() {
        return this.maximumSize;
    }

    public void setMaximumSize(Dimension dimension) {
        this.maximumSize = dimension;
    }

    public Dimension getMinimumSize() {
        return this.minimumSize;
    }

    public void setMinimumSize(Dimension dimension) {
        this.validateMinimumAndInsets(dimension, this.dragInsets);
        this.minimumSize = dimension;
    }

    public void deregisterComponent(Component ... componentArray) {
        for (Component component : componentArray) {
            component.removeMouseListener(this);
            component.removeMouseMotionListener(this);
        }
    }

    public void registerComponent(Component ... componentArray) {
        for (Component component : componentArray) {
            component.addMouseListener(this);
            component.addMouseMotionListener(this);
        }
    }

    public Dimension getSnapSize() {
        return this.snapSize;
    }

    public void setSnapSize(Dimension dimension) {
        this.snapSize = dimension;
    }

    private void validateMinimumAndInsets(Dimension dimension, Insets insets) {
        int n = insets.left + insets.right;
        int n2 = insets.top + insets.bottom;
        if (dimension.width < n || dimension.height < n2) {
            String string = "Minimum size cannot be less than drag insets";
            throw new IllegalArgumentException(string);
        }
    }

    @Override
    public void mouseMoved(MouseEvent mouseEvent) {
        Component component = mouseEvent.getComponent();
        Point point = mouseEvent.getPoint();
        this.direction = 0;
        if (point.x < this.dragInsets.left) {
            this.direction += 2;
        }
        if (point.x > component.getWidth() - this.dragInsets.right - 1) {
            this.direction += 8;
        }
        if (point.y < this.dragInsets.top) {
            ++this.direction;
        }
        if (point.y > component.getHeight() - this.dragInsets.bottom - 1) {
            this.direction += 4;
        }
        if (this.direction == 0) {
            component.setCursor(this.sourceCursor);
        } else {
            int n = cursors.get(this.direction);
            Cursor cursor = Cursor.getPredefinedCursor(n);
            component.setCursor(cursor);
        }
    }

    @Override
    public void mouseEntered(MouseEvent mouseEvent) {
        if (!this.resizing) {
            Component component = mouseEvent.getComponent();
            this.sourceCursor = component.getCursor();
        }
    }

    @Override
    public void mouseExited(MouseEvent mouseEvent) {
        if (!this.resizing) {
            Component component = mouseEvent.getComponent();
            component.setCursor(this.sourceCursor);
        }
    }

    @Override
    public void mousePressed(MouseEvent mouseEvent) {
        if (this.direction == 0) {
            return;
        }
        this.resizing = true;
        Component component = mouseEvent.getComponent();
        this.pressed = mouseEvent.getPoint();
        SwingUtilities.convertPointToScreen(this.pressed, component);
        this.bounds = component.getBounds();
        if (component instanceof JComponent) {
            JComponent jComponent = (JComponent)component;
            this.autoscrolls = jComponent.getAutoscrolls();
            jComponent.setAutoscrolls(false);
        }
    }

    @Override
    public void mouseReleased(MouseEvent mouseEvent) {
        this.resizing = false;
        Component component = mouseEvent.getComponent();
        component.setCursor(this.sourceCursor);
        if (component instanceof JComponent) {
            ((JComponent)component).setAutoscrolls(this.autoscrolls);
        }
    }

    @Override
    public void mouseDragged(MouseEvent mouseEvent) {
        if (!this.resizing) {
            return;
        }
        Component component = mouseEvent.getComponent();
        Point point = mouseEvent.getPoint();
        SwingUtilities.convertPointToScreen(point, component);
        this.changeBounds(component, this.direction, this.bounds, this.pressed, point);
    }

    protected void changeBounds(Component component, int n, Rectangle rectangle, Point point, Point point2) {
        int n2;
        int n3;
        int n4;
        int n5 = rectangle.x;
        int n6 = rectangle.y;
        int n7 = rectangle.width;
        int n8 = rectangle.height;
        if (2 == (n & 2)) {
            n4 = this.getDragDistance(point.x, point2.x, this.snapSize.width);
            n3 = Math.min(n7 + n5, this.maximumSize.width);
            n4 = this.getDragBounded(n4, this.snapSize.width, n7, this.minimumSize.width, n3);
            n5 -= n4;
            n7 += n4;
        }
        if (1 == (n & 1)) {
            n4 = this.getDragDistance(point.y, point2.y, this.snapSize.height);
            n3 = Math.min(n8 + n6, this.maximumSize.height);
            n4 = this.getDragBounded(n4, this.snapSize.height, n8, this.minimumSize.height, n3);
            n6 -= n4;
            n8 += n4;
        }
        if (8 == (n & 8)) {
            n4 = this.getDragDistance(point2.x, point.x, this.snapSize.width);
            Dimension dimension = this.getBoundingSize(component);
            n2 = Math.min(dimension.width - n5, this.maximumSize.width);
            n4 = this.getDragBounded(n4, this.snapSize.width, n7, this.minimumSize.width, n2);
            n7 += n4;
        }
        if (4 == (n & 4)) {
            n4 = this.getDragDistance(point2.y, point.y, this.snapSize.height);
            Dimension dimension = this.getBoundingSize(component);
            n2 = Math.min(dimension.height - n6, this.maximumSize.height);
            n4 = this.getDragBounded(n4, this.snapSize.height, n8, this.minimumSize.height, n2);
            n8 += n4;
        }
        component.setBounds(n5, n6, n7, n8);
        component.validate();
    }

    private int getDragDistance(int n, int n2, int n3) {
        int n4;
        int n5 = n3 / 2;
        n4 += (n4 = n - n2) < 0 ? -n5 : n5;
        n4 = n4 / n3 * n3;
        return n4;
    }

    private int getDragBounded(int n, int n2, int n3, int n4, int n5) {
        while (n3 + n < n4) {
            n += n2;
        }
        while (n3 + n > n5) {
            n -= n2;
        }
        return n;
    }

    private Dimension getBoundingSize(Component component) {
        if (component instanceof Window) {
            GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
            Rectangle rectangle = graphicsEnvironment.getMaximumWindowBounds();
            return new Dimension(rectangle.width, rectangle.height);
        }
        return component.getParent().getSize();
    }
}

