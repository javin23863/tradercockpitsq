angular.module('app.datasource.yahoo').config(function (sqPluginProvider, SQEventsProvider) {

    sqPluginProvider.plugin('DataSourceYahoo', 20, {
        title: Ltsq('Yahoo') + ':' + Ltsq('Download data for existing symbol'),
        templateUrl: '../../../plugins/DataSourceYahoo/download/downloadPopup.html',
        controller: 'yahooDownloadPopupCtrl',
        source: 'data',
        id: "datamanager-datasource-yahoo-update"
    });
});