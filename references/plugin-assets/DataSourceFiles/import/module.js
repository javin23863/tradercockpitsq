angular.module('app.datasource.files').config(function (sqPluginProvider, SQEventsProvider) {

    sqPluginProvider.plugin('DataSourceFiles', 10, {
        title: Ltsq('File import') + ':' + Ltsq('Import file'),
        templateUrl: '../../../plugins/DataSourceFiles/import/importPopup.html',
        controller: 'DataSourceFilesImportCtrl',
        source: 'data',
        id: "datamanager-datasource-files-import"
    });
});