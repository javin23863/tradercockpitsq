angular.module('app.data').config(function(sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionExport', 10, {
        title: Ltsq('Export to MT4 (FXT & HST)'),
        templateUrl: '../../../plugins/DataManagerActions/exportToMT4/exportToMT4.html',
        controller: 'ExportToMT4Ctrl',
        source: 'data',
        class: 'export',
        id: "datamanager-export-mt4"
    });
});