//product:TASKMANAGER
angular.module('app.settings').config(function(sqPluginProvider) {
	
	sqPluginProvider.plugin("SettingsTab", 50, {
        title: Ltsq('Load from files'),
        help: Ltsq('Load strategies from folder on the disc to a databank of your choice'),
        helpURL: 'load-from-files',
        task: 'LoadFromFiles',
        configElemName: 'LoadFromFiles',
        dataItem: 'tm-load-from-files',
        templateUrl: '../../../plugins/SettingsLoadFromFiles/settingsLoadFromFiles.html',
        controller: 'SettingsLoadFromFilesCtrl',
        getSettingsDescription : function(settingsElement, injector){
            return Ltsq('Load strategies from folder on the disc to a databank of your choice');
        }
    });
});