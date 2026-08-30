angular.module('app.brokers').config(function(sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionBroker', 20, {
        title: Ltsq('Update data for broker (automatic)'),
        key: 'update',
        source: 'brokers',
        class: 'brokers',
        id: "datamanager-broker-update",
        products: 'SQUANT' //SQUANT/QDM
    });
});