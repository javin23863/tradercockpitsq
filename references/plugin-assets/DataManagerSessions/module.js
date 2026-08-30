angular.module('app.sessions', ['sqplugin']).config(function(sqPluginProvider, SQEventsProvider) {

	sqPluginProvider.plugin("MainTab", 40, {
        title: Ltsq('Sessions'),
        help: Ltsq('Sessions are definitions of trading time, from market opening to market closing.<br/>Please note that they are not used and not supported in forex / MetaTrader, you can safely use Forex_247 session in MT4.'),
        dataItem: 'sessions',
        templateUrl: '../../../plugins/DataManagerSessions/views/sessions.html',
        controller: 'SessionsController',
        menuActive: 'instruments-sessions'
    });
});