angular.module('app.datasource.crypto.import', ['sqplugin']).config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin(
        'DataSourceCrypto',
        20,
        {
            title: Ltsq('Crypto') + ':' + Ltsq('Download data for existing symbol'),
            templateUrl: '../../../plugins/DataSourceCrypto/import/importPopup.html',
            controller: 'cryptoImportPopupCtrl',
            source: 'data',
            id: "datamanager-datasource-crypto-import"
        }
    );
});