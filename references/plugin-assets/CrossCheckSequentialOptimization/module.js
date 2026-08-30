angular.module('app').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("CrossCheck", 10, {
        name: 'SequentialOptimization',
        title: Ltsq('Sequential Optimization'),
        testSettingsTab: {
            templateUrl: '../../../plugins/CrossCheckSequentialOptimization/settings.html',
            controller: 'SequentialOptimizationCtrl',
        },
        filteringTab: {
            templateUrl: '../../../plugins/CrossCheckSequentialOptimization/filtering.html',
        }
    });
});