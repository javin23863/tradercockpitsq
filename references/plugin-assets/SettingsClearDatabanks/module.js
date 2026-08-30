//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 10, {
        title: Ltsq('Clear databanks'),
        help: Ltsq('Deletes all strategies from selected databanks.'),
        helpURL: 'clear-databanks',
        task: 'ClearDatabanks',
        configElemName: 'ClearDatabanks',
        dataItem: 'tm-clear-databanks',
        templateUrl: '../../../plugins/SettingsClearDatabanks/clearDatabanks.html',
        controller: 'SettingsClearDatabanksCtrl',
        getSettingsDescription : function(settingsElement, injector){
            return "";
        }
    });
});