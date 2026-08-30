angular.module('app.data').config(function(sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionCustomData', 30, {
        title: Ltsq('Recognize from file'),
        templateUrl: '../../../plugins/DataManagerCustomData/recognize/customDataRecognize.html',
        controller: 'CustomDataRecognizeCtrl',
        source: 'custom-da-ta',
        class: 'custom-data',
        id: "datamanager-custom-data-recognize"
    });
});