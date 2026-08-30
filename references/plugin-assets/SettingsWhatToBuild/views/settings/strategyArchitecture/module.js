angular.module('app.settings').config(function(sqPluginProvider) {

	sqPluginProvider.plugin("WhatToBuildSettings", 20, {
        settings: [Ltsq('Strategy style')],
        name: 'strategyArchitecture',
        templateUrl: '../../../plugins/SettingsWhatToBuild/views/settings/strategyArchitecture/strategyArchitecture.html',
        controller: 'StrategyArchitectureCtrl'
    });

});