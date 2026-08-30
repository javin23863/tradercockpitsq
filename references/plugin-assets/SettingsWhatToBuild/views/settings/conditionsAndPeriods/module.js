angular.module('app.settings').config(function(sqPluginProvider) {

	sqPluginProvider.plugin("WhatToBuildSettings", 30, {
        settings: [Ltsq('# of Conditions, Periods')],
        name: 'conditionsAndPeriods',
        templateUrl: '../../../plugins/SettingsWhatToBuild/views/settings/conditionsAndPeriods/conditionsAndPeriods.html',
        controller: 'ConditionsAndPeriodsCtrl'
    });

});