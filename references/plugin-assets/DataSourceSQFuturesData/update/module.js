angular.module('app.datasource.sqFuturesData').config(function (sqPluginProvider, SQEventsProvider) {

    sqPluginProvider.plugin('DataSourceSQFuturesData', 10, {
        title: Ltsq('SQ Futures data') + ':' + Ltsq('Update'),
        templateUrl: '../../../plugins/DataSourceSQFuturesData/update/updatePopup.html',
        controller: 'SQFuturesDataUpdateCtrl',
        source: 'data',
        id: "datamanager-datasource-sqfuturesdata-update"
    });
});