angular.module('app.settings').config(function(sqPluginProvider) {

	sqPluginProvider.plugin("WhatToBuildSettings", 10, {
        settings: [Ltsq('Trading directions')],
        name: 'tradingDirections',
        templateUrl: '../../../plugins/SettingsWhatToBuild/views/settings/tradingDirections/tradingDirections.html',
        controller: "TradingDirectionsCtrl"
    });

});