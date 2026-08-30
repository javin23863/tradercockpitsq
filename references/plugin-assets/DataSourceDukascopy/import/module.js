angular.module('app.datasource.dukascopy.import', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin(
        'DataSourceDukascopy',
        20,
        {
            title: Ltsq('Dukascopy data') + ':' + Ltsq('Download data for existing symbol'),
            templateUrl: '../../../plugins/DataSourceDukascopy/import/importPopup.html',
            controller: 'dukasImportPopupCtrl',
            source: 'data',
            id: "datamanager-datasource-dukascopy-import"
        }
    );
});