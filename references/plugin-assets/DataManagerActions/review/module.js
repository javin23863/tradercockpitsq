angular.module('app.data').config(function (sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionTools', 10, {
        title: Ltsq('View & Analyze'),
        templateUrl: '../../../plugins/DataManagerActions/review/review.html',
        controller: 'ReviewCtrl',
        source: 'data,custom-da-ta',
        class: 'view',
        id: "datamanager-export-review",
    });
});