angular.module('app.data').config(function(sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionCustomData', 20, {
        title: Ltsq('Import indicator data'),
        templateUrl: '../../../plugins/DataManagerCustomData/import/customDataImport.html',
        controller: 'CustomDataImportCtrl',
        source: 'custom-da-ta',
        class: 'custom-data',
        id: "datamanager-custom-data-import"
    });
});