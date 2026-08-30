//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 50, {
        title: Ltsq('Save to files'),
        help: Ltsq('Save strategies from databank to a folder(s) and format(s) of your choice'),
        helpURL: 'save-to-files',
        task: 'SaveToFiles',
        configElemName: 'SaveToFiles',
        dataItem: 'tm-save-to-files',
        templateUrl: '../../../plugins/SettingsSaveToFiles/settingsSaveToFiles.html',
        controller: 'SettingsSaveToFilesCtrl',
        getSettingsDescription : function(settingsElement, injector){
            return Ltsq('Save strategies from databank to a folder(s) and format(s) of your choice');
        }
    });
});