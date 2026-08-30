angular.module('app.datasource.files').config(function (sqPluginProvider, SQEventsProvider) {

    sqPluginProvider.plugin('DataSourceFiles', 10, {
        title: Ltsq('File import') + ':' + Ltsq('Mass import'),
        templateUrl: '../../../plugins/DataSourceFiles/massImport/massImportPopup.html',
        controller: 'DataSourceFilesMassImportCtrl',
        source: 'data',
        id: "datamanager-datasource-files-mass-import"
    });
});