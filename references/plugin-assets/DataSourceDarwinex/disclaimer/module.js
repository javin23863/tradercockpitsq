angular.module('app.datasource.darwinex').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin(
        'DataSourceDarwinexNotUsed',
        40,
        {
            title: Ltsq('Darwinex Tick Data') + ':' + Ltsq('Darwinex Data Disclaimer'),
            templateUrl: '../../../plugins/DataSourceDarwinex/disclaimer/darwinexDisclaimerPopup.html',
            controller: 'darwinexDisclaimerCtrl',
            source: 'data',
            id: "datamanager-datasource-dukascopy-disclaimer"
        }
    );
});