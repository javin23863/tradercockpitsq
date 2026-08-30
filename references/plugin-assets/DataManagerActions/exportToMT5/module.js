angular.module('app.data').config(function (sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionExport', 10, {
        title: Ltsq('Export to MT5 data (99% test)'),
        templateUrl: '../../../plugins/DataManagerActions/exportToMT5/exportToMT5.html',
        controller: 'ExportToMT5Ctrl',
        source: 'data',
        class: 'export',
        id: "datamanager-export-mt5"
    });
});