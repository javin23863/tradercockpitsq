angular.module('app.brokers', ['sqplugin']).config(function(sqPluginProvider, SQEventsProvider) {
	sqPluginProvider.plugin("MainTab", 52, {
        title: Ltsq('Broker profiles'),
        help: Ltsq('Broker profiles allow you to create and manage settings specific to your broker, from filtering stocks for stockpicker engines to different configuration of instruments and sessions.'),
        helpQDM: Ltsq('Broker profiles allow you to create and manage settings specific to your broker.'),
        dataItem: 'brokers',
        templateUrl: '../../../plugins/DataManagerBroker/views/brokers.html',
        controller: 'BrokersController',
        menuActive: 'brokers'
    });
});