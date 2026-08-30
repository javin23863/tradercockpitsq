angular.module('app.datasource.files').config(function (sqPluginProvider, SQEventsProvider) {

    sqPluginProvider.plugin('DataSourceFiles', 10, {
        title: Ltsq('File import') + ':' + Ltsq('Add symbol'),
        templateUrl: '../../../plugins/DataSourceFiles/add/addPopup.html',
        controller: 'DataSourceFilesAddCtrl',
        source: 'data',
        id: "datamanager-datasource-files-add"
    });
});