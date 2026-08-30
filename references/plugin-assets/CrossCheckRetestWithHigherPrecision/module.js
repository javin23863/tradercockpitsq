angular.module('app').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("CrossCheck", 4, {
        name: 'RetestWithHigherPrecision',
        title: Ltsq('Retest with higher precision'),
        testSettingsTab: {
            templateUrl: '../../../plugins/CrossCheckRetestWithHigherPrecision/settings.html',
            controller: 'RetestWithHigherPrecisionCtrl',
        }
    });
});