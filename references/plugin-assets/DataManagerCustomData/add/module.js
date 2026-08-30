angular.module('app.data').config(function(sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionCustomData', 10, {
        title: Ltsq('Add new'),
        templateUrl: '../../../plugins/DataManagerCustomData/add/customDataAdd.html',
        controller: 'CustomDataAddCtrl',
        source: 'custom-da-ta',
        class: 'custom-data',
        id: "datamanager-custom-data-add"
    });
});