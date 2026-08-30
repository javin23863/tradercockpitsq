angular.module('app').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("CrossCheck", 1, {
        name: 'MonteCarloManipulation',
        title: Ltsq('Monte Carlo trades manipulation'),
        testSettingsTab: {
            templateUrl: '../../../plugins/CrossCheckMonteCarloManipulation/settings.html',
            controller: 'MonteCarloManipulationCtrl',
        }
    });
});