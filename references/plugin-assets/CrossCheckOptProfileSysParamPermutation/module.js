angular.module('app').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("CrossCheck", 10, {
        name: 'OptProfileSysParamPermutation',
        title: Ltsq('Opt. Profile / Sys. Param. Permutation'),
        testSettingsTab: {
            templateUrl: '../../../plugins/CrossCheckOptProfileSysParamPermutation/settings.html',
            controller: 'OptProfileSysParamPermutationCtrl',
        },
        filteringTab: {
            templateUrl: '../../../plugins/CrossCheckOptProfileSysParamPermutation/filtering.html',
        }
    });
});