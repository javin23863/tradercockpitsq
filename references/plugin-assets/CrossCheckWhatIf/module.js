angular.module('app').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("CrossCheck", 2, {
        name: 'WhatIf',
        title: Ltsq('What If simulations'),
        testSettingsTab: {
            templateUrl: '../../../plugins/CrossCheckWhatIf/settings.html',
            controller: 'WhatIfCtrl',
        }
    });
});