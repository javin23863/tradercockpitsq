angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 50, {
        title: Ltsq('Log databank stats'),
        help: Ltsq('Saves current databank statistics to the log. Databank statistics consist of: Number of strategies in databank, Sum of the given metric, Average of the given metric'),
        helpURL: 'log-databank-stats',
        task: 'LogDatabankStats',
        configElemName: 'LogDatabankStats',
        dataItem: 'tm-log-databank-stats',
        templateUrl: '../../../plugins/SettingsLogDatabankStats/logDatabankStats.html',
        controller: 'LogDatabankStatsCtrl',
        getSettingsDescription : function(settingsElement, injector){
			return "Log databank stats";
        }
    });
});