angular.module('app.settings').config(function(sqPluginProvider) {

	sqPluginProvider.plugin("WhatToBuildSettings", 40, {
        settings: [Ltsq('Stop Loss')],
        name: 'stopLoss',
        templateUrl: '../../../plugins/SettingsWhatToBuild/views/settings/stopLoss/stopLoss.html',
        controller: 'StopLossCtrl'
    });

});