angular.module('app.datasource.files').config(function (sqPluginProvider, SQEventsProvider) {

    sqPluginProvider.plugin('DataSourceFiles', 50, {
        title: Ltsq('File import') + ':' + Ltsq('Import from SQ/QDM application'),
        templateUrl: '../../../plugins/DataSourceFiles/appImport/appImportPopup.html',
        controller: 'DataSourceFilesAppImportCtrl',
        source: 'data',
        id: "datamanager-datasource-files-app-import"
    });
});