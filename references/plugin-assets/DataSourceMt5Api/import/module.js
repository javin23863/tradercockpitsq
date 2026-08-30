angular.module('app.datasource.mt5api').config(function (sqPluginProvider, SQEventsProvider) {

    sqPluginProvider.plugin('DataSourceMt5Api', 10, {
        title: Ltsq('MT5 import') + ':' + Ltsq('Import data'),
        templateUrl: '../../../plugins/DataSourceMt5Api/import/importPopup.html',
        controller: 'DataSourceMt5ApiImportCtrl',
        source: 'data',
        id: "datamanager-datasource-mt5api-import",
    });
});