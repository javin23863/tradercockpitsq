angular.module('app.settings').config(function(sqPluginProvider) {

	sqPluginProvider.plugin("WhatToBuildSettings", 30, {
        settings: [Ltsq('Build mode')],
        name: 'buildMode',
        templateUrl: '../../../plugins/SettingsWhatToBuild/views/settings/buildMode/buildMode.html',
        controller: 'BuildModeCtrl'
    });

});