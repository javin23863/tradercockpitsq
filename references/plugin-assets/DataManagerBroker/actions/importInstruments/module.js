angular.module('app.brokers').config(function(sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionBroker', 25, {
        title: Ltsq('Import broker instruments from XML'),
        key: 'import-instruments',
        source: 'brokers',
        class: 'brokers',
        id: "datamanager-broker-import-instruments"
    });
});