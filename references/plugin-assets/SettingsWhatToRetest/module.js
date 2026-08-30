//product:TASKMANAGER
//product:RETESTER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 0, {
        title: Ltsq('What to retest'),
        help: Ltsq('Configure databank settings'),
        helpURL: 'what-to-retest',
        task: 'Retest',
        customProjectsOnly: true,
        configElemName: 'Databanks',
        relatedTask: 'Build', // Check sqUtilsCtrl.applyConfigToTask, Databanks used by WhatToBuild also
        dataItem: 'tm-what-to-retest',
        templateUrl: '../../../plugins/SettingsWhatToRetest/whatToRetest.html',
        controller: 'SettingsWhatToRetestCtrl'
    });
});