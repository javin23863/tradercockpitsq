//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 7, {
        title: Ltsq('Genetic options'),
        help: Ltsq('Configure Genetic options'),
        helpURL: 'genetic-options',
        task: 'Build',
        customProjectsOnly: false,
        configElemName: 'WhatToBuild',
        dataItem: 'genetic-options',
        templateUrl: '../../../plugins/SettingsGeneticOptions/views/geneticOptions.html',
        controller: 'SettingsGeneticOptionsCtrl'
    });
});