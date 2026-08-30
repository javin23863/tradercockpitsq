angular.module('app.brokers').config(function(sqPluginProvider) {

    sqPluginProvider.plugin('DataManagerActionBroker', 100, {
        title: Ltsq('Edit stocks'),
        key: 'edit-stocks',
        source: 'brokers',
        class: 'brokers',
        id: "datamanager-broker-edit",
        products: 'SQUANT' //SQUANT/QDM
    });
});