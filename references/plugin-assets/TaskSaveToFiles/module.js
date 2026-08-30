angular.module('app.settings').config(function(sqPluginProvider) {
    
    sqPluginProvider.plugin("SimpleTaskSettings", 4, {
        taskType: 'SaveToFiles',
        templateUrl: '../../../plugins/TaskSaveToFiles/simpleSettings/simpleSettings.html',
        controller: 'SimpleSaveToFilesSettingsCtrl',
        getInfoPanels: function(xmlConfig, injector){
            return [];
        }
    });
    
});