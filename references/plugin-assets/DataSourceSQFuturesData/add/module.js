angular.module('app.datasource.sqFuturesData').config(function (sqPluginProvider, SQEventsProvider) {

    sqPluginProvider.plugin('DataSourceSQFuturesData', 10, {
        title: Ltsq('SQ Futures data') + ':' + Ltsq('Find data'),
        templateUrl: '../../../plugins/DataSourceSQFuturesData/add/addPopup.html',
        controller: 'SQFuturesDataAddCtrl',
        source: 'data',
        id: "datamanager-datasource-sqfuturesdata-add"
    });
});