//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 10, {
        title: Ltsq('Automatic retest'),
        help: Ltsq('Automatically retest strategies using their last settings on the full data - this is usable when you download most recent data and you want to retest the strategies on them.'),
        helpURL: 'automatic-retest',
        task: 'AutomaticRetest',
        configElemName: 'AutomaticRetest',
        dataItem: 'tm-automatic-retest',
        templateUrl: '../../../plugins/SettingsAutomaticRetest/automaticRetest.html',
        controller: 'SettingsAutomaticRetestCtrl',
        getSettingsDescription : function(settingsElement, injector){
            return "";
        }
    });
});