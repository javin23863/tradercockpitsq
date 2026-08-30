angular.module('app.brokers').config(function(sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionBroker', 26, {
        title: Ltsq('Import broker sessions from XML'),
        key: 'import-sessions',
        source: 'brokers',
        class: 'brokers',
        id: "datamanager-broker-import-sessions"
    });
});