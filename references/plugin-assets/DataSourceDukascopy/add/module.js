angular.module('app.datasource.dukascopy').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin(
        'DataSourceDukascopy',
        10,
        {
            title: Ltsq('Dukascopy data') + ':' + Ltsq('Add new Dukascopy data symbol'),
            templateUrl: '../../../plugins/DataSourceDukascopy/add/addPopup.html',
            controller: 'dukasAddPopupCtrl',
            source: 'data',
            id: "datamanager-datasource-dukascopy-add"
        }
    );
});