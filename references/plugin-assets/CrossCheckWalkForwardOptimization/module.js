angular.module('app').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("CrossCheck", 6, {
        name: 'WalkForwardOptimization',
        title: Ltsq('Walk-Forward Optimization'),
        testSettingsTab: {
            templateUrl: '../../../plugins/CrossCheckWalkForwardOptimization/settings.html',
            controller: 'WalkForwardOptimizationCtrl',
        },
        filteringTab: {
            templateUrl: '../../../plugins/CrossCheckWalkForwardOptimization/filtering.html',
        },
    });
});