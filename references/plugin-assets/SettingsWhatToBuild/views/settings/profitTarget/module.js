angular.module('app.settings').config(function(sqPluginProvider) {

	sqPluginProvider.plugin("WhatToBuildSettings", 50, {
        settings: [Ltsq('Profit Target')],
        name: 'profitTarget',
        templateUrl: '../../../plugins/SettingsWhatToBuild/views/settings/profitTarget/profitTarget.html',
        controller: 'ProfitTargetCtrl'
    });

});