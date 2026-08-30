angular.module('app.brokers').config(function(sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionBroker', 10, {
        title: Ltsq('Add new'),
        key: 'add',
        source: 'brokers',
        class: 'brokers',
        id: "datamanager-broker-add"
    });
});