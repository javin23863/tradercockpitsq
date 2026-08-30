angular.module('app.datasource.dukascopy').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin(
        'DataSourceDukascopy',
        40,
        {
            title: Ltsq('Dukascopy data') + ':' + Ltsq('Dukascopy Data Disclaimer'),
            templateUrl: '../../../plugins/DataSourceDukascopy/disclaimer/disclaimerPopup.html',
            controller: 'dukascopyDataDisclaimerCtrl',
            source: 'data',
            id: "datamanager-datasource-dukascopy-disclaimer"
        }
    );
});