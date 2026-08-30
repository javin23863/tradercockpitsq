angular.module('app.datasource.crypto', ['sqplugin']).config(function (sqPluginProvider) {

    sqPluginProvider.plugin(
        "DataSourceCrypto",
        10, {
            title: Ltsq('Crypto') + ':',
            class: "crypto",
            source: 'data',
            id: "datamanager-datasource-crypto-menu"
        }
    );
});