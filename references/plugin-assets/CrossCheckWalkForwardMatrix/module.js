angular.module('app').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("CrossCheck", 5, {
        name: 'WalkForwardMatrix',
        title: Ltsq('Walk-Forward Matrix'),
        testSettingsTab: {
            templateUrl: '../../../plugins/CrossCheckWalkForwardMatrix/settings.html',
            controller: 'WalkForwardMatrixCtrl',
        },
        filteringTab: {
            templateUrl: '../../../plugins/CrossCheckWalkForwardMatrix/filtering.html',
        }
    });
});