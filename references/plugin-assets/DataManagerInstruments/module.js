angular.module('app.instruments', ['sqplugin']).config(function(sqPluginProvider, SQEventsProvider) {

	sqPluginProvider.plugin("MainTab", 20, {
        title: Ltsq('Instruments'),
        help: Ltsq('Instruments are symbol specifications. Here you specify what is their type, point value pip/tick size, etc.<br/>For example, you can have multiple EURUSD data imported from various sources, but they all share the same instrument - EURUSD.'),
        dataItem: 'instruments',
        templateUrl: '../../../plugins/DataManagerInstruments/views/instruments.html',
        controller: 'InstrumentsController',
        menuActive: 'instruments-sessions'
    });
});