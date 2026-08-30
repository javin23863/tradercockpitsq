angular.module('app.datasource.td').config(function (sqPluginProvider, SQEventsProvider) {

    sqPluginProvider.plugin('DataSourceTD', 10, {
        title: Ltsq('TickDownloader import') + ':' + Ltsq('Import data'),
        templateUrl: '../../../plugins/DataSourceTD/import/importPopup.html',
        controller: 'DataSourceTDImportCtrl',
        source: 'data',
        id: "datamanager-datasource-td-import",
    });
});