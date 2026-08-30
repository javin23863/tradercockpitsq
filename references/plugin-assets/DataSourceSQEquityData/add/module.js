angular.module('app.datasource.sqEquityData').config(function (sqPluginProvider, SQEventsProvider) {

    sqPluginProvider.plugin('DataSourceSQEquityData', 10, {
        title: Ltsq('SQ Equity data') + ':' + Ltsq('Find data'),
        templateUrl: '../../../plugins/DataSourceSQEquityData/add/addPopup.html',
        controller: 'SQEquityDataAddCtrl',
        source: 'data',
        id: "datamanager-datasource-sqequitydata-add"
    });
});