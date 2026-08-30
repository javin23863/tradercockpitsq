angular.module('app.data').config(function(sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionExport', 10, {
        title: Ltsq('Export to CSV'),
        templateUrl: '../../../plugins/DataManagerActions/exportToCsv/exportToCSV.html',
        controller: 'ExportToCsvCtrl',
        source: 'data',
        class: 'export',
        id: "datamanager-export-csv"
    });
});