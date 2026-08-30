/*
 * Decompiled with CFR 0.152.
 */
package com.strategyquant.lib.utils;

import com.strategyquant.lib.app.MainApp;
import com.strategyquant.lib.utils.SwingUtils;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;
import javax.swing.Action;
import javax.swing.JFileChooser;
import javax.swing.JTable;
import javax.swing.RowSorter;
import javax.swing.SortOrder;
import javax.swing.table.TableModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQFileChooserFactory {
    private static final String VIEW_TYPE_LIST = "viewTypeList";
    private static final String VIEW_TYPE_DETAILS = "viewTypeDetails";
    private static final String CHOOSER_CLOSING_PROPERTY = "JFileChooserDialogIsClosingProperty";
    private static final String VIEW_TYPE_PROPERTY = "viewType";
    private static final String IS_DETAILS = "isDetails";
    private static final String SORT_ORDER = "sortOrder";

    private SQFileChooserFactory() {
    }

    public static ISQFileChooser createPersistingJFileChooser(String string) {
        JFileChooserPersisterImpl jFileChooserPersisterImpl = new JFileChooserPersisterImpl(string);
        boolean bl = Boolean.parseBoolean(MainApp.settings().get("useAdvancedFileChooser", "true"));
        if (bl) {
            jFileChooserPersisterImpl.init();
        }
        return jFileChooserPersisterImpl;
    }

    private static class JFileChooserPersisterImpl
    implements ISQFileChooser {
        private final Logger logger = LoggerFactory.getLogger(this.getClass());
        private final Preferences persistentPrefs = Preferences.userNodeForPackage(this.getClass());
        private final JFileChooser chooser;
        private boolean isDetails;
        private OnChooserClosing chooserClosingListener;
        private OnViewTypeChanged viewTypeChangedListener;

        public JFileChooserPersisterImpl(String string) {
            if (string != null) {
                string = string.trim();
            }
            this.chooser = new JFileChooser(string);
        }

        public void init() {
            this.restoreSettings();
            this.registerForViewTypeChangeEvents();
            this.chooserClosingListener = new OnChooserClosing();
            this.chooser.addPropertyChangeListener(SQFileChooserFactory.CHOOSER_CLOSING_PROPERTY, this.chooserClosingListener);
        }

        @Override
        public JFileChooser getJFileChooser() {
            return this.chooser;
        }

        private void persistSettings() {
            this.persistentPrefs.putBoolean(SQFileChooserFactory.IS_DETAILS, this.isDetails);
            if (this.isDetails) {
                this.persistSortOrder();
            }
        }

        private void persistSortOrder() {
            byte[] byArray = this.serializeSortOrder();
            if (byArray != null) {
                this.persistentPrefs.putByteArray(SQFileChooserFactory.SORT_ORDER, byArray);
            }
        }

        private byte[] serializeSortOrder() {
            RowSorter<?> rowSorter = this.getRowSorter();
            if (rowSorter != null) {
                byte[] byArray;
                List<RowSorter.SortKey> list = rowSorter.getSortKeys();
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
                try {
                    objectOutputStream.writeObject(new SortOrderInfo(list));
                    byArray = byteArrayOutputStream.toByteArray();
                }
                catch (Throwable throwable) {
                    try {
                        try {
                            objectOutputStream.close();
                        }
                        catch (Throwable throwable2) {
                            throwable.addSuppressed(throwable2);
                        }
                        throw throwable;
                    }
                    catch (IOException iOException) {
                        this.logger.error("Could not serialize JFileChooser row sort order.", (Throwable)iOException);
                    }
                }
                objectOutputStream.close();
                return byArray;
            }
            return null;
        }

        private void restoreSettings() {
            this.isDetails = this.persistentPrefs.getBoolean(SQFileChooserFactory.IS_DETAILS, false);
            if (this.isDetails) {
                this.setToDetailsView();
                this.applyInitialSortOrder();
            } else {
                this.setToListView();
            }
        }

        private void setToDetailsView() {
            try {
                Action action = this.chooser.getActionMap().get(SQFileChooserFactory.VIEW_TYPE_DETAILS);
                action.actionPerformed(null);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }

        private void setToListView() {
            try {
                Action action = this.chooser.getActionMap().get(SQFileChooserFactory.VIEW_TYPE_LIST);
                action.actionPerformed(null);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }

        private void applyInitialSortOrder() {
            byte[] byArray = this.persistentPrefs.getByteArray(SQFileChooserFactory.SORT_ORDER, null);
            if (byArray == null) {
                return;
            }
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byArray);
            try (ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);){
                this.setSortInfo((SortOrderInfo)objectInputStream.readObject());
            }
            catch (IOException | ClassNotFoundException exception) {
                this.logger.error("Could not deserialize JFileChooser row sort order.", (Throwable)exception);
            }
        }

        private void setSortInfo(SortOrderInfo sortOrderInfo) {
            sortOrderInfo.setSortOrder(this.getRowSorter());
        }

        private RowSorter<?> getRowSorter() {
            JTable jTable = SwingUtils.getDescendantsOfType(JTable.class, this.chooser).get(0);
            RowSorter<? extends TableModel> rowSorter = jTable.getRowSorter();
            return rowSorter;
        }

        private void registerForViewTypeChangeEvents() {
            try {
                this.viewTypeChangedListener = new OnViewTypeChanged();
            }
            catch (Exception exception) {
                // empty catch block
            }
        }

        @Override
        public void close() {
            this.chooser.removePropertyChangeListener(SQFileChooserFactory.CHOOSER_CLOSING_PROPERTY, this.chooserClosingListener);
        }

        private final class OnChooserClosing
        implements PropertyChangeListener {
            private OnChooserClosing() {
            }

            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
                JFileChooserPersisterImpl.this.persistSettings();
            }
        }

        public static class SortOrderInfo
        implements Serializable {
            private static final long serialVersionUID = -5393878644049680645L;
            private final List<ColumnSortInfo> keyInfo = new ArrayList<ColumnSortInfo>();

            public SortOrderInfo(List<? extends RowSorter.SortKey> list) {
                for (RowSorter.SortKey sortKey : list) {
                    this.keyInfo.add(new ColumnSortInfo(sortKey));
                }
            }

            public void setSortOrder(RowSorter<?> rowSorter) {
                rowSorter.setSortKeys(this.makeSortKeys());
            }

            private List<RowSorter.SortKey> makeSortKeys() {
                ArrayList<RowSorter.SortKey> arrayList = new ArrayList<RowSorter.SortKey>();
                for (ColumnSortInfo columnSortInfo : this.keyInfo) {
                    arrayList.add(columnSortInfo.makeSortKey());
                }
                return arrayList;
            }

            public static class ColumnSortInfo
            implements Serializable {
                private static final long serialVersionUID = 5406885180955729893L;
                private final SortOrder sortOrder;
                private final int column;

                public ColumnSortInfo(RowSorter.SortKey sortKey) {
                    this.column = sortKey.getColumn();
                    this.sortOrder = sortKey.getSortOrder();
                }

                public RowSorter.SortKey makeSortKey() {
                    return new RowSorter.SortKey(this.column, this.sortOrder);
                }
            }
        }

        private class OnViewTypeChanged
        implements PropertyChangeListener {
            private OnViewTypeChanged() {
            }

            @Override
            public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
            }
        }
    }

    public static interface ISQFileChooser
    extends AutoCloseable {
        public JFileChooser getJFileChooser();

        @Override
        public void close();
    }
}

