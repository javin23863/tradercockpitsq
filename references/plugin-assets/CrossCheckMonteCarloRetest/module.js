angular.module('app').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("CrossCheck", 2, {
        name: 'MonteCarloRetest',
        title: Ltsq('Monte Carlo retest'),
        testSettingsTab: {
            templateUrl: '../../../plugins/CrossCheckMonteCarloRetest/settings.html',
            controller: 'MonteCarloRetestCtrl',
        }
    });
});