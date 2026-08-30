angular.module('app.datasource.yahoo').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin(
        'DataSourceYahoo',
        10,
        {
            title: Ltsq('Yahoo') + ':' + Ltsq('Add symbol'),
            templateUrl: '../../../plugins/DataSourceYahoo/add/addPopup.html',
            controller: 'yahooAddPopupCtrl',
            source: 'data',
            id: "datamanager-datasource-yahoo-add"
        }
    );
});