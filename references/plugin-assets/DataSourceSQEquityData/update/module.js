angular.module('app.datasource.sqEquityData').config(function (sqPluginProvider, SQEventsProvider) {

    sqPluginProvider.plugin('DataSourceSQEquityData', 10, {
        title: Ltsq('SQ Equity data') + ':' + Ltsq('Update'),
        templateUrl: '../../../plugins/DataSourceSQEquityData/update/updatePopup.html',
        controller: 'SQEquityDataUpdateCtrl',
        source: 'data',
        id: "datamanager-datasource-sqequitydata-update"
    });
});